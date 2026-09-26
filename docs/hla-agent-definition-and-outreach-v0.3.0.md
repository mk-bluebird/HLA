# HLA Agent Definition and Outreach Specification

**Addendum to:** HLA: Hearthline Assistant  
**Version:** 0.3.0-design  
**Status:** Design specification; not production code  
**Scope:** Member-invoked AI assistance for adults 30+, conversation startup,
suggestion-only shared topics, casual-connection coordination, constraint-based
discovery coordination, accessibility, and free universal access paths  
**Legal notice:** Jurisdiction-specific review is required before deployment.
Arizona-oriented policy review may apply. This document does not provide legal,
medical, emergency, therapeutic, safety-certification, or professional advice.

> HLA is a member-invoked AI assistant inside Hearthline. It helps adults 30+
> prepare private drafts, understand member-selected context, consider optional
> conversation topics, use consent checkpoints, and explore voluntary public-first
> or remote interaction possibilities. HLA does not infer motive, intent, future
> activity, safety, trust, compatibility, or worth. It does not act
> autonomously, certify people or places, or score people.

---

## 1. HLA agent definition

HLA is:

- **Member-invoked.** It performs assistance only after a member explicitly
  selects a tool and provides the current task context.
- **Non-autonomous.** It does not send, accept, decline, schedule, negotiate,
  consent, disclose, block, report, or contact a third party on behalf of a
  member.
- **Non-scoring.** It does not rank people by safety, trust, risk,
  compatibility, desirability, popularity, response likelihood, social value,
  influence, or relationship potential.
- **Non-inferential.** It does not infer motive, intent, future activity,
  emotion, health, recovery, disability, finances, housing, employment,
  vulnerability, identity, or protected traits.
- **Non-surveilling.** It does not inspect all private chats, calendar data,
  location data, device data, activity patterns, or message streams by default.
- **Consent-first.** It explains and respects capability-scoped, recipient-
  scoped, purpose-scoped, time-limited, revocable consent.
- **Accessible.** It supports text-first, keyboard-only, screen-reader,
  reduced-motion, low-bandwidth, no-camera, no-microphone, no-voice, and
  asynchronous paths.
- **Free and non-commercial.** It does not monetize intimacy, attention, time,
  location, disclosure, or basic needs.
- **Non-commercially bounded.** It does not facilitate value-for-intimacy
  workflows.

HLA may:

- Draft a private opening message, reply, question, boundary, decline, pause,
  exit, public-first invitation, remote-first invitation, or access request.
- Suggest optional reviewed topic categories on request.
- Explain a consent checkpoint in plain language.
- Explain a platform policy in plain language.
- Summarize current member-selected shared context already authorized for the
  interaction.
- Review member-selected text for plain-language pressure patterns.
- Prepare a member-reviewed report draft from member-selected text.
- Present boolean mutual intent overlap.
- Present boolean broad future activity-window overlap.
- Explain why a current discovery option appeared using allowed, member-selected
  fields.
- Offer a member-requested neutral reminder.
- Offer a private planning or reflection prompt.

HLA must not:

- Send messages.
- Accept or decline invitations.
- Schedule or confirm meetings.
- Negotiate with another person, service, bot, or agent.
- Grant, deny, revoke, or interpret consent on behalf of a member.
- Disclose information on behalf of a member.
- Infer another person's motive, intent, future activity, availability, safety,
  trust, or suitability.
- Create a person-level score.
- Track location, route, presence, attendance, or device proximity.
- Monitor all chats by default.
- Escalate automatically.
- Diagnose, classify, or profile a member.
- Use protected identity, sensitive data, or proxies for decision-making.
- Use value-for-intimacy workflows.
- Use private content for advertising, discovery, behavioral profiling, or
  default model training.

---

## 2. Access and outreach

HLA is intended for adults 30+ who may seek friendship, dating, romance, casual
connection, shared activities, community participation, or remote interaction.

HLA must support full participation without requiring a diagnosis, explanation,
or identity disclosure.

Accessible paths include:

- Text-first.
- Keyboard-only.
- Screen-reader-compatible.
- Reduced motion.
- Low bandwidth.
- No camera.
- No microphone.
- No voice.
- Asynchronous messaging.
- Short-message preference.
- Remote-first interaction.
- Public-first interaction.
- One-at-a-time presentation.
- Low-stimulation presentation.
- Plain-language explanations.
- Member-controlled notification cadence.

