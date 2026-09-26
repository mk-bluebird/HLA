# Source-Labeled Venue Information API

## GET /meeting-options/{venueRef}/information

Returns public, source-labeled venue or event information.

## Example response

```json
{
  "venueRef": "venue_public_reference",
  "items": [
    {
      "informationType": "published_recording_policy",
      "plainLanguageSummary": "The venue publishes a recording policy on its official website.",
      "sourcePublisher": "Venue publisher",
      "sourceUrl": "[https://example.org/policy](https://example.org/policy)",
      "retrievedAt": "2026-09-26T20:00:00Z",
      "reviewBy": "2026-10-26T20:00:00Z",
      "status": "listed"
    }
  ],
  "memberNotice": "Venue information comes from public sources and may change. Hearthline does not certify that a venue is safe, accessible, open, private, available, suitable, or that anyone consents to an interaction."
}
```

## Prohibited query parameters

The endpoint must reject:

```text
live
capacity
crowd
attendance
nearby
memberCount
checkIn
safetyScore
riskScore
trustScore
verifiedSafe
counterpartPresent
arrival
location
```

## Separate participant-consent interface

Participant consent must use only the Capability-Scoped Consent Lifecycle:

```text
POST /consent/lifecycles
POST /consent/lifecycles/{lifecycleId}/request
POST /consent/lifecycles/{lifecycleId}/grant
POST /consent/lifecycles/{lifecycleId}/withdraw
```

Venue policy is informational. It never grants participant consent.
