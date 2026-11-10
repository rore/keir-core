# Java HTTP load evidence integration

<!-- agent-workflow:start -->
**Outcome:** Readers can assess the measured Java HTTP capacity and reproduce the bounded workload without mistaking historical synthetic measurements for production guarantees.

**Target:** keir-core.

**Scope:** Canonical resource qualification documentation, a new opt-in Java HTTP benchmark harness and fresh compact measurements under conformance/resource-qualification; attributed historical conclusions and this task's Work Record.

**Constraints:** No production compiler/runtime changes, global tooling, services, releases or deployments. Preserve historical Windows/serial evidence and the measured pre-hardening source identity. Keep raw timing samples, dependencies, compiled outputs and private operational details out of Git. Roadmap reconciliation remains with its manager owner.

**Completion criteria:** The canonical reference links accurate setup/workload/limitations and fresh compact results tied to source and harness hashes; new bounded HTTP execution and cleanup, correctness-oracle rejection, result/link checks and independent review pass before authorized integration. Earlier conclusions remain explicitly author-reported and separately identified.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Readers can assess the measured Java HTTP capacity and reproduce the bounded workload without mistaking historical synthetic measurements for production guarantees.","scope":"Canonical resource qualification documentation, a small opt-in HTTP benchmark harness and compact historical results under conformance/resource-qualification; this task's Work Record.","constraints":"No production compiler/runtime changes, global tooling, services, releases or deployments. Preserve historical Windows/serial evidence and the measured pre-hardening source identity. Keep raw timing samples, dependencies, compiled outputs and private operational details out of Git. Roadmap reconciliation remains with its manager owner.","completion_criteria":"The canonical reference links accurate setup/workload/limitations and compact results; historical execution explicitly materializes the measured source; bounded smoke/correctness and cleanup, hash/result/link checks and independent review pass before authorized integration."}

**Behavior changes:**
[{"target":"task-context.scope","classification":"requirement-change","before":"Canonical resource qualification documentation, a small opt-in HTTP benchmark harness and compact historical results under conformance/resource-qualification; this task's Work Record.","after":"Canonical resource qualification documentation, a new opt-in Java HTTP benchmark harness and fresh compact measurements under conformance/resource-qualification; attributed historical conclusions and this task's Work Record.","reason":"Task owner revised delivery to independent new implementation and execution; the historical archive remains unavailable and must not be retried or bypassed.","impact":"New evidence qualifies only the new harness/current source; no direct revalidation or reproduction of the prior measured harness is claimed.","alternatives":"Remain blocked on the old archive or document historical conclusions alone.","authority":{"scope":"task","name":"task-owner"},"approval":{"by":"user","reference":"task-owner-followup","verbatim":"Build a small opt-in HTTP benchmark yourself from current Keir public APIs and the repository's existing fixtures/resource-probe documentation. Run a fresh bounded sample in this authorized environment. These are NEW measurements and NEW harness code."}},{"target":"task-context.completion_criteria","classification":"requirement-change","before":"The canonical reference links accurate setup/workload/limitations and compact results; historical execution explicitly materializes the measured source; bounded smoke/correctness and cleanup, hash/result/link checks and independent review pass before authorized integration.","after":"The canonical reference links accurate setup/workload/limitations and fresh compact results tied to source and harness hashes; new bounded HTTP execution and cleanup, correctness-oracle rejection, result/link checks and independent review pass before authorized integration. Earlier conclusions remain explicitly author-reported and separately identified.","reason":"Task owner revised delivery to independent new implementation and execution; the historical archive remains unavailable and must not be retried or bypassed.","impact":"New evidence qualifies only the new harness/current source; no direct revalidation or reproduction of the prior measured harness is claimed.","alternatives":"Remain blocked on the old archive or document historical conclusions alone.","authority":{"scope":"task","name":"task-owner"},"approval":{"by":"user","reference":"task-owner-followup","verbatim":"Build a small opt-in HTTP benchmark yourself from current Keir public APIs and the repository's existing fixtures/resource-probe documentation. Run a fresh bounded sample in this authorized environment. These are NEW measurements and NEW harness code."}}]

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:runtime-resource-qualification"}],"pull_requests":["https://github.com/rore/keir-core/pull/14"]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Benchmark paths are gray under existing Redline. New load scheduling, full-outcome assertions and bounded supervision warrant Elevated/Moderate review.

