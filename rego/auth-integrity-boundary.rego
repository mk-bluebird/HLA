package hearthline.auth_integrity_boundary

default allow := false

protected_security_classifications := {
  "authentication",
  "passkey",
  "credential",
  "attestation",
  "integrity",
  "session_binding"
}

prohibited_destinations := {
  "matching",
  "discovery",
  "ranking",
  "recommendation",
  "profile",
  "counterpart_notification",
  "analytics",
  "advertising",
  "experimentation",
  "moderation_priority",
  "trust_scoring",
  "risk_scoring"
}

allowed_security_destinations := {
  "authentication_service",
  "credential_revocation_service",
  "integrity_verifier",
  "security_history_service",
  "appeal_service"
}

deny contains message if {
  input.event.classification in protected_security_classifications
  input.destination in prohibited_destinations

  message := sprintf(
    "Protected authentication/integrity data cannot flow to %q.",
    [input.destination]
  )
}

deny contains message if {
  input.event.classification in protected_security_classifications
  not input.destination in allowed_security_destinations

  message := sprintf(
    "Protected authentication/integrity data has no approved destination contract: %q.",
    [input.destination]
  )
}

deny contains message if {
  some field in input.event.fields
  field in {
    "attestationToken",
    "attestationKeyId",
    "deviceId",
    "hardwareId",
    "advertisingId",
    "platformVersion",
    "deviceModel",
    "backupEligible",
    "backupState",
    "credentialProvider",
    "syncProvider",
    "estimatedDeviceCount",
    "integrityHistory",
    "deviceReliabilityScore"
  }

  message := sprintf(
    "Prohibited device or integrity-derived field present: %q.",
    [field]
  )
}

allow if {
  count(deny) == 0
  input.destination in allowed_security_destinations
}
