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
Ready for review
<!-- agent-workflow:end -->

## Implementation

Planning initialized before semantic implementation. Final design review resolves the implementation dependency; this is not a new user approval request.

The isolated Node contribution starts at `6de1b25543e55223289f5e5fd97da46b241f5ec3`. Model admission supports the exact existing event pair and the new decision/event pair. DECISION entrypoints require REQUEST/BEFORE_COMMIT, a required ALLOW/DENY enum field and zero write grant. Existing static and dynamic grant checks remain in use. No invocation API or CEL representation changes are needed.

The teaching consumer validates its known decision/message record, completed outcome, empty effects and consistent read-only trace. Invalid data returns BLOCKED. The version-selected scenario runner preserves the historical corpus and uses the compiler owner's new schema and bytes.

## Verification

The initial complete fast runtime suite passed 59 tests using source and 59 using the installed package namespace. Decision consumer and native-operation harness tests passed 48 cases. The existing installed event-host verifier and consumer tests passed 14 cases. No case was skipped.

All 112 supplied fixture manifest entries match the tested copy from compiler-owner staged tree `0da7d08ae163b4d9e29993f799a89547fb3a438b`. Source and package matched 39 Keir 3 scenarios and 51 vectors. The unchanged Keir 2 corpus matched 28 scenarios and 51 vectors. Complete Node and Java reports match after sorting scenario rows by group and ID and vector rows by ID, then comparing canonical JSON. Keir 3 normalized report SHA-256 is `b94f8b126f80dda2a77951c13f6eb098e3355c354f97e2e33b0f63d46ddce537`; Keir 2 remains `26bf1cf3d697497971ce1b753f02bd17090e7b5f1e3bf107f8ca7c3effedc4a2`.

The isolated package was built with `npm pack ./runtime-node --pack-destination .tools/packages --ignore-scripts` and installed offline in the example host. Initial package SHA-256 was `b2a9cceb8fca6cb1da31b6d2aabffc7e4c629cfe4a3df2bbda9ffaf8e2fe1c31`. The review-fix package has SHA-256 `b708433a3c64e46d2cfed8d0e737ca0ded1e07be687b98b28ff02f5ccf850e48`; its source, installed copy and package-test copy have identical validation bytes, and the example lock pins its new integrity. Combined documentation, release packaging and publication remain with the integration owner.

The teaching host keeps runtime outcome, decision consumption and native result separate. An ALLOWED consumer result reaches the simulated native transition once only after fresh host authority, selection, approval, input, fact-version and original-deadline checks. Authoritative absence ignores nonexistent guard binding metadata while retaining normal native validation. Selected unavailable artifacts, denied or rejected reads, cancellation, late settlement and changed state cause zero native attempts.

## Result review

Review identified two validation gaps in candidate `c5c6753c63ee060dd7fa19d5833d45b550f7ea3b`: list-valued additional decision fields were accepted, and mixed generation diagnostics selected the producer from the bound model rather than the artifact. Focused regressions reproduced both before the fix and pass afterward. Decision output now requires every declared field to be scalar while preserving optional scalars and event output lists. Envelope validation checks the supported artifact generation, its producer pin, then the bound model generation. All six invalid combinations of model, artifact and producer generations 2 and 3 assert the exact diagnostic code and path. The required decision field remains non-optional.

The review-fix fast suite passed 60 source tests and 60 tests through the installed package namespace. Its installed-package decision consumer and native-operation harness passed all 48 cases, and the existing installed event-host checks passed all 14 cases. The existing complete shared-corpus evidence above is retained; these fixes add negative admission and mixed-envelope checks without changing produced artifacts. Actual Java focused diagnostic comparison is requested from the compiler owner. The review delta remains uncommitted pending the integration owner's commit-metadata clarification. The workflow result-review check remains unsatisfied until independent technical review supplies its source, reviewed revision and verification assessment.

Independent implementation result review remains assigned to the manager and architect. Local scope and privacy checks found only the intended Node runtime, package metadata, tests and teaching files plus this record. The simulation demonstrates the example's native counters. Real host persistence, cancellation transport and transactional consistency remain separate host acceptance work.

The integration owner independently ran all 60 fast Node runtime tests against the combined compiler/Java/Node working tree; all passed without skips. Combined structural verification also passed 31 generation-1 fixtures with 19 structural rejections, 47 generation-2 fixtures with 35 rejections, and 73 generation-3 fixtures with 85 rejections. Independent architecture review is requested for the actual integrated source and the updated two-example release verifier; final Node teaching-runner and official package acceptance remain separate.

The final five-file teaching-runner delta is now integrated. The integration owner checked the supplied nine-file owner manifest, compared the complete Java/source-Node/installed-Node report objects for both retained generations, and verified all six diagnostic tuples and four standalone command outputs. All five focused example tests then passed in the combined integration checkout using the installed development package. Runtime validation and the release verifier were unchanged by this final delta. Official package smoke and independent result review remain open.

Independent architecture review subsequently passed the exact five-file teaching delta against its accepted hash manifest, closing the previously excluded Node portion. The review's one Java teaching-consumer identifier finding was separately corrected and passed re-review on the exact integrated source/test bytes. No other blocking Node semantic finding remains. Official Core 0.3 package and guide qualification still govern release acceptance.

