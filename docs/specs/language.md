<a id="read-and-decide-language-contract"></a>
<a id="rule-source-and-expressions"></a>
<a id="keir-ir-source-and-expressions"></a>
# Keir source and expressions

Keir source defines how one host entrypoint combines typed data and approved operations. The [`keir/2` source schema](../../schemas/source-2/source.schema.json) checks decoded structure. The compiler also checks the trusted [domain capability model](domain-model.md), expression types, availability, and call limits.

This page describes current `keir/2` authoring, including bounded lists, optional values and timestamps. The [workflow expression reference](workflow-expressions.md) specifies their checked forms and detailed examples. The [pre-operation decision contract](pre-operation-decisions.md) reserves Core 0.3.0 for authoring against DCM `/3`; it adds an entrypoint lifecycle, not source syntax. Core 0.3.0 remains candidate until qualification and release evidence is available. See the [exact version rules](compiler-artifact.md#source-and-artifact-identity) before choosing a compiler or model.

## Source document

The compiler accepts bounded UTF-8 JSON or YAML. The document and each nested object have a closed set of fields. Field names are unique. See [compiler and artifact contracts](compiler-artifact.md) for parsing, model selection and canonicalization.

| Required field | Meaning |
| --- | --- |
| `language` | The literal `keir/2`. |
| `domain` | Exactly `id`, `version` and `fingerprint`. The first two must equal the trusted DCM's domain fields. The fingerprint is its canonical SHA-256 digest as `sha256:` plus 64 lowercase hexadecimal digits. |
| `entrypoint` | The exact ID of a declared [entrypoint](domain-model.md#entrypoints). It determines the input and output records and call policy. |
| `name` | A source label matching `[a-z][a-z0-9-]{0,63}`. |
| `steps` | An ordered list of one to eight bindings or calls. |
| `return` | Exactly `{record: {field: binding, ...}}`, with every required field of the entrypoint output and its declared type. Optional scalar fields may be omitted. Unknown fields are invalid. |

Step IDs, argument names, and output field names match `[A-Za-z][A-Za-z0-9]{0,63}`. Step IDs are unique within the program and must not shadow `input`, [reserved words](domain-model.md#identifiers), `String`, `Boolean`, `Timestamp`, `Duration`, or names declared in the DCM's `types`. Entrypoint and capability IDs match `[A-Za-z][A-Za-z0-9.]{0,99}` and resolve in their respective DCM maps.

## Steps and data flow

| Step | Fields and behavior |
| --- | --- |
| Bind a value | `{id, let}`. Evaluate the binding in `let` and name its scalar or bounded-list result with `id`. Every `let` is unconditional. |
| Call a capability | `{id, call, args}`, with optional `when` and `onError`. `call` names a capability. `args` supplies its required `agent` parameters and any supplied optional ones, by name and declared type. Omit an optional argument by leaving out its key. Use `{}` when none are supplied. The host supplies `host` parameters independently. |

Each expression can use `input`, earlier `let` values, and the record results of earlier unguarded calls on the continuing success path. Host bindings, event metadata and the original request key are outside this environment. Forward references, references to a guarded result, and references to unavailable names fail with `DATAFLOW_UNAVAILABLE`. An expression inside a conditional has the same availability rules.

**Guards.** `when` names an earlier `let` of type `Boolean`. It takes a step ID rather than an expression. A false guard skips argument evaluation, dispatch and handler evaluation. The runtime still validates the result of a successful guarded call, but its result is unavailable to later expressions. A guard has no effect on static call and write counts.

**Business rejection handlers.** `onError` maps one to 16 distinct declared capability error codes to return records with the same shape as the root `return`. Codes match `[A-Z][A-Z0-9_]{0,63}`. A handler uses the environment immediately before its call. A declared business rejection ends the invocation, with a handler result if a matching handler succeeds. Neither later steps nor the root return run. Handlers cannot make calls or catch validation, resource, transient or uncertain failures. See [runtime execution](runtime.md) for adapter responses, receipts and termination.

The compiler counts every possible call, including calls behind a false or mutually exclusive guard. An extension can contain at most four calls and one possible `WRITE`, subject to [the entrypoint policy](domain-model.md#call-and-write-policy). A REQUEST/BEFORE_COMMIT/DECISION entrypoint permits no possible WRITE, even behind a false guard. Its return record must contain the required named-enum `decision` value `ALLOW` or `DENY`; ordinary declared bounded scalar fields may accompany it. Returning ALLOW permits the host to continue its checks; it is not evidence of native commit. A handled business rejection remains a rejection and cannot authorize continuation.

## Bindings

The same binding forms apply in a `let`, a call argument, a handler field or a return field. A binding uses exactly one form:

| Form | Meaning |
| --- | --- |
| `{literal: true}` or `{literal: "Follow up"}` | A Boolean or String literal. A String literal can also supply a declared enum member at a position expecting that enum. |
| `{literal: "project1", type: ProjectId}` | An explicit nominal String literal. The annotation must name a nominal String type declared in the DCM. |
| `{cel: input.taskId}` | A CEL expression whose result is scalar or a declared bounded list and matches the expected semantic type, where one is specified. |

A business string literal can be empty and contains at most 256 UTF-8 bytes. A CEL source expression contains at most 512 UTF-8 bytes. A nominal annotation gives a fixed string its business type. It does not grant access to the named resource or turn a runtime String into a nominal value. The compiler resolves the annotation only in the DCM's business type namespace. A host key type with the same spelling is separate metadata.

An unknown or non-nominal annotation reports `NOMINAL_MISMATCH` at `/type` relative to the binding. A valid nominal annotation incompatible with the expected type reports it at `/literal`. A protected argument override reports `HOST_BINDING_OVERRIDE`, regardless of annotation.

## Types and CEL bridge

The [model type system](domain-model.md#types) defines scalars, bounded scalar lists and records. Expressions can select fields from records and produce scalar or declared list values, but cannot construct records or lists. Each record field carries its declared semantic type into the expression. Lists are host-supplied ordered arrays with at most their declared zero-to-16 item cap; duplicates are allowed. Select or pass a list unchanged, or test exact membership with `value in list`. The member must have the element's exact semantic type; a direct enum member literal is also accepted for an enum list. No indexing, list equality, nested lists or list transformations are supported.

The [descriptor bridge](compiler-artifact.md#descriptortype-bridge) maps Boolean to protobuf `bool`, String/nominal/enum values to `string`, lists to repeated scalar fields, and Timestamp to `google.protobuf.Timestamp`. Values are validated for required fields, presence, enum membership and bounds before protobuf construction. An omitted optional scalar is absent; `null` is invalid, while `false` and an empty String are present. Lists are always required values.

Allowed expression syntax includes Boolean/string constants, identifiers, declared field selection, `==`/`!=` on Boolean/String/nominal/enum values, `!`, `&&`, `||`, `?:` with compatible scalar branches, and `+` on two plain `String` operands. Keir 2 also admits scalar list membership, direct optional-field `has`, literal Timestamp/Duration constructors, Timestamp plus Duration, Timestamp `==`, and strict Timestamp ordering. Timestamp `!=` is unsupported; negate equality instead. No record/list equality, indexing, general constructors, conversions, comprehensions or other calls are supported. Inspect checked AST and resolved references/overloads, not source spelling alone. CEL type checking must succeed without `dyn`; only standard implementations of the admitted operations in the pinned environment are available. Exact observed overload identifiers are part of the qualification evidence, not inferred from function spelling.

Use `has(record.field)` only on a direct selection of a declared optional field. Reading it requires a presence proof in the same expression: in the true arm of `has(x.f) ? ... : ...`, the right operand of `has(x.f) && ...`, or the false arm of `!has(x.f) ? ... : ...`. A separate Boolean binding does not carry that proof to a later expression. An unproven read reports `DATAFLOW_UNAVAILABLE`.

Timestamp wire values use exact UTC `YYYY-MM-DDTHH:mm:ss.SSSZ`, Gregorian years 0001 through 9999, valid calendar dates and seconds 00 through 59. `timestamp("2026-01-01T00:00:00.000Z")` requires one literal String in that form. `duration("604800s")` creates an expression-only Duration: `0s` or unsigned whole seconds with no leading zero, up to `31536000s`. Duration is not a model value or a standalone binding result. Timestamp equality and `<` compare instants; Timestamp plus Duration preserves milliseconds. There is no implicit clock: the host projects any reference time into input. See [elapsed-time semantics](workflow-expressions.md#timestamps-and-elapsed-durations) for overflow and CEL error masking; an escaping overflow prevents dispatch and reports `BUDGET_EXCEEDED` without erasing earlier write evidence.

The checked pair `_+_`/`add_string` takes exactly two plain `String` operands and returns a plain `String`. Concatenation preserves Unicode exactly. A 64-node expression can concatenate at most 32 bounded string leaves, so a single intermediate string has at most 8,192 UTF-8 bytes. Each String expression result is still limited to 256 UTF-8 bytes when it leaves the evaluator. A larger title expression result fails before a create call is dispatched.

The [scalar qualification](../../conformance/cel-profile/README.md) observed these function/overload pairs: `_==_`/`equals`, `_!=_`/`not_equals`, `!_`/`logical_not`, `_&&_`/`logical_and`, `_||_`/`logical_or`, `_?_:_`/`conditional`, with arities 2, 2, 1, 2, 2, 3 respectively and no receiver target. Reject unknown or missing overload references. The generic equality overload still requires the scalar semantic checks below; its name does not admit CEL's other comparable types. Unary negation chains can simplify during parsing: count the expanded checked AST, not source operator characters. This expression checkpoint does not establish the complete Keir overlay or program compiler.

The compiler's semantic overlay retains nominal and enum types through selections and scalar bindings. Nominal equality, assignment and conditional branches require the same nominal type. An unannotated literal cannot supply a nominal value. Enum equality may compare values of the same enum or compare one to a declared member literal. Enum assignment accepts a value of that enum, a member literal, or a conditional whose branches satisfy that rule. Runtime validation checks value shape and membership using the compiled binding types.

Assignment preserves exact semantic types. There is no enum-to-String or nominal-to-String conversion. Literal refinement is local to the expression being checked against an enum. For example, `let: {literal: "DONE"}` binds a String. A later reference to that variable cannot satisfy an enum just because its initializer was a member. A `let` that reads an enum-valued field retains that enum.

<a id="compiler-diagnostics"></a>
## Diagnostics and conformance

Compilation failure produces no artifact. See [compiler diagnostics](compiler-artifact.md) for the diagnostic format. The compiler checks document shape, trusted model identity and structural preflight before it checks CEL. Preflight checks guard references, capability and parameter names, required bindings, handler codes, output shape, literal compatibility and the conservative call and write counts.

Expressions are checked in step order. In a call, arguments come first in ASCII name order, then handlers in ASCII code order and their fields in ASCII name order. Root return fields come last in ASCII name order. Each expression completes parsing, typing and policy checks before the next. The compiler checks the aggregate AST budget after lowering, including all handler expressions. See [limits](runtime.md#limits) for per-expression and program ceilings.

For example, `when` pointing at a call or future step reports `DATAFLOW_UNAVAILABLE` at `/source/steps/<index>/when`. A guard pointing at an earlier non-Boolean `let` reports `TYPE_MISMATCH`. A second possible write reports `BUDGET_EXCEEDED`, and a write barred by the model's zero effect grant reports `ENTRY_GRANT_DENIED`. Malformed or ill-typed CEL reports `TYPE_MISMATCH`. A checked expression that uses forbidden CEL syntax reports `CEL_POLICY_DENIED`.

The [structural checker](../../scripts/check-keir-contracts.py) checks the available fixtures against their versioned schemas. The [compiler tests](../../compiler-java/src/test/java/dev/keir/compiler/GenericHookCompilerTest.java) cover nominal literals, handlers and String concatenation. The [Keir 2 expression examples](workflow-expressions.md#inventory-example) provide exact Core 0.2.0 model/artifact pairs and shared scenarios; they are not `/3` producer or qualification evidence.

## Example source

The complete [input-only decision source](../../conformance/keir-3/source-guard-input.json) reads a Boolean and returns ALLOW or DENY with a message. The [READ-based source](../../conformance/keir-3/source-guard.json) first calls a host capability, then combines its result with the input. Both use the exact [decision model](../../conformance/keir-3/dcm-guard.json). See the [walkthrough](../decision-walkthrough.md) for their roles and independent native checks. These `/3` fixtures are candidate inputs; package qualification is recorded separately.

<a id="source-and-example"></a>
<a id="implementation-reference-earlier-source-rules-and-example"></a>

[Original read-only source and example](../internal/history/compatibility-reference.md#original-read-source).
