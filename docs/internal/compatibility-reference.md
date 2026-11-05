# Earlier Keir contracts

Use these sections to maintain existing artifacts and hosts. For a new integration, start with the [Keir specification](../specs/README.md). See [serialized identifiers and compiler selectors](implementation-reference.md#serialized-identifiers-and-compiler-selectors) for the exact formats.

- [Original read-only compiler and artifact](#original-compiler-envelope)
- [Earlier admission and trust checks](#external-admission-pins)
- [Original read-only model and sample](#original-read-model)
- [Original read-only source and example](#original-read-source)
- [Earlier binding and invocation contracts](#earlier-binding)
- [Earlier qualified-write admission](#earlier-qualification)
- [Earlier runtime execution contracts](#earlier-runtime)
- [Catalog-bound model and artifact changes](#catalog-delta)
- [Catalog-bound examples, migration and acceptance](#catalog-examples)
- [Earlier follow-up walkthrough](#earlier-follow-up)
- [Original simulated-write source and artifact](#original-write-formats)
- [Original simulated-write model and key schema](#original-write-model)
- [Original simulated-write host API](#original-write-host-api)
- [Earlier Java runtime APIs](#earlier-java-apis)

<a id="original-compiler-envelope"></a>
## Original read-only compiler and artifact

<a id="legacy-read1-envelope-and-compiler-boundary"></a>
#### Implementation reference: earlier compiler and artifact contracts

<a id="compiler-boundary"></a>
#### Legacy read compiler boundary

This section specifies the read/1 compatibility artifact. The current single-write contract uses a separate profile and format with checked guard/handler fields and definite environments. Canonicalization and independent raw/canonical DCM ceilings remain shared; external admission pins apply only to legacy APIs. See the [implementation reference](implementation-reference.md) for retained historical contract details. Old artifacts are not reinterpreted or migrated.

Conceptual API: `compile(sourceBytes, trustedDcmBytes, trustedEnvironment) -> ArtifactBytes | Diagnostics`. The environment selects profile `keir-read/1` and exact compiler/CEL/protobuf package identities. CLI transport must preserve these arguments without shell interpolation, bound subprocess input/output/time/concurrency and emit no artifact on failure. Runtime execution never requires the compiler.

Source may select any declared entrypoint within the trusted DCM, subject to that entrypoint's grants. The compiler environment has no separate expected-entrypoint selector. The embedding host independently selects the expected entrypoint/consumer during runtime admission; successful compilation alone does not authorize that selection.

Validate [source and CEL](../specs/language.md), [DCM](../specs/domain-model.md) and aggregate bounds, then lower in source order. No source interpretation at runtime. The compiler emits a JSON object with exactly these fields:

| Field | Exact content |
| --- | --- |
| `format` | `keir-artifact/read-1` |
| `profile` | `keir-read/1` |
| `compiler` | Exact build identity string selected outside source |
| `environment` | `{celJava,celNode,protobufJava,protobufNode}`: exact qualified package version strings, no ranges |
| `sourceDigest` | Digest of canonical parsed source |
| `dcmDigest` | Digest of canonical DCM |
| `dcm` | Complete validated DCM object |
| `entrypoint` | Resolved entrypoint ID |
| `descriptors` | Standard padded base64 of a deterministic `FileDescriptorSet` |
| `expressions` | Map `e0`, `e1`, ... to `{checked,variables,resultType}` |
| `steps` | Array of lowered CALL/BIND instructions below |
| `return` | `{type,fields}`: output record type and field-to-binding map |

`checked` is standard padded base64 of deterministic `dev.cel.expr.CheckedExpr` bytes. `variables` maps variable names to DCM/builtin type names; it includes only variables available before that expression, with `input` always present. `resultType` is the semantic scalar type after overlay checking. No timestamps, absolute source paths, concrete host identities, request keys, signatures or artifact self-digest appear in the envelope.

Lowered binding is exactly `{literal:boolean|string,type:typeName}` or `{expr:expressionId,type:typeName}`. CALL is `{op:"CALL",id,capability,version,args,resultType}`; `args` contains all and only agent parameters, and `resultType` is the DCM result record. Host parameter names/types remain in the bound DCM and are not source args. BIND is `{op:"BIND",id,value,resultType}` with scalar binding `value`. The ordered steps plus root return are the complete instruction program; there are no implicit guards, error transitions or retry handlers.

Assign expression IDs in step order, visiting argument names in ASCII lexical order, then root return fields in that order. A BIND visits its one value. Literal bindings have no expression ID. Variables include all prior immutable bindings, but AST references must be to declared names. Runtime checks instruction/opcode/type/capability/version consistency with this envelope before preparation; trusted provenance does not excuse malformed format.

<a id="external-admission-pins"></a>
## Earlier admission and trust checks

#### Admission and trust

This section preserves the legacy external-pin contract. The current single-write contract uses the [binding and preparation contract](../specs/host-model-binding.md#effect4-binding-and-preparation) instead.

Sample host obtains compiler output through a host-controlled path and separately retains its expected artifact digest. `prepare` receives expected digest, expected DCM digest, entrypoint/consumer and supported environment from trusted host configuration, **not from the artifact or customer**. Bound raw byte size, compare exact SHA-256 to that external expected value before parsing/preparing nested content, then validate canonical encoding, DCM digest, versions, bounds and instruction integrity. Any mismatch means no prepared handle and no adapter call. Recomputing artifact-local hashes cannot authenticate forged output.

The independently held digest authenticates only the host-selected bytes in this controlled example; it is not a general compiler signing system. Do not bypass these required external-pin checks in legacy preparation. No production activation registry, signing-key lifecycle, approval UI or cross-tenant routing is claimed. Changing DCM fields/descriptors, consumer meaning, profile or dependency pins requires explicit recompilation and host reselection; this profile has no automatic compatibility migration.

<a id="original-read-model"></a>
## Original read-only model and sample

<a id="legacy-read1-model"></a>
#### Implementation reference: original read-only model

This compatibility section preserves the `/1` schema, including required grants. For current generic defaults without named grants, see the [hook contract](../specs/generic-hook-capability-contract.md). Do not remove fields from an existing DCM or treat its sample grant map as the default hook design. Exact historical schema identities remain documented in the [implementation reference](implementation-reference.md).

This section defines `keir-dcm/read-1`. A host selects the authoritative model. The source references it through its exact model identity. See the [read/1 source example](../specs/language.md#source-and-example).

#### Closed schema

This schema remains `keir-dcm/read-1`. The implemented [effect DCM](../specs/single-effect.md#closed-dcm-additions) has a separate format, explicit lifecycle/effect authority, one protected native-key contract and simulation-only assurance. Those fields must not be silently admitted into the read format.

All fields below are required; unknown fields fail. Maps have unique keys. Types and identifiers obey language identifier rules. Root: `{format,domain,types,capabilities,entrypoints}`. `format` is `keir-dcm/read-1`; `domain` is `{id,version}` with nonempty bounded strings. Types are named definitions of exactly one shape:

- `{kind:"nominal",base:"String"}`.
- `{kind:"enum",values:[nonempty unique bounded strings]}`.
- `{kind:"record",fields:{fieldName:typeName,...}}` with at least one field, all required scalar types.

`Boolean` and `String` are built-ins and cannot be redefined. Type names must resolve. No cycles, nested records, reserved-name collisions or unsupported kinds. The format's administrative arrays/maps do not admit collections into CEL.

A capability is `{version,parameters,result,effect,errors}`. `parameters` maps names to `{type,binding}` where binding is `agent` or `host`; all are required supported scalar types. `result` names a record. `effect` must be `READ`; writes and unknown effect classes fail. `errors` is a unique list of allowed business error codes matching `[A-Z][A-Z0-9_]{0,63}`. Transient failure is a generic adapter outcome, not a user-handleable error. A host-bound parameter is resolved by the same name from a separate trusted binding map, not a CEL expression or source-selected path.

An entrypoint is `{input,output,trigger,consumer,grants}`. Input/output name records. `trigger` is `REQUEST`. `consumer` is `{id,version,kind}` with kind `DISPLAY`; its complete value is compatibility-sensitive. Consumer `id` uses the capability/entrypoint identifier rule `[A-Za-z][A-Za-z0-9.]{0,99}` and excludes CEL reserved identifiers; `version` is a nonempty bounded string. `grants` maps allowed capability IDs to positive integer maximum call counts, bounded by the profile. No implicit grant. Source call counts are checked conservatively; runtime enforces them again. Profile limits are defined centrally, not overridable in source/DCM.

#### Sample model

This is a simulated sample contract, not a certification of real task access. The host's adapter registration must separately declare whether it is fake or real and meet the current authorization contract.

```json
{
  "format":"keir-dcm/read-1",
  "domain":{"id":"sample.tasks","version":"1"},
  "types":{
    "TaskId":{"kind":"nominal","base":"String"},
    "UserId":{"kind":"nominal","base":"String"},
    "TaskStatus":{"kind":"enum","values":["OPEN","COMPLETED"]},
    "Classification":{"kind":"enum","values":["DONE","PENDING"]},
    "ClassifyInput":{"kind":"record","fields":{"taskId":"TaskId"}},
    "Task":{"kind":"record","fields":{"id":"TaskId","status":"TaskStatus"}},
    "ClassifyResult":{"kind":"record","fields":{"classification":"Classification"}}
  },
  "capabilities":{
    "tasks.get":{
      "version":"1",
      "parameters":{"taskId":{"type":"TaskId","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},
      "result":"Task","effect":"READ","errors":["TASK_NOT_FOUND","ACCESS_DENIED"]
    }
  },
  "entrypoints":{
    "ClassifyTask":{
      "input":"ClassifyInput","output":"ClassifyResult","trigger":"REQUEST",
      "consumer":{"id":"sample.classification","version":"1","kind":"DISPLAY"},
      "grants":{"tasks.get":1}
    }
  }
}
```

Compile context selects the trusted model and compiler/profile/environment pins. Source selects one entrypoint declared in that model and cannot widen its grants. The host independently pins the intended entrypoint and consumer at runtime admission and may narrow grants at admission/invocation. No tenant overlays, endpoint registration, certifications registry, activation routing or source imports are implemented by this format. A future addition needs an explicit format/profile compatibility decision, not ignored fields.

The host validates current actor/resource access on every adapter call. An artifact grant is a ceiling, not authentication or ownership evidence. `tasks.get` must return the requested task or reject; this relational obligation is host-specific and does not become a task-specific branch in the generic runtime. Host bindings are supplied separately from validated agent arguments; collisions are rejected, never resolved by merge precedence. Fake adapters may model outcomes but cannot establish real authorization.

<a id="original-read-source"></a>
## Original read-only source and example

<a id="source-and-example"></a>
#### Implementation reference: earlier source rules and example

The [catalog-bound contract](../specs/generic-hook-capability-contract.md) permits repeated or different published reads and adds explicit nominal literals. The following example uses the original read-only contract. See the [implementation reference](implementation-reference.md) for the exact selectors and source tag.

This page defines `keir-read/1`. Implemented `when`/`onError` syntax and definite-availability restrictions belong only to the separately selected [single-effect profile](../specs/single-effect.md#source-and-definite-availability); they are not accepted additions to this read grammar.

Accept UTF-8 JSON or YAML containing only the JSON data model. Reject duplicate keys, unknown properties, aliases, anchors, merge keys, tags, multiple documents, malformed Unicode, null and numbers in bindings. No coercion. Mapping order is immaterial; step order matters. Enforce [limits](../specs/runtime.md#limits) before and during parsing.

For both source and DCM, YAML uses JSON-compatible scalar resolution, not YAML 1.1 implicit typing. Only lowercase plain `true`, `false`, `null` and the JSON number grammar `-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?` resolve implicitly to non-string values. Other nonempty plain scalars, including `yes`, `on`, `TRUE`, timestamps, `0x10`, `01`, `.inf` and `~`, remain strings. Quoted scalars remain strings. Empty/omitted YAML values are rejected rather than implicitly becoming null. Mapping keys must resolve to strings; reject non-string keys and duplicate decoded keys. Reject every explicit tag, anchor, alias and merge key, including otherwise standard explicit tags. This parsing rule does not admit numeric/null bindings: subsequent schema validation rejects them and accepts numbers only in the bounded integer metadata fields that explicitly require them. No rounding, loss of numeric precision or conversion to fit an expected field type is permitted; for example an unquoted numeric `domain.version` is rejected, not stringified.

Root fields, all required and no others: `language`, `domain`, `entrypoint`, `name`, `steps`, `return`. `language` is `keir/v0`. `domain` contains exactly `id`, `version`, `fingerprint`; these match the trusted DCM and its canonical digest. `name` matches `[a-z][a-z0-9-]{0,63}`. `entrypoint` and capability IDs match `[A-Za-z][A-Za-z0-9.]{0,99}`. Step IDs and record/type/field names match `[A-Za-z][A-Za-z0-9]{0,63}`. Step IDs are unique and cannot be `input`, a type name, or CEL reserved identifier.

Each step is exactly `{id, call, args}` or `{id, let}`. `args` is a map of declared agent-bound parameter names to bindings. A binding is exactly `{literal: boolean|string}` or `{cel: string}`. `return` is exactly `{record: {field: binding, ...}}`, with precisely the entrypoint output fields. `steps` is nonempty. No `when`, `onError`, writes, collections, optional fields, time, general branching, retries or executable source.

Example source for the exact [sample model](../specs/domain-model.md#sample-model). Its fingerprint was computed from that model's canonical JSON; it authenticates neither the host nor the model's guarantees.

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

<a id="earlier-binding"></a>
## Earlier binding and invocation contracts

<a id="legacy-read1-effect1-read2-effect2-and-effect3"></a>
#### Implementation reference: earlier binding contracts

The remaining sections preserve the versioned legacy binding and admission contracts. Their approval, scope, authorization and qualification inputs do not apply to effect/4. The `/2` reference-model API extends `/1`; effect/3 adds real-mode qualification. See [testing](../testing.md) for implementation evidence, separately from these requirements.

#### One binding boundary

The explicit [effect/3 extension](../specs/real-single-effect-admission.md#legacy-effect3-qualification) adds an optional final `qualification` argument to `bindModel`. Java adds `bindModel(bytes, approval, invokers, Qualification)` and retains the old three-argument overload; explicit fourth-argument null is invalid. Its `Qualification` contains evidence and a synchronous `Supplier<?>` current callback accepting Boolean true by value. Node treats undefined as omission and rejects null and boxed Boolean results. Qualification is required only for real effect/3, forbidden for simulation and earlier profiles. All existing `/1` and `/2` rules below remain unchanged. Native-host/deployment qualification is a separate obligation.

The host binds a complete approved DCM and its invokers together, then reuses that opaque BoundModel. Preparation accepts no separate invoker map. A callable normally bridges to an internal HTTP/RPC service; it need not implement business logic locally.

Implemented API families (diagnostics must be checked before using handles):

```text
bindModel(dcmBytes, approval, invokers) -> {boundModel, diagnostic}
prepareProgram(artifactBytes, boundModel, admission) -> {prepared, diagnostic}
Node: invokeProgram(prepared, businessInput, trustedInvocation) -> Promise<Outcome>
Java read: invokeProgram(prepared, businessInput, Invocation2) -> CompletionStage<Outcome>
Java effect: invokeEffectProgram(prepared, businessInput, EffectInvocation2) -> CompletionStage<Outcome>
```

`approval` is a closed host-only record: `{scope, modelRef, profile, environment, mode, keyBinding?}`. Scope is an opaque host-selected nonempty String of at most 256 UTF-8 bytes, compared by exact equality; it is not an artifact field or credential. Effect bindings require the existing native `keyBinding`: its type must match DCM hostKey.type and its validator must be callable; snapshot but do not invoke the validator during binding. Read bindings omit it. Effect/2 mode remains simulation-only. Approval comes from independently trusted configuration, never from customer-supplied self-description.

Registration is atomic. Bound raw bytes before parsing; validate canonical DCM size, schema and exact approved identity, derive descriptors, then validate **all** invokers. Require exactly one callable per catalog capability ID, including unused or policy-denied operations. Missing, extra, duplicate, non-callable, wrong-version, wrong-mode or incompatible key-type entries reject the entire binding. READ/WRITE classification comes only from the DCM. Read-profile catalogs containing WRITE definitions still require their bindings but cannot dispatch them. Registration is not a per-extension allowlist; optional policy and fresh authorization restrict dispatch separately.

Preserve registration metadata shapes: read `{version,mode,invoke}`, effect `{version,mode,keyType,invoke}` for every capability. Copy model bytes and metadata, snapshot callable references and expose no mutable map. Registration calls neither invokers nor authorizers. Host code owns callable behavior and thread safety; snapshots cannot freeze captured clients. Changing a callable requires a new binding, not map mutation.

#### Exact reference artifact

For `keir-artifact/read-2` and `keir-artifact/effect-2`, replace `/1` envelope fields `dcm`, `dcmDigest`, `descriptors` with exactly `modelRef`:

```text
{domainId, domainVersion, format, digest, descriptorRecipe}
```

`domainId`/`domainVersion` equal DCM `domain.id`/`domain.version`; `format` is the corresponding `keir-dcm/read-2` or `keir-dcm/effect-2`; `digest` hashes the complete canonical DCM using [existing canonicalization](../specs/compiler-artifact.md#canonical-bytes-and-digest-rules). `descriptorRecipe` is exactly `keir-record-descriptors/1`: the existing [descriptor bridge](../specs/compiler-artifact.md#descriptortype-bridge), including every DCM record, sorted fields, scalar types, file/package names and no imports. Unknown recipes fail closed.

All remaining envelope fields retain their profile rules, including compiler, environment, sourceDigest, expressions, steps and return. No embedded fallback, descriptor override, endpoint or resolver is allowed. The compiler receives the full trusted DCM and uses this recipe when checking expressions. Source domain fingerprint still equals the canonical DCM digest. Artifact digest covers the complete new canonical envelope.

Preparation bounds and authenticates artifact bytes against the external expected artifact digest before parsing. Admission retains its independently selected model digest, compiler/environment, entrypoint, consumer and restrictions; mode must equal the binding's mode. Profile selection is pinned by binding approval. `keyBinding` moves to effect registration and cannot be overridden during preparation. Require exact equality between artifact modelRef and bound modelRef, and between artifact profile/environment and binding approval. Validate the entire plan and checked-expression metadata against that model; model approval never substitutes for program validation.

Cache pure model/type state only by full modelRef, profile and exact environment tuple `{celJava,celNode,protobufJava,protobufNode}`. Approval scope remains isolated; any cross-scope deduplication shares only inert validated state, never invokers, clients or permission. Same-named records in different models must not collide. Prepared pins its binding and expression plans. Neither handle caches invocation identity, protected values, keys, authorization results or deadlines.

#### Invocation and remote dispatch

`trustedInvocation` retains `/2` bindings, deadline, authorize and optional narrowing policy/effect ceiling; add required `scope`, matching the binding approval scope. Effect invocations also supply the original native requestKey/event. The host selects this scope from authenticated context. It closes the authorizer over the selected binding and checks current approval/revocation plus caller authority on the existing `{entrypoint}` and `{entrypoint,capability,version}` requests. Scope equality alone grants no permission.

Keep callable signatures unchanged: read `(agentArgs, protectedArgs, remainingMs)`; effect adds fourth `{requestKey,event}`. Evaluated business arguments and trusted host bindings are separate validated snapshots. Identity needed by a service comes through host bindings or a host-controlled request context, never customer fields. No process-global “current user.” A reusable client may retain transport configuration/service credentials within its approved scope; per-user credentials and identity must remain request-local and must not leak through pooled mutable headers.

The remaining deadline is a duration in milliseconds, not a cross-process clock timestamp. The bridge budgets connection, serialization and remote work within it. Host-owned routing, discovery, authentication, serialization and client libraries must preserve the native key contract: forward the original immutable key, never stringify/hash/rederive it. A transport unable to preserve that contract is unsupported; this contract defines no key wire format or generic transport SDK.

Map service replies to existing typed outcomes and declared rejection codes, not arbitrary HTTP statuses. READ retains its result/failure rules. WRITE SUCCESS requires CONFIRMED; BUSINESS_REJECTION and TRANSIENT_FAILURE require definite NONE; possible commit without definitive evidence maps UNKNOWN_COMMIT/UNKNOWN. Transport failure or timeout after dispatch cannot establish NONE. Thrown/malformed responses retain existing failure and effect accounting. No automatic retries, including hidden client retries for WRITE.

Preserve the existing response/timeout winner: winning effect evidence settles before completion-clock/result validation, so invalid result data cannot erase CONFIRMED. Late responses cannot resume execution or rewrite its returned receipt. Timeout/cancellation does not prove remote cancellation. Keep aggregate budgets, fresh per-dispatch checks and the hard maximum of one potential WRITE, including guarded calls. Customer constants remain source-authored; binding adds no configuration input or default named grants.

#### Coexistence and lifecycle

Different model versions/digests and different host bindings can coexist. Even additive catalog changes produce a new model reference; programs adopting that changed model require explicit recompilation/selection. Existing programs may keep using their exact earlier snapshot while its dependencies remain available and approved. Replacing invokers for the same model creates a new binding; existing Prepared objects retain the old one. To use the replacement, explicitly prepare/reselect again. Never silently route an old handle to “latest.”

Lifecycle is host-owned: deprecation may prohibit new preparation while allowing existing activations; revocation denies new invocation and subsequent dispatch through current authorizers. Already-dispatched work cannot be recalled; retain its honest receipt. Retirement releases dependencies only after hosts account for prepared/in-flight users. Missing exact models at cold start fail locally, without network resolution. Evicting unpinned pure cache entries changes neither approval nor revocation. Maintain existing individual byte/AST limits and finite host limits for model count/bytes, prepared handles, preparation concurrency and queues; fail before excess allocation.

Diagnostics are redacted and deterministic across runtimes. Binding schema/identity/registration errors use `ADMISSION_INVALID` under `/binding`; size excess uses `BUDGET_EXCEEDED`. Unsupported/mismatched artifact references use `ARTIFACT_INVALID` at `/artifact/modelRef`; conflicting admission pins use `ADMISSION_INVALID` under `/admission`. Wrong invocation scope uses `HOST_CONTEXT_INVALID` at the empty pointer; current revocation uses existing `AUTHORITY_DENIED`. No handle or dispatch follows preflight failure. Validate binding before preparation; within preparation preserve external-digest-before-parse ordering, then envelope/reference, admission and plan checks.

#### Equivalent host sketches

Both sketches use the complete two-capability [walkthrough catalog](../specs/generic-hook-example.md). Helpers represent host-owned values/clients, not Keir utilities. Approval pins the modelRef, environment and simulation mode; admission pins the artifact and hook/consumer. `freshInvocation` supplies matching scope, current authorizer, bindings, deadline and original key/event. These sketches show the implemented wrappers; complete runnable integrations are in the [Java](../../examples/java-host/README.md) and [Node](../../examples/node-host/README.md) hosts. Real-write admission remains rejected.

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

#### Conformance obligations

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

<a id="earlier-qualification"></a>
## Earlier qualified-write admission

<a id="legacy-effect3-qualification"></a>
#### Compatibility reference: qualification and admission

The following sections specify only `keir-effect/3`. They preserve its qualification protocol and extend the [single-effect semantics](../specs/single-effect.md) and [legacy host model binding](../specs/host-model-binding.md#legacy-read1-effect1-read2-effect2-and-effect3). Effect/4 does not accept this qualification input. Implementation and native-host evidence are recorded in [testing](../testing.md), outside this contract.

#### Compatibility boundary

The legacy identities are exactly `keir-effect/3`, `keir-dcm/effect-3`, `keir-artifact/effect-3` and `keir-scenario/effect-3`. The compiler selects the profile explicitly (`--profile=keir-effect/3`, Java `Environment.effect3()`). No format detection by optional field presence and no automatic upgrade of existing bytes.

The DCM retains the effect-2 closed schema except that `format` is `keir-dcm/effect-3` and `assurance` is exactly `HOST_QUALIFIED_SINGLE_WRITE`. That value declares the required admission protocol; it is not evidence of host qualification. Entrypoints remain EVENT/AFTER_COMMIT with EVENT_REPORT consumption. REQUEST, BEFORE_COMMIT, multiple possible writes and unsupported transport guarantees remain rejected. Catalogs expose generic native operations, not customer rules.

The artifact retains the effect-2 reference envelope and executable semantics with profile/format/modelRef format changed to /3. Source remains `keir/v0`; its domain fingerprint identifies the /3 DCM. Existing checked-expression and descriptor recipe rules are unchanged. The compiler counts all potential WRITE calls, including mutually exclusive guards: maximum one; zero is valid. Effect ceiling defaults remain one and narrowing-only. No new expression construct, source key access or business configuration channel.

Every /1 and /2 format, schema, API behavior and rejection stays unchanged. In particular, effect/2 still requires SIMULATION_ONLY and rejects real mode. Read/2 does not acquire write permission. Unknown /3 assurance, mixed formats, stale compiler/environment pins and mismatched model references fail before a handle or dispatch is produced.

#### Host qualification input

Reuse the binding/preparation/invocation API families. Add one optional final argument to `bindModel`; Java adds an overload rather than changing old callers:

```text
bindModel(dcmBytes, approval, invokers, qualification?)
qualification = {evidence, isCurrent}
```

`qualification` is required only for effect/3 with `approval.mode = "real"`. It is forbidden in simulation mode and all earlier profiles. Approval otherwise retains its existing exact fields, including scope, modelRef, environment, mode and native keyBinding. Invoker records retain `{version, mode, keyType, invoke}` for all capabilities in an effect catalog. Preparation and invocation signatures remain unchanged; Java effect/3 uses the existing effect invocation record's scope/bindings/event/key/authorization/deadline fields. Add profile-aware dispatch, not a second interpreter.

`evidence` is a closed, host-only data record:

```text
{
  contract: "keir-real-effect-evidence/1",
  id: String,
  revision: positive safe integer,
  scope: String,
  modelRef: existing exact ModelRef,
  environment: existing exact Environment,
  hostBuild: "sha256:<64 lowercase hex digits>",
  keyType: String,
  capabilities: [{id: String, version: String}],
  reportDigest: "sha256:<64 lowercase hex digits>"
}
```

Bound evidence to 16,384 canonical UTF-8 bytes before copying; reject unknown fields. `id` is nonempty, at most 128 UTF-8 bytes; scope/keyType retain existing 256-byte limits; all strings must be well-formed. Capability identifiers/versions use existing DCM validation and limits. Capabilities are sorted by ID, unique, and exactly enumerate the complete registered catalog, including reads and unused capabilities. ModelRef and environment exactly match approval; scope equals approval scope; keyType equals DCM hostKey.type and registered native key binding. `hostBuild` identifies the reviewed adapter/service/schema deployment. Changing those semantics or callable registrations requires new binding and qualification, never mutable substitution under an existing handle.

`isCurrent` is a trusted synchronous, non-I/O callback returning exactly Boolean. It closes over an independently approved deployment record and verifies this evidence revision/report, host build, scope and revocation/expiry state. Async/thenable/non-Boolean return or exception fails closed. The host implements it; it is never accepted from source, scenario, browser or artifact. Snapshot the evidence and callback reference with the binding. Do not cache its result.

In Node, only primitive `true` grants authority. Every other callback return denies synchronously without awaiting or assimilating thenables. On the tested Node baseline, defensive rejection handling is limited to native same-realm Promises with the captured intrinsic prototype, no own constructor, and unchanged complete intrinsic constructor/species descriptors, rechecked on each cleanup attempt. Captured intrinsic invocation bypasses an own `then` getter; both fulfillment and rejection handlers discard their values so cleanup cannot forward a later-poisoned fulfilled object. Constructor/species overrides, subclasses, foreign-realm Promises, proxies and altered intrinsic paths are denied without cleanup getter execution. The host must handle asynchronous failures outside that verified subset. This does not contain arbitrary same-process host code: an invalid callback can still cause a host-level unhandled rejection or terminate its own process. No global rejection handler, supplied-object mutation or runtime upgrade is part of admission.

The report identified by reportDigest must be reviewed qualification evidence for the deployed native service: original key preservation; atomic matching-key/payload receipts; mismatch rejection; retention without reuse; current owner/resource authorization; truthful commit evidence; crash, timeout and concurrent duplicate tests; hidden retry prohibition. The host deployment approval explicitly selects that report and build. Runtime checks identity and current approval; it cannot prove database atomicity or certify a dishonest host from a JSON claim. An allowlisted host-owned report is required; a customer-supplied digest, DCM assurance or successful simulation is insufficient. No remote report fetching, registry, signature infrastructure or generic transport package is introduced.

Qualification evidence applies only to the native service and deployment it identifies. Matching method signatures do not qualify another host or transport; each requires its own reviewed report and explicit support.

#### Validation and revocation

Binding validates, in order: existing byte/schema/model approval rules; profile/mode compatibility; evidence shape and exact equality; all invoker registrations; then `isCurrent()`. It invokes no business adapter or existing per-user authorizer. Failure produces no BoundModel. Add `qualification` as a closed optional fourth argument; null is not omission.

Preparation retains external expected artifact digest before parse, exact artifact/model/compiler/environment/entrypoint/consumer validation, and ordinary plan checks. Real effect/3 preparation also requires the retained qualification to be current. Evidence belongs to BoundModel, never to source or artifact; its identity is retained with Prepared for audit. A later deployment using new evidence explicitly prepares a new binding; it cannot rewrite an old handle.

At invocation entry and before every reached READ or WRITE, require current qualification in addition to existing scope, current host authorization, policy, effect ceiling, argument, budget and deadline checks. Check qualification before invoking the per-user authorizer or adapter. Current authorization still closes over protected actor/event/resource identity. Qualification approval is not user permission, and program approval is not qualification.

Recheck the remaining deadline after the synchronous qualification callback and before dispatch. The callback has no permission to extend budgets or reserve an effect. A blocking host callback violates the host contract; neither runtime claims to preempt arbitrary host code.

If qualification changes during a call, that call's evidence still settles under the existing response/timeout winner rules. Recheck before any subsequent dispatch; do not erase a CONFIRMED receipt or claim rollback. The native adapter must check authority immediately before its own commit. No claim that callback revocation recalls work already dispatched.

Binding/preparation qualification failures use `ADMISSION_INVALID` at `/binding/qualification` or `/admission/qualification`; size excess uses `BUDGET_EXCEEDED`. Invocation/dispatch revocation or callback failure uses `AUTHORITY_DENIED`, with ordinary profile termination and retained effect evidence. No raw evidence, user IDs, keys, service exceptions or payloads enter diagnostics/trace.

#### Same program, separate execution authority

An effect/3 artifact can be prepared in either simulation or real mode against the same exact /3 DCM/modelRef, compiler and environment. Simulation requires all invokers to be simulation-mode and rejects qualification; real requires all registrations to be real-mode and the current qualification above. Neither mode inherits a Prepared handle from the other. No fallback between modes on failure.

The effect/3 scenario envelope retains effect-2's strict input, trusted fixture selection, ordered stubs and expected outcome/calls/consumer fields, changing only format. Fixtures construct protected event/key values; they cannot supply real invokers or qualification. The simulation adapter records generic create arguments and returns fixtures without touching native task tables. Business expectations are independently chosen before observing results. Approval pins the exact artifact and scenario evidence; enabling the same artifact additionally requires real host qualification and current user authority. No recompile or behavior change between checked and real program bytes.

#### Required shared acceptance

Run the same artifact, model and vector identities through compiler, Java and Node. Extend existing effect scenarios rather than duplicating language semantics.

1. /2 still rejects real mode; /3 rejects wrong/missing evidence, extra fields, oversized evidence, null, wrong model/scope/environment/key type/catalog/build approval and stale revision. Zero adapter calls.
2. Simulation rejects qualification and mixed/real invokers; real rejects simulation invokers. Same artifact passes the expected program scenarios in both properly admitted modes.
3. Current callback false, throw, async or invalid result rejects binding/preparation; revocation at invocation and between read/write denies before dispatch. Revocation after confirmed write retains CONFIRMED.
4. Two potential writes fail compile and forged-artifact preparation. Zero narrowed ceiling blocks a reached write; false guard dispatches nothing. Native key identity, protected actor/event source and original event remain intact.
5. Duplicate-confirmed native receipt, definite rejection, transient NONE, timeout/UNKNOWN, malformed outer envelope, malformed value after CONFIRMED and response/deadline winner retain existing semantics and consumer evidence.
6. Exact byte/model/consumer/compiler/environment mismatch and missing dependencies fail before dispatch; scopes sharing pure caches cannot share invokers or qualification decisions.

Native persistence and race qualification are separate host obligations. Passing deterministic vectors alone does not qualify a deployment.

<a id="earlier-runtime"></a>
## Earlier runtime execution contracts

<a id="legacy-read1-execution-and-profile-extensions"></a>
#### Implementation reference: earlier execution contracts

The next three sections specify read/1. Its grant maps and authorizer remain required. [Host model binding](../specs/host-model-binding.md#legacy-read1-effect1-read2-effect2-and-effect3) defines the `/2` binding, reference-artifact and optional narrowing-policy extensions. [Single-effect semantics](../specs/single-effect.md) and [effect/3 qualification](../specs/real-single-effect-admission.md#legacy-effect3-qualification) define the effect extensions. None of those legacy host inputs is required by effect/4.

#### Prepare and invoke

These APIs and outcomes remain the read-profile contract. The implemented [single-effect runtime delta](../specs/single-effect.md#host-api-and-native-key) adds a separate protected key/event channel, current effect grant, terminal handler results and retained effect receipts only under its separate profile. It does not weaken the read loader or change read adapter signatures.

`prepare(artifactBytes, admission, adapters) -> Prepared | Diagnostic` validates the artifact before resolving adapters and preparing CEL. Admission is the closed host-owned record `{expectedArtifactDigest,expectedDcmDigest,entrypoint,consumer,compiler,environment,mode,grants,limits?}`. Consumer, compiler and environment must exactly match the selected artifact contract. `mode` is `simulation` or `real`. Preparing calls neither authorization callbacks nor adapters. A Prepared handle is immutable and caches only plan/environment and admission ceilings, not user identity or mutable invocation data.

`adapters` is a host-supplied map from capability ID to the closed registration `{version,mode,invoke}`. Every called capability needs a registration with the exact DCM version and admission mode; unknown IDs, invalid registrations, missing required registrations and version/mode mismatches fail preparation. Registered but unused DCM capabilities grant no authority. Snapshot validated registration metadata/function references during preparation; later map mutation must not change a prepared binding. This is an in-process API, not a registry service or customer configuration format.

The registration is an in-process dispatch interface, not a requirement that the capability's business implementation runs locally. Host adapters normally bridge to internal services over HTTP/RPC or another host-owned client. The host owns routing, credentials, trusted identity propagation and response mapping; Keir validates the declared boundary. See the [service invocation target](../specs/generic-hook-capability-contract.md#capability-invocation-across-services). Current profile/admission restrictions and real-adapter qualification requirements remain unchanged.

`invoke(prepared, businessInput, trustedInvocation) -> Outcome` gets a fresh closed input record and trusted `{bindings,deadline,authorize,grants}` context. `bindings` supplies declared protected scalar parameters by name; `deadline` uses the host monotonic clock; `authorize` checks current invocation/capability scope immediately before each dispatch. The host constructs this context; it is not deserialized verbatim from customer JSON. No CEL variable exposes it. Validate input, host bindings and the current host grant before any call. A host may narrow grants, never widen artifact/DCM rights.

Both admission and invocation `grants` are required maps from capability ID to positive integer call ceilings, shaped like entrypoint grants. Omission of an ID means zero permission, never inheritance. Admission grants may only narrow the selected DCM entrypoint; fresh invocation grants may only narrow admission. Reject unknown IDs, invalid counts or widening during preparation, or as FAILED/`HOST_CONTEXT_INVALID` for invocation context. Snapshot invocation grants for that invocation and count every dispatch; a missing/exhausted grant terminates as FAILED/`AUTHORITY_DENIED` before dispatch. Current authorization remains required even with a positive remaining count. This explicit fresh map supersedes the earlier consultation suggestion that admission grants alone suffice; no additional per-user policy framework is required.

Authorization receives an immutable closed request `{entrypoint}` once for the invocation, then `{entrypoint,capability,version}` immediately before each call. The host callback captures current identity/resource context; protected values are not copied into this request or CEL. Check grants before calling the per-call authorization function.

`authorize` returns an asynchronous Boolean for the selected invocation or call: `true` permits continuing validation/dispatch, `false` terminates as FAILED/`AUTHORITY_DENIED`. A thrown exception, rejected future/promise or non-Boolean result terminates as FAILED/`AUTHORITY_CHECK_FAILED`; it is not a business denial or an adapter exception. No adapter dispatch follows either failure. Authorization consumes the invocation deadline; if the deadline expires while it is pending, classify LIMIT_EXCEEDED/`BUDGET_EXCEEDED` and discard its eventual result. Check expiry again after a successful callback before dispatch. Never expose the callback's exception details.

`deadline` is a host-owned pair `{clock,expiresAtMs}`, where `clock.nowMs()` returns nondecreasing integer milliseconds from an arbitrary monotonic origin and `expiresAtMs` is in that same clock domain. Both are nonnegative exact integers no larger than 2^53-1; reject invalid values/clock regression as FAILED/`HOST_CONTEXT_INVALID`. Expired means `nowMs() >= expiresAtMs`; remaining budget is `max(0, expiresAtMs - nowMs())` milliseconds. Java hosts may derive integer milliseconds from their monotonic timer; Node hosts do likewise. Never serialize these readings as UTC/wallclock or compare origins across processes. Shared fixtures inject identical logical clock readings and expiry values; real hosts independently own their clocks and native timeout scheduling. This context is not part of the artifact or business input.

Adapter call is `invoke(agentArgs, protectedArgs, remainingDeadline) -> AdapterOutcome`, with separately validated immutable argument records. Do not merge into a caller-controlled object. The adapter still checks current object/resource access and must enforce its actual I/O deadline. Generic runtime cannot prove that a task service returned the requested task or that its authorization implementation is correct.

Adapter outcomes: `{kind:"SUCCESS",value:record}`, `{kind:"BUSINESS_REJECTION",code:declaredCode}`, or `{kind:"TRANSIENT_FAILURE"}`. Undeclared codes, invalid shapes, extra fields, uncertain/effectful responses or thrown exceptions produce `FAILED` with `ADAPTER_INVALID` or `ADAPTER_EXCEPTION`, never an ordinary result. No automatic retry or handler. READ operations must not claim mutation evidence; uncertainty about a write is outside this profile.

#### Outcomes and trace

Outcome is `{classification,result?,error?,trace}`. Classification is `COMPLETED`, `BUSINESS_REJECTION`, `FAILED`, or `LIMIT_EXCEEDED`. Only COMPLETED carries `result`, exactly the output record. All other classifications carry `error:{code,path}` and no result. Business rejection uses its declared business code; transient read failure uses `ADAPTER_TRANSIENT`; scope denial uses `AUTHORITY_DENIED`; validation uses `INPUT_INVALID`, `HOST_BINDING_INVALID`, or `ADAPTER_INVALID`; expression evaluation errors use `EXPRESSION_FAILED`; limits use `BUDGET_EXCEEDED`. No host exception text is returned. Loader/compile failures are diagnostics, not execution outcomes. Runtime paths use escaped JSON pointers: `/input` (optionally a safe schema field), `/hostBindings` (no protected values), `/steps/<zero-based-index>` or `/return`; invocation-wide failures use the empty pointer. Loader paths are rooted at `/artifact` or `/admission`.

Value-size violations use LIMIT_EXCEEDED/`BUDGET_EXCEEDED` at every typed boundary: business input, protected bindings, evaluated arguments, adapter results and returned output. A string above 256 UTF-8 bytes or a bounded serialized record above 8,192 bytes is a limit violation. Wrong types, missing/null/unknown fields and invalid enum members use the boundary's validation code instead. Validate scalar type/Unicode before its byte bound, then enum membership; validate record shape and fields before its serialized bound. No failure supplies a normal result or permits a later dispatch.

Trace is an ordered array of `{sequence,event,step?,capability?,code?}`; sequence starts at 0. Emit `INVOCATION_START`, then `STEP_START` for each reached step; for CALL emit `CALL_DISPATCHED` only after authorization/validation and immediately before dispatch, then `CALL_SUCCEEDED` on valid success. For successful BIND emit `VALUE_BOUND`. Finish with `RETURNED` then `TERMINATED` on success, otherwise `TERMINATED` with error code. No later step starts after failure. No values, arguments, host bindings, raw CEL, credentials or exception stacks appear. A trusted conformance adapter records exact arguments separately for assertions. Trace capacity is bounded and exceeding it terminates instead of silently dropping audit events; reserve the terminal event slot.

The host consumer receives only an outcome from its selected invocation, validates output type/classification and displays a result only on COMPLETED. Consumption receipt is separately `{status:"DISPLAYED",result:record}` or `{status:"NOT_DISPLAYED",code:string}`. It is not a runtime event or evidence of a native transaction.

<a id="catalog-delta"></a>
## Catalog-bound model and artifact changes

#### Versioned delta

This section records the `/2` delta from `/1`. Effect/3 and effect/4 retain its reference-model and nominal-literal rules; their host API differences are specified above. These identifiers are exact compatibility boundaries:

| Selected profile | DCM format | Artifact format | Existing behavior retained |
| --- | --- | --- | --- |
| `keir-read/2` | `keir-dcm/read-2` | `keir-artifact/read-2` | REQUEST, DISPLAY, READ-only calls and three-argument adapters |
| `keir-effect/2` | `keir-dcm/effect-2` | `keir-artifact/effect-2` | EVENT/AFTER_COMMIT, EVENT_REPORT, guards/terminal handlers, native key channel and simulation-only admission |

Source remains `keir/v0`; trusted compiler configuration must explicitly select the profile. Keep all `/1` type, CEL, canonicalization, descriptor, external pin, outcome and trace rules except the changes stated here, including the explicit nominal-literal addition above. The compiler emits the selected new format/profile pair with its exact supported build identity; both loaders reject mixed pairs and unknown versions. No version inference from optional fields, source constructs or adapter presence.

Both new DCM formats accept READ and WRITE capability definitions in the shared catalog; `read/2` still rejects every WRITE **call**. This permits a read hook to use a catalog that also publishes writes for effect hooks without exposing write authority. All definitions, including unused ones, remain schema/type validated. This does not combine REQUEST and EVENT entrypoint schemas: each format retains its existing lifecycle and consumer restrictions.

Replace required `entrypoint.grants` with optional `entrypoint.capabilityPolicy`. Remove `grants` from the new admission and invocation records; add optional `capabilityPolicy` there too. `grants`, including `{}`, is an unknown field under `/2`, never a synonym. `/1` continues requiring its old maps with omitted IDs meaning zero permission.

For `effect/2`, `effectGrant` becomes optional at entrypoint, admission and invocation. The entrypoint default is one; admission omission inherits the entrypoint ceiling; invocation omission inherits admission. Explicit values are integers 0 or 1 and may only narrow the preceding ceiling. `read/2` has an intrinsic zero-write ceiling and no `effectGrant` field. The [binding contract](../specs/host-model-binding.md) moves required effect `keyBinding` to model registration and adds the trusted invocation approval scope; key/event/assurance obligations remain required. Other required API/DCM fields stay required unless that contract explicitly relocates them. Java must represent absence distinctly from zero; Node must distinguish missing properties from null. Null never means omission.

<a id="catalog-examples"></a>
## Catalog-bound examples, migration and acceptance

#### Examples and rejection obligations

These are read/2 fragments, not complete source documents or `/1` fixtures. The admission/invocation examples and callback diagnostics below describe legacy `/2`, not effect/4. Assume the approved catalog already defines `tasks.get` and `tasks.getSummary` as READ operations accepting agent `taskId` and protected `actorId`, returning a record with `status: TaskStatus`. It also defines the generic `tasks.create` as WRITE. Use existing scalar/nominal types, an input with two TaskId fields, and an output Classification enum.

```yaml
# entrypoints.ClassifyTasks in keir-dcm/read-2
input: ClassifyInput
output: ClassifyResult
trigger: REQUEST
consumer: {id: sample.classification, version: '1', kind: DISPLAY}
# No capabilityPolicy, grants or capability names.
```

Program A calls `tasks.get(input.taskId)` and classifies its result. Program B uses the **same unchanged hook and DCM** with these steps, substituting only source and its independently selected artifact digest:

```yaml
steps:
  - id: first
    call: tasks.getSummary
    args: {taskId: {cel: input.taskId}}
  - id: second
    call: tasks.getSummary
    args: {taskId: {cel: input.otherTaskId}}
return:
  record:
    classification: {cel: 'first.status == "COMPLETED" && second.status == "COMPLETED" ? "DONE" : "PENDING"'}
```

Both compile and run with no advanced filters when authorized and within budgets. They have distinct source/artifact digests; the unchanged DCM digest covers both published operations. Program B does not require a special-purpose host operation combining the reads. The input projection must supply both fields for either program; source cannot broaden input visibility.

An optional hook restriction `capabilityPolicy: {allow: [tasks.get], maxCalls: {tasks.get: 2}}` rejects B but permits two `tasks.get` calls. Admission may narrow that to one; a two-call artifact then fails preparation. Invocation may narrow to zero; the first reached call is denied before its authorization callback/adapter. Removing this restriction is a host-approved DCM revision and recompilation, never a customer source edit. `deny: [tasks.getSummary]` is an alternative opt-in filter, not generated default configuration.

| Case | Required result |
| --- | --- |
| Different published READ; repeated READ; all three policies absent | Accepted if typed, authorized and within aggregate bounds; same hook contract, separate compiled programs. |
| Empty policy/empty quota map vs empty allowlist | First two inherit; empty allowlist rejects every call. Test at hook, admission and invocation boundaries. |
| Unknown/unpublished capability or source-forged catalog/policy/binding | Fail closed; no unauthorized dispatch. An installed adapter alone cannot publish an operation. |
| Known capability denied by current authorization | AUTHORITY_DENIED before its adapter; prior confirmed effects remain recorded. |
| Explicit restrictive policy, quota exhaustion, attempted widening | Preserve upstream restrictions; reject malformed/widened configuration before calls and reached exhausted permission before dispatch. |
| Two reads vs aggregate bound one; fifth potential call | BUDGET_EXCEEDED; named quotas cannot increase the aggregate bound. |
| A WRITE in read/2 or zero effect ceiling; second potential WRITE | Reject before execution, including false guards; no inference from idempotency metadata. |
| PRE_COMMIT placement, real effect admission, changed consumer semantics | Reject under these profiles; no silent lifecycle conversion. |
| Same artifact through Java/Node and packaged hosts | Match typed results, ordered calls/arguments, trace, effects and consumer receipt, including denial/timeout and unchanged receipt rules. |

Diagnostics retain safe paths/text and existing codes. Unknown calls use `CAPABILITY_UNKNOWN`; invalid DCM policy uses `DCM_INVALID`; source calls denied by hook policy or zero effect ceiling use `ENTRY_GRANT_DENIED` at `/source/steps/<index>/call`, with the corresponding loader code at `/artifact/steps/<index>`. Hard call/write overflow uses `BUDGET_EXCEEDED`. Malformed/widened admission uses `ADMISSION_INVALID` under `/admission/capabilityPolicy` (or `/admission/effectGrant`); a well-formed admission too narrow for the plan uses `AUTHORITY_DENIED` there. Invalid/widened invocation uses `HOST_CONTEXT_INVALID` at the empty pointer; reached permission exhaustion uses `AUTHORITY_DENIED` at the step. New policy preflight follows existing structural/authority-before-expression ordering; for a known call check aggregate call overflow, hard WRITE overflow, effect permission, then capability restrictions. Keep `/1` diagnostic behavior intact.

#### Migration and acceptance

Do not delete grants from existing DCMs, patch artifact versions, reinterpret omission or regenerate old goldens. Preserve `/1` compilation, loader rejection behavior, required host APIs and fixtures. `/2` requires explicit profile selection; an unsupported runtime must reject it. A host migrates by approving a new model and explicit compiler selection, recompiling, validating the new artifact in both runtimes, then separately selecting its digest/entrypoint/consumer. Old Prepared handles and active artifacts retain old semantics.

A behavior-preserving migration converts each old grant map into `capabilityPolicy: {allow: <old keys>, maxCalls: <old map>}` at the same hook/admission/invocation layer. An empty old map becomes `allow: []`, not `{}`. Preserve explicit old effect ceilings. Broadening to the corrected generic default is a separate deliberate host decision: remove the migrated restrictions only with host approval and renewed artifact selection. Never let an SDK translate old missing grants into the new default.

New host API entrypoints distinguish the optional-policy context from old required-grant records without breaking existing callers. Node selects semantics from the validated profile, not the presence of `effectGrant`; Java uses new context types and explicit methods. Adapter signatures and consumer wire outcomes need no change: read/2 keeps the read signature, effect/2 keeps the protected original-key/event signature. Revalidate registration version/mode/key compatibility and selected consumer; do not silently reroute old consumers. Scenario formats `keir-scenario/read-2` and `keir-scenario/effect-2` distinguish these defaults from old required-grant fixtures.

Acceptance requires positive/negative fixtures from the table, identical deterministic compiler bytes for both runtimes, all existing `/1` regression, packaged-host examples without default named grants, and matching product assistant guidance. The local [conformance corpus](../../conformance/generic-hooks/README.md) and [testing evidence](../testing.md) record passing checks. This technical regression does not establish real-adapter guarantees or production readiness.

<a id="earlier-follow-up"></a>
## Earlier follow-up walkthrough

Status: **implemented `/2` example with local Java/Node and packaged-host verification**. The exact model/source, six program variants and 28 shared scenarios live in [generic-hook conformance](../../conformance/generic-hooks/README.md). This follows the [generic-hook contract](../specs/generic-hook-capability-contract.md); effects remain simulation-only. The `/1` fixture retains its older input/grant contract for compatibility.

#### Host publishes the hook and catalog

For this example the host defines TaskCompleted input as `{taskId}` and emits it after native commit. This is a host choice, not a restriction that all hosts expose only taskId. Other legitimate payload fields remain host-defined regardless of what a customer program uses. The customer's eligible project and fixed title do not belong in this hook schema.

This complete DCM has no named hook grants or advanced filters. The catalog defines operations independently of this program; approving native adapters and current access remains the host's responsibility. The sample uses simulation-only effect admission.

The input projection and `FollowupResult` output below are this example host's choices, not mandatory Keir payload/result types. Every extension targeting this hook must honor that host-defined result contract.

```json
{
  "format":"keir-dcm/effect-2",
  "domain":{"id":"sample.tasks","version":"2"},
  "types":{
    "TaskId":{"kind":"nominal","base":"String"},
    "ProjectId":{"kind":"nominal","base":"String"},
    "UserId":{"kind":"nominal","base":"String"},
    "EventId":{"kind":"nominal","base":"String"},
    "TaskStatus":{"kind":"enum","values":["OPEN","COMPLETED"]},
    "FollowupStatus":{"kind":"enum","values":["CREATED","NO_OP"]},
    "TaskCompletedInput":{"kind":"record","fields":{"taskId":"TaskId"}},
    "FollowupResult":{"kind":"record","fields":{"status":"FollowupStatus"}},
    "Task":{"kind":"record","fields":{"id":"TaskId","projectId":"ProjectId","status":"TaskStatus"}},
    "CreatedTask":{"kind":"record","fields":{"id":"TaskId"}}
  },
  "capabilities":{
    "tasks.get":{"version":"1","parameters":{"taskId":{"type":"TaskId","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},"result":"Task","effect":"READ","errors":["TASK_NOT_FOUND","ACCESS_DENIED"]},
    "tasks.create":{"version":"1","parameters":{"projectId":{"type":"ProjectId","binding":"agent"},"title":{"type":"String","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},"result":"CreatedTask","effect":"WRITE","errors":["CREATE_REJECTED","ACCESS_DENIED"]}
  },
  "entrypoints":{
    "TaskCompleted":{"input":"TaskCompletedInput","output":"FollowupResult","trigger":"EVENT","placement":"AFTER_COMMIT","consumer":{"id":"sample.followup.report","version":"1","kind":"EVENT_REPORT"},"event":{"idType":"EventId","sourceType":"TaskId"}}
  },
  "hostKey":{"source":"requestKey","type":"sample.RequestUuid/1"},
  "assurance":"SIMULATION_ONLY"
}
```

Effect ceiling omission means one under this format; it never permits a second potential WRITE. Catalog declarations and simulation cannot certify real native behavior.

`tasks.create` is ordinary task creation. The extension's condition, selected project and title make the new task a follow-up in this scenario. There is no special follow-up service operation, implicit parent link or `sourceTaskId` creation argument. If the host exposes a generic relationship field, the program must supply it explicitly. Trigger provenance remains in protected event context and does not become a customer-controlled task field.

#### Customer authors the program

The customer chooses project `project1` and title `Follow up` inside this source. The ProjectId annotation is supported under explicit `/2` compilation; `/1` retains its earlier literal restrictions. The fingerprint below is the canonical digest of the DCM above, not an authentication credential.

```yaml
language: keir/v0
domain: {id: sample.tasks, version: '2', fingerprint: 'sha256:68b72e4da41aa006a6a8182b5b8a6a4363a6734688da3c1e0c3807c0aff4ac04'}
entrypoint: TaskCompleted
name: follow-up-in-selected-project
steps:
  - id: eligibleProject
    let: {literal: 'project1', type: ProjectId}
  - id: source
    call: tasks.get
    args:
      taskId: {cel: input.taskId}
  - id: eligible
    let: {cel: 'source.status == "COMPLETED" && source.projectId == eligibleProject'}
  - id: created
    call: tasks.create
    when: eligible
    args:
      projectId: {cel: eligibleProject}
      title: {literal: 'Follow up'}
return:
  record:
    status: {cel: 'eligible ? "CREATED" : "NO_OP"'}
```

There is no read of the guarded `created` result. The ordinary return is reached only after successful calls; rejection or uncertainty cannot be relabeled CREATED. Four steps and two potential calls fit existing aggregate ceilings. The input task and protected event source must refer to the same native task under the host contract; the read adapter must return that requested task or reject. Project equality does not replace authorization.

#### Compile, approve and prepare

| Boundary | Concrete input/output |
| --- | --- |
| Compile input | The complete source above, separately trusted DCM bytes above, and explicit `Environment.effect2()` or CLI `--profile=keir-effect/2`. No settings/configuration argument. |
| Compiler checks | Strict shape, pins, nominal constant/dataflow, protected parameter exclusion, catalog/effect/policy constraints, bounded CEL and aggregate limits. No artifact on failure. |
| Compiler output | One canonical `keir-artifact/effect-2` artifact with profile `keir-effect/2`, source identity, exact `modelRef`, checked expressions, four lowered steps and return. The [reference envelope](#exact-reference-artifact) omits embedded DCM/descriptors; descriptors derive from the bound model. `eligibleProject` lowers to `{literal:"project1",type:"ProjectId"}`; title remains `{literal:"Follow up",type:"String"}`. See the exact [generated artifact](../../conformance/generic-hooks/effect/artifact.json). |
| Host selection | Review intended behavior, then retain the exact produced artifact digest independently with expected DCM digest, entrypoint TaskCompleted, consumer `sample.followup.report` version 1 and compiler/environment pins. A self-reported digest in customer data cannot authorize activation. |
| Binding | Call `bindModel(dcmBytes, approval, invokers)` once and check its diagnostic before using `boundModel`. Approval selects host scope, exact modelRef, profile/environment, `mode:simulation` and native keyBinding. Register both catalog capabilities, not just whichever a particular extension calls. |
| Preparation | Call `prepareProgram(artifactBytes, boundModel, admission)` and check its diagnostic before using `prepared`. External pins and simulation mode must match; no invoker map or keyBinding override. Omitted policy, effect ceiling and limits inherit safe defaults. These host-owned records are never customer authority. |

Both adapter registrations use `{version:"1",mode:"simulation",keyType:"sample.RequestUuid/1",invoke:<host function>}`. Preparation validates metadata and the artifact without calling adapters or authorization. Both loaders reconstruct and validate the typed literal environment instead of trusting forged annotations. The [host sketches](../specs/host-model-binding.md#equivalent-host-sketches) show the Java/Node wrappers; complete runnable integrations are linked there.

The exact modelRef is `{domainId:"sample.tasks",domainVersion:"2",format:"keir-dcm/effect-2",digest:"sha256:68b72e4da41aa006a6a8182b5b8a6a4363a6734688da3c1e0c3807c0aff4ac04",descriptorRecipe:"keir-record-descriptors/1"}`. See [equivalent Java/Node host sketches](../specs/host-model-binding.md#equivalent-host-sketches). The host-held scope and invokers are not serialized into this reference.

#### Host invokes with unchanged facts and protected context

Business input is exactly `{"taskId":"task1"}`. Trusted invocation supplies:

- `scope` matching the bound model's host approval namespace, selected from authenticated host context. The current authorizer checks that binding remains permitted.

- `bindings: {actorId:"user1"}` from authenticated host scope, not source.
- `deadline: {clock:<host monotonic function>, expiresAtMs:<same-domain expiry>}` and `authorize:<current invocation/per-call function>`.
- Original immutable `requestKey` of the registered native key type, and `event: {id:"event1",source:"task1"}` from the host event. No key conversion or source access.
- No `grants`, configuration map, customer-selected project or title. Optional policy/effect ceilings are omitted; this does not omit current authorization.

The host selects the approved artifact for this invocation. The runtime executes it against those facts/context; it does not re-author source or fetch implicit settings. Entry authorization precedes execution. Each reached call receives separate agent/protected arguments, remaining deadline and protected key/event context. The host authorizer and native adapter remain responsible for current resource access and invariants; a typed constant can still refer to a nonexistent, wrong-scope or revoked project.

With read result `{id:"task1",projectId:"project1",status:"COMPLETED"}`, the generic create receives agent `{projectId:"project1",title:"Follow up"}` and protected `{actorId:"user1"}`; original key/event information remains in the separate native context. A valid SUCCESS/CONFIRMED response with `{id:"task2"}` permits COMPLETED/CREATED with a CONFIRMED receipt. A different project or OPEN status skips the write and returns COMPLETED/NO_OP with no effect. An inaccessible resource rejects; a dispatched write with unknown commit terminates UNCERTAIN, no normal result. EVENT_REPORT preserves these distinctions and cannot undo the native event.

#### Run the example

After the [build/package setup](../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts), run either host from the repository root:

```sh
node examples/node-host/generic.mjs conformance/generic-hooks/effect
java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.GenericHost conformance/generic-hooks/effect
```

Each emits the same nineteen verified rows, including creation, no-op conditions, changed project/title constants, denial, rejection and uncertain write responses. Use the `read` directory for the nine read scenarios, including repeated and alternate reads. The [combined verifier](../../conformance/generic-hooks/README.md) checks complete outcomes, ordered calls, traces, effects and consumption.

#### Acceptance obligations

Change only the source literal from `project1` to `project2`: DCM bytes/digest, hook schema, host code, input and protected context remain unchanged. Canonical source and artifact digests change; normal review and selection of the new artifact are required. With the original project1 task, the new program skips the write. Changing only the title similarly changes source/artifact, not the hook. Compare Java and Node executing each exact compiled artifact, not independently regenerated variants.

Reject wrong nominal types, unknown/non-nominal annotations, dynamic casts, bounds overflow, forged literal metadata, protected argument overrides, unauthorized capabilities and a second potential WRITE. Keep `/1` fixture bytes and literal rejection behavior unchanged. Regression covers these obligations and current authorization after source review; see [measured evidence](../testing.md). Native authorization, idempotency and production readiness still require separate host proof.

<a id="original-write-formats"></a>
## Original simulated-write source and artifact

<a id="versions-and-inherited-rules"></a>
#### Implementation reference: inherited source and format rules

This compatibility section preserves the effect/1 identity and embedded-model rules. Current single-write behavior uses the version boundaries above and the [artifact contract](../specs/compiler-artifact.md#effect4-identity). Exact profile values remain in code examples and compatibility references.

Select `keir-effect/1` in the trusted compiler environment; retain source family `keir/v0`. Require DCM format `keir-dcm/effect-1` and artifact format `keir-artifact/effect-1`. These identifiers are profile compatibility boundaries, not release versions. Use the exact supported compiler build identity selected by the host; do not relax existing compiler/environment pins. CEL/protobuf package pins, scalar types, operators, descriptor recipe, canonical encoding and external artifact/DCM digests are unchanged. Reject mixed profile/format pairs; never infer a profile from source constructs or upgrade an old artifact.

The [read contracts](../specs/README.md) apply except for the explicit additions below. No collections, optional values, numeric business values, time, retries, recovery, multi-effect namespace or general branch language is added. All [existing ceilings](../specs/runtime.md#limits) remain: raw source and raw DCM each <=65,536 bytes, canonical embedded DCM <=65,536 bytes, artifact <=1,048,576 bytes, eight steps, four potential calls, 512 total AST nodes and 64 trace events. Canonical DCM overflow fails compilation with `BUDGET_EXCEEDED` at `/dcm`, preparation at `/artifact/dcm`; no artifact/prepared handle. Add a ceiling of **one potential WRITE call**, counting guarded calls even with constant-false guards. No mutual-exclusivity analysis. All handler expressions count toward aggregate AST/byte limits.

<a id="original-write-model"></a>
## Original simulated-write model and key schema

The exact root and entrypoint forms in this section are effect/1. Effect/2–4 retain the hostKey/event type rules and post-commit lifecycle, while replacing required grants with optional hook restrictions as specified in the [generic-hook contract](../specs/generic-hook-capability-contract.md#effect4-contract-and-version-boundaries). Effect/3 and effect/4 use their respective assurance values in the [compatibility table](implementation-reference.md#contract-compatibility).

The root is exactly `{format,domain,types,capabilities,entrypoints,hostKey,assurance}`. `assurance` must be `SIMULATION_ONLY` for effect/1; it is a restriction, not a certification. `hostKey` is exactly `{source,type}`: source must be `requestKey`; type is a nonempty string bounded to 256 UTF-8 bytes identifying an exact host-native key contract. It is **not** a business/CEL type and cannot be referenced as a key contract by source parameters, record fields, literals or descriptors. Its label belongs to a separate namespace: equality with `String`, `Boolean` or a declared business type name is permitted and creates no type alias, conversion or access to the key. Business references resolve only against built-ins/DCM types, never `hostKey.type`. One binding only, shared by all calls in this invocation.

Capability fields are unchanged except `effect` accepts `READ` or `WRITE`. The root key and entrypoint event context are supplied to both kinds through the protected adapter channel below, never merged into `parameters`. Existing scalar `binding:host` parameters remain supported. WRITE responses must obey the effect outcomes below. All business error codes remain stable declared strings; this profile caps each capability's errors at 16.

Entrypoint is exactly `{input,output,trigger,placement,consumer,grants,effectGrant,event}`. Require `trigger:EVENT`, `placement:AFTER_COMMIT`, and consumer `{id,version,kind:EVENT_REPORT}`. `effectGrant` is integer 0 or 1. `event` is exactly `{idType,sourceType}`, both naming declared nominal String types. Per-capability grants remain positive integer call ceilings; omitted IDs grant zero. A zero effect grant admits no WRITE calls, even guarded ones. No pre-commit entrypoint is supported by this profile; reject `BEFORE_COMMIT` or other placements rather than enabling a write through the existence of a key. The read profile continues rejecting WRITE capabilities.

<a id="original-write-host-api"></a>
## Original simulated-write host API

This section preserves the legacy effect/1 API. Later compatibility profiles move key binding into model binding and add qualification. The current API uses [BoundEffect APIs](../specs/host-model-binding.md#api-shapes) with no key validator or authorizer callback and adds the absolute deadline to adapter context. The [original-key identity rule](../specs/real-single-effect-admission.md#effect-outcomes-and-key-identity) applies to every version. See the [implementation reference](implementation-reference.md) for retained historical contract details.

For this profile only, admission extends the existing closed record with required `effectGrant` and `keyBinding`. `effectGrant` is 0 or 1, may only narrow the selected entrypoint, and must cover the artifact's conservative WRITE count. `keyBinding` is the host-only closed `{type,validate}` registration: type exactly matches `dcm.hostKey.type`; validate is a synchronous host function accepting one opaque value and returning Boolean. Snapshot its function reference and metadata during preparation. Do not invoke it at prepare time. Each adapter registration is exactly `{version,mode,keyType,invoke}`; `keyType` must equal `dcm.hostKey.type` for both READ and WRITE, otherwise preparation fails with `ADMISSION_INVALID` at `/admission/adapters`. Version, mode and keyBinding mismatches also fail preparation with `ADMISSION_INVALID` under `/admission`. All adapters and admission must have `mode:simulation`; reject `real` for this profile, even with supplied assertions or certificates. Real effect admission needs a separately reviewed host-evidence contract and compatibility change.

Invocation extends `{bindings,deadline,authorize,grants}` with required `{effectGrant,requestKey,event}`. Fresh effectGrant may only narrow admission (0 or 1); omission, widening or invalid form is `HOST_CONTEXT_INVALID`. Validate all invocation fields before any adapter call, including when the write will be skipped. `event` is exactly `{id,source}`, validated against the selected event nominal types, then copied as immutable scalar data. `requestKey` must be non-null and accepted by the snapshotted key validator; false, throw or non-Boolean result is `HOST_CONTEXT_INVALID`. No raw validation exception escapes. The host validator must be bounded, pure, and perform no I/O; deadline checks bracket it, with no hard preemption claim.

The runtime does not parse, stringify, hash, copy, freeze recursively, reconstruct or otherwise transform the key. The host supplies an immutable key value and owns its type, format, logical uniqueness, stability, collision scope and any wire serialization. In-process forwarding preserves the original value/reference to **both read and write adapters**; no execution/step/event ID substitutes for it. Across processes the host, outside Keir, owns lossless reconstruction of the same typed logical key. Prepared contains the validator, never an invocation key/event. No key/event values enter CEL, artifacts, public trace or error text.

The effect/1 adapter signature is `invoke(agentArgs, protectedArgs, remainingMs, hostContext)`, where hostContext is exactly `{requestKey,event}`. Snapshot/freeze the container and validated event scalars without traversing the opaque key. Existing read-profile adapters retain their three-argument contract. Java may expose the extra context as a small typed record and a separate adapter interface; no change to the old public invocation/adapter contract is required. Generic runtime must not route the key through scalar `Artifact.value` or protobuf conversion.

The sample host's key type is `sample.RequestUuid/1`: Java uses `java.util.UUID`; Node uses a host-created immutable branded value holding the same UUID logical value. A plain string or unbranded object fails the host validator. Only the host fixture loader constructs native values from test data. This is a sample host representation, not a new Keir scalar or a required UUID library. Tests assert that both adapters receive their invocation's exact original object/value and that neither runtime invokes conversion methods.

Before a reached dispatch: check remaining step/call budgets, current capability grant, current effectGrant for WRITE, arguments/protected bindings, current authorization, deadline and trace reserve. Invocation and per-call authorization request shapes remain unchanged; the host callback captures event/key/resource context. A zero fresh effectGrant is a valid narrowing: a reached WRITE fails `AUTHORITY_DENIED` before dispatch; a skipped WRITE performs no dynamic grant consumption. Static admission still counts all potential writes. Check effect permission before the per-call authorization callback. No retries, durable ledger or recovery API is introduced.

<a id="earlier-java-apis"></a>
## Earlier Java runtime APIs

<a id="legacy-2-apis"></a>
#### Implementation reference for earlier APIs

##### Catalog-bound read and simulated write

The explicit `/2` APIs bind a complete host-approved catalog once, then prepare reference-only programs against that immutable binding. Call `KeirRuntime.bindModel(dcmBytes, approval, invokers)` and check its `ModelBinding` before using `boundModel()`. Approval supplies the exact model reference, scope, profile, environment and mode; effect approval also supplies the typed `KeyBinding`. Register every catalog capability, including unused or policy-denied operations. Binding snapshots metadata and callable references without invoking them.

Call `prepareProgram(artifactBytes, boundModel, admission)` with independent artifact/model pins, entrypoint, consumer, compiler, environment and mode. It directly validates the `/2` envelope and checked plan against reusable model descriptors. Optional `capabilityPolicy` and limits only narrow the hook; omitted effect ceilings inherit the default maximum of one potential WRITE. `/2` admission forbids legacy `grants` and preparation-time invokers or key bindings.

For fresh execution, use `invokeProgram` with `Invocation2` for read/2, or `invokeEffectProgram` with `EffectInvocation2` for effect/2. Supply matching scope, current protected bindings, deadline, authorizer and `Optional.empty()` for an omitted policy. Effects additionally require the original native key, event and `OptionalLong.empty()` to inherit the admitted effect ceiling. Null Optional objects are invalid. Invocation methods reject handles from the other profile generation. The execution engine retains authorization, limits and response/timeout receipts.

The [legacy /3 qualification protocol](../specs/real-single-effect-admission.md) extends the legacy binding flow for `keir-effect/3`.

<a id="legacy-1-apis"></a>
##### Original read and simulated write

`keir-runtime-java` executes the shared `keir-read/1` artifact with JDK 21 and CEL Java 0.14.0. It has no dependency on the Keir compiler, CEL compiler/parser, ANTLR or YAML. Compilation belongs outside execution; see the independent [Java host](../../examples/java-host/README.md) for a packaged simulation.

The separate `keir-effect/1` API implements the [single-effect contract](../specs/single-effect.md) with simulation registrations only. Legacy and generic effect scenarios pass shared Java/Node packaged-host verification; the packaged read entrypoint remains compatible.

`KeirRuntime.prepare(bytes, admission, adapters)` returns a `Preparation` containing either an immutable `Prepared` handle or a safe `Diagnostic`. Admission supplies an externally trusted whole-artifact digest, DCM digest, entrypoint, consumer, compiler and environment pins, adapter mode, grants and optional narrowing limits. The whole-byte digest is checked before JSON parsing. Canonical JSON, the complete DCM and plan, generated descriptors, checked CEL metadata and the restricted expression profile are checked before CEL runtime preparation. Preparation invokes no host callback.

`KeirRuntime.invoke(prepared, businessInput, invocation)` returns a `CompletionStage<Outcome>`. Construct `Invocation` in trusted host code from fresh protected bindings, `Deadline(LongSupplier clock, long expiresAtMs)`, an asynchronous `Authorizer` and fresh grants. Clock readings use nondecreasing nonnegative integer milliseconds in the same arbitrary monotonic domain as expiry, bounded by 2^53-1. Omitted capability grants mean zero; positive counts may only narrow admission. Invocation data is validated and copied before asynchronous authorization.

Authorization receives `{entrypoint}`, then `{entrypoint,capability,version}` before each call. An `Adapter` registers an exact version, simulation/real mode and asynchronous function receiving separate immutable agent and protected argument maps plus remaining milliseconds. It returns the closed SUCCESS, BUSINESS_REJECTION or TRANSIENT_FAILURE records from the [runtime contract](../specs/runtime.md). Host adapters retain responsibility for resource access and actual I/O deadlines.

`Outcome.toMap()` exposes a deeply immutable JSON-compatible record; `toJson()` serializes its canonical bytes. In the read profile only COMPLETED includes a result. Failures carry safe code/path pairs; traces expose event order and identifiers, without argument values or exception text. Validate results again in the selected host consumer.

For effects use `prepareEffect(bytes, admission, Map<String,EffectAdapter>)` and `invokeEffect(prepared, businessInput, EffectInvocation)`. These named methods preserve the original read adapter, invocation and preparation signatures. Preparation requires the effect artifact/model formats explicitly and independently checks guards, pre-call handler environments, checked CEL, one potential write and static grants. It invokes no validator, authorizer or adapter.

Effect admission extends the existing map with `effectGrant` (integer 0 or 1) and a typed `new KeyBinding(type, validator)` registration. Each immutable `EffectAdapter(version, mode, keyType, function)` must match the DCM and use `simulation`; real mode is rejected. The validator's `Object validate(Object key)` must return a Boolean and must be bounded, pure and perform no I/O. Preparation retains its function reference; each invocation supplies its own key.

`EffectInvocation` adds `Long effectGrant`, `Object requestKey` and `Map<String,Object> event` to the read invocation fields. The grant may only narrow admission; zero denies a reached write before its authorization callback. The event is exactly `{id,source}`, with nominal String values. Invocation validates bindings, grants, event and a non-null accepted key before the first adapter call, including on no-op paths. The sample validator accepts `java.util.UUID`; the runtime makes no assumption about that representation.

The effect adapter receives `(agentArgs, protectedArgs, remainingMs, HostContext)`. `HostContext` contains the invocation's **original key reference** and an immutable event snapshot. The runtime never traverses, clones, converts, hashes or compares the key. The host owns its immutability, uniqueness and wire representation. Neither key nor event values enter CEL, artifacts or traces. Both read and write adapters in the effect profile use this fourth argument; read-profile adapters keep their three arguments.

An effect outcome always adds immutable `effects()` / `effects` containing zero or one `{step,capability,state}` receipts. Skipped and undispatched writes have none. A dispatched write starts UNKNOWN; a winning closed response establishes CONFIRMED or NONE before completion clock and value checks. Timeout winners ignore late responses. Failures retain evidence; an outstanding UNKNOWN forces UNCERTAIN with the original safe failure code. Confirmation is adapter evidence, not rollback or crash durability.

Business rejection is terminal. A matching typed handler may add a result while retaining BUSINESS_REJECTION and its original error. Handler failure adds that rejection as the nonrecursive `error.cause`. False guards skip arguments, per-call authorization and dispatch. Trace reservation prevents writes unless intent, dispatch, settlement, possible success and termination can all fit.
