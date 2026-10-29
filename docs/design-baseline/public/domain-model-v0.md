# Keir Domain Capability Model v0 - normative draft (architecture v0.4)

**Package baseline:** v0.4 (public working draft, 28 October 2026). The language remains at semantic profile v0; the pre-release source identifier is now `keir/v0`. No new execution instruction is introduced.

**Status:** Target DCM contract, not an implemented semantic validator. The [fictional task-domain fixture](examples/task-domain.yaml) is a documented, typed example, not evidence a real host has certified its guarantees. Any `pending` certification MUST block production activation. The shared DCM selects domain-wide static capabilities and supplies types, effects, grants, source provenance, host-owned parameters and contract obligations. The **effective capability catalog** may additionally contain host-admitted tenant-scoped capabilities. An agent must not supply or modify that catalog. The agent cannot edit the authoritative DCM. The examples and full IR definition are in [Keir Extension IR v0](keir-language-v0.md).

## 1. Model identity and inputs

A DCM has `modelVersion: '0.1.0'`, a stable `domain.id`, semantic `domain.version`, pinned `sources`, `types`, `capabilities`, `entrypoints`, and `contracts`. The compiler computes a canonical SHA-256 over the complete **shared DCM**; the illustrative fixture fingerprint is recorded in [`examples/task-domain.sha256`](examples/task-domain.sha256). A deployed plan MUST pin the **exact** digest, not merely the version string.

Host onboarding has two explicit passes:

1. **Import selected interfaces.** For OpenAPI, import operation IDs, parameter/result types, required/optional fields, enumerations, nullability where accurately declared, and published error payload shapes. For AsyncAPI, import **selected message types**, their provenance and possible entrypoint input schema. Internal host operations use a manually supplied typed contract. Importing a document does **not** grant all of its operations or create a listener/consumer.
2. **Host semantic approval.** The application owner binds each admitted operation to one trusted adapter and adds nominal IDs, units, allowed fields, host-only parameters, effects, authorization and consistency contracts, maximum result/call counts, allowed entrypoints, error classes, ambiguous outcomes, replay policy and activation evidence. OpenAPI/AsyncAPI alone cannot establish these guarantees.

A `source` pin contains `{kind,artifact,digest,revision?}`. `artifact` is inert provenance, not a URI that the untrusted program can fetch. Imports must run in an isolated administrative build step with approved references; the runtime contains no arbitrary source resolver.

## 2. Types

Named types are one of:

- `nominal`: underlying `string` or `int64`, with optional pattern. `TaskId`, `ProjectId`, and `UserId` are **not interchangeable** even though they are strings on the wire.
- `enum`: closed set of string values; no unknown numeric fallback.
- `record`: fixed field map of `{type, required, nullable?, maxLength?, maxItems?}`. Unknown fields are rejected by the type checker/runtime projector.
- `list`: declared element type and positive finite `maxItems` (zero allowed for a deliberately empty list). Every list supplied to CEL MUST have such a bound.
- `unit`: signed `int64` or explicitly approved exact decimal representation plus unit/scale/max absolute value. v0 allows exact integer-minor-unit calculations; it does not promise arbitrary decimal CEL arithmetic.

Built-in type names are `String`, `Boolean`, `Int64`, `Instant`, `Duration`. Nominal types must be checked **before** exposing values as ordinary CEL strings. For an optional/nullable field, the compiler MUST demand an explicit presence proof and MUST NOT silently coerce absence to zero, empty string, or false. RFC 3339 instants are normalized to UTC by the adapter. Input and output size, string length, nested record depth and collection limits are enforced at both activation and runtime.

## 3. Capability definitions

An admitted capability may be native to the host or tenant-specific (for example, backed by a registered customer endpoint). These categories use the **same typed call contract**. Its source origin does not grant broader IR authority. A capability has an exact `version`, a pinned `origin.source/operationId` (or internal source), typed `parameters`, typed `result`, declared `effects`, named `preconditions`, named `guarantees`, typed `errors`, and `replay`. `maxResponseItems` is required for variable-length result capability families; any unbounded external array is inadmissible for an expression-visible result.