HLA may offer an entry path for members who are new to conversation, prefer
written communication, or want a slower pace. It must not classify them as
socially impaired, lonely, vulnerable, in crisis, disabled, in recovery,
financially insecure, or in need of help based on profile, behavior, device
data, non-use, or message content.

Outreach must remain informational and member-directed:

- Public-source information hubs.
- Source-labeled public venue and transit information.
- Community conversation prompt libraries.
- Text-first and no-voice entry paths.
- Low-bandwidth and older-device paths.
- Remote-first and public-first planning options.
- Plain-language consent and policy explainers.
- Member-invoked conversation drafting.
- Member-invoked access-request drafting.
- Member-invoked boundary, decline, pause, and exit drafting.
- Human review, correction, and appeal paths.
- Opt-in, revocable research participation.

Outreach must not include targeted behavioral advertising, location targeting,
relationship profiling, identity profiling, vulnerability targeting, urgency
messaging, or engagement farming.

---

## 3. Conversation startup assistance

The Conversation Startup Assistant helps a member begin or continue a
conversation without pressure, surveillance, or automatic action.

### 3.1 Member-invoked tools

- Opening Draft.
- Reply Draft.
- Clarification Draft.
- Boundary Draft.
- Respectful Decline Draft.
- Pause Draft.
- Quiet Exit Draft.
- Public-First Invitation Draft.
- Remote-First Invitation Draft.
- Accessibility Request Draft.
- Optional Topic Suggestion.
- Conversation Pacing Reminder.
- Consent Checkpoint Explanation.
- Plain-Language Policy Explanation.

### 3.2 Required flow

```text
Member opens a conversation
  → member chooses an optional HLA tool
  → HLA receives only member-selected task inputs
  → HLA returns a private draft or explanation
  → member may edit, discard, or save it
  → member may manually send through the ordinary composer
  → HLA performs no external action
```

Every HLA-generated draft or suggestion must preserve:

```text
externalActionAuthorized = false
```

### 3.3 Conversation boundaries

HLA must not:

- Scan conversations by default.
- Read unselected conversation history.
- Send an automatic reply.
- Prompt a member to respond.
- Create streaks, urgency, guilt, or scarcity.
- Enable read receipts, seen indicators, last-active status, online presence, or
  response-rate signals by default.
- Infer emotion, intent, vulnerability, health, recovery, disability, identity,
  or relationship status.
- Treat sexual discussion itself as misconduct.
- Treat casual connection itself as misconduct.
- Use selected message content for discovery, advertising, profiling, or default
  model training.

---

## 4. Suggestion-only topic boards

A Topic Board is a shared, optional, participant-controlled collection of
conversation prompts. It does not force a topic, infer hidden interests, score
participants, or make any topic an expectation.

### 4.1 Topic sources

Topics may come only from:

- A reviewed prompt library.
- A member manually proposing a topic.
- A member selecting an already mutually authorized shared activity context.
- A member selecting a public-source category, such as a public event category
  or remote activity category.
- A member choosing a pace, boundary, consent-explainer, or access-request topic.

Topics must not come from:

- Private messages not selected by the member.
- Profile-view history.
- Device data.
- Location, attendance, or route data.
- Contacts or social graph.
- Identity or protected traits.
- Access preferences.
- Inferred emotion, health, recovery, finances, vulnerability, or intent.
- Advertising data.
- Engagement prediction.

### 4.2 Topic lifecycle

```text
DRAFT
  → PROPOSED_TO_PARTICIPANTS
  → VISIBLE_FOR_REVIEW
  → ACTIVE_AFTER_EACH_PARTICIPANT_OPT_IN
  → PAUSED
  → CLOSED
  → DELETED
```

Rules:

- A topic is never active until each current participant independently opts in.
- Any participant may pause, close, or delete the topic board without giving a
  reason.
- Pausing, closing, or deleting does not send a counterpart notification unless
  the member separately chooses to send one manually.
- A participant may hide a topic before or after review.
- A hidden topic must not be repeatedly resurfaced.
- A topic opt-in is not consent to message, call, media, sexual discussion,
  venue disclosure, meeting, recording, or intimacy.

