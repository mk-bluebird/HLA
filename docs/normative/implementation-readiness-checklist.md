# Implementation Readiness Checklist

A module is implementation-ready only when all items are complete.

- [ ] Every persistent field maps to an allowed-data-class entry.
- [ ] Every omitted sensitive field is covered by a prohibited-field rule.
- [ ] Every trust-zone crossing has an explicit API contract.
- [ ] Every state transition maps to a canonical state machine.
- [ ] Every TTL has deletion and expiry tests.
- [ ] Every cryptographically erasable data class has lifecycle-key ownership.
- [ ] Every release gate has positive, negative, adversarial, deletion, and egress-denial tests.
- [ ] Every accessible alternative provides equivalent ordinary participation.
- [ ] Every external provider has a minimization contract and outage route.
- [ ] Every cache is non-authoritative and cannot bypass revocation.
- [ ] Every storage boundary has distinct runtime credentials and no wildcard grants.
- [ ] Every residual risk has an accepted-risk record with a review date.
- [ ] Required reviewers approved the exact invariant and gate version.
