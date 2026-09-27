# Invariant: No Member-Level Behavioral Metadata

**Invariant ID:** `INV-NO-MEMBER-LEVEL-BEHAVIORAL-METADATA`  
**Policy version:** `behavioral-metadata-v1`  
**Status:** approved  
**Owner:** Privacy Boundary Engineering  
**Privacy reviewer:** Privacy Review  
**Accessibility reviewer:** Accessibility Review  
**Safety reviewer:** Safety Review  
**Next review date:** 2027-03-26  
**Executable tests:** `tests/adversarial/no-member-level-behavioral-metadata.test.ts`

## Statement

Hearthline must not collect, retain, expose, aggregate, join, score, infer from,
or transmit member-level behavioral metadata except for a narrowly documented,
member-visible operational purpose that cannot be served by a less sensitive
mechanism.

## Rationale

Behavioral metadata can reconstruct sensitive information even when message
content is unavailable. Typing duration, edit count, delete count, checkpoint
latency, pause frequency, session length, interaction timing, view events, and
similar signals can become proxies for identity, disability, health, emotional
state, relationship interest, availability, vulnerability, or social value.

The absence of message-sentiment analysis does not protect members if the
behavioral ingredients used to construct sentiment or willingness profiles remain
available.

## Prohibited member-level fields

The following fields are prohibited from member-level analytics, discovery,
matching, ranking, moderation profiling, advertising, default model training,
experimentation, counterpart presentation, and cross-feature joins:

- Typing duration.
- Typing cadence.
- Keystroke intervals.
- Edit count.
- Delete count.
- Draft duration.
- Draft abandonment.
- Message composition time.
- Checkpoint response latency.
- Consent response latency.
- Pause frequency.
- Mute frequency.
- Hide frequency.
- Decline frequency.
- Block frequency.
- Report frequency.
- Session length.
- Session frequency.
- Screen dwell time.
- Scroll depth.
- View count.
- Profile-view count.
- Read receipt.
- Seen state.
- Online state.
- Last-active state.
- Notification open rate.
- Notification dismiss rate.
- Interaction timestamp sequence.
- Meeting-plan timing.
- Topic opt-in timing.
- Topic hide timing.
- Feature-flag assignment.
- Experiment cohort assignment.
- Device activity.
- Background activity.
- Browser/device fingerprint.
- Accessibility-path selection history.

## Narrow operational exceptions

An exception is permissible only when all conditions hold:

1. The field is required for a current member-requested operation.
2. The field is not retained after that operation unless a documented,
   content-minimized accountability purpose requires limited retention.
3. The field is not attached to a member analytics profile.
4. The field is not available to discovery, matching, ranking, moderation
   profiling, advertising, research, or model-training systems.
5. The field is not visible to another member.
6. The field has a named purpose, retention class, access control, and deletion
   behavior.
7. The exception has privacy, accessibility, and safety review.

Examples may include an ephemeral client-side timer for a member-selected
reminder or a short-lived server timestamp necessary to expire a capability
grant. Such data must not become behavioral telemetry.

## Required enforcement

- Client telemetry SDKs must denylist all prohibited fields.
- Server event schemas must reject prohibited fields.
- Analytics ingestion must reject prohibited fields and per-member behavioral
  event streams.
- Data warehouses must not contain prohibited fields keyed by member,
  pseudonymous member reference, device, account, or stable session identifier.
- Contract validation must reject behavioral-metadata fields.
- Model feature stores and vector pipelines must reject behavioral fields.
- Sensitive feature experiments must not collect behavioral outcome metrics.
- Moderator tooling must not display behavioral history.
- Any operational exception must be isolated, short-lived, and inaccessible to
  unrelated consumers.

## Required member-facing behavior

Hearthline must not display:

- Read receipts by default.
- Seen indicators by default.
- Online status.
- Last-active status.
- Typing indicators.
- View history.
- Response-rate indicators.
- Activity streaks.
- Attendance/reliability history.
- Behavioral labels.

## Release-gate tests

- Telemetry payloads containing prohibited fields are rejected.
- Analytics ingestion rejects per-member behavioral events.
- Discovery contracts reject behavioral metadata.
- Model/vector ingestion rejects writing style, timing, and interaction features.
- Counterpart payloads contain no behavioral state.
- Keyboard, screen-reader, text-first, low-bandwidth, and asynchronous paths do
  not emit additional behavioral telemetry.
