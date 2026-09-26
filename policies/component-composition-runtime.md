# Component Composition Runtime Policy

## Purpose

The Component Composition Runtime integrates Hearthline components through
explicit, versioned, purpose-limited service contracts.

It prevents a monolithic profile, universal data bus, sensitive-data lake, or
cross-feature surveillance path.

## Required integration rules

Every component integration must define:

- Producer.
- Consumer.
- Purpose.
- Input schema.
- Output schema.
- Allowed fields.
- Forbidden fields.
- Authorization requirement.
- Consent requirement.
- Retention class.
- Deletion behavior.
- Revocation behavior.
- Audit event format.
- Failure mode.
- Policy version.
- Accessibility implications.
- Appeal/correction implications where material decisions are affected.

## Default deny

A component may not access another component's data unless a documented contract
explicitly allows the exact field for the exact purpose.

Unknown fields, undocumented consumers, invalid policy versions, expired grants,
withdrawn grants, and failed authorization checks must fail closed.

## Event boundaries

Events must not include:

- Raw private messages.
- Intimate media.
- Calls or recordings.
- Exact location, route, attendance, or presence.
- Contact graph or profile views.
- Identity, health, recovery, disability, finances, housing, employment, or
  vulnerability.
- Person-level scores.
- Broad profile snapshots.
- Unbounded free-text narratives.
- Advertising identifiers.

## Runtime invariants

- `externalActionAuthorized: false` for all assistant-generated objects.
- No person-level score, rank, reputation, trust, safety, risk, compatibility,
  desirability, or response prediction.
- No identity, health, recovery, sexuality, finance, or vulnerability inference.
- No value-for-intimacy or dependency brokerage.
- Every object is member-owned, revocable, and retention-classed.
- Accessibility parity is a release gate.
- Consent revocation propagates before sensitive action.
- Research data remains operationally isolated.
