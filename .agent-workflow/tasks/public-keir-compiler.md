# Compile the public Keir contract

Owning feature: [Public package release](../../roadmap/features/public-package-release.md).
Design: [Public contract normalization](../../docs/internal/public-contract-normalization.md).

<!-- agent-workflow:start -->
**Outcome:** Compile the single public Keir contract and supply exact shared fixtures for both runtimes.

**Target:** keir-core

**Scope:** Compiler entry points and validation, source/DCM/artifact/scenario schemas, shared fixtures and focused compiler/schema checks. Port relevant compiler behavior coverage and remove superseded compiler selectors and parsers.

**Constraints:** Keep current single-mutation semantics and deterministic lowering. Use the normal compiler invocation. Keep existing preview data and published releases intact. Use an isolated integration worktree until both runtimes and dependent consumers build.

**Completion criteria:** The normal CLI and Java compiler emit the same current artifact from the same source and DCM. Obsolete identifiers, selectors and redundant fields fail closed. Both runtime teams receive the same committed artifact bytes, digests and scenario expectations.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Compile the single public Keir contract and supply exact shared fixtures for both runtimes.","scope":"Compiler entry points and validation, source/DCM/artifact/scenario schemas, shared fixtures and focused compiler/schema checks. Port relevant compiler behavior coverage and remove superseded compiler selectors and parsers.","constraints":"Keep current single-mutation semantics and deterministic lowering. Use the normal compiler invocation. Keep existing preview data and published releases intact. Use an isolated integration worktree until both runtimes and dependent consumers build.","completion_criteria":"The normal CLI and Java compiler emit the same current artifact from the same source and DCM. Obsolete identifiers, selectors and redundant fields fail closed. Both runtime teams receive the same committed artifact bytes, digests and scenario expectations."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** The compiler and schemas define the artifact admitted by both runtimes. The format cutover preserves the existing execution limits and host authority.

**Discovery:** The revised design specifies one closed contract and removes the old profile selectors. The current compiler and older conformance callers still reference development formats; exact callers and integration order need review.

**Material assumptions:** Both runtimes can use shared compiler-owned vectors. Removing an old entry point must be coordinated with its callers and the shared suites before delivery to main.

**Plan:** Inventory normal compiler entry points, old-profile branches and their callers. Port the current semantic tests to the new contract, make the ordinary compiler invocation accept only the new source and DCM, and freeze the schemas and shared output in an isolated checkout. Publish the fixture revision to both runtime owners after independent review. Remove branches specific to obsolete formats after porting relevant behavior.

**Verification plan:**
- Compile the same new source/DCM through the normal CLI and Java API, compare artifact bytes, and validate all four new document shapes.
- Verify nominal types, concatenation, guard and handler behavior, and second possible write rejection. Check obsolete inputs, selectors and stray fields fail closed.
- Share committed artifact and scenario bytes with both runtime owners. Require shared conformance before integrating the release change.

**Plan review:** Pending the concrete compiler plan and independent technical review.

**Approvals:** The user approved the single public contract and clean cutover for Core and Daymark.

**State:** Blocked or returned to planning
<!-- agent-workflow:end -->
