# HLA Agent Hardened Interaction Invariants

**Status:** Normative design invariants  
**Applies to:** HLA Agent, Social-Decoder, conversation startup, Topic Boards,
casual-connection coordination, capability-scoped consent, planning tools,
member controls, reporting, and moderation  
**Version:** 0.3.0-design-hardened

## 1. Planning without outcome optimization

HLA may support voluntary planning clarity and mutual acknowledgment.

HLA may provide:

- Broad future activity windows.
- Remote-first and public-first activity templates.
- Source-labeled public meeting-option information.
- Source-labeled transit information.
- Member-invoked invitation drafts.
- Member-invoked decline, pause, and exit drafts.
- Mutual public-first meeting-plan templates.
- Member-controlled trusted-contact plan templates.
- Capability-scoped consent checkpoints.
- Member-requested reminders.
- Clear cancellation, pause, withdrawal, and quiet-exit controls.

HLA must not:

- Predict whether a meeting will occur.
- Optimize for meeting occurrence.
- Measure attendance.
- Require arrival confirmation.
- Measure no-shows.
- Generate reliability scores.
- Create countdowns or urgency prompts.
- Penalize a member for declining, cancelling, pausing, withdrawing, or not
  creating a meeting plan.
- Notify a counterpart that another member declined, cancelled, paused, or
  withdrew unless the member separately chooses to send a message.
- Treat meeting occurrence, date count, relationship outcome, sexual activity,
  media sharing, or disclosure as a success metric.

### Planning invariant

```text
PlanningSupport ≠ MeetingPrediction
PlanningSupport ≠ MeetingOptimization
PlanningSupport ≠ AttendanceTracking
PlanningSupport ≠ ObligationToMeet
```

## 2. Baseline consent checkpoints

Every capability transition requires a separate, member-visible,
capability-scoped consent checkpoint where the feature requires permission.

Mandatory baseline transition points include:

```text
Discovery option → reciprocal-interest window
Reciprocal interest → direct conversation
Direct conversation → voice call
Direct conversation → video call
Direct conversation → media sharing
Direct conversation → sexual-topic discussion
Interaction context → named public-venue disclosure
Interaction context → public-first meeting plan
Any interaction → recording
Any interaction → AR camera
Any interaction → AR microphone
Any interaction → AR spatial mapping
```

A baseline checkpoint must identify:

- The exact capability.
- The purpose.
- The recipient or session scope.
- Data or device access involved.
- Whether recording, retention, export, or copying risk exists.
- Expiry.
- Withdrawal mechanism.
- Block and report controls.
- A clear statement that declining, waiting, silence, or changing one’s mind has
  no penalty.

### Optional additional checkpoints

Members may create additional voluntary checkpoints, including:

- “Ask again before discussing sexual topics.”
- “Ask before switching from text to voice.”
- “Ask before suggesting a named venue.”
- “Ask before sharing media.”
- “Ask before continuing a discussion after a pause.”
- “Ask before changing from public-first to another setting.”

Additional checkpoints must not:

- Be required to use core account, privacy, exit, blocking, or reporting tools.
- Reveal a member’s reason for wanting an additional checkpoint.
- Affect discovery ranking, visibility, access, or social standing.
- Become an identity, health, disability, risk, or vulnerability label.

### Wording variation

HLA may use a reviewed catalog of semantically equivalent plain-language
checkpoint wording.

Wording variation must:

- Preserve the same substantive permission request.
- Preserve the same decline, pause, withdrawal, and exit controls.
- Be accessible through text-first, keyboard, screen-reader, reduced-motion,
  low-bandwidth, no-camera, no-microphone, no-voice, and asynchronous paths.
- Be versioned and reviewable.
- Not be randomized as a trap, suspicion test, A/B coercion experiment, or
  behavioral manipulation mechanism.

### Checkpoint invariant

```text
Baseline checkpoint required at defined capability transitions.

No baseline checkpoint implies suspicion.
No optional checkpoint implies vulnerability.
No checkpoint may create pressure to proceed.
```

## 3. Topic Board states

Topic Board visibility and activation are distinct.

### Topic states

```text
DRAFT
PROPOSED_TO_PARTICIPANTS
VISIBLE_FOR_REVIEW
HIDDEN_BY_PARTICIPANT
BLOCKED_BY_PARTICIPANT
ACTIVE_FOR_OPTED_IN_PARTICIPANTS
PAUSED
CLOSED
DELETED
```

### State meaning

