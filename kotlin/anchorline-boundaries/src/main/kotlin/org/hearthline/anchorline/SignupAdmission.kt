package org.hearthline.anchorline

import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class SignupCapacityMode {
    OPEN,
    ELEVATED_LOAD,
    QUEUED,
    PAUSED
}

data class AnonymousSignupChallenge(
    val challengeId: ChallengeId,
    val challenge: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val proofOfWorkDifficulty: Int,
    val consumedAt: Instant?
)

@JvmInline
value class SignupTicketId(val value: UUID)

data class AnonymousSignupTicket(
    val ticketId: SignupTicketId,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val passkeyRegistrationUsesRemaining: Int,
    val ageProofUsesRemaining: Int,
    val accountShellUsesRemaining: Int
)

sealed interface SignupAdmissionRoute {
    data class Challenge(
        val challengeId: ChallengeId,
        val challenge: ByteArray,
        val expiresAt: Instant,
        val proofOfWorkDifficulty: Int
    ) : SignupAdmissionRoute

    data class Queue(
        val retryAfterSeconds: Long
    ) : SignupAdmissionRoute

    data class Unavailable(
        val retryAfterSeconds: Long
    ) : SignupAdmissionRoute

    data class AccessibleAlternative(
        val ticket: AnonymousSignupTicket
    ) : SignupAdmissionRoute
}

interface GlobalSignupCapacity {
    fun currentMode(): SignupCapacityMode
    fun retryAfterSeconds(): Long
    fun globalProofOfWorkDifficulty(): Int
}

interface AnonymousSignupChallengeRepository {
    fun save(value: AnonymousSignupChallenge)
    fun consume(challengeId: ChallengeId, at: Instant): AnonymousSignupChallenge?
    fun deleteExpired(now: Instant): Int
}

interface AnonymousSignupTicketRepository {
    fun save(value: AnonymousSignupTicket)
    fun consumePasskeyRegistration(ticketId: SignupTicketId, at: Instant): Boolean
    fun consumeAgeProof(ticketId: SignupTicketId, at: Instant): Boolean
    fun consumeAccountShell(ticketId: SignupTicketId, at: Instant): Boolean
    fun deleteExpired(now: Instant): Int
}

class AnonymousSignupAdmissionService(
    private val clock: Clock,
    private val random: SecureRandom,
    private val capacity: GlobalSignupCapacity,
    private val challenges: AnonymousSignupChallengeRepository,
    private val tickets: AnonymousSignupTicketRepository
) {
    companion object {
        private val CHALLENGE_TTL = Duration.ofMinutes(3)
        private val TICKET_TTL = Duration.ofMinutes(10)
    }

    fun begin(): SignupAdmissionRoute {
        val now = clock.instant()

        return when (capacity.currentMode()) {
            SignupCapacityMode.OPEN -> issueChallenge(now, 0)

            SignupCapacityMode.ELEVATED_LOAD -> issueChallenge(
                now,
                capacity.globalProofOfWorkDifficulty()
            )

            SignupCapacityMode.QUEUED ->
                SignupAdmissionRoute.Queue(capacity.retryAfterSeconds())

            SignupCapacityMode.PAUSED ->
                SignupAdmissionRoute.Unavailable(capacity.retryAfterSeconds())
        }
    }

    /**
     * This route is equal in account capabilities and social participation.
     * It does not expose why an optional admission signal was skipped.
     */
    fun accessibleAlternative(): SignupAdmissionRoute.AccessibleAlternative {
        val now = clock.instant()
        val ticket = ticket(now)
        tickets.save(ticket)
        return SignupAdmissionRoute.AccessibleAlternative(ticket)
    }

    private fun issueChallenge(
        now: Instant,
        difficulty: Int
    ): SignupAdmissionRoute.Challenge {
        val challenge = AnonymousSignupChallenge(
            challengeId = ChallengeId(UUID.randomUUID()),
            challenge = ByteArray(32).also(random::nextBytes),
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL),
            proofOfWorkDifficulty = difficulty,
            consumedAt = null
        )

        challenges.save(challenge)

        return SignupAdmissionRoute.Challenge(
            challengeId = challenge.challengeId,
            challenge = challenge.challenge.copyOf(),
            expiresAt = challenge.expiresAt,
            proofOfWorkDifficulty = difficulty
        )
    }

    private fun ticket(now: Instant): AnonymousSignupTicket =
        AnonymousSignupTicket(
            ticketId = SignupTicketId(UUID.randomUUID()),
            issuedAt = now,
            expiresAt = now.plus(TICKET_TTL),
            passkeyRegistrationUsesRemaining = 1,
            ageProofUsesRemaining = 1,
            accountShellUsesRemaining = 1
        )
}
