# Node runtime

ESM library implementing bounded read and single-effect profiles. Legacy `keir-read/1` and `keir-effect/1` APIs remain available; `keir-read/2` and `keir-effect/2` add complete scoped model binding and reference artifacts. `keir-effect/3` adds explicit host-qualified real admission; older effect profiles remain simulation-only. It prepares trusted Java compiler output once and returns typed outcomes with redacted traces. Runtime execution requires Node, not Java or the compiler. The current tested environment is Node 24.14.1, CEL-ES 0.6.1 and protobuf 2.6.2; dependency pins are recorded in the npm lockfile.

The [read and decide](../roadmap/features/portable-core-first-slice.md) profile preserves its three-argument adapter API and DISPLAY consumption. The [single-effect contract](../docs/specs/single-effect.md) adds one guarded post-commit write with receipts and terminal business handlers. The [effect/3 protocol](../docs/specs/real-single-effect-admission.md) checks separately approved host qualification. Source parsing, native-host qualification, live activation and production containment remain outside this library.

For effect/3, `bindModel(dcmBytes, approval, invokers, qualification)` requires the optional final argument only when `approval.mode` is `real`. It is forbidden for simulation and earlier profiles. `undefined` means omission; explicit `null` is invalid. Qualification is the closed `{evidence,isCurrent}` record in the protocol: complete catalog, exact scope/model/environment/key identity, independently approved host build/report and positive revision. Evidence is capped at 16,384 canonical UTF-8 bytes during encoding, before snapshotting. The runtime retains independent immutable evidence and callback/invoker references.

`isCurrent` is a trusted synchronous, non-I/O callback that returns primitive Boolean true only while that exact host deployment/report/revision remains approved. Exceptions, Promises, thenables and boxed Boolean objects deny admission. The library cannot establish native atomicity, retention or deployment trust from supplied JSON or a constant callback. An independently reviewed native report and current host/user authority remain required.

`prepareProgram` and `invokeProgram` keep their existing signatures. Separate simulation and real bindings can prepare the same exact effect/3 artifact. Current qualification is checked at binding, preparation, invocation entry, before authorization and again before dispatch after asynchronous authorization; callback time consumes the existing deadline. Revocation denies later calls but cannot undo a dispatched effect or erase a CONFIRMED receipt. The original native key remains separate from business arguments. Existing NONE/CONFIRMED/UNKNOWN and late-response winner rules apply without replay or mode fallback.

`isCurrent` is trusted, bounded, synchronous host code: only primitive `true` grants authority. Exceptions and every other return deny synchronously. On the tested Node baseline, defensive cleanup of an accidentally returned Promise is limited to native same-realm values with the captured intrinsic prototype, no own constructor and unchanged complete intrinsic constructor/species descriptors. Cleanup bypasses an own `then` getter and discards both fulfilled and rejected values. Other Promise shapes and thenables are denied without cleanup getters or assimilation. The host owns unsupported asynchronous work and its rejection handling; this is not a same-process host-code sandbox or a guarantee that hostile callbacks cannot terminate their own process.

```js
import { prepare, invoke, ENVIRONMENT } from '@keir/runtime-node';

const selected = prepare(artifactBytes, {
  expectedArtifactDigest, expectedDcmDigest, entrypoint: 'ClassifyTask',
  consumer: { id: 'sample.classification', version: '1', kind: 'DISPLAY' },
  compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: ENVIRONMENT,
  mode: 'simulation', grants: { 'tasks.get': 1 }
}, {
  'tasks.get': { version: '1', mode: 'simulation', invoke: readTask }
});
if (selected.code) throw new Error(selected.code);

const outcome = await invoke(selected, { taskId: 'task1' }, {
  bindings: { actorId: 'user1' }, grants: { 'tasks.get': 1 },
  deadline: { clock, expiresAtMs }, authorize
});
```

The example above retains the `/1` API. Its IDs are fictional fixture values. `clock`, `authorize`, `readTask`, digests and artifact selection are host-owned inputs. Never derive the trusted expected digest from the same untrusted artifact being admitted. [The artifact contract](../docs/specs/compiler-artifact.md) and [runtime contract](../docs/specs/runtime.md) define the supported formats and authority boundary.

`prepare` returns an opaque frozen handle or the closed diagnostic `{code,path,message}`. It checks the externally held digest before parsing, then canonical UTF-8 JSON, the complete DCM/plan, exact pins and consumer, descriptors, CEL variables/types and function/overload pairs. Protobuf nesting is bounded before decoding. Unknown fields, duplicate keys, noncanonical encodings and unsupported constructs fail closed. Preparation calls neither adapters nor authorization. The handle snapshots registration metadata/functions and contains no caller identity or invocation values.

For `keir-read/1`, `invoke` returns `{classification,result?,error?,trace}`. Only COMPLETED has a result; failures carry `{code,path}` without raw exceptions. BUSINESS_REJECTION is distinct from FAILED and LIMIT_EXCEEDED. The trace contains only sequence, event, approved step/capability IDs and safe codes. Invocation input, bindings and arguments are validated and copied; each invocation gets fresh state. A consumer must validate a COMPLETED result before displaying it; it must never turn failure into a default classification.

