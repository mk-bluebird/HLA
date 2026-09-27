# HLA Member-Invoked Assistance Contract

## Purpose

HLA provides free, low-token-cost, member-invoked assistance during eligible
Hearthline interactions.

It helps a member prepare a choice. It does not make, send, negotiate, or
interpret that choice for them.

## Invocation scope

HLA receives only:

- The member-selected tool.
- The member instruction.
- Member-selected text, if any.
- Current member-authorized settings required for that task.
- Current authorized shared context required for explanation.

HLA does not receive by default:

- Full conversation history.
- Unselected messages.
- Private media.
- Counterpart information outside current authorized scope.
- Location, route, attendance, or presence.
- Device and behavioral metadata.
- Calendar or schedule data.
- Message timing, read receipts, typing data, or online state.
- Consent history.
- Identity, health, recovery, finance, housing, employment, or vulnerability
  data unless a member explicitly selects a separately permitted field for the
  stated task.

## Free core tools

A member may use HLA at no charge to:

- Draft an opening or reply.
- Draft a boundary.
- Draft a clarification question.
- Draft a respectful decline.
- Draft a pause or quiet exit.
- Draft a public-first invitation.
- Draft a remote-first invitation.
- Draft an access request.
- Receive a reviewed topic suggestion.
- Explain a consent checkpoint.
- Explain a policy.
- Review selected text for plain-language pressure patterns.
- Compare current authorized shared contexts.
- Explain why a discovery option appeared.
- Review privacy scope.
- Review retention/expiry.
- Consider a public-first or remote activity category.
- Open a private reflection or planning prompt.

## Draft state machine

```text
IDLE
  → MEMBER_INVOKES_NAMED_TOOL
  → PRIVATE_DRAFT_OR_EXPLANATION
  → MEMBER_EDITS_OR_SAVES_OR_DISCARDS
  → OPTIONAL_ORDINARY_COMPOSER
  → MEMBER_MANUALLY_SENDS_OR_DOES_NOT_SEND
  → IDLE
```

## Non-autonomous rule

```text
externalActionAuthorized = false
```

HLA cannot:

- Send a message.
- Send a reaction or like.
- Accept or decline an invitation.
- Create a meeting.
- Schedule a calendar event.
- Share venue detail.
- Share location.
- Contact a trusted contact.
- File a report.
- Block, mute, restrict, or withdraw consent.
- Contact a third party.
- Negotiate with another person or agent.

## Privacy rule

HLA requests are private and must not create:

- A counterpart-visible indicator.
- A free-tier or paid-tier indicator.
- A “used AI” label.
- A conversation-quality score.
- A social-skills score.
- A vulnerability label.
- A discovery-ranking input.
- Advertising or default model-training data.

## Plain-language member notice

> HLA can help you prepare a draft or understand a setting. It does not send
> messages, decide for you, or read your conversations unless you select text
> for a specific request.
