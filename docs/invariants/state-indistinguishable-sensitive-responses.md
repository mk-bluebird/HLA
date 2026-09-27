# Invariant: State-Indistinguishable Sensitive Responses

**Invariant ID:** `INV-STATE-INDISTINGUISHABLE-SENSITIVE-RESPONSES`  
**Policy version:** `sensitive-response-v1`  
**Status:** approved  
**Owner:** Platform Security Engineering  
**Privacy reviewer:** Privacy Review  
**Accessibility reviewer:** Accessibility Review  
**Safety reviewer:** Safety Review  
**Next review date:** 2027-03-26  
**Executable tests:** `tests/adversarial/state-indistinguishable-sensitive-responses.test.ts`

## Statement

Sensitive endpoints must not reveal hidden counterpart state through response
status, response shape, payload detail, ordering, timing behavior, or
counterpart-visible UI transitions beyond what the requesting member is
authorized to know.

## Scope

Sensitive hidden state includes:

- Whether another member blocked, muted, restricted, hid, paused, withdrew, or
  reported.
- Whether another member is online, nearby, active, typing, viewing, or present.
- Whether a grant was denied for a private reason.
- Whether a topic threshold is waiting on a participant.
- Whether a report exists or is under review.
- Whether a meeting plan was attended, cancelled by another person, or ignored.
- Whether a counterpart has hidden a topic or discovery option.

## Required controls

Use combinations of:

- Uniform response categories where safe.
- Constant response shape for equivalent authorization classes.
- No counterpart-specific error reasons.
- No progress indicators revealing participant state.
- No counterpart-visible state-transition timing.
- Batching or deferred non-urgent updates where privacy benefit is demonstrated.
- Minimum response duration or bounded jitter only when threat modeling justifies
  it and it does not delay urgent member controls.
- Separate operational performance telemetry from member identity.
- No timing normalization that delays block, withdrawal, revocation, or quiet
  exit confirmation to the member initiating the control.

## Prohibited claims

The platform must not claim strict constant-time behavior for all network/API
operations unless it can demonstrate that property under its deployment model.

## Release-gate tests

- Statistical latency comparison for documented sensitive endpoint classes.
- Response-shape comparison across hidden counterpart states.
- Status-code comparison across hidden counterpart states.
- UI snapshot comparison across hidden counterpart states.
- No threshold-progress, waiting-on, roster, seen, online, or presence leak.
- Accessibility-path response comparison.
