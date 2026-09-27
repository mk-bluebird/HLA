package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class SignupReceiptId(val value: UUID)

enum class RegistrationEvidence {
    WEBAUTHN_CHALLENGE_CONSUMED,
    WEBAUTHN_RP_ID_VALIDATED,
    WEBAUTHN_ORIGIN_VALIDATED,
    WEBAUTHN_USER_VERIFICATION_SATISFIED,
    CREDENTIAL_REGISTERED,
    CREDENTIAL_ACCOUNT_BINDING_CREATED,
    AGE_GATE_ELIGIBLE
}

data class SignupProvenanceReceipt(
    val receiptId: SignupReceiptId,
    val accountId: AccountId,
    val outcome: String,
    val evidence: Set<RegistrationEvidence>,
    val registrationPolicyVersion: String,
    val webAuthnVerifierVersion: String,
    val ageGatePolicyVersion: String,
    val completedAt: Instant,
    val expiresAt: Instant
) {
    init {
        require(outcome == "COMPLETED")
        require(expiresAt.isAfter(completedAt))
        require(evidence.isNotEmpty())
        require(registrationPolicyVersion.isNotBlank())
        require(webAuthnVerifierVersion.isNotBlank())
        require(ageGatePolicyVersion.isNotBlank())
    }
}

interface SignupProvenanceRepository {
    fun append(receipt: SignupProvenanceReceipt)

    fun deleteExpired(now: Instant): Int
}

object SignupProvenanceEgress {
    fun requireAllowed(destination: TrustZone) {
        require(
            destination in setOf(
                TrustZone.AUTHENTICATION,
                TrustZone.AGE_GATE,
                TrustZone.GOVERNANCE
            )
        ) {
            "Signup provenance cannot leave security/governance boundaries."
        }
    }
}
