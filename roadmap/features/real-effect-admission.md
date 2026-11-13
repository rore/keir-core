---
id: "real-effect-admission"
title: "Bind hosts for real single effects"
status: "done"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-core"
---

## Summary

Bind a host DCM and complete compatible implementations, prepare an extension, and execute at most one real WRITE with fresh context and honest outcomes. The current host API needs no deployment credential. Supported source/model/artifact tuples follow the [compiler and runtime compatibility matrix](../../docs/specs/compiler-artifact.md); earlier internal profile names below identify historical evidence, not current public format labels.

**Delivered correction:** The [audit](../../docs/internal/real-application-design-audit.md) and [host-contract alignment](../../.agent-workflow/tasks/host-contract-alignment.md) led to the reviewed simple bind/prepare/invoke boundary and explicit host-owned authority. Compiler/Java/Node correction, shared and installed conformance, hosted CI and the native demo integration are complete, as recorded below and in [real task integration](demo-task-integration.md). The [Core 0.3.1 release record](../../docs/releases/core-0.3.1-candidate.md) supplies the later package/compatibility evidence. No corrective implementation assignment or deployment-report gate remains open on this card.

## In Scope

- Define evidence identity/version/scope, current authority, native key compatibility, payload mismatch/retention and honest commit/unknown mapping.
- Choose a compatibility boundary explicitly; never reinterpret an existing simulation assurance or enable real mode with an unchecked flag.
- Implement compiler/admission/runtime checks in Java and Node; use concrete native adapter requirements from the demo while keeping core domain-independent.

## Out of Scope

Multiple writes, self-certifying declarations, exactly-once claims, automatic retries and general production certification.

## Done When

- [x] Design/spec: corrected host responsibilities and the simple initialization API are reconciled with the baseline and pass independent plan/result review.
- [x] Implementation: both runtimes admit only the selected supported real profile and reject missing, stale or incompatible evidence.
- [x] Verification: shared conformance covers original-key identity, authority, definite NONE/CONFIRMED versus UNKNOWN, deadlines and rejection before dispatch.

## Notes

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Native contracts and evidence, sections 3 and 8](../../docs/internal/design-baseline/public/domain-model-v0.md); [Host integration, sections 5-7 and 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Keys and admission, sections 3-7](../../docs/internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md).

The checkpoints below preserve the earlier prototype-to-correction sequence and its then-pending gates. The accepted completion at the end supersedes those pending statements; neither rich collections nor AI is a prerequisite to executing a supported approved artifact.

**Historical effect/3 contract:** [Real single-effect admission](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/specs/real-single-effect-admission.md) and [native handoff](../../docs/internal/real-task-effect-handoff.md) pass independent architecture review, including the frozen host clarification delta. This selects a new effect/3 boundary without changing effect/2. The concrete source/test plan under [the implementation record](../../.agent-workflow/tasks/real-single-effect-admission.md) passes independent review and implementation is underway. Final combined conformance, packaged verification and independent result review remain. Design and plan approval do not qualify production use or the native host.

**Historical effect/3 result gate:** Corrected core edb43cb passes independent delta result review, reusing the accepted unchanged broad review and measured Java/Node/source/installed conformance. The safe-subset repair preserves ordinary async cleanup and strict authority with explicit unsupported host async limits. Manager accepts exact core integration and verified package consumption by the isolated demo implementation. Main delivery/hosted CI and recoverable cleanup remain; no native qualification is inferred.

**Historical effect/3 integration:** Core d59e375 is pushed to main with all 52 reviewed source blobs preserved exactly. [Contract CI](https://github.com/rore/keir-core/actions/runs/41873759463) and [workflow CI](https://github.com/rore/keir-core/actions/runs/41873759417) pass; exact runtime/compiler package copies are verified in the isolated demo. Core implementation delivery is accepted. Recoverable checkout cleanup is released and remains the final housekeeping item. Native host qualification is separate.

**Corrected integration:** Effect/4 source `95e5fd0` passes independent result and simplicity review, reusing existing engines and preserving legacy APIs. Reported final verification passes 271 Java and 454 Node tests, shared same-artifact/source-installed conformance and exact package/class checks. Manager verified all 61 integrated source blobs and stages exact packages for isolated demo consumption. Hosted CI and recoverable cleanup remain; actual native application and customer-journey acceptance belong to the demo feature.
**Complete:** The reviewed correction is integrated, both hosted CI jobs pass, exact packages execute the demo's 50 native/runtime cases, and task-worktree cleanup is independently verified with complete recoverable source/evidence preserved. Actual customer authoring and UX acceptance remain owned by [real task integration](demo-task-integration.md).