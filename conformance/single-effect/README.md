<a id="single-effect-simulation"></a>
# Single-write simulation

Internal engineering reference for a historical simulation. This is the Java-compiled sample for the accepted [single-write contract](../../docs/specs/single-effect.md). Source and model match the [design example](../../docs/internal/single-effect-followup-design.md). It conditionally creates one follow-up after reading task status and project; handlers return terminal typed rejection records. The guarded result is absent from every later expression environment.
[Reproduce the original write simulation artifact](../../docs/internal/implementation-reference.md#compile-original-simulation).

Stdout is the canonical artifact; preserve it as bytes. `artifact.sha256` independently pins the 5,439-byte artifact; `dcm.sha256` pins the 1,721-byte canonical model. Two fresh compiler processes reproduced these bytes, and the original two-argument read CLI still reproduced its golden artifact.

The immutable five primary source/model/artifact files are shared by both runtimes. The [scenarios](scenarios.json) are an ordered array of closed records in the [original simulated-write scenario format](../../docs/internal/implementation-reference.md#serialized-identifiers-and-compiler-selectors). Each record is bounded to 65,536 serialized bytes and four stubs. Its `given` contains only business input and a fixed trusted fixture ID. Expected outcomes, full traces, ordered calls and EVENT_REPORT receipts are hand-authored independently of runtime output.

Run the default source-runtime simulation without installing the example host:

```sh
node conformance/single-effect/run-node.mjs
node --test conformance/single-effect/node.test.mjs examples/node-host/effect-consumer.test.mjs
```

The batch emits `{id,outcome,calls,consumption}` rows in corpus order. The 25 vectors cover creation; status/project no-ops; handled/unhandled read and write rejection; transient/unavailable reads; definite no-effect write failure; unknown/lost response; current authority and original event source; confirmation retained beside clock/return failure; and explicitly simulated duplicate/payload mismatch. Runtime module tests retain the wider admission, deadline-race and size-boundary matrix. The old 61-case read corpus remains separate and unchanged.

The simulator's fixed fixtures own the immutable native request key, event, actor, grants, authorization and logical clock. Node brands an opaque object holding a UUID in a private WeakMap; Java uses the same logical UUID as a native `UUID`. Both adapters must receive their invocation's original native value. Conversion methods deliberately throw in Node. Only the host constructs these values; scenarios cannot select arbitrary paths, callbacks, modes or authority.

| Trusted fixture ID | Host behavior |
| --- | --- |
| `normal` | Actor `user1`, event `event1` from `task1`, both call grants and effect grant 1; logical time 0, expiry 1000. |
| `no-effect-grant`, `no-write-grant` | Narrow only fresh invocation authority; no-op scenarios still complete. |
| `write-denied`, `invocation-denied` | Deny the selected authorization boundary. |
| `invalid-key` | Supply an unbranded string; fail before adapter dispatch. |
| `wrong-origin` | Event source differs from selected task; the host denies write authorization. |
| `read-throws`, `lost-response` | Throw after matching the read/write stub; the latter leaves effect UNKNOWN. |
| `response-expired`, `response-bad-clock` | Set the clock to expiry or -1 before returning the write response; winning evidence remains retained. |
| `trace-after-confirmed` | Admit 13 trace slots: the write confirms, then return tracing exceeds its allowance. |
| `simulated-duplicate`, `simulated-payload-mismatch` | Predetermined fake prior creation confirms the same payload or rejects a changed title. No real ledger or duplicate-safety claim. |

Before consuming each stub, the adapter checks actual capability/version/arguments/protected actor, exact key identity, event and requested-resource relationship. It captures actual call fields independently. After invocation, captures are paired with actual CALL_DISPATCHED entries by ordinal; count, capability and step mismatches fail in an external violation ledger, even if the runtime safely catches an adapter exception. Reported step IDs come only from the trace. Harness regressions include swapped labels at identical read sites; there is no second plan interpreter.

The [packaged Node host](../../examples/node-host/README.md) injects the installed library into that same simulator. The existing contract verifier additionally requires fresh compiler bytes, complete Java/Node rows and both packaged hosts to agree. Local combined verification passes all 25 rows, four compiler processes and four packaged-host batches, preserving the 61 read cases. See [testing evidence](../../CONTRIBUTING.md#testing-principles) for measurements and CI status. All fixtures are fictional; they certify no production persistence, authority, deadline containment, recovery, exactly-once behavior or real-mode admission.
