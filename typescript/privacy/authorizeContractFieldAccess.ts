export interface FieldAccessRequest {
  readonly contractId: string;
  readonly producer: string;
  readonly consumer: string;
  readonly purpose: string;
  readonly field: string;
  readonly policyVersion: string;
  readonly authorizationCurrent: boolean;
  readonly consentCurrent: boolean | null;
  readonly retentionClass: string;
  readonly requestTimestamp: string;
}

export interface ApprovedComponentContract {
  readonly contractId: string;
  readonly producer: string;
  readonly consumer: string;
  readonly purpose: string;
  readonly allowedFields: readonly string[];
  readonly forbiddenFields: readonly string[];
  readonly authorizationRequirement:
    | "member_owned_read"
    | "current_capability_grant"
    | "policy_authorized_service"
    | "human_review_authorization"
    | "aggregate_governance_authorization";
  readonly consentRequirement:
    | "static_public_content"
    | "policy_metadata"
    | "member_selected_input"
    | "current_capability_grant"
    | "separate_research_opt_in";
  readonly retentionClass: string;
  readonly policyVersion: string;
  readonly state: "approved" | "deprecated" | "revoked";
}

export type FieldAccessDecision =
  | {
      readonly allowed: true;
      readonly reason: "approved_contract_field_access";
    }
  | {
      readonly allowed: false;
      readonly reason:
        | "contract_not_approved"
        | "contract_identity_mismatch"
        | "purpose_mismatch"
        | "policy_version_mismatch"
        | "field_not_allowlisted"
        | "field_forbidden"
        | "authorization_not_current"
        | "consent_not_current"
        | "retention_class_mismatch"
        | "generic_no_consent_not_permitted";
    };

const MEMBER_SENSITIVE_FIELDS = new Set([
  "identity_claim",
  "access_control",
  "communication_preference",
  "consent_grant",
  "consent_history",
  "boundary",
  "pace_preference",
  "private_message",
  "private_media",
  "broad_region",
  "venue_detail",
  "meeting_plan",
  "trusted_contact_plan",
  "block",
  "restriction",
  "report",
  "appeal",
  "typing_duration",
  "edit_count",
  "response_time",
  "profile_view",
  "presence",
  "location",
  "route",
  "attendance"
]);

export function authorizeContractFieldAccess(
  contract: ApprovedComponentContract,
  request: FieldAccessRequest
): FieldAccessDecision {
  if (contract.state !== "approved") {
    return {
      allowed: false,
      reason: "contract_not_approved"
    };
  }

  if (
    contract.contractId !== request.contractId ||
    contract.producer !== request.producer ||
    contract.consumer !== request.consumer
  ) {
    return {
      allowed: false,
      reason: "contract_identity_mismatch"
    };
  }

  if (contract.purpose !== request.purpose) {
    return {
      allowed: false,
      reason: "purpose_mismatch"
    };
  }

  if (contract.policyVersion !== request.policyVersion) {
    return {
      allowed: false,
      reason: "policy_version_mismatch"
    };
  }

  if (contract.forbiddenFields.includes(request.field)) {
    return {
      allowed: false,
      reason: "field_forbidden"
    };
  }

  if (!contract.allowedFields.includes(request.field)) {
    return {
      allowed: false,
      reason: "field_not_allowlisted"
    };
  }

  if (!request.authorizationCurrent) {
    return {
      allowed: false,
      reason: "authorization_not_current"
    };
  }

  if (contract.retentionClass !== request.retentionClass) {
    return {
      allowed: false,
      reason: "retention_class_mismatch"
    };
  }

  const fieldIsSensitive = MEMBER_SENSITIVE_FIELDS.has(
    request.field
  );

  if (
    fieldIsSensitive &&
    (
      contract.consentRequirement === "static_public_content" ||
      contract.consentRequirement === "policy_metadata"
    )
  ) {
    return {
      allowed: false,
      reason: "generic_no_consent_not_permitted"
    };
  }

  if (
    (
      contract.consentRequirement === "member_selected_input" ||
      contract.consentRequirement === "current_capability_grant" ||
      contract.consentRequirement === "separate_research_opt_in"
    ) &&
    request.consentCurrent !== true
  ) {
    return {
      allowed: false,
      reason: "consent_not_current"
    };
  }

  return {
    allowed: true,
    reason: "approved_contract_field_access"
  };
}
