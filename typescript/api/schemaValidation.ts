/**
 * JSON Schema validation adapter for the Hearthline API boundary.
 *
 * IMPORTANT — temporary boundary adapter:
 * No JavaScript JSON-Schema validator dependency is installed in this
 * repository, and this milestone forbids installing one. This module
 * therefore implements a narrowly scoped validator that understands exactly
 * the flat object shapes used by `schemas/*.schema.json` (string/boolean/enum/
 * const/pattern/date-time/min-max-length fields, string-array fields with
 * minItems/maxItems/uniqueItems, and top-level `additionalProperties:false`).
 * It is NOT a general JSON Schema implementation. When a real validator
 * dependency is adopted, only this file should change; the router, stores,
 * and handlers keep using the same `SchemaValidator` interface.
 *
 * Behavior contract:
 * - The adapter loads the actual schema file from disk at construction time
 *   and cross-checks its property list against the reviewed field shapes in
 *   `requestShapes.ts`. Any drift throws — it never silently accepts or
 *   rejects payloads.
 * - Unknown fields are rejected whenever the schema sets
 *   `additionalProperties: false`. Prohibited fields are never stripped.
 * - Public error codes are stable and non-sensitive. Detailed diagnostics are
 *   available only through `diagnostics` on the result (tests/dev logs).
 */

import { readFileSync } from "node:fs";
import { isPlainObject } from "../core/guards.ts";
import {
  MEMBER_ACTION_SHAPE,
  SCHEMA_SHAPES,
  type ObjectShape
} from "./requestShapes.ts";

export interface SchemaValidationResult {
  readonly valid: boolean;
  readonly errors: readonly string[];
  /** Development/test-only detail. Never serialize into an API response. */
  readonly diagnostics?: readonly string[];
}

export interface SchemaValidator {
  validate(schemaFile: string, value: unknown): SchemaValidationResult;
}

export const SCHEMAS = {
  mutualVisibilityContext: "schemas/mutual-visibility-context.schema.json",
  encounterCapsule: "schemas/encounter-capsule.schema.json",
  intentRecognitionPreview: "schemas/intent-recognition-preview.schema.json",
  discoverySurfaceAllocation:
    "schemas/discovery-surface-allocation.schema.json",
  communityPulse: "schemas/community-pulse.schema.json",
  publicResourceEntry: "schemas/public-resource-entry.schema.json"
} as const;

/** Stable, bounded public error code carried by SchemaValidationError. */
export type SchemaPublicErrorCode =
  | "invalid_payload"
  | "unknown_field"
  | "missing_field"
  | "schema_not_registered";

export class SchemaValidationError extends Error {
  readonly code: SchemaPublicErrorCode;
  /** Internal diagnostics; callers must not surface these to clients. */
  readonly diagnostics: readonly string[];

  constructor(
    code: SchemaPublicErrorCode,
    diagnostics: readonly string[] = []
  ) {
    super(code);
    this.name = "SchemaValidationError";
    this.code = code;
    this.diagnostics = diagnostics.slice(0, 10);
  }
}

interface LoadedSchema {
  readonly additionalProperties: boolean;
  readonly required: ReadonlySet<string>;
  readonly properties: ReadonlySet<string>;
}

