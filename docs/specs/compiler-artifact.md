<a id="read-and-decide-compiler-and-artifact"></a>
# Compiler and artifact contracts

<a id="effect4-identity"></a>
<a id="keir-ir-and-artifact-identity"></a>
## Source and artifact identity

Select the single-write contract in trusted compiler configuration. One compiler produces the same artifact for Java and Node. [Exact wire identifiers](../internal/implementation-reference.md#serialized-identifiers-and-compiler-selectors)

Conceptual API: `compile(sourceBytes, trustedDcmBytes, trustedEnvironment) -> ArtifactBytes | Diagnostics`. The environment selects exact compiler/CEL/protobuf identities. The source chooses a declared entrypoint; its domain fingerprint must match the canonical trusted DCM. Compilation validates the complete source, model, type/dataflow rules and static bounds before emitting an artifact. Failure emits diagnostics and no artifact. Runtime execution never requires the compiler.

The compiled artifact envelope is exactly `{format, profile, compiler, environment, sourceDigest, modelRef, entrypoint, expressions, steps, return}`. `modelRef` is the exact [model reference](host-model-binding.md#exact-reference-artifact) for the current DCM format and descriptor recipe. The host supplies the referenced model locally. `environment` is `{celJava,celNode,protobufJava,protobufNode}` with exact supported values. Effect CALL instructions and expression availability follow [single-write lowering](single-effect.md#artifact).

The [language contract](language.md#types-and-cel-bridge) defines Keir operators, including bounded plain String concatenation. The current contract supports [explicit nominal literals](generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority). Canonical encoding, digest and descriptor recipe rules below apply across versions. Earlier profiles embed or derive descriptors according to their own contracts.

[Preparation](host-model-binding.md#effect4-binding-and-preparation) derives and checks model/compiler/environment compatibility internally. The host selects approved bytes and owns stored-byte integrity; it supplies no expected digest, scope, consumer, compiler or environment admission metadata. This does not remove those fields from artifact/model contracts. Implementation and package evidence are recorded in [testing](../testing.md).

<a id="legacy-read1-envelope-and-compiler-boundary"></a>
<a id="implementation-reference-earlier-compiler-and-artifact-contracts"></a>
<a id="compiler-boundary"></a>
<a id="legacy-read-compiler-boundary"></a>

[Original read-only compiler and artifact](../internal/compatibility-reference.md#original-compiler-envelope).

## Canonical bytes and digest rules

The shared [byte ceilings](runtime.md#limits) apply separately to each raw source/DCM document and to the complete DCM object serialized with the canonical JSON rules below. Each ceiling is 65,536 bytes, inclusive; canonical escaping can expand a raw DCM that fits its input ceiling. After validating the DCM and before artifact emission, the compiler must reject canonical DCM overflow with `BUDGET_EXCEEDED` at `/dcm` and emit no artifact. The current runtime enforces model bounds during binding and reports size failures under `/binding/dcm`. Runtimes that use embedded models check that ceiling during preparation at `/artifact/dcm`. Failure produces no handle or dispatch. The separate whole-artifact ceiling does not relax either DCM ceiling; do not truncate the model or discard unused definitions to make it fit.

Canonical JSON is UTF-8, no BOM/trailing newline/whitespace, object keys sorted lexically by Unicode scalar value (schema keys and identifiers are ASCII), arrays retain order. Emit Boolean as `true`/`false`, integers as minimal unsigned decimal (metadata counts only), and strings with `"`/`\\` escaped, U+0000..U+001F encoded as lowercase `\u00xx`, all other valid Unicode scalars literally encoded in UTF-8. Do not normalize Unicode. Reject unpaired surrogates and numbers outside nonnegative safe integer metadata ranges; source binding numbers/null are unsupported. This recipe has no floats, negative zero or locale-dependent rendering.

Digest is `sha256:` plus lowercase SHA-256 hex of exactly those bytes. Source digest includes CEL source strings verbatim after JSON/YAML decoding (no expression whitespace rewriting). DCM digest includes the whole validated DCM, including enum order and consumer definition. Artifact digest covers the entire canonical envelope, including base64 expressions and any embedded descriptors. There is **no** self-hash field to exclude. For example canonical bytes for `{"b":true,"a":"x"}` are `{"a":"x","b":true}` with no newline. Developers must add byte/hash vectors including escaped controls and non-ASCII values.

Protobuf serialization must use deterministic ordering of maps/repeated definitions. Preserve source information in CheckedExpr only if deterministic and free of absolute paths; use a constant source description. Compile the same decoded source/model/environment twice and require identical full artifact bytes. Recompiling for each runtime is not conformance. Runners accept only the canonical envelope and standard base64; reject duplicate/unknown fields, extra trailing content and unsupported versions rather than guessing compatibility.

## Descriptor/type bridge

Produce one file `keir_read.proto`, syntax `proto3`, package `keir.read`. Each DCM record becomes a message of the same name; messages and their fields are ordered by ASCII name. Assign field numbers starting at 1 in that order; field type is `bool` or `string`, with scalar nominal/enum types represented as string. No protobuf enum, map, optional or repeated field is needed. Descriptor `json_name` equals the DCM field name exactly; this prevents language-specific camel-case reinterpretation. No unused descriptor imports/extensions/options. Scalars stay scalar CEL declarations for `let`; messages become `keir.read.<RecordName>`.

Validate complete closed values before constructing a message and after extracting any result. In particular `false` and empty String are present values, not missing fields. Proto3's default behavior is not validation. These profiles never select optional fields or call `has`. Nominal/enum identity is maintained by the compiler overlay and bound DCM, not invented protobuf scalar types.

Node and Java prepare the same checked expressions in environments derived from these descriptors and variables. Runners independently validate the checked AST, overloads, type/reference maps and nominal/enum overlays; evaluator acceptance alone is not validation. Missing checked metadata must be rejected before execution.

Supported artifact environment pins are `celJava: "0.14.0"`, `celNode: "0.6.1"`, `protobufJava: "4.35.1"`, `protobufNode: "2.6.2"`. The corresponding supporting packages are `dev.cel:protobuf:0.14.0` and `@bufbuild/cel-spec:0.6.1`. The latter matches the CEL package version. Java's planner enables heterogeneous numeric comparisons as an engine option; all numeric business AST/types remain rejected. This option adds no numeric language support. See [testing](../testing.md) for descriptor and engine verification evidence.

<a id="admission-and-trust"></a>

[Earlier admission and trust checks](../internal/compatibility-reference.md#external-admission-pins).
