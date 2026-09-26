# Safety Support Actions, Minimal-Claim Credentials, and Non-Stigma Review Tests

## Safety Support Action Menu

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| SSA-001 | Member opens support menu | View actions | Member sees leave, block, report, trusted-contact, and device options |
| SSA-002 | Request includes severity score | Schema validation | Rejected |
| SSA-003 | Request includes automatic authority contact | Schema validation | Rejected |
| SSA-004 | Member selects trusted-contact message | Send flow | Exact contact and message shown; explicit confirmation required |
| SSA-005 | Delivery provider accepts message | Result shown | “Submitted” only; no claim of recipient awareness |
| SSA-006 | Member blocks counterpart | Counterpart access attempt | Future platform contact denied |
| SSA-007 | Member reports selected content | Report flow | Member reviews before submission |
| SSA-008 | System attempts to use safety action in matching | Policy check | Rejected |

## Optional Minimal-Claim Credential Presentation

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| MCC-001 | Member does not present credential | Uses ordinary discovery | Full core access remains available |
| MCC-002 | Member presents adult-policy predicate | Verify scoped claim | Returns current predicate only |
| MCC-003 | Request contains legal name or DOB | Schema validation | Rejected |
| MCC-004 | Request asks for safety or trust claim | Schema validation | Rejected |
| MCC-005 | Credential expires | Verification | Returns expired |
| MCC-006 | Member withdraws presentation | Verification | Returns withdrawn immediately |
| MCC-007 | Credential used in ranking | Policy check | Rejected |
| MCC-008 | Public DID or wallet required | Feature test | Rejected; non-cryptographic path available |

## Non-Stigma Connection Design Review

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| NSR-001 | New casual-connection feature proposed | Review starts | Consent, privacy, accessibility, and exit dimensions required |
| NSR-002 | Review payload contains person reference | Schema validation | Rejected |
| NSR-003 | Review payload contains community acceptance score | Schema validation | Rejected |
| NSR-004 | Feature uses “upgrade to serious” language | Review outcome | Requires remediation |
| NSR-005 | Feature links money to intimacy | Review outcome | Blocked from release |
| NSR-006 | Feature lacks no-camera path | Review outcome | Requires remediation or release block |
| NSR-007 | Feature lacks withdrawal path | Review outcome | Blocked from release |
| NSR-008 | Feature passes all dimensions | Review outcome | Approved with documented safeguards |
