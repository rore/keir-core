# Run bounded workflow expressions in Node

Owning feature [Bounded follow-up expressions](../../roadmap/features/enrich-followup-expressions.md). Companion contribution [Compiler and Java runtime](workflow-core-java.md).

<!-- agent-workflow:start -->
**Outcome:** Admit and execute compiler-produced Keir 2 workflow artifacts in Node with the same public outcomes, calls and effect evidence as Java.

**Target:** keir-core

**Scope:** Node model and artifact admission, checked expressions, value conversion, evaluation and Node tests.

**Constraints:** Retain the public host API, protected authority, resource ceilings, deadlines, receipts and uncertainty. Use the pinned CEL and protobuf libraries and the compiler owner's exact fixtures. Work on the isolated candidate branch. The manager owns integration and release.

**Completion criteria:** Node independently rejects incompatible pins, forged metadata, unsafe optional reads and malformed or over-budget values. Shared Keir 2 scenarios match expected outcomes, trace, receipts and dispatch counts. Escaping Timestamp overflow yields the budget diagnostic and preserves earlier confirmed effects. Masked overflow leaves the expression result intact.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Admit and execute compiler-produced Keir 2 workflow artifacts in Node with the same public outcomes, calls and effect evidence as Java.","scope":"Node model and artifact admission, checked expressions, value conversion, evaluation and Node tests.","constraints":"Retain the public host API, protected authority, resource ceilings, deadlines, receipts and uncertainty. Use the pinned CEL and protobuf libraries and the compiler owner's exact fixtures. Work on the isolated candidate branch. The manager owns integration and release.","completion_criteria":"Node independently rejects incompatible pins, forged metadata, unsafe optional reads and malformed or over-budget values. Shared Keir 2 scenarios match expected outcomes, trace, receipts and dispatch counts. Escaping Timestamp overflow yields the budget diagnostic and preserves earlier confirmed effects. Masked overflow leaves the expression result intact."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:enrich-followup-expressions"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Gray runtime paths enforce public input, checked expression and effect boundaries. The change coordinates admission, descriptor construction and evaluation across the existing public API.

**Discovery:** The frozen workflow contract selects the Keir 2 format tuple and unchanged CEL/protobuf pins. Pinned CEL merges escaping Boolean errors by retaining the first error's expression ID and replacing its cause. Positive artifact AST IDs leave a reserved internal ID for the typed Timestamp range error. The protobuf JSON codec needs raw value validation before conversion to preserve omission, enforce bounds and reject noncanonical dates.

**Material assumptions:** The compiler owns canonical fixture bytes and descriptor rules. Recheck exact pins and full scenario results against the final producer revision. Return to contract review if the runtimes disagree on the same bytes or CEL no longer preserves the reserved error ID through Boolean merges.

**Plan:** Cut model and artifact admission over to Keir 2. Derive deterministic descriptors from the admitted model. Validate checked types, references, overloads and local presence proof independently of CEL planning. Replace only the Timestamp-plus-Duration binding. Convert and bound values at the existing runtime boundaries. Migrate the Node test runners to the producer corpus and compare exact results before handoff.

**Verification plan:**
- Public outcomes, receipts and dispatch counts → run the producer's pinned scenarios and rejection vectors.
- Closed checked metadata and value admission → exercise forged IDs and overloads, optional false and empty values, omission, list bounds, semantic types and strict Timestamp inputs.
- Timestamp error behavior → check Boolean masking and escaping overflow through public invocation after a confirmed WRITE.
- Descriptor identity and preserved host behavior → compare the Inventory descriptor digest and run the authority, deadline, receipt and transport regressions.

**Plan review:** Agent technical review: the frozen workflow contract and architect review qualified the narrow Timestamp binding. The architect verified the pinned error merge behavior and required positive checked AST IDs, private marker classification and public API tests for masking, propagation and receipt preservation.

**Approvals:** The manager authorized the isolated Node contribution and assigned this record. Human plan approval is unnecessary at Elevated risk.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

