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

**State:** Ready to implement
<!-- agent-workflow:end -->
