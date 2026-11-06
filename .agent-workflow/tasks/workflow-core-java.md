# Compile and run bounded workflow expressions in Java

Owning feature: [Bounded follow-up expressions](../../roadmap/features/enrich-followup-expressions.md).

<!-- agent-workflow:start -->
**Outcome:** Compile a neutral Inventory workflow with bounded scalar membership, optional presence and elapsed Timestamp addition. Run the same canonical artifact in Java and provide its exact inputs and expected outcomes to the Node owner.

**Target:** keir-core

**Scope:** Compiler and Java runtime admission and evaluation, versioned schemas, and shared compiler-produced Inventory conformance. Node runtime implementation is a separate contribution.

**Constraints:** Retain the public host API, typed host authority, deadline, receipts, uncertainty, one possible WRITE and current aggregate limits. Use pinned CEL/protobuf and the exact reviewed overloads. Keep the /1 fixture tree intact. No release, deployment or Daymark changes.

**Completion criteria:** Deterministic compiler bytes and a closed schema/descriptor contract. Java executes positive Inventory cases and rejects malformed values, unsafe optional reads, wrong semantic element types, forged metadata and incompatible model/artifact pairs before the relevant adapter dispatch. Shared full outcomes and dispatch counts support Node parity.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"Compile a neutral Inventory workflow with bounded scalar membership, optional presence and elapsed Timestamp addition. Run the same canonical artifact in Java and provide its exact inputs and expected outcomes to the Node owner.","scope":"Compiler and Java runtime admission and evaluation, versioned schemas, and shared compiler-produced Inventory conformance. Node runtime implementation is a separate contribution.","constraints":"Retain the public host API, typed host authority, deadline, receipts, uncertainty, one possible WRITE and current aggregate limits. Use pinned CEL/protobuf and the exact reviewed overloads. Keep the /1 fixture tree intact. No release, deployment or Daymark changes.","completion_criteria":"Deterministic compiler bytes and a closed schema/descriptor contract. Java executes positive Inventory cases and rejects malformed values, unsafe optional reads, wrong semantic element types, forged metadata and incompatible model/artifact pairs before the relevant adapter dispatch. Shared full outcomes and dispatch counts support Node parity."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:enrich-followup-expressions"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Large

**Reason:** Gray compiler, runtime admission, schema and conformance paths. Multiple independently verifiable implementation units share a public trust boundary.

**Discovery:** Core baseline `b747ed761b0863965e44700a1de5de1623989b08`. The isolated Inventory feasibility run produced byte-identical descriptors, checked expressions and wire values across pinned Java and Node. Native CEL defaults absent fields, erases nominal list-element identity, and admits dynamic Duration and oversized lists. The exact typed timestamp binding is feasible in both engines and preserves CEL Boolean masking.

**Material assumptions:** Frozen contract `workflow-slice-r1` selects a clean `/2` cutover with compiler identity `keir-compiler-java/0.2.0` and no compatibility layer. Production field numbering and explicit JSON naming stay deterministic. The frozen `/1` corpus remains historical evidence and provides exact rejection inputs. Existing external teaching and packaging entry points require the combined integration owner's cutover.

**Plan:** First extend the closed model and value boundary in `Dcm` and `Artifact` for bounded lists of existing scalar families, optional scalar fields/parameters, and strict UTC millisecond Timestamp. Apply per-element, count, and total-byte caps before adapter dispatch. Keep optional output construction and host binding validation explicit. Next update `Expressions` and `CheckedCel` to independently validate exact CEL shapes, wire types, references and overloads. Admit list membership only with matching semantic types, direct optional field presence, local branch proofs, Timestamp equality and less-than, literal-only Timestamp/Duration constructors, and checked addition. Gate these by the approved version boundary. Deterministic descriptors keep sorted field numbers and explicit JSON names, adding ordered synthetic oneofs for optional scalar fields and the pinned Timestamp dependency. Replace only the Timestamp-plus-Duration runtime overload and map escaping typed range errors, preserving String addition and lazy Boolean behavior. Keep `KeirRuntime` invocation changes limited to value/optional argument assembly. After the exact contract decisions, add separate versioned schemas and canonical Inventory fixtures. Coordinate exact hashes and public runtime outcome rows with the Node owner. Stop fixture freeze on unresolved format, cross-runtime or public compatibility discrepancies.

