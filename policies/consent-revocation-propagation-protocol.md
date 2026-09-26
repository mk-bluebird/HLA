# Consent Revocation Propagation Protocol

## Purpose

This protocol ensures that withdrawing a capability-scoped consent grant stops
the relevant access and action pathways across Hearthline systems.

It applies to consent withdrawal, scope reduction, expiry, blocking, restriction,
account closure, and policy-required revocation.

## Fail-closed rule

A service must deny a sensitive action unless it can confirm:

- The grant exists.
- The grant is active.
- The grant is unexpired.
- The grant version is current.
- The recipient and purpose match.
- No block, restriction, withdrawal, expiry, or policy boundary applies.

Cached authorization may optimize a read only when the cache is version-bound,
short-lived, revocation-aware, and not used as the final authorization decision
for high-sensitivity actions.

## High-sensitivity capabilities

The following require synchronous current-grant validation at action time:

- Voice call.
- Video call.
- Media sharing or viewing.
- Sexual-topic discussion mode.
- Named public-venue detail.
- Public-first meeting-plan acceptance.
- Trusted-contact plan sharing.
- Recording.
- AR camera.
- AR microphone.
- AR spatial mapping.

## Required revocation effects

On withdrawal, block, restriction, expiry, or account closure:

- New access is denied immediately.
- Capability tokens become invalid.
- Recipient-scoped projections are removed.
- Signed links are invalidated.
- Queued notifications and actions are cancelled.
- Active feature sessions lose the capability.
- New media access is denied.
- New call or recording access is denied.
- New venue-detail access is denied.
- Future reads fail closed.
- Content-minimized accountability event is recorded.
- No retaliation, visibility penalty, explanation demand, or re-prompt occurs.

## Scope rule

Revocation applies to the exact capability, purpose, recipient scope, and
derived access objects covered by the grant.

A withdrawal of media permission must not silently close an unrelated direct
conversation unless the member separately chooses that action.

A block must override all active capability grants for the blocked relationship.

## Observability

The system may monitor operational delivery of revocation events, but must not
turn withdrawal patterns into member-level risk, trust, safety, reputation,
desirability, engagement, or behavioral profiles.

## Required member-facing text

> Your permission has been withdrawn for this feature. Hearthline will stop new
> access through this feature. It cannot erase information another person
> independently copied, recorded, or remembered before withdrawal.