function keyOf(schemaFile: string): string {
  return schemaFile.replace(/^schemas\//, "").replace(/\.schema\.json$/, "");
}

function loadSchema(rootDir: string, schemaFile: string): LoadedSchema {
  const raw = readFileSync(`${rootDir}/${schemaFile}`, "utf8");
  const doc = JSON.parse(raw) as Record<string, unknown>;
  const properties = isPlainObject(doc["properties"])
    ? new Set(Object.keys(doc["properties"]))
    : new Set<string>();
  const required = Array.isArray(doc["required"])
    ? new Set(doc["required"].filter((r): r is string => typeof r === "string"))
    : new Set<string>();
  return {
    additionalProperties: doc["additionalProperties"] !== true,
    required,
    properties
  };
}

/**
 * Creates a validator bound to a repository root directory so schema files
 * can be located relative to it (tests use the repo root; a deployed service
 * can pass its package root).
 */
export function createSchemaValidator(rootDir: string): SchemaValidator {
  const loaded = new Map<string, LoadedSchema>();

  function getLoaded(schemaFile: string): LoadedSchema {
    const cached = loaded.get(schemaFile);
    if (cached) return cached;
    const parsed = loadSchema(rootDir, schemaFile);
    loaded.set(schemaFile, parsed);
    return parsed;
  }

  function shapeFor(schemaFile: string): ObjectShape {
    const name = keyOf(schemaFile);
    const shape = SCHEMA_SHAPES[name];
    if (!shape) {
      throw new SchemaValidationError("schema_not_registered", [name]);
    }
    // Drift guard: the on-disk schema and the reviewed shape must describe
    // the same set of properties before either is trusted.
    const parsed = getLoaded(schemaFile);
    const shapeKeys = new Set(Object.keys(shape.fields));
    for (const k of parsed.properties) {
      if (!shapeKeys.has(k)) {
        throw new SchemaValidationError("schema_not_registered", [
          `${name}.${k}`
        ]);
      }
    }
    for (const k of shapeKeys) {
      if (!parsed.properties.has(k)) {
        throw new SchemaValidationError("schema_not_registered", [
          `${name}.${k}`
        ]);
      }
    }
    return shape;
  }

  function validateWithShape(
    shape: ObjectShape,
    rejectUnknown: boolean,
    value: unknown
  ): SchemaValidationResult {
    const errors: string[] = [];
    const diagnostics: string[] = [];

    if (!isPlainObject(value)) {
      return {
        valid: false,
        errors: ["invalid_payload"],
        diagnostics: ["expected a JSON object"]
      };
    }

    for (const [field, spec] of Object.entries(shape.fields)) {
      const present = Object.prototype.hasOwnProperty.call(value, field);
      if (!present) {
        if (spec.required) {
          errors.push("missing_field");
          diagnostics.push(`missing_field:${field}`);
        }
        continue;
      }
      let ok = false;
      try {
        ok = spec.validate(value[field]);
      } catch {
        ok = false;
      }
      if (!ok) {
        errors.push("invalid_payload");
        diagnostics.push(`invalid_payload:${field}`);
      }
    }

    for (const key of Object.keys(value)) {
      if (!Object.prototype.hasOwnProperty.call(shape.fields, key)) {
        if (rejectUnknown) {
          errors.push("unknown_field");
          diagnostics.push(`unknown_field:${key}`);
        }
      }
    }

    return {
      valid: errors.length === 0,
      errors: [...new Set(errors)],
      diagnostics
    };
  }

  return {
    validate(schemaFile: string, value: unknown): SchemaValidationResult {
      const shape = shapeFor(schemaFile);
      const parsed = getLoaded(schemaFile);
      return validateWithShape(shape, parsed.additionalProperties, value);
    }
  };
}

/**
 * Validates a member-controlled action body (`{ memberRef }` only). Uses the
 * same strict semantics: unknown fields are rejected, never stripped.
 */
export function validateMemberActionBody(
  value: unknown
): SchemaValidationResult {
  const shape = MEMBER_ACTION_SHAPE;
  const errors: string[] = [];
  const diagnostics: string[] = [];
  if (!isPlainObject(value)) {
    return {
      valid: false,
      errors: ["invalid_payload"],
      diagnostics: ["expected a JSON object"]
    };
  }
  for (const [field, spec] of Object.entries(shape.fields)) {
    const present = Object.prototype.hasOwnProperty.call(value, field);
    if (!present) {
      if (spec.required) {
        errors.push("missing_field");
        diagnostics.push(`missing_field:${field}`);
      }
      continue;
    }
    if (!spec.validate(value[field])) {
      errors.push("invalid_payload");
      diagnostics.push(`invalid_payload:${field}`);
    }
  }
  for (const key of Object.keys(value)) {
    if (!Object.prototype.hasOwnProperty.call(shape.fields, key)) {
      errors.push("unknown_field");
      diagnostics.push(`unknown_field:${key}`);
    }
  }
  return {
    valid: errors.length === 0,
    errors: [...new Set(errors)],
    diagnostics
  };
}

/**
 * Throws a typed `SchemaValidationError` with a bounded public code when the
 * payload fails validation. Callers map the code to an HTTP status; they must
 * never include `error.diagnostics` in a response body.
 */
export function requireValid(
  validator: SchemaValidator,
  schemaFile: string,
  value: unknown
): void {
  const result = validator.validate(schemaFile, value);
  if (result.valid) return;
  const first = result.errors[0] ?? "invalid_payload";
  throw new SchemaValidationError(
    first === "unknown_field"
      ? "unknown_field"
      : first === "missing_field"
        ? "missing_field"
        : "invalid_payload",
    result.diagnostics ?? []
  );
}
