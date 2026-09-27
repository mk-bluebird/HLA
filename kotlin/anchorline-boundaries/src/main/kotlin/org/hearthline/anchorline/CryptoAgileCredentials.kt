package org.hearthline.anchorline

import java.time.Instant

data class WebAuthnAlgorithm(
    val coseAlgorithmId: Long,
    val protocolName: String
) {
    init {
        require(protocolName.isNotBlank())
        require(protocolName.length <= 128)
    }
}

data class CryptoAgileCredential(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray,
    val algorithm: WebAuthnAlgorithm,
    val createdAt: Instant,
    val status: CredentialStatus,
    val generation: Long,
    val revokedAt: Instant?
) {
    init {
        require(publicKeyCose.isNotEmpty())
    }
}

interface WebAuthnAlgorithmRegistry {
    fun isSupported(coseAlgorithmId: Long): Boolean
}

interface MemberAuthorizedCredentialAddition {
    fun verifyExistingCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        challenge: ByteArray,
        assertion: ByteArray
    ): Boolean
}

interface CryptoAgileCredentialRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): CryptoAgileCredential?

    fun save(value: CryptoAgileCredential)
}

/**
 * Migration adds a standard credential only after fresh authorization.
 * It has no migration-source, provider, hardware, import/export, or algorithm
 * quality field and does not alter any social-product state.
 */
class CryptoAgileCredentialRegistrationService(
    private val algorithms: WebAuthnAlgorithmRegistry,
    private val authorizer: MemberAuthorizedCredentialAddition,
    private val credentials: CryptoAgileCredentialRepository
) {
    fun add(
        accountId: AccountId,
        authorizingCredentialId: CredentialId,
        challenge: ByteArray,
        assertion: ByteArray,
        credential: CryptoAgileCredential
    ) {
        require(credential.accountId == accountId)
        require(algorithms.isSupported(credential.algorithm.coseAlgorithmId))

        val authorized = authorizer.verifyExistingCredential(
            accountId,
            authorizingCredentialId,
            challenge.copyOf(),
            assertion.copyOf()
        )

        if (!authorized) {
            throw AuthorizationDenied(
                "Fresh authorization from an active credential is required."
            )
        }

        if (credentials.find(accountId, credential.credentialId) != null) {
            throw AuthorizationDenied("Credential is already registered.")
        }

        credentials.save(credential)
    }
}
