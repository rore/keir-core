# Binding a host model to capability invokers

Status: **implemented experimental `/2` contract, locally verified in Java and Node**. This implements the [shared-model direction](../internal/artifact-model-sharing-evaluation.md) and [generic-hook contract](generic-hook-capability-contract.md). See [testing evidence](../testing.md) and the runnable hosts. `/1` APIs and bytes are preserved; real transport/native guarantees and production qualification remain open.

## One binding boundary

The host binds a complete approved DCM and its invokers together, then reuses that opaque BoundModel. Preparation accepts no separate invoker map. A callable normally bridges to an internal HTTP/RPC service; it need not implement business logic locally.

Implemented API families (diagnostics must be checked before using handles):

```text
bindModel(dcmBytes, approval, invokers) -> {boundModel, diagnostic}
prepareProgram(artifactBytes, boundModel, admission) -> {prepared, diagnostic}
Node: invokeProgram(prepared, businessInput, trustedInvocation) -> Promise<Outcome>
Java read: invokeProgram(prepared, businessInput, Invocation2) -> CompletionStage<Outcome>
Java effect: invokeEffectProgram(prepared, businessInput, EffectInvocation2) -> CompletionStage<Outcome>
```

`approval` is a closed host-only record: `{scope, modelRef, profile, environment, mode, keyBinding?}`. Scope is an opaque host-selected nonempty String of at most 256 UTF-8 bytes, compared by exact equality; it is not an artifact field or credential. Effect bindings require the existing native `keyBinding`: its type must match DCM hostKey.type and its validator must be callable; snapshot but do not invoke the validator during binding. Read bindings omit it. Effect mode remains simulation-only. Approval comes from independently trusted configuration, never from customer-supplied self-description.

Registration is atomic. Bound raw bytes before parsing; validate canonical DCM size, schema and exact approved identity, derive descriptors, then validate **all** invokers. Require exactly one callable per catalog capability ID, including unused or policy-denied operations. Missing, extra, duplicate, non-callable, wrong-version, wrong-mode or incompatible key-type entries reject the entire binding. READ/WRITE classification comes only from the DCM. Read-profile catalogs containing WRITE definitions still require their bindings but cannot dispatch them. Registration is not a per-extension allowlist; optional policy and fresh authorization restrict dispatch separately.

Preserve registration metadata shapes: read `{version,mode,invoke}`, effect `{version,mode,keyType,invoke}` for every capability. Copy model bytes and metadata, snapshot callable references and expose no mutable map. Registration calls neither invokers nor authorizers. Host code owns callable behavior and thread safety; snapshots cannot freeze captured clients. Changing a callable requires a new binding, not map mutation.

## Exact reference artifact

For `keir-artifact/read-2` and `keir-artifact/effect-2`, replace `/1` envelope fields `dcm`, `dcmDigest`, `descriptors` with exactly `modelRef`:

```text
{domainId, domainVersion, format, digest, descriptorRecipe}
```

