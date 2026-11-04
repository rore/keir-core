---
id: "demo-task-integration"
title: "Run a real single-effect extension after task completion"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Connect an explicitly admitted scalar extension to native TaskCompleted events and generic task creation, preserving actual authority and effect evidence.

Current priority: replace the invented task-advice demonstration with recognizable task-management customization, beginning with completion-driven task creation. Connect the reviewed native foundation, core real-effect admission and actual Studio journey. Later generic operations should follow actual task-model needs. Preserve existing customer data/history during the transition; do not merely rename advice or claim planned behavior works.

**Current assignment:** Completing a task in a customer-chosen existing project creates a task with a customer-chosen title through generic capabilities. The native event/receipt foundation is reviewed, integrated and verified in hosted CI. The concrete [actual journey integration plan](../../.agent-workflow/tasks/real-task-customization-integration.md) is complete and undergoing independent technical review before implementation release. Host-qualification setup and interface decisions are accepted, including a minimal demo-owned simulation fixture adapter over public runtime APIs; core effect/3 implementation proceeds independently. Existing approvals remain confined to their original read-only behavior.

## In Scope

- Commit native completion before admitting a reaction; pin artifact/model/consumer selection once and preserve the original event/key.
- Define durable admission, restart/redelivery, event ordering, current authorization, causation/re-entry and old-version retention.
- Select bindings within the originating user's scope. Persist trusted event ownership independently of the current browser login, so switching users cannot change an admitted event's scope or selected program.
- Keep tasks.create generic: project/title come from source. Test real persistence and operation-key idempotency, with any business uniqueness specified separately.

## Out of Scope

Multiple writes, implicit source-task relationships, derived downstream keys, automatic replay and changing SIMULATION_ONLY by assertion.

## Done When

- [x] Design/spec: settle native event/key/dedupe/retention and selection contracts against the core real-effect admission requirements.
- [ ] Implementation: a reviewed scalar program creates or skips a task against actual services using pinned packages; native completion survives reaction failure.
- [ ] Verification: concurrent duplicates, payload mismatch, revoked access, restart, changed selection, lost response and UNKNOWN have correct persistent outcomes; no blind replay.
- [ ] User scope: queue an event for user A, switch the browser to B, then process/redeliver A's event. It retains A's pinned selection and current dispatch authorization, cannot select B's customization and cannot access B's data.

## Notes

**Dependencies:** [native-task-app](../features/native-task-app.md); [real-task-reads](../features/real-task-reads.md); [real-effect-admission](../features/real-effect-admission.md).

**Design input:** [Post-commit events and consumers, sections 7, 12 and 13C](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Original keys and evidence, sections 3-7](../../docs/internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md); [First task integration, sections 3-4](../../docs/internal/design-baseline/public/task-management-poc.md).

This preserves the existing feature ID. Its former broad obligations have explicit owners: native CRUD -> native-task-app/task-organization; packaged real reads -> real-task-reads; effect concurrency/replay/authorization -> this card; enforced DENY -> task-create-guard; review/activation UI -> extension-approval-lifecycle. Core and native harnesses can verify their contributions independently, but the current delivery includes the actual customer authoring, checking, approval, activation and visible task-outcome journey. Richer syntax remains separate scope.

The independently reviewed [host handoff](../../docs/internal/real-task-effect-handoff.md) fixes event occurrence, original-key, atomic receipt, selection/revocation and legacy-preservation rules. Foundation tests verify permanent revocation across reactivation, retained confirmed outcomes and actual SQLite process-crash/concurrency behavior. Runtime and deployment qualification remain separate acceptance gates; no real native effect has been enabled.

The bounded [native foundation](../../.agent-workflow/tasks/native-task-effect-foundation.md) is delivered: schema8 preserves legacy selection/data/accounting and provides dormant event/receipt services, without a public runner or effect activation. Full task/Studio acceptance follows qualified integration of both foundations.

**Native foundation integrated:** Demo f02932b adds the dormant schema8 event/key/receipt and hook-selection foundation. Independent result review passes, including 284 fast tests, 12 selected physical process cases, 42 affected browser cases and exact preservation checks. The reviewed source is pushed to main and [hosted CI passes](https://github.com/rore/keir-demo-app/actions/runs/41855295728); recoverable cleanup remains. No user deployment or real effect activation is claimed. Next work connects qualified binding/dispatch, actual authoring and native task outcomes using the core effect/3 implementation.
