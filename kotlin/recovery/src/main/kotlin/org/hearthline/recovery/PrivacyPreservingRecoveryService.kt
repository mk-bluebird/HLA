package org.hearthline.recovery

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@JvmInline
value class AccountId(val value: UUID)

@JvmInline
value class RecoveryId(val value: UUID)

enum class RecoveryState {
    RECOVERY_REQUESTED,
    RECOVERY_AUTHORIZED,
    RECOVERY_HOLD,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    REVIEW_REQUIRED
}

enum class RecoveryAuthorizationMethod {
    SECOND_REGISTERED_PASSKEY,
    INDEPENDENT_SECURITY_KEY,
    OFFLINE_RECOVERY_CODE,
    OPTIONAL_FEDERATED_BINDING
}

/**
 * No profile, discovery, relationship, message, device, IP, location, document,
 * biometric, or social-graph information appears in this record.
 */
data class RecoveryCase(
    val recoveryId: RecoveryId,
    val accountId: AccountId,
    val state: RecoveryState,
    val createdAt: Instant,
    val expiresAt: Instant,
    val authorizedAt: Instant?,
    val holdEndsAt: Instant?,
    val authorizationMethod: RecoveryAuthorizationMethod?,
    val replacementCredentialPending: Boolean,
    val completedAt: Instant?,
    val cancelledAt: Instant?
)

data class RecoveryChallenge(
    val recoveryId: RecoveryId,
    val challenge: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant?
)

sealed interface RecoveryAuthorizationAttempt {
    data class RegisteredPasskey(
        val assertion: ByteArray
    ) : RecoveryAuthorizationAttempt

    data class OfflineRecoveryCode(
        val code: CharArray
    ) : RecoveryAuthorizationAttempt

    data class FederatedBinding(
        val authorizationCode: String,
        val pkceVerifier: String
    ) : RecoveryAuthorizationAttempt
}

interface RecoveryCaseRepository {
    fun save(value: RecoveryCase)
    fun find(recoveryId: RecoveryId): RecoveryCase?
    fun update(recoveryId: RecoveryId, transform: (RecoveryCase) -> RecoveryCase): RecoveryCase?
    fun deleteExpired(now: Instant): Int
}

interface RecoveryChallengeRepository {
    fun save(value: RecoveryChallenge)
    fun consume(recoveryId: RecoveryId, consumedAt: Instant): RecoveryChallenge?
    fun deleteExpired(now: Instant): Int
}

interface RecoveryProofVerifier {
    fun verifyRegisteredIndependentPasskey(
        accountId: AccountId,
        challenge: ByteArray,
        assertion: ByteArray
    ): Boolean

    fun verifyOfflineRecoveryCode(
        accountId: AccountId,
        recoveryId: RecoveryId,
        code: CharArray
    ): Boolean

    /**
     * This verifies only a pre-existing isolated federated binding.
     * It must not use email equality, name, photo, device, or provider trust score.
     */
    fun verifyOptionalFederatedBinding(
        accountId: AccountId,
        authorizationCode: String,
        pkceVerifier: String
    ): Boolean
}

interface RecoveryNotifier {
    /**
     * Notification delivery is informational and must not contain a credential
     * replacement token or create an alternate recovery path.
     */
    fun notifyRecoveryStarted(accountId: AccountId, recoveryId: RecoveryId)
    fun notifyRecoveryAuthorized(accountId: AccountId, recoveryId: RecoveryId)
    fun notifyRecoveryCompleted(accountId: AccountId, recoveryId: RecoveryId)
}

interface CredentialReplacementService {
    fun addReplacementCredentialAfterHold(
        accountId: AccountId,
        recoveryId: RecoveryId,
        newCredentialPublicKey: ByteArray
    )

    fun revokeAllSessions(accountId: AccountId)
}

