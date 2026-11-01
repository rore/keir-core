---
id: "native-task-app"
title: "Use a persistent task app without extensions"
status: "queued"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Deliver the first useful application path: projects and task create/list/edit/complete, retained across restarts, with Keir and AI disabled.

## In Scope

- Choose the smallest UI, persistence and identity model in a bounded design review; local/single-user use is sufficient initially.
- Implement native task services, projects, stable IDs, validation and a usable completion flow; preserve a clean service boundary for later adapters.
- Use isolated project tooling and test storage. Keep extensions out of native CRUD correctness.

## Out of Scope

AI, Keir activation, deployment infrastructure, collaboration and a scheduling engine. Rich organization is a separate feature.

## Done When

- [ ] Design/spec: settle the minimal task model, native validation, identity boundary and persistence lifecycle in demo scope.
- [ ] Implementation: create a project, add/edit a task, list and complete it through the UI; restart and retain the data.
- [ ] Verification: fast service/persistence and one UI journey cover invalid input, missing records and normal completion with extensions disabled.

## Notes

**Dependencies:** No unfinished core prerequisite.

**Design input:** [Task app MVP, sections 1-2](../../docs/internal/design-baseline/public/task-management-poc.md).