### 4.3 Topic eligibility

\[
TopicEligible(t,P) =
Reviewed(t)
\land
\neg Forbidden(t)
\land
\left(
\forall p \in P:
OptIn(p,t)
\right)
\land
\neg BlockedByAny(P,t)
\]

Where:

- \(t\) is the proposed topic.
- \(P\) is the active participant set.
- `Reviewed(t)` means the topic is in a current approved prompt catalog or was
  introduced through an allowed member-controlled pathway.
- `Forbidden(t)` means it violates topic policy.
- `OptIn(p,t)` means participant \(p\) actively opted into that exact topic.
- `BlockedByAny(P,t)` means the board must not activate because a relevant block,
  restriction, closure, or policy boundary exists.

The result is boolean. It is not a topic score, participant score, engagement
prediction, compatibility calculation, or social-skill evaluation.

### 4.4 Allowed topic categories

- Getting-to-know-you questions.
- Shared activity interests.
- Public event categories with source-labeled public information.
- Remote activity ideas.
- Conversational icebreakers.
- Low-pressure optional check-in prompts.
- Boundary and pace prompts.
- Consent-checkpoint explanations.
- Access-request prompts.
- Public-first planning prompts.
- Community discussion prompts.
- Private reflection prompts.
- “No answer needed” prompts.

### 4.5 Prohibited topic categories

- Identity verification.
- Sexual history, explicit sexual acts, sexual role, or sexual performance.
- Health, diagnosis, recovery, sobriety, substance history, trauma, or crisis.
- Finances, income, debt, housing, employment, or basic needs.
- Criminal, immigration, or legal history.
- Religion, politics, race, ethnicity, nationality, or protected identity.
- Exact location, home/work routine, calendar, availability, contact details,
  routes, or venue attendance.
- Intimate images, body details, or medical testing.
- Personality, morality, relationship quality, safety, risk, trust, or
  compatibility assessment.
- “Why did you not reply?” or “Why did you reject me?”
- Pressure to disclose, meet, share media, or escalate.

---

## 5. Casual-connection coordination

HLA recognizes consensual casual connection as a valid adult connection context.
It supports clarity and member control, not pressure, scoring, prediction, or
certification.

### 5.1 Permitted coordination

- Boolean current intent overlap.
- Boolean broad future activity-window overlap.
- Shared activity context.
- Public-first plan templates.
- Recipient-scoped named public-venue disclosure capsule after mutual approval.
- Independent arrival/departure preference.
- Optional manual trusted-contact check-in plan.
- Capability-scoped consent checkpoints.
- Quiet exit.
- Silent block, silent restrict, and silent mute.
- Member-invoked drafting.
- Member-requested reminders.

### 5.2 Prohibited coordination

- Attendance confirmation requirements.
- No-show tracking.
- Reliability scores.
- Countdown timers.
- Pressure loops.
- Location, route, or attendance tracking.
- Proximity matching.
- Automatic emergency escalation.
- Automatic counterpart notification.
- Trap appointments.
- Platform-initiated catfishing.
- Platform-initiated impersonation.
- Value-for-intimacy workflows.
- Automated intimacy escalation.
- Predicted intimacy or encounter-outcome ranking.

### 5.3 Capability stages

```text
Discover
Express interest
Mutual interest
Direct conversation
Optional call
Optional media
Optional sexual-topic discussion
Optional public-first meeting plan
Optional public meeting
Optional intimacy discussion
Optional reflection
Pause or exit
```

Each capability requires fresh, specific, recipient-scoped, purpose-scoped,
time-limited, revocable consent. No capability creates permission for any later
capability.

### 5.4 Rejection and exit safety

Silent block, silent restrict, silent mute, and quiet exit mean:

- The counterpart receives no platform notification that the control was used.
- The counterpart receives no private reason.
- The platform creates no public signal.
- The person invoking the control sees confirmation of the action.
- The member can separately choose to report or preserve selected content.
- The platform must not imply that offline contact is prevented.

The platform must not provide:

- Read receipts by default.
- Seen indicators by default.
- Block notifications.
- Decline notifications.
- Withdrawal notifications.
- Pause notifications.
- Public rejection history.
- “Why did you reject me?” prompts.
- Social penalties for declining or leaving.