**Discovery:** Main advanced to d484c46cc416ad98aa4753d6759586d48d833e01, including separate current-generation concurrency qualification. Preserve that work. Historical HTTP conclusions refer to 566caa4d4b826dc87e6493090bd5a53672dc26f0 and have not been directly revalidated here. New measurements will use repository public compiler/runtime APIs, current fixtures and independently written harness code.

**Material assumptions:** Historical archive access is excluded: no retries or alternate transfer. Available task-local Java/Node/build caches support fresh execution. Inspect actual CPU affinity and cgroup controls; stop on resource/time/output breach rather than enlarge limits. Manager-owned roadmap reconciliation must be handed to its owner.

**Plan:** Add java-http/{HttpProbe.java,run.mjs,README.md,results.json} under conformance/resource-qualification and a prominent link/summary in the existing README. HttpProbe uses public Keir compile/bind/prepare/invoke APIs, Jackson core already in runtime dependencies, loopback JDK HTTP server, 32 admitted operations, virtual HTTP handlers and controlled delayed callbacks. Prepare once outside measured requests. Compile the ordinary fixture and a generated valid dense 473-node membership rule; verify AST shape and fixture pins. Independently construct full expected traces/outcomes and check per-invocation input/actor/event/native-key identity. Compare manual native logic using equivalent callbacks/trace construction. run.mjs supervises Java with two-CPU affinity, 512 MiB heap, 1 GiB observed RSS watchdog, 1 MiB capture and 300-second workload budget. Use open-loop arrivals with planned-arrival latency/dispatch lag, at most 512 outstanding, at most 4800 offered/sec, at most 60 seconds per stage; label generator guard drops separately. Record warmup, short ramps, up to two 30-second native/Keir confirmation pairs, overload/slow callbacks and recovery. Smoke uses at most four outstanding and one second per case; demonstrate bad expected outcome rejection and induced child timeout cleanup. Hash source inputs, runtime source tree, built jars and harness files. Keep samples/build outputs ignored, publish compact stages. Final independent review, fresh policy/workflow checks and applicable CI precede authorized normal delivery. Prepare the exact roadmap note for its manager owner.

**Verification plan:**
- New source identity → Git revision, runtime/compiler Git tree IDs, fixture pins, generated artifact/expected hashes, jar/harness SHA-256.
- Historical attribution → supplied author conclusions retain old source identity and are not labeled directly revalidated.
- Compact evidence → reconcile offered = correct + busy + generator drops + transport errors + mismatches/failures, recompute raw percentile summaries before discarding samples.
- Smoke correctness → native/Keir typical/dense HTTP cases, complete-outcome/context assertions and an intentionally wrong expected result that must be rejected.
- Bounds/cleanup → loopback, affinity, duration/concurrency/capture/RSS watchdogs and child-close receipts after success and induced timeout.
- Integration → relative links, syntax, fresh Redline/Work Record check, independent exact-final review and applicable CI; no full product regression claim.

**Plan review:** Agent technical review: revised independent plan accepted conditionally at 2f4988dadf6dca1e67984ae9c9098e6916adda93 (Work Record SHA-256 3768fd6716fdd3f26dc32bfc9da5c9300c92e9e0130616e38e8199af6804f937). Stage accounting, equivalent shared native boundaries, recovery/control verification and removal of stale recovery notes are incorporated below.

**Approvals:** Not required at this risk level. User-authorized normal validated delivery applies; no release or environment changes are included.

**Exceptions:** —

**State:** Ready for review

<!-- agent-workflow:end -->

## Implementation

- Verified repository guidance, current process, source identities and canonical feature reference. No production changes or roadmap edits.
- Original archive-dependent approach stopped. The revised task excludes archive access and authorizes independent new code and measurements.
- Current branch: main; reviewed delivery revision c22eef77b97f8efb9b474f500f2f9a3560eb2502 includes current main d484c46. Implementation and diagnostic evidence validation are complete. Final workflow validation and exact-head CI passed. The user explicitly approved the draft-to-ready transition and normal merge; the manager-owned roadmap note remains pending handoff. Historical raw data is not an input.

## Evidence

- `git fetch origin` and fast-forward main: passed; no concurrent local changes at pickup.
- GitHub open-PR query: none found.
- Minimap item-ref: exact source tuple captured above.
- Pre-edit Redline classification: gray benchmark paths, excluded Work Record, no boundary violation; Elevated floor.

## Plan review

