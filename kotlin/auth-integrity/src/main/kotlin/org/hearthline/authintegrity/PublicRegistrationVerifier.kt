package org.hearthline.authintegrity

import java.time.Clock
import java.time.Instant

/**
 * Verifies the public, privacy-preserving registration path.
 *
 * This verifier rejects malformed ceremonies and accepts only valid WebAuthn
 * credentials with required user verification. It intentionally does not
 * receive attestation certificates, AAGUIDs, hardware claims, device data, or
 * credential-manager metadata.
 */
class PublicRegistrationVerifier(
    private val clock: Clock,
    private val challengeStore: RegistrationChallengeStore,
    private val webAuthnRegistrationVerifier: WebAuthnRegistrationVerifier,
    private val credentialRepository: CredentialRepository,
    private val securityHistory: SecurityHistoryRepository
) {
    fun finishRegistration(
        accountId: AccountId,
        challengeId: String,
        response: WebAuthnRegistrationResponse
    ): RegisteredCredential {
        val now = clock.instant()

        val challenge = challengeStore.consume(challengeId, now)
            ?: throw RegistrationDenied("Registration challenge was not found or was already used.")

        if (challenge.accountId != accountId) {
            throw RegistrationDenied("Registration challenge belongs to a different account.")
        }

        if (now >= challenge.expiresAt) {
            throw RegistrationDenied("Registration challenge has expired.")
        }

        /*
         * This must validate:
         *
         * - challenge
         * - RP ID
         * - expected origin
         * - clientDataJSON.type == "webauthn.create"
         * - authenticator data RP ID hash
         * - required user verification
         * - signature / attestation-object structure sufficient for credential
         *   creation under `attestation = none`
         * - credential public key algorithm
         * - duplicate credential handling
         *
         * It must NOT extract, store, or enrich account state with:
         *
         * - AAGUID
         * - attestation certificate chain
         * - manufacturer/model
         * - secure hardware claim
         * - TPM / StrongBox / Secure Enclave result
         * - device identifier
         * - backup status
         * - credential-manager provider
         */
        val verified = webAuthnRegistrationVerifier.verifyNoneAttestationRegistration(
            expectedChallenge = challenge.challengeBase64Url,
            expectedRpId = "auth.hearthline.example",
            expectedOrigin = response.origin,
            response = response
        )

        val credential = RegisteredCredential(
            accountId = accountId,
            credentialId = verified.credentialId,
            publicKeyCose = verified.publicKeyCose,
            createdAt = now,
            status = CredentialStatus.ACTIVE
        )

        credentialRepository.save(credential)

        securityHistory.append(
            SecurityHistoryReceipt(
                accountId = accountId,
                eventKind = SecurityEventKind.PASSKEY_REGISTERED,
                actionClass = ProtectedActionClass.REGISTRATION_FINALIZE,
                outcome = "passkey_registered",
                occurredAt = now,
                expiresAt = now.plusSeconds(
                    AuthIntegrityBoundary.MEMBER_VISIBLE_SECURITY_EVENT_TTL_SECONDS
                )
            )
        )

        return credential
    }
}

interface RegistrationChallengeStore {
    fun consume(challengeId: String, consumedAt: Instant): RegistrationChallengeRecord?
}

interface WebAuthnRegistrationVerifier {
    fun verifyNoneAttestationRegistration(
        expectedChallenge: String,
        expectedRpId: String,
        expectedOrigin: String,
        response: WebAuthnRegistrationResponse
    ): VerifiedRegistration
}

data class WebAuthnRegistrationResponse(
    val credentialId: CredentialId,
    val clientDataJson: ByteArray,
    val attestationObject: ByteArray,
    val origin: String
)

data class VerifiedRegistration(
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray
)

class RegistrationDenied(message: String) : RuntimeException(message)
