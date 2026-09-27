# Invariant: No Standing Operator Plaintext Access

**Invariant ID:** `INV-NO-STANDING-OPERATOR-PLAINTEXT-ACCESS`  
**Policy version:** `operator-access-v1`  
**Status:** approved  
**Owner:** Security Engineering  
**Privacy reviewer:** Privacy Review  
**Accessibility reviewer:** Accessibility Review  
**Safety reviewer:** Safety Review  
**Next review date:** 2027-03-26  
**Executable tests:** `tests/adversarial/no-standing-operator-plaintext-access.test.ts`

## Statement

No operator has standing unilateral plaintext access to sensitive member data.

Exceptional access must be purpose-limited, time-bounded, independently
authorized, logged, reviewable, and protected through separation of duties.

## Sensitive data scope

- Private messages selected for a report/appeal.
- Sensitive media.
- Identity claims.
- Access controls.
- Consent and boundary context.
- Venue and meeting-plan details.
- Trusted-contact details.
- Reports, appeals, and moderation evidence.
- Sensitive local recovery/export material where applicable.

## Required controls

- Encryption at rest.
- Keys governed separately from ordinary database access.
- No default database role with plaintext decryption capability.
- No raw sensitive data in application logs, analytics, or debug tooling.
- Break-glass access requires:
  - named purpose;
  - case or incident reference;
  - time-bounded grant;
  - independent approval;
  - minimum data scope;
  - immutable access log;
  - post-access review;
  - automatic expiry.
- Backup and restore pathways are included in the access-control model.
- Client-side/local-first protection is preferred where feasible.
- Deployment, observability, and key-policy changes require review.

## Limitations

This invariant reduces privileged-access risk. It does not claim that every
operator threat, endpoint compromise, malicious deployment, client compromise,
or legal process risk is eliminated.

## Release-gate tests

- Ordinary operator role cannot decrypt sensitive record.
- Database dump contains ciphertext for protected fields.
- Application logs reject protected plaintext.
- Break-glass requires multi-party approval.
- Break-glass grant expires automatically.
- Break-glass access produces immutable audit evidence.
- Backup restore does not create standing plaintext access.
- Key-policy change requires reviewed deployment path.
