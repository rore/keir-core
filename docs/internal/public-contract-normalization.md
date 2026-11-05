# Public contract normalization

Decision: one clean pre-release cutover before Core 0.1.0, with Daymark using the same current published contract. Backward compatibility is not required. This supersedes the earlier legacy-reader, selector, migration, saved-conversation and dual-pin requirements. The [release feature](../../roadmap/features/public-package-release.md) owns delivery status. This document defines implementation acceptance; it does not claim code, packages or tests are complete.

Preserve the existing preview data and previously published artifacts without modifying, deleting or migrating them. Their preservation does not require the new release to read or execute them. Use fresh isolated seeded data for verification. No publication, installed-tool change, service restart or live-data operation is authorized here.

## Single supported contract

| Surface | Literal | Owned promise |
| --- | --- | --- |
| Source language | `keir/1` | Grammar, typing, dataflow and source semantics |
| DCM format | `keir-dcm/1` | Model structure and meaning |
| Artifact format | `keir-artifact/1` | Serialized plan schema and execution semantics |
| Verification format | `keir-scenario/1` | Test-only ordered stub/expectation protocol |

The only supported compilation combination is source `keir/1` + DCM `keir-dcm/1` -> artifact `keir-artifact/1`. The scenario contract accepts only that artifact/model pair. Their shared initial suffix is incidental; never compare suffixes to infer compatibility. Future changes version the owning contract only: new source syntax requires a new source identity, even when its lowering preserves artifact semantics. Model structure/meaning changes version the DCM; executable encoding/meaning changes version the artifact. Same encoding with different execution meaning is a different artifact contract.

There is no independent execution profile. Remove artifact `profile` and DCM `assurance`; reject them as unknown fields. The fixed assurance marker proved no native behavior. Removing it does not remove any authorization or single-mutation checks.

The DCM root is exactly `{format, domain, types, capabilities, entrypoints, hostKey}`. Preserve the current effect/4 semantic baseline: scalar/record and nominal types, complete implementation registration, key contract, EVENT/AFTER_COMMIT/EVENT_REPORT, protected bindings and host-owned post-commit consumer declarations. Optional `capabilityPolicy` and `effectGrant` stay on entrypoints. Absent grant means one; explicit zero prohibits WRITE. Conservatively count guarded writes toward the maximum of one possible WRITE. No extra host input, callback, admission policy, multi-write support or stronger runtime guarantee is introduced.

The artifact root is exactly `{format, compiler, environment, sourceDigest, modelRef, entrypoint, expressions, steps, return}`. Its modelRef is exactly `{domainId, domainVersion, format, digest, descriptorRecipe}`, with `format: keir-dcm/1` and `descriptorRecipe: keir-record-descriptors/1`. Retain canonical model encoding/digest, deterministic protobuf, checked-expression validation, instruction/dataflow checks, bounds, outcomes/traces and String concatenation. No embedded-model fallback or resolver. A sourceDigest is an identity, not authentication or approval.

Source keeps the existing closed root with `language: keir/1`. Domain ID/version/fingerprint must match the trusted DCM exactly. Host-owned domain/capability/consumer versions retain exact equality and business meaning; do not invent a business-version change merely for a Keir format change. The complete canonical model digest changes with its format or content, including unused catalog entries.

The scenario root is exactly `{format, id, given, stubs, expect}`; `given` is exactly `{input, fixture}`. Program selection belongs to the trusted test invocation, not a `given.program` field. No new runtime simulation API or customer tool is needed.

### Shared scenario validation

Core and Daymark must consume one version-controlled structural schema at `schemas/scenario-1/scenario.schema.json`, with schema ID `urn:keir:schema:scenario:1`, and the same lexical acceptance vectors. This is an implementation deliverable, not an already-existing schema. Do not maintain divergent copies. Freeze these rules for `keir-scenario/1`:

