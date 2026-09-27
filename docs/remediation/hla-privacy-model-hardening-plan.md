# HLA Privacy Model Hardening Plan

**Version:** 0.1.0  
**Status:** Normative remediation and implementation plan  
**Scope:** HLA/Social-Decoder consent, privacy, accessibility, discovery,
moderation, eventing, governance, and advanced-cryptography deferral  
**Audience:** Engineering, privacy, accessibility, safety, policy, legal-review,
research, and release-governance teams

## 1. Objective

This plan turns Hearthline's boundary model into an enforceable system property.

The program prioritizes:

1. Elimination of model-breaking enforcement gaps.
2. Elimination of low-cost side channels that can combine into prohibited
   inference or scoring systems.
3. Accessibility parity as a mandatory release gate.
4. Explicit residual-risk acceptance only where a risk cannot reasonably be
   removed without destroying core access.
5. Deferral of advanced cryptography until ordinary consent, revocation,
   deletion, authorization, and audit pathways are demonstrably correct.

## 2. Severity rubric

### S1: model-breaking

An S1 exists when a prohibited mechanism can be directly reached or reconstructed
through a supported API, data flow, contract join, client bypass, background job,
analytics path, event stream, or foreseeable composition of deployed components.

Examples:

- A client-only consent checkpoint can be bypassed by a custom client.
- A sensitive capability is usable without a server-side current-grant check.
- An approved cross-contract join can reconstruct a member profile or prohibited
  sensitive attribute.
- A location, attendance, presence, safety, trust, risk, reputation, or
  compatibility signal can reach discovery, allocation, moderation, or a
  counterpart-facing response.
- An assistant draft can be sent without a member-operated sending boundary.
- A withdrawal leaves a valid cached token, signed link, queued job, or active
  session capable of continuing access.

### S2a: invariant-adjacent and preventable

An S2a exists when a low-cost omission, metadata channel, experiment assignment,
retention pattern, UI signal, or aggregation behavior can reasonably combine
with other permitted data to reconstruct a prohibited signal.

Examples:

- Hide/decline events aggregate into a reliability or negative-person score.
- Feature flag assignment becomes a behavioral or vulnerability proxy.
- Accessibility preferences become counterpart-visible by default.
- Topic opt-in history persists across contexts and becomes an identity proxy.
- Private writing is embedded or indexed.
- Consent history is reused to infer willingness, availability, or relationship
  pattern.
- Region updates reveal movement timing.
- Meeting-plan state becomes attendance or no-show tracking.
- Multi-capability consent is accepted in one request.
- Group threshold progress reveals who has not opted in.
- Report timing is visible to a counterpart or affects interaction state.
- A generic policy-authorized-service role accesses sensitive data without
  contract-specific authorization.

### S2b: documented residual risk

An S2b is a remaining risk that:

- Cannot be eliminated without disproportionate loss of core access;
- Has bounded impact and documented compensating controls;
- Has no known path to direct reconstruction of a prohibited state;
- Has a named accountable owner;
- Has a mandatory review date;
- Has a rollback/kill-switch plan where applicable.

### S3: defense in depth

An S3 is a non-blocking improvement that increases clarity, resilience, or
operational assurance without representing a currently reachable prohibited
state.

## 3. Release policy

- Any unresolved S1 blocks release.
- Any unresolved S2a blocks release of the affected surface.
- S2b may proceed only through explicit residual-risk acceptance.
- Accessibility parity is independently required for all release surfaces.
- A passing unit test does not override an unresolved normative conflict.
- Any disagreement among policy, schema, test, runtime behavior, deployment
  evidence, or member-facing wording is a release-blocking specification defect.

## 4. Dual-artifact governance model

Every invariant must have two coupled artifacts.

### 4.1 Normative specification

Path:

```text
docs/invariants/<invariant-name>.md
```

Required sections:

- Invariant statement.
- Scope.
- Rationale and threat model.
- Prohibited inputs/states/outcomes.
- Required architectural enforcement.
- Required member-facing behavior.
- Required retention/deletion behavior.
- Required accessibility behavior.
- Required audit evidence.
- Related schemas/contracts.
- Release-gate test references.
- Policy version.
- Owner.
- Review date.
- Change history.

### 4.2 Executable assurance

Path:

```text
tests/adversarial/<invariant-name>.test.ts
```

Required test classes:

- Positive test: permitted behavior remains available.
- Negative test: direct violation is rejected.
- Composition test: adjacent permitted components cannot reconstruct a prohibited
  outcome.
