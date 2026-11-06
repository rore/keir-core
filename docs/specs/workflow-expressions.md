# Keir 2 workflow expressions

This is the contract for the Core 0.2.0 candidate. Use the released Keir 1 references for Core 0.1.0. Java and Node.js must pass the same artifact and scenario checks before the candidate is released.

Keir 2 adds bounded list membership, optional values and elapsed-time expressions. The host still defines its events, input data and operations. The host binds a model, prepares the compiled artifact and invokes it after the triggering event commits.

## Bounded scalar lists

A model can declare a named list type such as `{"kind":"list","element":"Label","maxItems":8}`. A record field or capability parameter can refer to the type by name. Its elements are `String`, `Boolean`, a nominal String type, or an enum. The `maxItems` integer ranges from 0 through 16. Zero permits only an empty list.

The wire value is an ordered JSON array. Duplicates are allowed. `value in list` tests exact membership and requires the element's declared semantic type, including nominal or enum identity. For an enum list, a direct String literal naming a declared member can also be used, as in `"LOW" in input.priorities`. A String variable or computed String still has type `String`. Select a list supplied by the host or pass it through unchanged. The model rejects nested lists, records as elements, and values over the declared cap. Each String still has the 256-byte limit, and the complete value still has the 8,192-byte limit.

For example, if `input.labels` has element type `Label`, bind the fixed label with that same type before testing membership.

```yaml
- id: reviewLabel
  let: {literal: 'cold-storage', type: Label}
- id: needsReview
  let: {cel: 'reviewLabel in input.labels'}
```

## Optional fields and arguments

A required record field keeps its type reference, for example `"occurredAt": "Timestamp"`. Declare an optional scalar field as `"expiresAt": {"type":"Timestamp","optional":true}`. A capability parameter can add `"optional":true` to its usual `type` and `binding` declaration. The flag, when present, is exactly `true`. Lists and records are required values in this contract.

An omitted field or argument is absent. `null` is invalid. A supplied `false` or empty string is present. A source can omit an optional call argument or return field by leaving out its key. The compiler preserves the omission in the artifact's `args` or `fields` map. Protected parameters still come from the host.

Use `has(record.field)` on a direct selection of a declared optional field. A read of that field requires a presence proof in the same expression. The proof holds in the true arm of `has(x.f) ? ... : ...`, the right operand of `has(x.f) && ...`, and the false arm of `!has(x.f) ? ... : ...`. An unproven read reports `DATAFLOW_UNAVAILABLE`. A presence test in a separate step does not carry that proof into a later expression.

Runtimes validate raw input and adapter values before conversion. Records reject unknown fields. Timestamps must already have the exact wire spelling, and outgoing timestamps retain three fractional digits. Oversized values report `BUDGET_EXCEEDED`.

## Timestamps and elapsed durations

| Value | Contract |
| --- | --- |
| `Timestamp` | Built-in scalar. The wire value is a UTC string in exact `YYYY-MM-DDTHH:mm:ss.SSSZ` form. Gregorian years range from 0001 through 9999. Dates must exist on the calendar, with exactly three fractional digits and seconds from 00 through 59. |
| `timestamp("2026-01-01T00:00:00.000Z")` | Constructs a Timestamp from one literal String in the same format. |
| `duration("604800s")` | Constructs an expression-only Duration from one literal String. Use `0s` or unsigned decimal whole seconds without a leading zero followed by `s`. The maximum is `31536000s`, or 365 elapsed days. |

Timestamp equality (`==`) and strict ordering (`<`) compare instants. Swap operands for greater-than and negate a strict comparison for non-strict ordering. Add a Duration to a Timestamp with the Timestamp on the left. Addition preserves milliseconds. `604800s` is exactly 168 elapsed hours, including across daylight-saving changes. For time relative to an event, use a timestamp projected by the host in the input.

An inventory application can supply required `occurredAt` and optional `expiresAt` timestamps. This expression extends the existing expiry when present and otherwise uses the event time.

```text
has(input.expiresAt)
  ? input.expiresAt + duration("604800s")
  : input.occurredAt + duration("604800s")
```

Invalid constructor literals report `TYPE_MISMATCH` at compilation. Addition outside the supported Timestamp range produces a typed CEL error. Only an error that escapes the expression reports `BUDGET_EXCEEDED` at its binding and prevents its consuming call from dispatching. The outcome is `LIMIT_EXCEEDED` unless an earlier uncertain write requires `UNCERTAIN`. A prior confirmed write keeps its `CONFIRMED` receipt.

CEL Boolean semantics still apply. A pure arithmetic error can be masked by `false` in an `&&` expression or by `true` in an `||` expression, in either operand order. A conditional evaluates only its selected arm, and an error in its condition propagates. Input validation, deadlines, authorization and effect evidence remain mandatory outside expression evaluation.

## Inventory example

On an inventory arrival, the extension asks the host whether a matching item is already open. It checks the requested label, the lookup result, and the optional `enabled` flag before requesting one adjustment. The due time is 168 hours after the existing expiry when present, or after the arrival time otherwise. [Model](../../conformance/keir-2/dcm-inventory.json) · [Source](../../conformance/keir-2/source-inventory.json) · [Compiled artifact](../../conformance/keir-2/artifact-inventory.json)

