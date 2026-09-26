/**
 * Hearthline framework-neutral API router.
 *
 * `createHearthlineApi` returns a pure `(HttpRequest) => Promise<HttpResponse>`
 * function: no server, no framework, no network binding. A future service can
 * mount it behind any transport and add authentication/authorization
 * middleware in front of it without changing these request contracts.
 *
 * Boundary rules enforced here:
 * - Every mutable route rejects prohibited fields (location, scores,
 *   presence, messaging, inference, automation) before anything else —
 *   rejection, never stripping.
 * - Bodies are validated against the repository JSON Schemas (or the reviewed
 *   member-action shape) via the injected `SchemaValidator`.
 * - Unknown query parameters and unknown routes get stable error codes.
 * - Sensitive state responses are `Cache-Control: no-store`.
 */

import type { HttpRequest, HttpResponse } from "./httpTypes.ts";
import { badRequest, noStoreJson, notFound } from "./httpTypes.ts";
import {
  SCHEMAS,
  SchemaValidationError,
  validateMemberActionBody,
  type SchemaValidator
} from "./schemaValidation.ts";
import type {
  CommunityPulseStore,
  DiscoveryControlStore,
  EncounterCapsuleStore,
  PublicResourceStore
} from "./inMemoryStores.ts";
import {
  createPublicResourcesHandler,
  handleGetPublicResource,
  handleListPublicResources
} from "../handlers/publicResources.ts";
import {
  createDiscoveryActionHandler,
  requireMemberRef
} from "../handlers/discoveryActions.ts";
import { findProhibitedFields, isConfirmOnlyPreview } from "../core/guards.ts";
import type {
  EncounterCapsule,
  IntentRecognitionPreview
} from "../core/types.ts";

export interface HearthlineApiDependencies {
  readonly resourceStore: PublicResourceStore;
  readonly discoveryStore: DiscoveryControlStore;
  readonly capsuleStore: EncounterCapsuleStore;
  readonly pulseStore: CommunityPulseStore;
  readonly validator: SchemaValidator;
  readonly now?: () => Date;
}

const PUBLIC_RESOURCE_LIST_PARAMS = new Set(["category", "jurisdiction"]);
const PAUSE_GET_PARAMS = new Set(["memberRef"]);

/** Prohibited-field pre-check shared by every mutable endpoint. */
function prohibitedFieldRejection(body: unknown): HttpResponse | undefined {
  const offenders = findProhibitedFields(body);
  if (offenders.length === 0) return undefined;
  // Report only that prohibited fields were present — never their values.
  return badRequest(
    "prohibited_field",
    `request contains prohibited field(s): ${offenders.slice(0, 3).join(", ")}`
  );
}

function schemaErrorToResponse(error: unknown): HttpResponse {
  if (error instanceof SchemaValidationError) {
    switch (error.code) {
      case "unknown_field":
        return badRequest("unknown_field");
      case "missing_field":
        return badRequest("missing_field");
      case "invalid_payload":
        return badRequest("invalid_payload");
      default:
        // schema_not_registered etc. is a server-side wiring problem; do not
        // leak internals to clients.
        return noStoreJson(500, { error: { code: "internal_error" } });
    }
  }
  return noStoreJson(500, { error: { code: "internal_error" } });
}

/** Capsule metadata projection — exactly the fields this milestone exposes. */
function capsuleMetadata(capsule: EncounterCapsule): Record<string, unknown> {
  return {
    capsuleId: capsule.capsuleId,
    purpose: capsule.purpose,
    status: capsule.status,
    expiresAt: capsule.expiresAt,
    contentFormat: capsule.contentFormat,
    contentSize: capsule.contentSize,
    requiresMutualAcceptance: capsule.requiresMutualAcceptance,
    noExportUi: capsule.noExportUi,
    noPlatformRecovery: capsule.noPlatformRecovery,
    externalActionAuthorized: capsule.externalActionAuthorized
  };
}

function splitPath(path: string): string[] {
  return path.split("?")[0].split("/").filter((segment) => segment.length > 0);
}

