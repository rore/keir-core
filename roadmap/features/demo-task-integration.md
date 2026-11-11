---
id: "demo-task-integration"
title: "Run a real single-effect extension after task completion"
status: "done"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Connect an explicitly admitted scalar extension to native TaskCompleted events and generic task creation, preserving actual authority and effect evidence.

**Delivered.** Two live customizations passed six independent examples each and created the intended task in their respective workspaces after separate approval and activation. Nonmatching completion, repeat-result checks, generated-task completion and turning off were verified. The designer accepted the final example explanations, naming, request excerpts and turn-off placement. Twelve automated browser cases verified deletion. The [reviewed increment](https://github.com/rore/keir-demo-app/commit/e066c055af9eee7da71d15b8b184d93f65e42581) was delivered with original data and settings preserved; its [hosted verification](https://github.com/rore/keir-demo-app/actions/runs/41982700292) passed 341 unit and all 168 browser tests. See [the integration record](../../.agent-workflow/tasks/real-task-customization-integration.md) and [host-contract alignment](../../.agent-workflow/tasks/host-contract-alignment.md). This is historical acceptance, not the current serving revision; the [later delivery record](../../.agent-workflow/tasks/windows-authoring-guide-integrity.md) covers the packaged Core 0.3.1 update.

## In Scope

- Commit native completion before admitting a reaction; pin artifact/model/consumer selection once and preserve the original event/key.
- Define durable admission, restart/redelivery, event ordering, current authorization, causation/re-entry and old-version retention.
- Select bindings within the originating user's scope. Persist trusted event ownership independently of the current browser login, so switching users cannot change an admitted event's scope or selected program.
- Keep tasks.create generic: project/title come from source. Test real persistence and operation-key idempotency, with any business uniqueness specified separately.

## Out of Scope

Multiple writes, implicit source-task relationships, derived downstream keys, automatic replay and changing SIMULATION_ONLY by assertion.

## Done When

- [x] Design/spec: reconcile native task integration and host capability responsibilities with the corrected host contract.
- [x] Implementation: a reviewed scalar program creates or skips a task against actual services using pinned packages. Native completion survives reaction failure.
- [x] Verification: controlled tests cover concurrent duplicates, payload mismatch, revoked access, restart, changed selection, lost response and UNKNOWN without blind replay.
- [x] User scope: native persistence tests verify that an admitted event retains its originating owner and pinned selection under current authority, and prevents cross-owner access. Separate browser tests verify account switching with a held response. The full queued-redelivery sequence was not exercised in a single browser test.

## Notes

**Dependencies:** [native-task-app](../features/native-task-app.md); [real-task-reads](../features/real-task-reads.md); [real-effect-admission](../features/real-effect-admission.md).

**Design input:** [Post-commit events and consumers, sections 7, 12 and 13C](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Original keys and evidence, sections 3-7](../../docs/internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md); [First task integration, sections 3-4](../../docs/internal/design-baseline/public/task-management-poc.md).

This preserves the existing feature ID. Its former broad obligations have explicit owners: native CRUD -> native-task-app/task-organization; packaged real reads -> real-task-reads; effect concurrency/replay/authorization -> this card; enforced DENY -> task-create-guard; review/activation UI -> extension-approval-lifecycle. Core and native harnesses can verify their contributions independently, but the current delivery includes the actual customer authoring, checking, approval, activation and visible task-outcome journey. Richer syntax remains separate scope.

The independently reviewed [host handoff](../../docs/internal/real-task-effect-handoff.md) defines event occurrence, original keys, atomic receipts, selection and revocation, and legacy preservation. The [native foundation](../../.agent-workflow/tasks/native-task-effect-foundation.md) added schema 8 and verified revocation, retained receipts and SQLite process crash and concurrency behavior before the live integration.
