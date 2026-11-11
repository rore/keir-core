---
id: "rich-task-customizations"
title: "Compose real task workflows in Daymark"
status: "done"
priority: "medium"
commitment: "committed"
milestone: "Extend host behavior safely"
repo: "keir-demo-app"
---

## Summary

Let a business user combine task creation and completion triggers, conditions and a single action in the Customization Studio. Daymark owns the task model, native operations, event policy and Studio presentation.

The first increment delivered title copying, exact matching and fixed suffixes in Daymark. A live rule created the expected follow-up title, and upgrade checks verified that prior approvals and pending completions still execute. [Implementation record](../../.agent-workflow/tasks/daymark-title-aware-task-customizations.md)

## In Scope

- Publish Task created and Task completed in the Daymark model and support one active customization per trigger. Preserve owner and event replay boundaries.
- Compose conditions on project, title, tags, priority, due date and whether a matching open task exists in a project. Use one action to create a task with title, note, tags, priority and due date, or update the triggering task with a tag, priority or due date.
- Seed discoverable workflows for intake prioritization, cross-project review, follow-up suppression and missing due dates. Show the rule and its simulations inline, preserve explicit approval and activation, and verify the resulting native task state.

- Publish a revised Daymark catalog for the bounded Boolean query and task fields. Support exact title matching. The user chooses projects and titles. Store due times as optional instants so elapsed-hour rules have the same result across time zones.
- Compose get, search, tag condition and one create with a due date 168 elapsed hours later; no specialized follow-up capability.
- Define actual query completeness and any domain uniqueness separately from operation-key idempotency.

## Out of Scope

Scheduling, implicit parent relationships, arbitrary search expansion or treating search-before-create as atomic duplicate prevention.

## Done When

- [x] Review the expanded task model, generic arguments, and the authorized snapshot used by the open-task check.
- [x] Run the same reviewed program in simulation and against native task data, showing created and no-op outcomes.
- [x] Verification: both triggers, exact title matching, tags, existing matches, absent dates, elapsed time across daylight-saving transitions, incomplete queries, overflow, concurrent duplicates and uncertain writes have honest outcomes.

## Notes

The Keir 2 slice was delivered at Daymark `16c7f5c` with Keir `0.2.0`. All four workflows ran the same compiled artifacts in simulation and against native tasks. Studio distinguishes no change, a check that could not run, and an unconfirmed write. Its hosted build, Node and Chromium checks passed. The open-task check describes an authorized snapshot, so concurrent actions can still create matching tasks; native regression verifies that boundary and stable replay. [Daymark integration](https://github.com/rore/keir-demo-app/commit/16c7f5c54548058da6f509f20ed00835b46720e1) · [Hosted verification](https://github.com/rore/keir-demo-app/actions/runs/42468891600) · [Stage B work record](../../.agent-workflow/tasks/daymark-workflow-stage-b.md). These are historical delivery identities, not the current serving revision. The [later package and rollout evidence](../../.agent-workflow/tasks/windows-authoring-guide-integrity.md) records Core 0.3.1 adoption with supported saved artifacts retained.

Closure verification included a live provider journey. The observed YAML-authored follow-up passed its four independent simulations and native execution in two disposable projects. The running demo includes the verified-artifact title-suffix summary and compact inline review for short screens. The exact unread-notification layout regression fails with the old CSS and passes with the correction. Saved conversations and evidence survived the demo restart. The temporary qualification customization was deleted through the UI, preserving the owner's other items and selections.

**Dependencies:** [task-organization](../features/task-organization.md); [demo-task-integration](../features/demo-task-integration.md); [enrich-followup-expressions](../features/enrich-followup-expressions.md).

**Design input:** [Task examples and native guarantees, sections 3-4 and 7-8](../../docs/internal/design-baseline/public/task-management-poc.md); [Independent scenario expectations, sections 4-5](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md).
