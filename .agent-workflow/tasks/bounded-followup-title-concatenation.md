# Add bounded string concatenation for follow-up titles

<!-- agent-workflow:start -->
**Outcome:** A single-write rule can append a fixed suffix to a task title through standard CEL string concatenation.

**Target:** keir-core

**Scope:** Admit plain String plus String for effect/4 in the compiler and Java and Node runtime validators, with shared conformance and release evidence.

**Constraints:** Preserve previous profiles, artifacts, host authority, resource limits and one-write behavior. Use the existing checked CEL evaluator. Keep numeric and list addition outside this increment.

**Completion criteria:** Compile a field plus literal to one artifact. Verify matching Java and Node behavior, including unsupported overloads, Unicode and value boundaries, and no write after expression failure.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A single-write rule can append a fixed suffix to a task title through standard CEL string concatenation.","scope":"Admit plain String plus String for effect/4 in the compiler and Java and Node runtime validators, with shared conformance and release evidence.","constraints":"Preserve previous profiles, artifacts, host authority, resource limits and one-write behavior. Use the existing checked CEL evaluator. Keep numeric and list addition outside this increment.","completion_criteria":"Compile a field plus literal to one artifact. Verify matching Java and Node behavior, including unsupported overloads, Unicode and value boundaries, and no write after expression failure."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:enrich-followup-expressions"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The change expands checked expression admission. Matching validation and bounded allocations matter across the compiler and both runtimes.

**Discovery:** The pinned effect/4 compiler and both runtime validators reject addition. The underlying CEL libraries provide a standard string overload.

**Material assumptions:** The existing CEL evaluator supports the same checked string overload in both runtimes and existing tree and value limits bound evaluation. Prove both before adopting it.

**Plan:** Check the exact checked overload and verify existing scope and bounds. Admit only the selected string overload, update specifications and add shared positive and negative cases. Verify source and packaged execution and hand off exact package provenance.

**Verification plan:** Expression and denial semantics → shared compiler, Java and Node conformance. Resource limits → largest bounded expression tree and value boundary checks. Compatibility → unchanged old goldens, legacy rejection, and older runtime rejection of the new checked artifact.

**Plan review:** Agent technical review: independent architecture review of the existing checked expression and task model paths accepted this split. The implementer checks exact paths, risk and compatibility before editing and returns material deviations for review.

**Approvals:** Not required at this risk level.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation candidate

Candidate `84763506ad501ca0c6cb01103c8053803ad8adf1` adds effect/4 string concatenation in the compiler and both runtimes. The developer reported 273 Java and 455 Node tests passing, shared source and installed package parity, and rejection of both new artifacts by the previous packages before dispatch. The largest tested concatenation tree produced 8,192 UTF-8 bytes in both evaluators, and the shared cases verify the 256-byte result boundary and no write after rejection.

The manager verified the frozen revision, all 16 evidence files and the package digests.

## Review findings

The initial independent review found the three checked expression gates consistent with the agreed limits and prior profile behavior. It asked for shared coverage of an oversized intermediate used in Boolean equality, an unselected oversized branch and the normal title artifact in simulation.

Candidate `9d7b5651b18f646c30e0efc448036579dad991e2` adds those cases through the existing drivers. The manager verified the six evidence files and compared all 35 complete rows across Java and Node source and package runs.

## Result review

Agent technical review: The independent architect accepted the checked expression gates, resource bounds, previous-profile compatibility and the added shared cases. The selected oversized result fails before the create call. The unselected oversized branch preserves the valid title, and the normal simulation matches the complete real-mode fixture result.

Reviewed revision: `9d7b5651b18f646c30e0efc448036579dad991e2`, directly above production candidate `84763506ad501ca0c6cb01103c8053803ad8adf1`.

Verification adequacy: Both reviews and exact frozen evidence support core integration. The focused Java package run uses the current test driver and fixtures with the previously built production package. Daymark's compatibility and live suffix journey require separate verification.

The [integrated revision](https://github.com/rore/keir-core/commit/9be8eb4af5694e14ab3efdc84c111c9449c0e68f) matches all 23 reviewed source and test paths. The [hosted contract suite](https://github.com/rore/keir-core/actions/runs/41997584091) and [Agent Workflow checks](https://github.com/rore/keir-core/actions/runs/41997583852) passed. Daymark consumed the verified packages for isolated integration.

The core increment is delivered. After confirming package consumption, the developer removed the task checkout with Git. The manager verified its removal and the retained branch, source bundle and recovery evidence.
