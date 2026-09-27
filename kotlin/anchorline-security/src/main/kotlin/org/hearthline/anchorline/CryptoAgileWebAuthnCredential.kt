package org.hearthline.anchorline

import java.time.Instant

/**
 * COSE algorithm values are stored as protocol identifiers, not as a product
 * quality or member-security classification.
 */
data class WebAuthnCredentialAlgorithm(
    val coseAlgorithmId: Long,
    val protocolName: String,
    val verifierFamily: VerifierFamily
) {
    init {
        require(protocolName.isNotBlank() && protocolName.length <= 128) {
            "Protocol algorithm name must be present and bounded."
        }
    }
}

enum class VerifierFamily {
    CLASSICAL_ECDSA,
    CLASSICAL_EDDSA,
    POST_QUANTUM_ML_DSA,
    POST_QUANTUM_SLH_DSA,
    HYBRID_CLASSICAL_POST_QUANTUM
}

/**
 * The credential record remains scoped to the authentication boundary.
 * Algorithm metadata cannot egress to social-product systems.
 */
data class CryptoAgileWebAuthnCredential(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray,
    val algorithm: WebAuthnCredentialAlgorithm,
    val createdAt: Instant,
    val status: CredentialStatus,
    val generation: Long,
    val deprecatedAfter: Instant?,
    val revokedAt: Instant?
)

interface CryptoAgileCredentialRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): CryptoAgileWebAuthnCredential?

    fun save(credential: CryptoAgileWebAuthnCredential)

    fun listActive(accountId: AccountId): List<CryptoAgileWebAuthnCredential>
}

interface AlgorithmAgileAssertionVerifier {
    fun verify(
        credential: CryptoAgileWebAuthnCredential,
        expectedChallenge: ByteArray,
        assertion: WebAuthnAssertion
    ): Boolean
}

/**
 * An algorithm registry supports current and future verifier implementations.
 * It does not assign security tiers to members.
 */
class WebAuthnAlgorithmRegistry(
    private val supportedAlgorithms: Map<Long, WebAuthnCredentialAlgorithm>
) {
    fun requireSupported(coseAlgorithmId: Long): WebAuthnCredentialAlgorithm =
        supportedAlgorithms[coseAlgorithmId]
            ?: throw AuthorizationDenied(
                "This credential algorithm is not currently supported."
            )

    fun isSupported(coseAlgorithmId: Long): Boolean =
        supportedAlgorithms.containsKey(coseAlgorithmId)
}

/**
 * Migration adds a new credential; it does not mutate or silently replace an
 * existing credential. Existing compatible credentials remain usable until a
 * separately approved deprecation plan takes effect.
 */
class PostQuantumCredentialMigrationService(
    private val credentials: CryptoAgileCredentialRepository,
    private val algorithmRegistry: WebAuthnAlgorithmRegistry,
    private val existingCredentialAuthorizer: ExistingCredentialFreshAuthorizationVerifier
) {
    fun addMemberAuthorizedCredential(
        accountId: AccountId,
        authorizingCredentialId: CredentialId,
        authorizationChallenge: ByteArray,
        authorizationAssertion: ByteArray,
        newCredential: CryptoAgileWebAuthnCredential
    ) {
        require(newCredential.accountId == accountId) {
            "New credential must belong to the authorized local account."
        }

        val authorized = existingCredentialAuthorizer.verify(
            accountId = accountId,
            credentialId = authorizingCredentialId,
            challenge = authorizationChallenge.copyOf(),
            assertion = authorizationAssertion.copyOf()
        )

        if (!authorized) {
            throw AuthorizationDenied(
                "A fresh active credential authorization is required."
            )
        }

        algorithmRegistry.requireSupported(
            newCredential.algorithm.coseAlgorithmId
        )

        if (credentials.find(accountId, newCredential.credentialId) != null) {
            throw AuthorizationDenied(
                "Credential is already registered to this account."
            )
        }

        credentials.save(newCredential)
    }
}