export function createHearthlineApi(
  dependencies: HearthlineApiDependencies
): (request: HttpRequest) => Promise<HttpResponse> {
  const resources = createPublicResourcesHandler(dependencies.resourceStore);
  const discovery = createDiscoveryActionHandler(dependencies.discoveryStore);
  const clock = dependencies.now ?? (() => new Date());

  async function handle(request: HttpRequest): Promise<HttpResponse> {
    try {
      return route(request);
    } catch (error) {
      return schemaErrorToResponse(error);
    }
  }

  function route(request: HttpRequest): HttpResponse {
    const segments = splitPath(request.path);
    const [head, second, third, fourth] = segments;

    /* ------------------------------------------------------------ */
    /* Public Resources Library (browse-only, no member identity)     */
    /* ------------------------------------------------------------ */
    if (head === "public-resources") {
      if (request.method !== "GET") return notFound("route_not_found");
      for (const key of Object.keys(request.query)) {
        if (!PUBLIC_RESOURCE_LIST_PARAMS.has(key)) {
          return badRequest("unsupported_query_parameter");
        }
      }
      const now = clock();
      if (second === undefined) {
        return handleListPublicResources(resources, {
          category: request.query["category"],
          jurisdiction: request.query["jurisdiction"],
          now
        });
      }
      return handleGetPublicResource(resources, second, now);
    }

    /* ------------------------------------------------------------ */
    /* Discovery controls (member-controlled, explicit memberRef)     */
    /* ------------------------------------------------------------ */
    if (head === "discovery" && second === "pause") {
      const now = clock();
      if (third === undefined && request.method === "GET") {
        for (const key of Object.keys(request.query)) {
          if (!PAUSE_GET_PARAMS.has(key)) {
            return badRequest("unsupported_query_parameter");
          }
        }
        const memberRef = request.query["memberRef"];
        if (typeof memberRef !== "string" || memberRef.trim() === "") {
          return badRequest("missing_field", "memberRef");
        }
        return discovery.getPauseState(memberRef);
      }
      if (request.method !== "POST") return notFound("route_not_found");
      const rejected = prohibitedFieldRejection(request.body);
      if (rejected) return rejected;
      const result = validateMemberActionBody(request.body);
      if (!result.valid) {
        throw new SchemaValidationError(result.errors[0] ?? "invalid_payload");
      }
      const memberRef = requireMemberRef(request.body);
      if (memberRef === undefined) {
        return badRequest("missing_field", "memberRef");
      }
      if (third === undefined) return discovery.pause(memberRef, now);
      if (third === "resume" && fourth === undefined) {
        return discovery.resume(memberRef, now);
      }
      return notFound("route_not_found");
    }

    if (head === "shared-activity-discovery") {
      if (request.method !== "POST") return notFound("route_not_found");
      const now = clock();
      const rejected = prohibitedFieldRejection(request.body);
      if (rejected) return rejected;
      const result = validateMemberActionBody(request.body);
      if (!result.valid) {
        throw new SchemaValidationError(result.errors[0] ?? "invalid_payload");
      }
      const memberRef = requireMemberRef(request.body);
      if (memberRef === undefined) {
        return badRequest("missing_field", "memberRef");
      }
      if (second === "options" && third !== undefined && fourth !== undefined) {
        if (fourth === "hide") {
          return discovery.hideOption(memberRef, third, now);
        }
        if (fourth === "express-interest") {
          return discovery.expressInterest(memberRef, third, now);
        }
      }
      if (
        second === "preferences" &&
        third === "show-fewer" &&
        fourth === undefined
      ) {
        return discovery.setShowFewer(memberRef, now);
      }
      return notFound("route_not_found");
    }

    /* ------------------------------------------------------------ */
    /* Encounter capsules (mutual acceptance + expiry enforced)       */
    /* ------------------------------------------------------------ */
    if (head === "encounter-capsules") {
      if (request.method !== "GET") return notFound("route_not_found");
      if (second === undefined) return notFound("route_not_found");
      const capsule = dependencies.capsuleStore.getActive(second, clock());
      if (!capsule) return notFound("capsule_not_available");
      return noStoreJson(200, capsuleMetadata(capsule));
    }

    /* ------------------------------------------------------------ */
    /* Community pulse (aggregate only)                               */
    /* ------------------------------------------------------------ */
    if (head === "community-pulse" && second === undefined) {
      if (request.method !== "GET") return notFound("route_not_found");
      const pulse = dependencies.pulseStore.getCurrent(clock());
      if (!pulse) return notFound("community_pulse_not_available");
      return noStoreJson(200, pulse);
    }

    /* ------------------------------------------------------------ */
    /* Intent recognition previews (confirmation-only, not executed)  */
    /* ------------------------------------------------------------ */
    if (
      head === "intent-recognition" &&
      second === "previews" &&
      third === undefined
    ) {
      if (request.method !== "POST") return notFound("route_not_found");
      const rejected = prohibitedFieldRejection(request.body);
      if (rejected) return rejected;
      // Full schema validation at the boundary (unknown fields rejected).
      const validation = dependencies.validator.validate(
        SCHEMAS.intentRecognitionPreview,
        request.body
      );
      if (!validation.valid) {
        throw new SchemaValidationError(
          validation.errors[0] ?? "invalid_payload"
        );
      }
      const preview = request.body as IntentRecognitionPreview;
      // Non-executable invariants, enforced independently of the schema.
      if (preview.queryExecutable !== false) {
        return badRequest("preview_not_confirm_only");
      }
      if (preview.nextAction !== "confirm_or_edit") {
        return badRequest("preview_not_confirm_only");
      }
      if (!isConfirmOnlyPreview(preview)) {
        return badRequest("preview_not_confirm_only");
      }
      // Echo unchanged. Nothing persisted, nothing executed, nothing sent.
      return noStoreJson(200, {
        acceptedForConfirmation: true,
        preview
      });
    }

    /* ------------------------------------------------------------ */
    /* Everything else                                                */
    /* ------------------------------------------------------------ */
    return notFound("route_not_found");
  }

  return handle;
}
