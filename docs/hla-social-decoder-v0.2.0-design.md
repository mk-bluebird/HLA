# HLA Social-Decoder

**Version:** 0.2.0-design  
**Status:** Design specification; not production code  
**Audience:** Adults 30+, builders, accessibility reviewers, safety reviewers,
policy reviewers, moderators, and researchers  
**Scope:** Member-declared connection intent, interaction preferences, pacing,
matching presentation styles, free public-first services, and source-labeled
public-place or remote interaction options  
**Legal notice:** Jurisdiction-specific review is required before deployment.
Arizona-oriented policy review may apply. This document does not provide legal,
medical, emergency, therapeutic, safety-certification, or professional advice.

> The Social-Decoder helps members express and understand their own current
> intent, preferences, pacing, and future activity possibilities, and discover
> mutually declared compatible options. It does not determine another person's
> motive, intent, future activity, safety, trust, compatibility, desirability,
> availability, or worth.

---

## 1. Purpose

The Social-Decoder is a consent-first HLA component for adults 30+ who want
friendship, dating, romance, community, shared activities, or consensual adult
intimacy at their own pace.

It supports:

- Member-declared current connection intent.
- Member-selected interaction, communication, access, and comfort preferences.
- Member-controlled interaction pacing and stage movement.
- Constraint-based presentation styles without person-level scores.
- Member-declared future activity windows without schedules or calendar access.
- Free, public-first, source-labeled public meeting or remote activity options.
- Consent checkpoints for messaging, calls, media, sexual topics, venue detail,
  meeting plans, recording, and optional AR capabilities.
- Accessibility and accommodation controls without diagnosis or identity
  inference.
- Anti-discrimination, privacy, non-commercial integrity, correction, appeal,
  and quiet-exit controls.

It does not:

- Infer motive, intent, schedule, future activity, emotion, vulnerability,
  health, recovery, disability, finances, housing, employment, or identity.
- Track location, routes, attendance, device proximity, Wi-Fi/Bluetooth, or
  nearby status.
- Certify venues, people, routes, plans, or encounters as safe, trusted,
  verified, accessible, available, suitable, or appropriate.
- Create safety, trust, risk, reputation, popularity, desirability,
  compatibility, influence, or social-acceptance scores.
- Monitor all private chats by default.
- Negotiate, message, accept, decline, schedule, or consent autonomously.
- Exchange intimacy, attention, time, location, or disclosure for money,
  housing, employment, transportation dependency, debt relief, drugs, gifts,
  or basic needs.

---

## 2. Non-negotiable boundaries

The Social-Decoder must reject:

- Motive, intent, or future-activity inference.
- Schedule inference from behavior, device data, account activity, response
  timing, or non-use.
- Location precision, distance, proximity, routes, location history, venue
  attendance, current presence, or nearby status.
- Contact-book matching, social-graph matching, profile-view matching, or
  third-party relationship data.
- Person-level scoring or ranking.
- Safety, trust, accessibility, availability, or suitability certification.
- Reputation portability, public credential graphs, public DIDs, or public
  social credentials.
- Universal AI moderation, default chat surveillance, or default message
  analysis.
- Agent-to-agent social negotiation.
- Commercialized intimacy or value-for-access workflows.
- Forced identity, accessibility, relationship, health, financial, or location
  disclosure.
- Streaks, urgency prompts, guilt loops, inactivity nudges, or engagement
  farming.

---

## 3. Informed acknowledgment

The Social-Decoder acknowledgment is an informed interface notice. It is not a
liability waiver, contract, consent to another member, consent to data uses
outside the selected feature, or waiver of legal rights.

The acknowledgment must not be required for:

- Reading public policies.
- Browsing the Public Resources Library.
- Viewing accessibility information.
- Accessing block, report, deletion, correction, appeal, or quiet-exit tools.
- Closing an account.
- Reading source-labeled public information.
- Using text-first account and privacy controls.

