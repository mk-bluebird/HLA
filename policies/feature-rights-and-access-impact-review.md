# Feature Rights and Access Impact Review

## Purpose

The Feature Rights and Access Impact Review evaluates whether a proposed
Hearthline feature preserves autonomy, consent, privacy, accessibility,
non-discrimination, non-commercial integrity, correction, appeal, and meaningful
exit.

It is a feature-level release review. It is not a numeric equality score,
member-level fairness score, demographic profile, protected-class inference
system, or behavioral-ranking system.

## Required review dimensions

Every feature review must assess:

1. Access
   - Does the feature work without camera, microphone, voice, headset, gesture,
     travel, driving, precise location, legal-name disclosure, or paid access?
   - Does a text-first path provide core equivalence?

2. Autonomy
   - Can members pause, revise, withdraw, decline, hide, mute, block, report,
     delete, and leave without explanation or penalty?
   - Does the feature avoid urgency, scarcity, streaks, guilt, or engagement
     pressure?

3. Consent
   - Are requested capabilities specific, recipient-scoped, purpose-scoped,
     time-limited, and revocable?
   - Does the design reject implied consent?

4. Privacy
   - Is only the minimum necessary data collected?
   - Are purpose, audience, retention, deletion, correction, and access control
     documented?
   - Are sensitive data excluded from advertising, default model training, and
     transferable assets?

5. Non-discrimination
   - Are protected traits, sensitive traits, and proxy fields excluded from
     matching, allocation, moderation, pricing, and access?
   - Can the system demonstrate this through field manifests, schemas, query
     plans, and policy tests?

6. Accessibility
   - Are keyboard, screen-reader, text-first, reduced-motion, low-bandwidth,
     asynchronous, no-camera, no-microphone, and no-voice paths tested?

7. Safety and exit
   - Are block, mute, restrict, report, correction, human review, appeal, and
     quiet exit available and understandable?
   - Does the feature avoid claims of safety certification or emergency response?

8. Non-commercial integrity
   - Does the feature prevent social or sexual access from being linked to money,
     housing, employment, transportation dependency, debt relief, drugs, gifts,
     or basic needs?

9. Accountability
   - Are policy version, reason codes, human-review route, retention, and
     material-change review defined?

## Permitted evidence

A review may use:

- Schema and API-boundary tests.
- Allowed-field and forbidden-field manifests.
- Query-plan inspection.
- Synthetic and opt-in usability testing.
- Accessibility test results.
- Consent and revocation tests.
- Deletion-propagation tests.
- Source-labeled policy evidence.
- Aggregate, thresholded, delayed product-clarity signals.
- Human-review and appeal-path testing.

## Prohibited evidence

Do not use:

- Inferred protected identity.
- Demographic profiling by default.
- Person-level equality or fairness score.
- Engagement, time spent, message volume, reply rate, date count, intimacy,
  disclosure, purchase, or profile-completion metrics as success measures.
- Private messages, location, venue attendance, social graph, or health/recovery
  data unless a separately approved, member-consented process specifically
  requires minimal material for a report or appeal.

## Outcomes

A review outcome must be one of:

- approved_with_safeguards
- requires_remediation
- blocked_from_release
- requires_policy_or_legal_review
