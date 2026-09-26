# Mutual Activity Window Policy

## Purpose

A Mutual Activity Window lets a member share a broad, future-facing opportunity
for a selected activity or conversation without revealing an exact schedule,
calendar, work routine, home routine, location, or real-time availability.

## Allowed fields

- Future start date.
- Future end date.
- Broad time block.
- Activity scope.
- Broad member-selected region or remote option.
- Expiry time.
- Visibility scope.
- Recipient-specific consent reference where applicable.

## Prohibited fields

- Exact start time.
- Exact end time.
- Calendar event title.
- Calendar event description.
- Calendar attendee.
- Calendar provider identifier.
- Recurring schedule.
- Historical availability.
- Work schedule.
- Home routine.
- Live online state.
- Last active time.
- Response timing.
- Device usage.
- Current location.
- Coordinates.
- Route.
- Transit plan.
- Venue attendance.
- Travel origin or destination.
- Inferred energy, health, recovery, disability, finances, caregiving, or
  vulnerability.

## Member-facing statement

> This window shares only a broad future activity possibility. It does not show
> your calendar, exact schedule, current availability, location, route, or past
> routine. You can pause or withdraw it at any time.

## Matching rules

- The system may determine whether broad windows overlap.
- The system may not expose a counterpart's exact blocks, hidden blocks, or
  non-overlap reason.
- The system may not use overlap frequency, repeated availability, response
  speed, or calendar behavior to rank people.
- A matching result does not open messaging or schedule a meeting.
- A mutual interest and separate consent are required before any plan is created.

## Retention

- Windows expire by default.
- Expired, paused, and withdrawn windows leave candidate generation immediately.
- The platform must not construct an availability history from prior windows.
- Windows must not be used for advertising, behavioral profiling, model training,
  or “reliability” scoring.
