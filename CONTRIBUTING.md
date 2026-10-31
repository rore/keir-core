# Contributing

This project is at the design-to-implementation boundary. Start with the [roadmap](roadmap/board.md) and choose a bounded change with observable acceptance criteria.

The [shared roadmap](roadmap/board.md) tracks both core and demo work. Keep feature status and cross-repository acceptance there. Follow the [current Agent Workflow process](docs/internal/workflow/current-process.md) for code, mixed changes and governance work in either repo, with one task-specific Work Record in core. Documentation-only and roadmap-only changes are exempt through the configured applicability paths. Demo code and tests remain in demo; it needs no second workflow installation.

Work directly on `main` for now; no PR is required. Keep planning, risk-based review, verification and delivery evidence in the record. Use a unique explicit task identity rather than a record named `main`.

[Advisory CI](docs/internal/workflow/agent-workflow-ci-proposal.md) runs on main pushes and PRs. Read the job summaries and verdict artifacts for findings. Workflow findings do not fail the jobs; invalid policies, missing reports and tool failures do. No check is required for pushing or merging.

For a semantic change, include the affected compiler and runtime behavior, shared fixtures, documentation and compatibility implications in one reviewable change. Both runtimes must execute the same artifact with equivalent observable outcomes. Passing expression tests alone does not establish full compiler or host safety.

Follow the [testing guidelines and initial behavioral-contract plan](docs/testing.md). Default tests stay fast; investigate regressions in test duration, select slow checks explicitly and preserve the product obligations expressed by protected cases.

Keep framework work here and task-application code in `keir-demo-app`. Link dependent issues or PRs across repositories when GitHub repositories exist; do not duplicate their status in several backlogs. Record consequential decisions in `docs/internal/decisions/`.

Use project-local or isolated tooling. Build commands and CI checks will be documented when implemented. Do not install or change global tools as part of setup.

Release packages separately for Java compiler, Java runtime and Node runtime. Exact coordinates and versioning policy remain undecided. Before a release, verify admitted CEL features and shared artifact conformance, and test the demo against the release candidate. The demo must pin its dependencies rather than rely on a sibling checkout.

License choice is pending; resolve it before inviting public contributions or publishing packages.