Both grant maps are required and contain positive call ceilings; omitting a capability grants zero permission. Admission narrows the selected entrypoint, and invocation grants narrow admission. Authorization receives a frozen `{entrypoint}` request, then `{entrypoint,capability,version}` before each read. It must return Boolean; denial, callback failure and timeout have different safe outcomes. The host callback captures current scope, and the adapter independently enforces resource access and native invariants.

Read-profile adapters are registered as `{version,mode,invoke}` under exact capability IDs. The callback receives separate immutable agent arguments, protected arguments and remaining milliseconds. Its only supported responses are `{kind:'SUCCESS',value}`, `{kind:'BUSINESS_REJECTION',code}`, and `{kind:'TRANSIENT_FAILURE'}`. The callback must enforce its actual I/O deadline. Runtime timeout discards late results; it does not cancel underlying host I/O. Simulation registration is explicit and cannot be selected with real mode; a mode label alone certifies no real adapter guarantees.

Optional admission `limits` narrows `steps` (0..8), `calls` (0..4), `totalAstNodes` (0..512) and `traceEvents` (2..64). Static limits are checked at preparation and dispatch/step limits again at invocation. Trace capacity for dispatch, success and termination is reserved before a call. Other fixed caps include 64 nodes/depth 16 per expression, 256 UTF-8 bytes per scalar string and 8,192 serialized bytes per input/result. No truncation or automatic retry occurs.

Declared value-size ceiling breaches produce LIMIT_EXCEEDED/`BUDGET_EXCEEDED`; wrong types, missing/null/unknown fields and invalid enum members use the corresponding boundary validation failure. Neither returns a fabricated ordinary result.

Deadlines use the host's nondecreasing exact integer millisecond clock and an expiry in that same origin. Equality at expiry is expired; invalid/regressing clocks fail. Asynchronous authorization/adapter waits have a finite native remaining-time budget, with long timers chunked safely, and late callbacks cannot change termination. Pure scalar CEL currently executes synchronously under the admitted AST/data limits, with deadline checks before and after evaluation. It cannot be interrupted by a timer. No hard synchronous CPU deadline, complete process-memory isolation or production multi-tenant containment is claimed.

Business values are validated before protobuf projection. Required `false` and empty String values survive; missing/null/unknown fields do not become defaults. Native protobuf JSON-name conversion also preserves valid fields named `constructor`, `toString`, `toJSON` and `valueOf`. Nominal/enum refinements remain the compiled DCM/type contract; wire strings do not carry independent authority.

## Single-effect profile

The explicit profile/format pair is `keir-effect/1` and `keir-artifact/effect-1`, with `keir-dcm/effect-1`. Admission adds `effectGrant` (0 or 1) and `keyBinding:{type,validate}`. Each adapter adds the exact native `keyType`; all registrations and admission must use `mode:'simulation'`. Fake guarantees never authorize real mode. Preparation snapshots metadata and validator/function references without invoking host callbacks.

Invocation adds `effectGrant`, `requestKey` and `event:{id,source}`. Fresh call/effect grants may narrow admission. The host supplies an immutable native key and a bounded pure synchronous validator. The runtime brackets validation with clock checks, preserves the original key reference, and forwards it to every read/write adapter as the fourth argument `hostContext:{requestKey,event}`. That context and copied event scalars are frozen without traversing, freezing, cloning or converting the key. Key/event values never enter CEL or public traces. Hosts/adapters still enforce event origin, resource access and native invariants.

Only an earlier unconditional Boolean BIND can guard a call. A false guard skips arguments, per-call authorization, dispatch and dynamic effect counting. Guarded results are excluded from all later CEL environments; no optional values or presence language is added. All potential writes count toward the one-write admission ceiling, including mutually exclusive or constant-false guards.

WRITE responses are `{kind:'SUCCESS',value,effect:'CONFIRMED'}`, `{kind:'BUSINESS_REJECTION',code,effect:'NONE'}`, `{kind:'TRANSIENT_FAILURE',effect:'NONE'}` or `{kind:'UNKNOWN_COMMIT',effect:'UNKNOWN'}`. READ responses retain their old shapes and reject effect evidence. A winning WRITE response establishes valid outer evidence before completion-clock and payload checks. A timeout winner keeps UNKNOWN; later responses cannot revise it. Malformed payload after a valid confirmation retains CONFIRMED. Throw, lost response or malformed outer evidence leaves UNKNOWN; no retry occurs.

Effect outcomes always include `effects`, an immutable array of zero or one `{step,capability,state}` receipts. No attempted write means an empty array. UNKNOWN forces UNCERTAIN without a normal result. Confirmed or definite-none evidence survives later deadline, validation or trace failure. A declared terminal business handler may add a typed result while classification remains BUSINESS_REJECTION and `error` retains the original rejection. Handler failure keeps that rejection in nonrecursive `error.cause`. Consumer validation and the EVENT_REPORT receipt belong to the embedding host; this module does not add a host/simulation service.

