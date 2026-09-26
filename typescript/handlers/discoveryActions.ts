/**
 * Discovery action handlers (framework-neutral).
 *
 * Member-controlled discovery actions: pause / resume, hide an option,
 * express interest in an option, and request "show fewer". Every action here
 * requires an explicit member reference supplied by the caller (in production
 * this would be bound to the authenticated session by middleware; the
 * contract stays identical).
 *
 * What these handlers deliberately DO NOT do:
 * - open or reference any message channel or notification path;
 * - create presence state (pause is not online/offline/hidden);
 * - return counterpart identity, scores, location, or activity;
 * - derive any ranking or reputation from hide/pause/interest behavior.
 */

import { isNonEmptyString } from "../core/guards.ts";
import type { DiscoveryControlStore } from "../api/inMemoryStores.ts";
import type { HttpResponse } from "../api/httpTypes.ts";
import { badRequest, noStoreJson } from "../api/httpTypes.ts";

export interface DiscoveryActionHandler {
  getPauseState(memberRef: string): HttpResponse;
  pause(memberRef: string, now: Date): HttpResponse;
  resume(memberRef: string, now: Date): HttpResponse;
  hideOption(memberRef: string, optionId: string, now: Date): HttpResponse;
  expressInterest(
    memberRef: string,
    optionId: string,
    now: Date
  ): HttpResponse;
  setShowFewer(memberRef: string, now: Date): HttpResponse;
}

/**
 * Extracts and validates `memberRef` from a parsed, already-schema-checked
 * body. Kept separate so the router can apply it uniformly to every mutable
 * discovery route.
 */
export function requireMemberRef(body: unknown): string | undefined {
  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    return undefined;
  }
  const candidate = (body as Record<string, unknown>)["memberRef"];
  return isNonEmptyString(candidate) ? candidate : undefined;
}

export function createDiscoveryActionHandler(
  store: DiscoveryControlStore
): DiscoveryActionHandler {
  const invalidMember = (): HttpResponse =>
    badRequest("invalid_member_ref");

  return {
    getPauseState(memberRef) {
      if (!isNonEmptyString(memberRef)) return invalidMember();
      // Exactly one field: the boolean control state. No timestamps, no
      // session state, no presence, no history, no reason.
      return noStoreJson(200, { paused: store.isPaused(memberRef) });
    },
    pause(memberRef, now) {
      if (!isNonEmptyString(memberRef)) return invalidMember();
      store.pause(memberRef, now);
      return noStoreJson(200, { paused: true });
    },
    resume(memberRef, now) {
      if (!isNonEmptyString(memberRef)) return invalidMember();
      store.resume(memberRef, now);
      return noStoreJson(200, { paused: false });
    },
    hideOption(memberRef, optionId, now) {
      if (!isNonEmptyString(memberRef)) return invalidMember();
      if (!isNonEmptyString(optionId)) {
        return badRequest("invalid_option_id");
      }
      store.hide(memberRef, optionId, now);
      return noStoreJson(200, { hidden: true });
    },
    expressInterest(memberRef, optionId, now) {
      if (!isNonEmptyString(memberRef)) return invalidMember();
      if (!isNonEmptyString(optionId)) {
        return badRequest("invalid_option_id");
      }
      // Records ONLY the member's own interest action. Nothing is sent,
      // nothing is opened, no counterpart is notified or revealed.
      store.recordInterest(memberRef, optionId, now);
      return noStoreJson(200, { state: "interest_recorded" });
    },
    setShowFewer(memberRef, now) {
      if (!isNonEmptyString(memberRef)) return invalidMember();
      store.setShowFewer(memberRef, now);
      return noStoreJson(200, { showFewer: true });
    }
  };
}
