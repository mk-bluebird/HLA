# Public Resources, Optional Identity, and Fairness Review Tests

## Public Resources Library

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| PRL-001 | Member is not signed in | Browse public resource entry | Public path works where technically feasible |
| PRL-002 | Request includes member need field | Create resource lookup | Rejected |
| PRL-003 | Request includes eligibility prediction | Create resource lookup | Rejected |
| PRL-004 | Resource source is stale | Render entry | Marked outdated; no availability claim |
| PRL-005 | Member opens source URL | Inspect event logs | No member-level click record retained |
| PRL-006 | Resource entry has no accessibility information | Render entry | Shows information unknown or source-linked |
| PRL-007 | System attempts resource recommendation from profile | Policy check | Denied |
| PRL-008 | System attempts to share member information with provider | Policy check | Denied |

## Optional Identity and Discovery Preference Schema

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| OID-001 | Member leaves all identity facets blank | Uses discovery | Full core access remains available |
| OID-002 | Member self-states an identity facet | Read own profile | Member can view/edit/delete own field |
| OID-003 | Sensitive facet is created | Validate default visibility | Private only |
| OID-004 | Member withdraws identity facet | Counterpart refreshes view | Facet unavailable immediately |
| OID-005 | Request includes confidence or authenticity field | Schema validation | Rejected |
| OID-006 | System uses identity facet in ranking | Policy evaluation | Denied |
| OID-007 | Member creates species/other self-expression facet | Profile management | Accepted as self-stated optional text; no verification |
| OID-008 | Counterpart asks why facet is missing | Profile API | No omission/withdrawal metadata returned |
| OID-009 | Casual connection flow begins | Identity disclosure check | No identity facet required |

## Fairness Data-Path and Outcome Review

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| FDP-001 | Discovery query requests only allowlisted fields | Evaluate manifest | Allowed |
| FDP-002 | Discovery query requests race | Evaluate manifest | Rejected |
| FDP-003 | Discovery query requests location proxy | Evaluate manifest | Rejected |
| FDP-004 | Discovery query requests response speed | Evaluate manifest | Rejected |
| FDP-005 | Policy version changes | Run review | Policy-change review event required |
| FDP-006 | Member requests decision explanation | Decision history | Plain-language reason and appeal route available |
| FDP-007 | Review requires voluntary demographic study | Study setup | Separate ethics/privacy protocol required |
| FDP-008 | Small-cell demographic result proposed | Research publication check | Suppressed or rejected |
| FDP-009 | Fairness result attempts person-level rank adjustment | Policy enforcement | Rejected |
| FDP-010 | Accessibility path differs after feature change | Review event | Requires remediation or release block |
