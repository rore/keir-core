---
id: "multi-binding-composition"
title: "Proposal: compose multiple extensions at one hook"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Decide supported activation modes and aggregate guarantees when one hook selects more than one program.

## In Scope

- Choose exclusive, all-must-continue, ordered or independent fan-out semantics explicitly rather than inferring them.
- Define decision/patch conflicts, priorities, total quotas, failure handling, revocation and binding changes.
- Assess native key/effect collisions across programs and native callers; per-program bounds do not create hook-wide bounds.

## Out of Scope

Automatic policy chains, patch conflict resolution or a durable workflow engine.

## Done When

- [ ] Decision: choose the smallest useful composition scenario and activation modes, or retain exclusive selection.
- [ ] Before adoption: define combined consumer outcome, aggregate budgets/authority and collision/partial-failure gates.
- [ ] If adopted: shared conformance and real host tests cover conflicting bindings, limits, binding changes and honest effects.

## Notes

**Dependencies:** [extension-approval-lifecycle](../features/extension-approval-lifecycle.md).

**Design input:** [Binding composition, sections 8 and 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Cross-binding admission, sections 5 and 8](../../docs/internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
