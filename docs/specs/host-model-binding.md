<a id="binding-a-host-model-to-capability-invokers"></a>
# Host model binding

Bind a trusted [DCM](domain-model.md) and its host implementations, prepare an approved [artifact](compiler-artifact.md), and reuse the prepared handle with fresh invocation context. Event extensions and pre-operation decisions use the same APIs. The [contract matrix](compiler-artifact.md#source-and-artifact-identity) identifies supported exact pairs.

Use the [profile matrix](compiler-artifact.md#source-and-artifact-identity) when selecting packages and host definitions. Package adoption does not relabel or approve an existing artifact.

<a id="corrected-host-bound-4-binding"></a>
<a id="effect4-binding-and-preparation"></a>
<a id="binding-a-host-dcm-and-preparing-keir-ir"></a>
## Bind the DCM and prepare a compiled rule

### Bind the model

Supply the DCM as UTF-8 JSON bytes. Formatted JSON is accepted. Binding derives the model identity from the canonical form, so the input bytes do not have to be canonical. If you compile against a YAML model, bind its [equivalent JSON](compiler-artifact.md#input-encodings).

The runtime validates the DCM, derives the type descriptors and checks the complete implementation map. The map has exactly one callable registration per published capability ID, including unused capabilities. Each registration's `version` matches that capability and its `keyType` matches `hostKey.type` in the DCM. Every registration has the same mode, either `real` or `simulation`. An empty catalog with an empty implementation map is valid and mode-neutral.

Binding snapshots the model and registration metadata and captures callable references. It makes no capability calls and performs no live authorization or key authentication. Replacing a callable requires a new binding. Existing prepared handles retain their original binding. Bindings for different exact models or modes can coexist.

### Prepare an artifact

The host selects and verifies approved artifact bytes before calling `prepareProgram`. The runtime checks canonical encoding, exact model compatibility, the compiler and package identities, the complete plan, checked expressions, and [limits](runtime.md#limits). It validates guard and handler references, types, definite availability and static call and write counts. A write prohibited by the entrypoint's zero allowance fails with `ENTRY_GRANT_DENIED` at `/artifact/steps/<index>`. A second possible write exceeds the hard limit and fails with `BUDGET_EXCEEDED`, including when its guard is false. No implementation runs during preparation.

The current API takes no extra admission record. The host verifies stored-byte integrity and chooses which artifact to enable. The runtime independently checks the artifact against the model supplied at binding.

<a id="exact-corrected-api-shapes"></a>
### API shapes

**Node.js.** Import the functions from `@keir/runtime-node`. Both byte inputs are `Uint8Array` values. Registration values are `{version,mode,keyType,invoke}`. The callback receives `(args, protectedArgs, remainingMs, context)` and may return the [adapter response](runtime.md#adapter-responses) directly or through a promise. Its context is `{requestKey,event:{id,source},deadline:{clock:{nowMs},expiresAtMs}}`.

```javascript
bindModel(dcmBytes, implementations) // {boundModel, diagnostic}
prepareProgram(artifactBytes, boundModel) // {prepared, diagnostic}
invokeProgram(prepared, businessInput, {bindings, deadline, requestKey, event}) // Promise<Outcome>
```

**Java.** The runtime's static methods and nested types live in `dev.keir.runtime.KeirRuntime`. Every capability maps to a `BoundEffectAdapter`.

```java
@FunctionalInterface
public interface BoundEffectFunction {
    CompletionStage<?> invoke(Map<String,Object> args, Map<String,Object> protectedArgs,
        long remainingMs, BoundEffectContext context);
}
public record BoundEffectAdapter(String version, String mode, String keyType, BoundEffectFunction invoke) {}
public record BoundEffectContext(Object requestKey, Map<String,Object> event, Deadline deadline) {}
public record BoundEffectInvocation(Map<String,Object> bindings, Deadline deadline,
    Object requestKey, Map<String,Object> event) {}
public record Deadline(LongSupplier clock, long expiresAtMs) {}

public static ModelBinding bindModel(byte[] dcmBytes, Map<String,BoundEffectAdapter> implementations)
public static Preparation prepareProgram(byte[] artifactBytes, BoundModel boundModel)
public static CompletionStage<Outcome> invokeBoundEffectProgram(Prepared prepared,
    Object businessInput, BoundEffectInvocation invocation)
```

Java `ModelBinding` and `Preparation` expose `boundModel()` and `prepared()`, respectively, with `diagnostic()` and `succeeded()` on both. In either runtime, a successful result contains a handle and a null diagnostic. A failure contains a null handle and `{code,path,message}`. Binding rejects invalid input with `ADMISSION_INVALID` under `/binding`, or `BUDGET_EXCEEDED` for a size or resource limit. Preparation diagnoses invalid binding or artifact data, incompatible compiler and environment pins, policy violations and limits. Check diagnostics before invoking. Java's `Outcome` exposes the [runtime outcome](runtime.md#outcome) through `toMap()` and `toJson()` as well as accessors for its individual fields.

### Exact numeric text helpers

In the numeric profile, inputs, protected/agent arguments, adapter values and outcomes carry [Integer/Decimal as canonical strings](domain-model.md#numeric-values). Reject Java Long/BigDecimal and Node number/bigint instead of silently coercing them. Use explicit helpers when the host accepts ergonomic text:

```java
String quantity = KeirRuntime.normalizeInteger("100.00"); // "100"
String amount = KeirRuntime.normalizeDecimal("+00100.00"); // "100"
Map<String,Object> input = Map.of("subtotal", amount); // when the selected DCM declares Decimal
```

```javascript
import { normalizeInteger, normalizeDecimal } from '@keir/runtime-node';
const quantity = normalizeInteger('100.00'); // '100'
const input = { subtotal: normalizeDecimal('+00100.00') }; // { subtotal: '100' }
```

Java exposes public static `String normalizeInteger(String)` and `normalizeDecimal(String)` on `dev.keir.runtime.KeirRuntime`. Node exposes these two functions at the package root. They accept only exact text matching `[+-]?[0-9]+(\.[0-9]+)?`, with a 256-ASCII-character cap checked before parsing. Remove representational leading and fractional trailing zeros, normalize signed zero, then enforce int64 or normalized precision ≤38/scale ≤18. No rounding occurs: Integer accepts `"100.00"`, not `"100.01"`; Decimal accepts redundant fractional zeros beyond scale 18 only when removing them gives an exact in-range value.

Reject null/non-string values, whitespace, exponents, separators, non-ASCII digits, `.5`, `1.` and overflow. Java throws `IllegalArgumentException`; Node throws `TypeError`, without echoing raw input. These are host helpers, not CEL functions or permissive runtime admission. `"100.00"` still fails strict numeric input if passed without normalization. Helpers do not enforce business units, currencies, authorization or domain ranges.

## Invocation inputs and adapter context

| Input | Contract |
| --- | --- |
| `businessInput` | Exact record declared by the entrypoint's input type. The rule reads this as `input`. |
| `bindings` | Closed map of the protected parameter names required by the plan. See [Protected bindings](#protected-bindings). |
| `event` | Exact `{id,source}` object, typed by the entrypoint's `event.idType` and `event.sourceType`. The runtime snapshots the validated fields. |
| `requestKey` | Original non-null host key, treated as opaque by the runtime. |
| `deadline` | Absolute expiry and a clock in the same time base. See [Deadlines](runtime.md#deadlines). |

The current invocation accepts only these host context fields. The runtime passes separate immutable business and protected arguments to each implementation. It supplies the remaining milliseconds and the context holding the original key, copied event and deadline. Node captures the expiry and the original `nowMs` callable with its receiver in a wrapper. Java constructs a `Deadline` using the original clock supplier and expiry. Neither resets the deadline.

Numeric internal projections belong to one invocation. Reuse immutable bound/prepared handles, not caller values or mutable engine messages. Callbacks and outcomes receive canonical strings, never Decimal protobuf internals. Validate complete adapter records even when later expressions do not use every numeric field. Numeric errors do not change receipt settlement or native authority.

### Protected bindings

Supply exactly the union of the parameters marked `binding: host` across all calls in the prepared plan. The runtime checks them before any step runs, including parameters used only by guarded calls whose guard will be false. One value supplies a name shared by several calls and must satisfy every declared type for that name. Missing, extra or mistyped fields fail with `HOST_BINDING_INVALID` at `/hostBindings`.

For the [follow-up rule](../../conformance/keir-3/source.yaml), supply `{actorId: "user1"}` in Node or `Map.of("actorId", "user1")` in Java. For the [no-call rule](../../conformance/keir-3/source-no-call.yaml) against the same [DCM](../../conformance/keir-3/dcm.json), supply `{}` or `Map.of()`. The no-call plan still needs the other invocation fields and a complete implementation map at binding.

<a id="request-key"></a>
### Original request key and authority

The host supplies one immutable native key. Every reached READ or WRITE implementation receives that exact original value or object, separately from business and protected arguments. The runtime never substitutes a source value, hashes, stringifies or appends a step suffix. The host owns persistent serialization and rehydration. A rehydrated key must represent the same original native identity.

The host implementation checks current permissions, key authenticity and event provenance before performing an operation. The host also owns its domain invariants, transport, storage and concurrency. The current runtime has no separate authorization callback or key validator. A plan that reaches no capability can complete without calling a native authorization or key-authentication check, while invocation and resource validation still apply. Keep caller identity, keys and authorization decisions out of shared prepared handles.

Implementations can call local code or remote services. The host owns service routing, credentials, identity propagation and transport. Pass the remaining budget without restarting it, and check the absolute deadline before native commit. An implementation denial counts as an attempted call. It establishes a definite no-effect outcome only when the implementation supplies the evidence defined in [Write evidence](runtime.md#write-evidence). Native reconciliation is a separate host observation. It cannot replace the original runtime receipt.

## Pre-operation invocation

A REQUEST/BEFORE_COMMIT/DECISION hook uses these same methods and context fields. Its `event.id` identifies an operation attempt and `event.source` its host-defined scope, without claiming a prior commit. Its plan has no possible writes; binding still requires registrations for the full catalog. No-call plans legitimately need no protected actor binding, so the host must authenticate and authorize native continuation independently.

The host distinguishes authoritative absence from a selected artifact that is unavailable or incompatible. Absence takes the ordinary native path; unavailable selection blocks. Only a valid completed ALLOW with empty effects is eligible to continue. Recheck approval, selection, permissions, proposed input and read facts at native commit. Reserve part of the enclosing deadline for those checks and commit, and discard late results after cancellation. See the [walkthrough](../quickstart.md#try-a-pre-operation-decision) and [complete decision obligations](pre-operation-decisions.md#host-invocation-and-consistency-obligations).

## Exact reference artifact

See [Model reference](compiler-artifact.md#model-reference) for the exact fields and identity. Preparation compares all of them against the bound DCM.

<a id="legacy-read1-effect1-read2-effect2-and-effect3"></a>
<a id="implementation-reference-earlier-binding-contracts"></a>
<a id="one-binding-boundary"></a>
<a id="invocation-and-remote-dispatch"></a>
<a id="coexistence-and-lifecycle"></a>
<a id="equivalent-host-sketches"></a>
<a id="conformance-obligations"></a>

[Earlier binding and invocation contracts](../internal/history/compatibility-reference.md#earlier-binding).
