# Node runtime

ESM library implementing the bounded `keir-read/1` and simulation-only `keir-effect/1` artifact profiles. It prepares trusted Java compiler output once and returns typed outcomes with redacted traces. Runtime execution requires Node, not Java or the compiler. The current tested environment is Node 24.14.1, CEL-ES 0.6.1 and protobuf 2.6.2; dependency pins are recorded in the npm lockfile.

The [read and decide](../roadmap/features/portable-core-first-slice.md) profile preserves its three-argument adapter API and DISPLAY consumption. The [single-effect contract](../docs/specs/single-effect.md) adds one guarded post-commit write with receipts and terminal business handlers. Complete compiler/Java-runtime/Node-runtime acceptance remains a separate combined gate. Source parsing, real effect admission, live activation and production containment remain outside this library.

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

The IDs above are fictional fixture values. `clock`, `authorize`, `readTask`, digests and artifact selection are host-owned inputs. Never derive the trusted expected digest from the same untrusted artifact being admitted. [The artifact contract](../docs/specs/compiler-artifact.md) and [runtime contract](../docs/specs/runtime.md) define the supported formats and authority boundary.

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

Build a local tarball and run the separately packaged [host example](../examples/node-host/README.md). The package exports only `prepare`, `invoke` and the fixed qualified `ENVIRONMENT`; validation and qualification helpers are internal.
