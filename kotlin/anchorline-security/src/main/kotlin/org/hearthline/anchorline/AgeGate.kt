package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class AgePredicateResult {
    AGE_OVER_30,
    NOT_AGE_OVER_30,
    UNAVAILABLE,
    INVALID,
    EXPIRED,
    NONCE_MISMATCH
}

/**
 * INV-AGE-05:
 *
 * This is the complete projection visible outside AgeGate. It has no DOB, age,
 * age range, identity information, proof content, provider subject, document,
 * credential ID, payment value, device detail, or verification method.
 */
data class AgeEligibilityGrant(
    val accountId: AccountId,
    val state: AgeEligibilityState,
    val expiresAt: Instant?,
    val policyVersion: String,
    val updatedAt: Instant
)

data class AgeProofChallenge(
    val challengeId: ChallengeId,
    val accountId: AccountId,
    val audience: String,
    val nonce: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant? = null
)

/**
 * Opaque proof bytes must never escape the AgeGate verifier call.
 */
@JvmInline
value class OpaqueAgeProofPresentation(val bytes: ByteArray)

interface AgeProofChallengeRepository {
    fun save(challenge: AgeProofChallenge)
    fun consume(challengeId: ChallengeId, at: Instant): AgeProofChallenge?
    fun deleteExpired(now: Instant): Int
}

interface AgeEligibilityGrantRepository {
    fun save(grant: AgeEligibilityGrant)
    fun find(accountId: AccountId): AgeEligibilityGrant?
}

interface ThirdPartyAgePredicateVerifier {
    /**
     * Verifies:
     * - `age_over_30` predicate;
     * - exact Hearthline AgeGate audience;
     * - fresh nonce;
     * - issuer cryptographic validity;
     * - proof expiry;
     * - replay/status conditions.
     *
     * It must not return DOB, exact age, age range, subject ID, credential ID,
     * name, address, document, payment data, or proof method metadata.
     */
    fun verify(
        expectedAudience: String,
        expectedNonce: ByteArray,
        rawPresentation: OpaqueAgeProofPresentation
    ): AgePredicateResult
}

sealed interface AgeGateRoute {
    data object Eligible : AgeGateRoute
    data object NotEligible : AgeGateRoute
    data object AccessibleReview : AgeGateRoute
    data object Retry : AgeGateRoute
}

class AgeGateService(
    private val clock: Clock,
    private val challenges: AgeProofChallengeRepository,
    private val grants: AgeEligibilityGrantRepository,
    private val verifier: ThirdPartyAgePredicateVerifier
) {
    companion object {
        const val AUDIENCE = "https://agegate.hearthline.example"
        const val POLICY_VERSION = "age-over-30-predicate-v1"

        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
        private val GRANT_TTL: Duration = Duration.ofDays(365)
    }

    fun begin(accountId: AccountId): AgeProofChallenge {
        val now = clock.instant()

        return AgeProofChallenge(
            challengeId = ChallengeId(UUID.randomUUID()),
            accountId = accountId,
            audience = AUDIENCE,
            nonce = CryptoSupport.randomBytes(32),
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL)
        ).also(challenges::save)
    }

    /**
     * INV-AGE-04:
     * Raw proof data exists only as an input to this method. It is neither
     * returned nor stored.
     */
    fun verify(
        accountId: AccountId,
        challengeId: ChallengeId,
        presentation: OpaqueAgeProofPresentation
    ): AgeGateRoute {
        val now = clock.instant()

        val challenge = challenges.consume(challengeId, now)
            ?: return routeToAccessibleReview(accountId, now)

        if (challenge.accountId != accountId || now >= challenge.expiresAt) {
            return AgeGateRoute.Retry
        }

        return when (
            verifier.verify(
                expectedAudience = challenge.audience,
                expectedNonce = challenge.nonce,
                rawPresentation = presentation
            )
        ) {
            AgePredicateResult.AGE_OVER_30 -> {
                grants.save(
                    AgeEligibilityGrant(
                        accountId = accountId,
                        state = AgeEligibilityState.ELIGIBLE,
                        expiresAt = now.plus(GRANT_TTL),
                        policyVersion = POLICY_VERSION,
                        updatedAt = now
                    )
                )
                AgeGateRoute.Eligible
            }

            AgePredicateResult.NOT_AGE_OVER_30 -> {
                grants.save(
                    AgeEligibilityGrant(
                        accountId = accountId,
                        state = AgeEligibilityState.NOT_ELIGIBLE,
                        expiresAt = null,
                        policyVersion = POLICY_VERSION,
                        updatedAt = now
                    )
                )
                AgeGateRoute.NotEligible
            }

            AgePredicateResult.UNAVAILABLE,
            AgePredicateResult.INVALID,
            AgePredicateResult.EXPIRED,
            AgePredicateResult.NONCE_MISMATCH -> routeToAccessibleReview(
                accountId,
                now
            )
        }
    }

    /**
     * INV-AGE-06:
     *
     * Review routing is private and neutral. It does not create a public badge,
     * age range, verification tier, discovery effect, or counterpart signal.
     */
    private fun routeToAccessibleReview(
        accountId: AccountId,
        now: Instant
    ): AgeGateRoute {
        grants.save(
            AgeEligibilityGrant(
                accountId = accountId,
                state = AgeEligibilityState.REVIEW_REQUESTED,
                expiresAt = null,
                policyVersion = POLICY_VERSION,
                updatedAt = now
            )
        )
        return AgeGateRoute.AccessibleReview
    }
}

/**
 * INV-AGE-02:
 *
 * A structural allowlist states the only usable proof semantic. The system
 * rejects attempts to introduce passkeys, hardware/device attestation, payment,
 * provider account-age, or provider trust as age inputs.
 */
object AgeProofSemanticPolicy {
    private const val ONLY_ALLOWED_PREDICATE = "age_over_30"

    private val prohibitedAgeProofSources = setOf(
        "passkey",
        "webauthn",
        "hardware_attestation",
        "device_attestation",
        "app_attest",
        "play_integrity",
        "payment_instrument",
        "payment_card",
        "provider_account_age",
        "provider_trust",
        "provider_risk",
        "device_age",
        "device_ownership"
    )

    fun requireAllowed(predicateName: String, proofSources: Set<String>) {
        require(predicateName == ONLY_ALLOWED_PREDICATE) {
            "AgeGate accepts only the age_over_30 predicate."
        }

        val violations = proofSources.intersect(prohibitedAgeProofSources)
        if (violations.isNotEmpty()) {
            throw BoundaryViolation(
                "Prohibited age-proof source: ${violations.sorted().joinToString()}"
            )
        }
    }
}