Agent technical review: independent non-implementer reviewed main ce98892f8988158fa2fbae9c329eef3dc1e9504d, the initial Work Record (SHA-256 b5ab5ead0760e1169c2122a68e4c337fa74b765ba289a1401c3db57eaee772da), repository/workflow guidance, canonical resource reference and existing qualification/dense harnesses. Verdict: conditional acceptance; scope is proportionate and Elevated/Moderate is appropriate. No full product regression is required if scope stays within docs and the opt-in benchmark. Review conditions for smoke bounds, negative oracle check, cleanup on success/failure and historical identity are incorporated above. Exact target filenames must be settled after inspecting the archive. The reviewer did not inspect unavailable archive contents or certify HTTP results.

## Result review

Agent technical review: independent read-only non-implementer review of the six-file integration against d484c46cc416ad98aa4753d6759586d48d833e01; technical acceptance with no code/evidence blocker.

Reviewed revision: c22eef77b97f8efb9b474f500f2f9a3560eb2502.

Verification adequacy: independently recomputed all 22 load stages' raw percentile distributions and accounting/throughput/CPU arithmetic; verified compact original-report hashes/object equality, fixture/generated/binary/harness/source-tree identities, eight strict smoke stages and cleanup receipts, commissioning references, tables, relative links, unchanged production sources and public-content hygiene. Adequate for the stated bounded diagnostic. Both load reports correctly retain failed status; evidence validation certifies their accuracy, not capacity qualification. Graceful shutdown/actual-server timeout and port-release evidence comes from separate strict smoke. The two transport failures remain unexplained; real application capacity and general resource guarantees remain unqualified. Manager roadmap reconciliation is pending; feature remains active.

## Revised assignment

The task owner authorized independent new harness code and fresh execution instead of historical-archive reproduction. No further archive transfer will be attempted. Source-only discovery found five available logical CPUs, a shared four-core cgroup CPU quota and a 16 GiB cgroup memory limit; these are observations, not exclusive allocation. The existing manager-owned roadmap feature remains active.

Revised review conditions: offered counts follow monotonic scheduled arrivals independently of completions. Stop arrivals then drain at most five seconds; unfinished requests become transport failures. Preserve busy, drops, transport errors and incorrect outcomes separately. Native and Keir share HTTP parsing/serialization, 32-slot admission, callback delays/context checks, outcome assertion boundary and telemetry. This comparison includes shared overhead and does not isolate CEL cost. Record distinct generator/server affinity. Confirm overload accounting, clean recovery, zero admitted operations/pending callbacks, executor shutdown, child close and port release. RSS is a 100 ms sampled watchdog with kill response, not hard memory containment.

## Fresh harness implementation

Implemented the reviewed public-API host and open-loop generator. The first commissioning runs exposed an incorrect artifact-shape assumption, a CPU-list formatting assumption and extra host bindings on the no-call dense program; these harness defects were corrected before sampling. Absolute request deadlines now include connection/agent waiting. A separate idle Java HTTP server is deliberately timed out; child close and port release are verified. Successful server shutdown also verifies executor termination, zero active operations/pending callbacks and all 32 permits returned. Collection completion is distinct from each capacity-screen verdict. Existing JDK HTTP TCP_NODELAY support was checked locally and enabled consistently for both engines.

Offline packaging initially failed because this Java image lacks release metadata. Packaging succeeded in 12.263 seconds using the same Java 21 compiler with explicit source/target 21 and an empty release property; no tooling/environment installation or production source change was needed. First corrected smoke: 40/40 complete outcomes/context checks correct, negative oracle rejected, shutdown/timeout cleanup and port release passed. Exact-final smoke follows any further harness change.

## Sample recovery

Measured harness 432307b5ff570ecec83946a60377e40c108bbf4e completed 129 seconds of scheduled arrivals before the dense 600/s confirmation ended with two transport failures, zero incorrect outcomes and no resource breach. This failed run is preserved; no clean capacity claim applies to it. Initial code stopped on any transport failure after reconciling all client/server counters. A narrow control-flow fix retains reconciled transport errors as failed capacity screens, records their error codes and still stops on incorrect outcomes/counter mismatch/resource breaches. The unchanged Java host and fixtures were used for a focused 108-second completion: lower-rate ordinary 300/s pair, missing native dense 600/s baseline, slow callbacks and recovery. It retained failed status because recovery latency/dispatch lag missed the screen. Combined primary/followup arrival time was 237 seconds, within the roughly five-minute sample budget. No further load rerun is planned; proceed to validated integration and manager handoff.

## Verification of recorded evidence

