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

**Plan review:** Agent technical review: [Reviewed implementation plan](#reviewed-implementation-plan). The exact plan passed.

**Approvals:** The user approved the single public contract and clean cutover for Core and Daymark.

**State:** Ready for review
<!-- agent-workflow:end -->

## Reviewed implementation plan

The independent review passed for plan SHA-256 `ae9609c61835dc58e54c5487af1980b01e93043fcee63fc7634805647032ff37` against compiler base `1e96300`. The plan matches the accepted compiler and scenario rules in Core `4a7b59c`. It ports current compiler behavior, tests strict scenario bytes at their boundaries, and freezes one compiler-produced corpus for both runtimes. Compiler-only checks can run before integration. The combined release must also port or retire the old callers in the runtime tests, teaching hosts, qualification tools, and CI before it moves to main.

## Compiler result review

Independent result review passed at `5e7ba9c9d146b5cb5d8d462aae6502b42ce9170a`. The offline compiler package passed 59 tests. All eight artifacts matched between the Java API and packaged CLI. Structural checks passed 31 positive and 19 negative cases. The fixture root is `conformance/keir-1`, with adjacent SHA-256 pins, 20 scenario documents and 45 raw phase vectors. The compiler and corpus are frozen for both runtime tasks. Combined runtime, teaching host, qualification, and CI verification remain open.
