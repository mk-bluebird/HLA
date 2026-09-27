package org.hearthline.anchorline

enum class AttestationConveyance {
    NONE,
    INDIRECT,
    DIRECT,
    ENTERPRISE
}

enum class EnrollmentRoute {
    STANDARD_PASSKEY,
    ACCESSIBLE_ALTERNATIVE
}

enum class HumannessSignalKind {
    PRIVATE_ACCESS_TOKEN,
    PROOF_OF_WORK,
    RECAPTCHA_ENTERPRISE,
    AZURE_WAF_CHALLENGE,
    ACCESSIBLE_ALTERNATIVE
}

enum class HumannessSignalDecision {
    CONTINUE_CURRENT_REQUEST,
    ACCESSIBLE_ALTERNATIVE
}

data class ConsumerEnrollmentPolicy(
    val attestation: AttestationConveyance,
    val userVerificationRequired: Boolean,
    val route: EnrollmentRoute
) {
    init {
        require(attestation == AttestationConveyance.NONE) {
            "Consumer WebAuthn registration requires attestation='none'."
        }

        require(userVerificationRequired) {
            "Consumer WebAuthn registration requires user verification."
        }
    }
}

/**
 * Provider adapters export only one binary, current-request result.
 * They cannot expose risk score, reason codes, device details, cookies,
 * platform class, account state, or member classification.
 */
interface HumannessSignalAdapter {
    fun verifyCurrentRequest(
        signal: OpaqueIntegritySignal?
    ): HumannessSignalDecision
}

object EnrollmentEquivalencePolicy {
    fun requireEquivalent(
        standardCapabilities: Set<String>,
        alternativeCapabilities: Set<String>
    ) {
        require(standardCapabilities == alternativeCapabilities) {
            "Accessible enrollment alternatives must have equivalent capabilities."
        }
    }
}
