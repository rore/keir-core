# Generic runtime alignment: implementation freeze

Status: technical freeze for [the alignment task](../../../.agent-workflow/tasks/align-generic-hook-runtime.md), pending independent plan review. No implementation or passing `/2` execution is claimed. This appendix makes the [generic-hook](../../specs/generic-hook-capability-contract.md), [bound-model](../../specs/host-model-binding.md) and [worked-example](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/specs/generic-hook-example.md) targets concrete without changing their product semantics.

## Profiles and compatibility

Dispatch explicitly among read/1, effect/1, read/2 and effect/2. Never infer effect from optional `effectGrant` or `keyBinding` fields. Keep every `/1` public method/type/constructor, emitted compiler identity, diagnostic behavior and golden byte unchanged. Add no overload that makes an existing null call ambiguous.

The compiler adds `Environment.read2()` and `Environment.effect2()`. CLI keeps its two-argument read/1 default and `--profile=keir-effect/1`; add exact `--profile=keir-read/2` and `--profile=keir-effect/2`. Use the existing qualified libraries and `keir-compiler-java/0.1.0-SNAPSHOT` identity for this unreleased increment; profile/format distinguish new semantics. No dependency upgrades.

## Callable APIs

All Java additions are nested types/static methods of `dev.keir.runtime.KeirRuntime`:

```java
public static final class BoundModel { /* private construction; no setters */ }
public record ModelBinding(BoundModel boundModel, Diagnostic diagnostic) {
    public boolean succeeded() { return boundModel != null; }
}
public static ModelBinding bindModel(byte[] dcmBytes,
    Map<String,Object> approval, Map<String,?> invokers);
public static Preparation prepareProgram(byte[] artifactBytes,
    BoundModel boundModel, Map<String,Object> admission);
public record Invocation2(String scope, Map<String,Object> bindings,
    Deadline deadline, Authorizer authorize,
    Optional<Map<String,Object>> capabilityPolicy) {}
public record EffectInvocation2(String scope, Map<String,Object> bindings,
    Deadline deadline, Authorizer authorize,
    Optional<Map<String,Object>> capabilityPolicy, OptionalLong effectGrant,
    Object requestKey, Map<String,Object> event) {}
public static CompletionStage<Outcome> invokeProgram(
    Prepared prepared, Object businessInput, Invocation2 invocation);
public static CompletionStage<Outcome> invokeEffectProgram(
    Prepared prepared, Object businessInput, EffectInvocation2 invocation);
```

Reuse `Diagnostic`, `Preparation`, `Prepared`, `Outcome`, `Adapter`, `EffectAdapter`, `KeyBinding`, `Deadline`, `Authorizer` and `HostContext`. One binding method accepts `Map<String,?>`: read/2 requires Adapter entries, effect/2 requires EffectAdapter entries. Java approval uses the existing typed KeyBinding, not a map. ModelBinding and Preparation contain exactly one non-null component. BoundModel/Prepared have no public construction or mutation.

`Optional.empty()`/`OptionalLong.empty()` express omission; null Optional objects are invalid. Policy-map absent fields mean omission; present null is invalid. Snapshot at API entry before asynchronous work. Invocation validation must return the specified failure outcome, not throw from a record constructor. Wrong-profile or legacy Prepared passed to a new invocation method fails with HOST_CONTEXT_INVALID at the empty pointer. Legacy invocation methods reject `/2` handles rather than executing `/1` semantics.

Node adds these exports:

```text
bindModel(dcmBytes, approval, invokers)
  -> {boundModel: handle, diagnostic: null}
   | {boundModel: null, diagnostic: diagnostic}
prepareProgram(artifactBytes, boundModel, admission)
  -> {prepared: handle, diagnostic: null}
   | {prepared: null, diagnostic: diagnostic}
invokeProgram(prepared, businessInput, trustedInvocation) -> Promise<Outcome>
```

Inputs are Uint8Array bytes. Result wrappers are frozen closed two-field records; handles are opaque and privately branded with WeakMaps. Reject lookalikes. The prepared validated profile alone selects read/effect. Keep legacy `prepare` handle-or-diagnostic and `invoke` behavior for `/1`; reject `/2` at those entrypoints. Expected invalid input produces diagnostics/outcomes, never thrown exceptions or rejected promises. Use existing redacted diagnostic messages. Java's two new invocation names implement the same operation as Node's one new invocation export.

## Closed host records