| State | Meaning | May topic be treated as active conversation context? |
|---|---|---:|
| `DRAFT` | A private topic under preparation | No |
| `PROPOSED_TO_PARTICIPANTS` | A member proposed a topic to a defined participant set | No |
| `VISIBLE_FOR_REVIEW` | A topic may be reviewed as proposed/not active | No |
| `HIDDEN_BY_PARTICIPANT` | One participant hid the topic for themselves | No for that participant |
| `BLOCKED_BY_PARTICIPANT` | One participant blocked the topic or board | No for that participant |
| `ACTIVE_FOR_OPTED_IN_PARTICIPANTS` | Topic may be used by participants who individually opted in | Yes, only for opted-in participants |
| `PAUSED` | Topic activity is paused | No |
| `CLOSED` | Topic is closed to new use | No |
| `DELETED` | Topic content and projections are deleted per retention policy | No |

### Visibility rules

A topic may become visible to participants as:

```text
Proposed — not active — no response expected.
```

Visibility alone must not:

- Signal consensus.
- Signal mutual interest.
- Create a response expectation.
- Open direct messaging.
- Open a call, media, sexual-topic discussion, venue disclosure, meeting plan,
  recording, AR access, or any other capability.
- Reveal why another participant proposed, hid, blocked, paused, or closed it.
- Change a participant’s discovery visibility, ranking, or access.
- Be used to infer identity, health, relationship intent, finances, accessibility,
  vulnerability, or behavior.

### Activation rule

For a two-person interaction:

\[
TopicActive(t,\{a,b\}) =
OptIn(a,t)
\land
OptIn(b,t)
\land
\neg Blocked(a,b)
\land
\neg Paused(t)
\]

For a group interaction:

\[
TopicActiveFor(p,t) =
OptIn(p,t)
\land
GroupTopicThresholdMet(t)
\land
\neg BlockedOrHiddenBy(p,t)
\]

A group threshold may permit a topic to become active for the subset of
participants who opted in. It must not activate the topic for a participant who
has not opted in.

### Group threshold constraints

A group-defined threshold:

- Must be visible before a participant opts in.
- Must be expressed as a count or proportion, not a social-pressure signal.
- Must not name or identify non-participants.
- Must not reduce access, rank, or visibility for people who do not opt in.
- Must not become a popularity metric.
- Must not be used to determine trust, safety, desirability, compatibility, or
  social value.
- Must not permit use of a topic for a non-opted-in participant.
- Must not reveal which participant prevented a threshold from being met.

### Hide and block rules

Any participant may hide or block a topic for themselves.

When a participant hides or blocks a topic:

- The topic is no longer displayed to that participant.
- The topic cannot become active for that participant.
- No counterpart or group participant receives notification.
- The system does not expose a hide/block reason.
- The participant may leave the board without explanation.
- The action does not affect the participant’s general discovery visibility or
  standing.

### Topic invariant

```text
Topic visibility ≠ Topic activation.
Topic activation ≠ Consent to another capability.
Group threshold ≠ Individual consent.
Hide/block ≠ Social penalty.
```

## 4. Actor controls, reports, and moderation

Three separate classes of action must be handled differently.

### 4.1 Actor-initiated personal controls

Actor-initiated personal controls include:

- Block.
- Mute.
- Restrict.
- Quiet exit.
- Pause discovery.
- Withdrawal of consent.
- Reduction of disclosure scope.
- Revocation of venue detail.
- Cancellation of a meeting plan.
- Deletion of member-owned optional data.

When a member invokes a personal control:

- The member receives a clear, accessible confirmation.
- The system states the immediate effect on the member’s own access/control.
- The counterpart receives no notification.
- The counterpart receives no private reason.
- The system creates no public signal.
- The control takes effect immediately or fails closed.
- The system does not imply that it can prevent offline contact, copied media,
  screenshots, or actions outside Hearthline.

Required confirmation example:

> Your block is active. This person can no longer contact you through
> Hearthline. They are not notified. Blocking does not guarantee prevention of
> offline contact or access to information they independently copied earlier.

### 4.2 Member-submitted reports

A report is distinct from a block.

When a member submits a report:

- The member receives confirmation that the report was received.
- The member receives a plain-language outline of the review process.
- The member may save or update selected report material where policy allows.
- The member may request human review.
- The member may withdraw or supplement a report as policy permits.
- The reporting member does not receive confidential enforcement details about
  another member.
- The reported member does not receive reporter identity or private rationale
  unless required by applicable process and safe to disclose.
- Report content is not reused for discovery, advertising, ranking, or default
  model training.

Required confirmation example:

> Your report was received. Hearthline will review it under the applicable
> policy. We may not be able to share details of actions involving another
> member. You can review your report, add information where available, or ask
> for human review.

### 4.3 Platform moderation

Platform moderation may include a warning, restriction, suspension, removal, or
other material account action under documented policy.

When material action affects a member:

