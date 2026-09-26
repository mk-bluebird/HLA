/**
 * Hearthline API milestone tests.
 *
 * Runner: Node's built-in `node:test` module (available in the repository's
 * Node 20 toolchain; no test library is installed). Run with:
 *
 *   node --experimental-strip-types --test tests/hearthlineApi.test.ts
 *
 * Fixtures are local, synthetic identifiers only — no people, no private
 * messages, no exact locations, no health/identity data.
 */

import { test } from "node:test";
import assert from "node:assert/strict";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

import type { HttpRequest } from "../typescript/api/httpTypes.ts";
import { createSchemaValidator, SCHEMAS } from "../typescript/api/schemaValidation.ts";
import {
  createCommunityPulseStore,
  createDiscoveryControlStore,
  createEncounterCapsuleStore,
  createPublicResourceStore
} from "../typescript/api/inMemoryStores.ts";
import { createHearthlineApi } from "../typescript/api/hearthlineApi.ts";
import type {
  CommunityPulse,
  EncounterCapsule,
  IntentRecognitionPreview,
  PublicResourceEntry
} from "../typescript/core/types.ts";

const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const NOW = new Date("2026-09-27T12:00:00Z");

/* ------------------------------------------------------------------ */
/* Fixtures                                                            */
/* ------------------------------------------------------------------ */

function listedResource(): PublicResourceEntry {
  return {
    resourceId: "resource_transitinfo_000000000001",
    title: "Regional transit fare information (official publisher page)",
    category: "transit",
    officialOrPublisherSource: "Example Regional Transit Agency",
    sourceUrl: "https://transit.example.invalid/fares",
    sourceType: "public_transit_agency",
    jurisdiction: "Example County",
    plainLanguageSummary:
      "Published fare tables and pass information, as provided by the agency.",
    eligibilityUnknownOrSourceLinked: true,
    retrievedAt: "2026-09-01T00:00:00Z",
    reviewBy: "2026-12-01T00:00:00Z",
    status: "listed"
  };
}

function outdatedResource(): PublicResourceEntry {
  return {
    ...listedResource(),
    resourceId: "resource_foodinfo_00000000000002",
    title: "Expired community meal program listing",
    category: "food",
    reviewBy: "2026-09-01T00:00:00Z", // already passed relative to NOW
    status: "outdated"
  };
}

function acceptedCapsule(): EncounterCapsule {
  return {
    capsuleId: "capsule_testaccept_000000000001",
    participants: ["member_alpha_test_0000000001", "member_beta_test_0000000001"],
    purpose: "meeting_detail",
    status: "mutually_accepted",
    createdAt: "2026-09-20T00:00:00Z",
    mutuallyAcceptedAt: "2026-09-21T00:00:00Z",
    expiresAt: "2026-10-01T00:00:00Z",
    contentFormat: "text_plain",
    contentSize: "short",
    requiresMutualAcceptance: true,
    noExportUi: true,
    noPlatformRecovery: true,
    externalActionAuthorized: false
  };
}

function proposedCapsule(): EncounterCapsule {
  return { ...acceptedCapsule(), capsuleId: "capsule_testpropose_0000000001", status: "proposed" };
}

function expiredCapsule(): EncounterCapsule {
  return {
    ...acceptedCapsule(),
    capsuleId: "capsule_testexpired_000000001",
    expiresAt: "2026-09-26T00:00:00Z" // before NOW
  };
}

function currentPulse(): CommunityPulse {
  return {
    pulseId: "pulse_testcurrent_000000000001",
    scopeLabel: "Platform-wide weekly summary",
    headline: "More members used pause discovery this week than last week",
    detailSummary:
      "An aggregate trend across all members. No counts, no individuals, no places.",
    windowStart: "2026-09-20T00:00:00Z",
    windowEnd: "2026-09-27T00:00:00Z",
    publishedAt: "2026-09-27T06:00:00Z",
    expiresAt: "2026-10-04T00:00:00Z",
    aggregatedOnly: true,
    noIndividualMembers: true,
    noLocationData: true
  };
}

function validPreview(): IntentRecognitionPreview {
  return {
    previewId: "intentprev_testvalid_0000000001",
    requesterMemberRef: "member_alpha_test_0000000001",
    recognizedIntent: "shared_activity_suggestion",
    proposedText: "Suggest browsing board-games options together?",
    nextAction: "confirm_or_edit",
    queryExecutable: false,
    generatedAt: "2026-09-27T10:00:00Z"
  };
}

