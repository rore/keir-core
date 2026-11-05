---
id: "task-organization"
title: "Organize tasks with tags, priority, due dates and search"
status: "active"
priority: "medium"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Extend the native app into the useful task manager described in the design, independently of extension-language support.

## In Scope

- Add notes, editing/deletion policy, tags, priority, due/completion dates and bounded search/filter experiences.
- Specify missing values, query completeness and normal authorization/validation in the native service.
- Expose ordinary domain operations; do not implement a special follow-up algorithm for Keir.

## Out of Scope

Calendars, notifications, recurring scheduling, shared accounts and Keir-specific business logic.

## Done When

- [ ] Design/spec: define native field and bounded-query behavior, including absent due dates and completeness.
- [ ] Implementation: manage and find tasks by their ordinary fields with extensions disabled.
- [ ] Verification: persistence/UI tests cover search boundaries, absent dates, updates and validation without adding background scheduling.

## Notes

**First slice delivered:** Native task tags and bounded search within the selected project are on [Daymark main](https://github.com/rore/keir-demo-app/commit/9c36f0b34b4863f6bd673586cd30f0457365754c) and running in the verified demo. Tags and search work without AI or extensions. The [Work Record](../../.agent-workflow/tasks/daymark-native-tags-search.md) records browser and migration evidence. Priority, due dates and broader organization work remain open. Studio capability exposure is tracked under [richer customizations](rich-task-customizations.md).

**Dependencies:** [native-task-app](../features/native-task-app.md).

**Design input:** [Native task scope and catalog, sections 2-3](../../docs/internal/design-baseline/public/task-management-poc.md).
