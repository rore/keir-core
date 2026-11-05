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

**Verification plan:** Run the opt-in suite on the supported Java and Node versions. Record outcomes, CPU, elapsed time, peak and retained memory and watchdog cleanup. Verify that late responses cannot change settled outcomes. Keep exact local paths and private evidence in ignored local records.

**Plan review:** Architecture review endorsed measuring the existing profile before selecting further limits or isolation. Observed timings do not establish a hard deadline guarantee.

**Approvals:** No additional approval required for isolated repository work.

**State:** Ready to implement
<!-- agent-workflow:end -->