It may be shown before a member activates a feature that processes their
selected connection context for discovery or a reciprocal interaction.

```text
SOCIAL-DECODER ACKNOWLEDGMENT

I understand that:

- HLA does not determine another person's motive, intent, future activity,
  safety, trust, compatibility, desirability, or suitability.
- HLA does not certify any person, venue, route, plan, or encounter as safe,
  trusted, verified, accessible, available, or appropriate.
- HLA does not provide legal, medical, emergency, therapeutic, or safety advice.
- HLA does not track my location, route, attendance, or device proximity.
- HLA does not monitor all my private messages by default.
- HLA does not negotiate, message, accept, decline, schedule, or consent for me.
- Consent is specific, informed, ongoing, and revocable.
- I may pause, withdraw, delete, hide, mute, restrict, block, report, request
  human review, appeal, and leave without explanation.
- I retain all legal rights. This is not a waiver of rights.

I may choose what to share, with whom, for what purpose, and for how long.
```

---

## 4. Interaction and comfort controls

Interaction and comfort controls are member-selected practical preferences. They
are not diagnoses, identity labels, competence judgments, relationship signals,
vulnerability signals, consent grants, or discovery-ranking inputs.

Members may select:

- Text-first.
- Captions requested.
- Keyboard-only path.
- Screen-reader-compatible path.
- Reduced motion.
- No camera.
- No microphone.
- No voice requirement.
- Asynchronous messaging.
- Short-message preference.
- Quiet setting preferred.
- Lower-sensory setting preferred.
- Seated option preferred.
- Step-free public-information preference.
- Public-transit-information preference.
- Independent arrival preferred.
- Independent departure preferred.
- Remote-first interaction.
- Flexible future activity window.
- Low-cost option preference.
- One-at-a-time presentation.
- Low-stimulation presentation.

HLA must not infer disability, neurodivergence, health, recovery, age-related
condition, competence, emotional state, or vulnerability from these controls.

Controls are private by default. A member may disclose one selected control to
one selected counterpart or meeting plan only through recipient-scoped,
purpose-specific, revocable disclosure.

---

## 5. Interaction pacing and stage movement

Interaction pacing and stage movement describe how a member prefers to move
through possible interaction stages. They do not describe physical movement,
travel, routes, attendance, readiness, emotional state, availability, or
obligation.

Possible stages include:

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

Each capability requires fresh, capability-scoped consent. Consent to one stage
does not imply consent to another.

Members control:

- Which interaction possibilities they want to state.
- Whether to move to a later stage.
- Pace of movement between stages.
- Pause, narrowing, revision, withdrawal, and exit.
- Identity, media, venue-detail, and meeting-plan disclosure.
- Trusted-contact plan use.
- Text-first, remote-first, public-first, or asynchronous paths.

HLA must not infer readiness, intent, future activity, emotional state, or
availability from stage movement, response time, message length, message
frequency, silence, or non-response.

---

## 6. Matching presentation styles

Matching presentation styles are transparent methods for organizing already
eligible options. They must not rank people by worth, desirability, safety,
trust, compatibility, popularity, response likelihood, or social value.

Allowed styles:

- **Constraint-based:** Uses only member-selected allowed constraints.
- **Self-directed:** Member browses eligible options directly.
- **Chronological:** Shows eligible options by creation/update order where
  applicable.
- **Transparent rotation:** Rotates eligible options without hidden ranking.
- **Mutual-consent:** Presents only options where both members have current
  discovery consent.
- **Activity-first:** Organizes options by shared selected activity context.
- **Intent-first:** Organizes options by current declared connection intent.
- **Pace-first:** Organizes options by compatible declared pacing and boundary
  context.
- **Remote-first:** Organizes remote interaction options.
- **Public-first:** Organizes public-place or public-information options.
- **Low-stimulation:** Supports one-at-a-time or reduced presentation.
- **Member-organized:** Lets a member choose which permitted browsing category
  appears first in their own interface.

