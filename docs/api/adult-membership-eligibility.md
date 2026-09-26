# Adult Membership Eligibility API

## GET /adult-policy/eligibility/me

Returns the current member's own product-policy eligibility result.

### Response

```json
{
  "minimumAgePolicy": 30,
  "state": "eligible",
  "reviewBy": "2027-09-26T00:00:00Z",
  "policyVersion": "adult-membership-policy-v1"
}
```

## Internal capability projection

Internal connection services may receive only:

```json
{
  "memberRef": "member_opaque_reference",
  "adultPlatformEligible": true,
  "policyVersion": "adult-membership-policy-v1"
}
```

## Prohibited endpoints

The system must not provide:

```text
GET /verify/age/{memberId}
GET /profiles/{memberId}/age-verification
GET /profiles/{memberId}/exact-age
GET /profiles/{memberId}/verification-method
```

## Required feature flag

```text
minimal-adult-eligibility-disclosure = true
```

This flag is a permanent privacy boundary, not a user experiment.
