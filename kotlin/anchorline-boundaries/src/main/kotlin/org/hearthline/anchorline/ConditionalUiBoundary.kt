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
    val consumedAt: Instant?
)

data class ConditionalUiResponse(
    val challengeId: ChallengeId,
    val challenge: ByteArray,
    val expiresAt: Instant
)

interface ConditionalUiChallengeRepository {
    fun save(value: ConditionalUiChallenge)
    fun consume(challengeId: ChallengeId, at: Instant): ConditionalUiChallenge?
    fun deleteExpired(now: Instant): Int
}

/**
 * The endpoint has no account, username, email, phone, provider, profile, or
 * social input. It returns the same response structure to every caller.
 */
class ConditionalUiChallengeService(
    private val clock: Clock,
    private val random: SecureRandom,
    private val challenges: ConditionalUiChallengeRepository
) {
    companion object {
        private val TTL = Duration.ofMinutes(2)
    }

    fun issue(): ConditionalUiResponse {
        val now = clock.instant()

        val challenge = ConditionalUiChallenge(
            challengeId = ChallengeId(UUID.randomUUID()),
            challenge = ByteArray(32).also(random::nextBytes),
            issuedAt = now,
            expiresAt = now.plus(TTL),
            consumedAt = null
        )

        challenges.save(challenge)

        return ConditionalUiResponse(
            challengeId = challenge.challengeId,
            challenge = challenge.challenge.copyOf(),
            expiresAt = challenge.expiresAt
        )
    }
}

/**
 * WebAuthn conditional UI must omit allowCredentials. The browser/authenticator
 * keeps discoverable-credential availability local until a member selects one.
 */
data class ConditionalWebAuthnRequest(
    val rpId: String,
    val challenge: ByteArray,
    val userVerificationRequired: Boolean,
    val mediation: String,
    val allowCredentials: List<CredentialId>?
) {
    init {
        require(rpId == "auth.hearthline.example")
        require(mediation == "conditional")
        require(userVerificationRequired)
        require(allowCredentials == null) {
            "Conditional WebAuthn must omit allowCredentials."
        }
    }
}

object ConditionalUiPublicErrors {
    const val GENERIC_SIGN_IN_FAILURE =
        "We could not complete that sign-in. Try again or choose another sign-in method."
}
