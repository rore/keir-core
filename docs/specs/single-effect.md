<a id="single-effect-profile"></a>
<a id="keir-ir-single-write-behavior"></a>
# Single-write behavior

<a id="effect4-and-legacy-boundaries"></a>
<a id="single-write-behavior-and-compatibility-boundaries"></a>
See the [Keir specification](README.md#current-behavior) for the single-write contract.

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

Each [`keir-scenario/1` document](../../schemas/scenario-1/scenario.schema.json) has exactly `{format,id,given,stubs,expect}`. The `given` object has exactly `{input,fixture}`. The trusted host selects the pinned `keir-artifact/1` and its matching `keir-dcm/1` outside the document. The fixture name resolves in the host's trusted fixture table, which supplies protected context, the clock and deterministic behavior. Expected protected arguments describe what the host should dispatch.

Read one strict UTF-8 JSON document of at most 65,536 bytes. Reject a BOM, duplicate keys and unpaired surrogates. Containers may nest up to 16 levels, including the root. Numeric metadata uses nonnegative integer tokens up to 9,007,199,254,740,991, with no leading zero except `0`. Scenario and fixture IDs match the full string `[A-Za-z0-9][A-Za-z0-9_.-]{0,63}`. IDs are case-sensitive and unique within a suite. Each document allows up to four stubs and four expected calls.

Check lexical and structural validity first. Then check the selected artifact and model, resolve the fixture, and validate inputs and expectations against the DCM and plan before dispatch. The [shared conformance fixtures](../../conformance/keir-1/README.md) exercise this order, including incompatible formats and invalid expectations that must fail before dispatch.

Each stub is `{step,capability,version,args,protectedArgs,response}` in exact dispatch order. `response` is one [valid adapter outcome](runtime.md#adapter-responses); negative protocol tests inject malformed outcomes through the trusted test harness, not a permissive production adapter schema. Before consuming a stub, the test adapter checks actual capability/version and exact agent/protected arguments, and separately checks native key identity and protected origin against the selected fixture. The adapter context has no step label: after execution, reconcile captured calls with the trusted `CALL_DISPATCHED` trace entries by dispatch ordinal, require equal counts and capabilities, and compare each actual trace step with its stub's step. Derive reported `calls.step` from the trace, never from the expected stub or a second plan interpreter. A skipped call consumes no stub. Missing, extra, wrong-order, wrong-step, mismatched, or unconsumed stubs produce a failed simulation assertion outside runtime exception handling, even if execution otherwise returned COMPLETED. Do not turn a stub mismatch into a declared business rejection.

`expect` is exactly `{outcome,calls,consumption}`. Compare the full outcome and trace, ordered calls of shape `{step,capability,version,args,protectedArgs}`, and the event report. Trace sequences start at zero and match the entries' positions. Test clocks and fixture actions model deadlines and lost responses. Native permissions, persistence and duplicate handling require separate host evidence.

See [testing evidence](../testing.md) for measured behavior and [earlier qualification](real-single-effect-admission.md#legacy-effect3-qualification) for historical fixtures.
