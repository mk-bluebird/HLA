/**
 * Hearthline core domain types (framework-neutral).
 *
 * These mirror the JSON Schemas under `schemas/` exactly. The schemas remain
 * the authoritative boundary contract; these types exist so handlers and
 * stores can be written against stable shapes without a runtime dependency.
 *
 * Intentionally absent from every type here: exact location, coordinates,
 * GeoHash, distance/proximity, routes, presence, online status, profile views,
 * social graph data, response metrics, photo quality, popularity, and any
 * compatibility / safety / risk / trust / reputation / desirability score.
 */

export type ISODateTimeString = string;

/* ------------------------------------------------------------------ */
/* Public Resources Library                                            */
/* schemas/public-resource-entry.schema.json                           */
/* ------------------------------------------------------------------ */

export const PUBLIC_RESOURCE_CATEGORIES = [
  "transit",
  "food",
  "housing_information",
  "tenant_rights",
  "health_information",
  "community_clinics",
  "recovery_information",
  "legal_information",
  "employment_information",
  "worker_rights",
  "accessibility_information",
  "community_information",
  "emergency_information"
] as const;

export type PublicResourceCategory =
  (typeof PUBLIC_RESOURCE_CATEGORIES)[number];

export const PUBLIC_RESOURCE_SOURCE_TYPES = [
  "government",
  "official_provider",
  "public_nonprofit",
  "public_directory",
  "public_library",
  "public_transit_agency",
  "legal_aid_directory",
  "community_organization"
] as const;

export type PublicResourceSourceType =
  (typeof PUBLIC_RESOURCE_SOURCE_TYPES)[number];

export type PublicResourceStatus =
  | "listed"
  | "under_review"
  | "outdated"
  | "removed";

export interface PublicResourceEntry {
  readonly resourceId: string;
  readonly title: string;
  readonly category: PublicResourceCategory;
  readonly officialOrPublisherSource: string;
  readonly sourceUrl: string;
  readonly sourceType: PublicResourceSourceType;
  readonly jurisdiction: string;
  readonly plainLanguageSummary: string;
  readonly contactMethodAsPublished?: string;
  readonly accessibilityInformationIfPubliclyAvailable?: string;
  /** Always true: eligibility is never asserted by Hearthline. */
  readonly eligibilityUnknownOrSourceLinked: true;
  readonly retrievedAt: ISODateTimeString;
  readonly reviewBy: ISODateTimeString;
  readonly status: PublicResourceStatus;
}

/* ------------------------------------------------------------------ */
/* Pause Discovery                                                     */
/* schemas/pause-discovery-state.schema.json                           */
/* ------------------------------------------------------------------ */

export type PauseDiscoveryStateName = "active" | "resumed" | "deleted";

export interface PauseDiscoveryState {
  readonly pauseId: string;
  readonly ownerRef: string;
  readonly state: PauseDiscoveryStateName;
  readonly createdAt: ISODateTimeString;
  readonly updatedAt: ISODateTimeString;
  readonly expiresAt?: ISODateTimeString;
}

/* ------------------------------------------------------------------ */
/* Mutual Visibility Context                                           */
/* schemas/mutual-visibility-context.schema.json                       */
/* ------------------------------------------------------------------ */

export type VisibilityLevel =
  | "hidden"
  | "broad_category_only"
  | "shared_context_visible"
  | "direct";

export interface MutualVisibilityContext {
  readonly contextId: string;
  readonly memberRefA: string;
  readonly memberRefB: string;
  readonly visibilityAtoB: VisibilityLevel;
  readonly visibilityBtoA: VisibilityLevel;
  readonly mutualConsent: boolean;
  readonly sharedActivityScope?: string;
  readonly generatedAt: ISODateTimeString;
}

/* ------------------------------------------------------------------ */
/* Encounter Capsule                                                   */
/* schemas/encounter-capsule.schema.json                               */
/* ------------------------------------------------------------------ */

export type EncounterCapsulePurpose =
  | "meeting_detail"
  | "place_description"
  | "context_note";

export type EncounterCapsuleStatus =
  | "proposed"
  | "mutually_accepted"
  | "paused"
  | "withdrawn"
  | "cancelled"
  | "expired";

export type EncounterCapsuleContentFormat = "text_plain";
export type EncounterCapsuleContentSize = "short" | "medium";

export interface EncounterCapsule {
  readonly capsuleId: string;
  readonly participants: readonly [string, string];
  readonly purpose: EncounterCapsulePurpose;
  readonly status: EncounterCapsuleStatus;
  readonly createdAt: ISODateTimeString;
  readonly mutuallyAcceptedAt?: ISODateTimeString;
  readonly expiresAt: ISODateTimeString;
  readonly contentFormat: EncounterCapsuleContentFormat;
  readonly contentSize: EncounterCapsuleContentSize;
  readonly requiresMutualAcceptance: true;
  readonly noExportUi: true;
  readonly noPlatformRecovery: true;
  /** Permanent boundary: a capsule never authorizes an external action. */
  readonly externalActionAuthorized: false;
}

/* ------------------------------------------------------------------ */
/* Intent Recognition Preview                                          */
/* schemas/intent-recognition-preview.schema.json                      */
/* ------------------------------------------------------------------ */

export type RecognizedIntentKind =
  | "shared_activity_suggestion"
  | "meeting_logistics"
  | "boundary_statement"
  | "cancellation_request"
  | "resource_inquiry";

export type IntentNextAction = "confirm_or_edit" | "no_action";

export interface IntentRecognitionPreview {
  readonly previewId: string;
  readonly requesterMemberRef: string;
  readonly recognizedIntent: RecognizedIntentKind;
  readonly proposedText: string;
  readonly nextAction: IntentNextAction;
  /** Permanent boundary: previews are never executable. */
  readonly queryExecutable: false;
  readonly generatedAt: ISODateTimeString;
}

/* ------------------------------------------------------------------ */
/* Discovery Surface Allocation                                        */
/* schemas/discovery-surface-allocation.schema.json                    */
/* ------------------------------------------------------------------ */

export type DiscoveryPresentationMode =
  | "chronological"
  | "one_at_a_time"
  | "category_browse";

export interface DiscoverySurfaceAllocation {
  readonly allocationId: string;
  readonly memberRef: string;
  readonly presentationMode: DiscoveryPresentationMode;
  readonly chosenCategory?: string;
  readonly optionRefs: readonly string[];
  readonly showFewerRequested: boolean;
  readonly generatedAt: ISODateTimeString;
}

/* ------------------------------------------------------------------ */
/* Community Pulse                                                     */
/* schemas/community-pulse.schema.json                                 */
/* ------------------------------------------------------------------ */

export interface CommunityPulse {
  readonly pulseId: string;
  readonly scopeLabel: string;
  readonly headline: string;
  readonly detailSummary: string;
  readonly windowStart: ISODateTimeString;
  readonly windowEnd: ISODateTimeString;
  readonly publishedAt: ISODateTimeString;
  readonly expiresAt: ISODateTimeString;
  /** All three must be true for a pulse to be publishable. */
  readonly aggregatedOnly: true;
  readonly noIndividualMembers: true;
  readonly noLocationData: true;
}

/* ------------------------------------------------------------------ */
/* API request payloads (member-controlled discovery actions)          */
/* ------------------------------------------------------------------ */

export interface MemberActionRequest {
  readonly memberRef: string;
}

export interface PublicResourceListQuery {
  readonly category?: PublicResourceCategory;
  readonly jurisdiction?: string;
}
