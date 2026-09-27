package org.hearthline.signup

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Privacy-preserving signup admission control.
 *
 * Explicitly excluded from this implementation:
 *
 * - IP address and IP hash
 * - user-agent value and user-agent hash
 * - browser/device fingerprint
 * - platform or hardware classification
 * - location, ASN, region, or network reputation
 * - behavioral timing score
 * - account or member risk score
 * - cross-account linkage
 *
 * Controls are bound only to single-use, short-lived anonymous challenges and
 * aggregate service capacity. Expired records are purged and are never attached
 * to a member profile.
 */
class PrivacyPreservingSignupGuard(
    private val clock: Clock,
    private val random: SecureRandom = SecureRandom(),
    private val challengeStore: SignupChallengeStore,
    private val ticketStore: SignupAdmissionTicketStore,
    private val capacity: GlobalSignupCapacity,
    private val proofOfWork: ProofOfWorkVerifier
) {
    companion object {
        private val CHALLENGE_TTL = Duration.ofMinutes(3)
        private val TICKET_TTL = Duration.ofMinutes(10)
    }

    fun beginRegistration(): SignupAdmission {
        val now = clock.instant()

        return when (capacity.currentMode()) {
            SignupCapacityMode.OPEN -> issueChallenge(
                now = now,
                requiredDifficulty = 0
            )

            SignupCapacityMode.ELEVATED_LOAD -> issueChallenge(
                now = now,
                requiredDifficulty = capacity.globalProofOfWorkDifficulty()
            )

            SignupCapacityMode.QUEUED -> SignupAdmission.Queued(
                retryAfterSeconds = capacity.retryAfterSeconds()
            )

            SignupCapacityMode.PAUSED -> SignupAdmission.Unavailable(
                retryAfterSeconds = capacity.retryAfterSeconds()
            )
        }
    }

    fun exchangeChallengeForTicket(
        challengeId: UUID,
        proof: ProofOfWorkSolution?
    ): SignupTicketExchange {
        val now = clock.instant()

        val challenge = challengeStore.consume(challengeId, now)
            ?: return SignupTicketExchange.Rejected(
                publicMessage = "This registration step is no longer available. Start again."
            )

        if (now >= challenge.expiresAt) {
            return SignupTicketExchange.Rejected(
                publicMessage = "This registration step expired. Start again."
            )
        }

        if (challenge.requiredDifficulty > 0) {
            val suppliedProof = proof ?: return SignupTicketExchange.AccessibleAlternative(
                publicMessage =
                    "This registration step needs an additional check. " +
                        "You can use the accessible alternative instead."
            )

            if (!proofOfWork.isValid(
                    challengeBytes = challenge.challengeBytes,
                    nonce = suppliedProof.nonce,
                    requiredDifficulty = challenge.requiredDifficulty
                )
            ) {
                return SignupTicketExchange.Rejected(
                    publicMessage = "This registration step could not be completed. Start again."
                )
            }
        }

        val ticket = SignupAdmissionTicket(
            ticketId = UUID.randomUUID(),
            issuedAt = now,
            expiresAt = now.plus(TICKET_TTL),
            passkeyRegistrationRemaining = 1,
            agePredicateSubmissionRemaining = 1,
            accountShellCreationRemaining = 1
        )

        ticketStore.save(ticket)

        return SignupTicketExchange.Allowed(ticket)
    }

    /**
     * An accessible alternative is not a member risk state. It simply gives the
     * applicant a route that avoids computational work, CAPTCHA-like tasks, camera,
     * audio, or biometric requirements.
     */
    fun issueAccessibleAlternativeTicket(): SignupAdmissionTicket {
        val now = clock.instant()

        return SignupAdmissionTicket(
            ticketId = UUID.randomUUID(),
            issuedAt = now,
            expiresAt = now.plus(TICKET_TTL),
            passkeyRegistrationRemaining = 1,
            agePredicateSubmissionRemaining = 1,
            accountShellCreationRemaining = 1
        ).also(ticketStore::save)
    }

    private fun issueChallenge(
        now: Instant,
        requiredDifficulty: Int
    ): SignupAdmission.Challenge {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)

        val challenge = SignupChallenge(
            challengeId = UUID.randomUUID(),
            challengeBytes = bytes,
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL),
            requiredDifficulty = requiredDifficulty,
            consumedAt = null
        )

        challengeStore.save(challenge)

        return SignupAdmission.Challenge(
            challengeId = challenge.challengeId,
            challengeBase64Url = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(challenge.challengeBytes),
            expiresAt = challenge.expiresAt,
            requiredProofOfWorkDifficulty = requiredDifficulty
        )
    }
}

