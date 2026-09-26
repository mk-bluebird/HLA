# Pause Discovery Policy

## Purpose

Pause Discovery lets a member remove their active discovery objects from new
candidate generation without disclosing a presence, absence, browsing state, or
reason to other members.

## Required behavior

When Pause Discovery is active:

- New discovery presentation stops for the member's active discovery objects.
- The member does not appear as nearby, online, active, inactive, hidden, or
  last active.
- The platform does not notify other members that the member paused.
- The platform does not expose who the member viewed, considered, hid, or
  declined.
- The member may resume discovery later without a visibility penalty.
- Existing blocks, restrictions, consent withdrawals, reports, disclosures, and
  deletion requests remain active.
- Existing conversations remain governed by their separate conversation consent,
  mute, block, and restriction controls.

## Prohibited behavior

Pause Discovery must not:

- Collect hidden browsing history.
- Enable profile-view notifications.
- Preserve a “ghost” presence state.
- Reveal whether someone is currently using the app.
- Increase recommendations or notifications when a member resumes.
- Penalize a member for pausing.
- Use pause duration, frequency, or timing in discovery allocation, moderation,
  risk assessment, advertising, engagement analysis, or model training.
- Allow a counterpart to infer pause state through error messages, missing
  presence indicators, or changed profile behavior.

## Member-facing text

> Pausing discovery stops new connection options from being created from your
> active discovery settings. Other members are not told that you paused. HLA does
> not show online, nearby, or last-active status.
