# Java HTTP load evidence integration

<!-- agent-workflow:start -->
**Outcome:** Readers can assess the measured Java HTTP capacity and reproduce the bounded workload without mistaking historical synthetic measurements for production guarantees.

**Target:** keir-core.

**Scope:** Canonical resource qualification documentation, a small opt-in HTTP benchmark harness and compact historical results under conformance/resource-qualification; this task's Work Record.

**Constraints:** No production compiler/runtime changes, global tooling, services, releases or deployments. Preserve historical Windows/serial evidence and the measured pre-hardening source identity. Keep raw timing samples, dependencies, compiled outputs and private operational details out of Git. Roadmap reconciliation remains with its manager owner.

**Completion criteria:** The canonical reference links accurate setup/workload/limitations and compact results; historical execution explicitly materializes the measured source; bounded smoke/correctness and cleanup, hash/result/link checks and independent review pass before authorized integration.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Readers can assess the measured Java HTTP capacity and reproduce the bounded workload without mistaking historical synthetic measurements for production guarantees.","scope":"Canonical resource qualification documentation, a small opt-in HTTP benchmark harness and compact historical results under conformance/resource-qualification; this task's Work Record.","constraints":"No production compiler/runtime changes, global tooling, services, releases or deployments. Preserve historical Windows/serial evidence and the measured pre-hardening source identity. Keep raw timing samples, dependencies, compiled outputs and private operational details out of Git. Roadmap reconciliation remains with its manager owner.","completion_criteria":"The canonical reference links accurate setup/workload/limitations and compact results; historical execution explicitly materializes the measured source; bounded smoke/correctness and cleanup, hash/result/link checks and independent review pass before authorized integration."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:runtime-resource-qualification"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The existing Redline policy leaves benchmark code and conformance documentation unclassified (gray). Historical-source reproduction and evidence transformation need independent review.

**Discovery:** Main is ce98892f8988158fa2fbae9c329eef3dc1e9504d. Measurements used 566caa4d4b826dc87e6493090bd5a53672dc26f0. The canonical reference already preserves Keir 2 serial Windows measurements. No open Core PR was found. Work proceeds under the explicit task assignment without role adoption or changes to installed tooling.

**Material assumptions:** The supplied evidence archive must be locally readable and match its expected SHA-256 before adapting its harness. If transfer fails, stop dependent implementation and publication; continue read-only repository discovery and planning. Manager-owned roadmap changes require coordination rather than assumed role authority.

**Plan:** Inspect the verified archive and measured source/fixture manifest, then name exact harness/result/documentation files before implementation. Preserve exact measured harness files where practical, isolating portability changes in a small repository-native wrapper. Materialize historical source from Git objects into ignored build storage and use existing task-local dependency caches. Add a compact result file and detailed page linked prominently from the existing README. Keep overload, warmup and generator-limit negatives visible. Smoke execution must bind loopback, use at most four outstanding requests and five seconds per case, supervise each direct child for at most 45 seconds with at most 1 MiB capture, and prove cleanup after both success and induced failure/timeout. Validate provenance, results, syntax, links and a bounded smoke run; obtain independent final review, reconcile the roadmap with its owner, and integrate using authorized normal Git delivery. Stop on source mismatch, missing input or unreviewed scope expansion.

**Verification plan:**
- Historical identity → archive SHA-256, source/fixture hashes and confirmation that compilation/execution use the measured Git commit.
- Compact results → reconcile totals and success/rejection/drop arithmetic against archived summaries.
- Documentation/code integration → syntax and relative-link checks.
- Smoke correctness → short native/Keir typical/dense cases compare complete expected outcomes; a deliberately incorrect expected outcome must fail.
- Bounded execution → loopback, duration/concurrency/capture/time bounds and child cleanup checks after success and an induced failure/timeout.
- Delivery → fresh Redline/Work Record validation, exact-final independent review and applicable CI. These checks do not claim a full product regression or a rerun of historical measurements.

**Plan review:** Agent technical review: see Plan review below. Conditional acceptance; archive-dependent exact file naming remains pending. No benchmark implementation has begun.

**Approvals:** Not required at this risk level. User-authorized normal validated delivery applies; no release or environment changes are included.

**Exceptions:** —

**State:** Blocked
<!-- agent-workflow:end -->

## Implementation

- Verified repository guidance, current process, source identities and canonical feature reference. No production changes or roadmap edits.
- The required source archive is unavailable. No readable archive bytes are available. Dependent harness implementation and publication are blocked.
- Current branch: main; last verified main: ce98892f8988158fa2fbae9c329eef3dc1e9504d. Next action: obtain the evidence package through a supported transfer, verify its archive/source identities, then complete plan review and implementation.

## Evidence

- `git fetch origin` and fast-forward main: passed; no concurrent local changes at pickup.
- GitHub open-PR query: none found.
- Minimap item-ref: exact source tuple captured above.
- Pre-edit Redline classification: gray benchmark paths, excluded Work Record, no boundary violation; Elevated floor.

## Plan review

Agent technical review: independent non-implementer reviewed main ce98892f8988158fa2fbae9c329eef3dc1e9504d, the initial Work Record (SHA-256 b5ab5ead0760e1169c2122a68e4c337fa74b765ba289a1401c3db57eaee772da), repository/workflow guidance, canonical resource reference and existing qualification/dense harnesses. Verdict: conditional acceptance; scope is proportionate and Elevated/Moderate is appropriate. No full product regression is required if scope stays within docs and the opt-in benchmark. Review conditions for smoke bounds, negative oracle check, cleanup on success/failure and historical identity are incorporated above. Exact target filenames must be settled after inspecting the archive. The reviewer did not inspect unavailable archive contents or certify HTTP results.

## Result review

Pending implementation and verification. Broader resource qualification remains active.
