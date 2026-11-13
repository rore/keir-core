<a id="read-and-decide-language-contract"></a>
<a id="rule-source-and-expressions"></a>
<a id="keir-ir-source-and-expressions"></a>
# Keir source and expressions

Keir source defines how one host entrypoint combines typed data and approved operations. The compiler checks source structure, the trusted [domain capability model](domain-model.md), expression types, availability and call limits.

Published Core 0.4.0 uses `keir/2` with DCM `/3`, including bounded lists, optional values, timestamps and exact-guard scalar results. Development Core 0.5 additionally supports numeric `keir/3` with DCM `/4` and retains the old authoring pair. Numeric behavior has compiler/Java/Node source-regression evidence, not yet matching-package or complete-feature qualification. Use only a host-supported [exact tuple](compiler-artifact.md#source-and-artifact-identity). The [workflow reference](workflow-expressions.md) defines the operations below; old packages do not gain them by reading this page.

## Source document

The compiler accepts bounded UTF-8 JSON or YAML. The document and each nested object have a closed set of fields. Field names are unique. See [compiler and artifact contracts](compiler-artifact.md) for parsing, model selection and canonicalization.

| Required field | Meaning |
| --- | --- |
| `language` | `keir/2` with DCM `/3`, or development numeric `keir/3` with DCM `/4`. Cross-pairs are invalid. |
| `domain` | Exactly `id`, `version` and `fingerprint`. The first two must equal the trusted DCM's domain fields. The fingerprint is its canonical SHA-256 digest as `sha256:` plus 64 lowercase hexadecimal digits. |
| `entrypoint` | The exact ID of a declared [entrypoint](domain-model.md#entrypoints). It determines the input and output records and call policy. |
| `name` | A source label matching `[a-z][a-z0-9-]{0,63}`. |
| `steps` | An ordered list of one to eight bindings or calls. |
| `return` | Exactly `{record: {field: binding, ...}}`, with every required field of the entrypoint output and its declared type. Optional scalar fields may be omitted. Unknown fields are invalid. |

Step IDs, argument names, and output field names match `[A-Za-z][A-Za-z0-9]{0,63}`. Step IDs are unique within the program and must not shadow `input`, [reserved words](domain-model.md#identifiers), `String`, `Boolean`, `Timestamp`, `Duration`, or names declared in the DCM's `types`. Entrypoint and capability IDs match `[A-Za-z][A-Za-z0-9.]{0,99}` and resolve in their respective DCM maps.

The numeric profile also reserves `Integer` and `Decimal`. In retained DCM `/3`, those spellings can still name nominal String types; they never acquire numeric meaning retroactively. The [`/2` source schema](../../schemas/source-2/source.schema.json) stays unchanged. Development `/3` has its own schema, selected by the exact format matrix.

## Steps and data flow

| Step | Fields and behavior |
| --- | --- |
| Bind a value | `{id, let}`. Evaluate the binding in `let` and name its scalar or bounded-list result with `id`. Every `let` is unconditional. |
| Call a capability | `{id, call, args}`, with optional `when` and `onError`. `call` names a capability. `args` supplies its required `agent` parameters and any supplied optional ones, by name and declared type. Omit an optional argument by leaving out its key. Use `{}` when none are supplied. The host supplies `host` parameters independently. |

Each expression can use `input`, earlier `let` values, and the record results of earlier unguarded calls on the continuing success path. Host bindings, event metadata and the original request key are outside this environment. Forward, self-result and otherwise unavailable references fail with `DATAFLOW_UNAVAILABLE`.

**Guards.** `when` names an earlier unconditional `let` of type `Boolean`. It takes a step ID rather than an expression. A false guard skips argument evaluation, dispatch and handler evaluation. A guard has no effect on static call and write counts.

**Guarded scalar results.** Producer `keir-compiler-java/0.4.0` and both development 0.5 profiles permit `g ? r.field : fallback` when an earlier CALL `r` has `when: g`. The checked condition must be that exact Boolean BIND identifier. Its true arm may select declared scalar fields from `r`; the false arm gains no proof. The proof remains inside nested subexpressions of that true arm and ends with the expression. Parentheses that parse to the same identifier are immaterial. A separate alias, `g == true`, `g && other`, negation or a constant condition supplies no proof. If the CALL itself uses an alias BIND as its guard, that alias is the exact identifier required.

Optional scalar fields need both result-existence and field-presence proofs: `g ? (has(r.note) ? r.note : "") : ""` is valid; `g ? r.note : ""` and `has(r.note)` without `g` are not. All scalar identities remain exact, including numeric types in the numeric profile. Bare guarded records, list-field reads (including membership or size), record/list conditional results, null and implicit conversions remain unsupported. Unproved access reports `DATAFLOW_UNAVAILABLE`; prohibited record/list forms report `CEL_POLICY_DENIED` after ordinary CEL typing.

This rule applies uniformly to later BINDs, arguments, handler fields and return fields. A consuming CALL's `when` supplies no implicit proof to its arguments or handler. Arguments and handlers use the environment before their own CALL; neither sees its own result. A valid scalar conditional BIND becomes an ordinary available value, not an escaping proof. Retained producers 0.2.0, 0.3.0 and 0.3.1 keep their original rule: guarded results are unavailable. Use only packages that explicitly support the [exact tuple](compiler-artifact.md#source-and-artifact-identity).

**Business rejection handlers.** `onError` maps one to 16 distinct declared capability error codes to return records with the same shape as the root `return`. Codes match `[A-Z][A-Z0-9_]{0,63}`. A handler uses the environment immediately before its call. A declared business rejection ends the invocation, with a handler result if a matching handler succeeds. Neither later steps nor the root return run. Handlers cannot make calls or catch validation, resource, transient or uncertain failures. See [runtime execution](runtime.md) for adapter responses, receipts and termination.

The compiler counts every possible call, including calls behind a false or mutually exclusive guard. An extension can contain at most four calls and one possible `WRITE`, subject to [the entrypoint policy](domain-model.md#call-and-write-policy). A REQUEST/BEFORE_COMMIT/DECISION entrypoint permits no possible WRITE, even behind a false guard. Its return record must contain the required named-enum `decision` value `ALLOW` or `DENY`; ordinary declared bounded scalar fields may accompany it. Returning ALLOW permits the host to continue its checks; it is not evidence of native commit. A handled business rejection remains a rejection and cannot authorize continuation.

## Bindings

The same binding forms apply in a `let`, a call argument, a handler field or a return field. A binding uses exactly one form:

| Form | Meaning |
| --- | --- |
| `{literal: true}` or `{literal: "Follow up"}` | A Boolean or String literal. A String literal can also supply a declared enum member at a position expecting that enum. |
| `{literal: "project1", type: ProjectId}` | An explicit nominal String literal. The annotation must name a nominal String type declared in the DCM. |
| `{literal: "10", type: Integer}` or `{literal: "0.075", type: Decimal}` | Numeric-profile typed literal with canonical String payload. Without the annotation, `"10"` is String, not Integer. |
| `{cel: input.taskId}` | A CEL expression whose result is scalar or a declared bounded list and matches the expected semantic type, where one is specified. |

A business string literal can be empty and contains at most 256 UTF-8 bytes. A CEL source expression contains at most 512 UTF-8 bytes. A nominal annotation gives a fixed string its business type. It does not grant access to the named resource or turn a runtime String into a nominal value. The compiler resolves the annotation only in the DCM's business type namespace. A host key type with the same spelling is separate metadata.

Outside the two numeric built-ins in their profile, an unknown or non-nominal annotation reports `NOMINAL_MISMATCH` at `/type` relative to the binding. A valid nominal annotation incompatible with the expected type reports it at `/literal`. A protected argument override reports `HOST_BINDING_OVERRIDE`, regardless of annotation.

## Types and CEL bridge

The [model type system](domain-model.md#types) defines scalars, bounded scalar lists and records. Expressions can select fields from records and produce scalar or declared list values, but cannot construct records or lists. Each record field carries its declared semantic type into the expression. Lists are host-supplied ordered arrays with at most their declared zero-to-16 item cap; duplicates are allowed. Select or pass a list unchanged, or test exact membership with `value in list`. The member must have the element's exact semantic type; a direct enum member literal is also accepted for an enum list. No indexing, list equality, nested lists or list transformations are supported.

The [descriptor bridge](compiler-artifact.md#descriptortype-bridge) maps Boolean to protobuf `bool`, String/nominal/enum values to `string`, lists to repeated scalar fields, and Timestamp to `google.protobuf.Timestamp`. Numeric-profile Integer uses `int64`; Decimal is an opaque `keir.numeric.Decimal` message, logically scalar. Values are validated before construction. Omitted optional scalars stay absent; `null` is invalid, while zero, `false` and empty String are present. Lists are always required values.

Allowed expression syntax includes Boolean/string constants, identifiers, declared field selection, `==`/`!=` on Boolean/String/nominal/enum values, `!`, `&&`, `||`, `?:` with compatible scalar branches, and `+` on two plain `String` operands. Keir 2 also admits scalar list membership, direct optional-field `has`, literal Timestamp/Duration constructors, Timestamp plus Duration, Timestamp `==`, and strict Timestamp ordering. Timestamp `!=` is unsupported; negate equality instead. Only the numeric profile adds the closed numeric operations below. No record/list equality, indexing, general constructors/conversions, comprehensions or arbitrary calls are supported. Inspect checked AST and resolved references/overloads, not source spelling alone. CEL type checking must succeed without `dyn`; engine options grant no extra language features.

Use `has(record.field)` only on a direct selection of a declared optional field. Reading it requires a presence proof in the same expression: in the true arm of `has(x.f) ? ... : ...`, the right operand of `has(x.f) && ...`, or the false arm of `!has(x.f) ? ... : ...`. A separate Boolean binding does not carry that proof to a later expression. An unproven read reports `DATAFLOW_UNAVAILABLE`.

Timestamp wire values use exact UTC `YYYY-MM-DDTHH:mm:ss.SSSZ`, Gregorian years 0001 through 9999, valid calendar dates and seconds 00 through 59. `timestamp("2026-01-01T00:00:00.000Z")` requires one literal String in that form. `duration("604800s")` creates an expression-only Duration: `0s` or unsigned whole seconds with no leading zero, up to `31536000s`. Duration is not a model value or a standalone binding result. Timestamp equality and `<` compare instants; Timestamp plus Duration preserves milliseconds. There is no implicit clock: the host projects any reference time into input. See [elapsed-time semantics](workflow-expressions.md#timestamps-and-elapsed-durations) for overflow and CEL error masking; an escaping overflow prevents dispatch and reports `BUDGET_EXCEEDED` without erasing earlier write evidence.

The checked pair `_+_`/`add_string` takes exactly two plain `String` operands and returns a plain `String`. Concatenation preserves Unicode exactly. A 64-node expression can concatenate at most 32 bounded string leaves, so a single intermediate string has at most 8,192 UTF-8 bytes. Each String expression result is still limited to 256 UTF-8 bytes when it leaves the evaluator. A larger title expression result fails before a create call is dispatched.

The [scalar qualification](../../conformance/cel-profile/README.md) observed these function/overload pairs: `_==_`/`equals`, `_!=_`/`not_equals`, `!_`/`logical_not`, `_&&_`/`logical_and`, `_||_`/`logical_or`, `_?_:_`/`conditional`, with arities 2, 2, 1, 2, 2, 3 respectively and no receiver target. Reject unknown or missing overload references. The generic equality overload still requires the scalar semantic checks below; its name does not admit CEL's other comparable types. Unary negation chains can simplify during parsing: count the expanded checked AST, not source operator characters. This expression checkpoint does not establish the complete Keir overlay or program compiler.

The compiler's semantic overlay retains nominal and enum types through selections and scalar bindings. Nominal equality, assignment and conditional branches require the same nominal type. An unannotated literal cannot supply a nominal value. Enum equality may compare values of the same enum or compare one to a declared member literal. Enum assignment accepts a value of that enum, a member literal, or a conditional whose branches satisfy that rule. Runtime validation checks value shape and membership using the compiled binding types.

Assignment preserves exact semantic types. There is no enum-to-String or nominal-to-String conversion. Literal refinement is local to the expression being checked against an enum. For example, `let: {literal: "DONE"}` binds a String. A later reference to that variable cannot satisfy an enum just because its initializer was a member. A `let` that reads an enum-valued field retains that enum.

## Numeric syntax and types

Only `keir/3` with DCM `/4` admits ordinary decimal Integer constants and unary minus, including signed int64 minimum. Hex, leading-zero integer tokens, uint suffixes, floating-point literals and exponent notation are denied. Typed numeric literals and `decimal("12.5")` require the [canonical wire spelling and bounds](domain-model.md#numeric-values). The Decimal constructor accepts one literal String, never a computed String. JSON/YAML numeric binding payloads such as `{literal: 10}` remain invalid.

Use same-type `==`, `!=`, `<`, `<=`, `>`, `>=`, `+`, `-`, `*`, exact `/` and unary `-`. `toDecimal(i)` converts Integer exactly. `toInteger(d)` requires a whole Decimal within int64 range. No implicit promotion, numeric/String conversion or nominal cast is allowed: `1 == decimal("1")` and `g ? 1 : decimal("1")` fail typing. Decimal is not a record authors may inspect or construct.

```yaml
- id: rate
  let: {literal: '0.075', type: Decimal}
- id: amount
  let: {cel: 'multiplyRounded(input.subtotal, decimal("1") - rate, 2, "HALF_EVEN")'}
```

Here the host declares `input.subtotal: Decimal`. The snippet is syntax guidance, not a complete compiled program. [Exact arithmetic and explicit rounding](workflow-expressions.md#numeric-arithmetic-and-rounding) define failures, six rounding modes and literal scale/mode restrictions. `size(input.tags)` counts an existing named bounded list and returns Integer; it does not count remote matches or admit guarded lists. Numeric optionals and guarded results use the same proofs, for example `g ? (has(r.amount) ? r.amount : decimal("0")) : decimal("0")`.

<a id="compiler-diagnostics"></a>
## Diagnostics and conformance

Compilation failure produces no artifact. See [compiler diagnostics](compiler-artifact.md) for the diagnostic format. The compiler checks document shape, trusted model identity and structural preflight before it checks CEL. Preflight checks guard references, capability and parameter names, required bindings, handler codes, output shape, literal compatibility and the conservative call and write counts.

Expressions are checked in step order. In a call, arguments come first in ASCII name order, then handlers in ASCII code order and their fields in ASCII name order. Root return fields come last in ASCII name order. Each expression completes parsing, typing and policy checks before the next. The compiler checks the aggregate AST budget after lowering, including all handler expressions. See [limits](runtime.md#limits) for per-expression and program ceilings.

For example, `when` pointing at a call or future step reports `DATAFLOW_UNAVAILABLE` at `/source/steps/<index>/when`. A guard pointing at an earlier non-Boolean `let` reports `TYPE_MISMATCH`. A second possible write reports `BUDGET_EXCEEDED`, and a write barred by the model's zero effect grant reports `ENTRY_GRANT_DENIED`. Malformed or ill-typed CEL reports `TYPE_MISMATCH`. A checked expression that uses forbidden CEL syntax reports `CEL_POLICY_DENIED`.

The [structural checker](../../scripts/check-keir-contracts.py) checks the available fixtures against their versioned schemas. The [compiler tests](../../compiler-java/src/test/java/dev/keir/compiler/GenericHookCompilerTest.java) cover nominal literals, handlers and String concatenation. The [Keir 2 expression examples](workflow-expressions.md#inventory-example) provide exact Core 0.2.0 model/artifact pairs and shared scenarios; they are not `/3` producer or qualification evidence.

## Example source

The complete [input-only decision source](../../conformance/keir-3/source-guard-input.json) reads a Boolean and returns ALLOW or DENY with a message. The [READ-based source](../../conformance/keir-3/source-guard.json) first calls a host capability, then combines its result with the input. Both use the exact [decision model](../../conformance/keir-3/dcm-guard.json). See the [walkthrough](../decision-walkthrough.md) for their roles and independent native checks. The [version matrix](compiler-artifact.md#source-and-artifact-identity) identifies their formats and supported compiler producers.

The development [guarded-result source](../../conformance/keir-3/source-guarded-result.json) and [model](../../conformance/keir-3/dcm-guarded-result.json) conditionally create a domain-neutral record and return its actual validated reference, or preserve the input reference when skipped. Optional fields use explicit presence tests. The [guarded READ decision](../../conformance/keir-3/source-guarded-result-read.json) uses the same rule with zero WRITE authority. [Shared scenarios](../../conformance/keir-3/README.md) distinguish success, skipping, rejection and uncertain effects; no failure becomes fallback success.

### Numeric programs

The development numeric programs below use the exact source `/3` + model `/4` pair. [Run 43413093006](https://github.com/rore/keir-core/actions/runs/43413093006) compiled each program once and passed 27 independent complete cases across Java and Node. This is source/runtime evidence, not standalone teaching-consumer or installed-package qualification. No new run command or package compatibility is implied.

| Program | Complete inputs | Expected behavior |
| --- | --- | --- |
| Stock shortage | [Model](../../examples/numeric/stock/dcm.json) · [Source](../../examples/numeric/stock/source.json) · [11 cases](../../examples/numeric/stock/cases.json) | With stock `"7"`, request quantity `"3"` to reach threshold 10 and return the adapter's accepted quantity under the exact guard. Cover skip, overflow, rejection and confirmed/unknown write evidence. |
| Quote decision | [Model](../../examples/numeric/quote/dcm.json) · [Source](../../examples/numeric/quote/source.json) · [16 cases](../../examples/numeric/quote/cases.json) | Subtotal `"100"` and discount rate `"0.075"` yield ALLOW and amount `"92.5"`. Cover optional zero/absence, HALF_EVEN ties, normalized carry and invalid input. |

These case files are independent program-test data, not scenario `/4` documents. Host capabilities and amount units are example contracts, not Core services or money enforcement. A quote ALLOW still requires native authorization and commit checks; an event result never undoes its original event. Standalone consumers and package evidence remain separate work.

<a id="source-and-example"></a>
<a id="implementation-reference-earlier-source-rules-and-example"></a>

[Original read-only source and example](../internal/history/compatibility-reference.md#original-read-source).
