# Generic hooks and capability composition

<a id="corrected-host-bound-effect4"></a>
<a id="effect4-contract-and-version-boundaries"></a>
## Single-write behavior and compatibility boundaries

For single-write behavior, the host defines its catalog in the DCM and supplies every compatible implementation at initialization. [Host model binding](host-model-binding.md#effect4-binding-and-preparation) defines the binding API; [runtime execution](runtime.md#effect4-execution) defines invocation and limits; [single-write semantics](single-effect.md#effect-evidence-and-outcomes) defines write receipts. Binding checks completeness, versions, key types and homogeneous mode. Host implementations enforce current permissions, key authenticity and native commit rules. Empty catalogs and no-call programs are valid; runtime context, type and resource validation still applies.

The DCM can restrict operations with `capabilityPolicy` and limit writes with `effectGrant`. The defaults are the published catalog and at most one possible write. Keir supports explicit nominal literals, guards, handlers, and [bounded String concatenation](language.md#types-and-cel-bridge). [Compatibility details and wire identifiers](../internal/implementation-reference.md#contract-compatibility)

## Responsibility boundaries

Reference artifacts identify an exact immutable DCM. [Host model binding](host-model-binding.md) binds the complete catalog and implementations into a reusable handle. Program validation and fresh invocation context remain separate. The earliest artifacts retain their embedded model and descriptors.

A host publishes a scoped, approved capability catalog. A hook defines its input projection, allowed output and consumer, trigger, lifecycle placement and effect ceiling. A customer extension selects and composes catalog operations; the host need not anticipate its call list. A required map naming the operations of a sample program is not the generic hook contract.

Capabilities expose reusable host-domain operations rather than pre-implementing a customer's customization. In the task example, the host publishes `tasks.create` with project/title arguments; the extension decides when and why to create a follow-up. The adapter must not hide that project's filter, the title or the follow-up rule. Native domain-specific operations can still be appropriate when they express an actual host operation or atomic guarantee; Keir does not require every capability to be CRUD.

| Boundary | Authority and obligation |
| --- | --- |
| Catalog publication and discovery | Host selects the complete effective DCM for the authenticated customer scope. Publication controls visibility; discovery exposes only that approved scope. Neither descriptions nor discovery results authorize a native operation. |
| Hook definition | Host defines typed input/output, consumer semantics and lifecycle/effect constraints. No capability names or counts are required by default. |
| Authored composition | Source uses static calls to published capabilities and typed dataflow. It cannot publish operations, supply policy, select credentials or override protected bindings. |
| Compilation and artifact selection | Trusted compiler environment selects the profile and DCM. Compilation checks composition; host independently approves/selects the exact artifact, entrypoint and consumer. |
| Binding and preparation | The host selects the exact model and artifact. The runtime derives compatibility checks from them. The host separately checks current authorization. |
| Invocation and dispatch | The host supplies fresh context for every invocation. Implementations enforce current permissions, resource access, invariants and native deadlines. |
| Runtime | Execute the selected artifact against unchanged host payload and fresh protected context; enforce types, policies, bounds and honest outcomes. Do not re-author customer logic, infer customer choices or load implicit configuration. |
| Host integration assistant | Help the host draft/review hook payload, catalog, native adapters, authorization and consumption. Only host-owner approval publishes these contracts; the assistant cannot certify guarantees or invent authority. |
| Customer authoring agent | Help the customer author logic, constants and capability composition against the published contract. It may propose source/scenarios and request approval, but cannot change the hook/catalog, protected values, permissions or activation on its own. |

The [complete TaskCompleted walkthrough](generic-hook-example.md) applies this ownership model from host DCM and customer source through compilation, artifact selection and invocation. The source-authored project/title are independent of hook payload; explicit nominal literals preserve business typing without supplying authority.

The catalog is a host-selected snapshot, not a wildcard over installed adapters or future publications. Adding a catalog operation changes its digest; using that changed snapshot requires recompilation and host reselection. Existing programs may continue using their exact earlier approved snapshot while its dependencies remain available. Choosing a different **already-published** operation or repeating a read does not require changing the hook. Host-specific tenant selection, authenticated transport, endpoint publication and activation remain host responsibilities; no policy service is required.

## Capability invocation across services

Treat internal service-to-service operations, including HTTP or RPC calls, as the normal integration case. A capability describes a logical operation, not a requirement that its business implementation runs inside the runner process. The registered adapter is the runner-facing dispatch bridge; an in-process service method is another supported implementation choice.

The host supplies a compatible client/adapter for each registered capability version. That bridge maps validated arguments to the remote request and maps responses to declared results, rejections and effect evidence. Service routing, credentials, trusted identity propagation, connection management and transport details remain host-owned integration concerns, outside customer source and compiled artifacts. The DCM supplies the logical typed contract; a model reference does not itself locate or authorize a service. This does not require a built-in HTTP client, service discovery system or generated transport bindings in Keir.

Remote calls retain the same authorization, resource checks and deadline obligations. A remote operation can finish after a local timeout or cancellation. Losing a response after a possible write preserves UNKNOWN/UNCERTAIN unless the native contract establishes a definite outcome. Keir dispatches an eligible call at most once. Reconcile uncertain outcomes through the host before deciding whether to retry. Keep user credentials and current permissions out of shared preparation. The [current API](host-model-binding.md#api-shapes) and [earlier dispatch API](host-model-binding.md#invocation-and-remote-dispatch) both preserve the original native key.

## Event facts, extension choices and protected authority

The host defines the hook payload independently of any one extension. Preserve its legitimate fields even when a sample does not use them. Do not require the host to add customer-selected project/title fields merely to make one follow-up program work.

| Origin | Example and rule |
| --- | --- |
| Host-defined payload and native facts | `input.taskId` identifies the event task; the host may also legitimately project its actual project or other approved facts. Source reads these facts, but cannot reinterpret them as the customer's choice. |
| Authored program constants | The customer selects a particular eligible project and fixed title inside the program. They compile into the artifact. Another source requires explicit program logic, such as an approved capability, not an implicit settings/configuration channel. |
| Protected authority/context | Authenticated actor/tenant scope, credentials, event provenance and original native key stay host-owned and outside source/CEL. A business ID constant grants none of this authority. |

An explicit nominal annotation supplies business typing without changing the host payload or granting access. See [compatibility details](../internal/implementation-reference.md#contract-compatibility) for earlier language restrictions.

**Explicit nominal literals:** permit a binding exactly `{literal: string, type: nominalTypeName}` at existing binding positions, including BIND. The type must resolve to a host-published nominal String type. No arbitrary annotation, enum/built-in/record annotation, host-key contract type, null/numeric value, dynamic expression or String-variable cast is admitted. The annotation supplies business typing, never evidence of existence, ownership or access. Keep unannotated literal rules unchanged; in particular, no implicit String-to-nominal coercion. Apply existing Unicode and 256-byte string limits. Invalid shape/unknown fields retain structural diagnostics; unknown/non-nominal type or incompatible nominal assignment uses `NOMINAL_MISMATCH` at the binding/type or literal path as applicable. Host-bound argument overrides still fail preflight regardless of annotation.

A nominal literal binds the declared nominal type in the expression environment. Same-type comparisons and arguments are allowed. Lower to the artifact's `{literal,type}` representation. Both loaders validate the declared nominal type and value. The compiler preserves nominal provenance through BIND and checked metadata. Both runtimes reject forged type metadata. Keep credentials and native keys out of literals because they appear in source and artifacts.

Resolve the annotation only in the DCM business `types` namespace. An equal host-key metadata label neither grants native-key access nor invalidates a legitimate business nominal type. An unknown/non-nominal annotation fails with `NOMINAL_MISMATCH` at the binding's `/type`; a valid nominal annotation incompatible with its expected nominal type fails at `/literal`.

Example fragment for a single-write rule, assuming the host payload includes `taskId` and the catalog publishes the types and operations shown:

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

This fragment uses the same host payload for customers choosing different project and title constants. Changing a constant changes the canonical source and artifact digests and requires compilation, review and host selection of that revision. An explicitly called operation may supply a value under the usual catalog, type, authorization and budget rules. See the [complete source](../../conformance/generic-hooks/effect4/source.yaml).

Project equality is business logic, not authorization. Authoring tools may help resolve a project name to its ID, but source still contains a customer-authored reference. The host/adapter must enforce current access and native invariants, including nonexistent, wrong-scope or revoked projects. Unknown effect status and prior confirmed effects retain the existing semantics. Explicit nominal literals pass compiler and cross-runtime regression; that typing supplies no authority.

<a id="versioned-delta"></a>

[Catalog-bound model and artifact changes](../internal/compatibility-reference.md#catalog-delta).

## Optional restrictive controls

The current contract takes these restrictions from the DCM. Earlier catalog-bound and qualified-write APIs also supported narrowing at admission and invocation. See the [compatibility reference](../internal/compatibility-reference.md#catalog-delta).

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

Start with the catalog, then apply the hook restrictions. In the earlier APIs with admission and invocation policies, apply those layers in that order after the hook. Retain inherited allow/deny constraints and quotas; omission never resets them. An explicit allowlist must be a subset of preceding eligible IDs. An explicit quota must not exceed that ID's preceding ceiling (zero if denied, otherwise inherited quota or profile call ceiling four). Reject attempted widening rather than silently accepting a broader configuration and clipping it. A denylist may repeat an already-denied catalog ID. Within one layer, apply `allow` and `deny` before quotas; a positive quota for a denied ID is invalid. The source cannot supply any layer or substitute its own DCM.

Per-capability quotas are independent of aggregate resource budgets: a quota of four does not permit four calls when the admitted aggregate call limit is two. In legacy APIs the existing `admission.limits` contract remains narrowing-only; omitted fields retain profile ceilings (eight steps, four potential calls, 512 aggregate AST nodes, 64 trace events, and unchanged byte/value limits). No required hook-specific call budget or named quota is introduced. Repeated READs consume the aggregate budget normally; they are not deduplicated, cached or assumed to return the same state.

Compiler and loaders conservatively count all potential calls, including false guards and mutually exclusive branches, against hook policy, aggregate limits and effect ceilings. Legacy preparation also requires the artifact's potential counts to fit admission restrictions. Legacy invocation validates the fresh policy before any adapter call but applies its narrower permissions/counts to reached dispatches: a false guard consumes neither a call nor a quota and performs no per-call authorization. This preserves safe no-op execution under a narrowed invocation, as with a zero fresh effect ceiling.

In the earlier APIs with admission and invocation policies, the mandatory invocation authorization request `{entrypoint}` and each reached call request `{entrypoint,capability,version}` remain unchanged. Check resource budgets and effective restrictions before per-call authorization; then validate authority/deadline/trace capacity before dispatch. No positive quota or omitted filter bypasses authorization. False, exception, non-Boolean response and timeout retain the existing denial/failure rules. Protected arguments and key/event context remain separate from source data; reused Prepared handles cache no caller identity or current permission.

## Effect and lifecycle safety

The default maximum is one possible WRITE for a single-write hook and zero for a read-only hook. One is also the hard ceiling: configuration, idempotency claims, guards or mutually exclusive conditions cannot enable a second WRITE. A zero effect ceiling restricts writes without naming capabilities. Static overflow rejects compilation/preparation; dynamic checks precede dispatch. READ/WRITE classification comes from the pinned host catalog, never source or adapter claims.

The catalog-bound simulated-write contract only supports simulation after commit. A single-write rule cannot undo the original event or use its EVENT_REPORT as a command. Pre-commit interception is unsupported; reject `BEFORE_COMMIT` rather than adapting it to AFTER_COMMIT. Any future pre-commit contract must default to read/decide/return with the host performing its native effect after validation; an opaque key does not authorize independent writes before native commit. REQUEST/display continues to provide no permission for a native mutation.

Keep the single-effect receipt and winner rules unchanged: winning CONFIRMED/NONE evidence settles before completion clock/payload checks, UNKNOWN survives lost responses, late responses cannot mutate termination, and failure after CONFIRMED does not imply rollback. Simulation and declarations do not certify real authorization or idempotency.

<a id="examples-and-rejection-obligations"></a>
<a id="migration-and-acceptance"></a>

[Catalog-bound examples, migration and acceptance](../internal/compatibility-reference.md#catalog-examples).
