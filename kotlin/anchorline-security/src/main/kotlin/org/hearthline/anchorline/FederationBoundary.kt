package org.hearthline.anchorline

import java.time.Clock
import java.time.Instant
import java.util.UUID

enum class FederatedProvider {
    APPLE,
    GOOGLE,
    MICROSOFT
}

data class OidcClientConfiguration(
    val provider: FederatedProvider,
    val issuerUri: String,
    val clientId: String,
    val redirectUri: String,
    val scopes: Set<String>,
    val environment: DeploymentEnvironment
) {
    init {
        require(scopes == setOf("openid")) {
            "Consumer federation requests only the openid scope by default."
        }

        require(redirectUri.startsWith("https://")) {
            "OIDC redirect URI must use HTTPS."
        }
    }
}

enum class DeploymentEnvironment {
    PRODUCTION,
    STAGING,
    TEST
}

/**
 * Encrypted wrapper. The ciphertext deliberately cannot be inspected by any
 * caller outside the authentication boundary.
 */
data class EncryptedValue(
    val ciphertext: ByteArray,
    val keyVersion: Int
)

interface FederationSubjectCipher {
    fun encrypt(plaintext: ByteArray): EncryptedValue
    fun decrypt(value: EncryptedValue): ByteArray
}

/**
 * INV-FED-02:
 *
 * The durable model contains only:
 * binding ID, local account ID, issuer, encrypted pairwise subject, client ID,
 * lifecycle timestamps/status, and encryption key version.
 *
 * It contains no email, name, photo, phone, contacts, provider account age,
 * risk score, organization, graph, device data, token, tenant, or Microsoft oid.
 */
data class FederatedLoginBinding(
    val bindingId: FederationBindingId,
    val accountId: AccountId,
    val issuerUri: String,
    val encryptedPairwiseSubject: EncryptedValue,
    val clientId: String,
    val createdAt: Instant,
    val lastSuccessfulUseAt: Instant?,
    val status: FederationBindingStatus
)

interface FederatedLoginBindingRepository {
    fun save(binding: FederatedLoginBinding)

    /**
     * This lookup exists only in the authentication boundary and is keyed by
     * issuer plus encrypted pairwise-subject equality after controlled decrypt/
     * compare. It returns at most the local account binding for this issuer/client.
     */
    fun findByIssuerAndPairwiseSubject(
        issuerUri: String,
        subject: ByteArray,
        cipher: FederationSubjectCipher
    ): FederatedLoginBinding?

    fun findForAccount(
        accountId: AccountId,
        bindingId: FederationBindingId
    ): FederatedLoginBinding?

    fun disable(
        accountId: AccountId,
        bindingId: FederationBindingId,
        at: Instant
    ): Boolean
}

data class VerifiedOidcSubject(
    val issuerUri: String,
    val pairwiseSubject: ByteArray
)

/**
 * The OIDC verifier must validate Authorization Code + PKCE, issuer signature,
 * audience, nonce, state, expiry, authorization-code binding, and redirect URI.
 *
 * It releases only issuer and pairwise `sub` after verification.
 */
interface OidcAuthorizationCodeVerifier {
    fun verifyAuthorizationCodeWithPkce(
        configuration: OidcClientConfiguration,
        authorizationCode: String,
        pkceVerifier: String,
        expectedState: String,
        expectedNonce: String
    ): VerifiedOidcSubject
}

class OptionalFederationService(
    private val clock: Clock,
    private val bindings: FederatedLoginBindingRepository,
    private val cipher: FederationSubjectCipher
) {
    /**
     * INV-FED-01:
     * Federation is optional. This method creates a binding only after a local
     * account already exists and the member explicitly chooses it.
     */
    fun bindToExistingAccount(
        accountId: AccountId,
        configuration: OidcClientConfiguration,
        verifiedSubject: VerifiedOidcSubject
    ): FederatedLoginBinding {
        require(configuration.issuerUri == verifiedSubject.issuerUri) {
            "OIDC issuer does not match the configured issuer."
        }

        val now = clock.instant()
        val existing = bindings.findByIssuerAndPairwiseSubject(
            issuerUri = verifiedSubject.issuerUri,
            subject = verifiedSubject.pairwiseSubject,
            cipher = cipher
        )

        /*
         * INV-FED-03:
         *
         * A provider-subject collision is never an account-merge trigger.
         * It is a neutral refusal that requires the member to sign into the
         * already-bound local account or use the explicit account-consolidation
         * procedure later.
         */
        if (existing != null && existing.accountId != accountId) {
            throw AuthorizationDenied(
                "This optional provider sign-in is already connected to a different " +
                    "Hearthline account. Accounts are never merged automatically."
            )
        }

        return FederatedLoginBinding(
            bindingId = FederationBindingId(UUID.randomUUID()),
            accountId = accountId,
            issuerUri = verifiedSubject.issuerUri,
            encryptedPairwiseSubject = cipher.encrypt(
                verifiedSubject.pairwiseSubject.copyOf()
            ),
            clientId = configuration.clientId,
            createdAt = now,
            lastSuccessfulUseAt = null,
            status = FederationBindingStatus.ACTIVE
        ).also(bindings::save)
    }

    /**
     * This supports optional sign-in convenience only. The caller must still
     * establish Hearthline local session policy and can require a local passkey
     * before sensitive operations.
     */
    fun authorizeOptionalFederatedSignIn(
        configuration: OidcClientConfiguration,
        verifiedSubject: VerifiedOidcSubject
    ): AccountId {
        val binding = bindings.findByIssuerAndPairwiseSubject(
            issuerUri = verifiedSubject.issuerUri,
            subject = verifiedSubject.pairwiseSubject,
            cipher = cipher
        ) ?: throw AuthorizationDenied(
            "No active Hearthline account is connected to this optional provider sign-in."
        )

        if (binding.status != FederationBindingStatus.ACTIVE) {
            throw AuthorizationDenied("This optional provider sign-in is disabled.")
        }

        return binding.accountId
    }
}

/**
 * Structural guard against accidentally adding provider data to a binding DTO.
 */
object FederationDataMinimization {
    private val prohibitedProviderClaims = setOf(
        "email",
        "email_verified",
        "name",
        "given_name",
        "family_name",
        "picture",
        "locale",
        "phone_number",
        "address",
        "provider_account_age",
        "provider_risk_score",
        "provider_security_events",
        "provider_device_information",
        "provider_authentication_history",
        "provider_contacts",
        "provider_social_graph",
        "provider_tenant_membership",
        "provider_account_reputation",
        "oid",
        "access_token",
        "refresh_token",
        "id_token"
    )

    fun rejectUnexpectedClaims(claimNames: Set<String>) {
        val violations = claimNames.intersect(prohibitedProviderClaims)
        if (violations.isNotEmpty()) {
            throw BoundaryViolation(
                "Federation claim collection is prohibited: " +
                    violations.sorted().joinToString()
            )
        }
    }
}
