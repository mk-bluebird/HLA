# Invariant: <name>

**Invariant ID:** `INV-<identifier>`  
**Policy version:** `<version>`  
**Status:** draft | approved | deprecated  
**Owner:** `<role or team>`  
**Privacy reviewer:** `<role or team>`  
**Accessibility reviewer:** `<role or team>`  
**Safety reviewer:** `<role or team>`  
**Next review date:** `<YYYY-MM-DD>`  
**Executable tests:** `tests/adversarial/<name>.test.ts`

## Statement

<State the invariant in one falsifiable sentence.>

## Scope

<List the services, data classes, UI flows, and deployment paths to which it applies.>

## Rationale

<Explain the member-rights, privacy, accessibility, safety, or non-commercial
harm prevented by this invariant.>

## Prohibited states and inputs

- <Prohibited state/input 1>
- <Prohibited state/input 2>
- <Prohibited reconstructed outcome>

## Required enforcement

- <Schema/API enforcement>
- <Server-side authorization enforcement>
- <Event/cache/token/queue/session enforcement>
- <Retention/deletion enforcement>
- <Accessibility enforcement>
- <Deployment/configuration enforcement>

## Required member-facing behavior

<Describe clear member-visible effects, including pause, withdrawal, block, error,
explanation, correction, and appeal behavior.>

## Required audit evidence

- <Contract/version evidence>
- <Release-gate result>
- <Retention/deletion evidence>
- <Review record>

## Tests

| Test class | Required assertion |
|---|---|
| Positive | <Permitted behavior works> |
| Negative | <Direct violation is rejected> |
| Composition | <Adjacent contracts cannot reconstruct prohibited outcome> |
| Revocation | <Withdrawal/expiry blocks access everywhere> |
| Metadata | <Side-channel fields are absent/minimized> |
| Accessibility | <Invariant holds across access paths> |
| Deployment | <Flag/config cannot disable invariant> |

## Residual risk

<State `none` or link to a formal residual-risk record.>

## Change control

<Define required approvals for weakening, replacing, or deprecating the invariant.>
