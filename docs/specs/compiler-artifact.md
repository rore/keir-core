<a id="read-and-decide-compiler-and-artifact"></a>
# Compiler and artifact contracts

<a id="effect4-identity"></a>
<a id="keir-ir-and-artifact-identity"></a>
## Source and artifact identity

The compiler checks a [Keir source document](language.md) against a trusted [domain capability model (DCM)](domain-model.md). It emits one canonical artifact that Java and Node can execute.

The current authoring contract is reserved for Core 0.3.0 and remains candidate until qualification and release evidence is available. It keeps the Keir 2 source language and descriptor recipe while adding the [decision lifecycle](pre-operation-decisions.md). These identities version independently:

| Field | Current authoring value (Core 0.3.0 candidate) |
| --- | --- |
| Source `language` | `keir/2` |
| DCM `format` | `keir-dcm/3` |
| Artifact `format` | `keir-artifact/3` |
| Verification scenario `format` | `keir-scenario/3` |
| Compiler identity | `keir-compiler-java/0.3.0` |
| Descriptor recipe | `keir-record-descriptors/2` |

Structural schemas: [source](../../schemas/source-2/source.schema.json), [model](../../schemas/dcm-3/dcm.schema.json), [artifact](../../schemas/artifact-3/artifact.schema.json) and [scenario](../../schemas/scenario-3/scenario.schema.json). Semantic and cross-document checks remain required.

