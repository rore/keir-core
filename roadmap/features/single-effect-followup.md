---
id: single-effect-followup
title: Add a guarded follow-up effect and simulation
status: queued
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

The two core developers cover the portable implementation and samples under bounded manager assignments. Demo development is deferred until useful packaged core behavior is available; real persistence/authorization evidence belongs to [task integration](demo-task-integration.md). The architect owns any new effect/outcome contracts. No implementation is assigned by this plan alone.

### Before implementation

First accept the read-only scenario. Specify when, guarded-result availability and terminal business handlers; conservative counts over all possible writes; effect intent/confirmed/none/unknown records; and failure after an already confirmed effect. No guarded-result access is admitted without a supported presence proof.

Define the original host-key type/source, protected event/source binding, result/argument validation and the isolated simulation admission path. Pending fake guarantees must never be usable to admit real production adapters. Use one possible write and one binding; no mutual-exclusivity analysis or multi-key namespace design is needed.

Use a separately versioned minimal sample DCM whose scalar task projection and create signature do not require dueAt; preserve the original baseline fixture and its compatibility identity. Compare project IDs at their nominal ProjectId type. The target project and title are validated business data, not permissions: the adapter independently authorizes creation, verifies the requested source and protected origin relationship, and applies native invariants. A matching project condition must not become an authorization bypass.

### Acceptance and stop condition

Show qualifying and nonqualifying inputs, handled business rejection, read failure, definite write rejection and lost response on both runtimes. Assert unchanged host-key forwarding and protected origin; show that missing/incompatible key and a second guarded write are rejected. Preserve honest effect receipts if return evaluation fails after a write. Run the first scenario unchanged as regression coverage.

The scenario adapter verifies ordered arguments and expected results/effects, rejects missing/extra/unconsumed stubs, and labels duplicate-delivery behavior as simulated. Measure newly admitted expressions and adapter-budget outcomes separately. If a required effect/key/outcome guarantee cannot be represented, return that specific contract to design; do not hide it in a task-specific policy capability. Stop when this narrow follow-up and failure corpus work; leave broader expressions and real persistence to their owning features.
