package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class AccountSecurityState(
    val accountId: AccountId,
    val status: AccountStatus,
    val sessionGeneration: Long,
    val updatedAt: Instant
)

data class CredentialSecurityState(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val status: CredentialStatus,
    val generation: Long,
    val updatedAt: Instant
)

/**
 * INV-SESSION-01:
 *
 * No device, network, platform, location, hardware, presence, browser,
 * attestation, or device-trust field exists in this durable session model.
 */
data class CredentialBoundSession(
    val sessionId: SessionId,
    val accountId: AccountId,
    val authenticatingCredentialId: CredentialId,
    val credentialGenerationAtIssue: Long,
    val accountSessionGenerationAtIssue: Long,
    val issuedAt: Instant,
    val lastFreshAuthenticationAt: Instant,
    val expiresAt: Instant,
    val status: SessionStatus,
    val senderConstraintPublicKeyHash: ByteArray?,
    val revokedAt: Instant? = null
)

data class ActionAuthorization(
    val authorizationId: ActionAuthorizationId,
    val accountId: AccountId,
    val sessionId: SessionId,
    val action: SensitiveAction,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant? = null
)

interface AccountSecurityStateRepository {
    fun find(accountId: AccountId): AccountSecurityState?
}

interface CredentialSecurityStateRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): CredentialSecurityState?

    fun revokeAndIncrement(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): CredentialSecurityState?
}

interface CredentialBoundSessionRepository {
    fun save(session: CredentialBoundSession)
    fun find(sessionId: SessionId): CredentialBoundSession?
    fun revoke(sessionId: SessionId, at: Instant): Boolean
    fun expire(sessionId: SessionId, at: Instant): Boolean

    fun revokeByCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): Int

    fun revokeAllForAccount(accountId: AccountId, at: Instant): Int

    fun deleteExpired(now: Instant): Int
}

interface ActionAuthorizationRepository {
    fun save(value: ActionAuthorization)

    /**
     * Atomically consumes authorization. Replay must return null.
     */
    fun consume(
        authorizationId: ActionAuthorizationId,
        at: Instant
    ): ActionAuthorization?

    fun deleteExpired(now: Instant): Int
}

