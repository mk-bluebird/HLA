/**
 * Public Resources Library handler (framework-neutral).
 *
 * Browse-only, source-labeled access to public information entries. This
 * module intentionally contains no member identity parameter anywhere: the
 * library cannot record who looked at what, cannot infer need, and cannot
 * rank or recommend. The router in `typescript/api/hearthlineApi.ts` maps
 * these results onto HTTP responses.
 */

import type {
  PublicResourceCategory,
  PublicResourceEntry
} from "../core/types.ts";
import { isPublicResourceCategory } from "../core/guards.ts";
import type { PublicResourceStore } from "../api/inMemoryStores.ts";
import type { HttpResponse } from "../api/httpTypes.ts";
import { badRequest, json, notFound } from "../api/httpTypes.ts";

export interface PublicResourcesHandler {
  listResources(input: {
    readonly category?: PublicResourceCategory;
    readonly jurisdiction?: string;
    readonly now: Date;
  }): readonly PublicResourceEntry[];
  getResource(input: {
    readonly resourceId: string;
    readonly now: Date;
  }): PublicResourceEntry | undefined;
}

export function createPublicResourcesHandler(
  store: PublicResourceStore
): PublicResourcesHandler {
  return {
    listResources({ category, jurisdiction, now }) {
      // A bad category can never match an entry, but we reject it explicitly
      // so callers get a clear error instead of an empty list that looks
      // like "no resources exist".
      if (category !== undefined && !isPublicResourceCategory(category)) {
        throw new Error("bad_request");
      }
      return store.list({ category, jurisdiction, now });
    },
    getResource({ resourceId, now }) {
      return store.get(resourceId, now);
    }
  };
}

/**
 * GET /public-resources — list browse-only catalog entries.
 * No member identifier is accepted or required.
 */
export function handleListPublicResources(
  handler: PublicResourcesHandler,
  query: {
    readonly category?: unknown;
    readonly jurisdiction?: unknown;
    readonly now: Date;
  }
): HttpResponse {
  const { now, ...filters } = query;
  try {
    const entries = handler.listResources({ now, ...normalizeFilters(filters) });
    return json(200, { entries });
  } catch {
    return badRequestCategory();
  }
}

/**
 * GET /public-resources/{resourceId} — single entry, only when currently
 * listable. Absent, removed, outdated, or expired-review entries all answer
 * 404 resource_not_found without distinguishing which case applied.
 */
export function handleGetPublicResource(
  handler: PublicResourcesHandler,
  resourceId: string,
  now: Date
): HttpResponse {
  const entry = handler.getResource({ resourceId, now });
  if (!entry) return notFound("resource_not_found");
  return json(200, entry);
}

function normalizeFilters(filters: {
  readonly category?: unknown;
  readonly jurisdiction?: unknown;
}): {
  category?: PublicResourceCategory;
  jurisdiction?: string;
} {
  const out: {
    category?: PublicResourceCategory;
    jurisdiction?: string;
  } = {};
  if (filters.category !== undefined) {
    if (typeof filters.category !== "string") throw new Error("bad_request");
    out.category = filters.category as PublicResourceCategory;
  }
  if (filters.jurisdiction !== undefined) {
    // Broad, manually chosen jurisdiction label only. Never stored on any
    // member; there is no member state to store it in here.
    if (typeof filters.jurisdiction !== "string") {
      throw new Error("bad_request");
    }
    out.jurisdiction = filters.jurisdiction;
  }
  return out;
}

function badRequestCategory(): HttpResponse {
  return badRequest(
    "bad_request",
    "category must be one of the values defined by schemas/public-resource-entry.schema.json"
  );
}