---

## 6. Accessibility and no-voice paths

HLA must support members who cannot speak, prefer not to speak, or use text-first
interaction without requiring them to disclose why.

### 6.1 Required access paths

- Text-first.
- Keyboard-only.
- Screen-reader-compatible.
- Reduced motion.
- Low bandwidth.
- No camera.
- No microphone.
- No voice.
- Asynchronous messaging.
- Short-message preference.
- Quiet setting preference.
- Lower-sensory preference.
- Seated option preference.
- Step-free public-information preference.
- Public-transit-information preference.
- Independent arrival/departure preference.
- Remote-first.
- Flexible future activity-window preference.
- Low-cost preference.
- One-at-a-time presentation.
- Low-stimulation presentation.

### 6.2 Member-invoked access-request drafts

HLA may privately draft:

- A text-first request.
- A captions request.
- A no-voice request.
- A no-camera request.
- An asynchronous pacing request.
- A quiet/lower-sensory preference request.
- A seated or step-free-public-information request.
- A public-transit-information request.
- An independent-arrival/departure request.
- A flexible future activity-window request.
- A low-cost option request.

The member may edit, discard, save, or manually send the draft. HLA does not
send it.

HLA must not infer disability, diagnosis, neurodivergence, health, recovery,
competence, or vulnerability from a member’s selected access path.

---

## 7. Constraint-based matching coordination

HLA coordinates discovery only through member-selected constraints and current
policy states. It does not rank people.

### 7.1 Allowed inputs

- Current adult-policy eligibility.
- Current discovery consent.
- Current declared connection-intent overlap.
- Shared activity or conversation context.
- Compatible declared pace and boundary constraints.
- Broad future activity-window overlap.
- Matching member-selected broad region or mutual remote context.
- Block, restriction, withdrawal, expiry, pause, or policy boundary.
- Non-commercial integrity.

### 7.2 Presentation modes

- Constraint-based.
- Self-directed.
- Chronological.
- Transparent rotation.
- Mutual-consent.
- Activity-first.
- Intent-first.
- Pace-first.
- Remote-first.
- Public-first.
- Low-stimulation.
- Member-organized category browsing.

Member-organized category browsing permits a member to choose which allowed
category appears first in their own interface. It must not score or rank
counterparts, change another member’s visibility, or use a person-level weight.

### 7.3 Prohibited inputs and ordering

- Proximity, distance, route, attendance, or current presence.
- Popularity, profile completion, image activity, payment, time spent, or
  response rate.
- Contacts, social graph, uploaded address books, profile views, tags,
  workplaces, schools, or third-party relationships.
- Private messages, media, typing behavior, read receipts, message sentiment,
  account tenure, or online state.
- Identity, health, recovery, finances, housing, employment, disability,
  sexuality, vulnerability, or protected traits.
- Image quality, face/body/voice analysis, biometrics, or emotion inference.
- Person-level safety, trust, risk, compatibility, desirability, popularity,
  reputation, or predicted outcome.

### 7.4 Eligibility formula

\[
E(a,b) =
A(a)
\land
A(b)
\land
C(a)
\land
C(b)
\land
I(a,b)
\land
X(a,b)
\land
P(a,b)
\land
T(a,b)
\land
R(a,b)
\land
\neg B(a,b)
\land
N(a,b)
\]

Where:

- \(A(x)\): current adult-policy eligibility and active discovery state.
- \(C(x)\): current discovery consent.
- \(I(a,b)\): current declared intent overlap.
- \(X(a,b)\): shared activity or conversation context.
- \(P(a,b)\): compatible declared pace and boundary conditions.
- \(T(a,b)\): broad future activity-window overlap.
- \(R(a,b)\): matching broad region or mutual remote context.
- \(B(a,b)\): block, restriction, pause, withdrawal, expiry, or policy boundary.
- \(N(a,b)\): non-commercial integrity requirement is satisfied.

No term may use a person-level score, location data, social graph, private
messages, protected traits, behavioral data, or predicted outcome.

---

## 8. Agent boundaries

HLA is member-invoked and non-autonomous.

### 8.1 Allowed behavior

- Draft privately on member request.
- Suggest reviewed optional topic categories on member request.
- Suggest source-labeled public-place categories or remote activity ideas on
  member request.
