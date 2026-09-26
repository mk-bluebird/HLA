# Communication Controls, Access Preferences, and Venue Information Tests

## Communication and Comfort Controls

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| CCC-001 | Member chooses no controls | Uses core features | Full access without an “incomplete” marker |
| CCC-002 | Member selects text-first | Begins reciprocal interaction | Text-first preference can be shown only at selected scope |
| CCC-003 | Member withdraws a control | Counterpart refreshes disclosure | Control no longer visible |
| CCC-004 | Request includes autism, ADHD, diagnosis, or disability label | Schema validation | Rejected |
| CCC-005 | System uses controls in discovery ranking | Policy check | Rejected |
| CCC-006 | System uses controls for response prediction | Policy check | Rejected |
| CCC-007 | Keyboard-only member edits controls | Accessibility test | Full function available |
| CCC-008 | Screen-reader user withdraws controls | Accessibility test | Full function available |

## Access and Planning Preferences

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| APP-001 | Member selects no preferences | Uses meeting planning | Full access remains available |
| APP-002 | Member selects step-free information preference | Browse meeting options | Source-labeled attributes shown |
| APP-003 | Source lacks published attribute | Render option | Shows unknown; no negative suitability claim |
| APP-004 | Request includes disability or medical proof | Schema validation | Rejected |
| APP-005 | System uses preference to exclude people | Policy check | Rejected |
| APP-006 | Member shares one preference with named counterpart | Disclosure check | Requires recipient-scoped consent |
| APP-007 | Member withdraws preference | Meeting-plan view refresh | Future presentation removed immediately |
| APP-008 | Venue is presented | UI review | States source, retrieval date, uncertainty, and non-certification |

## Source-Labeled Venue Information Interface

| ID | Preconditions | Action | Expected result |
|---|---|---|---|
| SVI-001 | Official venue source exists | Create information record | Source, retrieval time, review date, and status required |
| SVI-002 | Request includes live capacity | Schema validation | Rejected |
| SVI-003 | Request includes member attendance | Schema validation | Rejected |
| SVI-004 | Request includes `verifiedSafe` | Schema validation | Rejected |
| SVI-005 | Venue publishes recording policy | Render item | Clearly attributed; does not grant participant consent |
| SVI-006 | Venue information is outdated | Render item | Marked outdated or excluded |
| SVI-007 | Member reports broken source link | Review workflow | Source maintenance review only; no venue rating |
| SVI-008 | API tries to return nearby member count | Contract test | Rejected |
