# Read-and-decide compiler and artifact

Status: implementation candidate; deterministic vectors and actual CEL bridge qualification are required before implementation acceptance. One Java compiler library/CLI emits one artifact consumed unchanged by both runtimes. This replaces the baseline's illustrative multi-file bundle for this profile only.

## Compiler boundary

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

Canonical JSON is UTF-8, no BOM/trailing newline/whitespace, object keys sorted lexically by Unicode scalar value (schema keys and identifiers are ASCII), arrays retain order. Emit Boolean as `true`/`false`, integers as minimal unsigned decimal (metadata counts only), and strings with `"`/`\\` escaped, U+0000..U+001F encoded as lowercase `\u00xx`, all other valid Unicode scalars literally encoded in UTF-8. Do not normalize Unicode. Reject unpaired surrogates and numbers outside nonnegative safe integer metadata ranges; source binding numbers/null are unsupported. This recipe has no floats, negative zero or locale-dependent rendering.

Digest is `sha256:` plus lowercase SHA-256 hex of exactly those bytes. Source digest includes CEL source strings verbatim after JSON/YAML decoding (no expression whitespace rewriting). DCM digest includes the whole validated DCM, including enum order and consumer definition. Artifact digest covers the entire canonical envelope, including base64 descriptors and expressions. There is **no** self-hash field to exclude. For example canonical bytes for `{"b":true,"a":"x"}` are `{"a":"x","b":true}` with no newline. Developers must add byte/hash vectors including escaped controls and non-ASCII values.

Protobuf serialization must use deterministic ordering of maps/repeated definitions. Preserve source information in CheckedExpr only if deterministic and free of absolute paths; use a constant source description. Compile the same decoded source/model/environment twice and require identical full artifact bytes. Recompiling for each runtime is not conformance. Runners accept only the canonical envelope and standard base64; reject duplicate/unknown fields, extra trailing content and unsupported versions rather than guessing compatibility.

## Descriptor/type bridge

Produce one file `keir_read.proto`, syntax `proto3`, package `keir.read`. Each DCM record becomes a message of the same name; messages and their fields are ordered by ASCII name. Assign field numbers starting at 1 in that order; field type is `bool` or `string`, with scalar nominal/enum types represented as string. No protobuf enum, map, optional or repeated field is needed. Descriptor `json_name` equals the DCM field name exactly; this prevents language-specific camel-case reinterpretation. No unused descriptor imports/extensions/options. Scalars stay scalar CEL declarations for `let`; messages become `keir.read.<RecordName>`.

Validate complete closed values before constructing a message and after extracting any result. In particular `false` and empty String are present values, not missing fields. Proto3's default behavior is not validation. The initial profile never selects optional fields or calls `has`. Nominal/enum identity is maintained by the compiler overlay and bound DCM, not invented protobuf scalar types.

Node and Java must prepare the same checked expressions in the environment derived from these descriptors and variables. Qualification must establish dynamic descriptor registration, exact field names, scalar operations and enum conditionals in the actual pinned libraries. CEL-ES's use of its own planner environment requires explicit AST/overload qualification; a CheckedExpr is not an independent safety certificate. If the proposed bridge fails, report and revise this single contract before engine implementation, not two incompatible representations.

The [scalar qualification](../../conformance/cel-profile/README.md) passes this descriptor bridge. Its resolved environment pins are `celJava: "0.14.0"`, `celNode: "0.6.1"`, `protobufJava: "4.35.1"`, `protobufNode: "2.6.2"`; supporting packages are `dev.cel:protobuf:0.14.0` and `@bufbuild/cel-spec:0.6.1`. The latter must match the CEL package version in this profile. Preserve full dependency resolution/lock evidence in the implementation. The Java `plannerRuntimeBuilder` requires `enableHeterogeneousNumericComparisons(true)` with these options; configure it explicitly while still rejecting all numeric AST/types under `keir-read/1`. This runtime option grants no numeric language support.

This qualifies the expression bridge, not full compiler/runtime acceptance. Removing CheckedExpr type/reference maps still allowed CEL-ES evaluation in the experiment; the separate admission gate rejected that corruption. Runners must not rely on the evaluator to validate missing checked metadata. See [evidence limits](../internal/read-and-decide-design.md#qualification-and-remaining-gates).

## Admission and trust

Sample host obtains compiler output through a host-controlled path and separately retains its expected artifact digest. `prepare` receives expected digest, expected DCM digest, entrypoint/consumer and supported environment from trusted host configuration, **not from the artifact or customer**. Bound raw byte size, compare exact SHA-256 to that external expected value before parsing/preparing nested content, then validate canonical encoding, DCM digest, versions, bounds and instruction integrity. Any mismatch means no prepared handle and no adapter call. Recomputing artifact-local hashes cannot authenticate forged output.

The independently held digest authenticates only the host-selected bytes in this controlled example; it is not a general compiler signing system. Never expose a public accept-unsigned path. No production activation registry, signing-key lifecycle, approval UI or cross-tenant routing is claimed. Changing DCM fields/descriptors, consumer meaning, profile or dependency pins requires explicit recompilation and host reselection; this profile has no automatic compatibility migration.