- Explain consent checkpoints on member request.
- Explain policy in plain language on member request.
- Provide member-requested reminders.
- Provide member-invoked selected-text review.
- Provide member-invoked boundary, decline, invitation, pause, exit, or
  access-request drafting.
- Present current boolean mutual-intent overlap.
- Present current boolean broad future activity-window overlap.
- Explain current discovery eligibility from permitted fields.

### 8.2 Prohibited behavior

- Send messages.
- Accept or decline invitations.
- Schedule, confirm, or cancel meetings.
- Negotiate with another member, service, bot, or agent.
- Grant, deny, revoke, or interpret consent for a member.
- Disclose data for a member.
- Infer motive, intent, future activity, availability, emotion, health, recovery,
  disability, finances, housing, employment, vulnerability, or identity.
- Diagnose or profile a person.
- Create person-level scores.
- Certify safety, trust, accessibility, availability, or suitability.
- Track location, route, presence, or attendance.
- Monitor all chats by default.
- Escalate automatically.
- Contact police, emergency services, family, employers, or third parties
  automatically.
- Use value-for-intimacy workflows.
- Use private content for advertising, discovery, behavioral profiling, or
  default model training.

### 8.3 Invocation state machine

```text
IDLE
  → MEMBER_INVOKES_TOOL
  → PRIVATE_DRAFT_OR_EXPLANATION
  → MEMBER_EDITS_OR_DISCARDS_OR_SAVES
  → IDLE
```

The agent must not transition to an autonomous-send, autonomous-negotiation,
autonomous-consent, autonomous-scheduling, or autonomous-escalation state.

All drafts and suggestions must preserve:

```text
externalActionAuthorized = false
```

---

## 9. Interface boundaries

| Interface | Purpose | Required boundary |
|---|---|---|
| `/hla/invoke` | Member invokes HLA | Explicit member request; no autonomous action |
| `/hla/drafts` | Private editable drafts | Not sent; external action false |
| `/hla/topics` | Reviewed topic suggestions | No inference or hidden targeting |
| `/hla/topic-boards` | Shared optional topic boards | Active only after each participant opts in |
| `/hla/conversation-startup` | Startup drafting tools | No default chat surveillance |
| `/hla/access-request-drafts` | Private access-request drafting | No diagnosis disclosure |
| `/hla/casual-coordination` | Consent-first coordination support | No scoring, tracking, or certification |
| `/hla/matching-coordination` | Constraint-based discovery explanation | No person-level ranking |
| `/hla/reminders` | Member-requested neutral reminders | No streaks, urgency, or inactivity triggers |
| `/hla/policy-explainer` | Plain-language platform policy | Not legal advice |
| `/hla/consent-explainer` | Plain-language consent explanation | No implied consent |
| `/hla/selected-text-review` | Member-selected text review | No default chat scanning |
| `/hla/decision-history` | Member-readable explanation | No counterpart private data |
| `/hla/appeals` | Correction and appeal requests | Human-review path where required |
| `/hla/community-climate` | Aggregate product-quality signals | No person-level scores |
| `/hla/acknowledgment` | Informed interface notice | Not a liability waiver or access barrier |
| `/hla/interaction-controls` | Member-selected practical controls | No diagnosis/identity inference |
| `/hla/pacing-preferences` | Stage and pace preferences | No readiness inference |
| `/hla/connection-intent-cards` | Current declared intent | Intent is not consent |
| `/hla/relationship-goals-canvas` | Current multidimensional goals | No commitment score |
| `/hla/boundaries` | Yes/no/ask-first/not-discussing | Private by default |
| `/hla/activity-windows` | Broad future activity contexts | No calendar/schedule/route |
| `/hla/matching-styles` | Browsing presentation selection | No counterpart ranking |
| `/hla/shared-activity-discovery` | Constraint-based discovery | No proximity/compatibility ranking |
| `/hla/meeting-options` | Source-labeled public information | No safety/accessibility certification |
| `/hla/transit-information` | Source-labeled transit information | No route tracking/safety assessment |
| `/hla/meeting-plans` | Mutual public-first plan | No origin/route/arrival exchange |
| `/hla/trusted-contact-check-in-plans` | Manual trusted-contact plan | No automatic escalation |
| `/hla/consent-checkpoints` | Capability-scoped consent | Immediate withdrawal propagation |
| `/hla/coordination-drafts` | Private non-sending drafts | No agent negotiation |
| `/hla/reflection-reminders` | Member-requested neutral reminders | No behavioral prompting |

