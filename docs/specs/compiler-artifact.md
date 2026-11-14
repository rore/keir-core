<a id="read-and-decide-compiler-and-artifact"></a>
# Compiler and artifact contracts

<a id="effect4-identity"></a>
<a id="keir-ir-and-artifact-identity"></a>
## Source and artifact identity

The compiler checks a [Keir source document](language.md) against a trusted [domain capability model (DCM)](domain-model.md). It emits one canonical artifact that Java and Node can execute.

Compiler identity, document formats and runtime compatibility version independently. This matrix defines their exact supported combinations. The [companion catalog](../agents/authoring-companion.md) records package/guide availability; use [installation](../installation.md) for released packages.

| Compiler | Source | DCM | Artifact | Scenario | Descriptor recipe | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `keir-compiler-java/0.4.0` | `keir/2` | `keir-dcm/3` | `keir-artifact/3` | `keir-scenario/3` | `keir-record-descriptors/2` | Published; no numeric types or size |
| `keir-compiler-java/0.5.0` | `keir/2` | `keir-dcm/3` | `keir-artifact/4` | `keir-scenario/4` | `keir-record-descriptors/2` | Published; retained authoring, no numeric types or size |
| `keir-compiler-java/0.5.0` | `keir/3` | `keir-dcm/4` | `keir-artifact/4` | `keir-scenario/4` | `keir-record-descriptors/3` | Published; numeric authoring |

Structural schemas: retained [source `/2`](../../schemas/source-2/source.schema.json), [model `/3`](../../schemas/dcm-3/dcm.schema.json), [artifact `/3`](../../schemas/artifact-3/artifact.schema.json) and [scenario `/3`](../../schemas/scenario-3/scenario.schema.json); new [source `/3`](../../schemas/source-3/source.schema.json), [model `/4`](../../schemas/dcm-4/dcm.schema.json), [artifact `/4`](../../schemas/artifact-4/artifact.schema.json) and [scenario `/4`](../../schemas/scenario-4/scenario.schema.json). Semantic and cross-document checks remain required. Scenario `/4` covers both new artifact tuples without rewriting old scenarios or their identities. Its consumers require matching support; schemas alone do not qualify a runner.

The new compiler accepts exactly its two source/DCM pairs and always emits producer 0.5.0/artifact `/4`, even for nonnumeric programs. It rejects cross-pairs and DCM `/2` authoring. Retained `/2` source with DCM `/3` preserves model digest, recipe `/2` bytes, enum/nominal rules, default grants and 0.4 guard semantics, but the new artifact needs fresh pins and approval. Nominal String types named Integer/Decimal keep that meaning in `/3`. Original compiler packages remain the way to reproduce historical authoring behavior. Core 0.2.0's pair remains in the [historical matrix](workflow-expressions.md#version-matrix).

Runtime 0.4.0 admits the first four exact tuples below with the same environment pins. Runtime 0.3.1 admits only the first three, and runtime 0.3.0 only the first two. Runtime 0.5.0 adds the last two; older runtimes reject artifact `/4`.

| Artifact | Bound model | Producer | Descriptor recipe |
| --- | --- | --- | --- |
| `keir-artifact/2` | `keir-dcm/2` | `keir-compiler-java/0.2.0` | `keir-record-descriptors/2` |
| `keir-artifact/3` | `keir-dcm/3` | `keir-compiler-java/0.3.0` | `keir-record-descriptors/2` |
| `keir-artifact/3` | `keir-dcm/3` | `keir-compiler-java/0.3.1` | `keir-record-descriptors/2` |
| `keir-artifact/3` | `keir-dcm/3` | `keir-compiler-java/0.4.0` | `keir-record-descriptors/2` |
| `keir-artifact/4` | `keir-dcm/3` | `keir-compiler-java/0.5.0` | `keir-record-descriptors/2` |
| `keir-artifact/4` | `keir-dcm/4` | `keir-compiler-java/0.5.0` | `keir-record-descriptors/3` |

Mixed tuples, `/1` contracts and decision semantics inside `/2` are rejected by both runtimes. Producers absent from this matrix are also rejected. Existing approved artifacts keep their exact model, bytes and approval identities; upgrading the runtime does not recompile or deactivate them. New or edited source uses the host-selected authoring pair and requires normal approval of the new bytes. Historical source can seed a draft, but prior approval never transfers to a recompiled artifact. Match full identities, never semver ordering.

