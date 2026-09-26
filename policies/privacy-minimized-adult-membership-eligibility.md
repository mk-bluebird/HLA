# Privacy-Minimized Adult Membership Eligibility Policy

## Purpose

Hearthline applies a 30+ adult-membership policy while collecting and exposing
the minimum information necessary to enforce that product boundary.

The policy does not rank people by age, assess maturity, establish legal
capacity, determine identity, or create a public age label.

## Eligibility result

The connection system may receive only:

```text
minimum_age_policy = 30
state = eligible | not_eligible | review_required | expired
assessed_at
review_by
policy_version
```

The connection system must not receive date of birth, exact age, identity
document, legal name, address, issuer metadata, credential subject identifier,
or age-verification method.

## No ageism requirements

Hearthline must not:

- Rank members by exact age or age range.
- Prefer younger or older members in discovery allocation.
- Infer life stage, health, fertility, employment, relationship status, maturity,
  desirability, or safety from age.
- Require a public age display.
- Treat a member's decision not to share an age range as incomplete or suspicious.
- Use age-policy verification method as a trust, safety, risk, reputation, or
  desirability signal.
- Use age in advertising, behavioral profiling, model training, or external
  data-sharing.
- Use the 30+ policy result to evaluate people outside the narrow membership
  purpose.

## Verification options

Members must have access to at least one pathway that does not require:

- Camera access.
- Face scan.
- Voice sample.
- Biometric collection.
- Public social account.
- Public DID.
- Blockchain wallet.
- Location sharing.
- Employer, school, relationship, health, financial, or recovery disclosure.

## Expiry and review

- An eligibility result may expire under a documented policy.
- Expiry fails closed for adult-only feature access.
- A member must have an accessible review or support path.
- Other members must not be told why access is unavailable.
- Eligibility review must not create a public or counterpart-facing status.

## Member-facing text

> Hearthline is for adults 30 and older under its membership policy. We keep the
> connection system limited to the minimum eligibility result needed for that
> rule. Your date of birth, exact age, and verification details are not shown to
> other members.
