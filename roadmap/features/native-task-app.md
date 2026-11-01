---
id: "native-task-app"
title: "Use a persistent task app without extensions"
status: "done"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-demo-app"
---

## Summary

Deliver a polished local web app: projects and task create/list/edit/complete, retained across restarts, with three seeded demo users and Keir/AI disabled.

## In Scope

- Choose the smallest local web stack, persistence and demo sign-in model. Visitors clone this repo and run native task management without a sibling checkout, hosted infrastructure or AI credentials. The complete AI extension-authoring demo later requires the visitor's own configured provider.
- Seed three fictional users with separate projects/tasks. Each represents one SaaS customer customization scope; enforce ownership in services, not only in the UI. This demonstrates isolation without building a production identity platform.
- Establish a distinctive visual direction before implementation: coherent typography, spacing and color, polished task interactions, responsive layouts, keyboard access and intentional loading/empty/error states. Verify actual rendered screens; a generic generated dashboard is insufficient.
- Implement native task services, projects, stable IDs, validation and a usable completion flow; preserve a clean service boundary for later adapters.
- Use isolated project tooling and test storage. Keep extensions out of native CRUD correctness.

## Out of Scope

AI, Keir activation, deployment infrastructure, collaboration and a scheduling engine. Rich organization is a separate feature.

## Done When

- [x] Design/spec: settle the minimal task model, user ownership/sign-in, persistence lifecycle and visual direction; document local prerequisites and the later per-user customization boundary.
- [x] Implementation: documented pinned setup/seed/start opens the web app from a clean clone; sign in as any of three seeded users, create a project, add/edit/list/complete tasks, sign out and restart with data retained. Document a deliberate data reset and keep runtime data/credentials untracked.
- [x] Verification: fast service/persistence and UI journeys cover invalid input, missing records, direct cross-user API/resource access, stale client state and pending requests during sign-out/switching. Verify clean local startup and persistence for all users with extensions disabled and without authoring services. Review representative desktop/mobile and empty/error screens against the visual direction.

## Notes

**Dependencies:** No unfinished core prerequisite.

**Design input:** [Task app MVP, sections 1-2](../../docs/internal/design-baseline/public/task-management-poc.md).

Delivered: the reviewed [native design](https://github.com/rore/keir-demo-app/blob/main/docs/native-task-app-design.md) is integrated on demo main at `e3a4335`, with setup/prototype links at `b6f2b03`. Both independently reproduced UI races are fixed and approved. Eleven Node groups and seven Chromium journeys pass; canonical locked installation/build and actual browser smoke pass. [Linux CI passes](https://github.com/rore/keir-demo-app/actions/runs/41297780394). The developer released the assignment and the task-created checkout was recoverably removed. [The core-owned record](../../.agent-workflow/tasks/native-task-app.md) retains verification/review evidence. Keir integration, live AI and production authentication are not part of this completed native slice.
