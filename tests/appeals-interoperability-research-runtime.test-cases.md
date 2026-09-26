# Appeals, Interoperability, Research, and Runtime Tests

## Adult-Context Moderation Appeals

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| AMA-001 | Material decision exists | Member creates appeal | Appeal enters submitted state |
| AMA-002 | Appeal is submitted | Receipt confirmation | Member receives acknowledgment |
| AMA-003 | Appeal requests text-first accommodation | Workflow | Accommodation is recorded |
| AMA-004 | Appeal payload includes reporter identity | Schema validation | Rejected |
| AMA-005 | Appeal payload includes private counterpart data | Schema validation | Rejected |
| AMA-006 | Material restriction is final | Review workflow | Human review required unless documented temporary measure |
| AMA-007 | Member appeals adult consensual content decision | Review | Evaluated under policy, not categorically denied |
| AMA-008 | Appeal history enters discovery pipeline | Policy test | Rejected |

## Member-Controlled Interoperability

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| MCI-001 | Member requests own preferences export | Export preview | Only selected member-owned fields included |
| MCI-002 | Export asks for counterpart messages | Scope validation | Rejected |
| MCI-003 | Import includes contact graph | Scope validation | Rejected |
| MCI-004 | OAuth scope includes automatic posting | Scope validation | Rejected |
| MCI-005 | Member revokes OAuth connection | Runtime | Token invalidated and future access denied |
| MCI-006 | Connection requests portable reputation | Scope validation | Rejected |
| MCI-007 | Member uses no external account | Core access | Full function remains available |
| MCI-008 | Export is generated | Accessibility review | Accessible documented format available |

## Opt-In Research Access Program

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| ORA-001 | Study lacks review references | Open for opt-in | Rejected |
| ORA-002 | Study requests private messages | Schema validation | Rejected |
| ORA-003 | Study requests aggregate usability feedback | Review | May proceed with opt-in |
| ORA-004 | Member declines study | Core access | No penalty |
| ORA-005 | Member withdraws | Future collection | Stops immediately |
| ORA-006 | Research dataset enters matching service | Policy test | Rejected |
| ORA-007 | Aggregate release has small cell | Release review | Suppressed or rejected |
| ORA-008 | Study retention expires | Destruction job | Dataset removed per policy |

## Component Composition Runtime

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| CCR-001 | Contract has allowed/forbidden fields | Submit for review | Accepted for review |
| CCR-002 | Contract lacks retention class | Submit | Rejected |
| CCR-003 | Contract allows raw messages | Submit | Rejected |
| CCR-004 | Contract allows exact location | Submit | Rejected |
| CCR-005 | Contract has unknown consumer | Runtime access | Denied |
| CCR-006 | Consent revoked | Sensitive consumer access | Denied; caches/tokens invalidated |
| CCR-007 | Contract expires or is revoked | Consumer call | Fail closed |
| CCR-008 | Research component requests operational profile | Contract validation | Rejected |
