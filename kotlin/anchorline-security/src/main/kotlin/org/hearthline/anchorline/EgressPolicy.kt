package org.hearthline.anchorline

/**
 * A compact classification system used before serializing an event or calling a
 * downstream service. It models prohibited information categories rather than
 * collecting the underlying device, hardware, provider, or profile values.
 */
enum class ProtectedDataClass {
    PASSKEY_CREDENTIAL,
    PASSKEY_PUBLIC_KEY,
    PASSKEY_LABEL,
    PASSKEY_TIMING,
    SESSION_BINDING,
    SESSION_STATUS,
    RECOVERY_STATE,
    RECOVERY_AUTHORIZATION_METHOD,
    FEDERATION_SUBJECT,
    FEDERATION_ISSUER,
    AGE_PROOF,
    AGE_ELIGIBILITY_INTERNAL,
    ATTESTATION_ARTIFACT,
    AUTHENTICATOR_PROVENANCE,
    HARDWARE_PROPERTY,
    DEVICE_PROPERTY,
    NETWORK_PROPERTY,
    PRESENCE_PROPERTY,
    ACCOUNT_CONSOLIDATION_STATE
}

data class BoundaryEvent(
    val name: String,
    val classifications: Set<ProtectedDataClass>,
    val destination: ServiceZone
)

/**
 * This policy implements a fail-closed egress rule.
 *
 * Authentication, recovery, age, federation, session, and credential metadata
 * may stay only inside their purpose-limited boundaries. It cannot be used as
 * input to social product surfaces.
 */
object ProtectedSecurityDataEgressPolicy {
    private val allowedDestinations = setOf(
        ServiceZone.AUTHENTICATION_BOUNDARY,
        ServiceZone.AGE_GATE,
        ServiceZone.RECOVERY_BOUNDARY,
        ServiceZone.SECURITY_HISTORY,
        ServiceZone.APPEAL_BOUNDARY
    )

    private val prohibitedDestinations = setOf(
        ServiceZone.PROFILE,
        ServiceZone.DISCOVERY,
        ServiceZone.MATCHING,
        ServiceZone.RANKING,
        ServiceZone.RECOMMENDATION,
        ServiceZone.MESSAGING,
        ServiceZone.COUNTERPART_NOTIFICATION,
        ServiceZone.ANALYTICS,
        ServiceZone.EXPERIMENTATION,
        ServiceZone.ADVERTISING,
        ServiceZone.MODERATION_PRIORITY,
        ServiceZone.TRUST_SCORING,
        ServiceZone.SAFETY_SCORING,
        ServiceZone.ELIGIBILITY_ENGINE
    )

    fun validate(event: BoundaryEvent) {
        if (event.classifications.isEmpty()) {
            return
        }

        if (event.destination in prohibitedDestinations) {
            throw BoundaryViolation(
                "Protected security data cannot egress to ${event.destination}: " +
                    event.classifications.joinToString()
            )
        }

        if (event.destination !in allowedDestinations) {
            throw BoundaryViolation(
                "No approved purpose-limited contract exists for destination " +
                    "${event.destination}."
            )
        }
    }
}

/**
 * GATE-HW-05:
 *
 * Inputs that look like provenance or device data are rejected at serialization
 * time. Production deployments should pair this with schema validation,
 * database permissions, and event-bus policies.
 */
object ProhibitedSecurityFieldNames {
    private val prohibited = setOf(
        "aaguid",
        "attestationStatement",
        "attestationObject",
        "attestationCertificate",
        "certificateChain",
        "authenticatorModel",
        "authenticatorManufacturer",
        "hardwareClassification",
        "secureEnclave",
        "strongBox",
        "tpm",
        "deviceModel",
        "operatingSystem",
        "platform",
        "credentialManagerProvider",
        "backupEligible",
        "backupState",
        "deviceId",
        "deviceFingerprint",
        "ipAddress",
        "ipHash",
        "asn",
        "location",
        "presence",
        "deviceTrust",
        "memberRiskScore",
        "providerRiskScore",
        "providerAccountAge",
        "microsoftOid",
        "googleAccessToken",
        "refreshToken",
        "dateOfBirth",
        "exactAge",
        "ageRange",
        "documentImage",
        "documentNumber"
    )

    fun requireAllowed(fields: Set<String>) {
        val violations = fields.intersect(prohibited)
        if (violations.isNotEmpty()) {
            throw BoundaryViolation(
                "Prohibited security/privacy fields: ${violations.sorted().joinToString()}"
            )
        }
    }
}
