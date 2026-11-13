# Integer and Decimal implementation

Identity: `agent-workflow:numeric-values-and-rules`. Core implementation for the manager-owned numeric feature; Daymark adoption remains a separate implementation task under the same feature. Baseline `32dffe40be2fa31d1bc8de3a85902195e99cace9`.

<!-- agent-workflow:start -->
**Outcome:**
Integer and exact Decimal work through the compiler and both runtimes, with exact normalization helpers, retained authoring/execution compatibility and runnable Core examples.

**Target:**
keir-core

**Scope:**
Numeric codecs/helpers, versioned compiler/model/artifact/scenario integration, Java/Node admission and evaluation, shared conformance, runnable examples, matching documentation and packaged-consumer verification. Implement only the currently released slice below; expand exact file ownership as integration discovery establishes it.

**Constraints:**
Preserve host authority, exact guards, existing limits, receipts, old artifact bytes and approvals. No silent rounding, float coercion, unsafe request/tenant caches, dependency/engine upgrade, live environment change or deployment. Local Maven/npm/JDK/build/install/cache and old control holds remain; real builds/tests use bounded disposable hosted jobs. Preserve unrelated dirty work and immutable assets.

**Completion criteria:**
The same newly compiled artifact has independent expected outcomes in Java and Node for all admitted scalar positions and operations, exact helpers and boundaries; old artifacts and current old-profile authoring remain supported without approval transfer. Runnable stock/Decimal quote and actual packaged Java/Node consumers pass. Specifications and schemas match actual behavior; measured preparation/warm/concurrent costs and independent review support acceptance without whole-app claims.

