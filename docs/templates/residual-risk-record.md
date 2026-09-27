# Residual Risk Record

**Risk ID:** `RR-<identifier>`  
**Status:** proposed | accepted | mitigated | retired  
**Severity:** S2b  
**Affected surface:** `<feature, service, UI, protocol, or deployment surface>`  
**Created:** `<YYYY-MM-DD>`  
**Review by:** `<YYYY-MM-DD>`  
**Named accountable owner:** `<role/team>`  
**Privacy reviewer:** `<role/team>`  
**Accessibility reviewer:** `<role/team>`  
**Safety reviewer:** `<role/team>`  
**Approval authority:** `<governance body>`  
**Related invariant(s):** `<INV identifiers>`  
**Related red-team finding(s):** `<finding identifiers>`

## Risk statement

<Describe the residual risk in falsifiable terms.>

## Why this remains residual

<Explain why elimination is not presently feasible without disproportionate
impact on core access, accessibility, autonomy, or product viability.>

## Threat model

- Adversary:
- Required attacker capability:
- Affected members:
- Preconditions:
- Potential harm:
- Worst plausible outcome:
- Why the risk does not presently qualify as S1 or S2a:

## Compensating controls

- <Control 1>
- <Control 2>
- <Control 3>

## Explicit limitations

<State what the system cannot guarantee.>

## Re-evaluation triggers

- New feature touching:
- New data source:
- New contract join:
- New experiment:
- New red-team finding:
- New accessibility-path behavior:
- New incident pattern:
- Policy or legal change:

## Monitoring and evidence

<List only aggregate, privacy-preserving operational evidence. Do not create a
member-level telemetry profile.>

## Rollback / kill-switch plan

<Describe how the affected surface can be disabled or narrowed.>

## Member-facing disclosure

<State whether a plain-language limitation must be presented to members.>

## Approval record

| Role | Name/reference | Decision | Date |
|---|---|---|---|
| Accountable owner |  |  |  |
| Privacy reviewer |  |  |  |
| Accessibility reviewer |  |  |  |
| Safety reviewer |  |  |  |
| Governance authority |  |  |  |
