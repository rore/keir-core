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
Ready to implement
<!-- agent-workflow:end -->

## Implementation

Planning initialized before semantic implementation. Final design review resolves the implementation dependency; this is not a new user approval request.