| Record | Required | Optional / restrictions |
| --- | --- | --- |
| Approval | scope, modelRef, profile, environment, mode | Effect requires keyBinding; read forbids it. Node keyBinding is exactly `{type,validate}`; Java uses typed KeyBinding. Snapshot the validator; do not execute it at bind. Read mode permits simulation/real; effect remains simulation-only. |
| Model reference | domainId, domainVersion, format, digest, descriptorRecipe | Exactly these five fields. Recipe is `keir-record-descriptors/1`; domain/model/profile/environment must match the reviewed contract. |
| Admission | expectedArtifactDigest, expectedDcmDigest, entrypoint, consumer, compiler, environment, mode | capabilityPolicy and limits; effect additionally permits effectGrant. Forbid grants, keyBinding, scope, invokers, profile and other unknown fields. ModelRef digest equals expectedDcmDigest; scope/profile/key binding come from BoundModel. |
| Read invocation | scope, bindings, deadline, authorize | capabilityPolicy |
| Effect invocation | read fields plus requestKey and event | capabilityPolicy and effectGrant |

Scope is a nonempty valid Unicode String of at most 256 UTF-8 bytes, with exact equality and no normalization. Consumer, limits, policy, event and deadline retain reviewed shapes: event exactly `{id,source}`; Node deadline `{clock:{nowMs:function},expiresAtMs}`, Java existing Deadline. Original opaque keys remain outside CEL and are forwarded unchanged. Omitted effectGrant inherits; present values are integer 0/1 and narrowing only.

The invoker map has own catalog-ID entries and exactly the complete catalog, including unused capabilities. Node read registrations are exactly `{version,mode,invoke}`; effect registrations add keyType. Java uses the corresponding records. All versions, modes and key types match the bound model. No registration callback runs. Neither callables nor scope are serialized into artifacts/scenarios.

## Diagnostic precedence

Use one safe diagnostic with this fixed group order. Inside a group, missing/unknown fields precede field-value errors; then visit fields/IDs in ASCII lexical order. Preserve published policy and expression/step ordering. `/1` precedence is unchanged.

### Binding

1. Approval shape/profile/environment/mode/scope/key metadata: ADMISSION_INVALID at `/binding/approval`, or `/binding/approval/<field>` for a present invalid value. Unsupported recipe is `/binding/approval/modelRef/descriptorRecipe` and is checked here.
2. DCM input type: ADMISSION_INVALID at `/binding/dcm`; excessive raw bytes: BUDGET_EXCEEDED there. Strict parse/full schema errors use ADMISSION_INVALID there; raw/canonical size errors use BUDGET_EXCEEDED there. Validate unused definitions too.
3. Exact model reference mismatch: ADMISSION_INVALID at `/binding/approval/modelRef`.
4. Derive descriptors; overflow uses BUDGET_EXCEEDED at `/binding/dcm/types`.
5. Invoker shape/exact key set: ADMISSION_INVALID at `/binding/invokers`; entry incompatibility at `/binding/invokers/<escaped-id>`.

### Preparation

1. Authentic BoundModel: ADMISSION_INVALID at `/binding`.
2. Admission closed shape and syntactically valid expected artifact digest: ADMISSION_INVALID at `/admission` or `/admission/expectedArtifactDigest`.
3. Artifact input type: ARTIFACT_INVALID at `/artifact`; excessive raw bytes: BUDGET_EXCEEDED there.
4. Raw SHA-256 mismatch: PIN_MISMATCH at `/artifact`, before parsing.
5. Canonical envelope/version/profile/environment and exact model reference: ARTIFACT_INVALID at the offending envelope field; reference mismatch at `/artifact/modelRef`; forbidden embedded fields at `/artifact`.
6. Remaining external admission pins/values: ADMISSION_INVALID at `/admission/<field>`. Malformed/widened policy uses `/admission/capabilityPolicy`; a valid ceiling too narrow for the admitted program uses AUTHORITY_DENIED there or at `/admission/effectGrant`.
7. Validate the entire plan/checked-expression metadata and static admitted budgets. Retain reviewed aggregate-limit, hard-write, effect-permission and policy ordering for known calls.

### Invocation

Validate branded/profile-correct Prepared, then closed trusted context, scope, optional restrictions/key/event/deadline under existing preflight. Malformed/widened context uses HOST_CONTEXT_INVALID at the empty pointer. Keep existing input/binding/type/budget paths/order. False authorization, including revocation, uses AUTHORITY_DENIED; exception/nonboolean uses AUTHORITY_CHECK_FAILED. Deadline and response/timeout winner semantics remain unchanged. Never expose scope, credentials, keys or exceptions in diagnostics.

## Shared fixture contract

One owner controls `conformance/generic-hooks/`:

```text
read/   dcm.json dcm.sha256 source.yaml artifact.json artifact.sha256 scenarios.json
effect/ dcm.json dcm.sha256 source.yaml artifact.json artifact.sha256 scenarios.json
read/   source-repeated.yaml artifact-repeated.json artifact-repeated.sha256
        source-alternate.yaml artifact-alternate.json artifact-alternate.sha256
effect/ source-project2.yaml artifact-project2.json artifact-project2.sha256
        source-title2.yaml artifact-title2.json artifact-title2.sha256
README.md run-node.mjs node.test.mjs verify.mjs
```

