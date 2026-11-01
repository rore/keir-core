---
id: "multi-effect-composition"
title: "Proposal: admit certified sequences of multiple effects"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Evaluate v0.7 beyond the current hard single-WRITE limit, with collision-aware admission and explicit partial/uncertain outcomes.

## In Scope

- Require actual dedupe identity scope, payload/retention/result-recovery behavior and native evidence for every participating capability.
- Analyze collisions across calls, bindings and native callers; different capability names alone do not prove independent namespaces.
- Forward the original key unchanged. Step IDs are trace identities, not generated downstream keys; optional host key slots remain a separate decision.

## Out of Scope

Atomicity inferred from idempotency, exactly-once business effects, automatic retries/replay, compensation or distributed transactions.

## Done When

- [ ] Decision: choose a concrete multi-effect scenario, admission policy and host reconciliation responsibility, or retain one WRITE.
- [ ] Before adoption: settle per-effect ledger/termination/partial-uncertain semantics and real collision-scope conformance.
- [ ] If adopted: both runtimes and actual host tests reject collisions and unsafe replay while preserving earlier confirmed and unknown effects.

## Notes

**Dependencies:** [real-effect-admission](../features/real-effect-admission.md); [demo-task-integration](../features/demo-task-integration.md).

**Design input:** [All v0.7 contracts and open decisions](../../docs/internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
