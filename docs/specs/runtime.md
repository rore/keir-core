<a id="read-and-decide-runtime"></a>
# Runtime execution and limits

This specification defines Keir runtime behavior for single-write execution. [Host model binding](host-model-binding.md#effect4-binding-and-preparation) defines the Java and Node APIs. Compatibility sections preserve earlier behavior and exact profile identities where required; selecting another profile requires its own contract, with no automatic upgrade.

Implementation evidence belongs in [testing](../testing.md); pending resource qualification belongs in the [resource roadmap](../../roadmap/features/runtime-resource-qualification.md). Neither changes the limits or guarantees specified here.

<a id="corrected-effect4-invocation-boundary"></a>
<a id="effect4-execution"></a>
## Single-write execution

Bind the complete DCM and implementations, then prepare the artifact against that binding. Invocation supplies fresh `bindings`, original non-null `requestKey`, typed `event` and `deadline`. A reached implementation receives the same absolute deadline/clock snapshot alongside `remainingMs`, protected arguments, event and original key. It must recheck expiry before native commit; a remaining duration must not be restarted. Host implementations own current permission and key authenticity. Empty catalogs are valid and mode-neutral; no-call programs can complete without native authorization, while context, input and resource validation still applies.

Execute the validated plan in source order. Validate business input, required protected bindings and event context before executing steps. Validate each reached call's arguments before dispatch and its response before using the result. Customer expressions cannot read or replace the request key or protected bindings. A false guard skips its call. The compiler and preparation count every potential WRITE, including mutually exclusive guards, and reject more than one; zero is valid. A runtime limit or failure stops subsequent steps and dispatches.

An outcome contains `classification`, ordered `trace` and `effects`. Depending on the classification it can also carry a `result`, an `error`, or both for a handled business rejection. The [single-effect outcome contract](single-effect.md#effect-evidence-and-outcomes) defines the permitted combinations. The [effect evidence contract](real-single-effect-admission.md#effect-outcomes-and-key-identity) defines NONE, CONFIRMED and UNKNOWN and the ordering of response and deadline checks. A program-authored result alone cannot establish a native commit. The runtime never retries a failed or uncertain invocation automatically.

The same artifact and exact model can run with separately bound simulation or real implementations. A binding cannot mix modes; simulation does not establish native effects. Prepared handles retain validated program/model state and their binding, not invocation identity, keys or authorization decisions. Every invocation supplies fresh context.

The [limits](#limits) below apply to the current single-write contract. Its [API](host-model-binding.md#api-shapes) takes the host context described above. Deadlines stop asynchronous waiting and prevent subsequent dispatch; they do not preempt synchronous CEL or arbitrary host code.

## Limits

For legacy admission APIs only, optional `admission.limits` is a closed narrowing-only record with optional `steps`, `calls`, `totalAstNodes`, `traceEvents`. Omitted fields retain the profile ceilings below. Values must be integers within 0..8, 0..4, 0..512 and 2..64 respectively; unknown fields or widening fail preparation. Preparation rejects a plan exceeding narrowed static step/call/aggregate-AST limits; runtime also enforces dispatch/step ceilings. Trace quota is enforced during invocation: reserve termination capacity before emitting events and, before adapter dispatch, enough capacity for its dispatch/success events and termination. If capacity is insufficient, terminate with LIMIT_EXCEEDED/`BUDGET_EXCEEDED` before dispatch. Do not drop events or report a completed call whose required trace cannot fit. These four quotas do not add new source/DCM knobs or weaken any other hard cap.

These ceilings are enforced directly by the runtime. Measured performance belongs in the [verification record](../testing.md). Changing a ceiling requires corresponding compatibility rules and fixtures.

| Quantity | Ceiling |
| --- | ---: |
| Raw source / DCM document bytes, each | 65,536 |
| Canonical DCM UTF-8 bytes | 65,536 |
| Artifact bytes | 1,048,576 |
| JSON/YAML structural nesting depth | 16 |
| DCM named types / capabilities / entrypoints | 16 / 8 / 4 |
| Fields per record / parameters per capability / enum cases | 16 / 16 / 16 |
| UTF-8 bytes in each business/model value string | 256 |
| Source steps / total calls | 8 / 4 |
| CEL source bytes per expression | 512 |
| Expanded AST nodes / depth per expression | 64 / 16 |
| Total expression AST nodes per artifact | 512 |
| Decoded descriptors / each CheckedExpr bytes | 65,536 / 16,384 |
| Serialized business input or adapter result bytes | 8,192 |
| Trace events | 64 |

Identifier and metadata strings also obey their schema bounds; artifact base64 and CEL strings use their dedicated bounds. All declared caps are enforced inclusively, with boundary+1 rejection. Limit collection sizes before materializing their contents; bound YAML parsing and protobuf decoding as well as invocation input. No attacker-controlled descriptor recursion or external resolver is allowed. Reject missing required fields and unknown fields; never truncate.

Check remaining deadline before every step, expression and adapter dispatch and after completion; never dispatch a later call after expiry. Host deadline budgets include queueing/preparation if preparation occurs in its request path; prefer prepare once outside it. Adapter and evaluator cancellation are separate. A timer around synchronous CEL or a Java interrupted thread does not demonstrate stopped computation. Document actual qualified enforcement; do not advertise a hard deadline, complete memory isolation or production multi-tenant containment without evidence. This profile promises no CPU gas counter equivalence between libraries. Deterministic clock/budget tests compare behavior; measured wall latency is not a cross-runtime equality assertion.

In-process CEL with enforced checked-AST/data ceilings and before/after deadline checks is permitted; a worker pool is not required. Asynchronous adapter waits, and authorization waits in legacy profiles, must race the remaining deadline and discard late results. This stops waiting, not necessarily the underlying work. It cannot preempt synchronous CEL and establishes no hard-deadline guarantee.

Each invocation uses fresh immutable data. A late adapter response after a deadline is discarded; it cannot append a successful result to a terminated invocation. No invocation state or identity survives in the Prepared handle. Compilation and preparation have host-owned finite time/concurrency budgets too; CLI cancellation must reap its child rather than leave abandoned work.

<a id="legacy-read1-execution-and-profile-extensions"></a>
<a id="implementation-reference-earlier-execution-contracts"></a>
<a id="prepare-and-invoke"></a>
<a id="outcomes-and-trace"></a>

[Earlier runtime execution contracts](../internal/compatibility-reference.md#earlier-runtime).
