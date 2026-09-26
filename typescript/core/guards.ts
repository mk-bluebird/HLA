/**
 * Hearthline core guards: pure, schema-derived predicates plus the
 * repository-wide prohibited-field denylist.
 *
 * These guards are framework-neutral and side-effect free. They are used by
 * the API layer at request boundaries and by stores when deciding what may be
 * returned to a caller.
 */

import type {
  CommunityPulse,
  EncounterCapsule,
  IntentRecognitionPreview,
  MutualVisibilityContext,
  PublicResourceCategory,
  PublicResourceEntry
} from "./types.ts";
import { PUBLIC_RESOURCE_CATEGORIES } from "./types.ts";

/* ------------------------------------------------------------------ */
/* Prohibited fields                                                   */
/* ------------------------------------------------------------------ */

/**
 * Fields that must never appear in inbound API payloads or outbound API
 * responses anywhere in Hearthline. Presence of any of these is an immediate
 * rejection (never silent stripping).
 */
export const PROHIBITED_FIELDS: readonly string[] = [
  // location / proximity
  "latitude",
  "longitude",
  "coordinates",
  "geoHash",
  "geohash",
  "distance",
  "proximity",
  "route",
  "routes",
  "locationHistory",
  "currentLocation",
  "exactLocation",
  // venue / presence
  "venueAttendance",
  "livePresence",
  "onlineStatus",
  "lastActive",
  "lastActiveAt",
  "profileViews",
  "viewedProfileRefs",
  "socialGraph",
  "contactBook",
  // scoring / ranking / reputation
  "responseSpeed",
  "photoQuality",
  "popularity",
  "compatibilityScore",
  "safetyScore",
  "riskScore",
  "trustScore",
  "reputationScore",
  "desirabilityScore",
  "engagementScore",
  "visibilityPenalty",
  // inference / automation / external action
  "responsePrediction",
  "messageSentiment",
  "privateMessageAnalysis",
  "identityInference",
  "healthInference",
  "financialInference",
  "vulnerabilityInference",
  "automaticMessaging",
  "autoSend",
  "automaticReporting",
  "emergencyEscalation",
  "externalActionAuthorization"
];

const PROHIBITED_FIELD_SET = new Set(
  PROHIBITED_FIELDS.map((field) => field.toLowerCase())
);

export function isProhibitedFieldName(key: string): boolean {
  return PROHIBITED_FIELD_SET.has(key.toLowerCase());
}

/**
 * Recursively checks a JSON value for prohibited field names. Returns the
 * offending keys (bounded) so callers can reject without echoing values.
 */
export function findProhibitedFields(value: unknown): string[] {
  const found = new Set<string>();
  const stack: unknown[] = [value];
  let budget = 5000;
  while (stack.length > 0 && budget-- > 0) {
    const current = stack.pop();
    if (Array.isArray(current)) {
      stack.push(...current);
    } else if (current !== null && typeof current === "object") {
      for (const [key, child] of Object.entries(current as Record<string, unknown>)) {
        if (isProhibitedFieldName(key)) {
          found.add(key);
        }
        stack.push(child);
      }
    }
  }
  return [...found].sort();
}

/* ------------------------------------------------------------------ */
/* Small primitives                                                    */
/* ------------------------------------------------------------------ */

export function isPlainObject(
  value: unknown
): value is Record<string, unknown> {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value) &&
    Object.getPrototypeOf(value) !== Date.prototype
  );
}

export function isNonEmptyString(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}

export function isIsoDateTime(value: unknown): value is string {
  return typeof value === "string" && !Number.isNaN(Date.parse(value));
}

export function isPastIso(value: string, now: Date): boolean {
  return Date.parse(value) <= now.getTime();
}

/* ------------------------------------------------------------------ */
/* Public resources                                                    */
/* ------------------------------------------------------------------ */

export function isPublicResourceCategory(
  value: unknown
): value is PublicResourceCategory {
  return (
    typeof value === "string" &&
    (PUBLIC_RESOURCE_CATEGORIES as readonly string[]).includes(value)
  );
}

/**
 * A resource is listable only when it is currently `listed` and has not
 * passed its review date. `under_review`, `outdated`, and `removed` entries
 * are never listed, and expired-review entries are treated as outdated.
 */
export function canListResource(
  entry: PublicResourceEntry,
  now: Date
): boolean {
  if (entry.status !== "listed") return false;
  if (!isIsoDateTime(entry.reviewBy)) return false;
  if (Date.parse(entry.reviewBy) <= now.getTime()) return false;
  return entry.eligibilityUnknownOrSourceLinked === true;
}

/* ------------------------------------------------------------------ */
/* Encounter capsules                                                  */
/* ------------------------------------------------------------------ */

/**
 * A capsule is readable only when mutually accepted and unexpired. Every
 * other state (proposed, paused, withdrawn, cancelled, expired) — or a
 * broken `externalActionAuthorized` invariant — makes it unavailable.
 */
export function canReadCapsule(capsule: EncounterCapsule, now: Date): boolean {
  if (capsule.status !== "mutually_accepted") return false;
  if (capsule.requiresMutualAcceptance !== true) return false;
  if (capsule.externalActionAuthorized !== false) return false;
  if (isPastIso(capsule.expiresAt, now)) return false;
  return true;
}

/* ------------------------------------------------------------------ */
/* Community pulse                                                     */
/* ------------------------------------------------------------------ */

export function canPublishPulse(pulse: CommunityPulse, now: Date): boolean {
  if (
    pulse.aggregatedOnly !== true ||
    pulse.noIndividualMembers !== true ||
    pulse.noLocationData !== true
  ) {
    return false;
  }
  if (isPastIso(pulse.expiresAt, now)) return false;
  return true;
}

/* ------------------------------------------------------------------ */
/* Intent recognition previews                                         */
/* ------------------------------------------------------------------ */

/**
 * Previews are confirmation-only. Anything marked executable, or whose next
 * action implies automatic execution, is rejected outright.
 */
export function isConfirmOnlyPreview(
  preview: IntentRecognitionPreview
): boolean {
  return (
    preview.queryExecutable === false &&
    preview.nextAction === "confirm_or_edit"
  );
}

/* ------------------------------------------------------------------ */
/* Mutual visibility context                                           */
/* ------------------------------------------------------------------ */

export function isMutuallyVisible(context: MutualVisibilityContext): boolean {
  return (
    context.mutualConsent === true &&
    context.visibilityAtoB !== "hidden" &&
    context.visibilityBtoA !== "hidden"
  );
}
