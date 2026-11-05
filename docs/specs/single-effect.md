<a id="single-effect-profile"></a>
<a id="keir-ir-single-write-behavior"></a>
# Single-write behavior

<a id="effect4-and-legacy-boundaries"></a>
<a id="single-write-behavior-and-compatibility-boundaries"></a>
See the [current specification](README.md#current-profile) for the single-write contract. This page keeps the earlier reference links and the shared simulation protocol.

<a id="source-and-definite-availability"></a>
[Source steps, guards and data flow](language.md#steps-and-data-flow)

<a id="closed-dcm-additions"></a>
[Entrypoint and native-key declarations](domain-model.md#entrypoints)

<a id="artifact"></a>
[Lowered steps and expressions](compiler-artifact.md#lowered-steps-and-expressions)

<a id="host-api-and-native-key"></a>
[Host API](host-model-binding.md#api-shapes) · [Original request key](host-model-binding.md#request-key)

<a id="effect-evidence-and-outcomes"></a>
[Adapter responses](runtime.md#adapter-responses) · [Write evidence](runtime.md#write-evidence) · [Outcome](runtime.md#outcome)

<a id="trace-and-consumer"></a>
[Trace and event report](runtime.md#trace-and-event-report)

<a id="versions-and-inherited-rules"></a>
<a id="implementation-reference-inherited-source-and-format-rules"></a>
[Original simulated-write source and artifact](../internal/history/compatibility-reference.md#original-write-formats) · [Frozen shared single-write contract](https://github.com/rore/keir-core/blob/57a74a57e6c5fec2e74d3561d4b2ad1da2e650c1/docs/specs/single-effect.md)

## Ordered simulation protocol and acceptance

This section describes repository conformance data. Use the [scenario format for the selected artifact](../internal/implementation-reference.md#serialized-identifiers-and-compiler-selectors). Fixtures provide protected inputs through the host API for that artifact.

Run the exact compiled bytes through the ordinary prepare and invoke path with explicit simulation registrations and the EVENT_REPORT consumer in both runtimes. A row has exactly `{format,id,given,stubs,expect}`.

| Conformance harness | Exact `given` fields |
| --- | --- |
| [Original simulated write](../../conformance/single-effect/README.md) | `{input,fixture}` |
| [Generic catalog-bound, qualified-write and current single-write batches](../../conformance/generic-hooks/README.md) | `{input,fixture,program}` |

`input` is the business input. `fixture` names a trusted test-host fixture that supplies protected context and deterministic behavior. In the generic harness, `program` selects a fixed, preloaded program. The current batch recognizes `main`, `zero`, `after-write-read`, and `no-call`. These names are test data. Hosts select approved artifacts independently and pass the prepared handle to the runtime API. Scenario data supplies no filesystem paths, executable callbacks, authority, or protected inputs.

The current generic batch accepts one to 128 rows in at most 1 MiB. Row IDs are unique. Each row is limited to 65,536 serialized bytes, four stubs and four expected calls.

Each stub is `{step,capability,version,args,protectedArgs,response}` in exact dispatch order. `response` is one [valid adapter outcome](runtime.md#adapter-responses); negative protocol tests inject malformed outcomes through the trusted test harness, not a permissive production adapter schema. Before consuming a stub, the test adapter checks actual capability/version and exact agent/protected arguments, and separately checks native key identity and protected origin against the selected fixture. The adapter context has no step label: after execution, reconcile captured calls with the trusted `CALL_DISPATCHED` trace entries by dispatch ordinal, require equal counts and capabilities, and compare each actual trace step with its stub's step. Derive reported `calls.step` from the trace, never from the expected stub or a second plan interpreter. A skipped call consumes no stub. Missing, extra, wrong-order, wrong-step, mismatched, or unconsumed stubs produce a failed simulation assertion outside runtime exception handling, even if execution otherwise returned COMPLETED. Do not turn a stub mismatch into a declared business rejection.

`expect` is exactly `{outcome,calls,consumption}`; outcome includes exact classification/result/error/effects/trace, calls is the ordered array of `{step,capability,version,args,protectedArgs}`, and consumption is the exact consumer receipt. Exclude real wall times. Harness diagnostics identify the mismatching field without leaking protected production values. Test clocks and fixture actions model timeout, denial and lost response without sleeps or live I/O. The original simulated-write corpus includes a simulated duplicate. Native duplicate handling and retention require host evidence.

The [shared conformance corpus](../../conformance/generic-hooks/README.md) and [testing evidence](../testing.md) identify the versioned vectors. Preserve earlier profile regression fixtures and packaged-host behavior. Simulation does not establish real persistence, authority implementation, duplicate suppression, hard deadlines, exactly-once execution or recovery. Host implementations own native guarantees. Earlier qualified-write artifacts retain their [qualification protocol](real-single-effect-admission.md#legacy-effect3-qualification).
