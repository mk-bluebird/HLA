package org.hearthline.session

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

class CredentialBoundSessionService(
    private val clock: Clock,
    private val accounts: AccountSecurityStateRepository,
    private val credentials: CredentialSecurityStateRepository,
    private val sessions: CredentialBoundSessionRepository,
    private val actionAuthorizations: ActionAuthorizationRepository
) {
    companion object {
        private val SESSION_MAX_AGE: Duration = Duration.ofHours(8)
        private val REAUTHENTICATION_INTERVAL: Duration = Duration.ofMinutes(30)
        private val SENSITIVE_ACTION_TTL: Duration = Duration.ofMinutes(5)
    }

    /**
     * Called only after a separate WebAuthn verifier has validated:
     *
     * - a fresh, single-use challenge;
     * - the expected RP ID;
     * - the expected origin;
     * - a valid assertion signature;
     * - required user verification;
     * - an active credential record.
     *
     * This method does not receive device metadata, attestation data, browser
     * fingerprint data, IP data, platform attributes, or location.
     */
    fun issueAfterVerifiedPasskey(
        accountId: AccountId,
        credentialId: CredentialId,
        channel: SessionChannel? = null
    ): CredentialBoundSession {
        val now = clock.instant()

        val account = accounts.find(accountId)
            ?: throw AuthorizationDenied("Account was not found.")

        require(account.status == AccountStatus.ACTIVE) {
            "Account is not active."
        }

        val credential = credentials.find(accountId, credentialId)
            ?: throw AuthorizationDenied("Credential was not found.")

        require(credential.status == CredentialStatus.ACTIVE) {
            "Credential is not active."
        }

        return CredentialBoundSession(
            sessionId = SessionId(UUID.randomUUID()),
            accountId = accountId,
            authenticatingCredentialId = credentialId,
            credentialGenerationAtIssue = credential.generation,
            issuedAt = now,
            lastReauthenticatedAt = now,
            expiresAt = now.plus(SESSION_MAX_AGE),
            status = SessionStatus.ACTIVE,
            channel = channel
        ).also(sessions::save)
    }

    /**
     * A fresh WebAuthn ceremony should call this after verification. It rotates
     * session identity so that a stolen pre-step-up session cannot inherit the
     * newly reauthenticated authority.
     */
    fun rotateAfterFreshPasskeyAssertion(
        existingSessionId: SessionId,
        credentialId: CredentialId
    ): CredentialBoundSession {
        val now = clock.instant()

        val existing = requireActiveSession(existingSessionId)
        val credential = requireActiveCredential(
            existing.accountId,
            credentialId
        )

        /*
         * Existing session is revoked before replacement is issued. This avoids
         * session fixation or privilege inheritance by a copied old session secret.
         */
        sessions.revoke(existingSessionId, now)

        return CredentialBoundSession(
            sessionId = SessionId(UUID.randomUUID()),
            accountId = existing.accountId,
            authenticatingCredentialId = credentialId,
            credentialGenerationAtIssue = credential.generation,
            issuedAt = now,
            lastReauthenticatedAt = now,
            expiresAt = now.plus(SESSION_MAX_AGE),
            status = SessionStatus.ACTIVE,
            channel = existing.channel
        ).also(sessions::save)
    }

    /**
     * All checks are server-authoritative. A valid cookie/token cannot override
     * current account, credential, generation, or session revocation state.
     */
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

        val account = accounts.find(session.accountId)
            ?: throw AuthorizationDenied("Account was not found.")

        if (account.status != AccountStatus.ACTIVE) {
            sessions.revoke(sessionId, now)
            throw AuthorizationDenied("Account is not active.")
        }

        val credential = credentials.find(
            session.accountId,
            session.authenticatingCredentialId
        ) ?: run {
            sessions.revoke(sessionId, now)
            throw AuthorizationDenied("Authenticating credential is unavailable.")
        }

        if (credential.status != CredentialStatus.ACTIVE) {
            sessions.revoke(sessionId, now)
            throw AuthorizationDenied("Authenticating credential was revoked.")
        }

        if (session.credentialGenerationAtIssue != credential.generation) {
            sessions.revoke(sessionId, now)
            throw AuthorizationDenied("Session is stale after credential change.")
        }

        return session
    }

    /**
     * A sensitive action requires a recent fresh passkey assertion. It is scoped
     * to the session and action, short-lived, and single-use.
     */
    fun beginSensitiveAction(
        sessionId: SessionId,
        action: SensitiveAction
    ): ActionAuthorization {
        val now = clock.instant()
        val session = requireActiveSession(sessionId)

        if (Duration.between(session.lastReauthenticatedAt, now) >
            REAUTHENTICATION_INTERVAL
        ) {
            throw FreshPasskeyRequired(
                "A fresh passkey confirmation is required for this action."
            )
        }

        return ActionAuthorization(
            actionAuthorizationId = ActionAuthorizationId(UUID.randomUUID()),
            accountId = session.accountId,
            sessionId = session.sessionId,
            action = action,
            issuedAt = now,
            expiresAt = now.plus(SENSITIVE_ACTION_TTL)
        ).also(actionAuthorizations::save)
    }

    fun consumeSensitiveAction(
        sessionId: SessionId,
        actionAuthorizationId: ActionAuthorizationId,
        expectedAction: SensitiveAction
    ): ActionAuthorization {
        val now = clock.instant()
        val session = requireActiveSession(sessionId)

        val authorization = actionAuthorizations.consume(
            actionAuthorizationId,
            now
        ) ?: throw AuthorizationDenied("Action authorization was not found.")

        if (
            authorization.accountId != session.accountId ||
            authorization.sessionId != session.sessionId ||
            authorization.action != expectedAction ||
            now >= authorization.expiresAt
        ) {
            throw AuthorizationDenied("Action authorization is not valid.")
        }

        return authorization
    }

    /**
     * Credential revocation increments the credential generation and then revokes
     * sessions whose issuance was bound to that credential. Either mechanism
     * independently prevents further privileged requests.
     */
    fun revokeCredentialAndLinkedSessions(
        accountId: AccountId,
        credentialId: CredentialId
    ): RevocationResult {
        val now = clock.instant()

        val newGeneration = credentials.revokeAndIncrementGeneration(
            accountId = accountId,
            credentialId = credentialId,
            revokedAt = now
        )

        val endedSessionCount = sessions.revokeByCredential(
            accountId = accountId,
            credentialId = credentialId,
            revokedAt = now
        )

        return RevocationResult(
            credentialGenerationAfterRevocation = newGeneration,
            linkedSessionsEnded = endedSessionCount
        )
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

data class RevocationResult(
    val credentialGenerationAfterRevocation: Long,
    val linkedSessionsEnded: Int
)

interface AccountSecurityStateRepository {
    fun find(accountId: AccountId): AccountSecurityState?
}

interface CredentialSecurityStateRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): CredentialSecurityState?

    fun revokeAndIncrementGeneration(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): Long
}

interface CredentialBoundSessionRepository {
    fun save(session: CredentialBoundSession)
    fun find(sessionId: SessionId): CredentialBoundSession?
    fun revoke(sessionId: SessionId, at: Instant): Boolean
    fun expire(sessionId: SessionId, at: Instant): Boolean

    fun revokeByCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): Int
}

interface ActionAuthorizationRepository {
    fun save(authorization: ActionAuthorization)

    /**
     * Must atomically mark the authorization consumed. A second use returns null.
     */
    fun consume(
        authorizationId: ActionAuthorizationId,
        consumedAt: Instant
    ): ActionAuthorization?
}

class AuthorizationDenied(message: String) : RuntimeException(message)
class FreshPasskeyRequired(message: String) : RuntimeException(message)
