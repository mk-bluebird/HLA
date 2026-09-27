package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

data class AccountSecurityEpoch(
    val accountId: AccountId,
    val status: AccountStatus,
    val generation: Long,
    val updatedAt: Instant
)

data class CredentialSecurityEpoch(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val status: CredentialStatus,
    val generation: Long,
    val updatedAt: Instant
)

data class CredentialBoundSession(
    val sessionId: SessionId,
    val accountId: AccountId,
    val credentialId: CredentialId,
    val accountGenerationAtIssue: Long,
    val credentialGenerationAtIssue: Long,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: SessionStatus
)

data class CapabilityGrant(
    val grantId: CapabilityGrantId,
    val accountId: AccountId,
    val credentialId: CredentialId,
    val accountGenerationAtIssue: Long,
    val credentialGenerationAtIssue: Long,
    val action: SensitiveAction,
    val expiresAt: Instant,
    val status: CapabilityStatus
)

interface AccountSecurityEpochRepository {
    fun find(accountId: AccountId): AccountSecurityEpoch?
    fun increment(accountId: AccountId, at: Instant): AccountSecurityEpoch?
}

interface CredentialSecurityEpochRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): CredentialSecurityEpoch?

    fun revokeAndIncrement(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): CredentialSecurityEpoch?
}

interface RevocationOutbox {
    fun append(event: RevocationOutboxEvent)
}

@JvmInline
value class RevocationEventId(val value: UUID)

data class RevocationOutboxEvent(
    val eventId: RevocationEventId,
    val accountId: AccountId,
    val credentialId: CredentialId?,
    val accountGeneration: Long,
    val credentialGeneration: Long?,
    val occurredAt: Instant,
    val deliveryDeadline: Instant
)

/**
 * Every authenticated resource request passes this fence before reads or writes.
 * Cache invalidation can accelerate cleanup but cannot replace these checks.
 */
class RevocationFence(
    private val accounts: AccountSecurityEpochRepository,
    private val credentials: CredentialSecurityEpochRepository
) {
    fun requireCurrent(
        session: CredentialBoundSession,
        grant: CapabilityGrant?,
        now: Instant
    ) {
        if (session.status != SessionStatus.ACTIVE || !now.isBefore(session.expiresAt)) {
            throw AuthorizationDenied("Session is inactive or expired.")
        }

        val account = accounts.find(session.accountId)
            ?: throw AuthorizationDenied("Account was not found.")

        if (
            account.status != AccountStatus.ACTIVE ||
            account.generation != session.accountGenerationAtIssue
        ) {
            throw AuthorizationDenied("Session is stale after account security change.")
        }

        val credential = credentials.find(
            session.accountId,
            session.credentialId
        ) ?: throw AuthorizationDenied("Credential was not found.")

        if (
            credential.status != CredentialStatus.ACTIVE ||
            credential.generation != session.credentialGenerationAtIssue
        ) {
            throw AuthorizationDenied("Session is stale after credential change.")
        }

        if (grant != null) {
            if (
                grant.status != CapabilityStatus.ACTIVE ||
                !now.isBefore(grant.expiresAt) ||
                grant.accountId != session.accountId ||
                grant.credentialId != session.credentialId ||
                grant.accountGenerationAtIssue != account.generation ||
                grant.credentialGenerationAtIssue != credential.generation
            ) {
                throw AuthorizationDenied("Capability grant is inactive or stale.")
            }
        }
    }
}

class CredentialRevocationCommand(
    private val accounts: AccountSecurityEpochRepository,
    private val credentials: CredentialSecurityEpochRepository,
    private val outbox: RevocationOutbox
) {
    fun revoke(
        accountId: AccountId,
        credentialId: CredentialId,
        now: Instant,
        deliveryDeadline: Instant
    ): RevocationOutboxEvent {
        val credential = credentials.revokeAndIncrement(
            accountId,
            credentialId,
            now
        ) ?: throw AuthorizationDenied("Credential was not found.")

        val account = accounts.increment(accountId, now)
            ?: throw AuthorizationDenied("Account was not found.")

        val event = RevocationOutboxEvent(
            eventId = RevocationEventId(UUID.randomUUID()),
            accountId = accountId,
            credentialId = credentialId,
            accountGeneration = account.generation,
            credentialGeneration = credential.generation,
            occurredAt = now,
            deliveryDeadline = deliveryDeadline
        )

        outbox.append(event)
        return event
    }
}
