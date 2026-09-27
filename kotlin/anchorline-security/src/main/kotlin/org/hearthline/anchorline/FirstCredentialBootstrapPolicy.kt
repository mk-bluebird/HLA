package org.hearthline.anchorline

enum class EnrollmentIntegrityEvidence {
    NONE,
    PRIVATE_ACCESS_TOKEN,
    APP_ATTEST,
    PLAY_INTEGRITY,
    WEB_AUTHN_DIRECT_ATTESTATION,
    WEB_AUTHN_ENTERPRISE_ATTESTATION,
    CAPTCHA,
    JAVASCRIPT_CHALLENGE
}

enum class RegistrationRoute {
    STANDARD_PASSKEY,
    ACCESSIBLE_ALTERNATIVE
}

/**
 * Consumer enrollment remains passkey-first and attestation-none.
 *
 * This policy makes it impossible to represent direct or enterprise WebAuthn
 * attestation as a consumer-registration requirement.
 */
data class FirstCredentialBootstrapPolicy(
    val attestationConveyance: AttestationConveyance,
    val userVerification: UserVerificationRequirement,
    val route: RegistrationRoute,
    val requestedIntegrityEvidence: EnrollmentIntegrityEvidence
) {
    init {
        require(attestationConveyance == AttestationConveyance.NONE) {
            "Consumer registration must request WebAuthn attestation='none'."
        }

        require(userVerification == UserVerificationRequirement.REQUIRED) {
            "Consumer registration requires user verification."
        }

        require(
            requestedIntegrityEvidence !in setOf(
                EnrollmentIntegrityEvidence.WEB_AUTHN_DIRECT_ATTESTATION,
                EnrollmentIntegrityEvidence.WEB_AUTHN_ENTERPRISE_ATTESTATION
            )
        ) {
            "Direct and enterprise WebAuthn attestation are prohibited."
        }
    }

    /**
     * Resulting account state is independent of whether a member took the
     * standard path or an accessible alternative.
     */
    fun resultingAccountState(): EnrollmentAccountState =
        EnrollmentAccountState(
            passkeyRequired = true,
            hardwareTier = null,
            integrityTier = null,
            discoveryEligibilityChanged = false,
            rankingEligibilityChanged = false,
            counterpartVisibleStatus = null
        )
}

data class EnrollmentAccountState(
    val passkeyRequired: Boolean,
    val hardwareTier: Nothing?,
    val integrityTier: Nothing?,
    val discoveryEligibilityChanged: Boolean,
    val rankingEligibilityChanged: Boolean,
    val counterpartVisibleStatus: Nothing?
)