const MEMBER_A = "member_alpha_test_0000000001";
const MEMBER_B = "member_beta_test_0000000001";

interface Harness {
  readonly request: (r: HttpRequest) => Promise<{ status: number; body: any; headers?: Record<string, string> }>;
  readonly stores: ReturnType<typeof createStores>;
}

function createStores() {
  const resourceStore = createPublicResourceStore();
  resourceStore.seed([listedResource(), outdatedResource()]);
  const discoveryStore = createDiscoveryControlStore();
  const capsuleStore = createEncounterCapsuleStore();
  capsuleStore.put(acceptedCapsule());
  capsuleStore.put(proposedCapsule());
  capsuleStore.put(expiredCapsule());
  const pulseStore = createCommunityPulseStore();
  pulseStore.put(currentPulse());
  return { resourceStore, discoveryStore, capsuleStore, pulseStore };
}

function createHarness(now: Date = NOW): Harness {
  const stores = createStores();
  const api = createHearthlineApi({
    resourceStore: stores.resourceStore,
    discoveryStore: stores.discoveryStore,
    capsuleStore: stores.capsuleStore,
    pulseStore: stores.pulseStore,
    validator: createSchemaValidator(REPO_ROOT),
    now: () => now
  });
  return {
    stores,
    request: async (r) => {
      const response = await api(r);
      return {
        status: response.status,
        body: response.body as any,
        headers: response.headers as Record<string, string> | undefined
      };
    }
  };
}

function get(path: string, query: Record<string, string | undefined> = {}): HttpRequest {
  return { method: "GET", path, query };
}

function post(path: string, body: unknown): HttpRequest {
  return { method: "POST", path, query: {}, body };
}

function errorBody(res: { body: any }): string {
  return res.body?.error?.code ?? "<no code>";
}

/* ------------------------------------------------------------------ */
/* Public Resources Library                                            */
/* ------------------------------------------------------------------ */

test("1. listed, current resource is returned", async () => {
  const h = createHarness();
  const res = await h.request(get("/public-resources"));
  assert.equal(res.status, 200);
  assert.equal(res.body.entries.length, 1);
  assert.equal(res.body.entries[0].resourceId, listedResource().resourceId);
});

test("2. outdated resource returns resource_not_found", async () => {
  const h = createHarness();
  const res = await h.request(get(`/public-resources/${outdatedResource().resourceId}`));
  assert.equal(res.status, 404);
  assert.equal(errorBody(res), "resource_not_found");
});

test("3. unknown category query value returns bad_request", async () => {
  const h = createHarness();
  const res = await h.request(get("/public-resources", { category: "not_a_category" }));
  assert.equal(res.status, 400);
  assert.ok(["bad_request", "invalid_payload"].includes(errorBody(res)));
});

test("4. unsupported query parameter returns unsupported_query_parameter", async () => {
  const h = createHarness();
  const res = await h.request(get("/public-resources", { sort: "popularity" }));
  assert.equal(res.status, 400);
  assert.equal(errorBody(res), "unsupported_query_parameter");
});

test("5. browse request has no member identifier and does not require one", async () => {
  const h = createHarness();
  const res = await h.request(get("/public-resources", { category: "transit" }));
  assert.equal(res.status, 200);
  assert.equal(res.body.entries.length, 1);
  // The store never received a member ref: browsing records nothing.
  assert.equal(h.stores.discoveryStore.isPaused(MEMBER_A), false);
});

test("6. resource response preserves eligibilityUnknownOrSourceLinked: true", async () => {
  const h = createHarness();
  const res = await h.request(get(`/public-resources/${listedResource().resourceId}`));
  assert.equal(res.status, 200);
  assert.equal(res.body.eligibilityUnknownOrSourceLinked, true);
});

/* ------------------------------------------------------------------ */
/* Pause Discovery                                                     */
/* ------------------------------------------------------------------ */

test("7. member starts unpaused", async () => {
  const h = createHarness();
  const res = await h.request(get("/discovery/pause", { memberRef: MEMBER_A }));
  assert.equal(res.status, 200);
  assert.deepEqual(res.body, { paused: false });
});

