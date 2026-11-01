---
id: "task-organization"
title: "Organize tasks with tags, priority, due dates and search"
status: "queued"
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

**Dependencies:** [native-task-app](../features/native-task-app.md).

**Design input:** [Native task scope and catalog, sections 2-3](../../docs/internal/design-baseline/public/task-management-poc.md).