- Revocation test: withdrawal/expiry removes access.
- Metadata test: event shape and retention cannot leak prohibited state.
- Accessibility-path test: enforcement holds through keyboard, screen-reader,
  text-first, reduced-motion, low-bandwidth, no-camera, no-microphone, no-voice,
  and asynchronous paths.
- Deployment test: release configuration cannot disable the invariant.

### 4.3 Conflict rule

```text
Approved normative specification
  + executable test
  + runtime authorization behavior
  + deployment evidence
must agree.

Any disagreement blocks release until a versioned review resolves the discrepancy.
```

No test may silently redefine policy by narrowing its assertions.

## 5. Phase 0: freeze and inventory

### Goals

- Freeze creation of new sensitive discovery, location, matching, moderation,
  media, meeting, and agent features.
- Inventory every API, schema, event, queue, cache, feature flag, analytics
  sink, model input, export path, and third-party integration.
- Identify all sensitive-field joins and implicit metadata channels.
- Establish a release gate framework before remediation begins.

### Required outputs

- Complete allowed-field manifest.
- Complete forbidden-field manifest.
- Event catalog with producer, consumer, purpose, schema, retention, and
  authorization.
- Component-contract inventory.
- Feature-flag inventory with sensitive-surface classification.
- Cache/token/signed-link inventory.
- Accessibility surface inventory.
- Data-retention inventory.
- S1/S2a/S2b/S3 issue register.
- Temporary deployment freeze for new sensitive data collection.

### Exit criteria

- No undocumented production consumer of sensitive data.
- Every event has an owner and a retention class.
- Every feature flag is classified as ordinary or sensitive.
- Every release surface has a named privacy, accessibility, safety, and policy
  owner.

## 6. Phase 1: S1 and S2a enforcement remediation

### 6.1 Server-enforced consent

Implement:

- Server-side current-grant validation for every sensitive capability.
- Recipient, purpose, capability, expiry, and version matching.
- Deny-by-default behavior.
- Single-capability consent requests only.
- Block/restriction override.
- Immediate revocation epoch/version invalidation.
- No capability activation based solely on client state.

Release gates:

- Client request without grant is denied.
- Stale grant version is denied.
- Expired grant is denied.
- Recipient mismatch is denied.
- Multi-capability request is rejected.
- Block overrides existing grant.
- Revocation invalidates active/cached pathways.

### 6.2 Contract isolation and join prevention

Implement:

- Contract-specific field allowlists.
- Contract-specific forbidden-field lists.
- Field-level purpose binding.
- Consumer identity and authorization binding.
- Join review for every new contract pair.
- Static checks preventing a consumer from obtaining fields outside its approved
  contract.
- Runtime checks rejecting unknown fields and unapproved consumers.

Release gates:

- No discovery service receives raw message, media, location, accessibility,
  identity, health, finance, housing, employment, report, block, or consent
  history data.
- No analytics consumer can join member identifiers across sensitive zones.
- No contract permits universal-profile reconstruction.
- No policy-authorized service can read sensitive data without contract-specific
  authorization.

### 6.3 Metadata and behavioral side-channel removal

Implement:

- No user-level hide/decline aggregation.
- No response-time, typing, edit-count, view-count, online/presence, or account
  tenure inputs outside their narrow operational purpose.
- No persistent topic opt-in history beyond defined context lifetime.
- No topic threshold progress display.
- No counterpart-visible report timing or withdrawal timing.
- No region-change event exposed to unrelated services.
- No meeting-plan outcome/no-show/attendance state.
- No feature experiment assignment on sensitive surfaces without governance
  approval and privacy review.
- No private-message embeddings, writing-style embeddings, or vector indexing.

Release gates:

- Attempted score aggregation is rejected.
- Sensitive feature flags cannot be assigned for behavioral experimentation.
- Topic/consent/region/plan metadata cannot reach discovery or analytics
  profiling.
- Vector ingestion of private text is rejected.
- Meeting-plan expiry deletes outcome-like state.

### 6.4 Accessibility parity as a mandatory gate

Implement a testable matrix for every member-facing sensitive flow:

- Keyboard-only.
- Screen-reader.
- Text-first.
- Reduced motion.
- Low bandwidth.
- No camera.
- No microphone.
- No voice.
- Asynchronous path.
- Plain-language path.

Release gates:

- Every consent request, withdrawal, block, report, appeal, pause, deletion,
  topic control, media control, and discovery control is operable through each
  applicable path.
- Privacy controls provide equivalent outcome, not merely an alternate label.
- No access preference becomes counterpart-visible by default.
- No path leaks extra metadata.

## 7. Consent history and accountability design

### 7.1 Operational projection

Store only the current authorization state needed to authorize a capability:

