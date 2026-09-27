package org.hearthline.authintegrity

import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Public-registration policy.
 *
 * Design constraints:
 *
 * - Hearthline accepts standards-compliant passkeys with user verification.
 * - Hearthline does not require a particular authenticator manufacturer,
 *   hardware security module, Secure Enclave, StrongBox, TPM, or operating system.
 * - `attestation = none` is intentional: Hearthline avoids collecting
 *   authenticator provenance and potentially identifying attestation certificates.
 * - No AAGUID allowlist or denylist is evaluated for ordinary member registration.
 * - No attestation metadata becomes a member attribute or product signal.
 */
class WebAuthnRegistrationPolicy(
    private val clock: Clock,
    private val random: SecureRandom = SecureRandom()
) {
    fun createPublicRegistrationOptions(
        accountId: AccountId,
        username: String,
        displayName: String,
        excludeCredentialIds: List<CredentialId>
    ): PublicKeyCredentialCreationOptions {
        val challenge = ByteArray(32).also(random::nextBytes)

        return PublicKeyCredentialCreationOptions(
            challenge = challenge,
            rp = RelyingParty(
                id = "auth.hearthline.example",
                name = "Hearthline"
            ),
            user = PublicKeyCredentialUserEntity(
                id = accountId.value.toString().toByteArray(Charsets.UTF_8),
                name = username,
                displayName = displayName
            ),
            pubKeyCredParams = listOf(
                PublicKeyCredentialParameter(
                    type = PublicKeyCredentialType.PUBLIC_KEY,
                    alg = -7
                ),
                PublicKeyCredentialParameter(
                    type = PublicKeyCredentialType.PUBLIC_KEY,
                    alg = -257
                )
            ),
            timeoutMillis = 300_000,
            authenticatorSelection = AuthenticatorSelectionCriteria(
                residentKey = ResidentKeyRequirement.REQUIRED,
                userVerification = UserVerificationRequirement.REQUIRED
            ),

            /*
             * Required Hearthline default:
             *
             * `none` deliberately requests no authenticator provenance evidence.
             * It provides the broadest consumer compatibility and does not create
             * a hardware-type eligibility barrier.
             */
            attestation = AttestationConveyancePreference.NONE,

            excludeCredentials = excludeCredentialIds.map {
                PublicKeyCredentialDescriptor(
                    type = PublicKeyCredentialType.PUBLIC_KEY,
                    id = it.value
                )
            },

            extensions = mapOf(
                /*
                 * CredProtect is optional. It asks supporting authenticators to
                 * require user verification before credential use.
                 *
                 * It is not a hardware test and must not become a trust tier.
                 */
                "credProtect" to "userVerificationRequired"
            )
        )
    }

    fun createRegistrationChallengeRecord(
        accountId: AccountId,
        options: PublicKeyCredentialCreationOptions
    ): RegistrationChallengeRecord {
        val now = clock.instant()

        return RegistrationChallengeRecord(
            challengeId = UUID.randomUUID(),
            accountId = accountId,
            challengeBase64Url = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(options.challenge),
            issuedAt = now,
            expiresAt = now.plusSeconds(300),
            attestationPolicy = AttestationConveyancePreference.NONE,
            consumedAt = null
        )
    }
}

data class RegistrationChallengeRecord(
    val challengeId: UUID,
    val accountId: AccountId,
    val challengeBase64Url: String,
    val issuedAt: Instant,
    val expiresAt: Instant,

    /*
     * This describes the policy used for the ceremony, not a member property.
     * Delete it with the challenge record after expiry / consumption.
     */
    val attestationPolicy: AttestationConveyancePreference,

    val consumedAt: Instant?
)

data class PublicKeyCredentialCreationOptions(
    val challenge: ByteArray,
    val rp: RelyingParty,
    val user: PublicKeyCredentialUserEntity,
    val pubKeyCredParams: List<PublicKeyCredentialParameter>,
    val timeoutMillis: Long,
    val authenticatorSelection: AuthenticatorSelectionCriteria,
    val attestation: AttestationConveyancePreference,
    val excludeCredentials: List<PublicKeyCredentialDescriptor>,
    val extensions: Map<String, String>
)

data class RelyingParty(
    val id: String,
    val name: String
)

data class PublicKeyCredentialUserEntity(
    val id: ByteArray,
    val name: String,
    val displayName: String
)

data class PublicKeyCredentialParameter(
    val type: PublicKeyCredentialType,
    val alg: Int
)

data class PublicKeyCredentialDescriptor(
    val type: PublicKeyCredentialType,
    val id: ByteArray
)

data class AuthenticatorSelectionCriteria(
    val residentKey: ResidentKeyRequirement,
    val userVerification: UserVerificationRequirement
)

enum class PublicKeyCredentialType {
    PUBLIC_KEY
}

enum class ResidentKeyRequirement {
    REQUIRED,
    PREFERRED,
    DISCOURAGED
}

enum class UserVerificationRequirement {
    REQUIRED,
    PREFERRED,
    DISCOURAGED
}

enum class AttestationConveyancePreference {
    NONE,
    INDIRECT,
    DIRECT,
    ENTERPRISE
}
