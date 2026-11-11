---
id: "runtime-resource-qualification"
title: "Qualify CPU, memory and deadline containment"
status: "queued"
priority: "high"
commitment: "committed"
milestone: "Release an embeddable Keir library"
repo: "keir-core"
---

## Summary

Close the production resource-evidence gap while preserving the already tested restricted scalar profile.

Bounded measurements are delivered; the residual production-containment work is unassigned. Queued status does not invalidate earlier measurements or imply that the existing demo cannot execute. Current package hardening and installed-consumer qualification are recorded in the [Core 0.3.1 release record](../../docs/releases/core-0.3.1-candidate.md); those checks do not establish arbitrary host-code preemption or unmeasured scale.

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

**Keir 2 measurement baseline:** [The delivered expression slice](enrich-followup-expressions.md) adds packaged Java/Node parity and serial whole-artifact measurements, including deadline expiry before a write and retained confirmed evidence after a later read expires. These retain their recorded source/producer identities and qualify the stated serial workloads, not general concurrency or CPU preemption.

**Completed bounded measurement contract:** The accepted dense serial screen below exercised one compiler-produced Keir 2 artifact with fully evaluated bindings and maximum-list membership, using identical bytes in Java and Node. Its agreed bounds were 100 warmups and 5,000 measured serial invocations per runtime under the isolated supervisor, a 45-second watchdog, 256 MiB engine heap and 512 MiB observed process budget. The last three retained-heap samples had to span no more than the larger of 8 MiB or 10% of warmed heap. This historical screening threshold is not proof of leak freedom or a new assignment. Sustained concurrency and hard containment remain separate, unqualified work.

**Dense serial screen accepted:** [The opt-in phase](../../conformance/resource-qualification/README.md#fully-evaluated-keir-2-membership-workload) verified 473 checked nodes and 80 fully reached membership scans. Both runtimes completed 100 warmups plus 5,000 measured invocations with identical artifact/outcome digests and zero dispatch. The run took 14.064 seconds and passed the agreed observed-memory and retained-heap screens. [Independent integration review](../../.agent-workflow/tasks/dense-resource-qualification.md#result-review) inspected the code and asserted the retained report. This closes that bounded measurement only; broader qualification remains queued. Keep the historical sample's identity intact when adapting future producer/fixture paths.

**Current compiler alignment verified:** The runtime developer updated the three existing harness paths needed by Core 0.3: current fixture selection, the already supplied title model at Node binding, and the independent Inventory-model assertion. Syntax checks pass. Three actual compiler-produced examples (normal, title and Inventory) prepare and execute once in each runtime with matching complete outcomes and expected artifact pins. The integration owner inspected the three-file diff, checked the final file hashes and independently compared the report objects and artifact/model digests. This is command-generation maintenance, not a new benchmark, resource allowance or command option. Historical Keir 2 measurements retain their original identity and do not qualify the new generation's full resource workload.

**Dependencies:** [bound-cel-execution](../features/bound-cel-execution.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Resource limits and gates, sections 6 and 10](../../docs/internal/design-baseline/public/keir-architecture.md); [Runner bounds, sections 6-7](../../docs/internal/design-baseline/public/compiler-artifact-and-runners-v0.md).

Also follows accepted decision 0003. Qualification accompanies each newly admitted construct; richer syntax must fail closed until its own costs are known. The first local scalar demo need not await a blanket production claim.
