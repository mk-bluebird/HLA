# Member-Directed Social Options Planner

## Purpose

The Member-Directed Social Options Planner helps a member privately explore
optional, low-pressure social possibilities using current inputs the member
chooses to provide.

It is not a routine enforcer, recommendation score, social-health assessment,
calendar agent, location tracker, or engagement mechanism.

## Member-selected inputs

The planner may accept only inputs the member actively enters for the current
planning session:

- Current connection goal.
- Preferred format.
- Broad planning window.
- Pace and comfort controls.
- Cost preference.
- Broad setting preference.
- Notification preference.

Inputs are optional. A member may use the planner with minimal information.

## Prohibited inputs and inference

The planner must not collect, derive, or use:

- Calendar events.
- Calendar titles, notes, attendees, locations, or provider identifiers.
- Exact schedule.
- Real-time availability.
- Location, routes, venue history, or device proximity.
- Energy level inferred from behavior, typing, message frequency, sleep, device
  use, health, or calendar.
- Health, disability, recovery, mental state, trauma, loneliness, crisis, or
  vulnerability.
- Income, housing, debt, employment, transport access, or financial need.
- Reply rate, time spent, engagement, match count, date count, or social graph.
- Protected identity or identity inference.
- A claim that an option is safe, suitable, therapeutic, or likely to improve
  wellbeing.

## Output requirements

Every output must be:

- Optional.
- Editable or dismissible.
- Non-repeating unless the member requests recurrence.
- Clearly labeled as a suggestion, not a task.
- Free of guilt, urgency, streaks, scarcity, or penalty language.
- Available through text-first, keyboard, screen-reader, reduced-motion,
  low-bandwidth, no-camera, no-microphone, and asynchronous paths.
- Accompanied by a pause or stop-planning control.

## Notification rules

Default: no notifications.

A member may choose:

- No notifications.
- Open planner manually only.
- Digest-only reminder.
- A single selected reminder.

The planner must not:

- Send urgency notifications.
- Use inactivity to trigger reminders.
- Create streaks.
- Warn that social opportunities are expiring because the member did not act.
- Penalize a member for dismissing, pausing, or ignoring an option.

## Calendar boundary

The initial planner must not access a calendar.

Any future calendar feature requires separate design review and must be:

- Optional.
- Local-first where feasible.
- Read-minimized.
- No event content, attendee, location, or title access.
- No cloud upload of calendar data.
- No automatic scheduling or messaging.
- Fully replaceable with a no-calendar path.
