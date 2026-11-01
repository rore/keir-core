---
id: "response-projection-consumer"
title: "Proposal: return bounded customer-defined views"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Evaluate the FocusList/response-transform path as a distinct read-only consumer contract.

## In Scope

- Specify bounded output records/lists, allowed projection/filter/order operations and protected fields.
- Make completeness/partial-view semantics explicit; a truncated page cannot claim global ordering.
- Define host validation and failure behavior separately from ordinary DISPLAY and native writes.

## Out of Scope

Unrestricted maps/sorting, protected-state mutation or silently treating partial reads as complete.

## Done When

- [ ] Decision: choose a concrete focus-list or response-transform scenario and its consumer/completeness contract.
- [ ] Before adoption: settle type/CEL/resource requirements and host validation, with rejection vectors for partial or forged output.
- [ ] If adopted: show same-artifact Java/Node consumption and actual task-view enforcement without leaking unauthorized data.

## Notes

**Dependencies:** [enrich-followup-expressions](../features/enrich-followup-expressions.md).

**Design input:** [Response transformation, sections 6.5 and 13D](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Focus-list example, section 4C](../../docs/internal/design-baseline/public/task-management-poc.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