test("8. POST /discovery/pause pauses the member", async () => {
  const h = createHarness();
  const res = await h.request(post("/discovery/pause", { memberRef: MEMBER_A }));
  assert.equal(res.status, 200);
  assert.deepEqual(res.body, { paused: true });
  assert.equal(h.stores.discoveryStore.isPaused(MEMBER_A), true);
});

test("9. GET /discovery/pause returns only { paused: true }", async () => {
  const h = createHarness();
  await h.request(post("/discovery/pause", { memberRef: MEMBER_A }));
  const res = await h.request(get("/discovery/pause", { memberRef: MEMBER_A }));
  assert.deepEqual(Object.keys(res.body), ["paused"]);
  assert.equal(res.body.paused, true);
});

test("10. resume returns { paused: false }", async () => {
  const h = createHarness();
  await h.request(post("/discovery/pause", { memberRef: MEMBER_A }));
  const res = await h.request(post("/discovery/pause/resume", { memberRef: MEMBER_A }));
  assert.equal(res.status, 200);
  assert.deepEqual(res.body, { paused: false });
  assert.equal(h.stores.discoveryStore.isPaused(MEMBER_A), false);
});

test("11. pause endpoint rejects unknown request fields", async () => {
  const h = createHarness();
  const res = await h.request(
    post("/discovery/pause", { memberRef: MEMBER_A, reason: "too busy" })
  );
  assert.equal(res.status, 400);
  assert.equal(errorBody(res), "unknown_field");
});

test("12. pause response includes no timestamp, presence, location, or history", async () => {
  const h = createHarness();
  await h.request(post("/discovery/pause", { memberRef: MEMBER_A }));
  const res = await h.request(get("/discovery/pause", { memberRef: MEMBER_A }));
  const serialized = JSON.stringify(res.body).toLowerCase();
  for (const banned of ["time", "date", "presence", "online", "location", "history", "lastactive"]) {
    assert.ok(!serialized.includes(banned), `response leaked ${banned}`);
  }
  assert.equal(res.headers?.["cache-control"], "no-store");
});

/* ------------------------------------------------------------------ */
/* Discovery controls                                                  */
/* ------------------------------------------------------------------ */

test("13. hide returns { hidden: true }", async () => {
  const h = createHarness();
  const res = await h.request(
    post("/shared-activity-discovery/options/option_boardgames_0000000001/hide", {
      memberRef: MEMBER_A
    })
  );
  assert.equal(res.status, 200);
  assert.deepEqual(res.body, { hidden: true });
});

test("14. hidden option is recorded hidden only for that member", async () => {
  const h = createHarness();
  const optionId = "option_boardgames_0000000001";
  await h.request(
    post(`/shared-activity-discovery/options/${optionId}/hide`, { memberRef: MEMBER_A })
  );
  assert.equal(h.stores.discoveryStore.isHidden(MEMBER_A, optionId), true);
  assert.equal(h.stores.discoveryStore.isHidden(MEMBER_B, optionId), false);
});

test("15. express interest returns only interest_recorded", async () => {
  const h = createHarness();
  const res = await h.request(
    post(
      "/shared-activity-discovery/options/option_bookclub_00000000001/express-interest",
      { memberRef: MEMBER_A }
    )
  );
  assert.equal(res.status, 200);
  assert.deepEqual(res.body, { state: "interest_recorded" });
});

test("16. express interest does not create message state", async () => {
  const h = createHarness();
  const optionId = "option_bookclub_00000000001";
  await h.request(
    post(`/shared-activity-discovery/options/${optionId}/express-interest`, {
      memberRef: MEMBER_A
    })
  );
  // The control store exposes no messaging surface at all; the only thing
  // recorded is the member's own interest flag.
  assert.equal(h.stores.discoveryStore.hasInterest(MEMBER_A, optionId), true);
  assert.equal(h.stores.discoveryStore.hasInterest(MEMBER_B, optionId), false);
  const keys = Object.getOwnPropertyNames(h.stores.discoveryStore);
  for (const key of keys) {
    assert.ok(!/message|channel|notif/i.test(key), `store exposes ${key}`);
  }
});

test("17. show-fewer state can be set", async () => {
  const h = createHarness();
  const res = await h.request(
    post("/shared-activity-discovery/preferences/show-fewer", { memberRef: MEMBER_A })
  );
  assert.equal(res.status, 200);
  assert.equal(h.stores.discoveryStore.wantsShowFewer(MEMBER_A), true);
  assert.equal(h.stores.discoveryStore.wantsShowFewer(MEMBER_B), false);
});

