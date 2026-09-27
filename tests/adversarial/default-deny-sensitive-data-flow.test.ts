import { describe, expect, it } from "vitest";
import {
  authorizeContractFieldAccess,
  type ApprovedComponentContract
} from "../../typescript/privacy/authorizeContractFieldAccess";

const approvedDiscoveryContract: ApprovedComponentContract = {
  contractId: "component_contract_discovery_context",
  producer: "connection_zone",
  consumer: "discovery_eligibility",
  purpose: "constraint_based_eligibility",
  allowedFields: [
    "current_connection_intent",
    "selected_activity_context",
    "selected_broad_region",
    "current_discovery_consent",
    "block_or_restriction_state"
  ],
  forbiddenFields: [
    "access_control",
    "private_message",
    "consent_history",
    "typing_duration",
    "location",
    "attendance",
    "safety_score",
    "risk_score",
    "compatibility_score"
  ],
  authorizationRequirement: "current_capability_grant",
  consentRequirement: "member_selected_input",
  retentionClass: "ephemeral_operational",
  policyVersion: "discovery-policy-v1",
  state: "approved"
};

describe("INV-DEFAULT-DENY-SENSITIVE-DATA-FLOW", () => {
  it("permits an allowlisted current discovery field", () => {
    const decision = authorizeContractFieldAccess(
      approvedDiscoveryContract,
      {
        contractId: "component_contract_discovery_context",
        producer: "connection_zone",
        consumer: "discovery_eligibility",
        purpose: "constraint_based_eligibility",
        field: "current_connection_intent",
        policyVersion: "discovery-policy-v1",
        authorizationCurrent: true,
        consentCurrent: true,
        retentionClass: "ephemeral_operational",
        requestTimestamp: "2026-09-26T00:00:00Z"
      }
    );

    expect(decision).toEqual({
      allowed: true,
      reason: "approved_contract_field_access"
    });
  });

  it("rejects an access control sent to discovery", () => {
    const decision = authorizeContractFieldAccess(
      approvedDiscoveryContract,
      {
        contractId: "component_contract_discovery_context",
        producer: "connection_zone",
        consumer: "discovery_eligibility",
        purpose: "constraint_based_eligibility",
        field: "access_control",
        policyVersion: "discovery-policy-v1",
        authorizationCurrent: true,
        consentCurrent: true,
        retentionClass: "ephemeral_operational",
        requestTimestamp: "2026-09-26T00:00:00Z"
      }
    );

    expect(decision).toEqual({
      allowed: false,
      reason: "field_forbidden"
    });
  });

  it("rejects consent history as a discovery input", () => {
    const decision = authorizeContractFieldAccess(
      approvedDiscoveryContract,
      {
        contractId: "component_contract_discovery_context",
        producer: "connection_zone",
        consumer: "discovery_eligibility",
        purpose: "constraint_based_eligibility",
        field: "consent_history",
        policyVersion: "discovery-policy-v1",
        authorizationCurrent: true,
        consentCurrent: true,
        retentionClass: "ephemeral_operational",
        requestTimestamp: "2026-09-26T00:00:00Z"
      }
    );

    expect(decision).toEqual({
      allowed: false,
      reason: "field_forbidden"
    });
  });

  it("rejects location reconstruction fields", () => {
    const decision = authorizeContractFieldAccess(
      approvedDiscoveryContract,
      {
        contractId: "component_contract_discovery_context",
        producer: "connection_zone",
        consumer: "discovery_eligibility",
        purpose: "constraint_based_eligibility",
        field: "location",
        policyVersion: "discovery-policy-v1",
        authorizationCurrent: true,
        consentCurrent: true,
        retentionClass: "ephemeral_operational",
        requestTimestamp: "2026-09-26T00:00:00Z"
      }
    );

    expect(decision.allowed).toBe(false);
    expect(decision.reason).toBe("field_forbidden");
  });

  it("rejects sensitive data under static-public-content consent", () => {
    const invalidContract: ApprovedComponentContract = {
      ...approvedDiscoveryContract,
      allowedFields: ["identity_claim"],
      forbiddenFields: [],
      consentRequirement: "static_public_content"
    };

    const decision = authorizeContractFieldAccess(
      invalidContract,
      {
        contractId: "component_contract_discovery_context",
        producer: "connection_zone",
        consumer: "discovery_eligibility",
        purpose: "constraint_based_eligibility",
        field: "identity_claim",
        policyVersion: "discovery-policy-v1",
        authorizationCurrent: true,
        consentCurrent: null,
        retentionClass: "ephemeral_operational",
        requestTimestamp: "2026-09-26T00:00:00Z"
      }
    );

    expect(decision).toEqual({
      allowed: false,
      reason: "generic_no_consent_not_permitted"
    });
  });
});
