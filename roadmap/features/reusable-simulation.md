---
id: "reusable-simulation"
title: "Offer reusable simulation and host-conformance tools"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-core"
---

## Summary

Reusable read-only tooling and Daymark's actual effect/decision simulation are delivered. The remaining scope is a reusable current-contract adapter/consumer/report seam, broader non-task examples in both runtimes and a native host-conformance kit. This queued card is not a claim that Daymark lacks simulation, independent expectations or same-artifact native execution.

## In Scope

- Define a versioned, bounded data-only scenario contract, deterministic adapters, structured mismatches and consumer-result output.
- Reuse the existing interpreter and diagnostic model; protected context/fixture selection remains host-owned, never scenario authority.
- Provide a host-conformance kit for native context and consumer checks; actual database/service guarantee tests remain host-owned.

## Out of Scope

A production adapter certification service or a mandatory replacement for the already working minimal harnesses.

## Done When

- [x] Delivered read-only tooling: versioned case/report schemas, host-owned authority, ordered stubs, independent expectations and bounded redacted reports have recorded packaged Java/Node parity and rejection evidence for that historical contract.
- [x] Delivered Daymark integration: current task simulation and native execution use the same compiled artifact and dependency identities, with independent expectations and honest created/no-op/unknown outcomes. This is host integration evidence, not a generic conformance kit.
- [ ] Current reusable seam: establish the remaining effect/decision adapter, consumer and report tooling against the current scenario contract, retaining explicit authority/redaction boundaries. Daymark-only tooling and historical read-only APIs do not by themselves close this obligation.
- [ ] Broader proof and kit: run task and non-task examples through reusable Java/Node tooling and provide native-context/consumer conformance checks. Verify same-artifact parity, missing/extra/out-of-order stubs, invalid authority, typed consumer failures and redacted reports for that current seam; real service/database guarantees stay host-owned.

## Notes

The residual reusable-seam/non-task/conformance-kit work is unassigned. Keep it queued until a bounded task is selected; do not repeat the delivered Daymark workflow to recreate evidence.

**Daymark simulation delivered:** [Rich task workflows](rich-task-customizations.md) record identical compiled programs in simulation and against native rows. The [current verification guide](https://github.com/rore/keir-demo-app/blob/main/tests/README.md) identifies independent task-effect expectations, actual runtime/native checks and the separate scripted-provider/UI-mock boundaries. Current scenario structure is in the [format matrix](../../docs/specs/compiler-artifact.md#source-and-artifact-identity). None of this establishes the broader generic seam or a second-host conformance kit by implication.

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**First increment delivered:** The independently reviewed [historical read-only simulation contract](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/specs/read-simulation.md) and [operator guide](../../docs/agents/read-simulation.md) preserve data-only cases, host-owned authority, strict ordered responses, independent compact expectations and bounded redacted reports through the same Java and Node artifact/runtime. These references are explicitly compatibility/history material, not today's normative effect contract. Revision `8e8f3c7` includes the [exact-artifact example](../../conformance/read-simulation/README.md). The [Work Record](../../.agent-workflow/tasks/read2-reusable-simulation.md) retains its original review, correction and delivery evidence.

**Verification:** Corrected candidate `8a5eeec`, source `8c63aa0`, passed targeted independent result review with no residual blockers. Verification passes 231 Java tests, 390 Node tests, nineteen canonical packaged report comparisons, twenty-nine schema documents and standalone package execution. Trace-free expectations, safe native summaries, BOM parity and distinct business versus host denial are covered. Hosted [product CI](https://github.com/rore/keir-core/actions/runs/41414555171) and [workflow CI](https://github.com/rore/keir-core/actions/runs/41414555168) pass. Source/proof/fixture identity, privacy and native/old-golden compatibility are verified. The size finding remains advisory. The released task checkout and merged branch were removed after verified recovery archival.

**Remaining:** A completed current-contract reusable adapter/consumer/report seam is not established by the retained historical tooling and Daymark-specific checks alone. Broader non-task examples and the reusable host-conformance kit remain explicit residuals. This does not reopen delivered task simulation; simulated PASS still cannot certify real adapters, callback purity or hard resource containment.

**Design input:** [Scenarios and test layers, sections 2-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Host conformance, section 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).
