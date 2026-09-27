# Invariant: No Cross-Contract Profile Reconstruction

**Invariant ID:** `INV-NO-CROSS-CONTRACT-PROFILE-RECONSTRUCTION`  
**Policy version:** `contract-isolation-v1`  
**Status:** approved  
**Owner:** Privacy Boundary Engineering  
**Privacy reviewer:** Privacy Review  
**Accessibility reviewer:** Accessibility Review  
**Safety reviewer:** Safety Review  
**Next review date:** 2027-03-26  
**Executable tests:** `tests/adversarial/no-cross-contract-profile-reconstruction.test.ts`

## Statement

No approved set of Hearthline component contracts may be joined, correlated, or
sequenced to reconstruct a member profile, identity proxy, behavioral profile,
location/routine history, attendance record, relationship history, risk signal,
trust signal, safety signal, or person-level score.

## Rationale

Field-level allowlists are insufficient if otherwise allowed records can be
joined through a stable `member_ref`, device reference, session reference,
timestamp sequence, broad-region history, or shared secondary identifier.

Privacy boundaries must apply to compositions of contracts, not only to each
contract in isolation.

## Required architecture

### Contract-scoped subject references

Contracts must not use a globally reusable member reference unless the specific
purpose requires it and cross-zone correlation has been approved.

Use one or more of:

- Purpose-scoped opaque subject references.
- Audience-scoped pseudonymous identifiers.
- Short-lived session references.
- One-time capability references.
- Aggregated/non-member-level records.
- Cryptographically or logically separated mapping services with strict
  authorization.

A stable member reference may exist only in the minimum services necessary for
member-owned state, authorization, deletion, correction, or appeal. It must not
be exported as a general-purpose integration key.

### Join declaration

Every component contract must declare:

```text
joinKeys
joinPurpose
joinRetention
joinAuthorizer
joinOutputFields
reconstructionRiskAssessment
```

A missing join declaration means joining is denied.

### Join approval

Every cross-contract join requires:

- Named producer and consumer.
- Named join key.
- Specific purpose.
- Exact input and output fields.
- Retention class.
- Revocation behavior.
- Independent privacy review.
- Accessibility and safety review where applicable.
- Reconstruction test.
- Policy version.
- Review-by date.

## Prohibited joins

The following are prohibited:

- Discovery data joined with access-control data.
- Discovery data joined with consent history.
- Discovery data joined with reports, blocks, restrictions, or appeals.
- Identity claims joined with behavioral metadata.
- Broad-region changes joined across time into a movement history.
- Meeting-plan records joined into attendance/no-show history.
- Topic opt-ins joined across contexts into an interest or identity profile.
- Trusted-contact plans joined with counterpart data.
- Research data joined with operational member records.
- Analytics data joined with member-sensitive state.
- Moderator cases joined into a general member dossier.

## Release-gate tests

- Two individually valid contracts cannot join on a universal member identifier.
- A contract join fails without explicit join authorization.
- Join output rejects prohibited fields.
- Join output cannot reconstruct a sensitive profile in adversarial fixtures.
- Expired/revoked contracts cannot be used in a join.
- Analytics, research, and discovery consumers cannot request join-capable keys.