The isolated branch `feat/workflow-core-node` starts at `b747ed761b0863965e44700a1de5de1623989b08`. Node independently validates Keir 2 model, artifact and checked metadata, then evaluates using the existing host-bound API. Timestamp dependencies are included for record fields or capability parameters. Optional omission is limited to authored arguments and record fields. The manager designated this record after the initial implementation checkpoint.

Enum membership uses the declared element type or the direct member-literal refinement. The parity follow-up rejects a forged conditional String needle even when both arms name valid enum members.

## Verification

- The isolated Node suite passed 54 tests including the loopback transport test on runtime revision `d74de6ba634c0d2192eec65d8531ec900df2d2b0` with the exact fixture bytes from producer commit `99cb6297a1b6a24830b60e776c541a4d3abb57f6`. Public overflow regressions cover direct comparison, conditional propagation, nested Boolean errors, both masking orders, a forged negative AST ID and an unrelated adapter failure after earlier masked overflow.
- Node and Java produced identical reports for 28 scenarios and 51 validation vectors using the frozen archive. The comparison sorted scenario rows by group and ID and validation rows by ID, then compared canonical JSON bytes. The report SHA-256 is `26bf1cf3d697497971ce1b753f02bd17090e7b5f1e3bf107f8ca7c3effedc4a2`. The Inventory algebra case retained its confirmed WRITE after overflow at `/steps/6`. The Inventory enum case returned its typed list with zero dispatches.
- The Inventory DCM digest is `sha256:77c67ad6a82149d461181d5c4ea661763a84b7055c6589bcce8049710362d959`. Node derived the same 834-byte Inventory descriptor set as the compiler, with SHA-256 `8a352e5893c7b33d15e325ede52b25d0ac7063ce8bd21a0af71f45985182e87a`.
- Compiler artifact SHA-256 values are `da22da3a7c31b7ae28ff1f3dda0876f51633d4191b66dcc4dabdda0cf0d714d7` for Inventory, `36758907bc6ab1cfd543c1bc610125ef8d4eda66f4efbbe12ec6a06033326adc` for Inventory algebra and `d753b015837996fd28a0afbc564fb3d022957b102b795371d8a3c8e062cf1a3a` for Inventory enum.
- The validation vector SHA-256 is `e212c3981a27e3f775fd7c54823ae26d538b11b49b58e7dd484f7de138e89cab`. The 78-file corpus manifest SHA-256 is `5d8d3c09ce2e745c89373946ba3e7e76b90dfa7672a344a7554f5e06abe71cbf`. The manifest sorts relative slash-separated paths and writes one `path sha256:digest` line per file with LF terminators using the frozen Git bytes.
- The public preparation regression reproduced the forged enum needle acceptance on `1d9c56d`. With the fix, the forged artifact fails admission and the compiler's direct-literal artifact passes. The full Node suite passed 55 tests on the same frozen corpus. The complete 28-scenario and 51-vector Node report is byte-identical to the report compared with Java above.
- The manager owns integration, the shared roadmap update and release verification.

## Result review

Agent technical review: an independent clean-context reviewer exercised the four Node runtime modules through public admission and invocation. The review found no blocking defects and confirmed checked ID rejection, optional omission, membership identity, masking, and preservation of confirmed effects on escaping overflow.

Reviewed revision: the Node runtime working diff based on `b747ed761b0863965e44700a1de5de1623989b08`. The review preceded the Timestamp-only parameter dependency adjustment and migration of the Node fixture runners. The dependency adjustment has focused coverage and the migrated runners were exercised against compiler-produced bytes.

An independent review of the enum membership follow-up on `1d9c56d` found no blocking issues. It compared the Node check with both Java admission paths and confirmed the public regression exercises the original accepted artifact and the forged conditional.

Verification adequacy: the independent probes used adapted feasibility expressions. The subsequent public tests and byte-for-byte normalized Node and Java reports exercise the frozen compiler-emitted Keir 2 corpus. Local checked-expression edits isolate propagation and forgery cases. Release package verification remains an integration responsibility.
