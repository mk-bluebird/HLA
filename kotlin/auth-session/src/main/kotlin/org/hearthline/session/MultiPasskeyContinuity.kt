package org.hearthline.session

import java.time.Clock
import java.time.Instant
import java.util.UUID

enum class CredentialLifecycleStatus {
    ACTIVE,
    REVOKED,
    REPLACED
}

enum class CredentialRegistrationPurpose {
    PRIMARY_ACCESS,
    INDEPENDENT_BACKUP,
    ACCESSIBILITY_OPTION,
    REPLACEMENT_AFTER_LOSS
}

/**
 * Credential records live only in the credential authority.
 *
 * The `memberProvidedLabel` is optional and is selected by the member. It is
 * never a device-derived label. For example:
 *
 * - "Daily sign-in"
 * - "Backup passkey"
 * - "Security key"
 *
 * Do not auto-populate it with a device model, browser, operating system,
 * provider, location, or last-used pattern.
 */
data class AccountCredential(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray,
    val createdAt: Instant,
    val status: CredentialLifecycleStatus,
    val registrationPurpose: CredentialRegistrationPurpose,
    val memberProvidedLabel: String?,
    val revokedAt: Instant? = null,
    val replacedByCredentialId: CredentialId? = null
)

interface AccountCredentialRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): AccountCredential?

    fun save(credential: AccountCredential)

    fun listActive(accountId: AccountId): List<AccountCredential>

    fun revoke(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): Boolean

    fun markReplaced(
        accountId: AccountId,
        oldCredentialId: CredentialId,
        replacementCredentialId: CredentialId,
        replacedAt: Instant
    ): Boolean
}

interface ExistingCredentialAuthorization {
    /**
     * Must verify a fresh challenge using an existing ACTIVE credential before a
     * new credential may be added. The new credential being registered can never
     * authorize itself.
     */
    fun verifyExistingCredential(
        accountId: AccountId,
        existingCredentialId: CredentialId,
        challenge: ByteArray,
        assertion: ByteArray
    ): Boolean
}

class MultiPasskeyContinuityService(
    private val clock: Clock,
    private val credentials: AccountCredentialRepository,
    private val existingCredentialAuthorization: ExistingCredentialAuthorization
) {
    /**
     * Adding a passkey requires authorization from an already-active credential.
     *
     * This binds the new credential to the same local account without:
     * - asking a provider to identify the member;
     * - reading Apple/Google/Microsoft identity data;
     * - comparing device metadata;
     * - merging any other account;
     * - constructing a cross-platform credential graph.
     */
    fun addCredential(
        accountId: AccountId,
        authorizingCredentialId: CredentialId,
        authorizationChallenge: ByteArray,
        authorizationAssertion: ByteArray,
        newCredentialId: CredentialId,
        newPublicKeyCose: ByteArray,
        purpose: CredentialRegistrationPurpose,
        memberProvidedLabel: String?
    ): AccountCredential {
        require(memberProvidedLabel == null || memberProvidedLabel.length <= 64) {
            "Credential label is too long."
        }

        val authorized = existingCredentialAuthorization.verifyExistingCredential(
            accountId = accountId,
            existingCredentialId = authorizingCredentialId,
            challenge = authorizationChallenge,
            assertion = authorizationAssertion
        )

        if (!authorized) {
            throw ContinuityDenied(
                "An existing active sign-in credential must authorize this change."
            )
        }

        val existing = credentials.find(accountId, newCredentialId)
        if (existing != null) {
            throw ContinuityDenied(
                "This credential is already registered to this account."
            )
        }

        val newCredential = AccountCredential(
            accountId = accountId,
            credentialId = newCredentialId,
            publicKeyCose = newPublicKeyCose,
            createdAt = clock.instant(),
            status = CredentialLifecycleStatus.ACTIVE,
            registrationPurpose = purpose,
            memberProvidedLabel = memberProvidedLabel
        )

        credentials.save(newCredential)
        return newCredential
    }

    /**
     * Replaces an old credential only within the existing account.
     *
     * This is not a claim that the new credential belongs to the same human.
     * It is an account-authorized transition completed by an already active
     * credential under a fresh challenge.
     */
    fun replaceCredential(
        accountId: AccountId,
        authorizingCredentialId: CredentialId,
        authorizationChallenge: ByteArray,
        authorizationAssertion: ByteArray,
        oldCredentialId: CredentialId,
        replacementCredentialId: CredentialId,
        replacementPublicKeyCose: ByteArray,
        memberProvidedLabel: String?
    ): AccountCredential {
        val replacement = addCredential(
            accountId = accountId,
            authorizingCredentialId = authorizingCredentialId,
            authorizationChallenge = authorizationChallenge,
            authorizationAssertion = authorizationAssertion,
            newCredentialId = replacementCredentialId,
            newPublicKeyCose = replacementPublicKeyCose,
            purpose = CredentialRegistrationPurpose.REPLACEMENT_AFTER_LOSS,
            memberProvidedLabel = memberProvidedLabel
        )

        val marked = credentials.markReplaced(
            accountId = accountId,
            oldCredentialId = oldCredentialId,
            replacementCredentialId = replacementCredentialId,
            replacedAt = clock.instant()
        )

        if (!marked) {
            /*
             * Production implementations should run this operation transactionally
             * with the new credential insertion. If marking fails, roll back the
             * new credential or place it in a non-active provisional state.
             */
            throw ContinuityDenied(
                "Credential replacement could not be completed safely."
            )
        }

        return replacement
    }

    fun removeCredential(
        accountId: AccountId,
        credentialId: CredentialId
    ): Boolean {
        /*
         * The caller must already have completed fresh authorization with another
         * active credential or a recovery flow. Do not allow the last active
         * credential to be removed unless the member has an accessible recovery
         * route and clearly confirms the consequence.
         */
        val activeCredentials = credentials.listActive(accountId)

        if (activeCredentials.size <= 1 &&
            activeCredentials.any { it.credentialId == credentialId }
        ) {
            throw ContinuityDenied(
                "Add another sign-in method or establish recovery before removing the final passkey."
            )
        }

        return credentials.revoke(
            accountId = accountId,
            credentialId = credentialId,
            revokedAt = clock.instant()
        )
    }
}

class ContinuityDenied(message: String) : RuntimeException(message)
