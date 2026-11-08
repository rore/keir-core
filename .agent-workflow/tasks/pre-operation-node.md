# pre-operation-node

Owning feature: [pre-operation-guard](../../roadmap/features/pre-operation-guard.md).

<!-- agent-workflow:start -->
**Outcome:**
The Node runtime executes the same read-only pre-operation decision artifacts as Java.

**Target:**
keir-core

**Scope:**
Node model/artifact validation and invocation, Node teaching consumer/harness and tests, Node package metadata. Compiler, Java, shared schemas/fixtures and prose documentation have separate owners.

**Constraints:**
Keep generic Core free of task semantics. Use the compiler owner's exact shared artifacts. Preserve existing event effects, protected bindings, deadlines and declared compatibility. No new invocation wrapper or public cancellation API.

**Completion criteria:**
The same shared bytes yield matching outcomes, calls, empty effects and host-consumer status in Java and Node; all invalid decision/write/expiry/mixed-version cases fail closed; existing event artifacts still run.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"The Node runtime executes the same read-only pre-operation decision artifacts as Java.","scope":"Node model/artifact validation and invocation, Node teaching consumer/harness and tests, Node package metadata. Compiler, Java, shared schemas/fixtures and prose documentation have separate owners.","constraints":"Keep generic Core free of task semantics. Use the compiler owner's exact shared artifacts. Preserve existing event effects, protected bindings, deadlines and declared compatibility. No new invocation wrapper or public cancellation API.","completion_criteria":"The same shared bytes yield matching outcomes, calls, empty effects and host-consumer status in Java and Node; all invalid decision/write/expiry/mixed-version cases fail closed; existing event artifacts still run."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:pre-operation-guard"}],"pull_requests":[]}

**Risk:**
Elevated

**Complexity:**
Large

**Reason:**
Unclassified contract and persistence integration paths require independent review; separately delivered runtime and host behavior must agree.

**Discovery:**
Current Node invocation and typed output suffice; validation currently restricts lifecycle and exact producer/model tuples. The new contract must derive zero writes for decisions and reject handled business rejection even when its result says ALLOW.

**Material assumptions:**
Existing invocation and bounded type machinery are sufficient; if implementation requires a new semantic/API contract, stop affected work for architect review while continuing independent work. Host local READs can be revalidated inside its short transaction; otherwise they cannot justify an authoritative ALLOW.

**Plan:**
Implement the frozen contract against compiler-produced fixtures; validate both exact supported pairs; add decision consumer and native-counter harness; verify focused tests, existing event cases and packaged Java/Node parity. Stop for changed fixture semantics or new public API requirements.

**Verification plan:**
Shared parity -> same artifact hashes and complete outcome/call/effect comparisons. Malformed/handled-rejection outcome -> consumer tests and zero native-counter assertions. Deadline/late reply -> deterministic clock/adapter tests. Existing event behavior -> complete fast runtime regression.

**Plan review:**
Agent technical review: the independent Core review passed design revision 9d2e593dc2215181593d467873e92cd0117bf402, with manager acceptance of the existing APIs, exact two-pair compatibility, zero-write lifecycle and host-owned continuation. Reviewed specification blob: 0cd4e9a904468798a3a233bb3f684ef2590c1100. Implementation result review remains required.

**Approvals:**
User authorized this slice, generic framework separation, proper QA and completion. No new environment changes or public visibility change are authorized.

**State:**
Ready to implement
<!-- agent-workflow:end -->

## Implementation

Planning initialized before semantic implementation. Final design review resolves the implementation dependency; this is not a new user approval request.

