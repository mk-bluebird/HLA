package org.hearthline.anchorline

import java.time.Instant

/**
 * This registry is scoped to one active credential ID. It contains no hardware,
 * device, browser, IP, location, provider, profile, age, or behavioral data.
 */
data class CredentialAccountBinding(
    val credentialId: CredentialId,
    val accountId: AccountId,
    val boundAt: Instant,
    val revokedAt: Instant?
) {
    fun isActive(): Boolean = revokedAt == null
}

interface CredentialAccountBindingRepository {
    /**
     * Must be backed by a database uniqueness constraint for active credential
     * IDs. An implementation must reject binding a credential that is already
     * active for another account.
     */
    fun bindIfUnbound(binding: CredentialAccountBinding): Boolean

    fun findActive(credentialId: CredentialId): CredentialAccountBinding?

    fun revoke(
        credentialId: CredentialId,
        accountId: AccountId,
        revokedAt: Instant
    ): Boolean
}

sealed interface CredentialBindingResult {
    data object Bound : CredentialBindingResult
    data object AlreadyBoundToSameAccount : CredentialBindingResult
    data object BoundToAnotherAccount : CredentialBindingResult
}

/**
 * A service in the authentication boundary only.
 *
 * No cross-account query is exposed to profile, discovery, matching,
 * recommendation, moderation, analytics, or support workflows.
 */
class CredentialUniquenessRegistry(
    private val bindings: CredentialAccountBindingRepository
) {
    fun bind(
        accountId: AccountId,
        credentialId: CredentialId,
        now: Instant
    ): CredentialBindingResult {
        val existing = bindings.findActive(credentialId)

        if (existing != null) {
            return if (existing.accountId == accountId) {
                CredentialBindingResult.AlreadyBoundToSameAccount
            } else {
                CredentialBindingResult.BoundToAnotherAccount
            }
        }

        val inserted = bindings.bindIfUnbound(
            CredentialAccountBinding(
                credentialId = credentialId,
                accountId = accountId,
                boundAt = now,
                revokedAt = null
            )
        )

        if (inserted) {
            return CredentialBindingResult.Bound
        }

        val concurrent = bindings.findActive(credentialId)
        return when {
            concurrent == null -> CredentialBindingResult.BoundToAnotherAccount
            concurrent.accountId == accountId ->
                CredentialBindingResult.AlreadyBoundToSameAccount
            else -> CredentialBindingResult.BoundToAnotherAccount
        }
    }
}
