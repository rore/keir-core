<a id="read-and-decide-compiler-and-artifact"></a>
# Compiler and artifact contracts

## Effect/4 identity

Select `keir-effect/4` explicitly in the trusted compiler environment. Source remains `keir/v0`; the model format is `keir-dcm/effect-4` with `HOST_BOUND_SINGLE_WRITE`, and the artifact format is `keir-artifact/effect-4`. Do not infer versions from missing fields or relabel earlier bytes. One compiler produces the same artifact for Java and Node.

Conceptual API: `compile(sourceBytes, trustedDcmBytes, trustedEnvironment) -> ArtifactBytes | Diagnostics`. The environment selects exact compiler/CEL/protobuf identities. The source chooses a declared entrypoint; its domain fingerprint must match the canonical trusted DCM. Compilation validates the complete source, model, type/dataflow rules and static bounds before emitting an artifact. Failure emits diagnostics and no artifact. Runtime execution never requires the compiler.

The effect/4 envelope is exactly `{format, profile, compiler, environment, sourceDigest, modelRef, entrypoint, expressions, steps, return}`. `modelRef` is the exact [model reference](host-model-binding.md#exact-reference-artifact), with format `keir-dcm/effect-4` and recipe `keir-record-descriptors/1`. No embedded DCM, descriptor override or network resolver is allowed. `environment` is `{celJava,celNode,protobufJava,protobufNode}` with exact supported values. The remaining expression/binding fields use the representations below; effect CALL instructions and expression availability follow [single-effect lowering](single-effect.md#artifact).

The [language contract](language.md#types-and-cel-bridge) defines effect/4 operators, including bounded plain String concatenation. [Explicit nominal literals](generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority) apply from `/2` onward. Canonical encoding, digest and descriptor recipe rules below apply across versions. `/1` embeds descriptors; `/2`–`/4` derive them from the exact bound model.

[Effect/4 preparation](host-model-binding.md#effect4-binding-and-preparation) derives and checks model/compiler/environment compatibility internally. The host selects approved bytes and owns stored-byte integrity; it supplies no expected digest, scope, consumer, compiler or environment admission metadata. This does not remove those fields from artifact/model contracts. Implementation and package evidence are recorded in [testing](../testing.md).

## Legacy read/1 envelope and compiler boundary

## Compiler boundary

This section specifies `keir-read/1`. The implemented [single-effect artifact](single-effect.md#artifact) uses a separate profile and format with checked guard/handler fields and definite environments. Canonicalization and independent raw/canonical DCM ceilings remain shared; external admission pins apply only to legacy APIs; old artifacts are not reinterpreted or migrated.

Conceptual API: `compile(sourceBytes, trustedDcmBytes, trustedEnvironment) -> ArtifactBytes | Diagnostics`. The environment selects profile `keir-read/1` and exact compiler/CEL/protobuf package identities. CLI transport must preserve these arguments without shell interpolation, bound subprocess input/output/time/concurrency and emit no artifact on failure. Runtime execution never requires the compiler.

Source may select any declared entrypoint within the trusted DCM, subject to that entrypoint's grants. The compiler environment has no separate expected-entrypoint selector. The embedding host independently selects the expected entrypoint/consumer during runtime admission; successful compilation alone does not authorize that selection.

Validate [source and CEL](language.md), [DCM](domain-model.md) and aggregate bounds, then lower in source order. No source interpretation at runtime. The compiler emits a JSON object with exactly these fields:

| Field | Exact content |
| --- | --- |
| `format` | `keir-artifact/read-1` |
| `profile` | `keir-read/1` |
| `compiler` | Exact build identity string selected outside source |
| `environment` | `{celJava,celNode,protobufJava,protobufNode}`: exact qualified package version strings, no ranges |
| `sourceDigest` | Digest of canonical parsed source |
| `dcmDigest` | Digest of canonical DCM |
| `dcm` | Complete validated DCM object |
| `entrypoint` | Resolved entrypoint ID |
| `descriptors` | Standard padded base64 of a deterministic `FileDescriptorSet` |
| `expressions` | Map `e0`, `e1`, ... to `{checked,variables,resultType}` |
| `steps` | Array of lowered CALL/BIND instructions below |
| `return` | `{type,fields}`: output record type and field-to-binding map |

`checked` is standard padded base64 of deterministic `dev.cel.expr.CheckedExpr` bytes. `variables` maps variable names to DCM/builtin type names; it includes only variables available before that expression, with `input` always present. `resultType` is the semantic scalar type after overlay checking. No timestamps, absolute source paths, concrete host identities, request keys, signatures or artifact self-digest appear in the envelope.

Lowered binding is exactly `{literal:boolean|string,type:typeName}` or `{expr:expressionId,type:typeName}`. CALL is `{op:"CALL",id,capability,version,args,resultType}`; `args` contains all and only agent parameters, and `resultType` is the DCM result record. Host parameter names/types remain in the bound DCM and are not source args. BIND is `{op:"BIND",id,value,resultType}` with scalar binding `value`. The ordered steps plus root return are the complete instruction program; there are no implicit guards, error transitions or retry handlers.

Assign expression IDs in step order, visiting argument names in ASCII lexical order, then root return fields in that order. A BIND visits its one value. Literal bindings have no expression ID. Variables include all prior immutable bindings, but AST references must be to declared names. Runtime checks instruction/opcode/type/capability/version consistency with this envelope before preparation; trusted provenance does not excuse malformed format.

## Canonical bytes and digest rules

The shared [byte ceilings](runtime.md#limits) apply separately to each raw source/DCM document and to the complete DCM object serialized with the canonical JSON rules below. Each ceiling is 65,536 bytes, inclusive; canonical escaping can expand a raw DCM that fits its input ceiling. After validating the DCM and before artifact emission, the compiler must reject canonical DCM overflow with `BUDGET_EXCEEDED` at `/dcm` and emit no artifact. For `/1`, both runtimes enforce the canonical embedded DCM ceiling during preparation at `/artifact/dcm`. Reference-model profiles enforce model bounds during binding; effect/4 reports binding size failures under `/binding/dcm`. Failure produces no handle or dispatch. The separate whole-artifact ceiling does not relax either DCM ceiling; do not truncate the model or discard unused definitions to make it fit.

Canonical JSON is UTF-8, no BOM/trailing newline/whitespace, object keys sorted lexically by Unicode scalar value (schema keys and identifiers are ASCII), arrays retain order. Emit Boolean as `true`/`false`, integers as minimal unsigned decimal (metadata counts only), and strings with `"`/`\\` escaped, U+0000..U+001F encoded as lowercase `\u00xx`, all other valid Unicode scalars literally encoded in UTF-8. Do not normalize Unicode. Reject unpaired surrogates and numbers outside nonnegative safe integer metadata ranges; source binding numbers/null are unsupported. This recipe has no floats, negative zero or locale-dependent rendering.

Digest is `sha256:` plus lowercase SHA-256 hex of exactly those bytes. Source digest includes CEL source strings verbatim after JSON/YAML decoding (no expression whitespace rewriting). DCM digest includes the whole validated DCM, including enum order and consumer definition. Artifact digest covers the entire canonical envelope, including base64 expressions and, for `/1`, descriptors. There is **no** self-hash field to exclude. For example canonical bytes for `{"b":true,"a":"x"}` are `{"a":"x","b":true}` with no newline. Developers must add byte/hash vectors including escaped controls and non-ASCII values.

Protobuf serialization must use deterministic ordering of maps/repeated definitions. Preserve source information in CheckedExpr only if deterministic and free of absolute paths; use a constant source description. Compile the same decoded source/model/environment twice and require identical full artifact bytes. Recompiling for each runtime is not conformance. Runners accept only the canonical envelope and standard base64; reject duplicate/unknown fields, extra trailing content and unsupported versions rather than guessing compatibility.

## Descriptor/type bridge

Produce one file `keir_read.proto`, syntax `proto3`, package `keir.read`. Each DCM record becomes a message of the same name; messages and their fields are ordered by ASCII name. Assign field numbers starting at 1 in that order; field type is `bool` or `string`, with scalar nominal/enum types represented as string. No protobuf enum, map, optional or repeated field is needed. Descriptor `json_name` equals the DCM field name exactly; this prevents language-specific camel-case reinterpretation. No unused descriptor imports/extensions/options. Scalars stay scalar CEL declarations for `let`; messages become `keir.read.<RecordName>`.

Validate complete closed values before constructing a message and after extracting any result. In particular `false` and empty String are present values, not missing fields. Proto3's default behavior is not validation. These profiles never select optional fields or call `has`. Nominal/enum identity is maintained by the compiler overlay and bound DCM, not invented protobuf scalar types.

Node and Java prepare the same checked expressions in environments derived from these descriptors and variables. Runners independently validate the checked AST, overloads, type/reference maps and nominal/enum overlays; evaluator acceptance alone is not validation. Missing checked metadata must be rejected before execution.

Supported artifact environment pins are `celJava: "0.14.0"`, `celNode: "0.6.1"`, `protobufJava: "4.35.1"`, `protobufNode: "2.6.2"`. The corresponding supporting packages are `dev.cel:protobuf:0.14.0` and `@bufbuild/cel-spec:0.6.1`. The latter matches the CEL package version. Java's planner enables heterogeneous numeric comparisons as an engine option; all numeric business AST/types remain rejected. This option adds no numeric language support. See [testing](../testing.md) for descriptor and engine verification evidence.

## Admission and trust

This section preserves the legacy external-pin contract. Effect/4 uses the [binding and preparation contract](host-model-binding.md#effect4-binding-and-preparation) instead.

Sample host obtains compiler output through a host-controlled path and separately retains its expected artifact digest. `prepare` receives expected digest, expected DCM digest, entrypoint/consumer and supported environment from trusted host configuration, **not from the artifact or customer**. Bound raw byte size, compare exact SHA-256 to that external expected value before parsing/preparing nested content, then validate canonical encoding, DCM digest, versions, bounds and instruction integrity. Any mismatch means no prepared handle and no adapter call. Recomputing artifact-local hashes cannot authenticate forged output.

The independently held digest authenticates only the host-selected bytes in this controlled example; it is not a general compiler signing system. Do not bypass these required external-pin checks in legacy preparation. No production activation registry, signing-key lifecycle, approval UI or cross-tenant routing is claimed. Changing DCM fields/descriptors, consumer meaning, profile or dependency pins requires explicit recompilation and host reselection; this profile has no automatic compatibility migration.
