# Discovery Allocation, Pause Discovery, and Intent Filter Tests

## Discovery Presentation Allocator

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| DPA-001 | Eligible options exist | Browse chronological mode | Results appear without a person score |
| DPA-002 | Request includes compatibility weight | Validate request | Rejected |
| DPA-003 | Request includes latitude, longitude, or distance | Validate request | Rejected |
| DPA-004 | Request includes safety or risk score | Validate request | Rejected |
| DPA-005 | Member chooses activity category | Browse options | Results organized by chosen category only |
| DPA-006 | Member hides an option | Refresh allocation | Hidden option does not resurface |
| DPA-007 | Member selects one-at-a-time mode | Browse options | No reduced core access or hidden rank |
| DPA-008 | Candidate has fewer photos or slower replies | Allocation test | No presentation penalty |

## Pause Discovery

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| PD-001 | Member has active discovery request | Enable pause | Request leaves candidate generation immediately |
| PD-002 | Counterpart searches discovery options | Member is paused | No pause-state explanation or presence signal exposed |
| PD-003 | Member resumes discovery | Candidate generation refreshes | New eligibility resumes without visibility penalty |
| PD-004 | Request includes `onlineStatus` | Schema validation | Rejected |
| PD-005 | Request includes profile-view history | Schema validation | Rejected |
| PD-006 | Member pauses discovery | Existing conversation | Conversation remains governed by separate controls |
| PD-007 | Member blocks counterpart while paused | Access attempt | Block overrides all relevant interactions |
| PD-008 | System uses pause duration in ranking | Policy test | Rejected |

## Current Connection Intent Filter

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| CIF-001 | Member selects casual connection | View intent card | Reminder states it is not consent |
| CIF-002 | Two members share friendship | Filter options | Shared context may be shown; no direct message opens |
| CIF-003 | Two members differ on relationship exploration | Context response | Difference is factual; no score or penalty |
| CIF-004 | One member selects “not discussing intimacy” | Counterpart attempts sexual-topic escalation | Separate consent checkpoint required |
| CIF-005 | Request includes `casualOnly` | Schema validation | Rejected |
| CIF-006 | Request includes relationship score | Schema validation | Rejected |
| CIF-007 | Member changes intent | Future discovery context | Updated immediately; no reputation or risk effect |
| CIF-008 | Member chooses “please do not ask again” | Counterpart sends repeated escalation | Member can block/report; no automated inference required |
