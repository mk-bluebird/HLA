package hearthline.fairness.data_path_boundary

default allow_operational_field := false

allowed_operational_fields := {
  "adult_platform_eligibility_state",
  "current_discovery_consent_state",
  "current_connection_intent",
  "selected_activity_context",
  "selected_atmosphere_preference",
  "selected_communication_preference",
  "selected_broad_region_id",
  "selected_future_time_block",
  "block_or_restriction_state",
  "noncommercial_boundary_state",
  "current_visibility_state",
  "current_capability_consent_state"
}

forbidden_operational_fields := {
  "race",
  "ethnicity",
  "nationality",
  "tribal_affiliation",
  "religion",
  "gender_identity",
  "sexual_orientation",
  "disability",
  "health",
  "recovery",
  "financial_status",
  "housing_status",
  "employment_status",
  "immigration_status",
  "legal_history",
  "name_embedding",
  "image_embedding",
  "voice_embedding",
  "language_style_embedding",
  "location_proxy",
  "neighborhood_proxy",
  "school_proxy",
  "employer_proxy",
  "device_proxy",
  "social_graph",
  "contact_book",
  "profile_views",
  "message_sentiment",
  "response_speed",
  "photo_quality",
  "engagement_score",
  "time_spent",
  "popularity_score",
  "safety_score",
  "risk_score",
  "trust_score",
  "compatibility_score",
  "desirability_score",
  "response_likelihood"
}

allow_operational_field if {
  input.field in allowed_operational_fields
}

deny_operational_field if {
  input.field in forbidden_operational_fields
}

decision_input_manifest_valid if {
  every field in input.requestedFields {
    allow_operational_field with input.field as field
  }
}
