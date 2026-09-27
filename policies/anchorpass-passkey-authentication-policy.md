# AnchorPass Passkey Authentication Policy

## Purpose

AnchorPass provides passkey-first authentication for Hearthline using WebAuthn
and FIDO2-compatible platform authenticators or roaming security keys.

It supports passwordless, phishing-resistant authentication while keeping
biometric verification, device PINs, patterns, and private keys outside
Hearthline's systems.

## Primary authentication method

The primary sign-in method is a WebAuthn passkey with:

- User verification required.
- A current server-generated challenge.
- Relying-party ID verification.
- Origin verification.
- Signature verification using the stored public credential key.
- Credential status check.
- Challenge expiry.
- Session rotation after successful sign-in.

## Supported device experiences

### Apple platforms

A member may use a passkey activated through Face ID, Touch ID, or device
passcode. Hearthline receives only a WebAuthn assertion and must not receive
biometric templates, biometric status, or device-unlock details.

### Android platforms

A member may use Android Credential Manager with a passkey provider. User
verification may use available device-local biometrics, PIN, or pattern.
Hearthline receives only the public-key credential assertion.

### Microsoft/Windows platforms

A member may use Windows Hello or a compatible WebAuthn authenticator. Hearthline
receives only the signed assertion and the credential public key material needed
for verification.

### Roaming authenticators

A member may use a FIDO2-compatible security key or cross-device authentication
flow where supported by platform/WebAuthn standards.

## Prohibited collection

AnchorPass must not collect, retain, or request:

- Passwords as the primary authentication secret.
- Biometric templates.
- Face ID data.
- Touch ID data.
- Fingerprint images.
- Device PIN.
- Device unlock pattern.
- Private passkey material.
- Security-key secret material.
- Government identity document for ordinary sign-in.
- Exact location.
- Contact graph.
- Profile, message, media, discovery, or relationship data for authentication.
- Device identity or authentication history for social ranking, matching, safety,
  trust, risk, desirability, or reputation.

## Lost-device and recovery principles

- A member may register multiple passkeys.
- A member may register a roaming security key.
- A member may revoke an individual passkey/device credential.
- Recovery must not silently weaken the account into password-only access.
- Recovery must not expose private account data before reauthentication.
- Recovery decisions require accessible human support or a documented
  privacy-minimized recovery mechanism.
- Recovery state must not affect discovery, matching, visibility, safety, or
  counterpart-facing information.

## Session policy

- Access sessions are short-lived.
- Refresh/session rotation occurs through server-controlled policy.
- Sensitive actions require current session and current capability consent where
  applicable.
- Session invalidation must propagate on credential revocation, account closure,
  block of a compromised session where applicable, or security-policy action.
- The platform must provide a member-readable session/device management screen.

## Accessibility requirements

A member must have access to at least one sign-in/recovery route that does not
require:

- Camera.
- Face scan.
- Voice.
- Gesture.
- Location.
- Travel.
- A particular mobile operating system.
- A public social account.
- A government identity document for ordinary sign-in.

## Member-facing language

> Hearthline uses passkeys to help protect your account. Your device may ask you
> to use Face ID, Touch ID, a fingerprint, Windows Hello, a PIN, a pattern, or a
> security key. Those checks happen on your device. Hearthline does not receive
> your biometric data, device PIN, or private passkey.
