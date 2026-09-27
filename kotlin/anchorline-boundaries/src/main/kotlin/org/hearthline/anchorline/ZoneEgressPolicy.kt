package org.hearthline.anchorline

enum class ProtectedDataClass {
    CREDENTIAL,
    SESSION,
    REVOCATION,
    CAPABILITY_GRANT,
    RECOVERY,
    FEDERATION,
    AGE_PROOF,
    AGE_ELIGIBILITY_INTERNAL,
    UNIQUENESS_PROOF,
    NULLIFIER,
    ADMISSION_SIGNAL,
    ATTESTATION,
    HARDWARE,
    DEVICE,
    NETWORK,
    BEHAVIORAL_BIOMETRIC,
    SIGNUP_PROVENANCE,
    CRYPTOGRAPHIC_ALGORITHM,
    PORTABILITY
}

data class BoundaryEvent(
    val name: String,
    val classifications: Set<ProtectedDataClass>,
    val destination: TrustZone
)

object ZoneEgressPolicy {
    private val permittedSecurityDestinations = setOf(
        TrustZone.AUTHENTICATION,
        TrustZone.RECOVERY,
        TrustZone.AGE_GATE,
        TrustZone.INTEGRITY,
        TrustZone.GOVERNANCE
    )

    private val prohibitedDestinations = setOf(
        TrustZone.PROFILE,
        TrustZone.DISCOVERY,
        TrustZone.MATCHING,
        TrustZone.RANKING,
        TrustZone.RECOMMENDATION,
        TrustZone.MESSAGING,
        TrustZone.COUNTERPART_NOTIFICATION,
        TrustZone.ANALYTICS,
        TrustZone.EXPERIMENTATION,
        TrustZone.ADVERTISING,
        TrustZone.MODERATION_PRIORITY,
        TrustZone.TRUST_SCORING,
        TrustZone.SAFETY_SCORING,
        TrustZone.RESEARCH
    )

    fun validate(event: BoundaryEvent) {
        if (event.classifications.isEmpty()) {
            return
        }

        if (event.destination in prohibitedDestinations) {
            throw BoundaryViolation(
                "Protected data cannot egress to ${event.destination}: " +
                    event.classifications.joinToString()
            )
        }

        if (event.destination !in permittedSecurityDestinations) {
            throw BoundaryViolation(
                "No approved protected-data contract for ${event.destination}."
            )
        }
    }
}