- A document is strict UTF-8 JSON, at most 65,536 bytes, with no BOM, duplicate object keys or unpaired surrogates. Maximum container nesting is 16 (root container counts as one). Reject non-JSON values; numeric metadata must be safe integers. These lexical checks supplement JSON Schema; schema validation alone is insufficient.
- `id` and `given.fixture` use ASCII `[A-Za-z0-9][A-Za-z0-9_.-]{0,63}`, matched against the entire string, including rejecting a trailing newline. Dots are accepted in both hosts. IDs are case-sensitive; suites reject duplicate IDs. Suite batch limits are host limits, not a different scenario grammar.
- Root and `given` have exactly the fields specified above. `expect` has exactly `{outcome, calls, consumption}`. `stubs` and `expect.calls` each contain zero to four items. A call has exactly `{step, capability, version, args, protectedArgs}`; a stub adds required `response`. `step` is ASCII `[A-Za-z][A-Za-z0-9]{0,63}` with full-string matching. Capability/version strings are nonempty, at most 256 UTF-8 bytes. Argument and input values are records; their names and scalar types are checked against the selected DCM, not a task-specific schema.
- Response variants are closed: SUCCESS `{kind, value}`, BUSINESS_REJECTION `{kind, code}`, TRANSIENT_FAILURE `{kind}`, or UNKNOWN_COMMIT `{kind, effect}`. WRITE adds required `effect` to the first three variants, respectively CONFIRMED, NONE and NONE; UNKNOWN_COMMIT requires UNKNOWN and is WRITE-only. The schema admits these structural variants; DCM validation selects the legal READ/WRITE variant and declared error codes.
- `outcome` has required `{classification, effects, trace}` and optional `result`/`error`, with the existing effect/4 classification and result/error-presence rules, including handled rejection. Effects contain at most one `{step, capability, state}`; trace has 2–64 entries using the existing closed event variants and contiguous sequence numbers. Error `{code, path}` permits one nonrecursive `cause` with `{code, path}`. Codes use full-string ASCII `[A-Z][A-Z0-9_]{0,63}`; paths are at most 1,024 UTF-8 bytes. Consumption retains the closed EVENT_REPORT forms: REPORTED with classification/effects and the same optional result/error, or NOT_REPORTED with code. Typed values retain DCM scalar/record bounds. Trace/effect consistency and matching consumption are semantic checks, not properties inferred from schema success.

Validation order is lexical/structural checks, exact scenario/artifact/model compatibility, trusted fixture lookup and DCM-typed inputs/stubs/expectations, then execution. Fixture names only identify entries in the selected host's trusted fixture table; a structurally valid name neither creates a fixture nor grants authority. Protected context is constructed by that host, never taken on trust from expected protected arguments. The initial new pair is exclusively `keir-scenario/1` + `keir-artifact/1` + `keir-dcm/1`. All obsolete scenario, artifact and model formats are unsupported. Reject obsolete or mixed pairs before any stub dispatch; do not relabel or convert them. Share vectors for dotted IDs, invalid characters/newlines, byte/depth boundaries, duplicate keys, unknown fields, unknown fixtures, wrong DCM types and mixed contract pairs. Fixture lookup may differ by host; lexical/structural acceptance must not.

## Compiler and runtime path

The ordinary two-file CLI invocation compiles the single supported contract. Java `KeirCompiler.Environment.supported()` selects that same contract. Remove old `--profile` switches and old profile-specific factories; do not add `--contract`, `keir1()` or another target knob. Reject extra selectors and obsolete source/model identities, even if their remaining fields look compatible. Selection is fixed by the trusted compiler build, with exact source/model validation.

Output producer is exactly `keir-compiler-java/0.1.0`, with environment exactly `{celJava: "0.14.0", celNode: "0.6.1", protobufJava: "4.35.1", protobufNode: "2.6.2"}`. Reject the old SNAPSHOT producer and arbitrary versions; do not relax producer/environment admission.

Keep the current host-bound API: `bindModel(dcmBytes, implementations)`, `prepareProgram(artifactBytes, boundModel)`, Node `invokeProgram` and Java `invokeBoundEffectProgram`. Binding accepts only DCM/1; preparation accepts only artifact/1 and checks full modelRef, producer, environment, plan and static bounds. Reuse the existing current evaluator and host authorization path. There is no legacy format dispatch, translation, fallback or second interpreter.

## Removal and retained semantics

| Area | Remove from the new release/current path | Retain and verify |
| --- | --- | --- |
| Compiler | Historical profile selectors/factories, old-format parsers and profile-dependent lowering branches used only by obsolete contracts | Current typing, deterministic lowering, diagnostics, nominal records, concatenation and hard single-WRITE analysis |
| Node and Java | Historical envelope/model readers, old producer acceptance, obsolete profile-only entry points and compatibility overloads with no current callers | Current host-bound binding/preparation/invocation, key and protected-context checks, authorization, resource checks, outcome/effect/trace semantics |
| Tests and schemas | Old-format success obligations and active schema/profile matrices; old harness program selectors | Port relevant behavioral cases to the new contract, freeze shared compiler-owned bytes, add obsolete-format rejection cases |
| Daymark | Old definition catalogs and fallback selectors, dual historical/current package pins, legacy guide/compiler routing, old-conversation resume/migration code and old-version execution obligations | One current definition/package/guide combination, owner/revision/evidence binding, explicit approval/activation, idempotent current submission receipts and uncertain-write handling |
| Docs and packaging | Current profile matrices, obsolete commands and compatibility promises; packaging branches used solely to maintain obsolete releases | Coherent current references/examples/schema, reproducible current packages, exact tested release row and immutable prior published artifacts |

