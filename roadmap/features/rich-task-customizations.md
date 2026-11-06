---
id: "rich-task-customizations"
title: "Compose real task workflows in Daymark"
status: "active"
priority: "medium"
commitment: "committed"
milestone: "Extend host behavior safely"
repo: "keir-demo-app"
---

## Summary

Let a business user combine task creation and completion triggers, conditions and a single action in the Customization Studio. Daymark owns the task model, native operations, event policy and Studio presentation.

The first increment delivered title copying, exact matching and fixed suffixes in Daymark. A live rule created the expected follow-up title, and upgrade checks verified that prior approvals and pending completions still execute. Search, tags and due dates remain queued. [Implementation record](../../.agent-workflow/tasks/daymark-title-aware-task-customizations.md).

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

- [ ] Design/spec: approve the expanded task model, generic arguments and real query/uniqueness contracts.
- [ ] Implementation: run the same reviewed program in simulation and against native task data, showing created and no-op outcomes.
- [ ] Verification: both triggers, exact title matching, tags, existing matches, absent dates, elapsed time across daylight-saving transitions, incomplete queries, overflow, concurrent duplicates and uncertain writes have honest outcomes.

## Notes

The native foundation is integrated in [Daymark 59c819a](https://github.com/rore/keir-demo-app/commit/59c819a1f801a50183d2fc232b433c160da03e31). It adds priority, optional exact due times and a local-time editor. An isolated upgrade from the shipped schema preserved data across all 20 tables. Studio design covers the four workflow scenarios and distinguishes confirmed no change, a check that could not run, and an unconfirmed write. This design still needs validation with the actual host and runtime.

**Dependencies:** [task-organization](../features/task-organization.md); [demo-task-integration](../features/demo-task-integration.md); [enrich-followup-expressions](../features/enrich-followup-expressions.md).

**Design input:** [Task examples and native guarantees, sections 3-4 and 7-8](../../docs/internal/design-baseline/public/task-management-poc.md); [Independent scenario expectations, sections 4-5](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md).
