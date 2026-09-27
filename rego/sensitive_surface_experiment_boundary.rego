package hearthline.experiments.sensitive_surface_boundary

default allow_experiment := false

sensitive_surfaces := {
  "consent_checkpoint",
  "consent_withdrawal",
  "block",
  "mute",
  "restrict",
  "report",
  "appeal",
  "quiet_exit",
  "identity_disclosure",
  "access_controls",
  "communication_controls",
  "discovery",
  "media_sharing",
  "meeting_plan",
  "trusted_contact_plan",
  "aftercare",
  "topic_board"
}

sensitive_surface if {
  input.surface in sensitive_surfaces
}

approved_sensitive_experiment if {
  input.surface in sensitive_surfaces
  input.governanceApprovalRef
  input.privacyReviewRef
  input.accessibilityReviewRef
  input.safetyReviewRef
  input.memberImpactStatement
  input.noCounterpartBehavioralPayload == true
  input.noMatchingPayload == true
  input.noProfileAnalyticsPayload == true
  input.noSensitiveOutcomeOptimization == true
}

allow_experiment if {
  not sensitive_surface
  input.experimentType == "non_behavioral_configuration_validation"
}

allow_experiment if {
  approved_sensitive_experiment
}

deny_reason["sensitive_surface_requires_explicit_governance_review"] if {
  sensitive_surface
  not approved_sensitive_experiment
}

deny_reason["behavioral_experiment_prohibited"] if {
  input.experimentType == "behavioral_optimization"
}

deny_reason["counterpart_or_matching_payload_prohibited"] if {
  input.noCounterpartBehavioralPayload != true
}

deny_reason["sensitive_outcome_optimization_prohibited"] if {
  input.noSensitiveOutcomeOptimization != true
}
