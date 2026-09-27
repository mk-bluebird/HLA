package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class CapabilityGrantId(val value: UUID)

enum class CapabilityType {
    ADD_PASSKEY,
    REMOVE_PASSKEY,
    START_EXPORT,
    DOWNLOAD_EXPORT,
    CHANGE_RECOVERY,
    COMPLETE_RECOVERY,
    DELETE_ACCOUNT
}

enum class CapabilityStatus {
    ACTIVE,
    CONSUMED,
    REVOKED,
    EXPIRED
}

data class RevocableCapabilityGrant(
    val grantId: CapabilityGrantId,
    val accountId: AccountId,
    val credentialId: CredentialId,
    val credentialGenerationAtIssue: Long,
    val accountSessionGenerationAtIssue: Long,
    val capability: CapabilityType,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: CapabilityStatus,
    val consumedAt: Instant? = null,
    val revokedAt: Instant? = null
)

interface RevocableCapabilityGrantRepository {
    fun save(grant: RevocableCapabilityGrant)

    /**
     * Atomically consumes an active grant only once.
     */
    fun consume(grantId: CapabilityGrantId, at: Instant): RevocableCapabilityGrant?

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

class CapabilityGrantAuthorizer(
    private val accounts: AccountRevocationStateRepository,
    private val credentials: CredentialRevocationStateRepository
) {
    fun requireCurrent(
        grant: RevocableCapabilityGrant,
        now: Instant
    ) {
        if (grant.status != CapabilityStatus.ACTIVE) {
            throw AuthorizationDenied("Capability grant is not active.")
        }

        if (now >= grant.expiresAt) {
            throw AuthorizationDenied("Capability grant expired.")
        }

        val account = accounts.find(grant.accountId)
            ?: throw AuthorizationDenied("Account was not found.")

        if (
            !account.active ||
            account.sessionGeneration != grant.accountSessionGenerationAtIssue
        ) {
            throw AuthorizationDenied("Capability grant is stale.")
        }

        val credential = credentials.find(grant.accountId, grant.credentialId)
            ?: throw AuthorizationDenied("Credential was not found.")

        if (
            !credential.active ||
            credential.generation != grant.credentialGenerationAtIssue
        ) {
            throw AuthorizationDenied("Capability grant is stale.")
        }
    }
}
