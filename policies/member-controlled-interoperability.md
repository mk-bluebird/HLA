# Member-Controlled Interoperability Policy

## Purpose

Member-Controlled Interoperability lets members export, import, or connect only
their own selected data through documented, purpose-limited mechanisms.

It reduces lock-in without exporting other members' data, creating a portable
reputation system, importing social graphs, or weakening Hearthline privacy and
consent boundaries.

## Permitted initial capabilities

- Member export of their own selected account data.
- Member export of member-authored drafts, preferences, settings, and permitted
  records.
- Member-controlled deletion/export request.
- Documented OAuth connection with explicit scope and revocation.
- Import of member-authored data after preview, confirmation, and field mapping.
- Source-labeled public resource links.
- Optional later presentation of minimal, short-lived predicates where a
  non-cryptographic path remains available.

## Prohibited capabilities

Hearthline must not:

- Export another member's profile, messages, media, identity claims, boundaries,
  consent grants, meeting details, reports, blocks, or private history.
- Import address books, contact graphs, social graphs, profile views, message
  history, location history, attendance, or behavioral analytics for discovery.
- Collect third-party passwords.
- Replay sessions or imitate browser sessions.
- Scrape external platforms or use unofficial APIs.
- Automatically post, message, like, follow, invite, or accept actions on an
  external platform.
- Publish consent, boundaries, intimate preferences, casual-connection intent,
  identity claims, access preferences, meeting plans, trusted-contact plans,
  location, or reports through federation.
- Create portable reputation, trust, safety, risk, desirability, popularity,
  compatibility, report-history, or encounter-history credentials.
- Require DID, wallet, blockchain, public key, public profile, or external
  social account for core access.

## OAuth requirements

Every OAuth connection must:

- Be initiated by the member.
- Show exact requested scopes.
- Explain purpose, retention, recipient, and revocation path.
- Request the minimum scopes necessary.
- Store tokens in an isolated integration zone.
- Support revocation.
- Avoid use of imported data for matching, ranking, advertising, or model
  training unless separately and explicitly authorized under a reviewed policy.

## Export requirements

Exports must:

- Be member-initiated.
- Preview included data classes.
- Exclude other members' private information.
- Exclude report evidence and safety-sensitive moderation details unless policy
  and applicable process permit a member-accessible copy.
- Be encrypted or otherwise protected where technically feasible.
- Be available in accessible, documented formats.
- Include retention/deletion information.

## Member-facing statement

> You control what you export or connect. Hearthline does not export other
> members' private information or create a portable reputation. External services
> have their own privacy and retention practices.