Variants use their directory's unchanged DCM. Effect main is the exact 1,534-byte model with digest `sha256:68b72e4da41aa006a6a8182b5b8a6a4363a6734688da3c1e0c3807c0aff4ac04`. Project2 changes only the project literal; title2 changes only the title to `Follow up again`. `tasks.create` business args are exactly projectId/title, protected actor separate, never sourceTaskId. Read catalog publishes `tasks.get`, a second approved read and an unused WRITE definition; preserve REQUEST/DISPLAY rather than invent a combined DCM format.

Scenario JSON is a bounded array of closed records:

```text
{format,id,given:{input,fixture,program},stubs,expect:{outcome,calls,consumption}}
```

Format is `keir-scenario/read-2` or `keir-scenario/effect-2`, matching its directory. Program is a host enum: read `main|repeated|alternate`, effect `main|project2|title2`. Hosts hardcode file/pin mappings; never interpret customer input as a path/module/model selector. IDs are unique and match `[A-Za-z0-9][A-Za-z0-9_-]{0,63}`.

Stubs/expected calls have the closed shape `{step,capability,version,args,protectedArgs}`; a stub adds response in the existing READ/WRITE outcome shape. No executable payload or callable authority is serialized. Expected outcomes/consumption use existing profile semantics. Hosts independently verify native key/event context. Compiler emits artifact bytes, never expected outcomes; preserve exact result/trace/effect/call comparisons and fail harness mismatches independently of runtime exception handling.

Fixture IDs are `normal`, `invocation-denied`, `call-denied`, `zero-effect`, `wrong-scope`, `revoked-after-read`, `invalid-key`, `wrong-origin`, `read-throws`, `lost-response`, `response-expired`. Normal supplies scope, actor user1, logical clock 0/deadline 1000, current authorization true and, for effects, original key plus event `{id:event1,source:task1}`. Entry/call-denied reject the respective first authorization; zero-effect supplies fresh effectGrant 0; wrong-scope changes only scope; revoked-after-read denies the next dispatch; invalid-key/wrong-origin use host fixture checks; read-throws throws on the first READ; lost-response throws after WRITE dispatch; response-expired preserves settlement-before-final-clock behavior. Unknown fixture IDs reject before runtime. Keep malformed registration/policy mutations in component/shared-driver tests, not a scenario host-configuration DSL.

Shared fixtures normal/invocation-denied/call-denied/wrong-scope/read-throws apply to read and effect. The others are effect-only in this initial corpus. All underlying runtime paths still require component coverage where applicable; fixture selection does not narrow the runtime contract.

## Finite host-harness budgets

These are controlled example/qualification limits, not a new runtime capacity API, production estimates or decoded-heap containment claims:

| Resource | Per harness instance |
| --- | --- |
| Live BoundModels | 4, counting distinct handles even for the same digest |
| Raw and canonical DCM bytes | Each 65,536; aggregate canonical model bytes 262,144 |
| Derived descriptor bytes | Each 65,536; aggregate 262,144 |
| Live Prepared handles | 8; retained raw artifact bytes each 1,048,576, aggregate 8,388,608 |
| Bind/prepare work | One operation at a time, no queue |
| In-flight invocations | 2, no queue |
| Scenario file / row | File 1 MiB and 128 rows; serialized row 65,536 bytes; 4 stubs/captured calls |
| Subprocess batch | Existing 30,000 ms / 1 MiB output bound |
| Default logical invocation | 1,000 ms; controlled clocks, no sleeps/network |

Reject would-exceed work at the host boundary before binding/preparation/invocation; assert no callbacks/dispatch. Host rejection has test reason CAPACITY_EXCEEDED, not a new Keir diagnostic/outcome. Prepared/in-flight users pin their model slot. Release counters only after references and in-flight work finish; use lexical harness ownership, not a global manager or runtime close/revocation API. No cache eviction implementation is required; use bound-handle reuse and release/rebind cold-start tests. Fresh authorizers implement approval revocation.

## Ownership and delivery gates

- A owns compiler-java, runtime-java, Java host and all conformance files/drivers/verifiers/resources. Publish deterministic compiler/reference fixtures first. Update the existing combined verifier to invoke the bounded `/2` batch and check every packaged Node source file, not a stale hard-coded subset.
- B owns runtime-node and Node host/package metadata. Consume A's exact fixtures; request shared changes from A. Do not generate separate Node goldens.
- Manager owns Work Record, roadmap, docs, selected-commit integration and overall acceptance. Developers report evidence through the shared task record without concurrent canonical writes.

After independent plan review, A starts compiler/fixture delivery while B may implement binding/schema/API unit work against this freeze. Then both implement model preparation, strict per-program validation and execution using existing safety algorithms. Do not accept parity on hand-written pseudo-artifacts. Finish packaged hosts, focused tests, one integrated fast regression/coverage/shared verifier run and independent result review. Preserve `/1` fixtures/tests and no-compiler/parser Java host isolation. No new CI workflow or production/real-write claim is included.
