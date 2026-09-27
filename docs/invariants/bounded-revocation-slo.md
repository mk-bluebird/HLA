# Invariant: Bounded Revocation SLO

**Invariant ID:** `INV-BOUNDED-REVOCATION-SLO`  
**Policy version:** `revocation-slo-v1`  
**Status:** approved  
**Owner:** Consent Core Engineering  
**Privacy reviewer:** Privacy Review  
**Accessibility reviewer:** Accessibility Review  
**Safety reviewer:** Safety Review  
**Next review date:** 2027-03-26  
**Executable tests:** `tests/adversarial/bounded-revocation-slo.test.ts`

## Statement

Withdrawal, expiry, block, restriction, account closure, or policy-required
revocation must stop future sensitive capability access within a documented,
measured, bounded service-level objective.

## Scope

This invariant applies to:

- Media viewing and sharing.
- Voice/video calls.
- Sexual-topic discussion modes.
- Named public-venue disclosure.
- Public-first meeting-plan capabilities.
- Trusted-contact plan disclosure.
- Recording.
- AR camera.
- AR microphone.
- AR spatial mapping.
- Any future sensitive recipient-scoped capability.

## Required enforcement

### Synchronous action-time authorization

High-sensitivity actions require a current server-side grant check at action
time. Cache state cannot override denial.

### Versioned, revocable session state

- Capability tokens must include grant version or revocation epoch.
- Server-side token introspection must reject stale version/epoch.
- Signed links must be revocable.
- Active sessions must receive revocation signal or heartbeat reauthorization.
- Queued work must verify authorization immediately before execution.
- Cached recipient projections must be invalidated.

### Service-level objective

Each capability class must document:

```text
maximum revocation propagation target
maximum heartbeat interval, if applicable
maximum token cache lifetime
maximum queue-cancellation interval
maximum active-session termination interval
measurement method
alert threshold
incident response path
```

The SLO must be selected per capability. The platform must not claim immediate
termination where architecture cannot provide it; member-facing language must be
accurate about future platform access and active-session termination behavior.

## Required member-facing behavior

When revocation is completed, the member receives clear confirmation of the
platform effect. The platform must not claim to erase information a recipient
copied, recorded, exported, or remembered before revocation.

## Release-gate tests

- Revoke during active persistent session.
- Revoke before queued action executes.
- Revoke after signed-link issuance.
- Revoke with stale authorization cache.
- Revoke with reconnecting client.
- Revoke under degraded event-consumer conditions.
- Verify fail-closed behavior when current grant state is unavailable.
- Measure termination/denial against documented SLO.
