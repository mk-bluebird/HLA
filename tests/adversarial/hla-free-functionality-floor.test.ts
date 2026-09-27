import { describe, expect, it } from "vitest";

type MemberPlan = "free" | "optional_enhanced_compute";

interface CapabilityRequest {
  readonly memberPlan: MemberPlan;
  readonly capability: string;
}

const FREE_REQUIRED_CAPABILITIES = new Set([
  "privacy_scope_review",
  "consent_explainer",
  "consent_withdrawal",
  "block",
  "mute",
  "restrict",
  "report",
  "appeal",
  "quiet_exit",
  "delete_member_data",
  "export_request",
  "text_first_access",
  "screen_reader_access",
  "keyboard_access",
  "reduced_motion_access",
  "low_bandwidth_access",
  "no_camera_access",
  "no_microphone_access",
  "no_voice_access",
  "self_directed_discovery",
  "chronological_discovery",
  "pause_discovery",
  "hide_discovery_option",
  "show_fewer_options",
  "intent_card",
  "boundary_ledger",
  "activity_window",
  "public_first_template",
  "remote_first_template",
  "source_labeled_meeting_information",
  "source_labeled_transit_information",
  "opening_draft",
  "reply_draft",
  "boundary_draft",
  "decline_draft",
  "pause_draft",
  "exit_draft",
  "access_request_draft",
  "topic_suggestion",
  "aftercare_reflection",
  "decision_history"
]);

function authorizeHlaCapability(
  request: CapabilityRequest
): { readonly allowed: boolean; readonly reason: string } {
  if (FREE_REQUIRED_CAPABILITIES.has(request.capability)) {
    return {
      allowed: true,
      reason: "free_functionality_floor"
    };
  }

  if (request.memberPlan === "optional_enhanced_compute") {
    return {
      allowed: true,
      reason: "optional_enhanced_compute"
    };
  }

  return {
    allowed: false,
    reason: "optional_feature_not_in_free_floor"
  };
}

describe("INV-HLA-FREE-FUNCTIONALITY-FLOOR", () => {
  it("allows free members to use safety and exit controls", () => {
    for (const capability of [
      "block",
      "report",
      "appeal",
      "quiet_exit",
      "consent_withdrawal"
    ]) {
      expect(
        authorizeHlaCapability({
          memberPlan: "free",
          capability
        })
      ).toEqual({
        allowed: true,
        reason: "free_functionality_floor"
      });
    }
  });

  it("allows free members to use low-token drafting", () => {
    expect(
      authorizeHlaCapability({
        memberPlan: "free",
        capability: "boundary_draft"
      })
    ).toEqual({
      allowed: true,
      reason: "free_functionality_floor"
    });
  });

  it("allows free members to use accessibility paths", () => {
    for (const capability of [
      "text_first_access",
      "screen_reader_access",
      "low_bandwidth_access",
      "no_voice_access"
    ]) {
      expect(
        authorizeHlaCapability({
          memberPlan: "free",
          capability
        }).allowed
      ).toBe(true);
    }
  });

  it("does not require a subscription to pause or organize discovery", () => {
    for (const capability of [
      "self_directed_discovery",
      "chronological_discovery",
      "pause_discovery",
      "hide_discovery_option",
      "show_fewer_options"
    ]) {
      expect(
        authorizeHlaCapability({
          memberPlan: "free",
          capability
        }).allowed
      ).toBe(true);
    }
  });

  it("does not turn enhanced compute into social advantage", () => {
    const prohibitedPaidAdvantages = [
      "paid_discovery_rank",
      "paid_reply_priority",
      "paid_safety_status",
      "paid_trust_status",
      "paid_consent_bypass",
      "paid_location_access",
      "paid_visibility"
    ];

    for (const capability of prohibitedPaidAdvantages) {
      expect(
        authorizeHlaCapability({
          memberPlan: "optional_enhanced_compute",
          capability
        })
      ).toEqual({
        allowed: false,
        reason: "optional_feature_not_in_free_floor"
      });
    }
  });
});
