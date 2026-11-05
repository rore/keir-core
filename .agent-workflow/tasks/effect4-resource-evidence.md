# Measure resource use for the single-mutation profile

<!-- agent-workflow:start -->
**Outcome:** Establish a measured resource envelope for the current scalar effect/4 profile on Java and Node.

**Target:** keir-core

**Scope:** Add an opt-in isolated qualification run using the public compiler and both runtimes, with existing conformance fixtures as its starting point.

**Constraints:** Keep the default test suite fast and preserve public APIs, execution semantics and host authority. Use test-owned processes and local fixtures only.

**Completion criteria:** Publish reproducible commands and measured CPU, peak and retained memory, elapsed time and cleanup within a declared workload. Identify any unenforced boundary with a focused follow-up.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Establish a measured resource envelope for the current scalar effect/4 profile on Java and Node.","scope":"Add an opt-in isolated qualification run using the public compiler and both runtimes, with existing conformance fixtures as its starting point.","constraints":"Keep the default test suite fast and preserve public APIs, execution semantics and host authority. Use test-owned processes and local fixtures only.","completion_criteria":"Publish reproducible commands and measured CPU, peak and retained memory, elapsed time and cleanup within a declared workload. Identify any unenforced boundary with a focused follow-up."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:runtime-resource-qualification"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Resource-heavy inputs and subprocess cancellation require finite workload limits and reliable cleanup. Release claims depend on how results are interpreted.

**Discovery:** Existing conformance establishes behavior across both runtimes. The accepted runtime design does not guarantee preemption of synchronous CEL by a deadline timer.

**Material assumptions:** Existing public APIs and test fixtures can exercise the admitted profile and test-owned compiler children. Report any boundary that cannot be exercised through those surfaces.

**Plan:** Declare input, concurrency and watchdog limits before execution. Exercise compiler, parse, prepare and invoke separately at admitted and rejected source, model, artifact and expression boundaries. Include repeated concatenation, warm runs, concurrent prepared handles on one model, expired and late adapter responses, and preservation of prior confirmed effects. Exercise compiler child timeout, cancellation, output cap and reaping independently. Record supported tool versions, measurement method and limitations. Route production changes through a separate reviewed plan if evidence shows a gap.

**Verification plan:**
- Measured envelope and cleanup → Run the opt-in suite on the supported Java and Node versions and retain outcome, CPU, elapsed time and memory in ignored build output.
- Late response safety → Compare settled receipts before and after releasing test-owned pending responses.
- Regression protection → Run both default suites.

**Plan review:** Agent technical review: see [Plan review](#plan-review) below.

**Approvals:** No additional approval required for isolated repository work.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

- Added the opt-in [resource qualification](../../conformance/resource-qualification/README.md) and its declared workload. Redline scope is Gray for the qualification code and Blue for testing documentation. The harness is outside both default test selectors.
- Compiled the workload with the public compiler API, checked matching CLI and fixture bytes, and supplied those artifacts to the public Java and Node runtime entry points. Measured parsing separately through the parsers used by those entry points.
- Added runtime-only checked-expression probes for 65 nodes, depth 17 and 513 total nodes. The 8,192/8,193-byte typed value pair keeps each scalar within the string ceiling.

## Verification evidence

Windows x64, Java 21.0.12 and Node 24.14.1. Three serial runs took 11.1, 10.8 and 9.3 seconds, including startup, the 1-second timeout probe and cleanup. Each run passed 21 compiler cases, 22 matching runtime facts and four direct-child supervision probes. All opened child handles reached close and the generated case directory was removed. Default Java passed 273 tests and Node passed 455. The first default runs hit the sandbox's loopback restriction. The same suites passed with access to their test-owned loopback endpoints.

| Sample from the first run | Elapsed | Process CPU | Heap sample |
| --- | ---: | ---: | ---: |
| Compiler, 65,536-byte source | 547 ms | 953 ms | 14.37 MB |
| Java, 1 MiB artifact parse | 89.2 ms | 203 ms | 17.04 MB |
| Node, 1 MiB artifact parse | 50.8 ms | 47 ms | 17.35 MB |
| Java, prepare 512 aggregate AST nodes | 32.9 ms | 125 ms | 18.26 MB |
| Node, prepare 512 aggregate AST nodes | 16.3 ms | 15 ms | 19.82 MB |
| Java, 30 warm aggregate invocations | 23.6 ms | 62.5 ms | 21.41 MB |
| Node, 30 warm aggregate invocations | 4.1 ms | Below accounting resolution | 23.13 MB |

Java heap samples sum memory-pool peaks. Node heap samples use phase endpoints, with process peak RSS of 111.14 MB. Compiler post-GC heap was 4.09 MB, compared with 1.76 MB at baseline. Java runtime ended at 8.71 MB, compared with 5.58 MB at baseline. Node ended at 12.74 MB, compared with 11.16 MB at baseline. The four-handle phase measured 8.74 MB live and 8.62 MB after release on Java, and 13.07 MB live and 12.70 MB after release on Node. These figures are process observations, including engine and host state.

The largest runtime-admitted artifact in this corpus was 26,157 bytes. The exact 1 MiB canonical probe reached structural rejection, and one additional byte triggered the byte gate. A valid near-ceiling artifact or a derived tighter reachable bound is still needed to qualify semantic admission at that size. Java whole-process RSS remains open. The measured adapter waits preserved UNKNOWN and prior CONFIRMED after late success. Hard preemption of synchronous CEL requires a separate design and qualification if it becomes a release requirement.

## Plan review

The architecture readiness review of Core `791cd0b` endorsed the bounded measurement plan, including compiler and runtime admission boundaries, shared model use, deadline settlement and child supervision. It called for measuring the current profile before choosing further limits or isolation. The result reviewer confirmed that the prior review covered the plan direction and principal cases.

## Result review

Independent architecture review approved the bounded measurement slice at `7c752209d87a9de3e6147a6ed0c24c4af0850fe3` after checking the harness, three saved reports and default-suite logs. The review verified the direct-child supervision and settled receipt claims. The harness code was integrated as reviewed. The owning roadmap item remains open for the supported production workload and containment decision.
