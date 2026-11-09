# pre-operation-compiler-java

Owning feature: [pre-operation-guard](../../roadmap/features/pre-operation-guard.md).

<!-- agent-workflow:start -->
**Outcome:**
A host can compile a read-only pre-operation decision and execute its exact artifact in Java.

**Target:**
keir-core

**Scope:**
Compiler, Java runtime, DCM/artifact/scenario schemas, shared guard fixtures, Java teaching consumer and affected build/version selection. Node runtime and documentation prose are separately owned.

**Constraints:**
Keep Core domain-independent. Reuse existing source syntax, invocation APIs and descriptors. Preserve exact existing approved event artifacts and single-write behavior. Native commits remain host-owned.

**Completion criteria:**
Valid input-only and READ-derived ALLOW/DENY execute with empty effects; invalid lifecycle/output and any potential WRITE reject before dispatch; exact supported model/producer tuples are enforced; shared fixtures execute unchanged in Java and Node.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A host can compile a read-only pre-operation decision and execute its exact artifact in Java.","scope":"Compiler, Java runtime, DCM/artifact/scenario schemas, shared guard fixtures, Java teaching consumer and affected build/version selection. Node runtime and documentation prose are separately owned.","constraints":"Keep Core domain-independent. Reuse existing source syntax, invocation APIs and descriptors. Preserve exact existing approved event artifacts and single-write behavior. Native commits remain host-owned.","completion_criteria":"Valid input-only and READ-derived ALLOW/DENY execute with empty effects; invalid lifecycle/output and any potential WRITE reject before dispatch; exact supported model/producer tuples are enforced; shared fixtures execute unchanged in Java and Node."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:pre-operation-guard"}],"pull_requests":[]}

**Risk:**
Elevated

**Complexity:**
Large

**Reason:**
Unclassified contract and persistence integration paths require independent review; separately delivered runtime and host behavior must agree.

**Discovery:**
The reviewed contract reuses existing records/enums, bind/prepare/invoke and zero-write enforcement. Current validators admit only EVENT/AFTER_COMMIT/EVENT_REPORT. See docs/specs/pre-operation-decisions.md.

**Material assumptions:**
Existing invocation and bounded type machinery are sufficient; if implementation requires a new semantic/API contract, stop affected work for architect review while continuing independent work. Host local READs can be revalidated inside its short transaction; otherwise they cannot justify an authoritative ALLOW.

**Plan:**
Implement the reviewed lifecycle and exact compatibility pairs; publish shared fixtures for the Node owner; extend Java consumer and negative cases; run affected compiler/Java tests and shared packaged parity. Stop on a contract mismatch and obtain architect resolution.

**Verification plan:**
ALLOW/DENY and empty effects -> compiler-produced shared fixtures and Java execution. Static write/lifecycle rejection -> compiler, forged-artifact preparation and binder tests. Compatibility -> old event vectors and rejected mixed tuples. Cross-runtime identity -> identical fixture hashes and combined parity.

**Plan review:**
Agent technical review: the independent Core review passed design revision 9d2e593dc2215181593d467873e92cd0117bf402, with manager acceptance of the existing APIs, exact two-pair compatibility, zero-write lifecycle and host-owned continuation. Reviewed specification blob: 0cd4e9a904468798a3a233bb3f684ef2590c1100. Implementation result review remains required.

**Approvals:**
User authorized this slice, generic framework separation, proper QA and completion. No new environment changes or public visibility change are authorized.

**State:**
Ready for review
<!-- agent-workflow:end -->

## Implementation

Planning initialized before semantic implementation. Final design review resolves the implementation dependency; this is not a new user approval request.

The producer's staged implementation and current standalone examples have been combined for integration without publication. Shared current fixtures retain source language and descriptor recipe 2 while using the reviewed model/artifact 3 tuple. Historical execution fixtures remain unchanged. Standalone follow-up and decision examples compile with the actual new compiler and run with runtime-only Java dependencies.

The existing release verifier now compiles and executes both examples for the current model, checking Java and installed Node against the same expected output. The retained model-2 release path still runs its original follow-up smoke. No separate release script or new release option was added.

## Evidence

The producer reports 73 compiler, 81 runtime and 21 fast teaching-host tests passed, plus all four explicit opt-in host tests and 39 current scenarios with 51 vectors. Standalone example verification passed in the producer checkout; integration/package evidence remains separate.

The verifier's four existing focused tests passed. Its actual published Core 0.2.0 archive/coordinate/installed-package smoke also passed with the trusted release checksums and source revision. This protects the existing release path; it does not qualify the new packages. Official Core 0.3.0 assets and the new integrated decision smoke remain pending final integration and release provenance.

Independent architecture result review found one teaching-consumer parity gap: Java accepted a completed ALLOW trace containing step identifier `bad step`, while Node returned BLOCKED. The compiler/Java owner applied the existing step and capability identifier grammars and added malformed-identifier and exact-length boundary regressions. All four focused consumer tests passed without failures, errors or skips. Independent re-review passed the exact two-file patch SHA-256 `bb786a263e145d5a145f1d6a8485835108f7ed48bd06430fb68f3650336a4b41`; the integration owner verified the old blobs and copied the exact reviewed source/test bytes. That finding is closed. The rest of the inspected compiler/runtime semantic changes had no blocking finding. Current packages must still pass the installed smoke; no rebuilt teaching-JAR or official-package certification follows from this focused source check.

An additional focused public-Java-API probe used the existing runtime JAR and runtime dependencies without changing shared fixtures. A decision output containing a list field rejects with ADMISSION_INVALID at /binding/dcm. Required and optional Boolean, Timestamp and nominal identifier fields all bind successfully; the same list field under a valid event-report hook still binds. All cases dispatch zero callbacks. The integration owner inspected the retained probe and actual output; no broad suite was repeated. The actual six-row Java/Node mixed-generation diagnostic matrix was also independently compared without rerunning it.

Final release acceptance is complete. Integrated source `b232944` passed Core CI, including Java coverage and complete current Java/Node report parity. Existing release scripts built Core 0.3.0 from that exact commit, and actual packaged compiler, Maven Java runtime and installed Node runtime passed both standalone examples on identical compiled artifacts. The five downloaded Core assets matched the qualified build and trusted checksums. Guide edition 5 passed committed-source, deterministic, offline and download verification and ships with the [published release](https://github.com/rore/keir-core/releases/tag/v0.3.0). This closes the package dependency; host-native persistence remains Daymark's separately verified responsibility.