From the repository root, after the shared Java artifact is present:

```powershell
npm.cmd ci --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
npm.cmd test --prefix runtime-node
npm.cmd run --prefix runtime-node test:coverage
```

Default tests include the shared [scenario corpus and harness checks](../conformance/read-and-decide/README.md), alongside module tests using `conformance/read-and-decide/artifact.json` and supplemental [Java-generated scalar fixtures](test/fixtures/README.md). They cover canonical bytes, closed admission, typed boundaries, exact traces, fresh grants/users, failed authorization/adapters, async expiry/late responses, reserved field names and source-condition changes. An interim Java compiler output can be selected with process-local `KEIR_ARTIFACT_FILE`; no local path belongs in tracked evidence. Tests do not independently compile CEL in Node or establish Java-runtime parity.

Effect module tests consume the separately Java-compiled `conformance/single-effect` fixture and its external artifact/model pins. They cover guards, original native keys, grants, receipts, response/timeout priority and late-result isolation. They do not execute a Node compiler or certify real adapters.

The reference profiles use separate [bound-model APIs](../docs/specs/host-model-binding.md):

```js
import { bindModel, prepareProgram, invokeProgram, ENVIRONMENT } from '@keir/runtime-node';

const binding = bindModel(approvedDcmBytes, {
  scope: approvedScope, modelRef: approvedModelRef, profile: 'keir-effect/2',
  environment: ENVIRONMENT, mode: 'simulation',
  keyBinding: { type: 'sample.RequestUuid/1', validate: validateRequestKey }
}, {
  'tasks.get': { version: '1', mode: 'simulation', keyType: 'sample.RequestUuid/1', invoke: readTask },
  'tasks.create': { version: '1', mode: 'simulation', keyType: 'sample.RequestUuid/1', invoke: createTask }
});
if (binding.diagnostic) throw new Error(binding.diagnostic.code);
const program = prepareProgram(approvedArtifactBytes, binding.boundModel, {
  expectedArtifactDigest, expectedDcmDigest, entrypoint: 'TaskCompleted',
  consumer: { id: 'sample.followup.report', version: '1', kind: 'EVENT_REPORT' },
  compiler: 'keir-compiler-java/0.1.0-SNAPSHOT', environment: ENVIRONMENT, mode: 'simulation'
});
if (program.diagnostic) throw new Error(program.diagnostic.code);
const result = await invokeProgram(program.prepared, { taskId: 'task1' }, {
  scope: approvedScope, bindings: { actorId: 'user1' }, deadline: { clock, expiresAtMs }, authorize,
  requestKey: originalRequestKey, event: { id: 'event1', source: 'task1' }
});
```

Both wrappers are frozen, closed two-field records: `{boundModel,diagnostic}` and `{prepared,diagnostic}`, with exactly one non-null value. Handles are privately branded; lookalikes and wrong API families fail closed. Model binding validates all definitions and exactly one compatible invoker per catalog capability, including unused operations. It snapshots bytes, metadata and functions without calling invokers or the key validator. Model/type state is reused within the binding, while each program's complete plan and checked expressions are independently validated. Reference artifacts cannot override the bound model, descriptors, scope or invokers.

The host independently approves the exact model reference, compiler/environment and artifact pins. `invokeProgram` requires the binding's exact scope and fresh authorization; scope equality alone grants nothing. No caller identity, key, event, deadline or authorization result is cached. Replacing a registration requires a new binding; existing Prepared handles retain the old one. Read bindings use three-field registrations and may use real or simulation mode; effect/2 bindings remain simulation-only and pass the original native key to both read and write adapters unchanged.

`/2` omits named grants by default. Optional `capabilityPolicy` (`allow`, `deny`, `maxCalls`) narrows the published catalog at hook, admission and fresh invocation boundaries; omission preserves earlier restrictions. Explicit widening or null values are invalid. The aggregate limits remain separate, and potential calls count conservatively at preparation. Read programs cannot dispatch WRITE operations even when the catalog publishes them. Effect ceilings inherit from the preceding boundary when omitted, defaulting to one at the hook, with a hard maximum of one potential WRITE. A fresh zero ceiling permits a skipped write but denies a reached one before dispatch.

The Java-compiled `/2` fixtures and source-only project/title variants are exercised by the default module/shared tests. The source owns those choices; generic `tasks.create` receives `projectId` and `title`, with native event provenance outside business arguments. No source compiler/parser, transport client, registry or activation service is added to the runtime. Finite handle and in-flight budgets in the sample host are host-owned qualification limits, not runtime lifecycle APIs or decoded-heap containment guarantees.

Build a local tarball and run the separately packaged [host example](../examples/node-host/README.md). The package exports `prepare`, `invoke`, `bindModel`, `prepareProgram`, `invokeProgram` and the fixed qualified `ENVIRONMENT`; validation and qualification helpers are internal.
