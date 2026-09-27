package org.hearthline.anchorline

import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

enum class RecoveryAuthorizationMethod {
    SECOND_REGISTERED_PASSKEY,
    INDEPENDENT_SECURITY_KEY,
    OFFLINE_RECOVERY_CODE,
    PREESTABLISHED_FEDERATED_BINDING,
    GOVERNED_EXCEPTIONAL_PROCESS
}

data class RecoveryCase(
    val recoveryId: RecoveryId,
    val accountId: AccountId,
    val state: RecoveryState,
    val createdAt: Instant,
    val expiresAt: Instant,
    val authorizedAt: Instant?,
    val holdEndsAt: Instant?,
    val authorizationMethod: RecoveryAuthorizationMethod?,
    val completedAt: Instant?,
    val cancelledAt: Instant?
)

data class RecoveryChallenge(
    val recoveryId: RecoveryId,
    val challenge: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant? = null
)

/**
 * REC-03:
 *
 * Store an expensive verifier, never a plaintext recovery code. The actual
 * implementation can use Argon2id/bcrypt/scrypt via an approved library;
 * this interface keeps that cryptographic detail injectable and reviewable.
 */
data class RecoveryCodeVerifier(
    val accountId: AccountId,
    val verifier: ByteArray,
    val salt: ByteArray,
    val createdAt: Instant,
    val usedAt: Instant?,
    val revokedAt: Instant?
)

interface RecoveryCodeHasher {
    fun hash(code: CharArray, salt: ByteArray): ByteArray
    fun verify(code: CharArray, salt: ByteArray, verifier: ByteArray): Boolean
}

interface RecoveryCodeRepository {
    fun save(verifier: RecoveryCodeVerifier)
    fun findActive(accountId: AccountId): List<RecoveryCodeVerifier>

    /**
     * Atomically marks one code used and revokes all unused recovery codes
     * for the account. Returns false on invalid/replayed/revoked code.
     */
    fun consumeOneAndRevokeRest(
        accountId: AccountId,
        code: CharArray,
        hasher: RecoveryCodeHasher,
        at: Instant
    ): Boolean

    fun revokeAll(accountId: AccountId, at: Instant): Int
}

interface RecoveryCaseRepository {
    fun save(value: RecoveryCase)
    fun find(recoveryId: RecoveryId): RecoveryCase?
    fun update(
        recoveryId: RecoveryId,
        transform: (RecoveryCase) -> RecoveryCase
    ): RecoveryCase?

    fun deleteExpired(now: Instant): Int
}

interface RecoveryChallengeRepository {
    fun save(value: RecoveryChallenge)
    fun consume(recoveryId: RecoveryId, at: Instant): RecoveryChallenge?
    fun deleteExpired(now: Instant): Int
}

interface IndependentRecoveryProofVerifier {
    fun verifyIndependentRegisteredPasskey(
        accountId: AccountId,
        challenge: ByteArray,
        assertion: ByteArray
    ): Boolean

    fun verifyPreestablishedFederatedBinding(
        accountId: AccountId,
        authorizationCode: String,
        pkceVerifier: String
    ): Boolean

    /**
     * AgeGate results, email links, SMS codes, profile facts, messages, photos,
     * contacts, location, staff recognition, and device/network properties are
     * intentionally absent from this interface.
     */
}

interface RecoverySessionInvalidator {
    fun revokeAllSessions(accountId: AccountId, at: Instant)
}

interface RecoveryNotifier {
    /**
     * Notice only. This cannot contain a token that can complete recovery.
     */
    fun recoveryStarted(accountId: AccountId, recoveryId: RecoveryId)
    fun recoveryAuthorized(accountId: AccountId, recoveryId: RecoveryId)
    fun recoveryCompleted(accountId: AccountId, recoveryId: RecoveryId)
}

sealed interface RecoveryAttempt {
    data class IndependentPasskey(
        val assertion: ByteArray
    ) : RecoveryAttempt

    data class OfflineCode(
        val code: CharArray
    ) : RecoveryAttempt

    data class PreestablishedFederatedBinding(
        val authorizationCode: String,
        val pkceVerifier: String
    ) : RecoveryAttempt
}

