package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class SignupCeremonyId(val value: UUID)

enum class SignupCeremonyOutcome {
    COMPLETED,
    REJECTED,
    ROUTED_TO_ACCESSIBLE_ALTERNATIVE
}

enum class RegistrationEvidence {
    WEBAUTHN_CHALLENGE_CONSUMED,
    WEBAUTHN_RP_ID_VALIDATED,
    WEBAUTHN_ORIGIN_VALIDATED,
    WEBAUTHN_USER_VERIFICATION_SATISFIED,
    CREDENTIAL_REGISTERED,
    CREDENTIAL_ACCOUNT_BINDING_CREATED,
    AGE_GATE_ELIGIBLE
}

/**
 * Account-private provenance receipt.
 *
 * The receipt contains security facts about one completed or attempted ceremony.
 * It contains no device, network, provider, attestation, profile, behavior,
 * location, or score data.
 */
data class SignupProvenanceReceipt(
    val ceremonyId: SignupCeremonyId,
    val accountId: AccountId?,
    val outcome: SignupCeremonyOutcome,
    val evidence: Set<RegistrationEvidence>,
    val registrationPolicyVersion: String,
    val webAuthnVerifierVersion: String,
    val ageGatePolicyVersion: String?,
    val startedAt: Instant,
    val completedAt: Instant,
    val expiresAt: Instant
) {
    init {
        require(completedAt >= startedAt) {
            "Signup ceremony completion must follow its start."
        }

        require(expiresAt > completedAt) {
            "Signup provenance expiry must follow completion."
        }

        require(registrationPolicyVersion.isNotBlank()) {
            "Registration policy version is required."
        }

        require(webAuthnVerifierVersion.isNotBlank()) {
            "WebAuthn verifier version is required."
        }

        if (outcome == SignupCeremonyOutcome.COMPLETED) {
            require(accountId != null) {
                "Completed signup provenance requires a local account ID."
            }

            require(
                evidence.containsAll(
                    setOf(
                        RegistrationEvidence.WEBAUTHN_CHALLENGE_CONSUMED,
                        RegistrationEvidence.WEBAUTHN_RP_ID_VALIDATED,
                        RegistrationEvidence.WEBAUTHN_ORIGIN_VALIDATED,
                        RegistrationEvidence.WEBAUTHN_USER_VERIFICATION_SATISFIED,
                        RegistrationEvidence.CREDENTIAL_REGISTERED,
                        RegistrationEvidence.CREDENTIAL_ACCOUNT_BINDING_CREATED,
                        RegistrationEvidence.AGE_GATE_ELIGIBLE
                    )
                )
            ) {
                "Completed signup provenance lacks required ceremony evidence."
            }
        }
    }
}

interface SignupProvenanceRepository {
    fun append(receipt: SignupProvenanceReceipt)

    fun findForMemberSecurityHistory(
        accountId: AccountId,
        now: Instant
    ): List<SignupProvenanceReceipt>

    fun deleteExpired(now: Instant): Int
}

/**
 * Only the identity, security-history, and appeal boundaries may receive this
 * receipt. It must not egress to any social-product service.
 */
object SignupProvenanceBoundary {
    fun validateDestination(destination: ServiceZone) {
        if (
            destination !in setOf(
                ServiceZone.AUTHENTICATION_BOUNDARY,
                ServiceZone.AGE_GATE,
                ServiceZone.SECURITY_HISTORY,
                ServiceZone.APPEAL_BOUNDARY
            )
        ) {
            throw BoundaryViolation(
                "Signup provenance cannot leave the security and appeal boundaries."
            )
        }
    }
}