test("18. missing memberRef is rejected", async () => {
  const h = createHarness();
  const res = await h.request(post("/discovery/pause", {}));
  assert.equal(res.status, 400);
  assert.equal(errorBody(res), "missing_field");
});

/* ------------------------------------------------------------------ */
/* Encounter capsules                                                  */
/* ------------------------------------------------------------------ */

test("19. mutually accepted, unexpired capsule can be read", async () => {
  const h = createHarness();
  const res = await h.request(get(`/encounter-capsules/${acceptedCapsule().capsuleId}`));
  assert.equal(res.status, 200);
  assert.equal(res.body.status, "mutually_accepted");
  assert.equal(res.headers?.["cache-control"], "no-store");
});

test("20. proposed capsule returns capsule_not_available", async () => {
  const h = createHarness();
  const res = await h.request(get(`/encounter-capsules/${proposedCapsule().capsuleId}`));
  assert.equal(res.status, 404);
  assert.equal(errorBody(res), "capsule_not_available");
});

test("21. expired capsule returns capsule_not_available", async () => {
  const h = createHarness();
  const res = await h.request(get(`/encounter-capsules/${expiredCapsule().capsuleId}`));
  assert.equal(res.status, 404);
  assert.equal(errorBody(res), "capsule_not_available");
});

test("22. capsule response preserves externalActionAuthorized: false", async () => {
  const h = createHarness();
  const res = await h.request(get(`/encounter-capsules/${acceptedCapsule().capsuleId}`));
  assert.equal(res.body.externalActionAuthorized, false);
  assert.equal(res.body.noExportUi, true);
  assert.equal(res.body.noPlatformRecovery, true);
  assert.equal(res.body.requiresMutualAcceptance, true);
});

/* ------------------------------------------------------------------ */
/* Community pulse                                                     */
/* ------------------------------------------------------------------ */

test("23. current pulse returned only when privacy flags are all true", async () => {
  const h = createHarness();
  const res = await h.request(get("/community-pulse"));
  assert.equal(res.status, 200);
  assert.equal(res.body.aggregatedOnly, true);
  assert.equal(res.body.noIndividualMembers, true);
  assert.equal(res.body.noLocationData, true);
});

test("24. expired pulse returns community_pulse_not_available", async () => {
  const stores = createStores();
  stores.pulseStore.put({ ...currentPulse(), expiresAt: "2026-09-26T00:00:00Z" });
  // Remove the current pulse by rebuilding the store with only an expired one.
  const expiredOnly = createCommunityPulseStore();
  expiredOnly.put({ ...currentPulse(), expiresAt: "2026-09-26T00:00:00Z" });
  const api = createHearthlineApi({
    resourceStore: stores.resourceStore,
    discoveryStore: stores.discoveryStore,
    capsuleStore: stores.capsuleStore,
    pulseStore: expiredOnly,
    validator: createSchemaValidator(REPO_ROOT),
    now: () => NOW
  });
  const res = await api(get("/community-pulse"));
  assert.equal(res.status, 404);
  assert.equal((res.body as any).error.code, "community_pulse_not_available");
});

test("25. pulse response has no raw count, location, presence, route, venue, or counterpart fields", async () => {
  const h = createHarness();
  const res = await h.request(get("/community-pulse"));
  const serialized = JSON.stringify(res.body).toLowerCase();
  for (const banned of [
    "count",
    "latitude",
    "longitude",
    "location",
    "presence",
    "online",
    "route",
    "venue",
    "attend",
    "member_",
    "distance",
    "proximity"
  ]) {
    assert.ok(!serialized.includes(banned), `pulse leaked ${banned}`);
  }
});

/* ------------------------------------------------------------------ */
/* Intent preview                                                      */
/* ------------------------------------------------------------------ */

test("26. valid confirmation-required preview is accepted unchanged", async () => {
  const h = createHarness();
  const preview = validPreview();
  const res = await h.request(post("/intent-recognition/previews", preview));
  assert.equal(res.status, 200);
  assert.equal(res.body.acceptedForConfirmation, true);
  assert.deepEqual(res.body.preview, preview);
});

