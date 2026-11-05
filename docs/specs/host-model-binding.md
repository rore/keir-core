<a id="binding-a-host-model-to-capability-invokers"></a>
# Host model binding

<a id="corrected-host-bound-4-binding"></a>
<a id="effect4-binding-and-preparation"></a>
<a id="binding-a-host-dcm-and-preparing-keir-ir"></a>
## Bind the DCM and prepare a compiled rule

For the current single-write contract, initialize with `bindModel(dcmBytes, implementations)`. Validate the complete catalog, including unused capabilities, matching versions and key types, callable implementations, and homogeneous real or simulation mode. An empty capability catalog and empty implementation map are valid and mode-neutral. The DCM still has its required domain and entrypoint definitions. Binding invokes no implementation and performs no live authorization or key authentication.

Prepare with `prepareProgram(artifactBytes, boundModel)`. The runtime derives model identity from the DCM, checks the artifact format, exact model compatibility and supported compiler environment, and enforces program and resource limits. The host selects approved bytes and verifies stored-byte integrity before calling this API. Invoke with fresh trusted bindings, deadline, original non-null request key and typed event. Stable host implementations enforce current permission, key authenticity and native commit conditions. The [API shapes](#api-shapes) specify the complete inputs for these operations.

Preparation can be reused with distinct fresh invocation contexts. Event data is snapshotted and the original opaque request key is passed unchanged. Node snapshots the deadline's expiry and captures the `nowMs` function with its source receiver in a wrapper; it does not freeze or mutate the source clock object. Java creates a `Deadline` with the same `LongSupplier` and expiry. Neither runtime resets the deadline. Java uses distinct adapter and invocation types with `invokeBoundEffectProgram`.

<a id="exact-corrected-api-shapes"></a>
### API shapes

Node registration values are `{version, mode, keyType, invoke}` with mode `simulation` or `real`. The invoke function receives `(args, protectedArgs, remainingMs, context)`, where context is `{requestKey, event: {id, source}, deadline: {clock: {nowMs}, expiresAtMs}}`. The only invocation shape is:

```javascript
bindModel(dcmBytes, implementations) // {boundModel, diagnostic}
prepareProgram(artifactBytes, boundModel) // {prepared, diagnostic}
invokeProgram(prepared, businessInput, {bindings, deadline, requestKey, event}) // Promise<Outcome>
```

Java uses the following nested `KeirRuntime` types and distinct invocation method. Every catalog capability, including READ entries, maps to a `BoundEffectAdapter`:

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

public static ModelBinding bindModel(byte[] dcmBytes, Map<String,BoundEffectAdapter> implementations)
public static Preparation prepareProgram(byte[] artifactBytes, BoundModel boundModel)
public static CompletionStage<Outcome> invokeBoundEffectProgram(Prepared prepared,
    Object businessInput, BoundEffectInvocation invocation)
```

`Deadline` reuses the existing `(LongSupplier clock, long expiresAtMs)` record. Java snapshots the event map and retains the original opaque key by reference.

The [runtime contract](runtime.md#effect4-execution) defines invocation and limits. [Write evidence](real-single-effect-admission.md#effect-outcomes-and-key-identity) defines receipts and late responses. The artifact contains the exact model reference using the current [wire identifiers](../internal/implementation-reference.md#serialized-identifiers-and-compiler-selectors) and [reference fields and descriptor recipe](#exact-reference-artifact). Exact model versions may coexist. Existing prepared handles retain their binding. Host code owns implementation thread safety, storage, transport and lifecycle.

Binding snapshots model and registration metadata and callable references. Missing or extra implementations, wrong version/key type, non-callable implementations and mixed modes reject the complete binding before any handle is returned. Changing a callable requires a new binding. Binding failures use `ADMISSION_INVALID` under `/binding`; byte-limit failures use `BUDGET_EXCEEDED`. Preparation validates canonical artifact bytes, exact model reference, supported compiler/environment, checked expressions and the complete plan before returning a handle. Neither operation calls a capability implementation.

## Exact reference artifact

The artifact's `modelRef` is exactly `{domainId, domainVersion, format, digest, descriptorRecipe}`. The domain fields and format match the bound DCM. The digest covers the complete [canonical DCM](compiler-artifact.md#canonical-bytes-and-digest-rules). The descriptor recipe identifies the [record descriptor rules](compiler-artifact.md#descriptortype-bridge). Reject unsupported recipes and any mismatch. See [exact values](../internal/implementation-reference.md#serialized-identifiers-and-compiler-selectors) for the format and recipe identifiers.

<a id="legacy-read1-effect1-read2-effect2-and-effect3"></a>
<a id="implementation-reference-earlier-binding-contracts"></a>
<a id="one-binding-boundary"></a>
<a id="invocation-and-remote-dispatch"></a>
<a id="coexistence-and-lifecycle"></a>
<a id="equivalent-host-sketches"></a>
<a id="conformance-obligations"></a>

[Earlier binding and invocation contracts](../internal/compatibility-reference.md#earlier-binding).
