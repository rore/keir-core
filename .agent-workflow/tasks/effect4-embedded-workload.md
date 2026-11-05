# Qualify a serial embedded effect/4 workload

<!-- agent-workflow:start -->
**Outcome:** Measure a reproducible resource envelope for a trusted embedded host using the scalar single-mutation profile.

**Target:** keir-core

**Scope:** Extend the opt-in resource harness with a serial soak, JVM process-wide memory observation and artifact-size evidence. Reuse existing boundary tests where sufficient.

**Constraints:** Keep default suites fast and preserve existing profile ceilings, runtime APIs and semantics. Use isolated fixtures and test-owned processes only. Measure one active invocation per host, with compilation and preparation outside the request path and cooperative adapters.

**Completion criteria:** Reproducible serial measurements include outcomes, elapsed time, CPU and memory trends, direct-child cleanup and a defensible artifact-size statement. Report hard-preemption and capacity limits separately.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Measure a reproducible resource envelope for a trusted embedded host using the scalar single-mutation profile.","scope":"Extend the opt-in resource harness with a serial soak, JVM process-wide memory observation and artifact-size evidence. Reuse existing boundary tests where sufficient.","constraints":"Keep default suites fast and preserve existing profile ceilings, runtime APIs and semantics. Use isolated fixtures and test-owned processes only. Measure one active invocation per host, with compilation and preparation outside the request path and cooperative adapters.","completion_criteria":"Reproducible serial measurements include outcomes, elapsed time, CPU and memory trends, direct-child cleanup and a defensible artifact-size statement. Report hard-preemption and capacity limits separately."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:runtime-resource-qualification"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Measurement tooling exercises resource-heavy cases and test processes. OS counters and artifact bounds require careful interpretation before making release claims.

**Discovery:** The existing harness covers a small shared workload, but the JVM measurements cover heap only and the 1 MiB probe is structurally invalid. The largest accepted fixture in that run was 26,157 bytes.

**Material assumptions:** The qualification target is a trusted embedded host with host-managed serial scheduling, cooperative adapters and at most one WRITE dispatch. The target has no hard wall-clock completion or tenant-isolation guarantee. Existing structural bounds and deadline checks still apply. A stronger requirement would need separate design.

**Plan:** Extend `conformance/resource-qualification/` only, with corresponding testing evidence and this record. Declare iteration count, watchdog, heap/process budget and platform before a serial soak of the large valid and concatenation cases. Record warm CPU/elapsed distributions and post-warmup memory samples. Investigate sustained unexplained growth. Use an existing OS counter for peak JVM working set for compiler and runtime children, or label sampled RSS and its interval. Keep heap separate. Check existing 8/9-step and 4/5-call evidence and add only missing cases. Derive a conservative serialized artifact bound from existing limits and cross-check it with a deliberately large compiler-produced fixture accepted in Java and Node. If a tighter maximum cannot be justified, retain the 1 MiB byte cap and record the open semantic boundary. Stop and report if evidence calls for runtime changes. Synchronous CEL and arbitrary host code are outside hard preemption claims.

**Verification plan:**
- Serial workload → reproducible opt-in runs confirm expected outcomes, settled receipts, cleanup, resource trends and the declared measurement budget on the stated platform.
- JVM memory → report process-wide working set or sampled RSS and method for compiler and runtime, separately from heap.
- Size and count boundaries → verify any derived bound against the accepted artifact and record exact 8/9 and 4/5 behavior from existing or new focused cases.

