# Generic hooks and capability composition

Status: **reviewed target contract; not implemented or accepted for execution**. The [owning feature](../../roadmap/features/generic-hook-capability-contract.md) tracks later implementation. Existing `keir-read/1` and `keir-effect/1` still require per-capability grants. Their passing tests do not qualify this correction. [Rationale and migration](../internal/generic-hook-capability-contract-design.md) identify affected components and remaining gates.

## Responsibility boundaries

**Packaging decision (2026-10-31):** upcoming `/2` artifacts reference an exact immutable host-approved DCM rather than embedding it. The proposed [host model binding](host-model-binding.md) ties the complete catalog and compatible callable invokers into one reusable handle, then prepares programs against it without a separate invoker map. It specifies the reference envelope, descriptor recipe and lifecycle rules for review before implementation. Model/descriptor preparation may be shared; program validation and fresh invocation authority remain separate. This develops the [adopted model-sharing direction](../internal/artifact-model-sharing-evaluation.md); existing `/1` bytes and loaders remain unchanged.

A host publishes a scoped, approved capability catalog. A hook defines its input projection, allowed output and consumer, trigger, lifecycle placement and effect ceiling. A customer extension selects and composes catalog operations; the host need not anticipate its call list. A required map naming the operations of a sample program is not the generic hook contract.

Capabilities expose reusable host-domain operations rather than pre-implementing a customer's customization. In the task example, the host publishes `tasks.create` with project/title arguments; the extension decides when and why to create a follow-up. The adapter must not hide that project's filter, the title or the follow-up rule. Native domain-specific operations can still be appropriate when they express an actual host operation or atomic guarantee; Keir does not require every capability to be CRUD.

| Boundary | Authority and obligation |
| --- | --- |
| Catalog publication and discovery | Host selects the complete effective DCM for the authenticated customer scope. Publication controls visibility; discovery exposes only that approved scope. Neither descriptions nor discovery results authorize a native operation. |
| Hook definition | Host defines typed input/output, consumer semantics and lifecycle/effect constraints. No capability names or counts are required by default. |
| Authored composition | Source uses static calls to published capabilities and typed dataflow. It cannot publish operations, supply policy, select credentials or override protected bindings. |
| Compilation and artifact selection | Trusted compiler environment selects the profile and DCM. Compilation checks composition; host independently approves/selects the exact artifact, entrypoint and consumer. |
| Admission | Host binds the complete approved DCM and invokers once, prepares against that handle with artifact/compiler/environment/hook pins, and may narrow policy and resource limits. Successful preparation is not current user authorization. |
| Invocation and dispatch | Fresh host context and required authorization check the invocation and each reached call. Adapters enforce actual resource access, invariants and I/O deadlines. Catalog visibility is never proof of these checks. |
| Runtime | Execute the selected artifact against unchanged host payload and fresh protected context; enforce types, policies, bounds and honest outcomes. Do not re-author customer logic, infer customer choices or load implicit configuration. |
| Host integration assistant | Help the host draft/review hook payload, catalog, native adapters, authorization and consumption. Only host-owner approval publishes these contracts; the assistant cannot certify guarantees or invent authority. |
| Customer authoring agent | Help the customer author logic, constants and capability composition against the published contract. It may propose source/scenarios and request approval, but cannot change the hook/catalog, protected values, permissions or activation on its own. |

The [complete TaskCompleted walkthrough](generic-hook-example.md) applies this ownership model from host DCM and customer source through compilation, artifact selection and invocation. The source-authored project/title are independent of hook payload; the required nominal-literal change is explicitly proposed rather than claimed implemented.

The catalog is a host-selected snapshot, not a wildcard over installed adapters or future publications. Adding a catalog operation changes its digest; using that changed snapshot requires recompilation and host reselection. Existing programs may continue using their exact earlier approved snapshot while its dependencies remain available. Choosing a different **already-published** operation or repeating a read does not require changing the hook. Host-specific tenant selection, authenticated transport, endpoint publication and activation remain host responsibilities; this correction adds no policy service.

## Capability invocation across services

Treat internal service-to-service operations, including HTTP or RPC calls, as the normal integration case. A capability describes a logical operation, not a requirement that its business implementation runs inside the runner process. The registered adapter is the runner-facing dispatch bridge; an in-process service method is another supported implementation choice.

The host supplies a compatible client/adapter for each registered capability version. That bridge maps validated arguments to the remote request and maps responses to declared results, rejections and effect evidence. Service routing, credentials, trusted identity propagation, connection management and transport details remain host-owned integration concerns, outside customer source and compiled artifacts. The DCM supplies the logical typed contract; a model reference does not itself locate or authorize a service. This does not require a built-in HTTP client, service discovery system or generated transport bindings in Keir.

