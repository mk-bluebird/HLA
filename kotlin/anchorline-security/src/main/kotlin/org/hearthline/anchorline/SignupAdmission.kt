package org.hearthline.anchorline

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class SignupChallenge(
    val challengeId: ChallengeId,
    val challengeBytes: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val globalProofOfWorkDifficulty: Int,
    val consumedAt: Instant? = null
)

data class SignupTicket(
    val ticketId: SignupTicketId,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val passkeyRegistrationRemaining: Int,
    val ageProofRemaining: Int,
    val accountShellRemaining: Int
)

data class ProofOfWorkSolution(
    val nonce: Long
)

sealed interface SignupAdmissionResult {
    data class ChallengeIssued(
        val challengeId: ChallengeId,
        val challengeBase64Url: String,
        val expiresAt: Instant,
        val requiredDifficulty: Int
    ) : SignupAdmissionResult

    data class Queued(
        val retryAfterSeconds: Long
    ) : SignupAdmissionResult

    data class TemporarilyUnavailable(
        val retryAfterSeconds: Long
    ) : SignupAdmissionResult

    data class TicketIssued(
        val ticket: SignupTicket
    ) : SignupAdmissionResult

    data class AccessibleAlternativeAvailable(
        val message: String
    ) : SignupAdmissionResult

    data class Rejected(
        val message: String
    ) : SignupAdmissionResult
}

interface SignupChallengeRepository {
    fun save(challenge: SignupChallenge)
    fun consume(challengeId: ChallengeId, at: Instant): SignupChallenge?
    fun deleteExpired(now: Instant): Int
}

interface SignupTicketRepository {
    fun save(ticket: SignupTicket)
    fun deleteExpired(now: Instant): Int

    /**
     * The three capabilities must be atomically decremented. Every false result
     * means the capability is absent, expired, or already consumed.
     */
    fun consumePasskeyRegistration(ticketId: SignupTicketId, at: Instant): Boolean
    fun consumeAgeProof(ticketId: SignupTicketId, at: Instant): Boolean
    fun consumeAccountShell(ticketId: SignupTicketId, at: Instant): Boolean
}

interface GlobalSignupCapacity {
    fun currentMode(): SignupCapacityMode
    fun retryAfterSeconds(): Long

    /**
     * Global-only value. It must be derived from aggregate service load, not
     * applicant, account, device, platform, IP, geography, or behavior.
     */
    fun proofOfWorkDifficulty(): Int
}

interface ProofOfWorkVerifier {
    fun valid(
        challenge: ByteArray,
        solution: ProofOfWorkSolution,
        requiredDifficulty: Int
    ): Boolean
}

class Sha256ProofOfWorkVerifier : ProofOfWorkVerifier {
    override fun valid(
        challenge: ByteArray,
        solution: ProofOfWorkSolution,
        requiredDifficulty: Int
    ): Boolean {
        require(requiredDifficulty >= 0)

        val nonce = ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(solution.nonce)
            .array()

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(challenge + nonce)

        return countLeadingZeroBits(digest) >= requiredDifficulty
    }

    private fun countLeadingZeroBits(bytes: ByteArray): Int {
        var count = 0

        for (value in bytes) {
            val unsigned = value.toInt() and 0xff

            if (unsigned == 0) {
                count += 8
                continue
            }

            count += Integer.numberOfLeadingZeros(unsigned) - 24
            break
        }

        return count
    }
}

class PrivacyPreservingSignupAdmissionService(
    private val clock: Clock,
    private val capacity: GlobalSignupCapacity,
    private val challenges: SignupChallengeRepository,
    private val tickets: SignupTicketRepository,
    private val proofOfWork: ProofOfWorkVerifier
) {
    companion object {
        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(3)
        private val TICKET_TTL: Duration = Duration.ofMinutes(10)
    }

    /**
     * GATE-SIGNUP-01:
     *
     * This method has no applicant/device/network argument. The only state
     * created is anonymous, short-lived challenge state.
     */
    fun begin(): SignupAdmissionResult {
        val now = clock.instant()

        return when (capacity.currentMode()) {
            SignupCapacityMode.OPEN -> issueChallenge(now, 0)
            SignupCapacityMode.ELEVATED_LOAD ->
                issueChallenge(now, capacity.proofOfWorkDifficulty())

            SignupCapacityMode.QUEUED ->
                SignupAdmissionResult.Queued(capacity.retryAfterSeconds())

            SignupCapacityMode.PAUSED ->
                SignupAdmissionResult.TemporarilyUnavailable(
                    capacity.retryAfterSeconds()
                )
        }
    }

    fun exchange(
        challengeId: ChallengeId,
        proof: ProofOfWorkSolution?
    ): SignupAdmissionResult {
        val now = clock.instant()
        val challenge = challenges.consume(challengeId, now)
            ?: return SignupAdmissionResult.Rejected(
                "This registration step is no longer available. Start again."
            )

        if (now >= challenge.expiresAt) {
            return SignupAdmissionResult.Rejected(
                "This registration step expired. Start again."
            )
        }

        if (challenge.globalProofOfWorkDifficulty > 0) {
            if (proof == null) {
                return SignupAdmissionResult.AccessibleAlternativeAvailable(
                    "An accessible registration alternative is available."
                )
            }

            if (!proofOfWork.valid(
                    challenge.challengeBytes,
                    proof,
                    challenge.globalProofOfWorkDifficulty
                )
            ) {
                return SignupAdmissionResult.Rejected(
                    "This registration step could not be completed. Start again."
                )
            }
        }

        return SignupAdmissionResult.TicketIssued(
            newTicket(now).also(tickets::save)
        )
    }

    /**
     * GATE-SIGNUP-04:
     *
     * This path bypasses optional computational friction. It does not require
     * camera, audio, biometrics, device attestation, location, payment, or
     * identity documents.
     */
    fun issueAccessibleAlternativeTicket(): SignupTicket {
        val now = clock.instant()
        return newTicket(now).also(tickets::save)
    }

    private fun issueChallenge(
        now: Instant,
        difficulty: Int
    ): SignupAdmissionResult.ChallengeIssued {
        val challenge = SignupChallenge(
            challengeId = ChallengeId(UUID.randomUUID()),
            challengeBytes = CryptoSupport.randomBytes(32),
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL),
            globalProofOfWorkDifficulty = difficulty
        )

        challenges.save(challenge)

        return SignupAdmissionResult.ChallengeIssued(
            challengeId = challenge.challengeId,
            challengeBase64Url = CryptoSupport.base64Url(challenge.challengeBytes),
            expiresAt = challenge.expiresAt,
            requiredDifficulty = difficulty
        )
    }

    private fun newTicket(now: Instant): SignupTicket =
        SignupTicket(
            ticketId = SignupTicketId(UUID.randomUUID()),
            issuedAt = now,
            expiresAt = now.plus(TICKET_TTL),
            passkeyRegistrationRemaining = 1,
            ageProofRemaining = 1,
            accountShellRemaining = 1
        )
}

/**
 * GATE-SIGNUP-05:
 *
 * Signup pacing may limit high-amplification registration operations, but
 * safety/exit controls bypass signup pacing completely.
 */
enum class AlwaysAvailableSafetyAction {
    BLOCK,
    REPORT,
    WITHDRAW_CONSENT,
    DELETE_ACCOUNT,
    CORRECT_PROFILE,
    APPEAL
}

object SignupPacingBoundary {
    fun requireAlwaysAvailable(action: AlwaysAvailableSafetyAction): Boolean = true
}
