# Guarded-result scalar dataflow

**Status: implemented and qualified as a `0.4.0` candidate; combined acceptance complete.** The bounded contract has independent source review, shared source checks and passed actual installed Java/Node package and edition-8 companion qualification. This is not a registry release or live-demo adoption announcement. The owning [guarded-result feature](../../roadmap/features/guarded-result-dataflow.md) records immutable inputs, measured outcomes, documentation acceptance and retained execution holds; the originating [broader language types proposal](../../roadmap/ideas/broader-language-types.md) retains unselected work. Maintained [language](../specs/language.md), [artifact](../specs/compiler-artifact.md) and [runtime](../specs/runtime.md) specifications describe the current contract. No additional scope is authorized.

**Historical design context:** This record was written from source and contract inspection at `c4b61e94873fdcf2b64d7c2cfb34b9475ab4f0fd`, before implementation tests. The implementation-seam observations, proposal wording and future-tense obligations below describe that design baseline, not missing current implementation or fresh assignments. The acceptance table is a contract, not a test-result report; completed source, package/companion and combined acceptance evidence belongs to the owning feature.

## 1. Decision and ownership

Permit scalar fields of a successful guarded call inside the true arm of a conditional whose condition is that call's exact immutable guard identifier. Reuse the existing conditional, CALL, field selection and scalar types. Add no source field, expression function, public host callback, optional-record type or proof field in the artifact.