The exact 0.4.0 tuple and both 0.5 tuples admit guarded-result scalar dataflow. Earlier producers retain their original declaration/execution semantics. Only 0.5 with model `/4` admits numeric constructs. Relabeling bytes with an older producer, crossing model/recipe pairs or changing a schema never grants semantics or approval. Original golden artifacts and digests stay unchanged.

### Compile source

Build the [compiler CLI](../../compiler-java/README.md#build-and-run) on JDK 21. From the repository root, pass the source path followed by the trusted DCM path. The development JAR retains its Maven snapshot filename; the artifact records the compiler's contract identity separately.

```sh
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar source.yaml dcm.json > artifact.json
```

The CLI writes artifact bytes to stdout on exit 0. On failure it writes an array of `{code,path,message}` diagnostics to stderr. Exit 1 means the compiler rejected the inputs. Exit 2 means the number of arguments is wrong or an input file is unreadable. Use byte-preserving redirection, including PowerShell 7.4 or later on Windows.

That path is the ordinary source-build output from the unchanged snapshot POMs. The candidate release builder stages the pinned source separately and versions its temporary Maven inputs: the 0.5 compiler ZIP contains `keir-compiler-java-0.5.0-cli.jar`. Do not substitute that filename into a source build or treat a snapshot JAR as the qualified candidate archive. The CLI still takes only source and DCM paths, in that order; select exact verified assets separately from build coordinates.

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

Artifact `/3` and development `/4` keep the envelope `{format,compiler,environment,sourceDigest,modelRef,entrypoint,expressions,steps,return}` and the lowered step shapes below. Their exact producer/model/recipe tuple selects semantics. The selected entrypoint names the DCM entrypoint and `sourceDigest` identifies the canonical decoded source. The `environment` object contains exactly `celJava`, `celNode`, `protobufJava` and `protobufNode` with the values in [Descriptor/type bridge](#descriptortype-bridge).

### Model reference

`modelRef` is exactly `{domainId,domainVersion,format,digest,descriptorRecipe}`. The domain ID, version and format match the bound DCM. The digest covers the complete canonical DCM. The descriptor recipe identifies how to derive record descriptors from that DCM. The artifact carries neither the model bytes nor their descriptors.

### Lowered steps and expressions

| Item | Representation |
| --- | --- |
| Literal or expression binding | Exactly `{literal,type}` or `{expr,type}`. The type retains scalar, nominal, enum or named bounded-list identity. Literal payloads remain Boolean or String, including canonical numeric strings with their explicit numeric type; lists and timestamps use expression bindings. |
| Binding step | `{op:"BIND",id,value,resultType}`, where `value` is the lowered binding. |
| Call step | `{op:"CALL",id,capability,version,args,resultType,when?,onError?}`. Arguments contain only source-supplied parameters. `when` names the earlier Boolean BIND. `onError` maps declared codes to lowered return records. |
| Return record | `{type,fields}`, where `type` is the entrypoint's output type and `fields` maps required and supplied optional output fields to lowered bindings. Omitted optional fields remain absent. This shape is used for both the root return and each handler. |
| Checked expression | `{checked,variables,resultType}`. `checked` contains standard base64 of the protobuf `CheckedExpr`. For producers 0.4.0 and 0.5.0, `variables` is the complete prior declaration/type environment, including unused guarded CALL results. Earlier producers include only definitely available names and omit guarded results. A declaration is not permission to read its value. |

Assign expression IDs `e0`, `e1` and so on while visiting steps in source order. Visit call arguments by name, handler codes by code and handler fields by name, all in ASCII order. Visit root return fields last, also in ASCII order. A literal consumes no expression ID. Omitted optional arguments remain absent from `args`. The runtimes recompute availability and check the expression IDs and metadata during [preparation](host-model-binding.md#prepare-an-artifact), including same-expression optional presence proofs, exact list element types and timestamp operations. See the [Keir 2 checked forms](workflow-expressions.md#checked-expressions-and-descriptor-identity).

For guard-capable tuples, compiler and preparation independently reconstruct prior Boolean BINDs and guarded-result provenance from validated ordered steps. Arguments and handlers use pre-CALL snapshots. Preparation requires the exact declaration map and independently validates every checked AST node, reference/type map, overload and semantic overlay. In a scalar conditional, only its exact Boolean guard identifier adds result-existence proof to the true arm; false arms and later expressions gain none. Optional-field proof remains separate. No serialized proof, opcode or field grants this authority. At most nine names are declared: input plus eight preceding steps. [Sparse activation](runtime.md#guarded-result-activation) controls which declarations have invocation values.

The artifact carries no special decision opcode or native-commit callback. Its selected `/3` or `/4` model entrypoint determines REQUEST/BEFORE_COMMIT/DECISION semantics and the zero-write ceiling. Preparation independently rejects every possible WRITE, including a false-guarded call, before adapter dispatch. A valid decision result does not establish a native commit; the host applies the [consumption and consistency contract](pre-operation-decisions.md#consumption-and-failure-policy).

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

Recipe `/3`, used only with DCM `/4`, retains that ordering and adds `int64` for Integer. Optional Integer fields use the same deterministic synthetic-oneof recipe; required fields omit it. Decimal is logically scalar but uses `.keir.numeric.Decimal` with natural presence. Always include `keir_numeric.proto` for `/4`, even if no model field or parameter uses Decimal: expression-only constructors and BINDs need the type at model binding.

The fixed numeric dependency is proto3, package `keir.numeric`, message `Decimal` with exactly `string value = 1` and `json_name = "value"`. It has no imports/options/extensions/source information. FileDescriptorSet order is optional Timestamp dependency, mandatory numeric dependency, then `keir_read.proto`; the record file imports dependencies in that order. Recipe `/2` bytes never gain the numeric dependency. Compiler and both runtimes independently reconstruct descriptors; an arbitrary message declaration is not a Decimal bridge.

Validate complete closed values before constructing a message and after extracting a result. Zero, `false` and empty String are present; omitted optional fields stay absent and `null` is invalid. Proto3 defaults do not validate requiredness. Numeric host strings map internally to long/bigint or the opaque Decimal message. Decimal internals, general protobuf constructors and `has(d.value)` are unavailable to source. Exact numeric/nominal/enum/list identity comes from the semantic overlay and DCM, not String storage.

Node and Java prepare the same checked expressions in environments derived from these descriptors and variables. Runners independently validate the checked AST, overloads, type/reference maps and nominal/enum overlays; evaluator acceptance alone is not validation. Missing checked metadata must be rejected before execution.

Supported artifact environment pins are `celJava: "0.14.0"`, `celNode: "0.6.1"`, `protobufJava: "4.35.1"`, `protobufNode: "2.6.2"`. Supporting packages are `dev.cel:protobuf:0.14.0` and `@bufbuild/cel-spec:0.6.1`. Java's heterogeneous-comparison option grants no mixed numeric comparisons or numeric support to retained profiles. See [testing](../internal/history/verification.md) for verification evidence.

### Numeric checked operations

Only the numeric profile admits the following additional checked forms. `I`, `D`, `S` and `B` mean Integer, Decimal, String and Boolean. Every call is global, without a receiver. [Source literal restrictions](language.md#numeric-syntax-and-types) and exact semantic typing apply in addition to these signatures.

| Function | Arguments → result | Overload |
| --- | --- | --- |
| `decimal` | S → D | `keir_decimal_from_string` |
| `toDecimal` | I → D | `keir_decimal_from_integer` |
| `toInteger` | D → I | `keir_integer_from_decimal` |
| `_+_`, `_-_`, `_*_`, `_/_` | D,D → D | `keir_decimal_add`, `keir_decimal_subtract`, `keir_decimal_multiply`, `keir_decimal_divide` |
| `-_` | D → D | `keir_decimal_negate` |
| `_<_`, `_<=_`, `_>_`, `_>=_` | D,D → B | `keir_decimal_less`, `keir_decimal_less_equal`, `keir_decimal_greater`, `keir_decimal_greater_equal` |
| `round` | D,I,S → D | `keir_decimal_round` |
| `multiplyRounded`, `divideRounded` | D,D,I,S → D | `keir_decimal_multiply_rounded`, `keir_decimal_divide_rounded` |
| `size` | Named bounded list → I | `size_list` |

Integer arithmetic uses `add_int64`, `subtract_int64`, `multiply_int64`, `divide_int64`, `negate_int64`; ordering uses `less_int64`, `less_equals_int64`, `greater_int64`, `greater_equals_int64`. Equality/inequality use `equals`/`not_equals` only after exact scalar-type checks, including canonical Decimal equality. The trusted Integer division binding rejects a nonzero remainder; native truncating division is not this contract. Numeric functions are fixed runtime bindings, not user extension points. Reject double/uint/dyn, wrong or extra overloads, hidden metadata and internal Decimal access before execution. Node's planner must resolve its own matching trusted bindings; metadata strings alone do not select execution.

<a id="admission-and-trust"></a>

[Earlier admission and trust checks](../internal/history/compatibility-reference.md#external-admission-pins).
