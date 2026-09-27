type ConditionalChallengeResponse = {
  challengeId: string;
  challengeBase64Url: string;
  expiresAt: string;
};

type PasskeyAssertionPayload = {
  challengeId: string;
  credentialId: string;
  clientDataJSON: string;
  authenticatorData: string;
  signature: string;
  userHandle?: string;
};

const base64UrlToBytes = (value: string): Uint8Array => {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(
    normalized.length + ((4 - (normalized.length % 4)) % 4),
    "="
  );

  const binary = atob(padded);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
};

const bytesToBase64Url = (value: ArrayBuffer): string => {
  const bytes = new Uint8Array(value);
  let binary = "";

  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }

  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
};

export const startConditionalPasskeySignIn = async (): Promise<void> => {
  if (
    !window.PublicKeyCredential ||
    !PublicKeyCredential.isConditionalMediationAvailable
  ) {
    return;
  }

  const conditionalAvailable =
    await PublicKeyCredential.isConditionalMediationAvailable();

  if (!conditionalAvailable) {
    return;
  }

  const challengeResponse = await fetch(
    "/auth/conditional/challenge",
    {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json"
      },
      body: "{}"
    }
  );

  if (!challengeResponse.ok) {
    return;
  }

  const challenge: ConditionalChallengeResponse =
    await challengeResponse.json();

  const credential = await navigator.credentials.get({
    mediation: "conditional",
    publicKey: {
      challenge: base64UrlToBytes(challenge.challengeBase64Url),
      timeout: 300_000,
      userVerification: "required",
      rpId: window.location.hostname
    }
  });

  if (!credential || !(credential instanceof PublicKeyCredential)) {
    return;
  }

  const response = credential.response;

  if (!(response instanceof AuthenticatorAssertionResponse)) {
    return;
  }

  const payload: PasskeyAssertionPayload = {
    challengeId: challenge.challengeId,
    credentialId: bytesToBase64Url(credential.rawId),
    clientDataJSON: bytesToBase64Url(response.clientDataJSON),
    authenticatorData: bytesToBase64Url(response.authenticatorData),
    signature: bytesToBase64Url(response.signature),
    userHandle: response.userHandle
      ? bytesToBase64Url(response.userHandle)
      : undefined
  };

  await fetch("/auth/conditional/verify", {
    method: "POST",
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
};
