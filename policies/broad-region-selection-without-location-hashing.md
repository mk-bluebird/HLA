# Broad Region Selection Without Location Hashing

## Purpose

Broad Region Selection lets a member manually choose a coarse context for
optional activity discovery without collecting, deriving, storing, or exposing
their physical location.

## Allowed values

- A manually selected broad region identifier.
- `remote`.
- `public_category_only`.

## Prohibited values and processing

Hearthline must not collect, derive, retain, or use:

- Latitude or longitude.
- GeoHash.
- Plus code.
- Map tile.
- IP-derived location.
- GPS.
- Cell-tower data.
- Wi-Fi or Bluetooth data.
- Device proximity.
- Geofence event.
- Radius query.
- Distance calculation.
- Route.
- Travel origin or destination.
- Location history.
- Venue attendance.
- Check-in.
- Current presence.
- Nearby status.
- Arrival/departure status.
- “Frequently visited” place.
- Location noise or differential-privacy parameter.
- k-anonymity group for person-location disclosure.

## Discovery rule

Two members may be eligible for a broad-area context only when they independently
selected the same broad region identifier, or both selected a compatible
non-location setting such as `remote` or `public_category_only`.

The system must not calculate distance, travel time, nearest venue, or physical
proximity.

## Meeting-detail rule

A named public venue may be disclosed only after a separate mutual, recipient-
scoped, time-limited consent checkpoint.

The venue detail must not become future discovery input, location history,
attendance history, or a counterpart-presence signal.

## Member-facing text

> Choose a broad area yourself, or choose remote. Hearthline does not use your
> GPS, route, device proximity, location history, or exact position.