Compiler 0.3.0 is the single current authoring compiler: it accepts source `/2` with DCM `/3` only and produces artifact `/3` with its own 0.3.0 identity. It rejects DCM `/2` and never stamps the old producer identity. The exact Core 0.2.0 contracts remain documented in the [workflow version matrix](workflow-expressions.md#version-matrix) for execution and history.

Runtime 0.3.0 admits exactly these two tuples, with the environment pins below in both:

| Artifact | Bound model | Producer | Descriptor recipe |
| --- | --- | --- | --- |
| `keir-artifact/2` | `keir-dcm/2` | `keir-compiler-java/0.2.0` | `keir-record-descriptors/2` |
| `keir-artifact/3` | `keir-dcm/3` | `keir-compiler-java/0.3.0` | `keir-record-descriptors/2` |

Mixed tuples, `/1` contracts and decision semantics inside `/2` are rejected by runtime 0.3.0. Existing approved event artifacts keep their exact old model and approval identities; upgrading the runtime does not recompile or deactivate them. New or edited source uses the current `/3` catalog, including unchanged EVENT entrypoints, and requires normal approval of the new bytes. Historical source can seed a draft, but its prior approval never transfers to the recompiled artifact.

### Compile source

Build the [compiler CLI](../../compiler-java/README.md#build-and-run) on JDK 21. From the repository root, pass the source path followed by the trusted DCM path. The development JAR retains its Maven snapshot filename; the artifact records the compiler's contract identity separately.

```sh
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar source.yaml dcm.json > artifact.json
```

The CLI writes artifact bytes to stdout on exit 0. On failure it writes an array of `{code,path,message}` diagnostics to stderr. Exit 1 means the compiler rejected the inputs. Exit 2 means the number of arguments is wrong or an input file is unreadable. Use byte-preserving redirection, including PowerShell 7.4 or later on Windows.

In Java, call `new KeirCompiler().compile(sourceBytes, trustedDcmBytes, KeirCompiler.Environment.supported())`. A successful `Compilation` has `artifact()`, `artifactDigest()` and an empty `diagnostics()` list. A failed compilation returns diagnostics and null artifact bytes and digest. `succeeded()` distinguishes the two outcomes.

### Input encodings

Both compiler inputs accept a single UTF-8 JSON document or Keir's restricted YAML data syntax. Use UTF-8 without a byte order mark. YAML supports mappings with string keys, sequences and scalar values. The compiler rejects duplicate keys, aliases, anchors, merge keys, explicit tags and multiple documents. Quote strings that would otherwise be decoded as Boolean, null or numeric values. The source and DCM rules still determine which values each field accepts.

The runtime's [model binding](host-model-binding.md#bind-the-model) accepts JSON. Use a JSON DCM to share one file between the compiler and runtime. If you maintain a YAML DCM, supply its equivalent JSON to the runtime. The model identity hashes the decoded model in canonical JSON. Changes to formatting or key order preserve that identity, while a conversion that changes a value, type or array order changes it. With the same decoded source, model and compiler configuration, equivalent YAML and JSON produce identical artifact bytes.

The host selects the compiler configuration and trusted model independently of the source. The source names a declared entrypoint and supplies its model's domain ID, version and canonical digest. The compiler checks the source, model, matching identities, types, data flow and [static limits](runtime.md#limits). A host running the compiler as a subprocess bounds time, concurrency and output, and reaps the process on cancellation.

### Diagnostics

Compilation currently returns the first failure. The diagnostic path identifies the source or model location. For example, `/source/steps/2/when` identifies the guard on the third step. Messages are safe summaries. Use the code and path to classify a rejection.

| Rejection | Codes |
| --- | --- |
| Input syntax, shape or construct | `SYNTAX_INVALID`, `FIELD_UNKNOWN`, `PROFILE_UNSUPPORTED` |
| Model or compiler environment | `DCM_INVALID`, `PIN_MISMATCH` |
| Capability selection or protected input | `CAPABILITY_UNKNOWN`, `ENTRY_GRANT_DENIED`, `HOST_BINDING_OVERRIDE` |
| Type or binding | `TYPE_MISMATCH`, `NOMINAL_MISMATCH`, `ENUM_MISMATCH`, `DATAFLOW_UNAVAILABLE` |
| Expression or resource policy | `CEL_POLICY_DENIED`, `BUDGET_EXCEEDED` |

See [source diagnostics](language.md#diagnostics-and-conformance) for examples of these rules on a source document.

## Artifact representation

The [artifact `/3` schema](../../schemas/artifact-3/artifact.schema.json) defines the envelope `{format,compiler,environment,sourceDigest,modelRef,entrypoint,expressions,steps,return}`. Its lowered steps retain the `/2` representation; the new format, producer and model-reference identities admit the decision lifecycle. The selected entrypoint names the DCM entrypoint and `sourceDigest` identifies the canonical decoded source. The `environment` object contains exactly `celJava`, `celNode`, `protobufJava` and `protobufNode` with the values in [Descriptor/type bridge](#descriptortype-bridge).

### Model reference

`modelRef` is exactly `{domainId,domainVersion,format,digest,descriptorRecipe}`. The domain ID, version and format match the bound DCM. The digest covers the complete canonical DCM. The descriptor recipe identifies how to derive record descriptors from that DCM. The artifact carries neither the model bytes nor their descriptors.

### Lowered steps and expressions

| Item | Representation |
| --- | --- |
| Literal or expression binding | Exactly `{literal,type}` or `{expr,type}`. The type retains the resolved scalar, nominal, enum or named bounded-list identity. Literal payloads remain Boolean or String; lists and timestamps use expression bindings. |
| Binding step | `{op:"BIND",id,value,resultType}`, where `value` is the lowered binding. |
| Call step | `{op:"CALL",id,capability,version,args,resultType,when?,onError?}`. Arguments contain only source-supplied parameters. `when` names the earlier Boolean BIND. `onError` maps declared codes to lowered return records. |
| Return record | `{type,fields}`, where `type` is the entrypoint's output type and `fields` maps required and supplied optional output fields to lowered bindings. Omitted optional fields remain absent. This shape is used for both the root return and each handler. |
| Checked expression | `{checked,variables,resultType}`. `checked` contains standard base64 of the protobuf `CheckedExpr`. `variables` maps every definitely available name, including unused names, to its type. A guarded call never adds a variable. |

Assign expression IDs `e0`, `e1` and so on while visiting steps in source order. Visit call arguments by name, handler codes by code and handler fields by name, all in ASCII order. Visit root return fields last, also in ASCII order. A literal consumes no expression ID. Omitted optional arguments remain absent from `args`. The runtimes recompute availability and check the expression IDs and metadata during [preparation](host-model-binding.md#prepare-an-artifact), including same-expression optional presence proofs, exact list element types and timestamp operations. See the [Keir 2 checked forms](workflow-expressions.md#checked-expressions-and-descriptor-identity).

The artifact carries no special decision opcode or native-commit callback. Its selected `/3` model entrypoint determines REQUEST/BEFORE_COMMIT/DECISION semantics and the zero-write ceiling. Preparation independently rejects every possible WRITE, including a false-guarded call, before adapter dispatch. A valid decision result does not establish a native commit; the host applies the [consumption and consistency contract](pre-operation-decisions.md#consumption-and-failure-policy).

The host selects approved artifact bytes and verifies their stored-byte integrity before preparation. The compiler-produced digest identifies the bytes. It does not establish who approved them or authenticate them on its own. Execution uses the prepared artifact and bound model without a compiler or source parser.

<a id="legacy-read1-envelope-and-compiler-boundary"></a>
<a id="implementation-reference-earlier-compiler-and-artifact-contracts"></a>
<a id="compiler-boundary"></a>
<a id="legacy-read-compiler-boundary"></a>

[Original read-only compiler and artifact](../internal/history/compatibility-reference.md#original-compiler-envelope).

## Canonical bytes and digest rules

The shared [byte ceilings](runtime.md#limits) apply separately to each raw source/DCM document and to the complete DCM object serialized with the canonical JSON rules below. Each ceiling is 65,536 bytes, inclusive. Canonical escaping can expand a raw DCM that fits its input ceiling. The compiler rejects canonical DCM overflow with `BUDGET_EXCEEDED` at `/dcm`. The runtime enforces model bounds during binding and reports size failures under `/binding/dcm`. The whole-artifact ceiling is independent of both DCM ceilings.

Canonical JSON is UTF-8, no BOM/trailing newline/whitespace, object keys sorted lexically by Unicode scalar value (schema keys and identifiers are ASCII), arrays retain order. Emit Boolean as `true`/`false`, integers as minimal unsigned decimal (metadata counts only), and strings with `"`/`\\` escaped, U+0000..U+001F encoded as lowercase `\u00xx`, all other valid Unicode scalars literally encoded in UTF-8. Do not normalize Unicode. Reject unpaired surrogates and numbers outside nonnegative safe integer metadata ranges; source binding numbers/null are unsupported. This recipe has no floats, negative zero or locale-dependent rendering.

Digest is `sha256:` plus lowercase SHA-256 hex of exactly those bytes. Source digest includes CEL source strings verbatim after JSON/YAML decoding (no expression whitespace rewriting). DCM digest includes the whole validated DCM, including enum order and consumer definition. Artifact digest covers the entire canonical envelope, including base64 expressions and any embedded descriptors. There is **no** self-hash field to exclude. For example canonical bytes for `{"b":true,"a":"x"}` are `{"a":"x","b":true}` with no newline. Developers must add byte/hash vectors including escaped controls and non-ASCII values.

Protobuf serialization uses deterministic ordering of maps and repeated definitions. The compiler writes `<input>` as the checked expression source location, and both runtimes require that value. The same decoded source, model and environment produce identical artifact bytes. Conformance runs those same bytes in both runtimes. Runtimes require the canonical envelope and standard base64 and reject duplicate or unknown fields, trailing content and unsupported versions.

## Descriptor/type bridge

Recipe `keir-record-descriptors/2` produces `keir_read.proto`, syntax `proto3`, package `keir.read`. Each DCM record becomes a message of the same name; messages and their fields are ordered by ASCII name. Assign field numbers starting at 1 in that order. Boolean uses `bool`; String, nominal and enum values use `string`, with no protobuf enum or map. List fields are repeated BOOL or STRING. Other fields use the optional label. Descriptor `json_name` equals the DCM field name exactly, preventing language-specific camel-case reinterpretation. Scalars stay scalar CEL declarations for `let`; messages become `keir.read.<RecordName>`.

Optional Boolean and String-based scalar fields use `proto3_optional` with a synthetic oneof. Required Boolean and String-based scalars omit `proto3_optional` and `oneof_index`. Oneofs follow field order with zero-based indexes; names start with `_` plus the field name, adding leading underscores until unique among field and oneof names. Timestamp fields use `.google.protobuf.Timestamp` and natural presence. Requiredness is checked before conversion.

Include `google/protobuf/timestamp.proto` only when the model uses Timestamp. It precedes `keir_read.proto` and is its named dependency. The standard descriptor is 255 bytes with SHA-256 `422a163a2d7051465e0a516584b4d96d18c270ec4e7ad3c21ad33e5c440ebecc`. Timestamp seconds use the standard epoch; nanos range from 0 through 999,999,999 and are divisible by 1,000,000. Duration literals have nonnegative seconds and zero nanos. Deterministic validation rejects unknown fields, extra imports, options, extensions and source information. See the [workflow descriptor reference](workflow-expressions.md#checked-expressions-and-descriptor-identity) for checked operation identities.

Validate complete closed values before constructing a message and after extracting any result. In particular `false` and empty String are present values, not missing fields; omitted optional fields stay absent and `null` is invalid. Proto3's default behavior is not validation. Nominal/enum identity and list element identity are maintained by the compiler overlay and bound DCM, not invented protobuf scalar types.

Node and Java prepare the same checked expressions in environments derived from these descriptors and variables. Runners independently validate the checked AST, overloads, type/reference maps and nominal/enum overlays; evaluator acceptance alone is not validation. Missing checked metadata must be rejected before execution.

Supported artifact environment pins are `celJava: "0.14.0"`, `celNode: "0.6.1"`, `protobufJava: "4.35.1"`, `protobufNode: "2.6.2"`. The corresponding supporting packages are `dev.cel:protobuf:0.14.0` and `@bufbuild/cel-spec:0.6.1`. The latter matches the CEL package version. Java's planner enables heterogeneous numeric comparisons as an engine option; all numeric business AST/types remain rejected. This option adds no numeric language support. See [testing](../testing.md) for descriptor and engine verification evidence.

<a id="admission-and-trust"></a>

[Earlier admission and trust checks](../internal/history/compatibility-reference.md#external-admission-pins).
