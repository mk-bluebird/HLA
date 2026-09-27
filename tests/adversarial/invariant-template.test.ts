import { describe, expect, it } from "vitest";

describe("INV-EXAMPLE: <invariant name>", () => {
  it("permits the narrowly allowed path", async () => {
    const result = await permittedOperation();

    expect(result.allowed).toBe(true);
  });

  it("rejects a direct prohibited input or state", async () => {
    await expect(
      prohibitedOperation({
        forbiddenField: "forbidden_value"
      })
    ).rejects.toThrow("forbidden_field");
  });

  it("rejects reconstruction through composition of adjacent components", async () => {
    await expect(
      composePermittedLookingInputsIntoProhibitedOutcome()
    ).rejects.toThrow("prohibited_reconstruction");
  });

  it("fails closed after revocation or expiry", async () => {
    const grant = await createScopedGrant();
    await withdrawGrant(grant.grantId);

    await expect(
      useSensitiveCapability(grant)
    ).rejects.toThrow("capability_grant_not_active");
  });

  it("does not expose metadata side-channel fields", async () => {
    const response = await memberVisibleResponse();

    expect(response).not.toHaveProperty("typingDuration");
    expect(response).not.toHaveProperty("editCount");
    expect(response).not.toHaveProperty("presence");
    expect(response).not.toHaveProperty("location");
    expect(response).not.toHaveProperty("score");
  });

  it("preserves the invariant through accessible interaction paths", async () => {
    const keyboardResult = await keyboardOnlyPath();
    const screenReaderResult = await screenReaderPath();
    const textFirstResult = await textFirstPath();
    const lowBandwidthResult = await lowBandwidthPath();

    expect(keyboardResult.protected).toBe(true);
    expect(screenReaderResult.protected).toBe(true);
    expect(textFirstResult.protected).toBe(true);
    expect(lowBandwidthResult.protected).toBe(true);
  });

  it("cannot be disabled through deployment configuration", async () => {
    const configuration = await productionConfiguration();

    expect(configuration.invariantEnabled).toBe(true);
    expect(configuration.userOverrideAllowed).toBe(false);
  });
});

async function permittedOperation() {
  return { allowed: true };
}

async function prohibitedOperation(
  _input: Readonly<Record<string, unknown>>
): Promise<never> {
  throw new Error("forbidden_field");
}

async function composePermittedLookingInputsIntoProhibitedOutcome(): Promise<never> {
  throw new Error("prohibited_reconstruction");
}

async function createScopedGrant() {
  return { grantId: "consent_example" };
}

async function withdrawGrant(_grantId: string): Promise<void> {}

async function useSensitiveCapability(
  _grant: Readonly<{ grantId: string }>
): Promise<never> {
  throw new Error("capability_grant_not_active");
}

async function memberVisibleResponse() {
  return { protected: true };
}

async function keyboardOnlyPath() {
  return { protected: true };
}

async function screenReaderPath() {
  return { protected: true };
}

async function textFirstPath() {
  return { protected: true };
}

async function lowBandwidthPath() {
  return { protected: true };
}

async function productionConfiguration() {
  return {
    invariantEnabled: true,
    userOverrideAllowed: false
  };
}