class RecoveryService(
    private val clock: Clock,
    private val random: SecureRandom,
    private val recoveryCases: RecoveryCaseRepository,
    private val challenges: RecoveryChallengeRepository,
    private val recoveryCodes: RecoveryCodeRepository,
    private val proofVerifier: IndependentRecoveryProofVerifier,
    private val sessions: RecoverySessionInvalidator,
    private val notifier: RecoveryNotifier
) {
    companion object {
        private val RECOVERY_CASE_TTL: Duration = Duration.ofHours(24)
        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
        private val RECOVERY_HOLD: Duration = Duration.ofHours(24)
    }

    fun begin(accountId: AccountId): RecoveryChallenge {
        val now = clock.instant()
        val recoveryId = RecoveryId(UUID.randomUUID())

        recoveryCases.save(
            RecoveryCase(
                recoveryId = recoveryId,
                accountId = accountId,
                state = RecoveryState.REQUESTED,
                createdAt = now,
                expiresAt = now.plus(RECOVERY_CASE_TTL),
                authorizedAt = null,
                holdEndsAt = null,
                authorizationMethod = null,
                completedAt = null,
                cancelledAt = null
            )
        )

        return RecoveryChallenge(
            recoveryId = recoveryId,
            challenge = ByteArray(32).also(random::nextBytes),
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL)
        ).also {
            challenges.save(it)
            notifier.recoveryStarted(accountId, recoveryId)
        }
    }

    /**
     * REC-08 and GATE-RECOVERY-02:
     *
     * Authorization begins a hold. It never produces a privileged session.
     */
    fun authorize(
        recoveryId: RecoveryId,
        attempt: RecoveryAttempt
    ): RecoveryCase {
        val now = clock.instant()

        val recoveryCase = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        requireRequestedAndUnexpired(recoveryCase, now)

        val challenge = challenges.consume(recoveryId, now)
            ?: throw RecoveryDenied("Recovery challenge is unavailable.")

        if (now >= challenge.expiresAt) {
            throw RecoveryDenied("Recovery challenge expired.")
        }

        val method = verifyAttempt(recoveryCase, challenge, attempt)
            ?: return requireNotNull(
                recoveryCases.update(recoveryId) {
                    it.copy(state = RecoveryState.REVIEW_REQUIRED)
                }
            )

        val authorized = requireNotNull(
            recoveryCases.update(recoveryId) {
                it.copy(
                    state = RecoveryState.HOLD,
                    authorizedAt = now,
                    holdEndsAt = now.plus(RECOVERY_HOLD),
                    authorizationMethod = method
                )
            }
        )

        notifier.recoveryAuthorized(authorized.accountId, recoveryId)
        return authorized
    }

    /**
     * GATE-RECOVERY-03:
     * An active valid credential can cancel recovery during the hold period.
     */
    fun cancelWithActiveCredential(recoveryId: RecoveryId): RecoveryCase {
        val now = clock.instant()
        val existing = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        if (existing.state !in setOf(RecoveryState.REQUESTED, RecoveryState.HOLD)) {
            throw RecoveryDenied("Recovery cannot be cancelled in its current state.")
        }

        return requireNotNull(
            recoveryCases.update(recoveryId) {
                it.copy(
                    state = RecoveryState.CANCELLED,
                    cancelledAt = now
                )
            }
        )
    }

    /**
     * GATE-RECOVERY-04:
     *
     * Recovery creates no restored historical session. Completion invalidates
     * all sessions, requiring a fresh session after the new passkey is created
     * through the credential service.
     */
    fun completeAfterHold(
        recoveryId: RecoveryId,
        addReplacementCredential: () -> Unit
    ): RecoveryCase {
        val now = clock.instant()
        val recoveryCase = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        if (recoveryCase.state != RecoveryState.HOLD) {
            throw RecoveryDenied("Recovery is not ready for completion.")
        }

        if (now < requireNotNull(recoveryCase.holdEndsAt)) {
            throw RecoveryDenied("Recovery hold has not completed.")
        }

        addReplacementCredential()
        sessions.revokeAllSessions(recoveryCase.accountId, now)

        return requireNotNull(
            recoveryCases.update(recoveryId) {
                it.copy(
                    state = RecoveryState.COMPLETED,
                    completedAt = now
                )
            }
        ).also {
            notifier.recoveryCompleted(it.accountId, recoveryId)
        }
    }

    /**
     * REC-01 and REC-02:
     *
     * Each code has 128 random bits and is returned only at generation time.
     * The caller must render it in accessible text and offer local/offline
     * storage guidance; no code is persisted in plaintext or resent later.
     */
    fun generateOfflineRecoveryCode(
        accountId: AccountId,
        hasher: RecoveryCodeHasher
    ): String {
        val codeBytes = ByteArray(16).also(random::nextBytes)
        val displayCode = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(codeBytes)

        val salt = ByteArray(16).also(random::nextBytes)
        val verifier = hasher.hash(displayCode.toCharArray(), salt)

        recoveryCodes.save(
            RecoveryCodeVerifier(
                accountId = accountId,
                verifier = verifier,
                salt = salt,
                createdAt = clock.instant(),
                usedAt = null,
                revokedAt = null
            )
        )

        return displayCode
    }

    private fun verifyAttempt(
        recoveryCase: RecoveryCase,
        challenge: RecoveryChallenge,
        attempt: RecoveryAttempt
    ): RecoveryAuthorizationMethod? =
        when (attempt) {
            is RecoveryAttempt.IndependentPasskey -> {
                if (proofVerifier.verifyIndependentRegisteredPasskey(
                        recoveryCase.accountId,
                        challenge.challenge,
                        attempt.assertion
                    )
                ) {
                    RecoveryAuthorizationMethod.SECOND_REGISTERED_PASSKEY
                } else {
                    null
                }
            }

            is RecoveryAttempt.OfflineCode -> {
                if (recoveryCodes.consumeOneAndRevokeRest(
                        recoveryCase.accountId,
                        attempt.code,
                        // This object is supplied by caller in production.
                        // The example chooses no implicit/global password hasher.
                        throw UnsupportedOperationException(
                            "Inject RecoveryCodeHasher through the adapter in production."
                        ),
                        clock.instant()
                    )
                ) {
                    RecoveryAuthorizationMethod.OFFLINE_RECOVERY_CODE
                } else {
                    null
                }
            }

            is RecoveryAttempt.PreestablishedFederatedBinding -> {
                if (proofVerifier.verifyPreestablishedFederatedBinding(
                        recoveryCase.accountId,
                        attempt.authorizationCode,
                        attempt.pkceVerifier
                    )
                ) {
                    RecoveryAuthorizationMethod.PREESTABLISHED_FEDERATED_BINDING
                } else {
                    null
                }
            }
        }

    private fun requireRequestedAndUnexpired(
        recoveryCase: RecoveryCase,
        now: Instant
    ) {
        if (now >= recoveryCase.expiresAt) {
            recoveryCases.update(recoveryCase.recoveryId) {
                it.copy(state = RecoveryState.EXPIRED)
            }
            throw RecoveryDenied("Recovery request expired.")
        }

        if (recoveryCase.state != RecoveryState.REQUESTED) {
            throw RecoveryDenied("Recovery request is not awaiting authorization.")
        }
    }
}
