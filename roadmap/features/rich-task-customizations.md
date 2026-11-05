---
id: "rich-task-customizations"
title: "Run the search, tag and due-date customization in the real app"
status: "active"
priority: "medium"
commitment: "committed"
milestone: "Extend host behavior safely"
repo: "keir-demo-app"
---

## Summary

Use the newly qualified language features and ordinary task services for the baseline invoice follow-up scenario.

The active increment exposes the completed task's title for copying and exact matching while existing rules keep their pinned model. A fixed suffix becomes available after the [Keir expression update](enrich-followup-expressions.md). Search, tags and due dates remain queued. [Implementation record](../../.agent-workflow/tasks/daymark-title-aware-task-customizations.md).

## In Scope

- Publish an explicitly revised generic catalog for bounded search and the selected task fields; project/title remain customer-authored.
- Compose get, search, tag condition and one create with a due date 168 elapsed hours later; no specialized follow-up capability.
- Define actual query completeness and any domain uniqueness separately from operation-key idempotency.

## Out of Scope

Scheduling, implicit parent relationships, arbitrary search expansion or treating search-before-create as atomic duplicate prevention.

## Done When

- [ ] Design/spec: approve the expanded task model, generic arguments and real query/uniqueness contracts.
- [ ] Implementation: run the same reviewed program in simulation and against native task data, showing created and no-op outcomes.
- [ ] Verification: tags, existing matches, absent dates, bounded/incomplete search, overflow, concurrent duplicates and uncertain writes have honest outcomes.

## Notes

**Dependencies:** [task-organization](../features/task-organization.md); [demo-task-integration](../features/demo-task-integration.md); [enrich-followup-expressions](../features/enrich-followup-expressions.md).

**Design input:** [Task examples and native guarantees, sections 3-4 and 7-8](../../docs/internal/design-baseline/public/task-management-poc.md); [Independent scenario expectations, sections 4-5](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md).
