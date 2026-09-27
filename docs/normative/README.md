# Hearthline Normative Specification Index

## Authority

This directory is the canonical source for product-boundary invariants,
data classes, state machines, release gates, residual risks, and
implementation traceability.

A production artifact must reference one or more invariant IDs from this
directory. A change that introduces a new persistent field, state transition,
trust-zone crossing, external provider, retention period, or member-visible
security consequence must update the relevant normative artifacts before code
can merge.

## Required artifacts

- invariant-registry.md
- prohibited-field-registry.md
- allowed-data-class-registry.md
- trust-zone-storage-map.md
- event-egress-matrix.md
- lifecycle-erasure-matrix.md
- state-machine-registry.md
- contradiction-resolution-log.md
- test-inventory.md
- residual-risk-registry.md
- release-gate-registry.md
- implementation-traceability.md
