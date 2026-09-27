package org.hearthline.anchorline

import java.time.Instant

data class ActiveCredentialBinding(
    val credentialId: CredentialId,
    val accountId: AccountId,
    val boundAt: Instant,
    val revokedAt: Instant?
) {
    fun active(): Boolean = revokedAt == null
}

interface ActiveCredentialBindingRepository {
    fun findForCredential(
        credentialId: CredentialId
    ): ActiveCredentialBinding?

    /**
     * Requires a database uniqueness constraint on active credential IDs.
     */
    fun bindIfUnbound(binding: ActiveCredentialBinding): Boolean

    fun revoke(
        credentialId: CredentialId,
        accountId: AccountId,
        at: Instant
    ): Boolean
}

sealed interface CredentialBindingResult {
    data object Bound : CredentialBindingResult
    data object AlreadyBoundToThisAccount : CredentialBindingResult
    data object Unavailable : CredentialBindingResult
}

/**
 * This service intentionally has no method that reveals a conflicting account.
 * A credential conflict is an authentication-state conflict, not a duplicate-
 * person determination.
 */
class AccountLocalCredentialBindingService(
    private val bindings: ActiveCredentialBindingRepository
) {
    fun bind(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): CredentialBindingResult {
        val existing = bindings.findForCredential(credentialId)

        if (existing != null) {
            return if (existing.accountId == accountId) {
                CredentialBindingResult.AlreadyBoundToThisAccount
            } else {
                CredentialBindingResult.Unavailable
            }
        }

        val inserted = bindings.bindIfUnbound(
            ActiveCredentialBinding(
                credentialId = credentialId,
                accountId = accountId,
                boundAt = at,
                revokedAt = null
            )
        )

        return if (inserted) {
            CredentialBindingResult.Bound
        } else {
            CredentialBindingResult.Unavailable
        }
    }
}