Member-organized presentation may use only categories such as:

- Activity context.
- Current connection intent.
- Text-first context.
- Public-first context.
- Remote-first context.
- Broad future activity window.
- Low-stimulation presentation mode.

Member-organized presentation must not assign weights to people or alter another
member's visibility. It must not use:

- Exact location, distance, routes, attendance, or presence.
- Contacts, social graph, or profile views.
- Private messages, response speed, popularity, or time spent.
- Identity, health, recovery, finances, housing, employment, disability, or
  vulnerability.
- Image quality, biometrics, or inferred emotional state.
- Predicted relationship, intimacy, safety, or response outcome.

---

## 7. Free public-first services

Free public-first services are no-cost, non-commercial, member-controlled tools.
They do not monetize intimacy, attention, time, location, disclosure, or
survival needs.

Possible services include:

- Public Meeting Options Index.
- Member-Directed Transit Information Planner.
- Consent Checkpoint Explainer.
- Member-Invoked Boundary Drafting.
- Member-Invoked Respectful Decline Drafting.
- Member-Invoked Public-First Invitation Drafting.
- Public-First Meeting Plan Template.
- Optional Trusted-Contact Check-In Plan Template.
- Community Conversation Prompt Library.
- Interaction and Comfort Controls Checklist.
- Plain-Language Policy Explainer.
- Report, Correction, and Appeal Path Finder.
- Quiet Exit Guide.
- Privacy Scope Review.
- Retention and Expiry Review.
- Source-Labeled Public Information Review.
- Non-Commercial Integrity Guide.

Every service must state:

```text
HLA does not certify safety, trust, accessibility, affordability, availability,
or suitability. This is not legal, medical, emergency, therapeutic, or
professional advice.
```

Services must not:

- Infer that a member needs a resource.
- Recommend a resource based on profile or behavior.
- Send member information to providers.
- Track resource searches, clicks, reading time, or outcomes at member level.
- Claim that a provider or venue is safe, available, appropriate, open,
  affordable, licensed, or suitable.
- Replace emergency services, healthcare, legal counsel, or case management.

---

## 8. Declared context only

HLA cannot determine another person's motive, intent, future activity, schedule,
availability, safety, trust, or compatibility.

It can only:

- Receive current member-declared connection intent.
- Receive current member-declared relationship goals.
- Receive current member-declared pacing and boundary preferences.
- Receive current member-declared broad future activity windows.
- Receive member-selected broad region or remote-only setting.
- Present mutual overlap when both members have active discovery consent.
- Present source-labeled public-place or remote activity information.
- Present capability-scoped consent checkpoints.
- Provide member-invoked drafting and explanation tools.

Mutual intent overlap is a boolean constraint, not a score:

\[
IntentOverlap(a,b) =
DeclaredIntent(a)
\land
DeclaredIntent(b)
\land
MutualDiscoveryConsent(a,b)
\]

Broad future activity-window overlap is also a boolean constraint:

\[
FutureActivityWindowOverlap(a,b) =
BroadWindow(a) \cap BroadWindow(b) \neq \emptyset
\]

No term may use exact time, calendar data, device state, attendance, location,
route, response timing, or inferred routine.

---

## 9. Conceptual interfaces