---

## 10. Review questions

1. How can HLA help members clarify whether they want to consider a voluntary
   public-first or remote interaction without pressure, prediction, attendance
   tracking, or expectation that a meeting should occur?
2. How can HLA suggest conversation topics without inference or surveillance?
3. How can Topic Boards remain opt-in and non-coercive?
4. How can HLA serve members who cannot speak without requiring disclosure?
5. How can HLA support members learning conversation skills without diagnosis,
   labeling, or social-value scoring?
6. How can HLA coordinate casual connection without scoring or certification?
7. How can rejection safety be preserved without reputation signals?
8. How can silent block, silent restrict, silent mute, and quiet exit work
   across interfaces?
9. How can adult-policy eligibility be enforced without identity surveillance?
10. How can impersonation reports be reviewed without authenticity scoring?
11. How can self-harm statements be handled without diagnosis, default chat
    surveillance, or automatic escalation?
12. How can source-labeled public information remain current without certifying
    safety?
13. How can broad future activity windows avoid schedule leakage?
14. How can member-invoked assistance avoid default chat surveillance?
15. How can coordination drafts remain private and non-sending?
16. How can interaction controls avoid diagnosis inference?
17. How can anti-discrimination audits avoid protected-class profiling?
18. How can Community Climate Signals remain aggregate and thresholded?
19. How can research remain opt-in, revocable, and data-minimized?
20. How can universal access remain non-extractive and non-commercial?
21. How can HLA defend members without teaching bad actors how to evade
    protections?
22. How can HLA teach conversation skills without diagnosing or labeling?
23. How can HLA support first-time casual-connection exploration without
    pressure?
24. How can group conversation remain accessible and non-ranking?
25. How can HLA remain free and non-commercial while preserving service quality?

---

## 10. Roadmap

### Phase 0

- Informed acknowledgment.
- Adult-policy eligibility.
- Consent Core.
- Boundary Ledger.
- Block, mute, restrict, report, correction, appeal, and quiet exit.
- Accessibility baseline.
- Revocation propagation.

### Phase 1

- Interaction and Comfort Controls.
- Pacing Preferences.
- Current Connection Intent Cards.
- Relationship Goals Canvas.
- Identity Disclosure Ladder.
- Rejection-safe defaults.
- Conversation Startup Drafting.

### Phase 2

- Shared Activity Discovery.
- Broad Future Activity Windows.
- Transparent rotation.
- Pause Discovery.
- Reciprocal Interest Window.
- Member-visible, context-triggered consent checkpoints.
- Suggestion-only Topic Boards.

### Phase 3

- Public Meeting Options Index.
- Transit Information Planner.
- Public-First Meeting Plan.
- Trusted-Contact Check-In Plan.
- Quiet Exit Guide.
- Access-Request Drafts.

### Phase 4

- Community Prompt Library.
- Virtual Gathering Contexts.
- Member-Invoked Conversation Assistance.
- Human-Directed Coordination Drafts.
- Reflection Reminders.
- Casual-Connection Coordination.

### Phase 5

- Decision Fairness Audit Trail.
- Aggregate Community Climate Signals.
- Formal consent and revocation invariants.
- Adversarial privacy testing.
- Accessibility parity verification.
- Source review and maintenance cadence.

---

## 11. Disclaimer

This is a design specification. It does not provide legal, medical, emergency,
therapeutic, safety-certification, accessibility-certification, or professional
advice.

HLA is intended for adults 30+ under its product policy. It prohibits minors,
coercion, harassment, stalking, doxxing, forced disclosure, non-consensual
activity, value-for-intimacy arrangements, discriminatory profiling, entrapment,
platform-initiated deception, and dark patterns.

Every member retains the right to pause, decline, withdraw, block, report,
delete, request correction, appeal, and leave without explanation.

HLA is member-invoked, non-autonomous, non-scoring, non-inferential,
non-surveilling, consent-first, accessible, free, and non-commercial.
