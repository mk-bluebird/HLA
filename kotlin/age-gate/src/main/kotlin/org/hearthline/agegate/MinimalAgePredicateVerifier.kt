package org.hearthline.agegate

import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A provider-agnostic reference design.
 *
 * The external issuer's proof must verify only:
 *
 *   age_over_30 == true
 *
 * It must be bound to:
 *
 *   - Hearthline's AgeGate audience
 *   - a fresh, one-time challenge
 *   - a short expiry
 *
 * It must not carry an issuer subject identifier, DOB, name, address,
 * document image, credential ID, payment information, or device metadata.
 */
class MinimalAgePredicateVerifier(
    private val clock: Clock,
    private val proofVerifier: ThirdPartyAgeProofVerifier,
    private val grants: AgeEligibilityGrantRepository,
    private val challenges: AgeEligibilityChallengeRepository
) {
    companion object {
        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
        private val ELIGIBILITY_GRANT_TTL: Duration = Duration.ofDays(365)

        const val POLICY_VERSION = "age-gate-30-plus-v1"
        const val AUDIENCE = "https://agegate.hearthline.example"
    }

    fun start(accountId: AccountId): AgeEligibilityChallenge {
        val now = clock.instant()

        return AgeEligibilityChallenge(
            challengeId = UUID.randomUUID(),
            accountId = accountId,
            nonce = secureNonce(),
            audience = AUDIENCE,
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL),
            consumedAt = null
        ).also(challenges::save)
    }

    fun verify(
        accountId: AccountId,
        challengeId: UUID,
        proof: OpaqueAgePredicatePresentation
    ): AgeGateDecision {
        val now = clock.instant()

        val challenge = challenges.consume(challengeId, now)
            ?: return routeToReview(accountId, now)

        if (challenge.accountId != accountId || now >= challenge.expiresAt) {
            return routeToReview(accountId, now)
        }

        /*
         * The verifier is expected to check cryptographic validity, audience,
         * one-time nonce binding, issuer trust configuration, expiry, and
         * revocation/status where the selected proof system supports it.
         *
         * It returns only the minimal predicate result. It must not surface
         * subject IDs, birth dates, issuer accounts, names, documents, or
         * reusable credential identifiers to this service.
         */
        val result = proofVerifier.verify(
            expectedAudience = challenge.audience,
            expectedNonce = challenge.nonce,
            proof = proof
        )

        return when (result) {
            MinimalAgeProofResult.AGE_OVER_30 -> {
                grants.save(
                    AgeEligibilityGrant(
                        accountId = accountId,
                        state = AgeEligibilityState.ELIGIBLE,
                        policyVersion = POLICY_VERSION,
                        issuedAt = now,
                        expiresAt = now.plus(ELIGIBILITY_GRANT_TTL),
                        updatedAt = now
                    )
                )
                AgeGateDecision.ELIGIBLE
            }

            MinimalAgeProofResult.NOT_AGE_OVER_30 -> {
                grants.save(
                    AgeEligibilityGrant(
                        accountId = accountId,
                        state = AgeEligibilityState.NOT_ELIGIBLE,
                        policyVersion = POLICY_VERSION,
                        issuedAt = now,
                        expiresAt = null,
                        updatedAt = now
                    )
                )
                AgeGateDecision.NOT_ELIGIBLE
            }

            MinimalAgeProofResult.UNAVAILABLE,
            MinimalAgeProofResult.INVALID,
            MinimalAgeProofResult.EXPIRED,
            MinimalAgeProofResult.NONCE_MISMATCH -> routeToReview(accountId, now)
        }
    }

    private fun routeToReview(
        accountId: AccountId,
        now: Instant
    ): AgeGateDecision {
        grants.save(
            AgeEligibilityGrant(
                accountId = accountId,
                state = AgeEligibilityState.REVIEW_REQUESTED,
                policyVersion = POLICY_VERSION,
                issuedAt = null,
                expiresAt = null,
                updatedAt = now
            )
        )

        return AgeGateDecision.ROUTE_TO_ACCESSIBLE_REVIEW
    }

    private fun secureNonce(): ByteArray {
        val random = UUID.randomUUID().toString().toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(random)
    }
}

data class AgeEligibilityChallenge(
    val challengeId: UUID,
    val accountId: AccountId,
    val nonce: ByteArray,
    val audience: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant?
)

@JvmInline
value class OpaqueAgePredicatePresentation(val bytes: ByteArray)

enum class MinimalAgeProofResult {
    AGE_OVER_30,
    NOT_AGE_OVER_30,
    UNAVAILABLE,
    INVALID,
    EXPIRED,
    NONCE_MISMATCH
}

interface ThirdPartyAgeProofVerifier {
    fun verify(
        expectedAudience: String,
        expectedNonce: ByteArray,
        proof: OpaqueAgePredicatePresentation
    ): MinimalAgeProofResult
}

interface AgeEligibilityGrantRepository {
    fun save(grant: AgeEligibilityGrant)
    fun find(accountId: AccountId): AgeEligibilityGrant?
}

interface AgeEligibilityChallengeRepository {
    fun save(challenge: AgeEligibilityChallenge)
    fun consume(challengeId: UUID, consumedAt: Instant): AgeEligibilityChallenge?
}

class InMemoryAgeEligibilityGrantRepository : AgeEligibilityGrantRepository {
    private val values = ConcurrentHashMap<AccountId, AgeEligibilityGrant>()

    override fun save(grant: AgeEligibilityGrant) {
        values[grant.accountId] = grant
    }

    override fun find(accountId: AccountId): AgeEligibilityGrant? = values[accountId]
}