```text
grantRef
grantVersion
state
capability
purpose
recipientScope
expiresAt
policyVersion
```

### 7.2 Content-minimized accountability receipt

Where accountability is needed, retain a restricted, short-lived receipt:

```text
opaque event ID
opaque grant reference
version
capability
purpose
recipient scope reference
revocation/expiry state
timestamp
policy version
retention class
```

Do not retain:

- Message content.
- Media.
- Identity claims.
- Location.
- Venue history.
- Relationship narratives.
- A sequence of consent decisions usable as a behavioral profile.

### 7.3 Prohibited reuse

Consent history must not be used for:

- Discovery.
- Matching.
- Allocation.
- Ranking.
- Reputation.
- Trust/safety/risk scoring.
- Advertising.
- Model training.
- Behavioral analytics.
- Future willingness inference.
- Availability inference.
- Relationship-history inference.

## 8. Phase 2: S2b residual-risk governance

An S2b cannot be silently deferred.

Each accepted residual risk requires:

```text
risk ID
description
affected surface
why removal is disproportionate
threat model
compensating controls
known limitations
named accountable owner
privacy owner
accessibility owner
safety owner
review date
rollback/kill-switch plan
member-facing disclosure requirement, if applicable
approval record
```

### Candidate S2b examples

- Presentation rotation as a limited ordering mechanism.
- Group social pressure not fully eliminable in bounded community discussion.
- Network timing side channels requiring more extensive transport-level work.
- Some client-rendering timing variance.

No candidate is automatically S2b. It must satisfy the residual-risk criteria and
be reviewed individually.

## 9. Phase 3: controlled feature resumption

No feature may resume or launch until:

- Phase 1 release gates pass.
- Required S2b acceptance records are current.
- Applicable feature rights/access review passes.
- Applicable accessibility matrix passes.
- Applicable adversarial tests pass.
- Contract and event inventory entries are approved.
- Retention/deletion and revocation behavior are tested.
- Member-facing explanation and exit paths are reviewed.

Initial feature sequence:

1. Consent Core and revocation propagation.
2. Block, mute, restrict, quiet exit, report, correction, and appeal.
3. Local-first drafts and member-authored controls.
4. Access and comfort controls.
5. Intent, boundary, and pacing objects.
6. Broad future activity windows and broad-region/manual remote context.
7. Constraint-based discovery without ranking.
8. Topic Boards with visibility/activation separation.
9. Public-first planning and source-labeled public information.
10. Capability-scoped media sharing.
11. Member-invoked AI drafting and selected-text assistance.
12. Governance, research, and narrowly scoped interoperability.

## 10. Deferred cryptographic work

ZKP, DID, verifiable credentials, and selective-disclosure mechanisms remain
deferred until the ordinary non-cryptographic system meets all Phase 1 gates.

### Preconditions for any cryptographic proposal

- A documented use case cannot be met by a simpler audited mechanism.
- The predicate is narrow and purpose-specific.
- A complete non-cryptographic equivalent remains available.
- No public DID, wallet, blockchain, or permanent identifier is required.
- Correlation and replay threats are documented.
- Revocation behavior is defined and tested.
- Accessibility and low-bandwidth paths exist.
- Privacy review, security review, and independent cryptographic review approve
  the design.
- The credential cannot become reputation, safety, trust, risk, desirability,
  report-history, or social-standing infrastructure.

### Permitted candidate predicate

```text
This account currently satisfies Hearthline's 30+ membership policy.
```

### Prohibited predicates

```text
This person is safe.
This person is trusted.
This person has no reports.
This person has a good reputation.
This person has successful encounters.
This person is low risk.
This person is desirable.
This person is likely to consent.
```

## 11. Continuous assurance

Each release must run:

- Schema validation.
- Contract validation.
- Forbidden-field tests.
- Consent/revocation tests.
- Cache/token/queue/session invalidation tests.
- Metadata side-channel tests.
- Accessibility parity tests.
- No-autonomous-action tests.
- Non-commercial-integrity tests.
- Retention/deletion tests.
- Audit/appeal tests.
- Feature rights/access review checks.
- Deployment-configuration checks.

## 12. Success criteria

The program succeeds when:

- A prohibited mechanism cannot be reconstructed through supported components.
- A missing field remains missing rather than becoming an inference.
- A revoked capability cannot be used through client, cache, token, queue,
  session, link, or downstream consumer.
- A sensitive feature provides equivalent accessible paths.
- A lower-level component cannot override the rights floor.
- A person cannot be transformed into a rank, score, behavioral profile, or
  location/attendance signal.
- A policy change cannot ship without corresponding test, contract, review, and
  release evidence.
