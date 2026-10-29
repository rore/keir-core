# Read-and-decide runtime

Status: implementation candidate. Java and Node share this behavior and [artifact contract](compiler-artifact.md). APIs below define data/ownership semantics; idiomatic Java futures and Node promises may implement the same asynchronous adapter boundary.

## Prepare and invoke

`prepare(artifactBytes, admission, adapters) -> Prepared | Diagnostic` validates the artifact before resolving adapters and preparing CEL. Admission contains trusted `expectedArtifactDigest`, `expectedDcmDigest`, `entrypoint`, expected `consumer`, supported compiler/environment pins and host restrictions. Adapter registry is keyed by exact capability ID/version and separately marks simulation versus real-host registration. Preparing does not call an adapter. A Prepared handle is immutable and caches only plan/environment, not user identity or mutable invocation data.

`invoke(prepared, businessInput, trustedInvocation) -> Outcome` gets a fresh closed input record and trusted `{bindings,deadline,authorize}` context. `bindings` supplies declared protected scalar parameters by name; `deadline` uses the host monotonic clock; `authorize` checks current invocation/capability scope immediately before each dispatch. The host constructs this context; it is not deserialized verbatim from customer JSON. No CEL variable exposes it. Validate input, host bindings and the current host grant before any call. A host may narrow grants, never widen artifact/DCM rights.

Adapter call is `invoke(agentArgs, protectedArgs, remainingDeadline) -> AdapterOutcome`, with separately validated immutable argument records. Do not merge into a caller-controlled object. The adapter still checks current object/resource access and must enforce its actual I/O deadline. Generic runtime cannot prove that a task service returned the requested task or that its authorization implementation is correct.

Adapter outcomes: `{kind:"SUCCESS",value:record}`, `{kind:"BUSINESS_REJECTION",code:declaredCode}`, or `{kind:"TRANSIENT_FAILURE"}`. Undeclared codes, invalid shapes, extra fields, uncertain/effectful responses or thrown exceptions produce `FAILED` with `ADAPTER_INVALID` or `ADAPTER_EXCEPTION`, never an ordinary result. No automatic retry or handler. READ operations must not claim mutation evidence; uncertainty about a write is outside this profile.

## Outcomes and trace

Outcome is `{classification,result?,error?,trace}`. Classification is `COMPLETED`, `BUSINESS_REJECTION`, `FAILED`, or `LIMIT_EXCEEDED`. Only COMPLETED carries `result`, exactly the output record. All other classifications carry `error:{code,path}` and no result. Business rejection uses its declared business code; transient read failure uses `ADAPTER_TRANSIENT`; scope denial uses `AUTHORITY_DENIED`; validation uses `INPUT_INVALID`, `HOST_BINDING_INVALID`, or `ADAPTER_INVALID`; expression evaluation errors use `EXPRESSION_FAILED`; limits use `BUDGET_EXCEEDED`. No host exception text is returned. Loader/compile failures are diagnostics, not execution outcomes. Runtime paths use escaped JSON pointers: `/input` (optionally a safe schema field), `/hostBindings` (no protected values), `/steps/<zero-based-index>` or `/return`; invocation-wide failures use the empty pointer. Loader paths are rooted at `/artifact` or `/admission`.

Trace is an ordered array of `{sequence,event,step?,capability?,code?}`; sequence starts at 0. Emit `INVOCATION_START`, then `STEP_START` for each reached step; for CALL emit `CALL_DISPATCHED` only after authorization/validation and immediately before dispatch, then `CALL_SUCCEEDED` on valid success. For successful BIND emit `VALUE_BOUND`. Finish with `RETURNED` then `TERMINATED` on success, otherwise `TERMINATED` with error code. No later step starts after failure. No values, arguments, host bindings, raw CEL, credentials or exception stacks appear. A trusted conformance adapter records exact arguments separately for assertions. Trace capacity is bounded and exceeding it terminates instead of silently dropping audit events; reserve the terminal event slot.

The host consumer receives only an outcome from its selected invocation, validates output type/classification and displays a result only on COMPLETED. Consumption receipt is separately `{status:"DISPLAYED",result:record}` or `{status:"NOT_DISPLAYED",code:string}`. It is not a runtime event or evidence of a native transaction.

## Limits

These are conservative **candidate admission ceilings**, not measured performance guarantees. Qualification must confirm or revise them before the profile is accepted; a change alters the profile/environment contract and fixtures. Implementations must not accept unbounded values while evidence is pending.

| Quantity | Candidate ceiling |
| --- | ---: |
| Raw source / DCM bytes, each | 65,536 |
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

Check remaining deadline before every step, expression and adapter dispatch and after completion; never dispatch a later call after expiry. Host deadline budgets include queueing/preparation if preparation occurs in its request path; prefer prepare once outside it. Adapter and evaluator cancellation are separate. A timer around synchronous CEL or a Java interrupted thread does not demonstrate stopped computation. Pure Node expression workers may supply termination, but must not own adapter authority. Document actual qualified enforcement; do not advertise a hard deadline, complete memory isolation or production multi-tenant containment without evidence. This profile promises no CPU gas counter equivalence between libraries. Deterministic clock/budget tests compare behavior; measured wall latency is not a cross-runtime equality assertion.

Each invocation uses fresh immutable data. A late adapter response after a deadline is discarded; it cannot append a successful result to a terminated invocation. No state/identity survives in the Prepared handle. Compilation and preparation have host-owned finite time/concurrency budgets too; CLI cancellation must reap its child rather than leave abandoned work.