Remote calls retain the same current authorization, resource checks and deadline obligations. A local timeout or cancellation does not establish that a remote operation stopped. Losing a response after a possible WRITE must preserve UNKNOWN/UNCERTAIN unless the native contract establishes a definite outcome; do not add automatic retries. Shared model preparation does not cache user credentials or current permissions. The proposed [dispatch semantics](host-model-binding.md#invocation-and-remote-dispatch) preserve the existing callable signatures and native-key contract.

## Event facts, extension choices and protected authority

The host defines the hook payload independently of any one extension. Preserve its legitimate fields even when a sample does not use them. Do not require the host to add customer-selected project/title fields merely to make one follow-up program work.

| Origin | Example and rule |
| --- | --- |
| Host-defined payload and native facts | `input.taskId` identifies the event task; the host may also legitimately project its actual project or other approved facts. Source reads these facts, but cannot reinterpret them as the customer's choice. |
| Authored program constants | The customer selects a particular eligible project and fixed title inside the program. They compile into the artifact. Another source requires explicit program logic, such as an approved capability, not an implicit settings/configuration channel. |
| Protected authority/context | Authenticated actor/tenant scope, credentials, event provenance and original native key stay host-owned and outside source/CEL. A business ID constant grants none of this authority. |

**Current evidence:** the `/1` sample declares `FollowupInput={taskId,projectId,title}` and injects those values through `conformance/single-effect/scenarios.json` `given.input`. Its source compares `source.projectId == input.projectId` and forwards `input.title`. This proves fixture-driven execution only; it does not implement authored project constants. Do not delete legitimate host payload fields, reinterpret the fixture, or introduce a configuration framework to repair this language gap.

**Observed language limitation:** `Expressions.literal` in `compiler-java/src/main/java/dev/keir/compiler/Expressions.java` assigns a string literal type String; `assign` rejects String-to-nominal conversion with `NOMINAL_MISMATCH`. `KeirCompiler.java` accepts only `{literal}` or `{cel}` source bindings. Thus `/1` can author a fixed String title but cannot directly author a ProjectId constant. The corrected target must address literal typing rather than move the customer's choice into hook input.

**Proposed minimal `/2` literal addition:** permit a binding exactly `{literal: string, type: nominalTypeName}` at existing binding positions, including BIND. The type must resolve to a host-published nominal String type. No arbitrary annotation, enum/built-in/record annotation, host-key contract type, null/numeric value, dynamic expression or String-variable cast is admitted. The annotation supplies business typing, never evidence of existence, ownership or access. Keep unannotated literal rules unchanged; in particular, no implicit String-to-nominal coercion. Apply existing Unicode and 256-byte string limits. Invalid shape/unknown fields retain structural diagnostics; unknown/non-nominal type or incompatible nominal assignment uses `NOMINAL_MISMATCH` at the binding/type or literal path as applicable. Host-bound argument overrides still fail preflight regardless of annotation.

A nominal literal binds the declared nominal type in the expression environment; same-type comparisons and arguments are allowed, other nominal types remain incompatible. Lower to the existing artifact `{literal,type}` representation; no new opcode, configuration section, compiler input or CEL variable is needed. Both loaders must validate declared nominal literal type/value compatibility under `/2`, while `/1` retains its rejection behavior. The compiler must preserve nominal provenance through BIND/checked metadata and both runtimes must reject forged type metadata. Literal values are in source/artifacts and must not contain credentials or native keys; trace/error redaction remains unchanged.

Resolve the annotation only in the DCM business `types` namespace. An equal host-key metadata label neither grants native-key access nor invalidates a legitimate business nominal type. An unknown/non-nominal annotation fails with `NOMINAL_MISMATCH` at the binding's `/type`; a valid nominal annotation incompatible with its expected nominal type fails at `/literal`.

Example fragment for the corrected effect profile, assuming the host payload includes `taskId` and the catalog already publishes the shown types/capabilities:

```yaml
steps:
  - id: eligibleProject
    let: {literal: 'project1', type: ProjectId}
  - id: source
    call: tasks.get
    args: {taskId: {cel: input.taskId}}
  - id: eligible
    let: {cel: 'source.status == "COMPLETED" && source.projectId == eligibleProject'}
  - id: created
    call: tasks.create
    when: eligible
    args:
      projectId: {cel: eligibleProject}
      title: {literal: 'Follow up'}
```

This is a proposed fragment, not executable `/1` syntax or a complete source document. It uses the same host payload for customers choosing different project/title constants. Changing a constant changes canonical source and artifact digests and requires ordinary compilation, review and host selection of that revision. No separate configuration revision, persistence, binding service or invocation override is introduced. An explicitly called capability may supply a value instead, under normal catalog, type, authorization and budget rules.

Project equality is business logic, not authorization. Authoring tools may help resolve a project name to its ID, but source still contains a customer-authored reference. The host/adapter must enforce current access and native invariants, including nonexistent, wrong-scope or revoked projects. Unknown effect status and prior confirmed effects retain the existing semantics. The explicit nominal-literal syntax requires review and cross-runtime conformance before implementation; the product default of authored constants is not an open configuration-model choice.

## Versioned delta

Proposed identifiers are exact compatibility boundaries, not currently accepted inputs:

| Selected profile | DCM format | Artifact format | Existing behavior retained |
| --- | --- | --- | --- |
| `keir-read/2` | `keir-dcm/read-2` | `keir-artifact/read-2` | REQUEST, DISPLAY, READ-only calls and three-argument adapters |
| `keir-effect/2` | `keir-dcm/effect-2` | `keir-artifact/effect-2` | EVENT/AFTER_COMMIT, EVENT_REPORT, guards/terminal handlers, native key channel and simulation-only admission |

Source remains `keir/v0`; trusted compiler configuration must explicitly select the profile. Keep all `/1` type, CEL, canonicalization, descriptor, external pin, outcome and trace rules except the changes stated here, including the explicit nominal-literal addition above. The compiler emits the selected new format/profile pair with its exact supported build identity; both loaders reject mixed pairs and unknown versions. No version inference from optional fields, source constructs or adapter presence.

Both new DCM formats accept READ and WRITE capability definitions in the shared catalog; `read/2` still rejects every WRITE **call**. This permits a read hook to use a catalog that also publishes writes for effect hooks without exposing write authority. All definitions, including unused ones, remain schema/type validated. This does not combine REQUEST and EVENT entrypoint schemas: each format retains its existing lifecycle and consumer restrictions.

Replace required `entrypoint.grants` with optional `entrypoint.capabilityPolicy`. Remove `grants` from the new admission and invocation records; add optional `capabilityPolicy` there too. `grants`, including `{}`, is an unknown field under `/2`, never a synonym. `/1` continues requiring its old maps with omitted IDs meaning zero permission.

For `effect/2`, `effectGrant` becomes optional at entrypoint, admission and invocation. The entrypoint default is one; admission omission inherits the entrypoint ceiling; invocation omission inherits admission. Explicit values are integers 0 or 1 and may only narrow the preceding ceiling. `read/2` has an intrinsic zero-write ceiling and no `effectGrant` field. The [binding contract](host-model-binding.md) moves required effect `keyBinding` to model registration and adds the trusted invocation approval scope; key/event/assurance obligations remain required. Other required API/DCM fields stay required unless that contract explicitly relocates them. Java must represent absence distinctly from zero; Node must distinguish missing properties from null. Null never means omission.

## Optional restrictive controls

`capabilityPolicy` is a closed record with optional `allow`, `deny`, `maxCalls`. It is advanced host configuration, not required setup and never source input. Unknown fields, nulls, invalid counts, unknown catalog IDs and duplicate list IDs fail validation. Lists/maps are bounded by the catalog ceiling (eight capabilities); IDs use existing capability identifier rules. No pattern matching, wildcard, inheritance registry or new expression language is added.

| Field or value | Exact meaning |
| --- | --- |
| Absent policy or `{}` | No additional restriction; inherit all earlier restrictions. At the DCM hook boundary this starts with the exact approved catalog. |
| Absent `allow` | No additional allowlist. |
| `allow: []` | No calls permitted. |
| Nonempty `allow` | Only those IDs remain eligible; IDs must be eligible under preceding policy. |
| Absent `deny` or `deny: []` | No additional denials. |
| Nonempty `deny` | Remove those catalog IDs; denial wins when an ID also appears in `allow`. Earlier denials cannot be removed. |
| Absent `maxCalls` or `maxCalls: {}` | No additional per-capability quota; earlier quotas still apply. |
| `maxCalls: {id: n}` | Narrow that capability to `n` potential/static and reached/dynamic calls, integer 0..4. Zero denies calls; omitted IDs do not become denied. |

Apply constraints in order: profile/catalog, hook, admission, fresh invocation. Retain inherited allow/deny constraints and quotas; omission never resets them. An explicit allowlist must be a subset of preceding eligible IDs. An explicit quota must not exceed that ID's preceding ceiling (zero if denied, otherwise inherited quota or profile call ceiling four). Reject attempted widening rather than silently accepting a broader configuration and clipping it. A denylist may repeat an already-denied catalog ID. Within one layer, apply `allow` and `deny` before quotas; a positive quota for a denied ID is invalid. The source cannot supply any layer or substitute its own DCM.

Per-capability quotas are independent of aggregate resource budgets: a quota of four does not permit four calls when the admitted aggregate call limit is two. The existing `admission.limits` contract remains narrowing-only; omitted fields retain profile ceilings (eight steps, four potential calls, 512 aggregate AST nodes, 64 trace events, and unchanged byte/value limits). No required hook-specific call budget or named quota is introduced. Repeated READs consume the aggregate budget normally; they are not deduplicated, cached or assumed to return the same state.

Compiler and loaders conservatively count all potential calls, including false guards and mutually exclusive branches, against hook policy, aggregate limits and effect ceilings. Preparation also requires the artifact's potential counts to fit admission restrictions. Invocation validates the fresh policy before any adapter call but applies its narrower permissions/counts to reached dispatches: a false guard consumes neither a call nor a quota and performs no per-call authorization. This preserves safe no-op execution under a narrowed invocation, as with a zero fresh effect ceiling.

The mandatory invocation authorization request `{entrypoint}` and each reached call request `{entrypoint,capability,version}` remain unchanged. Check resource budgets and effective restrictions before per-call authorization; then validate authority/deadline/trace capacity before dispatch. No positive quota or omitted filter bypasses authorization. False, exception, non-Boolean response and timeout retain the existing denial/failure rules. Protected arguments and key/event context remain separate from source data; reused Prepared handles cache no caller identity or current permission.

## Effect and lifecycle safety

The default maximum is one possible WRITE in `effect/2`, zero in `read/2`. One is also the hard ceiling: configuration, idempotency claims, guards or mutually exclusive conditions cannot enable a second WRITE. A zero effect ceiling restricts writes without naming capabilities. Static overflow rejects compilation/preparation; dynamic checks precede dispatch. READ/WRITE classification comes from the pinned host catalog, never source or adapter claims.

`effect/2` remains post-commit simulation only. It cannot undo the original event or treat an EVENT_REPORT as a command. Pre-commit interception is still unsupported by these formats; reject `BEFORE_COMMIT` rather than adapting it to AFTER_COMMIT. Any future pre-commit contract must default to read/decide/return with the host performing its native effect after validation; an opaque key does not authorize independent writes before native commit. REQUEST/display continues to provide no permission for a native mutation.

Keep the single-effect receipt and winner rules unchanged: winning CONFIRMED/NONE evidence settles before completion clock/payload checks, UNKNOWN survives lost responses, late responses cannot mutate termination, and failure after CONFIRMED does not imply rollback. Simulation and declarations do not certify real authorization or idempotency.

## Examples and rejection obligations

These are proposed `/2` fragments, not runnable `/1` fixtures. Assume the approved catalog already defines `tasks.get` and `tasks.getSummary` as READ operations accepting agent `taskId` and protected `actorId`, returning a record with `status: TaskStatus`. It also defines the generic `tasks.create` as WRITE. Use existing scalar/nominal types, an input with two TaskId fields, and an output Classification enum.

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

## Migration and acceptance

Do not delete grants from existing DCMs, patch artifact versions, reinterpret omission or regenerate old goldens. Preserve `/1` compilation, loader rejection behavior, required host APIs and fixtures. `/2` is opt-in after compiler, Java, Node and host conformance acceptance; an unsupported runtime must reject it. A host migrates by approving a new model and explicit compiler selection, recompiling, validating the new artifact in both runtimes, then separately selecting its digest/entrypoint/consumer. Old Prepared handles and active artifacts retain old semantics.

A behavior-preserving migration converts each old grant map into `capabilityPolicy: {allow: <old keys>, maxCalls: <old map>}` at the same hook/admission/invocation layer. An empty old map becomes `allow: []`, not `{}`. Preserve explicit old effect ceilings. Broadening to the corrected generic default is a separate deliberate host decision: remove the migrated restrictions only with host approval and renewed artifact selection. Never let an SDK translate old missing grants into the new default.

New host API entrypoints or explicit profile-aware overloads must distinguish the new optional-policy context from old required-grant records without breaking existing callers. Node selects semantics from the validated profile, not the presence of `effectGrant`; Java uses new context types or explicit overloads. Adapter signatures and consumer wire outcomes need no change: read/2 keeps the read signature, effect/2 keeps the protected original-key/event signature. Revalidate registration version/mode/key compatibility and selected consumer; do not silently reroute old consumers. Scenario schema versions must also distinguish optional-policy defaults from old required grant fixtures (proposed `keir-scenario/read-2` and `keir-scenario/effect-2`).

Acceptance requires positive/negative fixtures from the table, identical deterministic compiler bytes for both runtimes, all existing `/1` regression, packaged-host examples without default named grants, and matching product assistant guidance. It is a technical regression for the corrected requirement, not a product-value experiment. No test pass, new profile support or real-adapter guarantee is claimed by this document.
