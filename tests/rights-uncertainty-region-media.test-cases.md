# Rights Review, Structural Uncertainty, Broad Region, and Media Tests

## Feature Rights and Access Impact Review

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| RAR-001 | New feature is proposed | Start review | All required dimensions are present |
| RAR-002 | Review includes numerical equality score | Validate schema | Rejected |
| RAR-003 | Feature requires camera | Review outcome | Requires remediation or blocks release |
| RAR-004 | Feature lacks withdrawal path | Review outcome | Blocked from release |
| RAR-005 | Feature uses engagement as success metric | Review outcome | Requires remediation |
| RAR-006 | Feature connects value to intimacy | Review outcome | Blocked from release |
| RAR-007 | Feature passes rights/access evidence | Review outcome | Approved with documented safeguards |

## Structural Uncertainty Limits

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| SUL-001 | Intent is absent | Evaluate discovery | Field excluded; no inference |
| SUL-002 | Boundary is absent | Request escalation | Separate checkpoint required |
| SUL-003 | Region is absent | Browse discovery | Remote/self-directed option remains available |
| SUL-004 | Consent expired | Attempt sensitive action | Denied |
| SUL-005 | Request includes probability/confidence | Validate decision | Rejected |
| SUL-006 | Policy context is unclear | Evaluate operation | Routed to review or denied |
| SUL-007 | Private message submitted for matching embedding | Boundary test | Rejected |

## Broad Region Selection

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| BRS-001 | Member selects broad region manually | Create selection | Accepted |
| BRS-002 | Request includes latitude/longitude | Validate schema | Rejected |
| BRS-003 | Request includes GeoHash | Validate schema | Rejected |
| BRS-004 | Request includes radius or distance | Validate schema | Rejected |
| BRS-005 | Member selects remote | Create selection | Accepted without location permission |
| BRS-006 | Members choose same broad region | Eligibility check | Broad-context condition may pass; no distance calculated |
| BRS-007 | Named venue disclosure expires | Discovery input check | Venue never becomes future location history |

## Capability-Scoped Media Sharing

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| CMS-001 | Sender lacks active scoped grant | Create share | Denied |
| CMS-002 | Recipient has not granted view permission | Access media | Denied |
| CMS-003 | Share is active and unexpired | Recipient requests access | Current grant and scope checked |
| CMS-004 | Sender withdraws share | Recipient refreshes access | Denied immediately |
| CMS-005 | Share expires | Recipient requests access | Denied |
| CMS-006 | Request includes EXIF location | Validate schema | Rejected |
| CMS-007 | Request includes “no forwarding guarantee” | Validate schema | Rejected |
| CMS-008 | Sender declines watermark | Create share | No-watermark path remains available |
| CMS-009 | Media is used for matching or ranking | Policy check | Rejected |
| CMS-010 | Recipient is blocked | Access request | Denied |
