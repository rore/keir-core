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

**State:** Ready to implement
<!-- agent-workflow:end -->

## Plan review

Architecture review of the landed measurement slice recommended a bounded serial workload, JVM process-wide memory measurement and a valid artifact check against a derived bound. It approved reusing the current harness and existing boundary evidence. The qualification target defers hard interruption of synchronous CEL and arbitrary host code. Measurements do not establish a general containment or throughput guarantee.