This closes a composition gap: an extension can conditionally invoke an approved operation but currently cannot use its actual result, even under the original guard. Making the operation unconditional changes behavior; predicting its result from input is not equivalent. The [language](../specs/language.md#steps-and-data-flow) and [artifact](../specs/compiler-artifact.md#lowered-steps-and-expressions) currently exclude guarded results entirely.

The design follows [baseline composition and ownership](design-baseline/public/keir-architecture.md#3-logical-architecture-and-ownership), [host responsibilities](design-baseline/public/host-integration-and-hook-contracts-v0.5.md#1-scope-and-ownership), and [original-key ownership](design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md#3-responsibility-boundaries). [Both runtimes remain initial scope](decisions/0001-repositories-and-runtimes.md); [product direction is accepted](decisions/0002-product-proposition-and-evaluation.md); [CEL resource evidence remains required](decisions/0003-restricted-cel-resource-policy.md). Historical Java-later and proposed multi-effect rules do not override current contracts.

The host still owns native guarantees, authorization, original request keys, result consumers and real/simulated adapters. Keir preserves declared types, approval identity, resource enforcement, failure classification and effect evidence. A result reference is business data, not proof of a native effect beyond the existing receipt contract.

## 2. Exact language boundary

There is no new grammar. The newly admitted pattern is `g ? r.field : fallback`, where an earlier CALL with ID `r` has `when: g`, and `g` names an earlier unconditional Boolean BIND. Both branches satisfy the existing scalar conditional typing rules.

Define a conditional-result environment `C`, mapping each earlier guarded CALL ID to its declared result-record type and exact guard ID. Define a lexical proof set `P`, initially empty for each expression:

1. Visit a conditional's condition using the incoming `P`. Its checked function/overload must remain `_?_:_` / `conditional`, with three arguments and no receiver.
2. If its condition node is an identifier naming an existing Boolean BIND `g`, visit the true arm with `P` plus `g`. Visit the false arm with the incoming `P`. Otherwise neither arm gains a guard proof.
3. Existing proofs remain in force in nested subexpressions of their true arm. Each new expression starts with an empty set; proofs do not escape into later bindings, sibling return fields or other handlers.
4. A reference to a name in `C` is permitted only as the direct operand of a declared scalar-field selection, and only when its associated guard is in `P`. A direct optional-field presence test uses the same rule. Do not admit the record as a standalone value or admit its list fields, even inside a scalar-producing membership expression.
5. Optional scalar reads additionally need the existing same-expression field-presence proof. `g ? (has(r.note) ? r.note : "") : ""` is admitted; `g ? r.note : ""` without presence proof is not. `has(r.note)` alone cannot prove that `r` exists. Required scalar reads need only the guard proof.
6. Preserve exact nominal, enum, Boolean, String and Timestamp types and existing literal refinement. No conversion, nullable value, sentinel record, record/list conditional result or Duration-valued binding is introduced.

Proof matching is on the parsed/checked identifier and its resolved binding, not a source substring. Parentheses and standard parser normalization that produce that same identifier are immaterial. Do not add algebraic simplification or predicate implication to recognize `g == true`, `g && other`, `!g` in a false arm, or a separately bound alias. Qualify normalization in the pinned compiler rather than infer it from source spelling.

An alias is not proof for a different guard ID: if `when: g`, then `alias ? r.field : fallback` is rejected even if `alias` was bound from `g`. Conversely, if the call itself declares `when: alias`, that exact Boolean BIND is its guard; the ordinary identifier rule applies. No origin-tracing of Boolean values is needed.

### Expression positions and handlers

The rule applies uniformly to later BIND expressions, call arguments, handler output fields and root return fields. An explicit conditional remains necessary in an argument even when the consuming call has the same `when: g`; that call's guard supplies no implicit proof to its arguments or handler. An unconditional scalar BIND formed by a valid conditional is an ordinary available scalar afterward, not an escaping proof.

A call's arguments and handlers are checked in the environment before that call. Neither can reference that call's own result. A later handler may reference an earlier successful guarded result only through the same explicit conditional rule. The rejecting call's result is never bound. A rejection handler still terminates with BUSINESS_REJECTION; it cannot turn rejection into normal continuation or authorization.

Unproven, forward, self-result and wrong-guard reads use `DATAFLOW_UNAVAILABLE` at the owning source binding's existing diagnostic path. Existing nominal/enum/type mismatch diagnostics remain unchanged. Prohibited record/list access and unsupported CEL forms use `CEL_POLICY_DENIED`; malformed or ill-typed CEL retains `TYPE_MISMATCH`. Preserve current structural/policy preflight order and first-error behavior. Runtime preparation reports forged availability/checked metadata as `ARTIFACT_INVALID` at its existing artifact-validation path, before dispatch; unsupported producer identity retains `PIN_MISMATCH` at `/artifact/compiler`.

### Explicit exclusions

No implicit availability from a later call's guard; general predicate implication; guard-alias propagation; `&&`/`||` guard proofs; false-arm negated-guard proof; generic optional values; null/coalescing; dynamic field/argument omission; list fields of guarded results; numeric types; list predicates/transforms; record/list conditionals; new consumers; additional effects; or retries. Existing optional-field proof forms remain supported but do not themselves establish result existence.

## 3. Compiler and independent preparation

For the proposed producer only, expression `variables` is the complete declaration environment: `input`, every earlier BIND, and every earlier CALL's declared result type, including guarded calls and unused declarations. This changes the meaning of that existing map; it does not make all declarations unconditional runtime values. The existing maximum of nine declarations already covers input plus the eight-step ceiling.

Compiler and both preparation implementations independently walk ordered steps and reconstruct:

- the declaration/type environment;
- which IDs are Boolean BINDs;
- the guarded-result map `C`, derived only from preceding validated CALL `when` fields;
- the environment snapshot before each call for its arguments and handlers.

Do not serialize or trust a new dominance/proof assertion. At each expression, preparation requires the exact reconstructed `variables` map, then verifies the complete checked AST, type map, reference map, permitted overloads, scalar/nominal overlay, guard proofs and optional proofs. A declared variable is not permission to read it. Future/self variables remain excluded; expression ordering and IDs remain deterministic.

Parsing must recognize conditional result declarations so the current availability precheck does not reject every guarded name before checking its use. The semantic checker then enforces the lexical proof and scalar-field rules. Both compile-time checks and runtime preparation must reject the same unsupported program classes; the runtime never relies on compiler provenance alone to establish proof.

Source inspection identifies the current seams:

- `compiler-java/src/main/java/dev/keir/compiler/KeirCompiler.java`: guarded results are omitted from declarations; binding/handler snapshots and lowering need producer-specific semantics.
- `compiler-java/src/main/java/dev/keir/compiler/Expressions.java`: parsing availability, identifier/selection checking and conditional proof traversal need the new bounded rule alongside existing optional-field proofs.
- `runtime-java/src/main/java/dev/keir/runtime/Artifact.java` and `runtime-node/src/validation.js`: reconstruct declarations and guard provenance before expression preparation.
- `runtime-java/src/main/java/dev/keir/runtime/CheckedCel.java` and `runtime-node/src/cel.js`: independently verify proofs and construct sparse invocation activations.
- `runtime-java/src/main/java/dev/keir/runtime/KeirRuntime.java` and `runtime-node/src/index.js`: retain validated successful guarded results for the new producer.

These are affected areas, not permission to refactor unrelated paths.

## 4. Execution, activation and failure

For the new producer, a true-guard CALL binds its result only after a successful response has passed all existing deadline, response and value checks. A false guard leaves its result absent, skips arguments and dispatch, and emits the existing skipped-step trace. Store no placeholder, null, default protobuf message or empty record.

At expression evaluation, build the CEL activation from values actually bound in this invocation. Convert only present record values to protobuf. For each conditionally declared result, its known guard value explains absence: if the guard is false, omit that name entirely; if true on a continuing path, require a validated bound result. Missing definitely available values or a missing required true-guard result are internal execution failures (`EXPRESSION_FAILED` at the binding), not fallback opportunities. An unexpectedly retained false-guard result must not be exposed. Fresh invocation state prevents values leaking from an earlier invocation of a reused prepared artifact.

This is an essential implementation change: the inspected Node `evaluateBinding` currently converts every declared variable via `fromJson`, and Java `Bundle.evaluate` validates every declared variable through `Artifact.value` before CEL evaluation. Merely extending `variables` would make skipped-result branches fail eagerly. Sparse activation must avoid these conversions without weakening required-value validation.

CEL evaluates only the selected conditional arm. A skipped result in an unselected arm therefore must not be resolved or converted. All arms are still statically checked; constant-false syntax cannot hide an invalid guarded access. Absence is not an error-masking feature: true-guard business rejection, malformed response, uncertainty, timeout or other failure terminates before the later conditional runs. Do not catch these failures and select the fallback.

Preserve current settlement ordering: confirmed effect evidence is recorded before subsequent value/deadline failure where required by [runtime execution](../specs/runtime.md#write-evidence). A malformed result after CONFIRMED remains a failed invocation with that receipt, never a successful fallback. Unknown commit remains UNCERTAIN. Own-call handlers have no result; later handlers retain earlier receipts. Add no trace event solely for result retention.

Lazy evaluation with an omitted declared record variable is an implementation-evidence gate in both pinned CEL engines. If either engine cannot satisfy this through supported activation construction, stop and report the incompatibility; do not substitute fake records, broaden the language, upgrade engines or introduce a custom evaluator without review.

## 5. Compatibility and proposed release identity

Use the manager-approved exact producer identity `keir-compiler-java/0.4.0` with matching Core `0.4.0` packages for this additive language capability. This is a local implementation allocation, not a claim that a release exists or authorization to publish it. If discovery identifies a collision or another necessary version, return that decision to the manager before affected edits and change the exact identity consistently rather than admitting a range.

Keep source `keir/2`, model `keir-dcm/3`, artifact `keir-artifact/3`, scenario `keir-scenario/3`, descriptor recipe `keir-record-descriptors/2`, current canonicalization and all four engine pins. No structural field or wire-value family changes. The existing exact producer allowlist is the semantic discriminator; a new format suffix or feature-bit mechanism adds no necessary protection here.

| Admitted producer and tuple | Declaration and execution semantics in the new runtime |
| --- | --- |
| `keir-compiler-java/0.2.0`, artifact `/2`, DCM `/2`, recipe `/2` | Retained historical semantics; guarded results remain unavailable. |
| `keir-compiler-java/0.3.0` or `keir-compiler-java/0.3.1`, artifact `/3`, DCM `/3`, recipe `/2` | Retained current semantics; guarded results remain unavailable. |
| Proposed `keir-compiler-java/0.4.0`, artifact `/3`, DCM `/3`, recipe `/2` | Complete prior declarations plus independently checked conditional availability; successful guarded results retained. |

Select semantics by the exact validated tuple, not version ordering or expression shape. The new compiler always emits its new identity, including programs that do not use guarded results; it must not impersonate an old compiler. Existing runtimes reject the new producer through their closed allowlists. A forged new-semantics expression stamped with an old producer must still fail preparation in the new runtime. Neither changing the producer string nor passing schema validation creates trusted approval.

The artifact `/3` schema needs only the additional exact producer enum entry; its existing expression map shape and limits suffice. Source/DCM/scenario schemas and descriptors need no structural change. Golden old artifacts and their models, hashes, source identities and approvals remain untouched. Recompiling creates new artifact bytes and requires normal selection and approval; there is no automatic migration or approval transfer. Update the [compatibility matrix](../specs/compiler-artifact.md#source-and-artifact-identity) and follow the [release policy](../releasing.md), including downloaded package verification. Preserve immutable historical authoring companions; qualify a new matching edition rather than rewriting old guides or metadata.

## 6. Resource and authority bounds

Keep all [existing limits](../specs/runtime.md#limits): eight steps, four possible calls, one possible event WRITE, zero possible decision WRITEs, 64 AST nodes and depth 16 per expression, 512 aggregate AST nodes, 512 source bytes per expression, 16,384 bytes per checked expression, existing artifact/model/descriptor caps, 64 trace events, 8,192-byte business values, 256-byte business strings and existing list caps.

Static counts include all guarded calls/writes. No branch exclusivity discounts. Dynamic dispatch and deadline checks are unchanged. Guard proof construction traverses the same bounded AST and only retains guard IDs from the bounded preceding steps; it introduces no loops in the language or new CEL operation.

Retain each successful result at most once in invocation state until termination; discard invocation state afterward. At most four call-result records can be retained, each within the existing value cap. This is no larger a logical result count than four existing unguarded calls, but conversion/copying costs are not established by that observation. Keep complete validated records rather than introduce field-liveness analysis or host projection APIs. Lists in those records remain subject to validation and bounds even though this slice cannot read them from guarded results.

Measure maximum retained results, full activation conversion, repeated expressions and nested conditional depth using the actual engines. Preserve aggregate checks even for unselected branches. No new heap ceiling, execution timeout promise or production isolation claim follows from these static limits. Existing deadline/adapter and post-effect failure obligations remain independent.

## 7. Domain-neutral example

The host publishes an EVENT/AFTER_COMMIT entrypoint with these model declarations (all use existing DCM forms):

- `RecordRef`: nominal String; input record fields `createNew:Boolean`, `existingReference:RecordRef`, `label:String`.
- `CreatedRecord`: required `reference:RecordRef` and optional `note:String`.
- Output record: required `reference:RecordRef` and `message:String`.
- `records.create`: one WRITE, agent parameter `label:String`, result `CreatedRecord`; any protected parameters and original key remain host-bound under the existing contract.

This is a source **fragment**, not a compiler-produced fixture. The implementation must supply a complete model, normal domain fingerprint, source envelope and host registrations and compile them into one shared artifact.

```yaml
steps:
  - id: createNew
    let: {cel: input.createNew}
  - id: created
    call: records.create
    when: createNew
    args:
      label: {cel: input.label}
return:
  record:
    reference: {cel: 'createNew ? created.reference : input.existingReference'}
    message: {cel: 'createNew ? (has(created.note) ? created.note : "") : "unchanged"'}
```

When true, the returned reference is the adapter's validated result, not an inferred identifier. When false, no write is dispatched and the existing reference is returned. A present empty note is distinct from absence even though this example deliberately renders both as an empty message. No success path after a failed write exists. Real and simulated executions use the same artifact with separately bound adapters; fake adapters establish no native persistence guarantee.

## 8. Shared acceptance obligations

Each positive source is compiled once; those exact bytes run in Java and Node. Compare complete normalized outcomes, results, error codes/paths, ordered calls and arguments, traces and effect receipts, not just values. The following are required cases, not a reported test count:

| Case | Required observation |
| --- | --- |
| True guard, valid result | One dispatch; returned nominal reference equals the actual adapter result; CONFIRMED receipt. |
| False guard | No dispatch/receipt or argument evaluation; fallback selected; missing result never converted. Include a declared but unused skipped result. |
| Optional scalar | Absent, present empty String and present false Boolean remain distinguishable in typed fixtures; field reads require both proofs. |
| Scalar families | Boolean, String, nominal, enum and Timestamp fields retain their semantic type through the conditional and a later scalar BIND. |
| Nested conditionals | Outer exact-guard proof applies within its true arm only; unrelated nested conditions neither remove that proof nor create new ones. |
| False then true / true then false invocation | Reusing the same prepared artifact has no stale-result leakage. |
| Arguments and handlers | Explicit guarded conditional works in later arguments/handlers; own result is unavailable; same `when` alone gives no proof. |
| Guarded READ | Same scalar rule works for READ results; a decision fixture remains zero-write and preserves normal decision consumption. |
| Unproven reads | Direct `created.reference`, wrong/alias guard, `g && r.flag`, `!g ? fallback : r.field`, `has(r.note)` without guard, forward/self references are rejected. |
| Unsupported values | Guarded list selection even under membership, bare records, record/list conditional results, null and dynamic omissions remain rejected. |
| Type failures | Wrong nominal fallback, invalid enum literal and incompatible scalar branches retain current diagnostics. |
| False-guard policy bypass | A second possible WRITE, or any possible decision WRITE, still fails before execution. |
| Call failures | Rejection with/without handler, transient failure, exception, malformed/oversized result, unknown commit and timeout stop normal continuation; fallback never masks them. |
| Effect preservation | Invalid result or later failure after confirmed write preserves CONFIRMED; timeout winner/unknown commit preserve uncertainty and prohibit later dispatch. |
| Forged artifacts | Wrong/missing/extra variables, guard IDs or types; reordered steps; forged reference/type maps; bare or improperly dominated result access fail preparation before adapter calls. |
| Compatibility | Old goldens execute unchanged; new producer fails on old runtimes; new semantics falsely stamped with every retained producer fail on new runtimes; mixed tuples fail. |
| Limits | Existing boundary/+1 cases plus maximum guarded-result retention, maximum repeated expressions and nested proofs; no hidden conversions on skipped branches. |

Run the affected compiler and both runtime checks and shared corpus through the existing fast-test selection. Scope resource qualification to the changed retention/activation paths, reusing reliable unchanged evidence. Actual engine qualification must include false-guard missing variables, unused conditional declarations, selected versus unselected errors, and guarded optional-field presence on protobuf records. Do not infer laziness from the specification or a mock evaluator.

Package acceptance must use the shipped compiler CLI, shipped Java runtime and installed Node tarball outside the source checkout: compile this complete example and execute true, skipped and failure cases in both packaged hosts. Retain the existing event/decision smoke and historical artifact checks. Build/package-unit tests alone are insufficient. Record source revision, exact bytes, environment and full outcomes, then follow downloaded-asset verification before claiming release qualification. No live application rollout is part of this design assignment.

## 9. Affected contracts and open evidence gates

During implementation, update together:

- `docs/specs/language.md`: availability, guards, binding positions, typing, diagnostics and the narrow conditional proof.
- `docs/specs/compiler-artifact.md`: exact producer matrix, declaration-map meaning and independent reconstruction.
- `docs/specs/runtime.md`: validated result retention, sparse activation, unchanged failures/receipts and bounds.
- `docs/specs/workflow-expressions.md`: interaction with optional-field proofs and unchanged scalar conditional types; clarify that result existence and field presence are separate.
- `schemas/artifact-3/artifact.schema.json`: exact producer allowlist only. Review existing source/model/scenario schemas for consistency without unnecessary new versions or fields.
- `docs/agents/extension-assistant.md` and `docs/agents/host-integration-assistant.md`: authoring limits, host obligations and exact supported producer. Update installation/testing/release references and create a qualified matching companion edition when packaging, preserving historical editions.
- Existing compiler/runtime checks, shared `conformance/keir-3` corpus, resource qualification and packaged verification paths: add the cases above without replacing old compatibility fixtures.

Before implementation acceptance, resolve with evidence: (1) lazy execution with sparse record activation in each pinned engine, including unused declarations; (2) exact compiler normalization and matching independent preparation proofs; (3) retained-value and conversion cost within existing bounds; (4) producer-gated rejection of forged old-label artifacts; (5) actual packaged-host behavior. The local `0.4.0` producer/package allocation is approved; release publication remains a separate delivery action after its qualification requirements. If a gate fails, report the specific incompatibility and revisit this contract rather than silently widening scope or weakening a guarantee.

This proposal changes no live normative contract, package, role guide, roadmap record or runtime implementation. Acceptance and implementation ownership remain with the task owner.
