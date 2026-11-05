---
id: "runtime-resource-qualification"
title: "Qualify CPU, memory and deadline containment"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Release an embeddable Keir library"
repo: "keir-core"
---

## Summary

Close the production resource-evidence gap while preserving the already tested restricted scalar profile.

## In Scope

- Measure actual compiler and CEL-Java/CEL-ES behavior for admitted and adversarial inputs, decoded data and intermediate/aggregate allocations.
- Choose the smallest enforceable bounds or isolation mechanism; a timer/thread timeout alone is not proof of preemption.
- Bound compilation subprocess output/concurrency and prove cancellation/reaping where used; retain honest earlier effects on limit failure.

## Out of Scope

Assuming worker limits cover all process memory, adopting an isolation platform without evidence or claiming production safety from fakes.

## Done When

- [ ] Design/spec: define the supported workload and evidence needed for each CPU/memory/deadline claim.
- [ ] Implementation: enforce or explicitly reject unsupported work without changing the user environment or weakening admitted semantics.
- [ ] Verification: isolated stress/resource suites prove boundary and overflow behavior in both runtimes, including preserved CONFIRMED/UNKNOWN receipts after termination; late responses cannot change the settled outcome.

## Notes

**First slice delivered:** The [opt-in qualification harness](../../conformance/resource-qualification/README.md) measures the current `effect/4` profile using the compiler and both runtimes. Three runs matched 22 runtime checks, including settled write receipts after late responses. The [Work Record](../../.agent-workflow/tasks/effect4-resource-evidence.md) records scope and measurements.

**Serial workload measured:** The [second slice](../../.agent-workflow/tasks/effect4-embedded-workload.md) verified the 8/9-step and 4/5-call boundaries and measured 3,000 serial calls per runtime on a trusted host with cooperative adapters. It observed JVM working-set peaks and verified the same 30,777-byte artifact in both runtimes. The derived 495,792-byte compiler-output bound covers only the two measured catalogs. The large fixture short-circuits during invocation.

**Still open:** Worst-case admitted CPU and memory behavior, longer-duration retention, capacity beyond the serial workload, general near-ceiling artifact admission, and enforceable limits for synchronous evaluation. The 1 MiB probe remains byte and structural rejection evidence.

**Dependencies:** [bound-cel-execution](../features/bound-cel-execution.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Resource limits and gates, sections 6 and 10](../../docs/internal/design-baseline/public/keir-architecture.md); [Runner bounds, sections 6-7](../../docs/internal/design-baseline/public/compiler-artifact-and-runners-v0.md).

Also follows accepted decision 0003. Qualification accompanies each newly admitted construct; richer syntax must fail closed until its own costs are known. The first local scalar demo need not await a blanket production claim.
