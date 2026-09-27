package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class RevocationEventId(val value: UUID)

enum class RevocationReason {
    MEMBER_REMOVED_CREDENTIAL,
    MEMBER_SIGNED_OUT_EVERYWHERE,
    LOST_DEVICE_REPORTED,
    RECOVERY_COMPLETED,
    ACCOUNT_SECURITY_LOCK,
    ACCOUNT_CLOSURE,
    ADMINISTRATIVE_SECURITY_ACTION
}

data class CredentialRevocationState(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val active: Boolean,
    val generation: Long,
    val revokedAt: Instant?,
    val updatedAt: Instant
)

data class AccountRevocationState(
    val accountId: AccountId,
    val active: Boolean,
    val sessionGeneration: Long,
    val updatedAt: Instant
)

data class RevocationEvent(
    val eventId: RevocationEventId,
    val accountId: AccountId,
    val credentialId: CredentialId?,
    val reason: RevocationReason,
    val credentialGenerationAfterChange: Long?,
    val accountSessionGenerationAfterChange: Long?,
    val occurredAt: Instant
)

interface CredentialRevocationStateRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): CredentialRevocationState?

    /**
     * Must atomically set active=false and increment generation.
     */
    fun revokeAndIncrement(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): CredentialRevocationState?
}

interface AccountRevocationStateRepository {
    fun find(accountId: AccountId): AccountRevocationState?

    /**
     * Must atomically increment sessionGeneration.
     */
    fun incrementSessionGeneration(
        accountId: AccountId,
        at: Instant
    ): AccountRevocationState?
}

interface RevocationOutbox {
    /**
     * Must be written in the same transaction as the authoritative state
     * mutation. Workers may retry delivery; consumers must be idempotent.
     */
    fun append(event: RevocationEvent)
}

interface SessionRevocationWorker {
    fun revokeSessionsForCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): Int

    fun revokeAllAccountSessions(
        accountId: AccountId,
        at: Instant
    ): Int
}

interface CapabilityGrantRevocationWorker {
    fun revokeForCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): Int

    fun revokeForAccount(
        accountId: AccountId,
        at: Instant
    ): Int
}

interface ExportRevocationWorker {
    fun cancelForCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): Int

    fun cancelForAccount(
        accountId: AccountId,
        at: Instant
    ): Int
}

interface AuthorizationCacheInvalidator {
    fun invalidateCredential(
        accountId: AccountId,
        credentialId: CredentialId
    )

    fun invalidateAccount(accountId: AccountId)
}

/**
 * The command path makes live state authoritative before background cleanup.
 */
class CredentialRevocationService(
    private val clock: Clock,
    private val credentials: CredentialRevocationStateRepository,
    private val accounts: AccountRevocationStateRepository,
    private val outbox: RevocationOutbox,
    private val cache: AuthorizationCacheInvalidator
) {
    fun revokeCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        reason: RevocationReason
    ): RevocationEvent {
        val now = clock.instant()

        val credential = credentials.revokeAndIncrement(
            accountId = accountId,
            credentialId = credentialId,
            at = now
        ) ?: throw AuthorizationDenied("Credential was not found.")

        /*
         * The account epoch changes too. This makes every pre-existing session
         * subject to live epoch rejection, while asynchronous workers clean up.
         */
        val account = accounts.incrementSessionGeneration(accountId, now)
            ?: throw AuthorizationDenied("Account was not found.")

        val event = RevocationEvent(
            eventId = RevocationEventId(UUID.randomUUID()),
            accountId = accountId,
            credentialId = credentialId,
            reason = reason,
            credentialGenerationAfterChange = credential.generation,
            accountSessionGenerationAfterChange = account.sessionGeneration,
            occurredAt = now
        )

        outbox.append(event)
        cache.invalidateCredential(accountId, credentialId)
        cache.invalidateAccount(accountId)

        return event
    }

    fun revokeAllSessions(
        accountId: AccountId,
        reason: RevocationReason
    ): RevocationEvent {
        val now = clock.instant()

        val account = accounts.incrementSessionGeneration(accountId, now)
            ?: throw AuthorizationDenied("Account was not found.")

        val event = RevocationEvent(
            eventId = RevocationEventId(UUID.randomUUID()),
            accountId = accountId,
            credentialId = null,
            reason = reason,
            credentialGenerationAfterChange = null,
            accountSessionGenerationAfterChange = account.sessionGeneration,
            occurredAt = now
        )

        outbox.append(event)
        cache.invalidateAccount(accountId)

        return event
    }
}

/**
 * Background consumers are cleanup accelerators, not security authorities.
 * Security-sensitive endpoints continue to perform synchronous state checks.
 */
class RevocationPropagationWorker(
    private val sessions: SessionRevocationWorker,
    private val capabilities: CapabilityGrantRevocationWorker,
    private val exports: ExportRevocationWorker
) {
    fun consume(event: RevocationEvent) {
        val credentialId = event.credentialId

        if (credentialId != null) {
            sessions.revokeSessionsForCredential(
                event.accountId,
                credentialId,
                event.occurredAt
            )

            capabilities.revokeForCredential(
                event.accountId,
                credentialId,
                event.occurredAt
            )

            exports.cancelForCredential(
                event.accountId,
                credentialId,
                event.occurredAt
            )
        } else {
            sessions.revokeAllAccountSessions(
                event.accountId,
                event.occurredAt
            )

            capabilities.revokeForAccount(
                event.accountId,
                event.occurredAt
            )

            exports.cancelForAccount(
                event.accountId,
                event.occurredAt
            )
        }
    }
}
