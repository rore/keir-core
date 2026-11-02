---
id: "reusable-simulation"
title: "Offer reusable simulation and host-conformance tools"
status: "active"
priority: "medium"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-core"
---

## Summary

Turn the useful fixed sample harnesses into a small domain-independent tooling seam using the same runtime and artifact.

## In Scope

- Define a versioned, bounded data-only scenario contract, deterministic adapters, structured mismatches and consumer-result output.
- Reuse the existing interpreter and diagnostic model; protected context/fixture selection remains host-owned, never scenario authority.
- Provide a host-conformance kit for native context and consumer checks; actual database/service guarantee tests remain host-owned.

## Out of Scope

A production adapter certification service or a mandatory replacement for the already working minimal harnesses.

## Done When

- [ ] Design/spec: publish scenario/report schemas and the adapter/consumer seam with explicit authority and redaction boundaries.
- [ ] Implementation: run both task and non-task examples through reusable tooling in Java and Node without another execution engine.
- [ ] Verification: same-artifact parity, missing/extra/out-of-order stubs, invalid authority fields, typed consumer failures and redacted reports are covered.

## Notes

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**First increment:** The architect defined the bounded read/2 task-advice simulation contract and independent technical plan review approved `71540e3`. Implementation readiness is recorded at `faa6cfa`; the developer accepted ownership in an isolated checkout. The contract uses data-only scenarios, host-owned fixture authority, strict ordered responses, independent expectations and bounded redacted reports through the same Java and Node artifact/runtime. The [Work Record](../../.agent-workflow/tasks/read2-reusable-simulation.md) owns its plan and evidence. Studio UX is independent; effect simulation and the broader host-conformance kit remain outside this increment. Implementation and verification are in progress; no passing result is claimed yet.

**Progress:** Both wrappers, installed-package drivers, scenario/report schemas and shared exact-artifact examples are implemented at source `a58efb7`; the clean review handoff is `3b40ec3`. Retained verification passes 230 Java tests, 386 Node tests, fifteen packaged canonical report comparisons, exact fixture recompilation, offline schema checks and preserved legacy/generic conformance. Manager verified source identity, unchanged native code/old fixtures and public-content checks. Independent result review, integration and hosted CI remain pending. The complete diff retains a size advisory with no boundary violation or required checkpoint. This is qualified local evidence, not delivered or production-certified behavior.

**Design input:** [Scenarios and test layers, sections 2-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Host conformance, section 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