sealed interface SignupAdmission {
    data class Challenge(
        val challengeId: UUID,
        val challengeBase64Url: String,
        val expiresAt: Instant,
        val requiredProofOfWorkDifficulty: Int
    ) : SignupAdmission

    data class Queued(
        val retryAfterSeconds: Long
    ) : SignupAdmission

    data class Unavailable(
        val retryAfterSeconds: Long
    ) : SignupAdmission
}

data class SignupChallenge(
    val challengeId: UUID,
    val challengeBytes: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val requiredDifficulty: Int,
    val consumedAt: Instant?
)

data class SignupAdmissionTicket(
    val ticketId: UUID,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val passkeyRegistrationRemaining: Int,
    val agePredicateSubmissionRemaining: Int,
    val accountShellCreationRemaining: Int
)

data class ProofOfWorkSolution(
    val nonce: Long
)

sealed interface SignupTicketExchange {
    data class Allowed(
        val ticket: SignupAdmissionTicket
    ) : SignupTicketExchange

    data class AccessibleAlternative(
        val publicMessage: String
    ) : SignupTicketExchange

    data class Rejected(
        val publicMessage: String
    ) : SignupTicketExchange
}

interface SignupChallengeStore {
    fun save(challenge: SignupChallenge)
    fun consume(challengeId: UUID, consumedAt: Instant): SignupChallenge?
    fun deleteExpired(now: Instant): Int
}

interface SignupAdmissionTicketStore {
    fun save(ticket: SignupAdmissionTicket)
    fun consumePasskeyRegistration(ticketId: UUID, consumedAt: Instant): Boolean
    fun consumeAgePredicateSubmission(ticketId: UUID, consumedAt: Instant): Boolean
    fun consumeAccountShellCreation(ticketId: UUID, consumedAt: Instant): Boolean
    fun deleteExpired(now: Instant): Int
}

interface GlobalSignupCapacity {
    fun currentMode(): SignupCapacityMode
    fun globalProofOfWorkDifficulty(): Int
    fun retryAfterSeconds(): Long
}

enum class SignupCapacityMode {
    OPEN,
    ELEVATED_LOAD,
    QUEUED,
    PAUSED
}

interface ProofOfWorkVerifier {
    fun isValid(
        challengeBytes: ByteArray,
        nonce: Long,
        requiredDifficulty: Int
    ): Boolean
}

/**
 * A reference SHA-256 leading-zero-bit proof-of-work verifier.
 *
 * It retains no solve time, device characteristics, or per-applicant history.
 */
class Sha256LeadingZeroProofOfWorkVerifier : ProofOfWorkVerifier {
    override fun isValid(
        challengeBytes: ByteArray,
        nonce: Long,
        requiredDifficulty: Int
    ): Boolean {
        require(requiredDifficulty >= 0) {
            "Difficulty cannot be negative."
        }

        val nonceBytes = ByteBuffer.allocate(Long.SIZE_BYTES)
            .putLong(nonce)
            .array()

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(challengeBytes + nonceBytes)

        return leadingZeroBits(digest) >= requiredDifficulty
    }

    private fun leadingZeroBits(bytes: ByteArray): Int {
        var result = 0

        for (byte in bytes) {
            val unsigned = byte.toInt() and 0xff

            if (unsigned == 0) {
                result += 8
                continue
            }

            result += Integer.numberOfLeadingZeros(unsigned) - 24
            break
        }

        return result
    }
}

class InMemorySignupChallengeStore : SignupChallengeStore {
    private val values = ConcurrentHashMap<UUID, SignupChallenge>()

    override fun save(challenge: SignupChallenge) {
        values[challenge.challengeId] = challenge
    }

    override fun consume(
        challengeId: UUID,
        consumedAt: Instant
    ): SignupChallenge? {
        var result: SignupChallenge? = null

        values.computeIfPresent(challengeId) { _, current ->
            if (current.consumedAt == null) {
                current.copy(consumedAt = consumedAt).also { result = it }
            } else {
                current
            }
        }

        return result
    }

    override fun deleteExpired(now: Instant): Int {
        val expired = values.values
            .filter { now >= it.expiresAt }
            .map { it.challengeId }

        expired.forEach(values::remove)
        return expired.size
    }
}
