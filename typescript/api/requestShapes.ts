/**
 * Field shapes derived from schemas/*.schema.json.
 *
 * Generated during the API milestone session from the schema files and then
 * reviewed by hand; keep in sync with the schemas (the schemas remain the
 * authoritative contract). The adapter in `schemaValidation.ts` refuses to
 * load any schema whose property names do not exactly match these shapes, so
 * drift fails loudly instead of silently accepting or rejecting payloads.
 */

export interface FieldShape {
  readonly required: boolean;
  readonly validate: (value: unknown) => boolean;
}

export interface ObjectShape {
  readonly fields: Readonly<Record<string, FieldShape>>;
}

/** Body shape for every member-controlled discovery action. */
export const MEMBER_ACTION_SHAPE: ObjectShape = {
  fields: {
    memberRef: {
      required: true,
      validate: (v: unknown) =>
        typeof v === "string" && /^member_[A-Za-z0-9_-]{16,128}$/.test(v)
    }
  }
};

export const SCHEMA_SHAPES: Readonly<Record<string, ObjectShape>> = {
  "public-resource-entry": {
  fields: {
    resourceId: { required: true, validate: (v: unknown) => typeof v === "string" && /^resource_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    title: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 1 && (v as string).length <= 200 },
    category: { required: true, validate: (v: unknown) => typeof v === "string" && ["transit", "food", "housing_information", "tenant_rights", "health_information", "community_clinics", "recovery_information", "legal_information", "employment_information", "worker_rights", "accessibility_information", "community_information", "emergency_information"].some((e: string) => e === v) },
    officialOrPublisherSource: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 1 && (v as string).length <= 200 },
    sourceUrl: { required: true, validate: (v: unknown) => typeof v === "string" },
    sourceType: { required: true, validate: (v: unknown) => typeof v === "string" && ["government", "official_provider", "public_nonprofit", "public_directory", "public_library", "public_transit_agency", "legal_aid_directory", "community_organization"].some((e: string) => e === v) },
    jurisdiction: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 2 && (v as string).length <= 120 },
    plainLanguageSummary: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 1 && (v as string).length <= 1000 },
    contactMethodAsPublished: { required: false, validate: (v: unknown) => typeof v === "string" && (v as string).length <= 500 },
    accessibilityInformationIfPubliclyAvailable: { required: false, validate: (v: unknown) => typeof v === "string" && (v as string).length <= 1000 },
    eligibilityUnknownOrSourceLinked: { required: true, validate: (v: unknown) => v === true },
    retrievedAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    reviewBy: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    status: { required: true, validate: (v: unknown) => typeof v === "string" && ["listed", "under_review", "outdated", "removed"].some((e: string) => e === v) },
  }
},
  "pause-discovery-state": {
  fields: {
    pauseId: { required: true, validate: (v: unknown) => typeof v === "string" && /^discovery_pause_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    ownerRef: { required: true, validate: (v: unknown) => typeof v === "string" && /^member_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    state: { required: true, validate: (v: unknown) => typeof v === "string" && ["active", "resumed", "deleted"].some((e: string) => e === v) },
    createdAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    updatedAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    expiresAt: { required: false, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
  }
},
  "mutual-visibility-context": {
  fields: {
    contextId: { required: true, validate: (v: unknown) => typeof v === "string" && /^visctx_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    memberRefA: { required: true, validate: (v: unknown) => typeof v === "string" && /^member_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    memberRefB: { required: true, validate: (v: unknown) => typeof v === "string" && /^member_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    visibilityAtoB: { required: true, validate: (v: unknown) => typeof v === "string" && ["hidden", "broad_category_only", "shared_context_visible", "direct"].some((e: string) => e === v) },
    visibilityBtoA: { required: true, validate: (v: unknown) => typeof v === "string" && ["hidden", "broad_category_only", "shared_context_visible", "direct"].some((e: string) => e === v) },
    mutualConsent: { required: true, validate: (v: unknown) => typeof v === "boolean" },
    sharedActivityScope: { required: false, validate: (v: unknown) => typeof v === "string" && (v as string).length <= 120 },
    generatedAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
  }
},
  "encounter-capsule": {
  fields: {
    capsuleId: { required: true, validate: (v: unknown) => typeof v === "string" && /^capsule_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    participants: { required: true, validate: (v: unknown) => Array.isArray(v) && (v as unknown[]).every((item: unknown) => typeof item === "string" && /^member_[A-Za-z0-9_-]{16,128}$/.test(item)) && (v as unknown[]).length >= 2 && (v as unknown[]).length <= 2 && new Set((v as unknown[]).map((x: unknown) => JSON.stringify(x))).size === (v as unknown[]).length },
    purpose: { required: true, validate: (v: unknown) => typeof v === "string" && ["meeting_detail", "place_description", "context_note"].some((e: string) => e === v) },
    status: { required: true, validate: (v: unknown) => typeof v === "string" && ["proposed", "mutually_accepted", "paused", "withdrawn", "cancelled", "expired"].some((e: string) => e === v) },
    createdAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    mutuallyAcceptedAt: { required: false, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    expiresAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    contentFormat: { required: true, validate: (v: unknown) => v === "text_plain" },
    contentSize: { required: true, validate: (v: unknown) => typeof v === "string" && ["short", "medium"].some((e: string) => e === v) },
    requiresMutualAcceptance: { required: true, validate: (v: unknown) => v === true },
    noExportUi: { required: true, validate: (v: unknown) => v === true },
    noPlatformRecovery: { required: true, validate: (v: unknown) => v === true },
    externalActionAuthorized: { required: true, validate: (v: unknown) => v === false },
  }
},
  "intent-recognition-preview": {
  fields: {
    previewId: { required: true, validate: (v: unknown) => typeof v === "string" && /^intentprev_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    requesterMemberRef: { required: true, validate: (v: unknown) => typeof v === "string" && /^member_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    recognizedIntent: { required: true, validate: (v: unknown) => typeof v === "string" && ["shared_activity_suggestion", "meeting_logistics", "boundary_statement", "cancellation_request", "resource_inquiry"].some((e: string) => e === v) },
    proposedText: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 1 && (v as string).length <= 500 },
    nextAction: { required: true, validate: (v: unknown) => typeof v === "string" && ["confirm_or_edit", "no_action"].some((e: string) => e === v) },
    queryExecutable: { required: true, validate: (v: unknown) => v === false },
    generatedAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
  }
},
  "discovery-surface-allocation": {
  fields: {
    allocationId: { required: true, validate: (v: unknown) => typeof v === "string" && /^alloc_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    memberRef: { required: true, validate: (v: unknown) => typeof v === "string" && /^member_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    presentationMode: { required: true, validate: (v: unknown) => typeof v === "string" && ["chronological", "one_at_a_time", "category_browse"].some((e: string) => e === v) },
    chosenCategory: { required: false, validate: (v: unknown) => typeof v === "string" && (v as string).length <= 120 },
    optionRefs: { required: true, validate: (v: unknown) => Array.isArray(v) && (v as unknown[]).every((item: unknown) => typeof item === "string" && /^option_[A-Za-z0-9_-]{16,128}$/.test(item)) && (v as unknown[]).length <= 50 },
    showFewerRequested: { required: true, validate: (v: unknown) => typeof v === "boolean" },
    generatedAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
  }
},
  "community-pulse": {
  fields: {
    pulseId: { required: true, validate: (v: unknown) => typeof v === "string" && /^pulse_[A-Za-z0-9_-]{16,128}$/.test(v as string) },
    scopeLabel: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 1 && (v as string).length <= 120 },
    headline: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 1 && (v as string).length <= 200 },
    detailSummary: { required: true, validate: (v: unknown) => typeof v === "string" && (v as string).length >= 1 && (v as string).length <= 1000 },
    windowStart: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    windowEnd: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    publishedAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    expiresAt: { required: true, validate: (v: unknown) => typeof v === "string" && !Number.isNaN(Date.parse(v as string)) },
    aggregatedOnly: { required: true, validate: (v: unknown) => v === true },
    noIndividualMembers: { required: true, validate: (v: unknown) => v === true },
    noLocationData: { required: true, validate: (v: unknown) => v === true },
  }
},
};