| Scenario | Expected behavior |
| --- | --- |
| [Adjustment](../../conformance/keir-2/scenarios/inventory/inventory-adjusted.json) | The absent `enabled` flag uses the extension's default of `true`. The host confirms the adjustment, and the due time is `2026-03-14T12:34:56.789Z`. |
| [Present false](../../conformance/keir-2/scenarios/inventory/inventory-optional-false.json) | `enabled: false` skips the adjustment. The result still reports the due time computed from the existing expiry. |
| [Handled denial](../../conformance/keir-2/scenarios/inventory/inventory-handler-omits-due.json) | The host rejects the write with `ACCESS_DENIED` and `NONE` effect evidence. The handler returns `eligible: false` and omits `dueAt`. |
| [Timestamp overflow](../../conformance/keir-2/scenarios/inventory/inventory-selected-overflow.json) | The lookup runs, then timestamp addition fails with `BUDGET_EXCEEDED`. The extension stops before requesting the adjustment. |

## Execution boundary

An extension has at most eight steps, four possible calls and one possible write. Every possible write counts, including writes in mutually exclusive branches. A host may expose separate create and update operations, and each extension chooses at most one. Permissions, keys, the native mutation and its evidence remain the host's responsibility.

Each expression has at most 64 AST nodes, depth 16 and 512 source bytes. The artifact has at most 512 expression nodes in total. The ceiling is 16,384 bytes per checked expression, 65,536 bytes each for descriptors and the model, and 64 events per trace. A membership test checks at most 16 bounded scalar elements. These limits bound admitted work. Native callbacks and elapsed deadlines retain the existing host and runtime obligations.

## Checked expressions and descriptor identity

| Expression | Checked form |
| --- | --- |
| Scalar membership | `@in` with overload `in_list`. The list element is BOOL or STRING at the CEL layer and retains its exact DCM type in the semantic overlay. |
| Direct optional presence | A select with `testOnly=true`, BOOL result, and no call reference. |
| Timestamp and Duration constructors | `timestamp` / `string_to_timestamp` and `duration` / `string_to_duration`, each with one validated String literal. CEL well-known types are TIMESTAMP (2) and DURATION (3). |
| Timestamp addition | `_+_` / `add_timestamp_duration`, with Timestamp then Duration and a Timestamp result. |
| Timestamp comparisons | `_==_` / `equals` and `_<_` / `less_timestamp`. |

Preparation independently checks optional-field presence proofs and exact semantic types, as well as the complete checked expression and its type and reference maps.

The descriptor recipe `keir-record-descriptors/2` keeps `keir_read.proto`, `proto3`, package `keir.read`, ASCII-sorted records and fields, and consecutive field numbers starting at 1. Each field sets `json_name` to its exact model name. List fields are repeated BOOL or STRING fields. Other fields use the optional label. Optional Boolean and String-based scalars use `proto3_optional` and a synthetic oneof. Required Boolean and String-based scalars omit both `proto3_optional` and `oneof_index`. Oneofs follow field order with zero-based indexes. Each name starts with an underscore followed by the field name, with additional leading underscores until it is unique among field and oneof names. Timestamp fields use `.google.protobuf.Timestamp` and its natural presence. Requiredness is checked before conversion.

Include the standard `google/protobuf/timestamp.proto` descriptor only for models that use Timestamp. It precedes `keir_read.proto` and is named as a dependency. Its 255 bytes have SHA-256 `422a163a2d7051465e0a516584b4d96d18c270ec4e7ad3c21ad33e5c440ebecc`. Timestamp seconds use the standard epoch, with nanos in the range 0 through 999,999,999 and divisible by 1,000,000. Duration literals have nonnegative seconds and zero nanos.

Serialize descriptors deterministically. Validation rejects unknown fields and any additional imports, options, extensions or source information.

## Version matrix

| Component | Core 0.2.0 candidate |
| --- | --- |
| Source | [`keir/2`](../../schemas/source-2/source.schema.json) |
| Domain capability model | [`keir-dcm/2`](../../schemas/dcm-2/dcm.schema.json) |
| Compiled artifact | [`keir-artifact/2`](../../schemas/artifact-2/artifact.schema.json) |
| Verification scenario | [`keir-scenario/2`](../../schemas/scenario-2/scenario.schema.json) |
| Descriptor recipe | `keir-record-descriptors/2` |
| Artifact producer | `keir-compiler-java/0.2.0` |
| Core packages and authoring companion | Core `0.2.0`, companion edition `4`, pending qualification |

The artifact environment pins CEL Java `0.14.0`, CEL Node `0.6.1`, protobuf Java `4.35.1` and protobuf Node `2.6.2`. The matching CEL spec packages retain versions `0.14.0` and `0.6.1`. The `/2` scenario keeps the prior outer shape and lexical rules and uses the exact `/2` artifact and model pair. Time values in its business data are strings.

The runtime checks the full model reference, producer, environment and descriptor recipe. Package versions and document suffixes describe separate identities. The candidate compiler and runtimes use the `/2` contracts as a set. Use the matching Keir 1 packages for `/1` artifacts. When moving an extension to Keir 2, compile it against the new model and review the new artifact. Prior artifacts and approvals remain tied to their original bytes.