class PrivacyPreservingRecoveryService(
    private val clock: Clock,
    private val random: SecureRandom,
    private val recoveryCases: RecoveryCaseRepository,
    private val recoveryChallenges: RecoveryChallengeRepository,
    private val proofVerifier: RecoveryProofVerifier,
    private val notifier: RecoveryNotifier,
    private val credentialReplacement: CredentialReplacementService
) {
    companion object {
        private val RECOVERY_CASE_TTL = Duration.ofHours(24)
        private val RECOVERY_CHALLENGE_TTL = Duration.ofMinutes(5)
        private val RECOVERY_HOLD = Duration.ofHours(24)
    }

    fun begin(accountId: AccountId): RecoveryChallenge {
        val now = clock.instant()
        val recoveryId = RecoveryId(UUID.randomUUID())

        recoveryCases.save(
            RecoveryCase(
                recoveryId = recoveryId,
                accountId = accountId,
                state = RecoveryState.RECOVERY_REQUESTED,
                createdAt = now,
                expiresAt = now.plus(RECOVERY_CASE_TTL),
                authorizedAt = null,
                holdEndsAt = null,
                authorizationMethod = null,
                replacementCredentialPending = false,
                completedAt = null,
                cancelledAt = null
            )
        )

        val challenge = RecoveryChallenge(
            recoveryId = recoveryId,
            challenge = ByteArray(32).also(random::nextBytes),
            issuedAt = now,
            expiresAt = now.plus(RECOVERY_CHALLENGE_TTL),
            consumedAt = null
        )

        recoveryChallenges.save(challenge)
        notifier.notifyRecoveryStarted(accountId, recoveryId)

        return challenge
    }

    fun authorize(
        recoveryId: RecoveryId,
        attempt: RecoveryAuthorizationAttempt
    ): RecoveryCase {
        val now = clock.instant()

        val recoveryCase = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        requireActiveRecovery(recoveryCase, now)

        val challenge = recoveryChallenges.consume(recoveryId, now)
            ?: throw RecoveryDenied("Recovery challenge was unavailable or already used.")

        if (now >= challenge.expiresAt) {
            throw RecoveryDenied("Recovery challenge expired.")
        }

        val method = verifyAttempt(recoveryCase.accountId, challenge, recoveryId, attempt)
            ?: return requireNotNull(
                recoveryCases.update(recoveryId) {
                    it.copy(state = RecoveryState.REVIEW_REQUIRED)
                }
            )

        val authorized = requireNotNull(
            recoveryCases.update(recoveryId) {
                it.copy(
                    state = RecoveryState.RECOVERY_HOLD,
                    authorizedAt = now,
                    holdEndsAt = now.plus(RECOVERY_HOLD),
                    authorizationMethod = method,
                    replacementCredentialPending = true
                )
            }
        )

        notifier.notifyRecoveryAuthorized(authorized.accountId, recoveryId)
        return authorized
    }

    /**
     * An active credential can cancel recovery during the hold.
     * Cancellation is intentionally simple: it does not require a narrative,
     * profile data, or support interaction.
     */
    fun cancelWithActiveCredential(recoveryId: RecoveryId): RecoveryCase {
        val now = clock.instant()

        val existing = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        if (existing.state !in setOf(
                RecoveryState.RECOVERY_REQUESTED,
                RecoveryState.RECOVERY_AUTHORIZED,
                RecoveryState.RECOVERY_HOLD
            )
        ) {
            throw RecoveryDenied("This recovery request cannot be cancelled.")
        }

        return requireNotNull(
            recoveryCases.update(recoveryId) {
                it.copy(
                    state = RecoveryState.CANCELLED,
                    cancelledAt = now,
                    replacementCredentialPending = false
                )
            }
        )
    }

    /**
     * Completion adds a fresh credential only after the mandatory hold.
     *
     * It revokes current sessions rather than restoring session tokens, avoiding
     * a recovery path that silently inherits old session authority.
     */
    fun completeAfterHold(
        recoveryId: RecoveryId,
        newCredentialPublicKey: ByteArray
    ): RecoveryCase {
        val now = clock.instant()

        val recoveryCase = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        if (recoveryCase.state != RecoveryState.RECOVERY_HOLD) {
            throw RecoveryDenied("Recovery is not ready for credential replacement.")
        }

        val holdEndsAt = recoveryCase.holdEndsAt
            ?: throw RecoveryDenied("Recovery hold was not configured.")

        if (now < holdEndsAt) {
            throw RecoveryDenied("Recovery hold has not completed.")
        }

        credentialReplacement.addReplacementCredentialAfterHold(
            accountId = recoveryCase.accountId,
            recoveryId = recoveryId,
            newCredentialPublicKey = newCredentialPublicKey
        )

        credentialReplacement.revokeAllSessions(recoveryCase.accountId)

        val completed = requireNotNull(
            recoveryCases.update(recoveryId) {
                it.copy(
                    state = RecoveryState.COMPLETED,
                    completedAt = now,
                    replacementCredentialPending = false
                )
            }
        )

        notifier.notifyRecoveryCompleted(completed.accountId, recoveryId)
        return completed
    }

    private fun verifyAttempt(
        accountId: AccountId,
        challenge: RecoveryChallenge,
        recoveryId: RecoveryId,
        attempt: RecoveryAuthorizationAttempt
    ): RecoveryAuthorizationMethod? =
        when (attempt) {
            is RecoveryAuthorizationAttempt.RegisteredPasskey -> {
                if (proofVerifier.verifyRegisteredIndependentPasskey(
                        accountId = accountId,
                        challenge = challenge.challenge,
                        assertion = attempt.assertion
                    )
                ) {
                    RecoveryAuthorizationMethod.SECOND_REGISTERED_PASSKEY
                } else {
                    null
                }
            }

            is RecoveryAuthorizationAttempt.OfflineRecoveryCode -> {
                if (proofVerifier.verifyOfflineRecoveryCode(
                        accountId = accountId,
                        recoveryId = recoveryId,
                        code = attempt.code
                    )
                ) {
                    RecoveryAuthorizationMethod.OFFLINE_RECOVERY_CODE
                } else {
                    null
                }
            }

            is RecoveryAuthorizationAttempt.FederatedBinding -> {
                if (proofVerifier.verifyOptionalFederatedBinding(
                        accountId = accountId,
                        authorizationCode = attempt.authorizationCode,
                        pkceVerifier = attempt.pkceVerifier
                    )
                ) {
                    RecoveryAuthorizationMethod.OPTIONAL_FEDERATED_BINDING
                } else {
                    null
                }
            }
        }

    private fun requireActiveRecovery(
        recoveryCase: RecoveryCase,
        now: Instant
    ) {
        if (now >= recoveryCase.expiresAt) {
            recoveryCases.update(recoveryCase.recoveryId) {
                it.copy(state = RecoveryState.EXPIRED)
            }
            throw RecoveryDenied("Recovery request expired.")
        }

        if (recoveryCase.state != RecoveryState.RECOVERY_REQUESTED) {
            throw RecoveryDenied("Recovery is not awaiting authorization.")
        }
    }
}

class RecoveryDenied(message: String) : RuntimeException(message)
