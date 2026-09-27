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

data class RecoveryCodeVerifier(
    val accountId: AccountId,
    val verifier: ByteArray,
    val salt: ByteArray,
    val createdAt: Instant,
    val usedAt: Instant?,
    val revokedAt: Instant?
) {
    init {
        require(verifier.isNotEmpty()) {
            "Recovery-code verifier cannot be empty."
        }
        require(salt.size >= 16) {
            "Recovery-code salt must contain at least 128 bits."
        }
    }
}

interface RecoveryCodeHasher {
    fun hash(code: CharArray, salt: ByteArray): ByteArray

    fun verify(
        code: CharArray,
        salt: ByteArray,
        verifier: ByteArray
    ): Boolean
}

interface RecoveryCodeRepository {
    fun save(verifier: RecoveryCodeVerifier)

    fun findActive(accountId: AccountId): List<RecoveryCodeVerifier>

    /**
     * This operation must be atomic:
     *
     * 1. Verify the submitted code against an active verifier.
     * 2. Mark the matching verifier used.
     * 3. Revoke every other active recovery code for the account.
     *
     * It returns false for an unknown, expired, used, revoked, or invalid code.
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

    /**
     * The update must use compare-and-set, transaction isolation, or an
     * equivalent concurrency-safe mechanism. It must not allow simultaneous
     * transitions such as HOLD -> COMPLETED and HOLD -> CANCELLED.
     */
    fun update(
        recoveryId: RecoveryId,
        transform: (RecoveryCase) -> RecoveryCase
    ): RecoveryCase?

    fun deleteExpired(now: Instant): Int
}

interface RecoveryChallengeRepository {
    fun save(value: RecoveryChallenge)

    /**
     * Must atomically set consumedAt only when it is currently null.
     * A replayed challenge returns null.
     */
    fun consume(recoveryId: RecoveryId, at: Instant): RecoveryChallenge?

    fun deleteExpired(now: Instant): Int
}

interface IndependentRecoveryProofVerifier {
    fun verifyIndependentRegisteredPasskey(
        accountId: AccountId,
        challenge: ByteArray,
        assertion: ByteArray
    ): Boolean

    /**
     * This verifies only an existing isolated federation binding for the same
     * local Hearthline account. It must validate authorization code, PKCE,
     * issuer, audience, state, nonce, redirect URI, expiry, and binding.
     *
     * It must not use email, name, profile, photograph, device data, IP data,
     * provider trust, provider risk, provider account age, or provider oid.
     */
    fun verifyPreestablishedFederatedBinding(
        accountId: AccountId,
        authorizationCode: String,
        pkceVerifier: String
    ): Boolean
}

interface RecoverySessionInvalidator {
    fun revokeAllSessions(accountId: AccountId, at: Instant)
}

interface RecoveryNotifier {
    /**
     * Notifications are informational only. They must not contain a credential
     * replacement token, a recovery-completion link, or any other bearer secret.
     */
    fun recoveryStarted(accountId: AccountId, recoveryId: RecoveryId)

    fun recoveryAuthorized(accountId: AccountId, recoveryId: RecoveryId)

    fun recoveryCancelled(accountId: AccountId, recoveryId: RecoveryId)

    fun recoveryCompleted(accountId: AccountId, recoveryId: RecoveryId)
}

/**
 * Replaces an old/recovered credential set only after the recovery hold ends.
 * The implementation must atomically add the replacement credential and record
 * the replacement state before sessions are revoked.
 */
interface RecoveryCredentialReplacementService {
    fun addReplacementCredential(
        accountId: AccountId,
        recoveryId: RecoveryId,
        registration: ReplacementPasskeyRegistration
    )
}

data class ReplacementPasskeyRegistration(
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray
) {
    init {
        require(publicKeyCose.isNotEmpty()) {
            "Replacement passkey public key cannot be empty."
        }
    }
}

sealed interface RecoveryAttempt {
    data class IndependentPasskey(
        val assertion: ByteArray
    ) : RecoveryAttempt {
        init {
            require(assertion.isNotEmpty()) {
                "Passkey assertion cannot be empty."
            }
        }
    }

    data class OfflineCode(
        val code: CharArray
    ) : RecoveryAttempt {
        init {
            require(code.isNotEmpty()) {
                "Recovery code cannot be empty."
            }
        }
    }

    data class PreestablishedFederatedBinding(
        val authorizationCode: String,
        val pkceVerifier: String
    ) : RecoveryAttempt {
        init {
            require(authorizationCode.isNotBlank()) {
                "Authorization code cannot be blank."
            }
            require(pkceVerifier.isNotBlank()) {
                "PKCE verifier cannot be blank."
            }
        }
    }
}

