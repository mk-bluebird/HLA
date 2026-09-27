package org.hearthline.anchorline

import java.time.Clock
import java.time.Instant

/**
 * This service accepts a normal WebAuthn registration result. It intentionally
 * has no imported/exported, provider, migration, device, platform, or
 * credential-manager metadata field.
 */
data class PortablePasskeyRegistration(
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray
) {
    init {
        require(publicKeyCose.isNotEmpty()) {
            "Credential public key cannot be empty."
        }
    }
}

interface FreshCredentialAdditionAuthorizer {
    /**
     * Verifies a fresh assertion from an already active credential belonging to
     * the account. The newly added credential cannot authorize itself.
     */
    fun verifyExistingCredentialAuthorization(
        accountId: AccountId,
        authorizingCredentialId: CredentialId,
        challenge: ByteArray,
        assertion: ByteArray
    ): Boolean
}

interface PortableCredentialWriter {
    fun addCredential(
        accountId: AccountId,
        registration: PortablePasskeyRegistration,
        createdAt: Instant
    )
}

/**
 * Adds a passkey regardless of whether it was created locally, synchronized,
 * restored, or transferred through a credential-provider protocol.
 *
 * No origin/migration status is requested, accepted, retained, or emitted.
 */
class PortableCredentialRegistrationService(
    private val clock: Clock,
    private val authorizer: FreshCredentialAdditionAuthorizer,
    private val uniqueness: CredentialUniquenessRegistry,
    private val writer: PortableCredentialWriter
) {
    fun addToExistingAccount(
        accountId: AccountId,
        authorizingCredentialId: CredentialId,
        challenge: ByteArray,
        assertion: ByteArray,
        registration: PortablePasskeyRegistration
    ) {
        require(challenge.size in 16..1024) {
            "Credential-addition challenge must be bounded."
        }

        val authorized = authorizer.verifyExistingCredentialAuthorization(
            accountId = accountId,
            authorizingCredentialId = authorizingCredentialId,
            challenge = challenge.copyOf(),
            assertion = assertion.copyOf()
        )

        if (!authorized) {
            throw AuthorizationDenied(
                "A fresh existing passkey authorization is required."
            )
        }

        when (
            uniqueness.bind(
                accountId = accountId,
                credentialId = registration.credentialId,
                now = clock.instant()
            )
        ) {
            CredentialBindingResult.Bound,
            CredentialBindingResult.AlreadyBoundToSameAccount -> {
                writer.addCredential(
                    accountId = accountId,
                    registration = registration,
                    createdAt = clock.instant()
                )
            }

            CredentialBindingResult.BoundToAnotherAccount -> {
                throw AuthorizationDenied(
                    "This sign-in credential cannot be added to this account."
                )
            }
        }
    }
}
