/**
 * Narrow in-memory stores for the Hearthline API milestone.
 *
 * Deliberately NOT a universal member-profile store: each store holds only
 * the minimum state required for its own testable control, and none of them
 * can record browsing history, presence, location, scores, or messaging
 * state — those capabilities are intentionally absent from this layer.
 */

import type {
  CommunityPulse,
  EncounterCapsule,
  PublicResourceCategory,
  PublicResourceEntry
} from "../core/types.ts";
import {
  canListResource,
  canPublishPulse,
  canReadCapsule
} from "../core/guards.ts";

/* ------------------------------------------------------------------ */
/* PublicResourceStore                                                 */
/* ------------------------------------------------------------------ */

export interface PublicResourceStore {
  seed(entries: readonly PublicResourceEntry[]): void;
  list(input: {
    readonly category?: PublicResourceCategory;
    readonly jurisdiction?: string;
    readonly now: Date;
  }): readonly PublicResourceEntry[];
  get(resourceId: string, now: Date): PublicResourceEntry | undefined;
}

export function createPublicResourceStore(): PublicResourceStore {
  const byId = new Map<string, PublicResourceEntry>();

  return {
    seed(entries) {
      // Seed data is public catalog content only; no member identity is ever
      // accepted here, and no lookup activity is recorded anywhere below.
      for (const entry of entries) {
        byId.set(entry.resourceId, entry);
      }
    },
    list({ category, jurisdiction, now }) {
      const out: PublicResourceEntry[] = [];
      for (const entry of byId.values()) {
        if (!canListResource(entry, now)) continue;
        if (category !== undefined && entry.category !== category) continue;
        if (
          jurisdiction !== undefined &&
          entry.jurisdiction !== jurisdiction
        ) {
          continue;
        }
        out.push(entry);
      }
      // Stable, source-ordered output — never ranked, scored, or personalized.
      out.sort((a, b) => (a.resourceId < b.resourceId ? -1 : 1));
      return out;
    },
    get(resourceId, now) {
      const entry = byId.get(resourceId);
      if (!entry) return undefined;
      return canListResource(entry, now) ? entry : undefined;
    }
  };
}

/* ------------------------------------------------------------------ */
/* DiscoveryControlStore                                               */
/* ------------------------------------------------------------------ */

export interface DiscoveryControlStore {
  isPaused(memberRef: string): boolean;
  pause(memberRef: string, now: Date): void;
  resume(memberRef: string, now: Date): void;

  hide(memberRef: string, optionId: string, now: Date): void;
  isHidden(memberRef: string, optionId: string): boolean;

  recordInterest(memberRef: string, optionId: string, now: Date): void;
  hasInterest(memberRef: string, optionId: string): boolean;

  setShowFewer(memberRef: string, now: Date): void;
  wantsShowFewer(memberRef: string): boolean;
}

/**
 * Stores only booleans keyed by member + control. `now` is accepted for API
 * symmetry with the handlers but is deliberately NOT persisted: pause
 * timestamps, viewing history, and any derived score do not exist here.
 * Pausing affects only new candidate generation (see
 * `isEligibleForCandidateGeneration`); it is not a presence state.
 */
export function createDiscoveryControlStore(): DiscoveryControlStore {
  const paused = new Set<string>();
  const hidden = new Set<string>();
  const interests = new Set<string>();
  const showFewer = new Set<string>();

  const pairKey = (memberRef: string, optionId: string): string =>
    `${memberRef}\u0000${optionId}`;

  return {
    isPaused(memberRef) {
      return paused.has(memberRef);
    },
    pause(memberRef) {
      paused.add(memberRef);
    },
    resume(memberRef) {
      paused.delete(memberRef);
    },
    hide(memberRef, optionId) {
      hidden.add(pairKey(memberRef, optionId));
    },
    isHidden(memberRef, optionId) {
      return hidden.has(pairKey(memberRef, optionId));
    },
    recordInterest(memberRef, optionId) {
      interests.add(pairKey(memberRef, optionId));
    },
    hasInterest(memberRef, optionId) {
      return interests.has(pairKey(memberRef, optionId));
    },
    setShowFewer(memberRef) {
      showFewer.add(memberRef);
    },
    wantsShowFewer(memberRef) {
      return showFewer.has(memberRef);
    }
  };
}

/**
 * Candidate-generation hook used by discovery surfaces: a paused member is
 * excluded from NEW candidate generation. This is the full extent of what
 * pause does in this milestone.
 */
export function isEligibleForCandidateGeneration(
  store: DiscoveryControlStore,
  memberRef: string
): boolean {
  return !store.isPaused(memberRef);
}

/**
 * Filters options for one member: hidden options never resurface to that
 * member, and nothing else about the option set changes (no reordering).
 */
export function filterVisibleOptions(
  store: DiscoveryControlStore,
  memberRef: string,
  optionIds: readonly string[]
): readonly string[] {
  return optionIds.filter((optionId) => !store.isHidden(memberRef, optionId));
}

/* ------------------------------------------------------------------ */
/* EncounterCapsuleStore                                               */
/* ------------------------------------------------------------------ */

export interface EncounterCapsuleStore {
  put(capsule: EncounterCapsule): void;
  getActive(capsuleId: string, now: Date): EncounterCapsule | undefined;
}

export function createEncounterCapsuleStore(): EncounterCapsuleStore {
  const byId = new Map<string, EncounterCapsule>();

  return {
    put(capsule) {
      byId.set(capsule.capsuleId, capsule);
    },
    getActive(capsuleId, now) {
      const capsule = byId.get(capsuleId);
      if (!capsule) return undefined;
      // Only mutually accepted AND unexpired capsules are readable. Proposed,
      // paused, withdrawn, cancelled, and expired capsules all return
      // undefined here so the router can answer `capsule_not_available`.
      if (!canReadCapsule(capsule, now)) return undefined;
      return capsule;
    }
  };
}

/* ------------------------------------------------------------------ */
/* CommunityPulseStore                                                 */
/* ------------------------------------------------------------------ */

export interface CommunityPulseStore {
  put(pulse: CommunityPulse): void;
  getCurrent(now: Date): CommunityPulse | undefined;
}

export function createCommunityPulseStore(): CommunityPulseStore {
  const pulses: CommunityPulse[] = [];

  return {
    put(pulse) {
      pulses.push(pulse);
    },
    getCurrent(now) {
      // Newest first among publishable pulses; anything failing
      // canPublishPulse (privacy flags or expiry) is never returned.
      const eligible = pulses
        .filter((pulseItem) => canPublishPulse(pulseItem, now))
        .sort(
          (a, b) => Date.parse(b.publishedAt) - Date.parse(a.publishedAt)
        );
      return eligible[0];
    }
  };
}
