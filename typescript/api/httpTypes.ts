/**
 * Framework-neutral HTTP types for the Hearthline API layer.
 *
 * The router consumes `HttpRequest` and produces `HttpResponse` without any
 * dependency on a web framework, so the same handler can be mounted behind
 * any transport (Node http, serverless functions, tests).
 */

export interface HttpRequest {
  readonly method: "GET" | "POST" | "PATCH" | "DELETE";
  readonly path: string;
  readonly query: Readonly<Record<string, string | undefined>>;
  readonly body?: unknown;
}

export interface HttpResponse {
  readonly status: number;
  readonly headers?: Readonly<Record<string, string>>;
  readonly body: unknown;
}

const JSON_HEADERS = { "content-type": "application/json" } as const;
const NO_STORE_JSON_HEADERS = {
  "content-type": "application/json",
  "cache-control": "no-store"
} as const;

/** Plain, bounded error body. Never stack traces or internal objects. */
export interface ApiErrorBody {
  error: {
    code: string;
    detail?: string;
  };
}

export function json(status: number, body: unknown): HttpResponse {
  return { status, headers: JSON_HEADERS, body };
}

/**
 * For sensitive member state (pause state, discovery actions, capsules,
 * previews): always `Cache-Control: no-store`.
 */
export function noStoreJson(status: number, body: unknown): HttpResponse {
  return { status, headers: NO_STORE_JSON_HEADERS, body };
}

export function notFound(code: string): HttpResponse {
  const body: ApiErrorBody = { error: { code } };
  return noStoreJson(404, body);
}

export function badRequest(code: string, detail?: string): HttpResponse {
  const body: ApiErrorBody = { error: { code } };
  if (detail !== undefined) {
    // Bound detail length; never echo request values back at full size.
    body.error.detail = detail.slice(0, 200);
  }
  return noStoreJson(400, body);
}
