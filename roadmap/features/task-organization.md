---
id: "task-organization"
title: "Organize tasks with tags, priority, due dates and search"
status: "done"
priority: "medium"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Native task organization is delivered independently of extensions or AI: notes, completion timestamps, tags, priority, optional due instants and bounded text/tag search within the selected project. Native validation, owner isolation, absent/update behavior and query completeness are defined in the [host implementation reference](https://github.com/rore/keir-demo-app/blob/main/docs/native-task-app-design.md#data-model-and-storage).

## In Scope

- Add notes, editing/deletion policy, tags, priority, due/completion dates and bounded search/filter experiences.
- Specify missing values, query completeness and normal authorization/validation in the native service.
- Expose ordinary domain operations; do not implement a special follow-up algorithm for Keir.

## Out of Scope

Calendars, notifications, recurring scheduling, shared accounts and Keir-specific business logic.

## Done When

- [x] Design/spec: define native field and bounded-query behavior, including absent due dates, update preservation, time-zone handling and current-data pagination completeness.
- [x] Implementation: manage native fields and find tasks by literal text or exact normalized tags with extensions and AI disabled. Native task reopening/deletion remain outside the documented native slice; this card specifies their policy rather than requiring unimplemented operations.
- [x] Verification: accepted native persistence, migration, validation and browser tests cover search boundaries, absent dates, updates, owner isolation and daylight-saving cases without background scheduling.

## Notes

**Tags/search delivered:** [Daymark `9c36f0b`](https://github.com/rore/keir-demo-app/commit/9c36f0b34b4863f6bd673586cd30f0457365754c) delivers native tags and bounded selected-project search without AI or extensions. The [accepted record](../../.agent-workflow/tasks/daymark-native-tags-search.md#implementation-and-verification) retains 354 fast passes with 13 skips, 15 native browser journeys, migration/rollback and sparse-match pagination evidence.

**Priority/due dates delivered:** [The native-field record](../../.agent-workflow/tasks/daymark-workflow-native-foundation.md#result-review) accepts Daymark `59c819a`: 349 native tests pass with 13 skips, 22 native browser tests plus a year-boundary case pass, and the shipped-schema migration preserves existing rows and round-trips millisecond due times. Daylight-saving gaps and repeated hours are covered. These completed fields are not pending organization work; [richer customizations](rich-task-customizations.md) separately records their exposed Keir capabilities.

**Dependencies:** [native-task-app](../features/native-task-app.md).

**Design input:** [Native task scope and catalog, sections 2-3](../../docs/internal/design-baseline/public/task-management-poc.md).
