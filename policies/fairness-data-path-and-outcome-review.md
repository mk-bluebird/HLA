# Fairness Data-Path and Outcome Review Policy

## Purpose

This policy verifies that Hearthline product decisions do not use protected
identity, identity proxies, or sensitive traits in prohibited ways and that
members can seek correction or appeal when they believe a system decision was
unfair or incorrect.

It is not a racial or ethnic profiling system, demographic ranking system,
identity-inference system, or automated fairness verdict.

## Structural review requirements

For every discovery, moderation, access, allocation, and recommendation system,
Hearthline must verify:

- A versioned allowed-field manifest exists.
- A versioned forbidden-field manifest exists.
- Protected identity is excluded from ordinary decision inputs.
- Proxy fields are excluded where they could recreate prohibited identity
  inference or unequal treatment.
- Queries and service contracts reject forbidden fields.
- Explanations identify the permitted basis for a decision.
- Policy changes receive privacy, accessibility, safety, and fairness review.
- Members have correction, human-review, and appeal routes where a material
  platform decision affects them.

## Protected and proxy field boundaries

Ordinary operational decision systems must not use:

- Race, ethnicity, nationality, tribal affiliation, religion, gender identity,
  sexuality, disability, health, recovery, financial status, housing,
  employment, immigration, or legal history.
- Inferred identity from name, image, voice, language style, location, school,
  employer, neighborhood, device, social graph, or behavior.
- Exact or approximate location as a proxy for protected identity.
- Profile-view, response-rate, message-sentiment, photo-quality, engagement,
  time-spent, or social-network signals as ranking inputs.

## Voluntary research exception

Optional demographic research may occur only through a separately approved
study protocol. Study data must be:

- Isolated from operational systems.
- Pseudonymized where feasible.
- Aggregated and thresholded before reporting.
- Retention-limited.
- Not used for matching, moderation, advertising, access control, or personal
  scoring.
- Subject to withdrawal and deletion rules explained at consent time.

## Outcome review

When reviewing an outcome concern, prefer:

1. Input-path and policy-version inspection.
2. Schema and query-plan inspection.
3. Explanation and appeal evidence.
4. Accessibility-path testing.
5. Synthetic test cases.
6. Voluntary, approved research only if necessary.

The system must not infer a member's race or ethnicity to complete an audit.

## Prohibited outputs

Do not create:

- Race score.
- Ethnicity score.
- Protected-class confidence score.
- Demographic risk score.
- Demographic desirability score.
- Demographic compatibility score.
- Demographic safety score.
- Demographic moderation probability.
- “Fairness-adjusted” person ranking.
- Group-based visibility quota applied to individuals.
