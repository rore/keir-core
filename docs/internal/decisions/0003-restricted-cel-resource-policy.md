# 0003: Restricted CEL and resource containment

Date: 2026-10-29

Status: restricted CEL selected for the POC; exact profile limits and runtime containment remain provisional.

## Decision

Keep CEL as Keir's pure expression evaluator. The Java compiler checks expressions and emits pinned `CheckedExpr` protobuf plus descriptors. Both initial-scope runtimes, Java and Node, must accept the same versioned Keir CEL profile and produce equivalent observable behavior. Capability calls and host adapters remain Keir operations outside CEL; expressions cannot perform I/O or call tenant endpoints.

Define a versioned profile (working name **Keir CEL/1**) in the compiler and bind its identity, permitted AST nodes/overloads, type environment and limits into the compiled artifact. Runners verify the trusted artifact and enforce input, output and invocation bounds. Neither the CEL name nor a checked protobuf alone proves bounded execution or cross-language parity.

## Research findings and evidence limits

- In CEL-Java 0.14.0, `comprehensionMaxIterations` limits total comprehension iterations, including nested loops, when explicitly configured. Its default is `-1`, which disables that check. The option does not bound every costly built-in or establish a wall-clock deadline; the documented guard is not supported by the async interpreter. [Pinned Java options source](https://github.com/cel-expr/cel-java/blob/v0.14.0/common/src/main/java/dev/cel/common/CelOptions.java).
- In CEL-ES 0.6.1, the inspected `EvalFold` materializes the input list/map keys and iterates without a comparable meter or cancellation check in that loop. This is a finding about the pinned implementation, not every CEL implementation. [Pinned Node evaluator source](https://github.com/bufbuild/cel-es/blob/v0.6.1/packages/cel/src/planner.ts).
- The existing Java-to-Node checked-expression CI covers a limited admitted subset, not worst-case resource use or the full Java and Node runners. CEL-ES plans from the expression AST using its own environment; compiler and runner versions, descriptors, overloads and conformance fixtures must stay pinned. [Prior evidence register](../design-baseline/public/decisions-and-evidence.md).
- The supplied research reports a local Node worker experiment using a synthetic 64-`BigInt` calculation and an infinite JavaScript loop. It reports approximately 28 ms cold startup, 45 µs median warm round trip and successful termination. The experiment did **not** run CEL, and its script/results were not supplied with the research reviewed for this decision. Treat these numbers as unverified context, not a CEL benchmark or acceptance threshold.

## Proposed first profile

Admit checked, typed Boolean logic, conditionals, record access with explicit presence handling, comparable values, exact signed int64, UTC instants and elapsed durations. Admit `exists`, `all`, `filter` and `map` only over bounded inputs after their actual worst-case cost is established. Keep all capability calls outside expressions.

Initially reject `dyn`, arbitrary custom functions or overloads, regex, unrestricted maps, unknown AST nodes, unbounded collections and nested comprehensions in repeated loop bodies. The compiler must inspect the checked AST rather than filter source text. DCM-declared limits on list items, string length, record depth and output size must feed a conservative bound across **all** expressions in an invocation, including built-in costs and intermediate allocations. Fail closed when a needed bound is unknown.

The research suggests **64 items per list** and **120 expanded AST nodes per expression** as starting measurement knobs. They are not adopted limits and do not establish CPU safety. Calibrate them with real workloads and adversarial expressions before freezing the profile.

## Runtime containment

For Node, investigate prewarmed `worker_threads` evaluating only pure, approved expressions from bounded typed inputs. The parent runner retains all capability calls and effects. On a deadline, terminate and discard the worker, report a typed resource failure, and preserve any earlier confirmed effect in the trace. Test worker overhead, termination and memory with actual CEL-ES expressions. Node's worker `resourceLimits` affect the JS engine and are not complete process-memory isolation. [Node worker documentation](https://nodejs.org/api/worker_threads.html).

For Java, explicitly set the comprehension iteration guard and enforce the same static profile and runtime data bounds. A Java thread timeout or interruption alone does not prove that CPU-bound evaluation stopped. If a hard deadline is required, prove metering or isolation with actual CEL-Java evaluation. The two runtimes may use different containment mechanisms but must agree on accepted expressions, values and error outcomes.

For both runtimes, keep expression deadlines distinct from host-adapter network deadlines. A pure-expression timeout does not roll back a preceding confirmed capability effect. Host hook fallback remains a host-defined contract.

## Gates before production claims

1. Run admitted and adversarial expressions with the pinned CEL-ES evaluator in a worker at input sizes including 1, 16, 64 and an intentionally rejected larger case. Measure latency distribution, memory and termination.
2. Exercise AST/profile bypass attempts, nested macros, unknown overloads, huge strings, deep structures, forged artifacts and aggregate invocation budgets.
3. Execute the same admitted artifact corpus in the actual Java and Node runners. Compare values, errors, limits and termination classes, including dependency upgrades.
4. Test slow real adapters and previous confirmed effects separately from pure expression evaluation.
5. Decide and document the accepted profile, numeric bounds and hard-deadline guarantees only after those tests. Keep production multi-tenant containment claims blocked until then.

This decision supersedes the baseline's implication that resource bounds can be assumed from finite list length or AST size. The baseline remains a historical design input.
