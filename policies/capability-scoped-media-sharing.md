# Capability-Scoped Media Sharing Policy

## Purpose

Capability-Scoped Media Sharing lets an adult choose whether to share one media
item with one selected recipient for one stated purpose and limited period.

It does not guarantee that a recipient will not copy, screenshot, screen-record,
photograph, export, remember, or otherwise retain information after viewing.

## Required conditions

Before a sender may share media:

- Adult-policy eligibility is current where the media class requires it.
- A current recipient-scoped media-sharing consent grant exists.
- The sender confirms the selected recipient, media class, purpose, and expiry.
- The recipient has an active permission to receive or view the selected media
  class where required.
- No block, restriction, withdrawal, expiry, or policy prohibition applies.
- Non-commercial integrity checks pass.
- The sender sees a clear copy-risk notice.

## Media classes

- `non_sensitive_media`
- `sensitive_personal_media`
- `explicit_adult_media`

The sender classifies the media. Hearthline must not infer class from image,
voice, body, face, biometric, or automated content analysis.

## Required sender notice

> This media will be shared with the selected recipient for the chosen purpose
> until the expiry time. Hearthline can limit future platform access after
> withdrawal or expiry, but cannot guarantee that a recipient will not copy,
> record, screenshot, photograph, export, or remember what they receive.

## Required recipient notice

> This media was shared for this conversation scope. Do not forward, record,
> publish, or share it without the sender's separate permission. This notice
> does not replace the recipient's responsibility to respect boundaries.

## Prohibited behavior

Hearthline must not:

- Automatically classify intimate content using biometric, body, face, emotion,
  or identity inference.
- Require a member to share media.
- Use media or media metadata for matching, ranking, advertising, model
  training, identity inference, attractiveness scoring, or behavioral profiling.
- Claim that no forwarding is technically guaranteed.
- Reveal a recipient identity through persistent visible watermarking by default.
- Create a permanent media-access history visible to other members.
- Keep revoked or expired media available through links, caches, previews,
  derivatives, or search indexes.
- Treat a member's refusal to share media as a negative signal.
- Allow media sharing to substitute for specific consent to calls, meetings,
  venue disclosure, touch, sexual discussion, or intimacy.

## Optional watermarking boundary

A member may choose a privacy-reviewed, recipient-scoped watermark only if:

- It is optional.
- The exact watermark content is shown before sending.
- It does not reveal legal identity, contact information, location, or public
  identifier.
- It is not used as a tracking mechanism.
- A no-watermark path remains available.
- The system does not claim the watermark prevents copying.
