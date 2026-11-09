<a id="read-and-decide-dcm-contract"></a>
# Domain capability model

The domain capability model (DCM) is the host's contract for an extension. It describes which entrypoints the host exposes, the data they pass, and the operations an extension may call. Current authoring targets `keir-dcm/3`, with the bounded values of [Keir 2](workflow-expressions.md) and the [pre-operation decision lifecycle](pre-operation-decisions.md). The compiler and runtimes check decoded structure, references, UTF-8 byte limits and the relationships between fields.

[Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0) implements the `/3` model contract. The [`keir-dcm/3` schema](../../schemas/dcm-3/dcm.schema.json) checks current structure. Use the [version rules](compiler-artifact.md#source-and-artifact-identity) to distinguish current authoring from retained execution of old approved artifacts.

## Model fields and identity

Every top-level field below is required. Objects are closed, with unique keys and no extra fields. Optional fields described below must be omitted or valid. A null value is invalid.

| Field | Meaning |
| --- | --- |
| `format` | The literal `keir-dcm/3` for current authoring. |
| `domain` | Exactly `{id, version}`. Both are nonempty strings. Together with the model's canonical digest they identify the snapshot used to compile and run an extension. |
| `types` | One to 16 named business types, in addition to built-in `String`, `Boolean` and `Timestamp`. |
| `capabilities` | Zero to eight named host operations. An empty catalog supports extensions that use only their input and local bindings. |
| `entrypoints` | One to four named hooks that can run an extension. |
| `hostKey` | Exactly `{source, type}`. The source is `requestKey`. The type is a nonempty label for the host's native key contract. |

The [source document](language.md#source-document) names the domain ID, version and canonical digest. The [compiled artifact](compiler-artifact.md) references the exact model, including its format and descriptor recipe. The digest covers the complete DCM, including unused types and operations. Changing a model field changes the digest. To adopt the new model, recompile and select the new artifact. Existing artifacts continue to require the model they reference.

The compiler accepts UTF-8 JSON or YAML model bytes. Use UTF-8 JSON without a byte-order mark when binding the model in either runtime. Raw and canonical DCM bytes each have a 65,536-byte ceiling. Nonempty metadata strings such as the domain ID, version and key type have a 256 UTF-8 byte limit. See [canonical bytes and digest rules](compiler-artifact.md#canonical-bytes-and-digest-rules) and [limits](runtime.md#limits).

For a new or changed model, use the [standalone fingerprint recipe](../model-fingerprint.md), copy the result into source `domain.fingerprint`, and compile against that exact model. The recipe implements canonical JSON hashing; it does not replace compiler validation.

### Binding diagnostics

These are the required `bindModel` results with matching host registrations and otherwise valid input.

| Model input | Result |
| --- | --- |
| Valid UTF-8 JSON | Binds successfully. |
| The same model prefixed with a UTF-8 byte-order mark | `ADMISSION_INVALID` at `/binding/dcm`. |
| One capability declares 16 distinct valid error codes | Binds successfully. |
| One capability declares 17 distinct valid error codes | `BUDGET_EXCEEDED` at `/binding/dcm`. |

## Types

| Type | Declaration and use |
| --- | --- |
| Boolean and String | Built-in scalars. A business String can be empty and has a 256 UTF-8 byte limit. |
| Nominal String | `{kind: "nominal", base: "String"}`. Distinguishes identifiers such as `TaskId` and `ProjectId` even though both are carried as strings. |
| Enum | `{kind: "enum", values: [...]}`. One to 16 unique nonempty string values. Each has a 256 UTF-8 byte limit. |
| Timestamp | Built-in scalar with exact UTC wire form `YYYY-MM-DDTHH:mm:ss.SSSZ`, valid Gregorian dates in years 0001 through 9999, and seconds 00 through 59. |
| Bounded list | `{kind: "list", element: TypeName, maxItems: N}`. `N` is an integer from 0 through 16. Elements are Boolean, String, nominal String or enum; no Timestamp, records or nested lists. Ordered JSON arrays preserve duplicates. |
| Record | `{kind: "record", fields: {fieldName: TypeName, ...}}`. One to 16 fields, each a scalar or named bounded list. An optional scalar field uses `{type: TypeName, optional: true}` instead of the type name. |

Records are closed and cannot contain nested records. They are used for entrypoint input and output and capability results. Capability parameters accept scalars or named bounded lists. Lists are required values; only scalar fields and agent parameters can be optional. Omission means absence, `null` is invalid, and supplied `false` or empty String values remain present. A complete business value is bounded to 8,192 bytes, in addition to each String's 256-byte ceiling and each list's declared cap. `Duration` is an expression-only type, not a model value. See [language types and expressions](language.md#types-and-cel-bridge) for presence proofs, assignment, membership, timestamps and nominal literal rules.

## Identifiers

Type names, record field names and parameter names match `[A-Za-z][A-Za-z0-9]{0,63}`. Capability, entrypoint and consumer IDs match `[A-Za-z][A-Za-z0-9.]{0,99}`.

These identifiers exclude the reserved words `as`, `break`, `const`, `continue`, `else`, `false`, `for`, `function`, `if`, `import`, `in`, `let`, `loop`, `namespace`, `null`, `package`, `return`, `true`, `var`, `void` and `while`. Type names also exclude `Boolean`, `String`, `Timestamp`, `Duration`, `bool`, `bytes`, `double`, `dyn`, `int`, `list`, `map`, `null_type`, `string`, `type`, `uint` and `input`.

Names are case-sensitive and unique within their defining maps. A capability and an entrypoint can use the same ID, and different capabilities can reuse a parameter name. When an extension uses the same protected parameter name in multiple calls, the host supplies one value under that name. The runtime validates it against every required declaration, including those on guarded calls.

## Capabilities

Each capability is a reusable operation implemented by the host. The catalog describes the operations available for composition. A runtime binding supplies a matching implementation for each catalog entry.

| Required field | Meaning |
| --- | --- |
| `version` | Nonempty version string. The registered implementation must match it. |
| `parameters` | Zero to 16 named parameters. Each is `{type, binding}` with a scalar or named bounded-list type and a binding of `agent` or `host`. An agent scalar parameter may also declare `optional: true`; no other flag value is valid. Source supplies agent parameters; the host supplies required protected host parameters separately. |
| `result` | Name of a record type returned on success. |
| `effect` | `READ` or `WRITE`. The model determines which calls count toward the write ceiling. |
| `errors` | Zero to 16 unique declared business rejection codes matching `[A-Z][A-Z0-9_]{0,63}`. An extension may define [handlers](language.md#steps-and-data-flow) for those codes. |

The native key label is separate from the business type namespace, even when the labels happen to match. Each registered implementation declares the exact model key type. At invocation, the host supplies the original key and the runtime passes it in protected context. The host checks current permissions and key authenticity and implements the operation. See [host model binding](host-model-binding.md#api-shapes) for registration and invocation.

## Entrypoints

| Field | Meaning |
| --- | --- |
| `input`, `output` | Required record type names. The first defines the business data made available as `input`. The second defines the root return and any handler result. |
| `trigger`, `placement` | Required. `EVENT`/`AFTER_COMMIT` reports after the native operation commits. `REQUEST`/`BEFORE_COMMIT` evaluates permission before a proposed native operation. |
| `consumer` | Required. Exactly `{id, version, kind}` with a [consumer ID](#identifiers) and nonempty version string. Kind is `EVENT_REPORT` for EVENT/AFTER_COMMIT, or `DECISION` for REQUEST/BEFORE_COMMIT. No mixed combinations are valid. |
| `event` | Required for both lifecycles. Exactly `{idType, sourceType}`, resolving to declared nominal String types. The host supplies corresponding `{id,source}` invocation values outside source-visible input. For REQUEST these identify the operation attempt and host-defined scope, not a committed event or an existing output entity. |
| `capabilityPolicy`, `effectGrant` | Optional [call and write limits](#call-and-write-policy). |

EVENT/AFTER_COMMIT/EVENT_REPORT semantics are unchanged: an extension's result cannot veto or roll back the operation that fired the event. The host owns the relation between event provenance, projected business data, and returned native entities. For example, a task ID carried as input and a task ID carried as event source have the same nominal type in the sample model. The host decides whether they identify the same task for that hook.

A DECISION output is a host-named closed record with required `decision` referring to a named enum with exactly two distinct values, `ALLOW` and `DENY`, in either order. Reordering still changes the model digest. Additional host-declared bounded scalar fields may be required or optional, such as `message:String`. There is no built-in business decision type. Only `decision` controls continuation; a message is plain data, not markup or native authority.

The host consumes only a fully validated COMPLETED decision with empty effects. DENY blocks the native attempt. ALLOW permits fresh native authorization, selection, consistency and deadline checks before the host commits through its existing path. Handled business rejection, failure, uncertainty, malformed output, expiry or cancellation block continuation. Verified absence of an active binding may follow the ordinary native path; a selected but unavailable or incompatible binding must block. See [decision consumption and host obligations](pre-operation-decisions.md#consumption-and-failure-policy) for the complete failure and consistency boundary.

## Call and write policy

The default capability selection is the published catalog. The hard ceilings are four total calls and one possible write; the lifecycle and `effectGrant` can narrow the write ceiling to zero. An entrypoint can narrow its operations with a `capabilityPolicy` object. Its only fields are optional:

| Field | Effect |
| --- | --- |
| `allow` | Retain only the listed operations. Omission retains the catalog. An empty list permits no operations. |
| `deny` | Remove the listed operations after applying `allow`. Denial wins when the same operation is in both lists. Omission or an empty list removes none. |
| `maxCalls` | Set a per-capability ceiling from zero to four. A quota cannot exceed the ceiling after applying `allow` and `deny`, so a denied operation can only have quota zero. Omitted IDs retain their ceiling. |

All IDs must exist in the catalog. Lists contain unique IDs. Each list or quota map has at most eight entries. An absent policy or `{}` leaves the catalog limits in place. Quotas count possible calls at compilation and preparation and reached calls during execution. They do not increase aggregate limits.

`effectGrant` is the number of allowed possible writes. EVENT defaults to 1 and accepts 0 or 1; an event hook with grant 0 can run read-only extensions. DECISION defaults to 0, accepts explicit 0 and rejects 1. Its lifecycle prohibits every potential WRITE, including one behind a false or mutually exclusive guard. Compiler and model binding validate the decision shape and grant; preparation independently rejects such a WRITE with `ENTRY_GRANT_DENIED` before dispatch, and dispatch retains the zero-write check. The catalog may still contain WRITEs for other entrypoints. A second possible write exceeds the global hard ceiling. These restrictions do not grant native permission or sandbox host adapter code.

## Complete example

The [current decision model](../../conformance/keir-3/dcm-guard.json) exposes `BeforeOperation`, typed input and an optional result message. The [walkthrough](../decision-walkthrough.md) explains its input-only and READ-based extensions and the host's native commit boundary.

For a post-commit example, the table below explains the task operations in the [readable follow-up model](../../examples/node-host/follow-up/dcm.json), migrated to `/3` alongside its [source](../../examples/node-host/follow-up/source.yaml). Use each checked-in source/artifact with its exact model; the [format matrix](compiler-artifact.md#source-and-artifact-identity) identifies compatible producer and runtime versions.

| Part of the model | What it does in the example |
| --- | --- |
| `entrypoints.TaskCompleted` | The app runs the extension after it saves a task completion. `TaskCompletedInput` gives the extension one field, `taskId`. The app supplies event provenance separately. |
| `capabilities["tasks.get"]` | Reads the completed task by `taskId` and returns its ID, project, and status. |
| `capabilities["tasks.create"]` | Accepts the project and title chosen by the extension. Returns the new task’s ID on success. |
| `actorId` on both operations | A protected parameter. The app supplies the authenticated actor and passes the original request key in host context. |
| `types.FollowupResult` | Reports `CREATED` or `NO_OP` when the extension completes. The runtime reports write evidence separately. |

`TaskId` and `ProjectId` are distinct types, so the extension cannot pass a task ID where a project ID is expected. The example uses the default call and write limits. Each contract has its own model pin.

The [structural checker](../../scripts/check-keir-contracts.py) checks available fixtures against their versioned schemas. The [Java](../../runtime-java/src/test/java/dev/keir/runtime/BoundEffectRuntimeTest.java) and [Node.js](../../runtime-node/test/bound-effect.test.js) tests cover exact model binding, catalog registrations, and call and write ceilings. The [Core 0.3.0 release](https://github.com/rore/keir-core/releases/tag/v0.3.0) supplies the qualified package assets.

<a id="legacy-read1-model"></a>
<a id="implementation-reference-original-read-only-model"></a>
<a id="closed-schema"></a>
<a id="sample-model"></a>

[Original read-only model and sample](../internal/history/compatibility-reference.md#original-read-model).
