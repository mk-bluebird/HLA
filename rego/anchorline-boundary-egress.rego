package hearthline.anchorline.egress

default allow := false

protected_classes := {
  "CREDENTIAL",
  "SESSION",
  "REVOCATION",
  "CAPABILITY_GRANT",
  "RECOVERY",
  "FEDERATION",
  "AGE_PROOF",
  "AGE_ELIGIBILITY_INTERNAL",
  "UNIQUENESS_PROOF",
  "NULLIFIER",
  "ADMISSION_SIGNAL",
  "ATTESTATION",
  "HARDWARE",
  "DEVICE",
  "NETWORK",
  "BEHAVIORAL_BIOMETRIC",
  "SIGNUP_PROVENANCE",
  "CRYPTOGRAPHIC_ALGORITHM",
  "PORTABILITY"
}

prohibited_destinations := {
  "PROFILE",
  "DISCOVERY",
  "MATCHING",
  "RANKING",
  "RECOMMENDATION",
  "MESSAGING",
  "COUNTERPART_NOTIFICATION",
  "ANALYTICS",
  "EXPERIMENTATION",
  "ADVERTISING",
  "MODERATION_PRIORITY",
  "TRUST_SCORING",
  "SAFETY_SCORING",
  "RESEARCH"
}

deny contains message if {
  some classification in input.classifications
  classification in protected_classes
  input.destination in prohibited_destinations

  message := sprintf(
    "Protected class %q cannot egress to %q.",
    [classification, input.destination]
  )
}

deny contains message if {
  some field in input.fields
  field in {
    "sybilScore",
    "duplicateScore",
    "humanScore",
    "personhoodScore",
    "trustScore",
    "deviceGraph",
    "federationGraph",
    "accountLinkLikelihood",
    "keystrokeTiming",
    "pointerPath",
    "touchPressure",
    "behaviorEmbedding",
    "hardwareTrust",
    "migrationStatus",
    "providerRisk",
    "ipAddress"
  }

  message := sprintf("Prohibited field %q.", [field])
}

allow if {
  count(deny) == 0
}
