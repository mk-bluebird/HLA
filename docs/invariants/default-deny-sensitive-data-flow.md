# Invariant: Default-Deny Sensitive Data Flow

**Invariant ID:** `INV-DEFAULT-DENY-SENSITIVE-DATA-FLOW`  
**Policy version:** `default-deny-data-flow-v1`  
**Status:** approved  
**Owner:** Privacy Boundary Engineering  
**Privacy reviewer:** Privacy Review  
**Accessibility reviewer:** Accessibility Review  
**Safety reviewer:** Safety Review  
**Next review date:** 2027-03-26  
**Executable tests:** `tests/adversarial/default-deny-sensitive-data-flow.test.ts`

## Statement

No HLA component may collect, receive, derive, retain, join, expose, or use a
member-sensitive field unless a current, purpose-limited, contract-specific,
least-privilege authorization explicitly permits that exact field for that exact
consumer and purpose.

## Rationale

Hearthline's privacy, consent, accessibility, anti-discrimination, and
non-commercial boundaries fail if separately permissible fields can be joined
into a behavioral profile, identity proxy, location history, attendance record,
reputation signal, safety score, risk score, compatibility score, or inference
about a member.

Default denial prevents accidental expansion, administrative overreach,
cross-feature surveillance, feature-flag leakage, analytics profiling, and
model-training reuse.

## Sensitive fields

Sensitive fields include, but are not limited to:

- Identity claims and identity presentation.
- Access, comfort, and communication controls.
- Consent grants, withdrawals, expiry, and history.
- Boundaries and pace settings.
- Private messages, media, calls, and recordings.
- Broad-region selections, venue details, meeting plans, trusted-contact plans.
- Blocks, restrictions, reports, appeals, and moderation material.
- Device, behavioral, timing, read, view, presence, typing, and interaction data.
- Health, recovery, disability, finances, housing, employment, relationship,
  sexuality, legal, vulnerability, or protected-identity context.
- Any score, inference, prediction, embedding, vector, proxy, or derived signal.

## Required authorization tuple

Every permitted sensitive-data access must bind:

```text
contractId
producer
consumer
purpose
allowedField
authorizationRequirement
consentRequirement
policyVersion
retentionClass
revocationBehavior
failureMode
requestTimestamp
```

## Deny conditions

Deny access if any of the following applies:

- Consumer is not explicitly named in an approved contract.
- Field is not in the contract's allowlist.
- Field is in a forbidden-field list.
- Purpose is absent, stale, ambiguous, or mismatched.
- Policy version is absent or retired.
- Authorization is absent, expired, revoked, or mismatched.
- Consent is required but absent, expired, withdrawn, bundled, or mismatched.
- Retention class is absent.
- Revocation behavior is absent.
- Requested use could create a person-level score or prohibited inference.
- Requested use could join data across trust zones into a profile.
- Requested use sends data to discovery, ranking, advertising, profiling,
  default model training, or unrelated analytics.
- Requested experiment touches a sensitive surface without explicit governance
  approval.

## Explicit default denials

The following paths are always denied:

```text
access_controls → discovery
access_controls → ranking
access_controls → advertising
access_controls → profiling

private_messages → matching
private_messages → discovery
private_messages → embeddings
private_messages → vector_store
private_messages → default_model_training

consent_history → matching
consent_history → discovery
consent_history → reputation
consent_history → willingness_inference

block_report_appeal_data → discovery
block_report_appeal_data → ranking
block_report_appeal_data → person_score

meeting_plan_data → attendance
meeting_plan_data → no_show
meeting_plan_data → reliability_score
meeting_plan_data → discovery_after_expiry

trusted_contact_data → counterpart_profile_transfer
trusted_contact_data → discovery
trusted_contact_data → location_history

topic_opt_in_history → cross_context_profile
topic_opt_in_history → identity_inference
topic_opt_in_history → ranking

feature_assignment → counterpart_payload
feature_assignment → matching
feature_assignment → behavior_profile
```

## Required enforcement

- JSON Schema rejects forbidden fields at every service boundary.
- Contract validation rejects unknown consumers and unknown fields.
- Server-side authorization checks the full authorization tuple.
- Sensitive actions fail closed on authorization or revocation uncertainty.
- Event schemas are purpose-limited and content-minimized.
- Event consumers must be named and approved in a contract.
- Analytics ingestion rejects sensitive fields and sensitive event joins.
- Experiment configuration rejects sensitive-surface behavioral experiments
  unless a separately approved governance contract exists.
- Review workspaces are case-, role-, purpose-, and time-bounded.
- Deletion and revocation invalidate downstream projections, caches, queues,
  links, and active sessions as applicable.

## Required member-facing behavior

Members must be able to:

- See what they currently chose to share.
- Pause, withdraw, reduce, or delete a member-owned object.
- Use block, report, correction, appeal, and quiet-exit controls.
- Use core access paths without disclosing access preferences or sensitive
  identity context.
- Receive clear confirmation of their own completed control action.

Members must not be required to understand internal contracts, flags, joins, or
event systems in order to receive these protections.

## Release-gate evidence

- Contract allowlist/denylist test results.
- Forbidden-field rejection tests.
- Cross-contract reconstruction tests.
- Revocation propagation tests.
- Sensitive analytics-ingestion rejection tests.
- Feature-flag exposure tests.
- Topic/meeting/trusted-contact metadata tests.
- Accessibility parity test results.
- Policy version and contract approval records.
