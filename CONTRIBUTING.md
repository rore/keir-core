# Contributing

This project is at the design-to-implementation boundary. Start with the [roadmap](docs/roadmap.md) and choose a bounded change with observable acceptance criteria.

The [shared roadmap](roadmap/board.md) tracks both core and demo work. Keep feature status and cross-repository acceptance there. Agent Workflow is currently optional in core; when used, its Work Records stay in core. Demo changes use their own tests and review without a second Work Record process.

For a semantic change, include the affected compiler and runtime behavior, shared fixtures, documentation and compatibility implications in one reviewable change. Both runtimes must execute the same artifact with equivalent observable outcomes. Passing expression tests alone does not establish full compiler or host safety.

Keep framework work here and task-application code in `keir-demo-app`. Link dependent issues or PRs across repositories when GitHub repositories exist; do not duplicate their status in several backlogs. Record consequential decisions in `docs/decisions/`.

Use project-local or isolated tooling. Build commands and CI checks will be documented when implemented. Do not install or change global tools as part of setup.

Release packages separately for Java compiler, Java runtime and Node runtime. Exact coordinates and versioning policy remain undecided. Before a release, verify admitted CEL features and shared artifact conformance, and test the demo against the release candidate. The demo must pin its dependencies rather than rely on a sibling checkout.

License choice is pending; resolve it before inviting public contributions or publishing packages.