class CredentialBoundSessionService(
    private val clock: Clock,
    private val accounts: AccountSecurityStateRepository,
    private val credentials: CredentialSecurityStateRepository,
    private val sessions: CredentialBoundSessionRepository,
    private val actions: ActionAuthorizationRepository
) {
    companion object {
        private val SESSION_TTL: Duration = Duration.ofHours(8)
        private val RECENT_FRESH_AUTH_TTL: Duration = Duration.ofMinutes(30)
        private val ACTION_TTL: Duration = Duration.ofMinutes(5)
    }

    /**
     * Called only after a separate WebAuthn verifier confirms a fresh assertion.
     *
     * The `ephemeralSenderKey` must be newly generated per session when used.
     * Its hash is destroyed when the session ends and cannot be reused across
     * sessions, products, or accounts.
     */
    fun issueAfterVerifiedPasskey(
        accountId: AccountId,
        credentialId: CredentialId,
        ephemeralSenderKey: ByteArray? = null
    ): CredentialBoundSession {
        val now = clock.instant()
        val account = requireActiveAccount(accountId)
        val credential = requireActiveCredential(accountId, credentialId)

        return CredentialBoundSession(
            sessionId = SessionId(UUID.randomUUID()),
            accountId = accountId,
            authenticatingCredentialId = credentialId,
            credentialGenerationAtIssue = credential.generation,
            accountSessionGenerationAtIssue = account.sessionGeneration,
            issuedAt = now,
            lastFreshAuthenticationAt = now,
            expiresAt = now.plus(SESSION_TTL),
            status = SessionStatus.ACTIVE,
            senderConstraintPublicKeyHash = ephemeralSenderKey?.let(CryptoSupport::sha256)
        ).also(sessions::save)
    }

    /**
     * INV-SESSION-04:
     * Step-up rotates session identity before sensitive authorization is issued.
     */
    fun rotateAfterFreshPasskey(
        priorSessionId: SessionId,
        credentialId: CredentialId,
        ephemeralSenderKey: ByteArray? = null
    ): CredentialBoundSession {
        val now = clock.instant()
        val prior = requireActiveSession(priorSessionId)
        sessions.revoke(priorSessionId, now)

        return issueAfterVerifiedPasskey(
            accountId = prior.accountId,
            credentialId = credentialId,
            ephemeralSenderKey = ephemeralSenderKey
        )
    }

    fun requireActiveSession(sessionId: SessionId): CredentialBoundSession {
        val now = clock.instant()
        val session = sessions.find(sessionId)
            ?: throw AuthorizationDenied("Session was not found.")

        if (session.status != SessionStatus.ACTIVE) {
            throw AuthorizationDenied("Session is no longer active.")
        }

        if (now >= session.expiresAt) {
            sessions.expire(sessionId, now)
            throw AuthorizationDenied("Session expired.")
        }

        val account = requireActiveAccount(session.accountId)
        if (session.accountSessionGenerationAtIssue != account.sessionGeneration) {
            sessions.revoke(sessionId, now)
            throw AuthorizationDenied("Session is stale after account security change.")
        }

        val credential = credentials.find(
            session.accountId,
            session.authenticatingCredentialId
        ) ?: throw AuthorizationDenied("Authenticating credential is unavailable.")

        if (
            credential.status != CredentialStatus.ACTIVE ||
            credential.generation != session.credentialGenerationAtIssue
        ) {
            sessions.revoke(sessionId, now)
            throw AuthorizationDenied("Session is stale after credential change.")
        }

        return session
    }

    fun beginSensitiveAction(
        sessionId: SessionId,
        action: SensitiveAction
    ): ActionAuthorization {
        val now = clock.instant()
        val session = requireActiveSession(sessionId)

        if (Duration.between(session.lastFreshAuthenticationAt, now) >
            RECENT_FRESH_AUTH_TTL
        ) {
            throw FreshPasskeyRequired(
                "A fresh passkey confirmation is required for this action."
            )
        }

        return ActionAuthorization(
            authorizationId = ActionAuthorizationId(UUID.randomUUID()),
            accountId = session.accountId,
            sessionId = session.sessionId,
            action = action,
            issuedAt = now,
            expiresAt = now.plus(ACTION_TTL)
        ).also(actions::save)
    }

    fun consumeSensitiveAction(
        sessionId: SessionId,
        authorizationId: ActionAuthorizationId,
        action: SensitiveAction
    ): ActionAuthorization {
        val now = clock.instant()
        val session = requireActiveSession(sessionId)

        val authorization = actions.consume(authorizationId, now)
            ?: throw AuthorizationDenied("Action authorization unavailable.")

        if (
            authorization.accountId != session.accountId ||
            authorization.sessionId != session.sessionId ||
            authorization.action != action ||
            now >= authorization.expiresAt
        ) {
            throw AuthorizationDenied("Action authorization invalid.")
        }

        return authorization
    }

    /**
     * INV-SESSION-03 and GATE-SESSION-02:
     * Generation changes and linked-session revocation independently invalidate
     * sessions created by a revoked credential.
     */
    fun revokeCredential(
        accountId: AccountId,
        credentialId: CredentialId
    ): Int {
        val now = clock.instant()
        credentials.revokeAndIncrement(accountId, credentialId, now)
            ?: throw AuthorizationDenied("Credential was not found.")

        return sessions.revokeByCredential(accountId, credentialId, now)
    }

    private fun requireActiveAccount(accountId: AccountId): AccountSecurityState {
        val account = accounts.find(accountId)
            ?: throw AuthorizationDenied("Account was not found.")

        if (account.status != AccountStatus.ACTIVE) {
            throw AuthorizationDenied("Account is not active.")
        }

        return account
    }

    private fun requireActiveCredential(
        accountId: AccountId,
        credentialId: CredentialId
    ): CredentialSecurityState {
        val credential = credentials.find(accountId, credentialId)
            ?: throw AuthorizationDenied("Credential was not found.")

        if (credential.status != CredentialStatus.ACTIVE) {
            throw AuthorizationDenied("Credential is not active.")
        }

        return credential
    }
}
