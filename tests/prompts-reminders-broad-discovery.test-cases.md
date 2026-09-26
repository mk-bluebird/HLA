# Community Prompts, Reflection Reminders, and Broad-Area Discovery Tests

## Community Conversation Prompt Library

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| CPL-001 | Member selects a prompt category | Request random prompt | Returns only published, reviewed prompt from that category |
| CPL-002 | Member hides a prompt | Refresh prompt session | Same prompt does not reappear in that session |
| CPL-003 | Member disables prompts | Open discussion surface | No prompt is shown |
| CPL-004 | Prompt contains health or trauma question | Schema/policy validation | Rejected |
| CPL-005 | Prompt requests a location or schedule | Schema/policy validation | Rejected |
| CPL-006 | Search index receives member message text | Ingestion attempt | Rejected; only prompt catalog can be indexed |
| CPL-007 | Prompt is flagged | Review workflow | Opens content review; no user-level penalty |
| CPL-008 | Keyboard/screen-reader mode | Browse and skip prompt | Full function available |

## Member-Requested Reflection Reminder

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| RRM-001 | Member has no reminder | Uses core platform | No notifications sent |
| RRM-002 | Member creates one-time reminder | Scheduled time arrives | Neutral member-chosen reminder only |
| RRM-003 | Member dismisses reminder | Future delivery | No frequency increase, follow-up, or streak |
| RRM-004 | Request includes inactivity state | Schema validation | Rejected |
| RRM-005 | Request includes match count | Schema validation | Rejected |
| RRM-006 | Reminder contains another member's identity | Validation | Rejected |
| RRM-007 | Member pauses reminder | Delivery scheduler | No future delivery |
| RRM-008 | Notification permission denied | Reminder use | In-app text-first path remains available |

## Broad-Area Shared Activity Discovery

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| BASD-001 | Two active requests share permitted context | Eligibility evaluation | Option created with factual explanation |
| BASD-002 | Requests differ in region | Eligibility evaluation | No result; no distance or location revealed |
| BASD-003 | Request includes coordinate or GeoHash | Schema validation | Rejected |
| BASD-004 | Request includes proximity radius | Schema validation | Rejected |
| BASD-005 | Request includes profile-view or message data | Boundary validation | Rejected |
| BASD-006 | Member pauses request | Candidate generation | Immediate exclusion; no counterpart notification |
| BASD-007 | Member withdraws request | Cached discovery card | Removed immediately |
| BASD-008 | System attempts proximity ordering | Allocation test | Rejected |
| BASD-009 | System attempts numeric compatibility score | Allocation test | Rejected |
| BASD-010 | Option is presented | Contact pathway | No direct message opens until reciprocal interest |
