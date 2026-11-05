# Run the public Keir contract in Node

Owning feature: [Public package release](../../roadmap/features/public-package-release.md).
Design: [Public contract normalization](../../docs/internal/public-contract-normalization.md).

<!-- agent-workflow:start -->
**Outcome:** The Node runtime binds and executes the single public Keir contract using the exact shared artifacts.

**Target:** keir-core

**Scope:** runtime-node production validation, execution APIs, and directly owned tests and fixtures. Retire old format branches and port relevant safety tests.

**Constraints:** Preserve the current host-bound API and execution semantics. Consume the compiler-owned corpus without regenerating it. Keep the other runtime, external hosts, CI, published packages, and Daymark out of this slice.

**Completion criteria:** The runtime matches independent outcomes and ordered calls for the same compiler-produced bytes. Old envelopes and producers, invalid metadata, model mismatches, and invalid scenario inputs fail closed.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"The Node runtime binds and executes the single public Keir contract using the exact shared artifacts.","scope":"runtime-node production validation, execution APIs, and directly owned tests and fixtures. Retire old format branches and port relevant safety tests.","constraints":"Preserve the current host-bound API and execution semantics. Consume the compiler-owned corpus without regenerating it. Keep the other runtime, external hosts, CI, published packages, and Daymark out of this slice.","completion_criteria":"The runtime matches independent outcomes and ordered calls for the same compiler-produced bytes. Old envelopes and producers, invalid metadata, model mismatches, and invalid scenario inputs fail closed."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Runtime admission gates must preserve host authority and resource limits while retiring development formats.

**Discovery:** Compiler and fixtures passed independent review at `5e7ba9c`. Runtime-specific caller and coverage inventory remains part of the implementation plan.

**Material assumptions:** Both runtimes can share the compiler-owned fixture bytes. Shared harness, teaching-host and CI changes are combined separately before integration.

**Plan:** Trace format gates and obsolete callers, keep the current evaluator and typed host path, and verify current-only binding and preparation with shared fixture bytes. Preserve protected context, uncertainty and resource checks. Identify the runtime-owned harness work and hand off any shared caller dependencies without changing their ownership.

**Verification plan:**
- Use the frozen corpus to compare full outcomes, trace, ordered dispatch and consumer reports.
- Reject old and mismatched format inputs before dispatch. Preserve tests for malformed metadata, unknown effects, and bounds before affected calls.
- Verify shared raw scenario vectors at their specified failure phases. Run focused runtime checks before combined conformance.

**Plan review:** Pending a concrete runtime plan and independent technical review.

**Approvals:** The user approved the single-contract cutover and the existing host-owned execution guarantees remain in force.

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->
