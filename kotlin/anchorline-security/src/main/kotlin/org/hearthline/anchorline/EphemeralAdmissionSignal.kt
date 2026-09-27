package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class AdmissionSignalChallengeId(val value: UUID)

enum class AdmissionSignalKind {
    PRIVATE_ACCESS_TOKEN,
    RECAPTCHA_ENTERPRISE,
    AZURE_WAF_CHALLENGE,
    PROOF_OF_WORK,
    ACCESSIBLE_ALTERNATIVE
}

enum class AdmissionSignalDecision {
    CONTINUE_CURRENT_REQUEST,
    ACCESSIBLE_ALTERNATIVE
}

data class AdmissionSignalChallenge(
    val challengeId: AdmissionSignalChallengeId,
    val purpose: AdmissionSignalPurpose,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant?
)

enum class AdmissionSignalPurpose {
    SIGNUP_ADMISSION,
    HIGH_COST_REGISTRATION_ACTION
}

@JvmInline
value class OpaqueAdmissionSignal(val bytes: ByteArray)

interface AdmissionSignalChallengeRepository {
    fun save(value: AdmissionSignalChallenge)

    /**
     * Must atomically consume a challenge once.
     */
    fun consume(
        challengeId: AdmissionSignalChallengeId,
        at: Instant
    ): AdmissionSignalChallenge?

    fun deleteExpired(now: Instant): Int
}

/**
 * Provider-specific verification must return only a current-request decision.
 * It cannot return risk scores, device metadata, user identifiers, reason
 * codes, account history, or persistent classifications.
 */
interface EphemeralAdmissionSignalVerifier {
    fun verify(
        kind: AdmissionSignalKind,
        challenge: AdmissionSignalChallenge,
        signal: OpaqueAdmissionSignal
    ): AdmissionSignalDecision
}

data class AdmissionSignalReceipt(
    val challengeId: AdmissionSignalChallengeId,
    val purpose: AdmissionSignalPurpose,
    val decision: AdmissionSignalDecision,
    val expiresAt: Instant
)

interface AdmissionSignalReceiptRepository {
    fun save(receipt: AdmissionSignalReceipt)
    fun deleteExpired(now: Instant): Int
}

class EphemeralAdmissionSignalService(
    private val clock: Clock,
    private val challenges: AdmissionSignalChallengeRepository,
    private val verifier: EphemeralAdmissionSignalVerifier,
    private val receipts: AdmissionSignalReceiptRepository
) {
    companion object {
        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
        private val RECEIPT_TTL: Duration = Duration.ofMinutes(10)
    }

    fun issue(
        purpose: AdmissionSignalPurpose
    ): AdmissionSignalChallenge {
        val now = clock.instant()

        return AdmissionSignalChallenge(
            challengeId = AdmissionSignalChallengeId(UUID.randomUUID()),
            purpose = purpose,
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL),
            consumedAt = null
        ).also(challenges::save)
    }

    fun verify(
        kind: AdmissionSignalKind,
        challengeId: AdmissionSignalChallengeId,
        signal: OpaqueAdmissionSignal?
    ): AdmissionSignalDecision {
        val now = clock.instant()

        val challenge = challenges.consume(challengeId, now)
            ?: return AdmissionSignalDecision.ACCESSIBLE_ALTERNATIVE

        if (now >= challenge.expiresAt) {
            return AdmissionSignalDecision.ACCESSIBLE_ALTERNATIVE
        }

        val decision = when {
            kind == AdmissionSignalKind.ACCESSIBLE_ALTERNATIVE ->
                AdmissionSignalDecision.ACCESSIBLE_ALTERNATIVE

            signal == null ->
                AdmissionSignalDecision.ACCESSIBLE_ALTERNATIVE

            else -> verifier.verify(kind, challenge, signal)
        }

        receipts.save(
            AdmissionSignalReceipt(
                challengeId = challenge.challengeId,
                purpose = challenge.purpose,
                decision = decision,
                expiresAt = now.plus(RECEIPT_TTL)
            )
        )

        return decision
    }
}