Remove old-only code after tracing its callers; do not delete shared helpers or tests merely because their names contain an old profile. Port applicable semantic coverage before retiring obsolete-format success fixtures. Historical Git content and published archives need no rewrite. Historical documentation may remain clearly marked and outside the current teaching path. No general migration framework or compatibility service is needed.

## Distribution

Package versions identify implementation/API releases, separately from document contracts. Publish one tested row with exact compiler, Java runtime, Node runtime and companion versions/digests, producer and environment. Target Core 0.1.0 and companion edition 3; do not call that row qualified until final asset checks pass. A future packaging-only change need not change contract semantics or producer identity, but must accurately identify new package bytes/provenance. SemVer prefixes alone never establish compatibility.

Build metadata reproducibly from committed inputs. Update current normative references, source/DCM/artifact/scenario schemas, readable teaching inputs, normal compiler command, precompiled artifact, parity tests and links together after shared fixtures and both runtimes are ready. Companion v3 uses committed guide/schema/example inputs and immutable accessible specification links, independently verified inside the package. Previously published releases and companion archives remain untouched; current tooling has no obligation to rebuild or execute their formats. Publication remains outside this task.

## Daymark cutover and Studio sequencing

The current Studio UX implementation continues against its existing assigned base. Do not interrupt it or mix format changes into that delivery. The manager sequences a subsequent Daymark cutover after Studio integration and availability of the verified Core package combination. Reconcile affected Studio call sites then.

Use one current host definition with DCM/1, source/1, artifact/1, scenario/1, the normal compiler path and companion v3. Update new customization, authoring guide, simulation, review, submission, activation and walkthrough together. Remove old-definition selection and migration UI rather than exposing compatibility machinery. Do not copy old approvals or relabel old bytes.

Test in a fresh explicitly isolated data location with seeded tasks/users; never point cutover tests at preview data. Mark new databases with the current data version. Check the marker on existing databases before migrations, seeding, or authoring recovery can write to them. Reject unsupported or unmarked databases and verify the original bytes are unchanged using an isolated copy. Do not delete/reset/migrate preview data or replace its running installation. Any eventual environment deployment is separately scoped.

Verify current-contract authoring through activation and real host invocation, owner isolation, stale revision/evidence rejection, explicit approval of exact current bytes, receipt replay without duplicate submission, and uncertain-write behavior. Cold-start and backup/restore checks use only freshly created current-contract data. There is no old /2 or /3 database, conversation, receipt or artifact execution requirement.

## Implementation split and acceptance

| Owner | Work | Handoff |
| --- | --- | --- |
| Compiler + shared fixtures | Make normal path current-only; update parsers/schemas, producer and deterministic output; remove old selectors | Freeze artifact bytes/hashes and shared scenario schema/lexical vectors |
| Node runtime | Current-only format/producer validation using existing semantics; remove old-only branches | Consume frozen compiler fixtures; no independent golden regeneration |
| Java runtime | Equivalent current-only validation and retained host-bound API | Run identical artifact bytes and expected outcomes |
| Docs + packages | Update all current references, examples, commands, parity checks, companion and reproducible package metadata | Use exact final producer/runtime assets and verify links/checksums |
| Daymark | After Studio delivery, adopt one current definition/guide/package path and remove legacy scaffolding | Fresh isolated seeded end-to-end verification |

The manager owns sequencing and combined acceptance. No extra product approval is pending.

Required checks:

- Compile once through the normal CLI and Java entry point; run identical artifact bytes in both runtimes. Preserve relevant existing semantic coverage: nominal typing, concatenation, guards/handlers, protected context, NONE/CONFIRMED/UNKNOWN and handled rejection with result plus error.
- Reject obsolete source/DCM/artifact/scenario identities and producer, mixed pairs, obsolete selectors, stray profile/assurance/program fields, wrong model digest/recipe/environment, forged checked metadata and second possible WRITE before execution/stub dispatch as applicable.
- Enforce dynamic bounds at the defined check, including before the affected call where applicable. A later runtime failure can follow a confirmed write; preserve that effect and never imply rollback or automatic retry.
- Run common scenario lexical/structural vectors in Core and Daymark, including dots, full-string matching, malformed UTF-8, duplicate keys, boundaries, unknown fields/fixtures and DCM type mismatches. Do not turn host fixture lookup into schema authority.
- Run the fresh Daymark workflow and current receipt/cold-start/restore checks above. Assert old-format input is rejected without conversion and preview data remains untouched.
- Verify schemas, examples, compiler commands and both teaching hosts agree. Install final Java/Node packages in isolated consumers, compile once and execute without compiler dependencies at runtime. Check exact package integrity, companion links and reproducible metadata.
- Inspect the current release and Daymark paths for leftover old selectors, acceptance branches, dual pins and migration UI. Keep historical artifacts intact. No general containment/native-host guarantee follows merely from passing conformance.