- Exact measured source/harness revisions: `432307b5ff570ecec83946a60377e40c108bbf4e` (primary) and `25ec2ed590a461abee2d1035a313950bd6329be4` (focused follow-up). Java probe SHA-256 is `8f0bd719bb551edc042b4a4d7f68ae96a043684c008f6318c7441eea4067d691`; Node producer hashes remain in each run. Compiler/runtime Git trees match hardened main `d484c46cc416ad98aa4753d6759586d48d833e01`.
- Strict smoke on both frozen harness revisions: 40/40 requests correct, deliberately wrong expected outcome rejected, successful executor shutdown/returned permits, actual idle-server timeout/reaping and port release passed.
- Primary/follow-up: 22 load stages, 237 seconds of arrivals; 173,100 offered = 163,689 correct + 3,253 busy + 6,156 generator drops + two transport failures + zero incorrect outcomes. Raw nearest-rank p50/p95/p99, offered/result/throughput/CPU arithmetic, original report hashes, fixture/generated/binary/harness hashes and every child-close receipt checked locally. No observed RSS breach. Raw samples remain ignored; compact results retain all commissioning/measurement failures.
- Both load collections retained failed status: the primary stopped on two uncategorized transport failures; the focused follow-up completed all recovery requests correctly but failed the latency/dispatch-lag screen. Failure cleanup killed and reaped each JVM. Neither measured run reached graceful-shutdown probes; separate strict smoke verifies those probes. No further load rerun is planned.
- Clean fresh ordinary Keir point: 9,000/9,000 at 300 offered/s for 30 seconds, p99 12.7750 ms. Higher-rate busy responses, dense saturation/two transport failures and recovery latency miss remain explicit. One trial and synthetic callbacks establish no maximum, production SLA, leak freedom, hard containment or real-database qualification.
- `node --check conformance/resource-qualification/java-http/run.mjs`: passed. Offline source packaging passed with the recorded Java 21 source/target flags. `python3 scripts/check-keir-contracts.py`: passed (/1, /2 and /3 schemas/rejections); this is not a full native product regression.

## Roadmap contribution for manager owner

Canonical feature: `runtime-resource-qualification`, `roadmap/features/runtime-resource-qualification.md`, active. This task does not adopt a manager identity or edit that owner's file. Proposed exact addition under Notes:

> **Java HTTP diagnostic recorded:** The [HTTP load reference](../../conformance/resource-qualification/java-http/README.md) and [compact results](../../conformance/resource-qualification/java-http/results.json) add a repository-native opt-in harness and fresh hardened-source evidence: 237 seconds of scheduled arrivals, 173,100 offered, zero incorrect outcomes, with busy responses, generator drops, two transport errors and failed latency/recovery screens preserved. One ordinary Keir point completed 9,000/9,000 at 300/s for 30 seconds (12.7750 ms success p99); it is a tested synthetic point, not a maximum or production SLA. Earlier 566caa4 HTTP conclusions remain explicitly author-reported and not directly revalidated by this integration. The broader qualification feature remains active.

Manager reconciliation is pending acceptance of this focused contribution; no broad feature-completion claim is made.

## Delivery

[PR #14](https://github.com/rore/keir-core/pull/14) carries the independently reviewed six-file integration; its live PR/checks provide the normal merge receipt. The current change adds only this PR relationship and delivery/check evidence to the accepted diff. The manager-owned active roadmap contribution above remains pending owner reconciliation.

## Delivery checkpoint

Exact-head integration review accepted `6852adff917368f5195e5036a87a88f0c66bf24f` against main `4fab86b3d1f7baf0c4941c09a84116232f2eef5e`. The updated Keir contract CI (run 42873484282) and Agent Workflow CI (run 42873484310) passed on that head, including the separate bounded resource/concurrency checks. No review comments or unresolved threads were present.

The user subsequently explicitly approved marking draft PR #14 ready and merging it after rechecking the head, CI and review/conflict state. The reviewed head remained unchanged and its two CI workflows were green, with no review findings. Latest main changed only the root README; preserve it by normal merge, refresh exact-head checks, then retry the previously unexecuted ready transition once and complete normal integration. Manager roadmap reconciliation is still pending.

Task-created expanded run directories and transfer helpers were removed after verifying a compact ignored raw-evidence backup against original report bytes. No task worktrees or services were created. Retention is justified for manager acceptance and evidence audit; no duplicated Core source trees, compiled probe classes or raw samples are tracked.
