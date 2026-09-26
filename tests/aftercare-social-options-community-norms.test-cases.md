# Aftercare, Low-Pressure Social Options, and Community Norms Tests

## Optional Aftercare and Decompression

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| ACD-001 | Member has no aftercare session | Uses interaction features | No automatic aftercare opens |
| ACD-002 | Member opens aftercare manually | Create session | Private member-initiated session created |
| ACD-003 | Request includes counterpart reference | Validate session | Rejected |
| ACD-004 | Request includes encounter or intimacy status | Validate session | Rejected |
| ACD-005 | Member selects boundary review | Use tool | Private review available |
| ACD-006 | Member deletes session | Read session | Unavailable after deletion |
| ACD-007 | System tries to use reflection in matching | Policy check | Rejected |
| ACD-008 | System tries to generate counterpart rating | Policy check | Rejected |

## Low-Pressure Social Options Planner

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| LPS-001 | Member enters only format and pace | Create session | Optional suggestions may be generated |
| LPS-002 | Request includes stress or loneliness | Validate session | Rejected |
| LPS-003 | Request includes health or recovery field | Validate session | Rejected |
| LPS-004 | Planner tries to suggest a person | Policy check | Rejected |
| LPS-005 | Planner output claims wellbeing benefit | Policy check | Rejected |
| LPS-006 | Member dismisses suggestion | Future planning | No penalty or repeat pressure |
| LPS-007 | Member selects no notifications | Delivery system | No notification is sent |
| LPS-008 | Planner session expires | Downstream use | Inputs excluded from matching, advertising, and model training |

## Rights-Bounded Community Norms Co-Creation

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| RBN-001 | Norm uses permitted discussion format category | Rights-floor review | May proceed to member comment |
| RBN-002 | Norm requires camera | Schema/policy validation | Rejected |
| RBN-003 | Norm requires identity disclosure | Schema/policy validation | Rejected |
| RBN-004 | Norm restricts block/report/appeal | Schema/policy validation | Rejected |
| RBN-005 | Norm creates reputation points | Schema/policy validation | Rejected |
| RBN-006 | Norm links gifts to intimacy | Schema/policy validation | Rejected |
| RBN-007 | Member does not vote or comment | Community context | No penalty or access reduction |
| RBN-008 | Member appeals active norm | Workflow | Appeal route available |
| RBN-009 | Norm review date expires | Runtime check | Norm becomes expired or requires review |
| RBN-010 | Lower-layer norm conflicts with rights floor | Policy evaluation | Rights floor prevails |
