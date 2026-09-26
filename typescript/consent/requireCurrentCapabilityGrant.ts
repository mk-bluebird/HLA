export interface CurrentCapabilityGrant {
  readonly grantRef: string;
  readonly grantVersion: number;
  readonly capability: string;
  readonly purpose: string;
  readonly recipientRef: string;
  readonly expiresAt: string;
  readonly state: "granted" | "withdrawn" | "expired";
}

export interface CapabilityRequest {
  readonly grantRef: string;
  readonly expectedGrantVersion: number;
  readonly capability: string;
  readonly purpose: string;
  readonly recipientRef: string;
}

export interface GrantLookup {
  getCurrentGrant(
    grantRef: string
  ): Promise<CurrentCapabilityGrant | undefined>;
}

export async function requireCurrentCapabilityGrant(
  lookup: GrantLookup,
  request: CapabilityRequest,
  now: Date = new Date()
): Promise<CurrentCapabilityGrant> {
  const grant = await lookup.getCurrentGrant(request.grantRef);

  if (!grant) {
    throw new Error("capability_grant_not_found");
  }

  if (grant.state !== "granted") {
    throw new Error("capability_grant_not_active");
  }

  if (grant.grantVersion !== request.expectedGrantVersion) {
    throw new Error("capability_grant_version_stale");
  }

  if (Date.parse(grant.expiresAt) <= now.getTime()) {
    throw new Error("capability_grant_expired");
  }

  if (
    grant.capability !== request.capability ||
    grant.purpose !== request.purpose ||
    grant.recipientRef !== request.recipientRef
  ) {
    throw new Error("capability_grant_scope_mismatch");
  }

  return grant;
}