Each parameter declares `binding: agent|host`. The compiler MUST reject explicit agent supply of `host` parameters, including tenant ID, authenticated actor, idempotency key, and server-managed references. At runtime the host injects them **after** argument validation; the agent cannot add a same-named key to a nested object. The host also checks authorization/object scope for every invocation even if the compiler approved the operation. For tenant-specific HTTP-backed capabilities, the host alone owns endpoint registration, URL/destination validation, credentials, network policy, data egress approval and the actual transport implementation. Keir never makes arbitrary HTTP calls specified by IR and does not decide whether an external endpoint is trusted. **The follow-up example additionally host-binds `tasks.create.sourceTaskId` to the authenticated `TaskCompleted` entrypoint task ID.** The agent may query a source task but cannot redirect the protected origin ID of the created task.

A capability's read/write effects use a closed host-owned vocabulary, e.g. `task.read`, `project.read`, `task.create`, `task.assign`, `task.tag`. Effects are *declared upper bounds*, not proof of actual persistence; traces differentiate an attempted effect from a confirmed effect.

`preconditions` and `guarantees` reference IDs in `contracts`. A contract is a **declaration of an obligation**, not a Boolean guard that the extension may set and not a general contract-expression language. Allowed `kind`s: `scope`, `boundedResult`, `atomicUnique`, `compareAndSet`, `authorization`, `hostReplay`, `dataFreshness`. `enforcedBy` MUST be `trusted-host`; the host certification registry must independently resolve `certification` for activation. Neither a comment nor a successful mock makes a guarantee true.

### Critical fictional contracts

- `createTask` guarantees atomic uniqueness by `(tenant,sourceTaskId,kind)` and binds a host-generated replay key. This is required because a separate `listTasks` read cannot prevent concurrent duplicate follow-up creation. If a real host cannot certify it, **example A must not be activated for production**.
- `updateTask` requires `expectedVersion` compare-and-set and an approved-assignee check at the commit boundary. A prior `getProject` cannot replace the atomic permission/version check.
- `listTasks` returns at most the DCM maximum and may include scoped, bounded prerequisite-status evidence. These facts are advisory; their return does not lock the related tasks.

## 4. Entrypoints and composition

An entrypoint defines one typed invocation input, one typed output, a per-capability `grants` count, permitted effects, total calls, steps, CEL cost, output item count, wall clock budget, and required host contracts. **An entrypoint also has a host-owned lifecycle phase and result-consumer contract**. These specify what a returned decision/patch means, where enforcement occurs, and how unhandled errors or timeouts affect the surrounding operation. The agent never chooses an enforcement phase or failure policy.

- `TaskCompleted` is host-invoked with `TaskCompletedInput`, grants `getTask`, `listTasks`, and `createTask`. Thus a follow-up mutation is possible but an assignment or tag write is not. The dependency example uses the *same entrypoint and DCM* but does not invoke `createTask`; the compiler must not invent effects merely because a capability is granted.
- `TaskCreated` is host-invoked with `TaskCreatedInput`, grants `getTask`, `getProject`, and `updateTask` with assignment-only authority. `createTask`/`addTag` are not permitted here.

The DCM identifies a permitted **capability set**, not a single vendor-coded workflow. Multiple different programs select their own calls, conditions and ordering under the same grants. Static composition checks use **worst-case** effects and calls, including guarded operations, and all typed error-handling paths. There are no unbounded operations/loops in v0.

`maxCalls` and per-capability counts are both hard bounds. The host MUST apply stricter actual limits where available and reject oversized results, not truncate silently; truncation can turn an existence check into a false absence.

### 4.1 Hook/result-consumer contract (proposed DCM evolution)

A host-owned `PRE_OPERATION` hook may return `ALLOW`, `REJECT` with a declared business code, or a strictly constrained `PATCH`, but only if that exact entrypoint permits the respective outcome. The host applies/revalidates such a result before continuing native logic. Missing or invalid results, host adapter exceptions and deadline expiry follow a **preconfigured host policy**, not an IR-selected fail-open/fail-closed flag. Hooks must not bypass native validation or hold a database transaction across external calls. `POST_COMMIT` reactions cannot retroactively reject a committed action; response transformations cannot mutate protected host state.

The initial fictional DCM fixture only defines `TaskCompleted`; its grammar is unchanged. A follow-on illustrative `BeforeTaskCreate` hook can expose a typed proposed-task projection and grant `projects.get`. The extension returns `ALLOW` or `REJECT` based on that trusted project policy, and the task service enforces that response. [Hook/tenant design](hooks-and-tenant-capabilities.md) provides a non-executable example of the added contract fields. This is a **proposal**, not an implemented DCM schema addition.

### 4.2 Tenant-scoped capability overlay (proposed)

