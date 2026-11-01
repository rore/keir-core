---
id: "runtime-scale-observability"
title: "Measure many-extension performance and expose safe operational evidence"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Release an embeddable Keir library"
repo: "keir-core"
---

## Summary

Give embedders measured preparation/invocation costs and useful redacted diagnostics across many model/program bindings.

## In Scope

- Separate compile, cold prepare, warm invocation, adapter latency and retained-memory measurements; exercise shared model reuse and capacity/release behavior.
- Define safe per-run correlation, budget/effect evidence and diagnostic output without leaking native keys, identities or customer payloads.
- Keep benchmarks/stress explicit and fast regression fast; hosts own audit storage, access controls and alerting.

## Out of Scope

A global runtime registry, mandatory telemetry vendor or speculative optimization/code generation.

## Done When

- [ ] Design/spec: select representative workloads and observable fields before choosing performance targets.
- [ ] Implementation: reusable measurement/reporting paths demonstrate many programs sharing a model and independent scoped invocations.
- [ ] Verification: report measured distributions/memory and redaction tests; no unmeasured capacity, retention or latency guarantees.

## Notes

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Full-runner measurement, section 6.3](../../docs/internal/design-baseline/public/keir-architecture.md); [Trace and effect evidence, section 4](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Lifecycle/budgets, sections 10-11](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
