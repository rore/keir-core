<a id="read-and-decide-dcm-contract"></a>
# Domain capability model

The domain capability model (DCM) is the host's contract for an extension. It describes which entrypoints the host exposes, the data they pass, and the operations an extension may call. The [`keir-dcm/1` schema](../../schemas/dcm-1/dcm.schema.json) checks decoded structure. The compiler and runtimes also check references, UTF-8 byte limits and the relationships between fields.

## Model fields and identity

Every top-level field below is required. Objects are closed, with unique keys and no extra fields. Optional fields described below must be omitted or valid. A null value is invalid.

| Field | Meaning |
| --- | --- |
| `format` | The literal `keir-dcm/1`. |
| `domain` | Exactly `{id, version}`. Both are nonempty strings. Together with the model's canonical digest they identify the snapshot used to compile and run an extension. |
| `types` | One to 16 named business types, in addition to built-in `String` and `Boolean`. |
| `capabilities` | Zero to eight named host operations. An empty catalog supports extensions that use only their input and local bindings. |
| `entrypoints` | One to four named hooks that can run an extension. |
| `hostKey` | Exactly `{source, type}`. The source is `requestKey`. The type is a nonempty label for the host's native key contract. |

The [source document](language.md#source-document) names the domain ID, version and canonical digest. The [compiled artifact](compiler-artifact.md) references the exact model, including its format and descriptor recipe. The digest covers the complete DCM, including unused types and operations. Changing a model field changes the digest. To adopt the new model, recompile and select the new artifact. Existing artifacts continue to require the model they reference.

The compiler accepts UTF-8 JSON or YAML model bytes. Use UTF-8 JSON without a byte-order mark when binding the model in either runtime. Raw and canonical DCM bytes each have a 65,536-byte ceiling. Nonempty metadata strings such as the domain ID, version and key type have a 256 UTF-8 byte limit. See [canonical bytes and digest rules](compiler-artifact.md#canonical-bytes-and-digest-rules) and [limits](runtime.md#limits).

## Types

| Type | Declaration and use |
| --- | --- |
| Boolean and String | Built-in scalars. A business String can be empty and has a 256 UTF-8 byte limit. |
| Nominal String | `{kind: "nominal", base: "String"}`. Distinguishes identifiers such as `TaskId` and `ProjectId` even though both are carried as strings. |
| Enum | `{kind: "enum", values: [...]}`. One to 16 unique nonempty string values. Each has a 256 UTF-8 byte limit. |
| Record | `{kind: "record", fields: {fieldName: TypeName, ...}}`. One to 16 required scalar fields, each referring to a built-in, nominal or enum type. |

Records are closed and cannot contain nested records. They are used for entrypoint input and output and capability results. Capability parameters are scalars. See [language types and expressions](language.md#types-and-cel-bridge) for assignment, equality and nominal literal rules.

## Identifiers

Type names, record field names and parameter names match `[A-Za-z][A-Za-z0-9]{0,63}`. Capability, entrypoint and consumer IDs match `[A-Za-z][A-Za-z0-9.]{0,99}`.

These identifiers exclude the reserved words `as`, `break`, `const`, `continue`, `else`, `false`, `for`, `function`, `if`, `import`, `in`, `let`, `loop`, `namespace`, `null`, `package`, `return`, `true`, `var`, `void` and `while`. Type names also exclude `Boolean`, `String`, `bool`, `bytes`, `double`, `dyn`, `int`, `list`, `map`, `null_type`, `string`, `type`, `uint` and `input`.

Names are case-sensitive and unique within their defining maps. A capability and an entrypoint can use the same ID, and different capabilities can reuse a parameter name. When an extension uses the same protected parameter name in multiple calls, the host supplies one value under that name. The runtime validates it against every required declaration, including those on guarded calls.

## Capabilities

Each capability is a reusable operation implemented by the host. The catalog describes the operations available for composition. A runtime binding supplies a matching implementation for each catalog entry.

| Required field | Meaning |
| --- | --- |
| `version` | Nonempty version string. The registered implementation must match it. |
| `parameters` | Zero to 16 named parameters. Each is exactly `{type, binding}` with a scalar type and a binding of `agent` or `host`. Source supplies `agent` parameters. The host supplies protected `host` parameters separately. |
| `result` | Name of a record type returned on success. |
| `effect` | `READ` or `WRITE`. The model determines which calls count toward the write ceiling. |
| `errors` | Zero to 16 unique declared business rejection codes matching `[A-Z][A-Z0-9_]{0,63}`. An extension may define [handlers](language.md#steps-and-data-flow) for those codes. |

The native key label is separate from the business type namespace, even when the labels happen to match. Each registered implementation declares the exact model key type. At invocation, the host supplies the original key and the runtime passes it in protected context. The host checks current permissions and key authenticity and implements the operation. See [host model binding](host-model-binding.md#api-shapes) for registration and invocation.

## Entrypoints

| Field | Meaning |
| --- | --- |
| `input`, `output` | Required record type names. The first defines the business data made available as `input`. The second defines the root return and any handler result. |
| `trigger`, `placement` | Required. The current contract uses `EVENT` and `AFTER_COMMIT`. The host supplies the event after the original operation commits. |
| `consumer` | Required. Exactly `{id, version, kind}` with a [consumer ID](#identifiers), a nonempty version string, and kind `EVENT_REPORT`. The consumer reports the outcome of the extension. |
| `event` | Required. Exactly `{idType, sourceType}`. Both names resolve to declared nominal String types. The host supplies the corresponding event values outside the source-visible input. |
| `capabilityPolicy`, `effectGrant` | Optional [call and write limits](#call-and-write-policy). |

An extension's result cannot veto or roll back the operation that fired the event. The host owns the relation between event provenance, projected business data, and returned native entities. For example, a task ID carried as input and a task ID carried as event source have the same nominal type in the sample model. The host decides whether they identify the same task for that hook.

## Call and write policy

The default is the published catalog, a ceiling of four total calls and one possible write. An entrypoint can narrow its operations with a `capabilityPolicy` object. Its only fields are optional:

| Field | Effect |
| --- | --- |
| `allow` | Retain only the listed operations. Omission retains the catalog. An empty list permits no operations. |
| `deny` | Remove the listed operations after applying `allow`. Denial wins when the same operation is in both lists. Omission or an empty list removes none. |
| `maxCalls` | Set a per-capability ceiling from zero to four. A quota cannot exceed the ceiling after applying `allow` and `deny`, so a denied operation can only have quota zero. Omitted IDs retain their ceiling. |

All IDs must exist in the catalog. Lists contain unique IDs. Each list or quota map has at most eight entries. An absent policy or `{}` leaves the catalog limits in place. Quotas count possible calls at compilation and preparation and reached calls during execution. They do not increase aggregate limits.

`effectGrant` is the number of allowed possible writes, either 0 or 1. It defaults to 1. An event hook with grant 0 can run read-only extensions. Every potential write counts, including one behind a false or mutually exclusive guard. A second possible write exceeds the hard ceiling. These restrictions come from the DCM and do not grant native permission.

## Complete example

The [TaskCompleted DCM](../../conformance/keir-1/dcm.json) is the complete model used by the shared follow-up example. `TaskCompletedInput` exposes only the completed task ID. `tasks.get` reads the task and `tasks.create` requests a follow-up. Both operations take a protected `actorId`. The customer selects the project and title in [the source](language.md#example-source), while the host supplies the actor, event and original request key. The model omits both policy fields, so the default limits apply.

The [structural checker](../../scripts/check-keir-contracts.py) checks the current fixtures against the schemas. The [Java](../../runtime-java/src/test/java/dev/keir/runtime/BoundEffectRuntimeTest.java) and [Node.js](../../runtime-node/test/bound-effect.test.js) tests check exact model binding, catalog registrations, and call and write ceilings.

<a id="legacy-read1-model"></a>
<a id="implementation-reference-original-read-only-model"></a>
<a id="closed-schema"></a>
<a id="sample-model"></a>

[Original read-only model and sample](../internal/history/compatibility-reference.md#original-read-model).