test("27. preview with queryExecutable: true is rejected", async () => {
  const h = createHarness();
  const res = await h.request(
    post("/intent-recognition/previews", { ...validPreview(), queryExecutable: true })
  );
  assert.equal(res.status, 400);
  assert.notEqual(res.body.acceptedForConfirmation, true);
});

test("28. preview with an unknown field is rejected", async () => {
  const h = createHarness();
  const res = await h.request(
    post("/intent-recognition/previews", { ...validPreview(), autoSend: false })
  );
  assert.equal(res.status, 400);
  assert.equal(errorBody(res), "unknown_field");
});

test("29. preview is not persisted", async () => {
  const h = createHarness();
  const preview = validPreview();
  await h.request(post("/intent-recognition/previews", preview));
  // There is no retrieval route for previews, and the dependency set has no
  // preview store; re-fetching via any plausible path must 404.
  const res = await h.request(get(`/intent-recognition/previews/${preview.previewId}`));
  assert.equal(res.status, 404);
  assert.equal(errorBody(res), "route_not_found");
});

/* ------------------------------------------------------------------ */
/* Prohibited fields                                                   */
/* ------------------------------------------------------------------ */

test("30. prohibited fields are rejected on every mutable endpoint", async () => {
  const prohibitedBodies: Array<Record<string, unknown>> = [
    { memberRef: MEMBER_A, latitude: 1.0 },
    { memberRef: MEMBER_A, longitude: 2.0 },
    { memberRef: MEMBER_A, proximity: "near" },
    { memberRef: MEMBER_A, distance: 5 },
    { memberRef: MEMBER_A, safetyScore: 0.9 },
    { memberRef: MEMBER_A, riskScore: 0.1 },
    { memberRef: MEMBER_A, compatibilityScore: 0.5 },
    { memberRef: MEMBER_A, profileViews: 12 },
    { memberRef: MEMBER_A, messageSentiment: "positive" }
  ];
  const endpoints = [
    "/discovery/pause",
    "/discovery/pause/resume",
    `/shared-activity-discovery/options/option_boardgames_0000000001/hide`,
    `/shared-activity-discovery/options/option_boardgames_0000000001/express-interest`,
    "/shared-activity-discovery/preferences/show-fewer"
  ];
  for (const endpoint of endpoints) {
    for (const body of prohibitedBodies) {
      const h = createHarness();
      const res = await h.request(post(endpoint, body));
      assert.equal(res.status, 400, `${endpoint} accepted ${JSON.stringify(Object.keys(body))}`);
      assert.equal(errorBody(res), "prohibited_field", `at ${endpoint}`);
      // And nothing was mutated: e.g. hiding did not record state.
      if (endpoint === "/discovery/pause") {
        assert.equal(h.stores.discoveryStore.isPaused(MEMBER_A), false);
      }
    }
  }
  // Also enforced on the intent-preview route.
  const h = createHarness();
  const res = await h.request(
    post("/intent-recognition/previews", { ...validPreview(), coordinates: "1,2" })
  );
  assert.equal(res.status, 400);
  assert.equal(errorBody(res), "prohibited_field");
});

/* ------------------------------------------------------------------ */
/* Unsupported routes + schema constants                               */
/* ------------------------------------------------------------------ */

test("unsupported routes return route_not_found", async () => {
  const h = createHarness();
  const cases: HttpRequest[] = [
    { method: "DELETE", path: "/discovery/pause", query: {} },
    post("/members/lookup", { memberRef: MEMBER_A }),
    get("/encounter-capsules"),
    { method: "PATCH", path: "/community-pulse", query: {} },
    post("/public-resources", {})
  ];
  for (const c of cases) {
    const res = await h.request(c);
    assert.equal(res.status, 404, `${c.method} ${c.path}`);
    assert.equal(errorBody(res), "route_not_found", `${c.method} ${c.path}`);
  }
});

test("SCHEMAS constants point at files that validate against the adapter", () => {
  const validator = createSchemaValidator(REPO_ROOT);
  // Each registered schema loads and cross-checks without drift errors.
  for (const file of Object.values(SCHEMAS)) {
    const result = validator.validate(file, {});
    assert.equal(result.valid, false); // empty object fails required fields...
    assert.ok(result.errors.includes("missing_field"));
  }
  const ok = validator.validate(SCHEMAS.intentRecognitionPreview, validPreview());
  assert.equal(ok.valid, true);
});