**Plan review:** Agent technical review: see [Plan review](#plan-review) below.

**Approvals:** No additional approval required for isolated repository work.

**State:** Ready for review
<!-- agent-workflow:end -->

## Plan review

Architecture review of the landed measurement slice recommended a bounded serial workload, JVM process-wide memory measurement and a valid artifact check against a derived bound. It approved reusing the current harness and existing boundary evidence. The qualification target defers hard interruption of synchronous CEL and arbitrary host code. Measurements do not establish a general containment or throughput guarantee.

## Implementation

- Extended the opt-in harness with a Windows serial mode. It reuses the compiler, prepared programs, cooperative adapters and child watchdog. One Windows sampler observes the compiler API or runtime JVM from outside that process. The original mode retains the four-way overlap probe.
- The exact effect/4 compiler accepts eight steps and four calls. It reports `BUDGET_EXCEEDED` at `/source/steps` for nine steps or five calls. Existing public Java and Node preparation tests provide the corresponding artifact boundaries.
- Derived a conservative 495,792-byte compiler artifact bound for the two exact measured fixture catalogs. Both runtimes accepted the same 30,777-byte fixture. Its 512-node program fills all eight steps with long identifiers and string comparisons. Adding one return expression fails the aggregate limit. The fixture is a cross-check of the catalog-specific calculation.
- Redline scope is Gray for the opt-in harness. The Work Record carries the test evidence, and the shared testing documentation is reserved for its owner.

## Verification evidence

Two final Windows x64 runs of the [serial qualification](../../conformance/resource-qualification/README.md) took 10.7 and 12.4 seconds with Java 21.0.12 and Node 24.14.1. Each passed 26 compiler cases, 25 matching runtime checks, 3,000 timed invocations per runtime and four direct-child supervision probes. The aggregate and Boolean cases completed with the expected result and receipt, and oversized concatenation stopped before write dispatch. Both late-response cases preserved settled receipts. The original mode passed its 21 compiler cases, 22 shared checks and four probes in 9.2 seconds. The focused Java and Node count tests both passed. Each run verified child closure and removal of its temporary directory.

| Observed process memory | Run 1 | Run 2 |
| --- | ---: | ---: |
| Compiler API JVM OS peak working set | 167.1 MB | 150.2 MB |
| Runtime JVM OS peak working set | 227.4 MB | 207.5 MB |
| Node runtime peak RSS | 116.8 MB | 117.5 MB |

Windows reported 8–10 working-set samples for the compiler API JVM and 26–32 for the runtime JVM, with nominal 100 ms intervals and actual offsets in the raw reports. Heap is recorded separately. The short CLI is covered by elapsed time and exact fixture bytes. Process peaks stayed below the declared 512 MiB observation budget.

| Runtime and case | Total elapsed per 1,000 timed invocations | Process CPU per 1,000 | Block p95 across both runs |
| --- | ---: | ---: | ---: |
| Java, large program | 103–156 ms | 438–594 ms | 105–397 µs |
| Java, concatenation | 363–520 ms | 1,734–1,984 ms | 396–979 µs |
| Java, Boolean intermediate | 288–440 ms | 1,297–2,422 ms | 299–805 µs |
| Node, large program | 73–88 ms | 140–188 ms | 93–163 µs |
| Node, concatenation | 233–303 ms | 422–532 ms | 308–512 µs |
| Node, Boolean intermediate | 209–271 ms | 265–485 ms | 246–674 µs |

Java post-GC heap increased by 6–27 kB from the post-warmup sample to the final block. For the large case, roughly 17 kB of the increase occurred in the first block and the last two increases were under 2 kB each in both runs. Node large and concatenation cases ended below their post-warmup samples. The Boolean case rose by 168–177 kB and fell in the final block in both runs. The measurements include retained test telemetry. These short runs leave long-duration retention and capacity open.

The 1 MiB byte gate remains the general profile bound. The derived limit applies to compiler output for the two measured model digests. Hard synchronous preemption, general semantic artifact maxima and additional host capacity require separate evidence.

## Result review

Independent architecture review of `678023aac9542f52b62c4b2e67065bbfff4b9a3d` accepted the bounded serial workload and checked both final reports, the fixture catalog calculation and child cleanup. The sampler offsets are relative to the sampler clock. The 512-node fixture short-circuits during invocation and does not measure full-tree evaluation. The harness code was integrated as reviewed. General production containment remains open.