The shared DCM is domain-scoped; the host may pass a separately approved tenant capability catalog to compilation. The trusted compiler combines the two using explicit namespacing and **no tenant override of host-native IDs, bindings, entrypoints or contract guarantees**. Every tenant capability declares typed arguments/results, permitted effects, host-only fields, timeout ceilings and typed outcomes. The compiler checks the same call/effect/budget rules for native and tenant capabilities. The effective catalog's semantic digest and tenant authorization scope are pinned into the compiled artifact **in addition to** the shared DCM digest.

The `domain.fingerprint` in the current IR continues to refer to the shared DCM; the compiler receives tenant identity/catalog through **trusted invocation context**, not an untrusted IR field. An artifact using tenant capabilities is not portable between tenants merely because the capability names match. URL/credential changes may be transparent if the host maintains the approved contract and admission; type, effect, scope or guarantee changes require revalidation. The host may choose not to offer tenant endpoint registration at all; any agent proposal is available only through an explicitly offered host authoring feature.

**Timeout semantics:** The runner tracks total invocation budget and per-capability declared limits; the host adapter must enforce actual network deadlines and classify remote timeout/unknown side-effect evidence. The effective allowed deadline cannot exceed the remaining hook budget or a host-approved maximum. There is no automatic retry in v0; a remote timeout does not prove no side effect occurred.

## 5. Typed outcomes and replay

An error is identified by a stable code, kind `business|transient|uncertain`, type URI, status for Open Workflow mapping, and handling policy `return|abort|host-only`. HTTP status does not supply the semantic class. A capability can declare `UNKNOWN_COMMIT` even when its usual success response is `2xx`.

`replay.mode` is one of `read-only`, `host-idempotency`, `no-retry`, with `maxAttempts: 1` in v0. `unknownCommit` is explicit. `reconcile: host-only` means the generic synchronous program does not decide whether to repeat or refund an operation. A local step ledger is not proof of a remote commit; the host must use operation-specific evidence for reconciliation.

## 6. Version and compatibility

Recompilation/reapproval is mandatory if the signature, nominal type, field visibility, result completeness, effect, retry class, uncertain-outcome class, protection of host-bound fields, **hook result-consumer/failure semantics**, tenant effective catalog or semantic guarantee changes. A schema addition can still expand authority and is not automatically safe. Pin interface digests, DCM digest, adapter/contract certification versions and compiler/CEL policy version. Runtime activation checks the same pins.

## 7. What the machine-readable schema does *not* establish

The JSON Schema checks structure, enumerated fields, and declared scalar shapes. It **cannot** verify that named type/contract/capability references exist, that imported OpenAPI semantics were projected correctly, or that host certifications are genuine. The semantic DCM validator must perform these checks, including cycles in record references, nonpositive/unsafe resource caps, duplicate enum mappings, reserved identifier collisions, and incompatible nominal/unit assignments. These checks are required for a complete compiler but only partially implemented in the **fixture checker**.


## 8. Cross-language and compiled-artifact requirements

The shared DCM's **semantic digest** is pinned into the agent- or human-authored IR and compiled artifact. Where tenant capabilities are used, a trusted compiler also pins the effective catalog digest, tenant-scope binding, and host hook-consumer version into the artifact; these are not agent-supplied authority. Descriptors used by CEL must be generated from approved DCM types and pinned, never loaded from a user-chosen URL at runtime. The compiled artifact contains resolved capability IDs/versions, host/agent input slots, expected output types, possible effects, and required trusted contracts. The Node runner performs current host authorization and data-shape checks but does not need to parse the DCM's entire authoring structure at invocation time.

The compiler's type environment MUST distinguish nominal `TaskId`/`UserId`/`ProjectId` even if the CEL engine sees underlying strings; likewise signed integer amounts retain units and scales outside CEL. **The CEL interchange CI has not implemented this nominal/unit overlay**, and string-based type equivalence alone is insufficient for approval.

For the initial task demo, `tasks.search` must provide a complete answer for a bounded, sufficiently narrow origin filter (zero matches means none, not a silent truncated page), and `tasks.create` must atomically reject a duplicate origin key. These are real host obligations. An illustrative DCM can declare them, but only actual adapter tests and appropriate database constraints certify them.

**Notes on this package's example fingerprint:** The fixture SHA-256 is computed from a deterministic JSON rendering of its YAML mapping (Unicode preserved, keys sorted, no incidental whitespace). This is a reproducible **example-only** convention; the framework's normative DCM canonicalization and serialization must be specified and tested before production use. A digest alone cannot authenticate a document's author or certify its contracts.
