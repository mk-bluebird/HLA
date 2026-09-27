package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class AttestationConveyance {
    NONE,
    INDIRECT,
    DIRECT,
    ENTERPRISE
}

enum class UserVerificationRequirement {
    REQUIRED,
    PREFERRED,
    DISCOURAGED
}

enum class ResidentKeyRequirement {
    REQUIRED,
    PREFERRED,
    DISCOURAGED
}

/**
 * GATE-HW-03:
 *
 * A consumer registration policy must use NONE. Other values are represented
 * solely so that a policy validator can reject them. Their existence does not
 * authorize their use in this application.
 */
object ConsumerRegistrationAttestationPolicy {
    const val REQUIRED_VALUE = AttestationConveyance.NONE

    fun requireAllowed(value: AttestationConveyance) {
        if (value != REQUIRED_VALUE) {
            throw BoundaryViolation(
                "Consumer WebAuthn registration must use attestation='none'. " +
                    "Indirect, direct, and enterprise conveyance are prohibited."
            )
        }
    }
}

/**
 * This is the complete durable credential-registration model.
 *
 * GATE-HW-04:
 * It contains only account ID, credential ID, public key, timestamps,
 * credential state, and a server-maintained revocation generation.
 *
 * It cannot persist:
 * - AAGUID
 * - attestation statement
 * - certificate chain
 * - authenticator model/manufacturer
 * - Secure Enclave / StrongBox / TPM status
 * - platform, operating system, or device class
 * - credential manager/provider
 * - backup eligibility/state
 */
data class RegisteredPasskey(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray,
    val createdAt: Instant,
    val status: CredentialStatus,
    val generation: Long,
    val revokedAt: Instant? = null,
    val replacedAt: Instant? = null
)

data class ConsumerRegistrationOptions(
    val challenge: ByteArray,
    val rpId: String,
    val rpName: String,
    val accountId: AccountId,
    val username: String,
    val displayName: String,
    val timeoutMillis: Long,
    val residentKey: ResidentKeyRequirement,
    val userVerification: UserVerificationRequirement,
    val attestation: AttestationConveyance,
    val excludedCredentialIds: List<CredentialId>
) {
    init {
        ConsumerRegistrationAttestationPolicy.requireAllowed(attestation)
        require(timeoutMillis in 1_000..300_000) {
            "Registration timeout must be between 1 second and 5 minutes."
        }
    }
}

data class RegistrationChallenge(
    val challengeId: ChallengeId,
    val accountId: AccountId,
    val challenge: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant? = null
)

interface RegistrationChallengeRepository {
    fun save(challenge: RegistrationChallenge)
    fun consume(challengeId: ChallengeId, at: Instant): RegistrationChallenge?
    fun deleteExpired(now: Instant): Int
}

interface PasskeyRepository {
    fun save(passkey: RegisteredPasskey)
    fun find(accountId: AccountId, credentialId: CredentialId): RegisteredPasskey?
    fun listActive(accountId: AccountId): List<RegisteredPasskey>
    fun revokeAndIncrement(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): RegisteredPasskey?
}

class PublicRegistrationService(
    private val clock: Clock,
    private val challenges: RegistrationChallengeRepository,
    private val passkeys: PasskeyRepository
) {
    companion object {
        private val CHALLENGE_TTL: Duration = Duration.ofMinutes(5)
    }

    /**
     * GATE-HW-01:
     * Every public registration option is created with attestation NONE.
     */
    fun begin(
        accountId: AccountId,
        username: String,
        displayName: String
    ): ConsumerRegistrationOptions {
        val now = clock.instant()
        val challenge = RegistrationChallenge(
            challengeId = ChallengeId(UUID.randomUUID()),
            accountId = accountId,
            challenge = CryptoSupport.randomBytes(32),
            issuedAt = now,
            expiresAt = now.plus(CHALLENGE_TTL)
        )
        challenges.save(challenge)

        return ConsumerRegistrationOptions(
            challenge = challenge.challenge,
            rpId = "auth.hearthline.example",
            rpName = "Hearthline",
            accountId = accountId,
            username = username,
            displayName = displayName,
            timeoutMillis = CHALLENGE_TTL.toMillis(),
            residentKey = ResidentKeyRequirement.REQUIRED,
            userVerification = UserVerificationRequirement.REQUIRED,
            attestation = AttestationConveyance.NONE,
            excludedCredentialIds = passkeys.listActive(accountId)
                .map { it.credentialId }
        )
    }

    /**
     * The actual WebAuthn verifier is intentionally isolated behind this
     * minimal result interface. It may parse the response to validate the
     * ceremony but must only release credential ID and public key to storage.
     */
    fun finish(
        accountId: AccountId,
        challengeId: ChallengeId,
        verified: VerifiedPublicRegistration
    ): RegisteredPasskey {
        val now = clock.instant()

        val challenge = challenges.consume(challengeId, now)
            ?: throw RegistrationDenied("Registration challenge is unavailable.")

        if (challenge.accountId != accountId || now >= challenge.expiresAt) {
            throw RegistrationDenied("Registration challenge is invalid or expired.")
        }

        if (passkeys.find(accountId, verified.credentialId) != null) {
            throw RegistrationDenied("Credential is already registered for this account.")
        }

        return RegisteredPasskey(
            accountId = accountId,
            credentialId = verified.credentialId,
            publicKeyCose = verified.publicKeyCose.copyOf(),
            createdAt = now,
            status = CredentialStatus.ACTIVE,
            generation = 1L
        ).also(passkeys::save)
    }
}

data class VerifiedPublicRegistration(
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray
)

/**
 * GATE-HW-07:
 *
 * Public core access has a client-neutral result. This makes it impossible for
 * registration to return "hardware accepted", "device weak", "unsupported
 * platform", or a social eligibility tier.
 */
sealed interface CoreAccessRoute {
    data object StandardPasskeyFlow : CoreAccessRoute
    data object AccessibleAlternativeFlow : CoreAccessRoute
}

class AccessibleRegistrationRouter {
    fun choose(
        platformFeatureAvailable: Boolean,
        memberSelectedAlternative: Boolean
    ): CoreAccessRoute =
        if (platformFeatureAvailable && !memberSelectedAlternative) {
            CoreAccessRoute.StandardPasskeyFlow
        } else {
            CoreAccessRoute.AccessibleAlternativeFlow
        }
}
