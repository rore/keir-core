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

Current priority: replace the invented task-advice demonstration with recognizable task-management customization. Architect discovery is requested to map existing native services, the actual domain model and generic capabilities to the smallest real end-to-end scenario and its core/host/Studio implementation steps. Completion-driven task creation is the original candidate; confirm its real-effect admission dependencies rather than bypassing them. Later generic operations should follow actual task-model needs. Preserve existing customer data/history during the transition; do not merely rename advice or claim planned behavior works.

**Current assignment:** Architect discovery is complete and the manager adopts the first scenario: completing a task in a customer-chosen existing project creates a task with a customer-chosen title through generic capabilities. The architect is detailing the core/host contract; a developer is discovering the smallest native event/receipt patch under [the host-foundation record](../../.agent-workflow/tasks/native-task-effect-foundation.md). These run alongside Studio review. No real execution or advice-to-write migration is claimed. Existing approvals remain confined to their original read-only behavior.

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

This preserves the existing feature ID. Its former broad obligations now have explicit owners: native CRUD -> native-task-app/task-organization; packaged real reads -> real-task-reads; effect concurrency/replay/authorization -> this card; enforced DENY -> task-create-guard; full review/activation UI -> extension-approval-lifecycle. The first scalar integration may use trusted operator-selected immutable artifacts; the full lifecycle UI and richer syntax are not prerequisites.

The independently reviewed [host handoff](../../docs/internal/real-task-effect-handoff.md) fixes event occurrence, original-key, atomic receipt, selection/revocation and legacy-preservation rules. Permanent revocation across reactivation, retained confirmed outcomes and actual SQLite crash/concurrency qualification remain implementation acceptance gates. The developer is producing the concrete foundation patch plan; no real native effect has been enabled.
