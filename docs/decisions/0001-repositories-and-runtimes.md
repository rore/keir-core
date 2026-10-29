# 0001: Repository boundaries and initial runtimes

Date: 2026-10-28

Status: accepted in project setup discussion.

## Decision

Use two independent repositories named `keir-core` and `keir-demo-app`.

`keir-core` contains the Java compiler, Java runtime, Node runtime, shared artifact contract, conformance fixtures and small embedding examples. Both runtimes are initial scope; the earlier design's deferred Java runtime is superseded.

`keir-demo-app` contains the task manager, task-domain contracts and adapters, persistence, optional AI authoring, approval and activation UX. It consumes separately packaged core components.

Both runtimes consume the same compiled artifact. The Java compiler is independently consumable as a library and CLI. Runtime execution does not depend on running the compiler.

## Consequences

A semantic change can update compiler, both runtimes and conformance in one core PR. The first executable milestone must demonstrate parity across both runtimes before expanding language coverage.

The demo provides a real integration boundary and must pin compatible package versions. Candidate integration checks are needed before releases. Separate repositories add coordination work, so keep the active milestone small and track each task in its owning repository.

GitHub ownership/visibility, license, build tools, package coordinates and release versioning are not decided here. Setup creates local repositories only.
