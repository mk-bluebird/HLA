package org.hearthline.authintegrity

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryCredentialRepository : CredentialRepository {
    private val credentials = ConcurrentHashMap<String, RegisteredCredential>()

    private fun key(accountId: AccountId, credentialId: CredentialId): String =
        "${accountId.value}:${credentialId.stableStoreKey()}"

    override fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): RegisteredCredential? = credentials[key(accountId, credentialId)]

    override fun save(credential: RegisteredCredential) {
        credentials[key(credential.accountId, credential.credentialId)] = credential
    }

    override fun revoke(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): Boolean {
        val key = key(accountId, credentialId)

        return credentials.computeIfPresent(key) { _, current ->
            if (current.status == CredentialStatus.REVOKED) {
                current
            } else {
                current.copy(
                    status = CredentialStatus.REVOKED,
                    revokedAt = revokedAt
                )
            }
        }?.status == CredentialStatus.REVOKED
    }

    override fun allActiveForAccount(accountId: AccountId): List<RegisteredCredential> =
        credentials.values.filter {
            it.accountId == accountId && it.status == CredentialStatus.ACTIVE
        }

    override fun revokeAllForAccount(accountId: AccountId, revokedAt: Instant): Int {
        var changed = 0

        credentials.entries.forEach { entry ->
            val current = entry.value
            if (current.accountId == accountId && current.status == CredentialStatus.ACTIVE) {
                val replaced = current.copy(
                    status = CredentialStatus.REVOKED,
                    revokedAt = revokedAt
                )

                if (credentials.replace(entry.key, current, replaced)) {
                    changed += 1
                }
            }
        }

        return changed
    }
}

class InMemorySessionRepository : SessionRepository {
    private val sessions = ConcurrentHashMap<SessionId, AuthSession>()

    override fun find(sessionId: SessionId): AuthSession? = sessions[sessionId]

    override fun save(session: AuthSession) {
        sessions[session.sessionId] = session
    }

    override fun revoke(sessionId: SessionId, revokedAt: Instant): Boolean {
        val updated = sessions.computeIfPresent(sessionId) { _, current ->
            when (current.status) {
                SessionStatus.ACTIVE -> current.copy(
                    status = SessionStatus.REVOKED,
                    revokedAt = revokedAt
                )

                SessionStatus.REVOKED,
                SessionStatus.EXPIRED -> current
            }
        }

        return updated?.status == SessionStatus.REVOKED
    }

    override fun revokeByCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): Int {
        var changed = 0

        sessions.entries.forEach { entry ->
            val session = entry.value

            if (
                session.accountId == accountId &&
                session.authenticatingCredentialId == credentialId &&
                session.status == SessionStatus.ACTIVE
            ) {
                val replaced = session.copy(
                    status = SessionStatus.REVOKED,
                    revokedAt = revokedAt
                )

                if (sessions.replace(entry.key, session, replaced)) {
                    changed += 1
                }
            }
        }

        return changed
    }

    override fun revokeAllForAccount(accountId: AccountId, revokedAt: Instant): Int {
        var changed = 0

        sessions.entries.forEach { entry ->
            val session = entry.value

            if (session.accountId == accountId && session.status == SessionStatus.ACTIVE) {
                val replaced = session.copy(
                    status = SessionStatus.REVOKED,
                    revokedAt = revokedAt
                )

                if (sessions.replace(entry.key, session, replaced)) {
                    changed += 1
                }
            }
        }

        return changed
    }
}

class InMemoryChallengeRepository : ChallengeRepository {
    private val challenges = ConcurrentHashMap<ChallengeId, IntegrityChallenge>()

    override fun save(challenge: IntegrityChallenge) {
        challenges[challenge.challengeId] = challenge
    }

    override fun find(challengeId: ChallengeId): IntegrityChallenge? = challenges[challengeId]

    override fun consume(
        challengeId: ChallengeId,
        consumedAt: Instant
    ): IntegrityChallenge? {
        var consumed: IntegrityChallenge? = null

        challenges.computeIfPresent(challengeId) { _, current ->
            if (current.consumedAt != null) {
                current
            } else {
                current.copy(consumedAt = consumedAt).also { consumed = it }
            }
        }

        return consumed
    }

    override fun deleteExpired(now: Instant): Int {
        val expired = challenges.values.filter { it.isExpired(now) }.map { it.challengeId }
        expired.forEach(challenges::remove)
        return expired.size
    }
}

class InMemoryActionAuthorizationRepository : ActionAuthorizationRepository {
    private val authorizations = ConcurrentHashMap<ActionAuthorizationId, ActionAuthorization>()

    override fun save(authorization: ActionAuthorization) {
        authorizations[authorization.authorizationId] = authorization
    }

    override fun find(authorizationId: ActionAuthorizationId): ActionAuthorization? =
        authorizations[authorizationId]

    override fun deleteExpired(now: Instant): Int {
        val expired = authorizations.values
            .filter { it.isExpired(now) }
            .map { it.authorizationId }

        expired.forEach(authorizations::remove)
        return expired.size
    }
}

class InMemorySecurityHistoryRepository : SecurityHistoryRepository {
    private val events = mutableListOf<SecurityHistoryReceipt>()

    override fun append(receipt: SecurityHistoryReceipt) {
        synchronized(events) {
            events += receipt
        }
    }

    override fun deleteExpired(now: Instant): Int {
        synchronized(events) {
            val initialSize = events.size
            events.removeIf { now >= it.expiresAt }
            return initialSize - events.size
        }
    }

    fun allForAccount(accountId: AccountId): List<SecurityHistoryReceipt> =
        synchronized(events) {
            events.filter { it.accountId == accountId }.toList()
        }
}
