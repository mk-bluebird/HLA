# PQC WebAuthn Migration Test Matrix

## Credential compatibility

| Scenario | Expected result |
|---|---|
| Existing ES256 credential signs in | Accepted while policy remains active |
| Existing EdDSA credential signs in | Accepted while policy remains active |
| Future ML-DSA credential signs in | Verified only when final interoperable support is enabled |
| Future SLH-DSA credential signs in | Verified only when final interoperable support is enabled |
| Hybrid credential signs in | Both required algorithm components validate |
| Unsupported algorithm arrives | Generic sign-in failure; no counterpart or profile effect |

## Registration and migration

| Scenario | Expected result |
|---|---|
| Member adds PQ/hybrid credential using active passkey | New credential added; existing credentials unchanged |
| Member declines PQ/hybrid credential | Existing access unchanged |
| Older client cannot create PQ credential | Existing accessible sign-in and recovery remain available |
| Credential manager does not support PQ transfer | No account penalty or visibility change |
| New algorithm registration fails | Accessible alternative route available |
| Credential is removed during migration | Server-side revocation propagates to sessions and grants |

## Privacy boundaries

| Test | Expected result |
|---|---|
| Credential algorithm reaches profile service | Rejected by egress policy |
| Credential algorithm reaches discovery/ranking/matching | Rejected by egress policy |
| Credential algorithm appears in counterpart API | Rejected by contract test |
| Algorithm is used for member trust/reputation | Rejected by policy test |
| Algorithm appears in analytics member dimension | Rejected by schema test |

## Accessibility parity

| Path | Required support |
|---|---|
| Keyboard-only | Full registration, sign-in, migration, recovery |
| Screen reader | Equivalent labels, errors, confirmation, and exit |
| Text-first / low bandwidth | No hardware/PQC-specific gating |
| No camera / no biometric | Complete alternative path |
| Older supported client | Existing classical credential remains usable through transition |
| External security key | Standards-compliant support when available |
