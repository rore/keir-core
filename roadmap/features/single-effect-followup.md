---
id: single-effect-followup
title: Add a guarded follow-up effect and simulation
status: done
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Extend the working read-and-decide demonstration's compiler/artifact/runtimes to handle an admitted post-commit task-completion event: read task facts, decide whether a follow-up is needed, and execute one approved follow-up creation. Simulate the exact artifact and display confirmed, skipped, rejected and uncertain outcomes.

## Why

The second runnable increment adds an effect without losing the first end-to-end scenario. It exercises Keir's effect policy, protected host metadata, typed errors and honest uncertainty before a real task application's persistence is involved.

## In Scope

One bounded conditional effect and terminal typed business-error handling. The sample reads a task, tests its actual status and project against the invocation's selected project, then creates a follow-up with an ordinary bounded title. Use actual domain fields, not a new adapter that precomputes the customer's rule. Preserve original host-key pass-through, explicit effect/call grants, receipts and deterministic scenario adapters using the same artifact/runtime path. Update specs, schemas, conformance and both agent guides.

The sample host supplies the trusted invocation and original key. An adapter's uniqueness/idempotency behavior must be explicit; a preceding search does not establish duplicate suppression. Explain the difference between execution IDs and downstream API keys. A timeout is unknown unless an adapter establishes a definite no-effect result. The original task completion cannot be retroactively denied.

## Out of Scope

Multiple mutating calls, collection macros/search/date calculations, automatic replay, durable scheduling/recovery, rollback across systems, claiming fake-adapter certification, full tenant endpoint support and production activation. The baseline tag/search/168-hour program remains an explicit [later expression enhancement](enrich-followup-expressions.md), not a claim made by this smaller demonstration. Do not change the accepted original-key contract to simplify the sample.

## Done When

- The same source compiles once and executes identical artifact bytes in Java and Node for follow-up-created and no-op scenarios.
- Eligibility comes from source-authored conditions over real scalar task fields. A nonmatching status or project skips the write; changing that source condition needs no new customer-specific adapter.
- Scenario simulations use that same artifact and runner, compare expected calls/results/effects and make mismatches visible.
- Both adapters receive the original typed host key unchanged; source cannot select or override it.
- Business rejection, read failure, rejected write and lost-response cases produce the declared outcomes; an unknown write is not called harmless failure or success and no subsequent effect runs.
- A pre-commit guard cannot call the mutating capability. Exceeded call/effect grants and invalid host-key contracts are rejected before dispatch.
- The first advisory read-only corpus still passes, and all demonstrated semantics are reflected in maintained specs and cross-runtime conformance.

## Notes

Second runnable increment; depends on [the read-only path](portable-core-first-slice.md) and the additional measured profile/contract support it requires. It is deliberately smaller than the baseline invoice example. List macros, search and time arithmetic are introduced by their own later demonstration and evidence.

Core implementation and packaged samples are complete. Real persistence, authorization and admission evidence belongs to [task integration](demo-task-integration.md); the native demo app has not started. Further effect contracts remain separate work.

### Accepted contract

Product review on 2026-10-31 identified mandatory per-hook capability grants as an incorrect default. The [generic contract correction](generic-hook-capability-contract.md) owns the revised design and implementation. The accepted checkpoint below records what passed, not acceptance of that assumption as the intended extensibility model.

The [effect specification](../../docs/specs/single-effect.md) and [design rationale](../../docs/internal/single-effect-followup-design.md) define the supported delta: a separate profile, a prior Boolean guard, no guarded-result references, terminal business handlers, conservative one-write authority and retained receipts. Winning response evidence settles before completion checks; a prior timeout winner ignores late responses.

The separate scalar model preserves the baseline and read compatibility identities. Project equality and title are business data; adapters independently enforce native authorization and protected origin. Both adapters receive the original typed host key outside CEL. Real admission is rejected; simulations certify no production adapter.

### Completion evidence

Accepted on 2026-10-30 at implementation commit `ff69257`. The [product CI check](https://github.com/rore/keir-core/actions/runs/41113166355) passes compilation, both runtime suites, packaged-host tests and full transport parity. The [read demonstration](portable-core-first-slice.md) remains unchanged. This completes the first milestone's controlled demonstrations; real task integration stays separate.

- 164 Java tests (47 compiler, 95 runtime, 22 host) pass a clean coverage build in 33.57 seconds. Default Node regression passes 274 tests in 2.05 seconds; both installed-host tests pass in 0.50 seconds.
- Four fresh compiler processes reproduce the 5,439-byte effect artifact and 2,874-byte read artifact. Both runtimes and all four packaged-host batches match 25 effect scenarios and 61 read cases/52 invocations, including full outcomes, ordered calls, traces and consumption.
- Source review and regressions cover key-contract namespaces, original native identity, authority rejection before dispatch, exact handler result limits, deadline winner races, retained confirmation after failure and rejection provenance independent of handler labels. Simulation step IDs derive from actual dispatch traces; an external mismatch ledger catches swallowed adapter errors.
- [Testing evidence](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/testing.md) records coverage and package boundaries. Both runtimes consume the same supplemental Java-compiled handler fixture. Installed Node source matches the six-file archive with verified integrity; the Java host excludes compiler/parser dependencies.

The core developers completed their bounded assignments. No demo-app implementation is included. Real-mode admission, persistence, certified authorization/uniqueness, durable replay, exactly-once effects and hard synchronous resource containment remain outside this checkpoint.

### Retained regression obligations

Show qualifying and nonqualifying inputs, handled business rejection, read failure, definite write rejection and lost response on both runtimes. Assert unchanged host-key forwarding and protected origin; show that missing/incompatible key and a second guarded write are rejected. Preserve honest effect receipts if return evaluation fails after a write. Run the first scenario unchanged as regression coverage.

The scenario adapter verifies ordered arguments and expected results/effects, rejects missing/extra/unconsumed stubs, and labels duplicate-delivery behavior as simulated. Measure newly admitted expressions and adapter-budget outcomes separately. If a required effect/key/outcome guarantee cannot be represented, return that specific contract to design; do not hide it in a task-specific policy capability. Stop when this narrow follow-up and failure corpus work; leave broader expressions and real persistence to their owning features.