| Interface | Purpose | Required boundary |
|---|---|---|
| `/social-decoder/acknowledgment` | Informed interface acknowledgment | Not a liability waiver; no rights waived |
| `/interaction-controls` | Member-selected practical controls | No diagnosis or identity inference |
| `/pacing-preferences` | Stage movement and pace | No readiness or future-activity inference |
| `/connection-intent-cards` | Current declared intent | Intent is not consent |
| `/relationship-goals-canvas` | Multidimensional current goals | No casual-to-committed score |
| `/boundaries` | Yes/no/ask-first/not-discussing | Private by default; no scoring |
| `/activity-windows` | Broad future activity contexts | No calendar, schedule, route, or presence |
| `/matching-styles` | Presentation method selection | No person-level ranking |
| `/shared-activity-discovery` | Constraint-based discovery | No proximity or compatibility ranking |
| `/meeting-options` | Source-labeled public venue information | No safety/accessibility certification |
| `/transit-information` | Source-labeled public transit information | No route tracking or safety assessment |
| `/meeting-plans` | Mutual public-first plan | No origin, route, arrival, or transport exchange |
| `/trusted-contact-check-in-plans` | Manual trusted-contact messages | No automatic emergency escalation |
| `/consent/lifecycles` | Capability-scoped consent | Withdrawal works immediately |
| `/conversation-assistance` | Member-invoked drafting and selected-text review | No default chat surveillance |
| `/coordination-drafts` | Private, editable, non-sending drafts | No agent negotiation or external action |
| `/reflection-reminders` | Member-requested neutral reminders | No streaks or inactivity triggers |
| `/decision-history` | Member-readable decision explanation | No exposure of others' private data |
| `/appeals` | Correction and appeal requests | Human-review path where required |
| `/governance/community-climate` | Aggregate product-quality signals | No person-level influence or reputation |

---

## 10. Logic

### 10.1 Constraint-based eligibility

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

No term may use exact location, distance, route, attendance, contacts, social
graph, private messages, profile views, response speed, popularity, identity,
health, finances, housing, employment, disability, recovery, vulnerability,
image quality, biometrics, or predicted outcome.

### 10.2 Capability-scoped consent state

```text
unknown
requested
discussion_optional
granted
active
paused
declined
withdrawn
expired
completed
blocked_or_reviewable
optional_reflection
closed
```

A consent state belongs to one named capability, one purpose, one recipient or
session scope, and one expiry period.

Withdrawal must propagate immediately to authorization checks, active sessions,
recipient projections, signed links, caches, queues, and future reads.

### 10.3 Presentation allocation

When eligible options exceed display capacity:

- Respect member-selected browsing categories.
- Offer self-directed and chronological browsing.
- Use transparent rotation.
- Prevent repeat presentation after a member hides an option.
- Support one-at-a-time and low-stimulation modes.
- Explain each result in plain language.
- Allow members to show fewer options or pause discovery.
- Never use a person-level rank or score.

---

## 11. Wiring patterns

Possible implementation tools include:

- TypeScript for client flows, API contracts, schemas, and consent UI.
- Rust for privacy boundaries, token verification, expiry, deletion, and WASM.
- Go for narrowly scoped high-throughput services.
- Python for reviewed policy tooling, source maintenance, static analysis, and
  aggregate research tooling.
- PostgreSQL with row-level security for purpose-limited relational state.
- Redis only for short-lived, revocable operational state.
- NATS or equivalent ephemeral event transport for revocation-aware consumers.
- OPA/Rego for policy enforcement.
- JSON Schema for API and object validation.
- GraphQL only for narrowly scoped member-owned data access.
- JSON-LD only for public, non-sensitive context definitions.
- WebCrypto or libsodium/WASM for client-side encryption where feasible.
- IndexedDB for local-first drafts and private reflection.

The initial implementation must not require Kafka, Neo4j, Elasticsearch, vector
databases, PostGIS, DID, verifiable credentials, zero-knowledge proofs, or
calendar integration.

Any future evaluation of these tools requires a narrow documented purpose and
evidence that a simpler, more auditable alternative cannot meet that purpose.

Every module must:

- Expose a narrow service contract.
- State its purpose.
- Define retention, expiry, deletion, and correction behavior.
- Support withdrawal and revocation where relevant.
- Reject forbidden input fields.
- Produce content-minimized accountability events.
- Provide text-first and accessible paths.

---

## 12. Prohibited features

The Social-Decoder must not add:

- Motive inference.
- Intent inference.
- Future-activity or schedule inference.
- Location tracking.
- Proximity matching.
- Route planning or tracking.
- Attendance tracking.
- Contact-graph discovery.
- Social-graph discovery.
- Profile-view matching.
- Reputation.
- Trust scores.
- Safety scores.
- Risk scores.
- Compatibility scores.
- Desirability scores.
- Popularity scores.
- Influence scores.
- Social-acceptance scores.
- Wellness matching.
- Stress-relief matching.
- Universal AI moderation.
- Default chat surveillance.
- Automatic reporting or blocking.
- Agent negotiation.
- Calendar surveillance.
- Engagement farming.
- Streaks, urgency prompts, guilt loops.
- Forced identity or accessibility disclosure.
- Value-for-intimacy workflows.

---

## 13. Questions for review

1. How can members declare intent clearly without pressure?
2. How can HLA present mutual overlap without ranking people?
3. How can future activity windows remain broad enough to avoid schedule leakage?
4. How can public-place suggestions remain source-labeled and non-certifying?
5. How can consent checkpoints be understandable across communication and access
   preferences?
6. How can pacing preferences be revised without penalty?
7. How can non-response be respected without inference?
8. How can quiet exit work without notifying others?
9. How can free services remain non-commercial and non-extractive?
10. How can interaction controls avoid diagnosis inference?
11. How can anti-discrimination audits avoid protected-class profiling?
12. How can community climate signals remain aggregate and thresholded?
13. How can research remain opt-in, revocable, and data-minimized?
14. How can source-labeled public information remain current?
15. How can members review retention and expiry easily?
16. How can appeals and corrections be accessible and timely?
17. How can blocked, restricted, paused, expired, and withdrawn states propagate
    immediately?
18. How can trusted-contact plans remain manual and non-automatic?
19. How can member-invoked assistance avoid default chat surveillance?
20. How can coordination drafts remain private and non-sending?

---

## 14. Roadmap

### Phase 0: Foundations

- Informed acknowledgment.
- Adult-policy eligibility result.
- Consent Core.
- Boundary Ledger.
- Block, mute, restrict, report, correction, appeal, and quiet exit.
- Accessibility baseline.
- Privacy boundary enforcement.
- Retention and deletion framework.

### Phase 1: Member-authored interaction context

- Interaction and Comfort Controls.
- Pacing Preferences.
- Current Connection Intent Cards.
- Relationship Goals Canvas.
- Identity Disclosure Ladder.
- Non-Disclosure Identity Protection Pattern.
- Mutual Activity Windows.

### Phase 2: Constraint-based discovery

- Matching-style preferences.
- Shared Activity Discovery.
- Transparent presentation allocator.
- Broad future activity-window overlap.
- Pause Discovery.
- Reciprocal Interest Window.
- Plain-language result explanations.

### Phase 3: Public-first planning

- Public Meeting Options Index.
- Member-Directed Transit Information Planner.
- Named public-venue disclosure capsule.
- Public-First Meeting Plan.
- Optional Trusted-Contact Check-In Plan.
- Manual trusted-contact messages.

### Phase 4: Community and optional assistance

- Community Conversation Prompt Library.
- Virtual Gathering Contexts.
- Member-Invoked Conversation Assistance.
- Human-Directed Coordination Drafts.
- Member-Requested Reflection Reminders.
- Member-Directed Social Options Planner.

### Phase 5: Assurance

- Decision Fairness Audit Trail.
- Aggregate Community Climate Signals.
- Formal consent and revocation invariants.
- Adversarial privacy testing.
- Accessibility parity verification.
- Source maintenance and review cadence.

---

## 15. Disclaimer

This is a design specification. It does not provide legal, medical, emergency,
therapeutic, safety-certification, accessibility-certification, or professional
advice.

HLA is intended for adults 30+ under its product policy. It prohibits minors,
coercion, harassment, stalking, doxxing, forced disclosure, non-consensual
activity, value-for-intimacy arrangements, discriminatory profiling, and dark
patterns.

Every member retains the right to pause, decline, withdraw, block, report,
delete, request correction, appeal, and leave without explanation.