`domainId`/`domainVersion` equal DCM `domain.id`/`domain.version`; `format` is the corresponding `keir-dcm/read-2` or `keir-dcm/effect-2`; `digest` hashes the complete canonical DCM using [existing canonicalization](compiler-artifact.md#canonical-bytes-and-digest-rules). `descriptorRecipe` is exactly `keir-record-descriptors/1`: the existing [descriptor bridge](compiler-artifact.md#descriptortype-bridge), including every DCM record, sorted fields, scalar types, file/package names and no imports. Unknown recipes fail closed.

All remaining envelope fields retain their profile rules, including compiler, environment, sourceDigest, expressions, steps and return. No embedded fallback, descriptor override, endpoint or resolver is allowed. The compiler receives the full trusted DCM and uses this recipe when checking expressions. Source domain fingerprint still equals the canonical DCM digest. Artifact digest covers the complete new canonical envelope.

Preparation bounds and authenticates artifact bytes against the external expected artifact digest before parsing. Admission retains its independently selected model digest, compiler/environment, entrypoint, consumer and restrictions; mode must equal the binding's mode. Profile selection is pinned by binding approval. `keyBinding` moves to effect registration and cannot be overridden during preparation. Require exact equality between artifact modelRef and bound modelRef, and between artifact profile/environment and binding approval. Validate the entire plan and checked-expression metadata against that model; model approval never substitutes for program validation.

Cache pure model/type state only by full modelRef, profile and exact environment tuple `{celJava,celNode,protobufJava,protobufNode}`. Approval scope remains isolated; any cross-scope deduplication shares only inert validated state, never invokers, clients or permission. Same-named records in different models must not collide. Prepared pins its binding and expression plans. Neither handle caches invocation identity, protected values, keys, authorization results or deadlines.

## Invocation and remote dispatch

`trustedInvocation` retains `/2` bindings, deadline, authorize and optional narrowing policy/effect ceiling; add required `scope`, matching the binding approval scope. Effect invocations also supply the original native requestKey/event. The host selects this scope from authenticated context. It closes the authorizer over the selected binding and checks current approval/revocation plus caller authority on the existing `{entrypoint}` and `{entrypoint,capability,version}` requests. Scope equality alone grants no permission.

Keep callable signatures unchanged: read `(agentArgs, protectedArgs, remainingMs)`; effect adds fourth `{requestKey,event}`. Evaluated business arguments and trusted host bindings are separate validated snapshots. Identity needed by a service comes through host bindings or a host-controlled request context, never customer fields. No process-global “current user.” A reusable client may retain transport configuration/service credentials within its approved scope; per-user credentials and identity must remain request-local and must not leak through pooled mutable headers.

The remaining deadline is a duration in milliseconds, not a cross-process clock timestamp. The bridge budgets connection, serialization and remote work within it. Host-owned routing, discovery, authentication, serialization and client libraries must preserve the native key contract: forward the original immutable key, never stringify/hash/rederive it. A transport unable to preserve that contract is unsupported; this contract defines no key wire format or generic transport SDK.

Map service replies to existing typed outcomes and declared rejection codes, not arbitrary HTTP statuses. READ retains its result/failure rules. WRITE SUCCESS requires CONFIRMED; BUSINESS_REJECTION and TRANSIENT_FAILURE require definite NONE; possible commit without definitive evidence maps UNKNOWN_COMMIT/UNKNOWN. Transport failure or timeout after dispatch cannot establish NONE. Thrown/malformed responses retain existing failure and effect accounting. No automatic retries, including hidden client retries for WRITE.

Preserve the existing response/timeout winner: winning effect evidence settles before completion-clock/result validation, so invalid result data cannot erase CONFIRMED. Late responses cannot resume execution or rewrite its returned receipt. Timeout/cancellation does not prove remote cancellation. Keep aggregate budgets, fresh per-dispatch checks and the hard maximum of one potential WRITE, including guarded calls. Customer constants remain source-authored; binding adds no configuration input or default named grants.

## Coexistence and lifecycle

Different model versions/digests and different host bindings can coexist. Even additive catalog changes produce a new model reference; programs adopting that changed model require explicit recompilation/selection. Existing programs may keep using their exact earlier snapshot while its dependencies remain available and approved. Replacing invokers for the same model creates a new binding; existing Prepared objects retain the old one. To use the replacement, explicitly prepare/reselect again. Never silently route an old handle to “latest.”

Lifecycle is host-owned: deprecation may prohibit new preparation while allowing existing activations; revocation denies new invocation and subsequent dispatch through current authorizers. Already-dispatched work cannot be recalled; retain its honest receipt. Retirement releases dependencies only after hosts account for prepared/in-flight users. Missing exact models at cold start fail locally, without network resolution. Evicting unpinned pure cache entries changes neither approval nor revocation. Maintain existing individual byte/AST limits and finite host limits for model count/bytes, prepared handles, preparation concurrency and queues; fail before excess allocation.

Diagnostics are redacted and deterministic across runtimes. Binding schema/identity/registration errors use `ADMISSION_INVALID` under `/binding`; size excess uses `BUDGET_EXCEEDED`. Unsupported/mismatched artifact references use `ARTIFACT_INVALID` at `/artifact/modelRef`; conflicting admission pins use `ADMISSION_INVALID` under `/admission`. Wrong invocation scope uses `HOST_CONTEXT_INVALID` at the empty pointer; current revocation uses existing `AUTHORITY_DENIED`. No handle or dispatch follows preflight failure. Validate binding before preparation; within preparation preserve external-digest-before-parse ordering, then envelope/reference, admission and plan checks.

## Equivalent host sketches

Both sketches use the complete two-capability [walkthrough catalog](generic-hook-example.md). Helpers represent host-owned values/clients, not Keir utilities. Approval pins the modelRef, environment and simulation mode; admission pins the artifact and hook/consumer. `freshInvocation` supplies matching scope, current authorizer, bindings, deadline and original key/event. These sketches show the implemented wrappers; complete runnable integrations are in the [Java](../../examples/java-host/README.md) and [Node](../../examples/node-host/README.md) hosts. Real-write admission remains rejected.

```java
var binding = bindModel(approvedDcmBytes, approval, Map.of(
    "tasks.get", new EffectAdapter("1", "simulation", keyType,
        (args, trusted, ms, nativeContext) ->
            taskBridge.get(args, trusted, ms, nativeContext)),
    "tasks.create", new EffectAdapter("1", "simulation", keyType,
        (args, trusted, ms, nativeContext) ->
            taskBridge.create(args, trusted, ms, nativeContext))));
if (!binding.succeeded()) throw new IllegalArgumentException(binding.diagnostic().code());
var selection = prepareProgram(approvedArtifactBytes, binding.boundModel(), admission);
if (!selection.succeeded()) throw new IllegalArgumentException(selection.diagnostic().code());
var outcome = invokeEffectProgram(selection.prepared(), Map.of("taskId", taskId), freshInvocation);
```

```javascript
const binding = bindModel(approvedDcmBytes, approval, {
  "tasks.get": {version: "1", mode: "simulation", keyType,
    invoke: (args, trusted, ms, nativeContext) =>
      taskBridge.get(args, trusted, ms, nativeContext)},
  "tasks.create": {version: "1", mode: "simulation", keyType,
    invoke: (args, trusted, ms, nativeContext) =>
      taskBridge.create(args, trusted, ms, nativeContext)}
});
if (binding.diagnostic) throw new Error(binding.diagnostic.code);
const selection = prepareProgram(approvedArtifactBytes, binding.boundModel, admission);
if (selection.diagnostic) throw new Error(selection.diagnostic.code);
const outcome = await invokeProgram(selection.prepared, {taskId}, freshInvocation);
```

## Conformance obligations

Run identical artifact/model bytes in Java and Node. Acceptance requires:

| Positive case | Required negative counterpart |
| --- | --- |
| Two programs reuse one complete binding | Missing unused invoker, extra ID, wrong version/mode/key type reject atomically |
| Exact reference prepares offline | Wrong digest/format/recipe/environment, missing model or embedded fallback reject |
| Concurrent scopes and same-named model types remain isolated | Wrong-scope invocation and mutated registration cannot redirect calls |
| New binding coexists with old Prepared | Replacement cannot change old dispatch; revocation prevents subsequent dispatch |
| Remote success/rejection preserves typed evidence | Lost WRITE response stays UNKNOWN; late response and client retry cannot duplicate execution |
| Fresh bindings/key/event and source constants reach the right channels | Protected override/key conversion fails; second potential WRITE rejects |
| Cache eviction permits exact reload | Missing retired dependency fails; finite preparation limits reject excess |

The local shared corpus and component negatives exercise model binding, preparation and invocation while preserving all `/1` goldens; see [testing evidence](../testing.md). Real transport/native guarantees still require host evidence. Host-owned cache eviction, revocation and retirement remain lifecycle responsibilities, not new runtime management APIs.