/**
 * Recovery state machine:
 *
 * REQUESTED -> HOLD -> COMPLETED
 * REQUESTED -> REVIEW_REQUIRED
 * REQUESTED -> CANCELLED
 * HOLD      -> CANCELLED
 * REQUESTED -> EXPIRED
 * HOLD      -> EXPIRED
 *
 * Recovery never creates a privileged session. Completion requires a fresh
 * passkey sign-in after credential replacement and session invalidation.
 */
class RecoveryService(
    private val clock: Clock,
    private val random: SecureRandom,
    private val recoveryCases: RecoveryCaseRepository,
    private val challenges: RecoveryChallengeRepository,
    private val recoveryCodes: RecoveryCodeRepository,
    private val recoveryCodeHasher: RecoveryCodeHasher,
    private val proofVerifier: IndependentRecoveryProofVerifier,
    private val credentialReplacement: RecoveryCredentialReplacementService,
    private val sessions: RecoverySessionInvalidator,
    private val notifier: RecoveryNotifier
) {
    companion object {
        private val RECOVERY_CASE_TTL: Duration = Duration.ofHours(24)
        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
        private val RECOVERY_HOLD: Duration = Duration.ofHours(24)

        const val RECOVERY_CODE_ENTROPY_BYTES: Int = 16
        const val RECOVERY_CODE_SALT_BYTES: Int = 16
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
        ).also { challenge ->
            challenges.save(challenge)
            notifier.recoveryStarted(accountId, recoveryId)
        }
    }

    /**
     * A successful proof starts a mandatory hold. It does not issue a session,
     * add a credential, export data, or unlock sensitive account settings.
     */
    fun authorize(
        recoveryId: RecoveryId,
        attempt: RecoveryAttempt
    ): RecoveryCase {
        val now = clock.instant()

        val recoveryCase = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        requireAuthorizationState(recoveryCase, now)

        val challenge = challenges.consume(recoveryId, now)
            ?: throw RecoveryDenied("Recovery challenge is unavailable.")

        if (now >= challenge.expiresAt) {
            markExpired(recoveryId, now)
            throw RecoveryDenied("Recovery challenge expired.")
        }

        val method = verifyAttempt(
            recoveryCase = recoveryCase,
            challenge = challenge,
            attempt = attempt,
            now = now
        )

        if (method == null) {
            return requireNotNull(
                recoveryCases.update(recoveryId) { current ->
                    require(current.state == RecoveryState.REQUESTED) {
                        "Recovery is no longer awaiting authorization."
                    }
                    current.copy(state = RecoveryState.REVIEW_REQUIRED)
                }
            )
        }

        val authorized = requireNotNull(
            recoveryCases.update(recoveryId) { current ->
                require(current.state == RecoveryState.REQUESTED) {
                    "Recovery is no longer awaiting authorization."
                }
                current.copy(
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
     * The caller must have just completed a fresh assertion with an active
     * credential for the same account before invoking this method.
     */
    fun cancelWithActiveCredential(
        accountId: AccountId,
        recoveryId: RecoveryId
    ): RecoveryCase {
        val now = clock.instant()
        val existing = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        if (existing.accountId != accountId) {
            throw RecoveryDenied("Recovery request does not belong to this account.")
        }

        if (existing.state !in setOf(RecoveryState.REQUESTED, RecoveryState.HOLD)) {
            throw RecoveryDenied("Recovery cannot be cancelled in its current state.")
        }

        val cancelled = requireNotNull(
            recoveryCases.update(recoveryId) { current ->
                require(current.accountId == accountId) {
                    "Recovery request does not belong to this account."
                }
                require(current.state in setOf(RecoveryState.REQUESTED, RecoveryState.HOLD)) {
                    "Recovery cannot be cancelled in its current state."
                }
                current.copy(
                    state = RecoveryState.CANCELLED,
                    cancelledAt = now
                )
            }
        )

        notifier.recoveryCancelled(cancelled.accountId, recoveryId)
        return cancelled
    }

    /**
     * Completes recovery after the protection hold:
     *
     * 1. Ensures the hold elapsed.
     * 2. Adds the replacement passkey through the credential boundary.
     * 3. Revokes all historical sessions.
     * 4. Marks recovery completed.
     *
     * No old session is restored. The member signs in again using the new
     * passkey to establish a new session.
     */
    fun completeAfterHold(
        recoveryId: RecoveryId,
        replacement: ReplacementPasskeyRegistration
    ): RecoveryCase {
        val now = clock.instant()
        val recoveryCase = recoveryCases.find(recoveryId)
            ?: throw RecoveryDenied("Recovery request was not found.")

        requireCompletionState(recoveryCase, now)

        credentialReplacement.addReplacementCredential(
            accountId = recoveryCase.accountId,
            recoveryId = recoveryId,
            registration = replacement
        )

        sessions.revokeAllSessions(recoveryCase.accountId, now)

        val completed = requireNotNull(
            recoveryCases.update(recoveryId) { current ->
                require(current.state == RecoveryState.HOLD) {
                    "Recovery is not in a completion state."
                }
                require(now >= requireNotNull(current.holdEndsAt)) {
                    "Recovery hold has not completed."
                }
                current.copy(
                    state = RecoveryState.COMPLETED,
                    completedAt = now
                )
            }
        )

        notifier.recoveryCompleted(completed.accountId, recoveryId)
        return completed
    }

    /**
     * Generates exactly one displayable 128-bit recovery code. The code is
     * returned once; only an expensive verifier is retained.
     *
     * The caller is responsible for rendering this in accessible text and
     * explaining offline storage. It must not email, text, or log the code.
     */
    fun generateOfflineRecoveryCode(accountId: AccountId): String {
        val codeBytes = ByteArray(RECOVERY_CODE_ENTROPY_BYTES)
            .also(random::nextBytes)

        val displayCode = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(codeBytes)

        val salt = ByteArray(RECOVERY_CODE_SALT_BYTES)
            .also(random::nextBytes)

        val codeChars = displayCode.toCharArray()

        try {
            recoveryCodes.save(
                RecoveryCodeVerifier(
                    accountId = accountId,
                    verifier = recoveryCodeHasher.hash(codeChars, salt),
                    salt = salt,
                    createdAt = clock.instant(),
                    usedAt = null,
                    revokedAt = null
                )
            )
        } finally {
            codeChars.fill('\u0000')
            codeBytes.fill(0)
        }

        return displayCode
    }

    fun revokeOfflineRecoveryCodes(accountId: AccountId): Int =
        recoveryCodes.revokeAll(accountId, clock.instant())

    private fun verifyAttempt(
        recoveryCase: RecoveryCase,
        challenge: RecoveryChallenge,
        attempt: RecoveryAttempt,
        now: Instant
    ): RecoveryAuthorizationMethod? =
        when (attempt) {
            is RecoveryAttempt.IndependentPasskey -> {
                if (
                    proofVerifier.verifyIndependentRegisteredPasskey(
                        accountId = recoveryCase.accountId,
                        challenge = challenge.challenge,
                        assertion = attempt.assertion
                    )
                ) {
                    RecoveryAuthorizationMethod.SECOND_REGISTERED_PASSKEY
                } else {
                    null
                }
            }

            is RecoveryAttempt.OfflineCode -> {
                try {
                    if (
                        recoveryCodes.consumeOneAndRevokeRest(
                            accountId = recoveryCase.accountId,
                            code = attempt.code,
                            hasher = recoveryCodeHasher,
                            at = now
                        )
                    ) {
                        RecoveryAuthorizationMethod.OFFLINE_RECOVERY_CODE
                    } else {
                        null
                    }
                } finally {
                    attempt.code.fill('\u0000')
                }
            }

            is RecoveryAttempt.PreestablishedFederatedBinding -> {
                if (
                    proofVerifier.verifyPreestablishedFederatedBinding(
                        accountId = recoveryCase.accountId,
                        authorizationCode = attempt.authorizationCode,
                        pkceVerifier = attempt.pkceVerifier
                    )
                ) {
                    RecoveryAuthorizationMethod.PREESTABLISHED_FEDERATED_BINDING
                } else {
                    null
                }
            }
        }

    private fun requireAuthorizationState(
        recoveryCase: RecoveryCase,
        now: Instant
    ) {
        if (now >= recoveryCase.expiresAt) {
            markExpired(recoveryCase.recoveryId, now)
            throw RecoveryDenied("Recovery request expired.")
        }

        if (recoveryCase.state != RecoveryState.REQUESTED) {
            throw RecoveryDenied("Recovery request is not awaiting authorization.")
        }
    }

    private fun requireCompletionState(
        recoveryCase: RecoveryCase,
        now: Instant
    ) {
        if (now >= recoveryCase.expiresAt) {
            markExpired(recoveryCase.recoveryId, now)
            throw RecoveryDenied("Recovery request expired.")
        }

        if (recoveryCase.state != RecoveryState.HOLD) {
            throw RecoveryDenied("Recovery is not ready for credential replacement.")
        }

        if (now < requireNotNull(recoveryCase.holdEndsAt)) {
            throw RecoveryDenied("Recovery hold has not completed.")
        }
    }

    private fun markExpired(
        recoveryId: RecoveryId,
        now: Instant
    ) {
        recoveryCases.update(recoveryId) { current ->
            if (
                current.state == RecoveryState.REQUESTED ||
                current.state == RecoveryState.HOLD
            ) {
                current.copy(state = RecoveryState.EXPIRED)
            } else {
                current
            }
        }
    }
}
