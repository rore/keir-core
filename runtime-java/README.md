# Java runtime

## Current host-bound profile

Use these `KeirRuntime` entry points for `keir-effect/4`:

1. Call `bindModel(dcmBytes, implementations)` with the exact host-selected model and one `BoundEffectAdapter` per capability. All registrations use the same mode and match the catalog's capability versions and native key type.
2. Verify the selected artifact against a trusted digest in host code, then call `prepareProgram(artifactBytes, boundModel)`. Check `succeeded()` on both binding and preparation before using their handles.
3. Call `invokeBoundEffectProgram(prepared, businessInput, new BoundEffectInvocation(bindings, deadline, requestKey, event))` to get a `CompletionStage<Outcome>`. Supply fresh context for every invocation. The host implementations check current permissions and authenticate the original key.

See the [runnable quickstart](../docs/quickstart.md), [exact adapter and deadline types](../docs/specs/host-model-binding.md#corrected-host-bound-4-binding) and [verification evidence](../docs/testing.md#effect4-verification).

## Legacy /2 APIs

The explicit `/2` APIs bind a complete host-approved catalog once, then prepare reference-only programs against that immutable binding. Call `KeirRuntime.bindModel(dcmBytes, approval, invokers)` and check its `ModelBinding` before using `boundModel()`. Approval supplies the exact model reference, scope, profile, environment and mode; effect approval also supplies the typed `KeyBinding`. Register every catalog capability, including unused or policy-denied operations. Binding snapshots metadata and callable references without invoking them.

Call `prepareProgram(artifactBytes, boundModel, admission)` with independent artifact/model pins, entrypoint, consumer, compiler, environment and mode. It directly validates the `/2` envelope and checked plan against reusable model descriptors. Optional `capabilityPolicy` and limits only narrow the hook; omitted effect ceilings inherit the default maximum of one potential WRITE. `/2` admission forbids legacy `grants` and preparation-time invokers or key bindings.

For fresh execution, use `invokeProgram` with `Invocation2` for read/2, or `invokeEffectProgram` with `EffectInvocation2` for effect/2. Supply matching scope, current protected bindings, deadline, authorizer and `Optional.empty()` for an omitted policy. Effects additionally require the original native key, event and `OptionalLong.empty()` to inherit the admitted effect ceiling. Null Optional objects are invalid. Invocation methods reject handles from the other profile generation. The execution engine retains authorization, limits and response/timeout receipts.

The [legacy /3 qualification protocol](../docs/specs/real-single-effect-admission.md) extends the legacy binding flow for `keir-effect/3`.

## Legacy /1 APIs

`keir-runtime-java` executes the shared `keir-read/1` artifact with JDK 21 and CEL Java 0.14.0. It has no dependency on the Keir compiler, CEL compiler/parser, ANTLR or YAML. Compilation belongs outside execution; see the independent [Java host](../examples/java-host/README.md) for a packaged simulation.

The separate `keir-effect/1` API implements the [single-effect contract](../docs/specs/single-effect.md) with simulation registrations only. Legacy and generic effect scenarios pass shared Java/Node packaged-host verification; the packaged read entrypoint remains compatible.

`KeirRuntime.prepare(bytes, admission, adapters)` returns a `Preparation` containing either an immutable `Prepared` handle or a safe `Diagnostic`. Admission supplies an externally trusted whole-artifact digest, DCM digest, entrypoint, consumer, compiler and environment pins, adapter mode, grants and optional narrowing limits. The whole-byte digest is checked before JSON parsing. Canonical JSON, the complete DCM and plan, generated descriptors, checked CEL metadata and the restricted expression profile are checked before CEL runtime preparation. Preparation invokes no host callback.

`KeirRuntime.invoke(prepared, businessInput, invocation)` returns a `CompletionStage<Outcome>`. Construct `Invocation` in trusted host code from fresh protected bindings, `Deadline(LongSupplier clock, long expiresAtMs)`, an asynchronous `Authorizer` and fresh grants. Clock readings use nondecreasing nonnegative integer milliseconds in the same arbitrary monotonic domain as expiry, bounded by 2^53-1. Omitted capability grants mean zero; positive counts may only narrow admission. Invocation data is validated and copied before asynchronous authorization.

Authorization receives `{entrypoint}`, then `{entrypoint,capability,version}` before each call. An `Adapter` registers an exact version, simulation/real mode and asynchronous function receiving separate immutable agent and protected argument maps plus remaining milliseconds. It returns the closed SUCCESS, BUSINESS_REJECTION or TRANSIENT_FAILURE records from the [runtime contract](../docs/specs/runtime.md). Host adapters retain responsibility for resource access and actual I/O deadlines.

`Outcome.toMap()` exposes a deeply immutable JSON-compatible record; `toJson()` serializes its canonical bytes. In the read profile only COMPLETED includes a result. Failures carry safe code/path pairs; traces expose event order and identifiers, without argument values or exception text. Validate results again in the selected host consumer.

For effects use `prepareEffect(bytes, admission, Map<String,EffectAdapter>)` and `invokeEffect(prepared, businessInput, EffectInvocation)`. These named methods preserve the original read adapter, invocation and preparation signatures. Preparation requires the effect artifact/model formats explicitly and independently checks guards, pre-call handler environments, checked CEL, one potential write and static grants. It invokes no validator, authorizer or adapter.

Effect admission extends the existing map with `effectGrant` (integer 0 or 1) and a typed `new KeyBinding(type, validator)` registration. Each immutable `EffectAdapter(version, mode, keyType, function)` must match the DCM and use `simulation`; real mode is rejected. The validator's `Object validate(Object key)` must return a Boolean and must be bounded, pure and perform no I/O. Preparation retains its function reference; each invocation supplies its own key.

`EffectInvocation` adds `Long effectGrant`, `Object requestKey` and `Map<String,Object> event` to the read invocation fields. The grant may only narrow admission; zero denies a reached write before its authorization callback. The event is exactly `{id,source}`, with nominal String values. Invocation validates bindings, grants, event and a non-null accepted key before the first adapter call, including on no-op paths. The sample validator accepts `java.util.UUID`; the runtime makes no assumption about that representation.

The effect adapter receives `(agentArgs, protectedArgs, remainingMs, HostContext)`. `HostContext` contains the invocation's **original key reference** and an immutable event snapshot. The runtime never traverses, clones, converts, hashes or compares the key. The host owns its immutability, uniqueness and wire representation. Neither key nor event values enter CEL, artifacts or traces. Both read and write adapters in the effect profile use this fourth argument; read-profile adapters keep their three arguments.

An effect outcome always adds immutable `effects()` / `effects` containing zero or one `{step,capability,state}` receipts. Skipped and undispatched writes have none. A dispatched write starts UNKNOWN; a winning closed response establishes CONFIRMED or NONE before completion clock and value checks. Timeout winners ignore late responses. Failures retain evidence; an outstanding UNKNOWN forces UNCERTAIN with the original safe failure code. Confirmation is adapter evidence, not rollback or crash durability.

Business rejection is terminal. A matching typed handler may add a result while retaining BUSINESS_REJECTION and its original error. Handler failure adds that rejection as the nonrecursive `error.cause`. False guards skip arguments, per-call authorization and dispatch. Trace reservation prevents writes unless intent, dispatch, settlement, possible success and termination can all fit.

## Verification

Async authorization and adapter waits have finite native timeouts even when an injected logical clock stays stationary. Expiry checks run on independent daemon workers so a blocking host clock does not occupy the shared scheduler; that clock can still retain its own worker/invocation. Expired or regressing clocks stop later work, and late completions are ignored. Synchronous CEL and synchronous host callbacks cannot be preempted. Checked-AST/data ceilings and before/after clock checks do not establish hard execution deadlines, CPU gas equivalence, memory isolation or production tenant containment. Stopping the wait does not cancel underlying host I/O.

From the repository root, in a disposable PowerShell process with project-local caches:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl runtime-java -am test
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" verify
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -Pcoverage verify
```

The opt-in coverage profile uses [JaCoCo 0.8.16](https://www.jacoco.org/jacoco/index.html). It reports actual component coverage in each module's `target/site/jacoco/` directory; it adds no coverage threshold or CI gate. Tests use local fixtures and simulated callbacks, with no live services. The runtime tests include checked-wire/AST mutations, fresh authority, exact traces, Unicode/false/empty values, and native timeout behavior. Shared scenario execution uses the same externally pinned golden bytes as Node; the batch test driver lives only in test sources.

Checkpoint verification on 2026-10-29: the canonical offline clean coverage build passed 31 compiler, 56 runtime and 5 host tests in 35.404 seconds. The combined verifier matched Node's complete normalized output for all 61 shared cases (52 invocations), including exact traces, callback requests, remaining budgets and consumption receipts. Both module suites execute the same compiled two-read fixture for fresh-grant exhaustion, identity/counter reset and first-read failure. The independent host JAR runs all four sample outcomes without compiler/parser classes.

| Component | Covered lines | Covered branches |
| --- | ---: | ---: |
| Compiler | 711/756 (94.05%) | 501/607 (82.54%) |
| Runtime | 697/704 (99.01%) | 600/717 (83.68%) |

Coverage review includes authority, callback failure and timeout paths. Uncovered paths include defensive checks behind preparation invariants, CEL planner/evaluator exception handling and native timeout rearming beyond a 2,147,483,647 ms chunk. Long-duration timer behavior and hard synchronous interruption remain unqualified; these measurements are regression evidence, not containment guarantees.

Effect module checkpoint on 2026-10-30: 95 runtime tests passed independent offline clean coverage verification in 23.05 seconds, preserving all 56 existing read tests. Coverage is 869/875 lines and 808/939 branches. Tests execute the pinned primary artifact and load Node's same compiled handler-output fixture through existing test resources, including exact 8,192-byte success and 8,193-byte rejection with NONE and the original cause retained. They cover native key identity, separate key-label namespaces, prepared reuse, admission/authority rejection, false guards, terminal handlers, trace reservation, winning response evidence before invalid/expired clocks, timeout-first late responses and outer-envelope mutation.

See the [compiler build notes](../compiler-java/README.md) for pinned tooling and POSIX cache isolation.
