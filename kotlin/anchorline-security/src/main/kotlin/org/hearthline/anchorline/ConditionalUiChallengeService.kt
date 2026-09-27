package org.hearthline.anchorline

import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class ConditionalUiChallenge(
    val challengeId: ChallengeId,
    val challenge: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant? = null
)

data class ConditionalUiChallengeResponse(
    val challengeId: ChallengeId,
    val challengeBase64Url: String,
    val expiresAt: Instant
)

interface ConditionalUiChallengeRepository {
    fun save(challenge: ConditionalUiChallenge)

    /**
     * Atomically marks the challenge consumed. A replay returns null.
     */
    fun consume(
        challengeId: ChallengeId,
        at: Instant
    ): ConditionalUiChallenge?

    fun deleteExpired(now: Instant): Int
}

/**
 * This service has no account lookup or identifier input. It cannot expose an
 * account-existence oracle through its response shape, timing, or fields.
 */
class ConditionalUiChallengeService(
    private val clock: Clock,
    private val random: SecureRandom,
    private val challenges: ConditionalUiChallengeRepository
) {
    companion object {
        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
    }

    fun issue(): ConditionalUiChallengeResponse {
        val now = clock.instant()

        val challenge = ConditionalUiChallenge(
            challengeId = ChallengeId(UUID.randomUUID()),
            challenge = ByteArray(32).also(random::nextBytes),
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL)
        )

        challenges.save(challenge)

        return ConditionalUiChallengeResponse(
            challengeId = challenge.challengeId,
            challengeBase64Url = CryptoSupport.base64Url(challenge.challenge),
            expiresAt = challenge.expiresAt
        )
    }
}
