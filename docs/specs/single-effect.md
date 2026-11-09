<a id="single-effect-profile"></a>
<a id="keir-ir-single-write-behavior"></a>
# Effects and verification scenarios

<a id="effect4-and-legacy-boundaries"></a>
<a id="single-write-behavior-and-compatibility-boundaries"></a>
Event extensions have at most one possible write. Pre-operation decisions have none. See the [Keir specification](README.md#current-behavior) for the current contract and [decision consumption](runtime.md#decision-consumption) for the zero-write consumer.

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

Each `keir-scenario/3` document has exactly `{format,id,given,stubs,expect}`. The `given` object has exactly `{input,fixture}`. The trusted host selects the pinned `keir-artifact/3` and its matching `keir-dcm/3` outside the document. The fixture name resolves in the host's trusted fixture table, which supplies protected context, the clock and deterministic behavior. Expected protected arguments describe what the host should dispatch. The [format matrix](compiler-artifact.md#source-and-artifact-identity) identifies current schemas and the exact older `/2` execution pair.

Read one strict UTF-8 JSON document of at most 65,536 bytes. Reject a BOM, duplicate keys and unpaired surrogates. Containers may nest up to 16 levels, including the root. Numeric metadata uses nonnegative integer tokens up to 9,007,199,254,740,991, with no leading zero except `0`. Scenario and fixture IDs match the full string `[A-Za-z0-9][A-Za-z0-9_.-]{0,63}`. IDs are case-sensitive and unique within a suite. Each document allows up to four stubs and four expected calls.

Check lexical and structural validity first. Then check the selected artifact and model, resolve the fixture, and validate inputs and expectations against the DCM and plan before dispatch. The [conformance index](../../conformance/README.md) identifies actual fixture suites and their versions. Incompatible pairs and invalid expectations must fail before dispatch.

Each stub is `{step,capability,version,args,protectedArgs,response}` in exact dispatch order. `response` is one [valid adapter outcome](runtime.md#adapter-responses); negative protocol tests inject malformed outcomes through the trusted test harness, not a permissive production adapter schema. Before consuming a stub, the test adapter checks actual capability/version and exact agent/protected arguments, and separately checks native key identity and protected origin against the selected fixture. The adapter context has no step label: after execution, reconcile captured calls with the trusted `CALL_DISPATCHED` trace entries by dispatch ordinal, require equal counts and capabilities, and compare each actual trace step with its stub's step. Derive reported `calls.step` from the trace, never from the expected stub or a second plan interpreter. A skipped call consumes no stub. Missing, extra, wrong-order, wrong-step, mismatched, or unconsumed stubs produce a failed simulation assertion outside runtime exception handling, even if execution otherwise returned COMPLETED. Do not turn a stub mismatch into a declared business rejection.

`expect` is exactly `{outcome,calls,consumption}`. Compare the full outcome and trace, ordered calls of shape `{step,capability,version,args,protectedArgs}`, and the entrypoint's consumer result. EVENT_REPORT uses the existing report shape. DECISION uses `{status:ALLOWED,result}`, `{status:DENIED,result}` or exactly `{status:BLOCKED}`; the complete result includes any declared message. Trace sequences start at zero and match the entries' positions. Test clocks and fixture actions model deadlines and lost responses.

For decision integration, observe the native operation independently of `consumption`. DENIED and BLOCKED cause zero native attempts. ALLOWED reaches fresh native validation once and may still fail before commit. Test authoritative no binding separately from a selected-but-unavailable extension, along with changed selection, permissions, proposed input and read facts. Native permissions, persistence, consistency and duplicate handling require separate host evidence. See the [decision acceptance matrix](pre-operation-decisions.md#acceptance-matrix-and-implementation-split).

See [testing evidence](../testing.md) for measured behavior and [earlier qualification](real-single-effect-admission.md#legacy-effect3-qualification) for historical fixtures.