**Requirement baseline:**
{"source":"owner end-to-end numeric feature delegation","outcome":"Integer and exact Decimal work through the compiler and both runtimes, with exact normalization helpers, retained authoring/execution compatibility and runnable Core examples.","scope":"Numeric codecs/helpers, versioned compiler/model/artifact/scenario integration, Java/Node admission and evaluation, shared conformance, runnable examples, matching documentation and packaged-consumer verification. Implement only the currently released slice below; expand exact file ownership as integration discovery establishes it.","constraints":"Preserve host authority, exact guards, existing limits, receipts, old artifact bytes and approvals. No silent rounding, float coercion, unsafe request/tenant caches, dependency/engine upgrade, live environment change or deployment. Local Maven/npm/JDK/build/install/cache and old control holds remain; real builds/tests use bounded disposable hosted jobs. Preserve unrelated dirty work and immutable assets.","completion_criteria":"The same newly compiled artifact has independent expected outcomes in Java and Node for all admitted scalar positions and operations, exact helpers and boundaries; old artifacts and current old-profile authoring remain supported without approval transfer. Runnable stock/Decimal quote and actual packaged Java/Node consumers pass. Specifications and schemas match actual behavior; measured preparation/warm/concurrent costs and independent review support acceptance without whole-app claims."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:numeric-values-and-rules"}],"pull_requests":[]}

**Risk:** High

**Complexity:** Large

**Reason:**
Eventual integration changes independently checked compiler/runtime artifact trust contracts across three shipped components. Initial source-only codec extraction does not yet admit numeric artifacts or change existing execution.

**Discovery:**
Hosted qualification run 43401342724 passed at the baseline: 297 independent matching case results and 24 normalization vectors per engine, actual custom bindings, direct intermediate reuse, exact Integer division and concurrent state checks. Evidence is component-level, not full runtime qualification. The architect is finalizing the existing design from that evidence; do not repeat source discovery or the original review.

The finalized design `docs/internal/numeric-values-and-rules.md`, SHA-256 `74a703297f7ff0a2dcaca35d1341e0ed6b8e5dc75e44ada7eb94e395806a28a8`, is the integration contract. Sections 3/4/8/9.1/10/11 are accepted. Keep `Numeric.java` as the sole private Java codec. Source `/2` with model `/3` retains nominal Integer/Decimal spellings and recipe `/2`; source `/3` with model `/4` enables built-ins and recipe `/3`. Both emit artifact `/4` from producer 0.5.0. Historical tuples stay unchanged.

**Material assumptions:**
Normalized coefficient precision <=38 and scale 0..18, exact String helpers and explicit rounding satisfy the observed cases. Whole-runtime integration must preserve those semantics without repeated preparation or shared invocation values. Stop only the affected design choice on a genuine mismatch; resolve routine source/test defects within delegated authority.

**Plan:**
Start with the unchanged qualified codec/helper behavior and shared independent vectors. Keep existing numeric admission denied until the versioned compiler/runtime slice is reviewed. Then add the compiler, both admission/evaluation paths, shared outcomes and compatibility in small vertical slices using the architect's finalized contract. Use bounded hosted regression/package checks, not local held tools or legacy stress controls. Reuse existing utilities and reviews; no new evaluator, adapter framework or generic numeric configuration.

**Verification plan:**
Codec/helpers -> shared independent normalization/arithmetic/boundary/rounding vectors in existing Java/Node test frameworks; invalid inputs fail without rounding.
Compiler and runtime trust -> identical compiled bytes, exact tuple/schema/descriptor checks, forged and old-profile rejection, optional/guarded/error/receipt regression and no unauthorized dispatch.
Compatibility/examples/packages -> unchanged old bytes/outcomes, retained current authoring, runnable stock/quote, actual installed consumers and matching companion; no generated expected results masquerading as independent oracles.
Performance/isolation -> bounded full-runtime preparation/conversion/warm/concurrent measurements, invocation-local state, honest memory and topology limits; no arbitrary speed target.

**Plan review:**
Focused integration-plan GO received 2026-11-13 from the existing independent architect, no required corrections. Reviewed record SHA-256 `dd7707368a491ca35347fabd3b6614bf183a09af3b4f59a51de632357acb3d00` was unchanged before/after review. This releases the exact integration slice below; source/runtime/package acceptance remains separate. Required evidence: both new/four historical tuples, independent exact descriptors, actual token-span lexical checks, forged-gate rejection, complete scalar/error/receipt outcomes, invocation-local direct-BIND/projection isolation and actual reuse counts with retained coefficient reparsing disclosed.

Agent technical review: reuse completed independent numeric-probe plan/source GO for the unchanged initial codec/helper arithmetic and normalization rules, plus passing independent hosted vectors at the baseline. This reuse releases only the initial pure-codec/helper slice below. The architect's focused integration-plan review must cover new artifact/admission behavior before that slice is edited; no repeated broad design review.

**Approvals:**
Approved by user 2026-11-13: "you have the approval to drive this feature from start to end, just do it already". This delegates implementation and routine repair/test/delivery iterations. Do not ask for granular approvals already covered. Preserve environment protection and escalate only material uncovered product/safety/environment decisions.

Manager continuation delegates reviewed task commits, non-force main delivery and bounded manual hosted regression/repair iterations to the implementation owner. Verify personal attribution before commits, use `[skip ci]`, read back exact remote source before dispatch and avoid duplicate submissions. A manual-only 20-minute fast-regression workflow with ephemeral dependencies, nonpersistent checkout credentials and bounded sanitized evidence is authorized. Existing local execution holds and exclusion of legacy control workflows remain unchanged. Package consumers, companion and runnable examples are required; publication/deployment and approval transfer are not authorized.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Initial released slice

- New `runtime-java/src/main/java/dev/keir/runtime/Numeric.java`: internal bounded exact representation/arithmetic/strict validation and normalization.
- `runtime-java/src/main/java/dev/keir/runtime/KeirRuntime.java`: additive static `normalizeInteger(String)` and `normalizeDecimal(String)` only; existing invocation/admission unchanged.
- New `runtime-java/src/test/java/dev/keir/runtime/NumericTest.java`.
- New `runtime-node/src/numeric.js`: equivalent internal numeric operations/helpers.
- `runtime-node/src/index.js`: additive root helper exports only; existing invocation/admission unchanged.
- New `runtime-node/test/numeric.test.js`.
- New `conformance/numeric-values.json`: independent shared codec/arithmetic vectors, reusing qualified cases rather than regenerating expectations.
- This sole Work Record.

One fixed production policy, not configurable precision/scale. Java helpers reject invalid input with IllegalArgumentException; Node uses TypeError, without echoing raw input. Optional sign, ASCII digits and optional fractional digits; normalize representational zeros exactly, reject whitespace/exponents/non-strings and tokens over 256 characters. Integer normalization accepts exact fractional zeros but rejects a nonzero fraction/int64 overflow. Strict canonical numeric data stays strict. Decimal is normalized coefficient <=38 digits, scale <=18; integral trailing zeros still count toward precision. Keep explicit six rounding modes; no implicit promotion or rounding.

No model/schema/producer or descriptor changes are released in this initial slice. Before expanding, record exact affected files and obtain focused review of the newly uncovered integration seams. Pure helpers can proceed while that contract is finalized; do not serialize independent work unnecessarily.

## Evidence and recovery

Initial codec/helper implementation started at the recorded baseline in the isolated feature checkout. Exact ownership remains the eight paths above. Reuse the qualified BigInteger/bigint coefficient-and-scale arithmetic with one fixed policy, immutable internal values and no cache. Root changes add only the two String normalization helpers; model/artifact admission and invocation stay unchanged. Tests will consume one independent shared vector file through the existing JUnit/Node test frameworks. Real compilation/evaluation stays hosted. The manager owns canonical roadmap, delivery and acceptance. Worktree retained for active implementation; no unrelated checkout cleanup.

### Initial codec/helper source result

The initial eight-path slice is source-complete. Internal immutable Decimal values use normalized coefficient precision <=38 and scale <=18. Integer is signed int64 with exact division and checked overflow. Only fractional trailing zeros are removed, so integral trailing zeros count toward precision. Strict parsers reject noncanonical data; the two opt-in root helpers accept bounded exact numeric text and never round. Six explicit rounding modes apply to round/multiplyRounded/divideRounded, including validation before a no-op. JavaScript full-match checks reject final newlines. Errors use fixed messages, not raw input. No dependencies, adapters, wrapper API or cache were added.

Existing JUnit and Node tests consume `conformance/numeric-values.json`: 35 normalization and 100 operation vectors. The original 24 helper vectors and 65 matching qualified result strings were retained verbatim; added vectors cover strict representations, input rejection, bounds, no-op mode validation and immutable intermediate recovery. These are pure-codec tests, not artifact/runtime integration coverage.

Actual static checks, 2026-11-13:

- `node --check` passed for `runtime-node/src/numeric.js`, `runtime-node/src/index.js` and `runtime-node/test/numeric.test.js`, without imports or execution.
- Approved Python `-B` inert checks passed JSON structure, unique IDs/arities, six-mode coverage, qualified expectation reuse, exact eight-path scope, strict UTF-8, whitespace and focused privacy checks. No numeric/helper/vector evaluation occurred.
- Git HEAD matches the recorded baseline; index is unchanged. The only tracked deltas are the additive Java root methods and Node root export. Six reserved new files, including this record, complete the slice. Original Requirement baseline SHA-256 is `09f6f2c239468a3940d42879b08c7dba378d8e19ee53b8ea68d9242a9535e2eb`.
- Fresh path-only Redline returned GRAY/exit 1 with no boundary violations. It matches the existing pre-edit verdict used for structural Work Record validation. No diff/line evidence was supplied; its zero line count is not a measured size audit. No extra report file was created.
- Work Record checker returned blocking/exit 2 solely for `review.agent_result_review_present`: the initial slice has no independent result review yet. Other predicates passed or were skipped by the checker. This is the pending review gate, not a static-tool failure or permission to invent a verdict. `git diff --check` passed; the separate scan includes all untracked source files.

Java compilation and all new tests remain unrun locally. Manager-owned bounded hosted compilation/tests and independent source acceptance are pending. State Ready for review refers only to this initial source slice, not feature completion or integration permission. Await the architect's contract, focused integration review and exact-file lease before editing admission, schema, producer, compiler or runtime invocation paths. No commit, staging, push, dispatch, environment change or old-checkout write occurred.

## Integration slice: exact plan for focused review

In addition to the initial eight paths, the next vertical slice reserves:

- `compiler-java/src/main/java/dev/keir/compiler/Dcm.java`, `Expressions.java`, `KeirCompiler.java`.
- `runtime-java/src/main/java/dev/keir/runtime/Artifact.java`, `CheckedCel.java`, `Profile.java`.
- `runtime-node/src/validation.js`, `model.js`, `cel.js`.
- New `schemas/source-3/source.schema.json`, `schemas/dcm-4/dcm.schema.json`, `schemas/artifact-4/artifact.schema.json`, `schemas/scenario-4/scenario.schema.json`.
- New `compiler-java/src/test/java/dev/keir/compiler/NumericCompilerTest.java`, `runtime-java/src/test/java/dev/keir/runtime/NumericIntegrationTest.java`, `runtime-node/test/numeric-integration.test.js`.
- New `.github/workflows/numeric-fast-regression.yml`.

Existing initial-slice root files may now gain invocation integration after focused review. No canonical dirty documentation, old schema/golden artifact, engine/dependency or roadmap writes. Later corpus/example/package/companion paths will be named here after reading their existing runners; do not duplicate their framework.

Implementation sequence for the focused compiler/artifact/admission review:

1. Match the entire explicit profile tuple. Extend semantic-type classification by model format/kind, never spelling alone. Reserve Integer/Decimal only for model `/4`; retain old nominal types/identifiers and exact guard/default-grant behavior. Emit only producer 0.5.0/artifact `/4` from the compiler, accepting the two specified authoring pairs.
2. Derive deterministic descriptors independently in compiler/Java/Node: optional Timestamp dependency, mandatory fixed Decimal dependency for `/4`, record file last; optional int64 synthetic oneofs, Decimal natural presence, unchanged recipe `/2` bytes. Strict numeric literal validation in the compiler remains independent of runtime code. Check parsed-source numeric token positions as well as checked constants so folded hex/uint/float forms cannot bypass policy.
3. Extend each existing gate, not the probe evaluator. Admit only the settled operation/overload table and named bounded-list size. Check all type/ref-map entries, exact semantic assignments/branches, literal-only constructors/modes/scales and existing optional/exact guard proofs. Numeric failures use typed CEL errors; existing Timestamp overflow classification stays unchanged.
4. Boundary validation uses the one private runtime codec. Keep canonical host snapshots separate from invocation-owned projected values. Reuse complete validated record/scalar projections and retain actual successful Decimal BIND messages. Guard checks precede lookup; false results create no slot. No module-level value cache, per-invocation replanning or unqualified coefficient memo. Operand/result reparsing remains explicit.
5. Add independently asserted compiler/both-runtime vectors for new tuples, expression-only Decimal/list size, all scalar paths, hostile metadata, guards/errors/receipts and retained nominal-name authoring. Preserve authentic historical artifacts. Tests remain hosted; local checks are static only.

The existing architect is requested to review only these newly uncovered trust seams and exact paths, reusing accepted arithmetic/bridge review. No additional manager ACK or user approval is required. While that review is pending, the independent manual regression workflow may proceed. Implementation remains active under the same Work Record and feature association; the full feature is not complete.

### Integration progress

Focused plan GO is captured above. Tuple/model vocabulary, independent descriptors, checked numeric operation admission and invocation-owned projection/direct-BIND integration are now implemented in the leased principal files. Strict helper normalization now removes representational zeros as text before parsing the bounded coefficient, as required by design section 9; idempotence assertions accompany it. No coefficient memo was added. The manual fast-regression workflow is drafted with isolated dependencies, explicit stage/job caps and sanitized allowlisted evidence. Local execution remains static only.

Discovery extends exact test/tool ownership to `compiler-java/src/test/java/dev/keir/compiler/GenericHookCompilerTest.java`, `WorkflowCompilerTest.java`, `GuardedResultCompilerTest.java`, `PreOperationCompilerTest.java` and `scripts/check-keir-contracts.py`. Existing 0.4 compiler golden tests must now expect only the intentional new producer/artifact identity, while preserving and comparing every other old decoded field and retaining original golden files/digests. Runtime historical goldens remain unchanged. New schemas reuse immutable old structural subschemas by explicit URI; the existing schema registry must include those dependencies. These are direct callers of the changed tuple/schema seams, not new review scope.

The new compiler test compiles 97 accepted qualified expressions into one shared test artifact batch for both runtime suites, keeping the original normalized expectations and adding complete independent outcome/trace assertions. The two probe-only missing-scalar activations are excluded because production absence must use actual guarded CALL results; existing/new production guard vectors cover that separate obligation. Batch data is emitted only under test `target/`, never as regenerated expectations. A future pinned `conformance/keir-4/engine-matrix.json` may retain the hosted-produced bytes after verification; its path is reserved. No old corpus or expected result is rewritten.

Initial integration source checkpoint is ready for focused source review before the first bounded hosted regression. Node nonloading syntax checks passed for all modified runtime modules and the new integration test. Approved Python inert checks passed new closed-schema validation (including both 0.5 artifact tuples and crossed/old-producer rejections), workflow YAML/token/cache/exclusion checks, embedded Python syntax, JSON structure, source delimiter-only scan, privacy/whitespace and unchanged requirement baseline/index. `git diff --check` passed. These checks do not establish Java syntax/types, engine behavior or test success. No local Java/numeric/helper/module execution occurred.

The existing architect is requested to review the new production seams and manual workflow at this checkpoint, reusing arithmetic/bridge evidence. Source acceptance permits only reviewed delivery and hosted verification/repair, not feature completion. Remaining obligations include actual artifact batches, full host/protected/adapter/receipt/guard paths, retained-name execution, runnable stock/quote scenarios, versioned scenario consumers, bounded runtime measurements, packaged consumers, companion and documentation review. No source success is substituted for those gates.

## Result review

Agent technical review: independent existing architect focused SOURCE GO, 2026-11-13, no blocking source corrections. Accepted for reviewed checkpoint delivery and bounded hosted regression/repair only, not feature acceptance. Arithmetic/bridge review was reused. No reviewer edits or execution.

Reviewed revision: baseline `32dffe40be2fa31d1bc8de3a85902195e99cace9`; record SHA-256 `2c34bd069b2d20c84b0a4b5f0be459a382fff1dee0b0d98637da5f4ece23f96f` unchanged before/after; 29-file modified/untracked inventory SHA-256 `83dba27f7ffc8f30fcbc9b8c801f2ec0cfdd857710734f1651952757f6fa07b2`. Inventory encoding is sorted `sha256  relative-path` lines, UTF-8/LF with final LF. The implementer independently matched both hashes before this review-capture-only record update; source files remain unchanged.

Verification adequacy: both 0.5 tuples/historical admission, nominal-name handling, independently reconstructed descriptors, compiler token-position checks, runtime gates/registrations, invocation-owned snapshots/lazy projections/direct BIND, independent 97-expression extraction, schema reuse and identity-only legacy compiler comparisons passed source review. Manual workflow bounds/exclusions passed source review. Java compilation and all runtime assertions still require hosted execution. Full protected/adapter/receipt/guard paths, retained-name runtime execution, examples/scenarios, package/companion/docs and bounded actual-runtime performance remain outstanding. Operand/result reparsing remains disclosed. This review does not certify those unrun obligations.

Personal commit attribution and the intended personal remote/account were verified without configuration changes. Next action is the authorized `[skip ci]` checkpoint commit, non-force main push, exact remote readback and one manual fast-regression submission. Record the actual run identity/outcome before any repair iteration; no duplicate submission or legacy control dispatch.

Final checkpoint governance: fresh path-only Redline over the exact 29-path inventory returned GRAY/exit 1 with no boundary violations; structural Work Record validation returned clean/exit 0 after source-review capture. These are static governance checks, not engine or feature acceptance. The only post-review edit is review/verification capture in this record; all reviewed application, test, schema and workflow source bytes are unchanged.
