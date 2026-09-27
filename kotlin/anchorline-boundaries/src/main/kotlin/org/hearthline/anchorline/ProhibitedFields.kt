package org.hearthline.anchorline

object ProhibitedFields {
    private val exactNames = setOf(
        "keystrokeTiming",
        "dwellTime",
        "flightTime",
        "pointerPath",
        "touchPressure",
        "gestureSequence",
        "scrollRhythm",
        "voiceDynamics",
        "motionDynamics",
        "behaviorTemplate",
        "behaviorEmbedding",
        "behaviorHash",
        "behaviorScore",
        "baselineId",
        "humanBotClassification",
        "accountSharingClassification",

        "sybilScore",
        "duplicateScore",
        "humanScore",
        "personhoodScore",
        "trustScore",
        "safetyScore",
        "reputationScore",
        "legitimacyScore",
        "accountLinkLikelihood",

        "deviceId",
        "deviceFingerprint",
        "deviceGraph",
        "hardwareTrust",
        "hardwareClass",
        "secureEnclave",
        "strongBox",
        "tpm",
        "platform",
        "operatingSystem",
        "deviceModel",
        "deviceOwnership",

        "aaguid",
        "attestationStatement",
        "attestationObject",
        "attestationCertificate",
        "certificateChain",
        "credentialProvider",
        "credentialManager",
        "migrationStatus",
        "imported",
        "exported",
        "sourceDevice",
        "destinationDevice",
        "portabilityState",

        "ipAddress",
        "ipHash",
        "asn",
        "networkGraph",
        "location",
        "preciseLocation",
        "providerTrust",
        "providerRisk",
        "providerAccountAge",
        "providerSubject",
        "federationGraph",

        "dateOfBirth",
        "exactAge",
        "ageRange",
        "documentImage",
        "documentNumber",
        "paymentInstrument",
        "paymentCard",
        "paymentHistory"
    )

    fun requireAllowed(fieldNames: Set<String>) {
        val prohibited = fieldNames.intersect(exactNames)

        if (prohibited.isNotEmpty()) {
            throw BoundaryViolation(
                "Prohibited fields: ${prohibited.sorted().joinToString()}"
            )
        }
    }

    fun isProhibited(fieldName: String): Boolean =
        fieldName in exactNames
}
