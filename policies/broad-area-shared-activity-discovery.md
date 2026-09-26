# Broad-Area Shared Activity Discovery Policy

## Purpose

Broad-Area Shared Activity Discovery helps members find optional shared activity
or conversation contexts based only on current, member-selected, privacy-limited
settings.

It does not discover who is nearby, track movement, predict compatibility,
identify likely respondents, or establish familiarity.

## Permitted discovery inputs

- Current connection intent.
- Selected activity categories.
- Selected atmosphere preferences.
- Selected communication pace preferences.
- Member-selected broad region identifier.
- Remote-only setting.
- Broad future activity-window blocks.
- Language selected by the member.
- Member-selected access/comfort controls where voluntarily shared for an
  interaction purpose.
- Current visibility state.
- Block, restriction, and consent state.

## Prohibited discovery inputs

- Coordinates, location, address, IP-derived position, GeoHash, map tile,
  geofence, distance, route, travel origin, travel destination, or proximity.
- Background location, visit history, venue attendance, check-ins, current
  presence, active-now status, or nearby-device observations.
- Wi-Fi, Bluetooth, cell-tower, device, browser, or advertising identifiers.
- Contacts, social graph, uploaded address books, workplace, school, tags,
  profile views, or third-party relationship data.
- Private messages, media, message sentiment, typing behavior, read receipts,
  response speed, account tenure, or time spent.
- Health, disability, recovery, sobriety, sexuality, identity, finances,
  housing, employment, legal history, relationship history, vulnerability, or
  inferred personal state.
- Popularity, desirability, safety, risk, trust, compatibility, or reply-
  likelihood score.

## Presentation rules

- Use factual shared-context explanations.
- Offer self-directed filters and chronological browsing.
- Use transparent rotation when many eligible options exist.
- Do not rank people by proximity, activity frequency, profile completeness,
  popularity, response speed, media, engagement, or predicted outcome.
- Do not show counts of people nearby, active, in an area, or at a venue.
- Do not show roster, attendance, or crossed-paths information.
- Do not create a message thread from a presentation alone.

## Pause Discovery

Pause Discovery is the replacement for an `invisible` flag.

When a member pauses discovery:

- Their active discovery cards and activity windows leave new candidate
  generation immediately.
- The platform does not disclose that they paused.
- The platform does not display inactive, offline, nearby, last-active, or
  hidden-presence status.
- The member may resume later without penalty.
- Existing blocks, reports, privacy settings, and consent withdrawals remain in
  force.

## Required explanation

> This option is based only on current settings you and another member selected
> for discovery, such as a shared activity context, a broad region or remote
> setting, and broad future time blocks. It does not use contacts, mutual
> friends, exact location, location history, venue visits, live presence,
> private messages, profile views, popularity, or response likelihood.
