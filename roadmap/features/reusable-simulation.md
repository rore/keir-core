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

**First increment:** The independently reviewed [read/2 simulation contract](../../docs/specs/read-simulation.md) and [operator guide](../../docs/agents/read-simulation.md) provide data-only cases, host-owned authority, strict ordered responses, independent compact expectations and bounded redacted reports through the same Java and Node artifact/runtime. See the [runnable exact-artifact example](../../conformance/read-simulation/README.md) and [Work Record](../../.agent-workflow/tasks/read2-reusable-simulation.md). Studio UX is independent.

**Progress:** Corrected candidate `8a5eeec`, source `8c63aa0`, passed targeted independent result review with no residual blockers. Verification passes 231 Java tests, 390 Node tests, nineteen canonical packaged report comparisons, twenty-nine schema documents and standalone package execution. The initial four review findings are corrected: trace-free expectations, safe native summaries, BOM parity and distinct dispatched business denial versus host entry denial. Source/proof/fixture identity, privacy and native/old-golden compatibility are verified. Main integration is in progress; hosted CI and final delivery remain pending. The full diff retains a size advisory without a boundary violation or required checkpoint.

**Remaining:** Effect simulation, broader non-task examples and the reusable host-conformance kit remain on this feature; this read/2 increment does not complete it. Simulated PASS does not certify real adapters, host callback purity or hard resource containment.

**Design input:** [Scenarios and test layers, sections 2-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Host conformance, section 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
