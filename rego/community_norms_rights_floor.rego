package hearthline.community_norms.rights_floor

default allow_norm := false

permitted_categories := {
  "discussion_format",
  "topic_order",
  "text_first_default",
  "asynchronous_default",
  "voluntary_topic_pause",
  "approved_prompt_category",
  "respectful_disagreement_language",
  "public_source_citation_style",
  "community_event_framing",
  "accessibility_reminder_language",
  "moderation_process_explanation"
}

allow_norm if {
  input.rightsFloorValidated == true
  input.permittedCategory in permitted_categories
  input.state == "under_rights_review"
}

deny_norm_reason["rights_floor_not_validated"] if {
  input.rightsFloorValidated != true
}

deny_norm_reason["category_not_permitted"] if {
  not input.permittedCategory in permitted_categories
}

deny_norm_reason["missing_review_date"] if {
  not input.reviewBy
}