- The affected member receives plain-language notice where safe and feasible.
- The notice identifies the policy basis at an appropriate level of detail.
- The member receives a correction route.
- The member receives an accessible appeal route.
- The member receives human-review information where required.
- The platform protects reporter confidentiality, safety-sensitive details,
  anti-abuse methods, and third-party private data.
- The platform does not silently impose a material restriction without a review
  and appeal pathway, except where temporary safety-preserving action is needed;
  such action must still receive prompt review.

### Control and moderation invariant

```text
Actor control → actor confirmation + counterpart silence.

Report → reporter acknowledgement + process clarity + enforcement confidentiality.

Material platform moderation → affected-member notice where safe + policy basis
+ correction and appeal.
```

## 5. Global invariants

### 5.1 No person-level scoring

HLA must not create, display, store, or use:

- Safety score.
- Risk score.
- Trust score.
- Reputation score.
- Popularity score.
- Desirability score.
- Compatibility score.
- Response-likelihood score.
- Reliability score.
- No-show score.
- Influence score.
- Social-acceptance score.
- Relationship-readiness score.
- Vulnerability score.
- Identity authenticity score.

### 5.2 No prohibited inference

HLA must not infer motive, intent, future activity, availability, emotion,
health, recovery, disability, finances, housing, employment, vulnerability,
identity, sexuality, protected traits, relationship status, or private
circumstance from:

- Profile text.
- Private messages.
- Media.
- Device data.
- Calendar data.
- Location data.
- Routes.
- Venue attendance.
- Wi-Fi or Bluetooth signals.
- Contacts or social graph.
- Profile views.
- Read receipts.
- Response time.
- Typing behavior.
- Account tenure.
- Image, voice, body, gait, gaze, biometric, or emotion analysis.
- Silence, non-response, pauses, blocks, reports, or withdrawals.

### 5.3 No location or attendance processing

HLA must not collect, derive, retain, display, or use:

- Coordinates.
- GeoHash.
- IP-derived location.
- GPS.
- Route.
- Distance.
- Radius.
- Device proximity.
- Wi-Fi or Bluetooth proximity.
- Location history.
- Venue history.
- Check-ins.
- Attendance.
- Arrival or departure state.
- Nearby status.
- Current presence.
- Frequently visited places.

The only permitted spatial inputs for discovery are:

- Member-selected broad region identifier.
- `remote`.
- `public_category_only`.

### 5.4 No autonomous social action

```text
externalActionAuthorized = false
```

must remain true for all HLA-generated drafts, topic proposals, reminders,
planning suggestions, and explanations.

HLA must not send messages, accept or decline invitations, schedule meetings,
make contact, negotiate, disclose, grant consent, withdraw consent, file a
report, or contact external parties for a member.

### 5.5 No default chat surveillance

HLA may process only member-selected content for a member-invoked task.

It must not process:

- All conversations by default.
- Unselected message history.
- Private media by default.
- Calls or recordings by default.
- Background device activity.
- Calendar or location data.

### 5.6 Non-commercial integrity

HLA must not facilitate or optimize:

- Social or sexual access in exchange for money.
- Housing in exchange for social or sexual access.
- Employment in exchange for social or sexual access.
- Debt relief in exchange for social or sexual access.
- Transportation dependency in exchange for social or sexual access.
- Drugs in exchange for social or sexual access.
- Gifts or basic needs conditioned on social or sexual access.

### 5.7 Planning and autonomy

```text
Planning clarity ≠ Meeting prediction.
Planning clarity ≠ Meeting optimization.
Planning clarity ≠ Attendance tracking.
Planning clarity ≠ Obligation to meet.
```

### 5.8 Consent and topic states

```text
Topic visibility ≠ Topic activation.
Topic activation ≠ Consent to another capability.
Group threshold ≠ Individual consent.
Preference ≠ Consent.
Prior consent ≠ Current consent.
Match ≠ Consent.
Message ≠ Consent.
Meeting plan ≠ Consent to travel, private venue, recording, touch, or intimacy.
```

## 6. Release gates

A feature that touches conversation startup, topics, casual connection,
discovery, media, calls, venue detail, meeting plans, reporting, or moderation
must not release unless:

- Consent and revocation tests pass.
- All capability transitions have documented baseline checkpoints.
- Text-first and keyboard paths pass.
- Screen-reader, reduced-motion, low-bandwidth, no-camera, no-microphone,
  no-voice, and asynchronous paths pass.
- Allowed-field and forbidden-field boundary tests pass.
- No person-score field exists in schema, API, storage, analytics, or model
  input.
- No location, route, attendance, or presence field exists in schema, API,
  storage, analytics, or model input.
- Block, report, correction, appeal, and quiet-exit paths pass.
- Retention, expiry, deletion, and revocation propagation pass.
- Non-commercial integrity tests pass.
- Required privacy, accessibility, safety, policy, and legal reviews are
  complete.