**Verification plan:** Deterministic compilation and descriptor lineage → byte-for-byte regeneration and pinned fixture hashes. Membership, optional/default presence, literal/size bounds and type/overload forgery → compiler and public Java bind/prepare/invoke tests with exact code/path and dispatch counts. Temporal operations → strict inputs, both orders of Boolean masking, lazy conditionals, typed escaping overflow and preservation of earlier confirmed receipts. Authority and resource invariants → port applicable existing module regressions to compiler-produced /2 equivalents with their independent expectations. Compare full outcome, trace and consumption rows. Version separation → frozen /1 tree identity and exact incompatible-format admission cases. Shared schemas → offline structural checks for both fixture revisions and same-byte evidence from the Node owner.

**Plan review:** Agent technical review: independent clean-context workflow Java plan review approved the model, descriptor and checked-expression foundation against the frozen `/2` tuple, with the existing protected-argument boundary retained. Confirm the precise optional protected parameter contract before fixture freeze. Verify shared protected names across capabilities, forged protected arguments, optional false and empty values, and handler field omission.

**Approvals:** Manager assigned isolated implementation of the bounded scope. Human approval is not required at this risk level.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

The isolated candidate starts at `b747ed761b0863965e44700a1de5de1623989b08` under frozen `workflow-slice-r1`. Compiler and Java runtime model admission develop in parallel with a common semantic type representation at their existing boundaries. Fixture generation follows successful public admission and the exact shared schema contract.

The exact format tuple is `keir/2`, `keir-dcm/2`, `keir-artifact/2`, `keir-scenario/2`, `keir-record-descriptors/2`, and `keir-compiler-java/0.2.0`. Optional parameters are authored agent arguments. Protected binding keys remain exact and required. The compiler and Java runtime derive the same descriptor from the admitted model without adding a descriptor payload to the artifact.

## Verification

- Offline Maven package verification passed 63 compiler tests and 74 Java runtime tests, with no failures or skips. The public Java runner matched expected calls, outcome, trace, receipts and consumption for 28 scenarios, plus the phases and dispatch counts for 51 raw vectors. The resulting report SHA-256 is `953544de1166aa875b3e9d0beb9088d2c0a1091f226cc9fc59e7edc2c9f20ce2`. The model-binding suite also exercised four exact raw byte cases.
- Offline schema verification passed 31 Keir 1 fixtures and 19 structural rejections, plus 47 Keir 2 fixtures and 35 structural rejections. The Keir 1 schemas and corpus have unchanged Git content.
- Inventory artifact digests are `sha256:da22da3a7c31b7ae28ff1f3dda0876f51633d4191b66dcc4dabdda0cf0d714d7`, `sha256:36758907bc6ab1cfd543c1bc610125ef8d4eda66f4efbbe12ec6a06033326adc`, and `sha256:d753b015837996fd28a0afbc564fb3d022957b102b795371d8a3c8e062cf1a3a`. The independently derived compiler/runtime descriptor for the main Inventory model is 834 identical bytes, SHA-256 `8a352e5893c7b33d15e325ede52b25d0ac7063ce8bd21a0af71f45985182e87a`. The pinned 255-byte standard Timestamp descriptor comes first.
- Isolated public Java measurement used a 4,513-byte input with 16 long-prefix labels of 256 bytes each, reused preparation, and an immediate READ. After 10,000 warmup invocations, five blocks of 2,000 gave a median block mean of 755 microseconds and about 203 KB of caller-thread allocation per invocation. The run dispatched 20,001 READs and zero WRITEs including the verification invocation. This is a sample, with outcome creation and without outcome serialization.
- The Node owner received the canonical producer seam. Node parity, external teaching hosts, CI integration and released package verification remain combined acceptance work.

## Result review

Agent technical review: An independent clean-context reviewer found that direct enum member literals required refinement in both gates, and requested more shared boundary evidence. The final patch addresses both findings.

Reviewed revision: Product patch SHA-256 `a00b286f4a2eb797ba8019a1719f59985e06fc2a02a8bdce0565f27516bf5fac` against `b747ed761b0863965e44700a1de5de1623989b08`, excluding this record. Verdict PASS.

Verification adequacy: The reviewer verified the exact staged hash and final test logs. Shared cases exercise enum and Boolean membership, empty optional String presence, ordered list pass-through, duration endpoints, exact millisecond output, Boolean error masking and confirmed receipts. Public preparation rejects a forged presence proof and an old descriptor recipe. Redline classified the product paths Gray with no Red findings. Cross-runtime parity and external integration remain with the combined owner.
