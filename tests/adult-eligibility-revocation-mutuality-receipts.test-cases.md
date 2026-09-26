# Adult Eligibility, Consent Revocation, and Scoped Mutuality Receipt Tests

## Privacy-Minimized Adult Membership Eligibility

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| AME-001 | Current result is eligible | Request adult-only discovery | Adult-only feature can proceed subject to other policy checks |
| AME-002 | Result is expired | Request adult-only discovery | Denied until eligibility is refreshed |
| AME-003 | Connection service receives eligibility projection | Inspect service input | Contains boolean eligibility only |
| AME-004 | Request contains date of birth | Validate connection input | Rejected |
| AME-005 | Request contains exact age | Validate connection input | Rejected |
| AME-006 | Counterpart reads profile | Inspect response | No eligibility method, exact age, DOB, or review history |
| AME-007 | Member chooses not to show age range | Uses discovery | Full access; no incompleteness marker |
| AME-008 | System attempts age-based ranking | Allocation test | Rejected |

## Consent Revocation Propagation Protocol

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| CRP-001 | Active media grant exists | Grantor withdraws | New media access denied immediately |
| CRP-002 | Queued media delivery exists | Grantor withdraws before send | Queue rejects or cancels delivery |
| CRP-003 | Signed venue-detail link is active | Grantor withdraws venue grant | Link fails immediately |
| CRP-004 | Active call session exists | Grantor withdraws call capability | Capability ends or is disabled immediately |
| CRP-005 | Cached authorization says active | Central current-grant lookup says withdrawn | Sensitive action denied |
| CRP-006 | Block is applied | Existing recipient grants exist | All relevant capabilities denied |
| CRP-007 | Media grant withdrawn | Direct conversation grant remains active | Conversation remains available unless separately withdrawn |
| CRP-008 | Revocation consumer unavailable | Sensitive action requested | Fail closed |
| CRP-009 | Revocation event contains message text | Schema validation | Rejected |
| CRP-010 | Revocation event is processed twice | Consumer behavior | Idempotent invalidation |

## Scoped Mutuality Receipt Portability

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| SMR-001 | Adult-policy predicate is current | Issue internal receipt | Short-lived, scope-bound receipt created |
| SMR-002 | Request asks for safety predicate | Issue receipt | Rejected |
| SMR-003 | Request asks for reputation predicate | Issue receipt | Rejected |
| SMR-004 | Consent receipt is withdrawn | Verify receipt | Returns withdrawn immediately |
| SMR-005 | Receipt is used in discovery ranking | Policy check | Denied |
| SMR-006 | Receipt contains public DID | Schema validation | Rejected |
| SMR-007 | Member chooses no credential path | Uses equivalent feature | Full access retained |
| SMR-008 | External presentation is requested | Policy check | Denied in initial intra-Hearthline implementation |
| SMR-009 | Receipt expires | Verification | Returns expired |
| SMR-010 | Receipt is presented to wrong recipient scope | Verification | Denied |
