# AnchorPass Passkey Authentication Test Cases

## Passkey registration

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| AP-001 | Signed-in member | Requests passkey registration options | Server issues short-lived, one-time registration challenge |
| AP-002 | Client submits expired challenge | Verify registration | Rejected |
| AP-003 | Client submits challenge for wrong origin | Verify registration | Rejected |
| AP-004 | Client submits wrong RP ID | Verify registration | Rejected |
| AP-005 | Client submits registration without user verification | Verify registration | Rejected |
| AP-006 | Registration succeeds | Store credential | Public key and credential reference stored; no private key or biometric data stored |
| AP-007 | Registration request contains face template | Schema validation | Rejected |
| AP-008 | Registration request contains device PIN/pattern | Schema validation | Rejected |

## Passkey authentication

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| AP-101 | Valid active credential and challenge | Verify assertion | Short-lived session issued |
| AP-102 | Assertion challenge reused | Verify assertion | Rejected |
| AP-103 | Assertion has wrong origin | Verify assertion | Rejected |
| AP-104 | Assertion has wrong RP ID | Verify assertion | Rejected |
| AP-105 | Assertion lacks required user verification | Verify assertion | Rejected |
| AP-106 | Credential is revoked | Verify assertion | Rejected |
| AP-107 | Credential is mapped to another account | Verify assertion | Rejected |
| AP-108 | Stale signature counter violates policy | Verify assertion | Rejected or routed to recovery/security review |
| AP-109 | Login event enters discovery pipeline | Contract test | Rejected |
| AP-110 | Device type enters matching/ranking | Contract test | Rejected |

## Adult-policy separation

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| AGE-001 | Signed-in eligible member | Request adult-only feature projection | Returns `adultPlatformEligible: true` only |
| AGE-002 | Discovery service requests DOB | Contract validation | Rejected |
| AGE-003 | Discovery service requests exact age | Contract validation | Rejected |
| AGE-004 | Counterpart profile query | Inspect response | No eligibility state, DOB, age, verification method, or device data |
| AGE-005 | Eligibility result expired | Adult-only feature request | Denied or routed to review without exposing cause to counterpart |
| AGE-006 | Member uses non-Apple/non-Google/non-Microsoft device | Sign-in/recovery | Security-key or accessible alternative path remains available |

## Session and recovery

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| SES-001 | Active session | Member revokes credential | Session becomes revoked |
| SES-002 | Active session | Member signs out all sessions | Other sessions become revoked |
| SES-003 | Sensitive account action | Attempt without fresh reauthentication | Rejected |
| SES-004 | Block or consent withdrawal | Invoke action | Available without unnecessary reauthentication |
| SES-005 | Recovery request | No camera/biometric available | Accessible support/recovery path exists |
| SES-006 | Recovery state | Discovery allocation | No ranking, visibility, or trust effect |
