# Read-and-decide language contract

The [generic-hook proposal](generic-hook-capability-contract.md) keeps source `keir/v0` but changes catalog/policy checks under explicitly selected `/2` profiles. It permits repeated and different published reads within aggregate budgets without named hook grants, and proposes explicit nominal literals for authored project constants without changing the host payload or adding configuration channels. The `/1` rules and example below remain implemented compatibility behavior.

Status: implemented experimental checkpoint, verified locally in Java and Node. Profile `keir-read/1` is selected by the trusted compiler configuration. It implements a strict subset of source `keir/v0`; unsupported baseline syntax is rejected. See [status](README.md), [DCM](domain-model.md) and [artifact](compiler-artifact.md).

## Source and example

This page defines `keir-read/1`. Implemented `when`/`onError` syntax and definite-availability restrictions belong only to the separately selected [single-effect profile](single-effect.md#source-and-definite-availability); they are not accepted additions to this read grammar.

Accept UTF-8 JSON or YAML containing only the JSON data model. Reject duplicate keys, unknown properties, aliases, anchors, merge keys, tags, multiple documents, malformed Unicode, null and numbers in bindings. No coercion. Mapping order is immaterial; step order matters. Enforce [limits](runtime.md#limits) before and during parsing.

For both source and DCM, YAML uses JSON-compatible scalar resolution, not YAML 1.1 implicit typing. Only lowercase plain `true`, `false`, `null` and the JSON number grammar `-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?` resolve implicitly to non-string values. Other nonempty plain scalars, including `yes`, `on`, `TRUE`, timestamps, `0x10`, `01`, `.inf` and `~`, remain strings. Quoted scalars remain strings. Empty/omitted YAML values are rejected rather than implicitly becoming null. Mapping keys must resolve to strings; reject non-string keys and duplicate decoded keys. Reject every explicit tag, anchor, alias and merge key, including otherwise standard explicit tags. This parsing rule does not admit numeric/null bindings: subsequent schema validation rejects them and accepts numbers only in the bounded integer metadata fields that explicitly require them. No rounding, loss of numeric precision or conversion to fit an expected field type is permitted; for example an unquoted numeric `domain.version` is rejected, not stringified.

Root fields, all required and no others: `language`, `domain`, `entrypoint`, `name`, `steps`, `return`. `language` is `keir/v0`. `domain` contains exactly `id`, `version`, `fingerprint`; these match the trusted DCM and its canonical digest. `name` matches `[a-z][a-z0-9-]{0,63}`. `entrypoint` and capability IDs match `[A-Za-z][A-Za-z0-9.]{0,99}`. Step IDs and record/type/field names match `[A-Za-z][A-Za-z0-9]{0,63}`. Step IDs are unique and cannot be `input`, a type name, or CEL reserved identifier.

Each step is exactly `{id, call, args}` or `{id, let}`. `args` is a map of declared agent-bound parameter names to bindings. A binding is exactly `{literal: boolean|string}` or `{cel: string}`. `return` is exactly `{record: {field: binding, ...}}`, with precisely the entrypoint output fields. `steps` is nonempty. No `when`, `onError`, writes, collections, optional fields, time, general branching, retries or executable source.

Example source for the exact [sample model](domain-model.md#sample-model). Its fingerprint was computed from that model's canonical JSON; it authenticates neither the host nor the model's guarantees.

```yaml
language: keir/v0
domain: {id: sample.tasks, version: '1', fingerprint: 'sha256:e43a2657dadad2fdd4826c42b4e5048c7d7f046e61f48043777f5945e9a9a58f'}
entrypoint: ClassifyTask
name: classify-completed-task
steps:
  - id: source
    call: tasks.get
    args:
      taskId: {cel: input.taskId}
  - id: completed
    let: {cel: 'source.status == "COMPLETED"'}
return:
  record:
    classification: {cel: 'completed ? "DONE" : "PENDING"'}
```

Calls execute in source order. Success binds a required result record; a failure terminates, so later expressions never see a missing call result. `let` binds an immutable scalar. Return runs after all steps succeed. The host displays the classification; it does not treat it as permission for a native write.

## Types and CEL bridge

Supported semantic types: Boolean, bounded String, string-based nominal IDs, string-backed closed enums, and closed records of those scalar types. Records are only input/capability results/root output; no nested records. No Int64, floating point, bytes, null, optional, list, map, unit or temporal types in this profile. Reject unsupported types even if unused in the submitted DCM. This explicit narrowing avoids silently converting numbers through JavaScript `number`.

Generate protobuf record descriptors from the approved DCM as specified in [artifact](compiler-artifact.md). Boolean maps to protobuf `bool`; String, nominal and enum values map to protobuf `string`. Validate required fields, enum membership and bounds **before** protobuf construction, so missing fields cannot become protobuf defaults. A fresh CEL environment for each expression contains only `input` and earlier step IDs, with record message types or inferred scalar types. No host context is declared.

Allowed expression syntax: Boolean/string constants, identifiers, required field selection, scalar `==`/`!=`, `!`, `&&`, `||`, and `?:` with scalar branches. No record equality, indexing, constructors, conversions, macros, comprehensions or other calls. Inspect checked AST and resolved references/overloads, not source spelling alone. A select must resolve to a declared record field; presence-test selects are unsupported. CEL type checking must succeed without `dyn`; only standard implementations of these operations in the pinned environment are available. Exact observed overload identifiers are part of the qualification evidence, not inferred from function spelling.

The [scalar qualification](../../conformance/cel-profile/README.md) observed these function/overload pairs: `_==_`/`equals`, `_!=_`/`not_equals`, `!_`/`logical_not`, `_&&_`/`logical_and`, `_||_`/`logical_or`, `_?_:_`/`conditional`, with arities 2, 2, 1, 2, 2, 3 respectively and no receiver target. Reject unknown or missing overload references. The generic equality overload still requires the scalar semantic checks below; its name does not admit CEL's other comparable types. Unary negation chains can simplify during parsing: count the expanded checked AST, not source operator characters. This expression checkpoint does not establish the complete Keir overlay or program compiler.

The compiler's semantic overlay retains nominal/enum provenance through selections and scalar bindings. Nominal equality/assignment requires the same nominal type; no raw string literal can create a nominal value. Enum equality may compare the same enum or one of its declared string literals. Enum assignment accepts a member literal, that enum's value, or a conditional whose branches satisfy that rule. Reject arbitrary strings/other enums. Conditional nominal branches must have the same nominal type. Plain strings do not inherit identity or enum authority. Runtime validation checks value shape/membership; it cannot distinguish two wire-identical nominal strings without the compiled binding types.

Assignment preserves exact semantic types: there is no enum-to-String or nominal-to-String widening in this profile. Reject enum/String assignment mismatches with `ENUM_MISMATCH`; do not insert conversions. Literal refinement is local to the expression being checked against an enum: `let raw = "DONE"` binds String, so a later reference to `raw` cannot satisfy Classification merely because its initializer was a member literal. An enum-valued selection bound by `let` retains that enum and may satisfy the same enum. A conditional assigned to an enum must prove each branch is that enum or a direct member literal under the preceding rule; no constant propagation through String variables is required or admitted. The same restriction prevents treating a String variable as a member literal during enum equality checks.

## Diagnostics and conformance

Compilation failure produces no artifact. Diagnostics are ordered by source path then code, with `{code,path,message}` only. Paths are RFC-6901-style escaped JSON pointers into parsed source or DCM, prefixed `/source` or `/dcm`; whole-document errors use those roots. Messages are fixed safe explanations, never input values, source snippets, secrets or exception stacks.

Codes: `SYNTAX_INVALID`, `FIELD_UNKNOWN`, `PROFILE_UNSUPPORTED`, `PIN_MISMATCH`, `DCM_INVALID`, `CAPABILITY_UNKNOWN`, `ENTRY_GRANT_DENIED`, `HOST_BINDING_OVERRIDE`, `TYPE_MISMATCH`, `NOMINAL_MISMATCH`, `ENUM_MISMATCH`, `DATAFLOW_UNAVAILABLE`, `CEL_POLICY_DENIED`, `BUDGET_EXCEEDED`. Perform whole-document parse/shape, DCM/pins and structural/authority preflight before expression compilation. Preflight includes declared calls/grants, protected arguments, required bindings and literal compatibility. Then validate expressions in source order, visiting call arguments and return fields in ASCII name order; each expression completes parsing, typing, semantic overlay and profile admission before the next. A later expression's inferred type error does not outrank an earlier expression's failure. Check aggregate budgets after lowering. A single deterministic primary diagnostic is sufficient; order multiple diagnostics by source path then code. Unknown step references use `DATAFLOW_UNAVAILABLE`; malformed/ill-typed CEL uses `TYPE_MISMATCH`, and checked but forbidden AST uses `CEL_POLICY_DENIED`.

Examples: source actor override at `/source/steps/0/args/actorId` -> `HOST_BINDING_OVERRIDE`; unsupported `when` -> `PROFILE_UNSUPPORTED`; unknown arbitrary field -> `FIELD_UNKNOWN`; TaskId passed to UserId -> `NOMINAL_MISMATCH`; enum comparison with `"CLOSED"` when undeclared -> `ENUM_MISMATCH`. The shared [acceptance matrix](host-integration.md#acceptance-fixtures) is authoritative for cross-component tests.
