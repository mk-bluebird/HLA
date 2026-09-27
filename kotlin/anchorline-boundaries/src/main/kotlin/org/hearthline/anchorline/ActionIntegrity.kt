package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class IntegrityChallengeId(val value: UUID)

enum class IntegrityPurpose {
    SIGNUP_ADMISSION,
    HIGH_COST_REGISTRATION_ACTION
}

enum class IntegrityDecision {
    CONTINUE_CURRENT_REQUEST,
    ACCESSIBLE_ALTERNATIVE
}

data class IntegrityChallenge(
    val challengeId: IntegrityChallengeId,
    val purpose: IntegrityPurpose,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant?
)

@JvmInline
value class OpaqueIntegritySignal private constructor(
    private val bytes: ByteArray
) {
    fun copyBytes(): ByteArray = bytes.copyOf()

    companion object {
        fun of(value: ByteArray): OpaqueIntegritySignal {
            require(value.isNotEmpty())
            require(value.size <= 256 * 1024)
            return OpaqueIntegritySignal(value.copyOf())
        }
    }
}

interface IntegrityChallengeRepository {
    fun save(value: IntegrityChallenge)
    fun consume(challengeId: IntegrityChallengeId, at: Instant): IntegrityChallenge?
    fun deleteExpired(now: Instant): Int
}

interface ActionIntegrityVerifier {
    /**
     * May return only an immediate request decision.
     *
     * It must not return risk scores, reasons, device data, provider metadata,
     * account history, cookies, client labels, or member classifications.
     */
    fun verify(
        challenge: IntegrityChallenge,
        signal: OpaqueIntegritySignal
    ): IntegrityDecision
}

class ActionIntegrityService(
    private val clock: Clock,
    private val challenges: IntegrityChallengeRepository,
    private val verifier: ActionIntegrityVerifier
) {
    companion object {
        private val TTL = Duration.ofMinutes(5)
    }

    fun issue(purpose: IntegrityPurpose): IntegrityChallenge {
        val now = clock.instant()

        return IntegrityChallenge(
            challengeId = IntegrityChallengeId(UUID.randomUUID()),
            purpose = purpose,
            issuedAt = now,
            expiresAt = now.plus(TTL),
            consumedAt = null
        ).also(challenges::save)
    }

    fun verify(
        challengeId: IntegrityChallengeId,
        signal: OpaqueIntegritySignal?
    ): IntegrityDecision {
        val now = clock.instant()
        val challenge = challenges.consume(challengeId, now)
            ?: return IntegrityDecision.ACCESSIBLE_ALTERNATIVE

        if (!now.isBefore(challenge.expiresAt) || signal == null) {
            return IntegrityDecision.ACCESSIBLE_ALTERNATIVE
        }

        return verifier.verify(challenge, signal)
    }
}
