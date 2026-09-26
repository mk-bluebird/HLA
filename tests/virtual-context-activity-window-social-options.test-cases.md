# Virtual Gathering Context, Mutual Activity Window, and Social Options Tests

## Virtual Gathering Context

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| VGC-001 | Context contains text-first option | Member joins through keyboard/screen reader | Full text-first access works |
| VGC-002 | Request contains `verifiedSafe` | Validate context | Rejected |
| VGC-003 | Request contains trust score | Validate context | Rejected |
| VGC-004 | Context expires | New join attempt | Denied; no new entry |
| VGC-005 | Member leaves context | Inspect later discovery payload | No attendance record available |
| VGC-006 | Recording checkpoint absent | Participant tries recording action | Denied |
| VGC-007 | Report is submitted | Governance processing | Human-review route available; no venue score created |
| VGC-008 | Context asks for camera | Access check | No-camera alternative remains available |

## Mutual Activity Window

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| MAW-001 | Two active broad windows overlap | Evaluate overlap | Returns boolean overlap and broad explanation only |
| MAW-002 | Windows do not overlap | Evaluate overlap | Does not reveal counterpart's exact dates or blocks |
| MAW-003 | Request includes exact start time | Validate schema | Rejected |
| MAW-004 | Request includes calendar-event identifier | Validate schema | Rejected |
| MAW-005 | Window expires | Candidate generation | Window excluded immediately |
| MAW-006 | Member withdraws window | Cached result refresh | Window removed immediately |
| MAW-007 | Location permission denied | Create remote window | Full function works |
| MAW-008 | Overlap result used to auto-create meeting | Policy check | Denied; mutual interest and plan consent still required |

## Member-Directed Social Options Planner

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| SOP-001 | Member supplies minimal current inputs | Generate suggestions | Optional, editable suggestions returned |
| SOP-002 | Request includes calendar data | Validate session | Rejected |
| SOP-003 | Request includes inferred energy score | Validate session | Rejected |
| SOP-004 | Member chooses notification `none` | Planner lifecycle | No notifications sent |
| SOP-005 | Member dismisses a suggestion | Planner state | No repeat, penalty, or streak |
| SOP-006 | Suggestion attempts to send invitation | Validate output | Rejected |
| SOP-007 | Member uses text-first path | Create and dismiss suggestions | Full functionality works |
| SOP-008 | Planner session expires | Inspect downstream systems | Inputs excluded from matching, advertising, and model training |
