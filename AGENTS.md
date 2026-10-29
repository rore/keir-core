# Working in keir-core

- Keep the framework domain-independent. Task services, real task adapters and AI authoring belong to `keir-demo-app`.
- `roadmap/` is the single planning source for both repositories. Update the owning feature here when core or demo work changes its status or completion evidence. Updates confined to `roadmap/`, `docs/README.md`, or `docs/roadmap.md` do not need Agent Workflow or a Work Record. Agent Workflow is scoped to core work only; do not require its Work Records in the demo repo.
- For roadmap work, use the repo-local Minimap skill at `.agents/skills/minimap-roadmap/SKILL.md` (Claude: `.claude/skills/minimap-roadmap/SKILL.md`). Agent Workflow is advisory and opt-in for now: use it when explicitly requested, but do not require a Work Record or PR for ordinary changes. The config makes records and Redline verdicts optional, and the Redline policy is in shadow mode; no hooks or CI gates are installed. Revisit enforcement when there is enough code and verification to support it.
- Both Java and Node runtimes are initial scope. A semantic feature is complete only when the compiler and both runtimes pass shared conformance for the same artifact.
- Update affected specifications, fixtures and compatibility rules together. Treat `docs/design-baseline/` as historical input; current decisions and the roadmap take precedence over its deferred Java-runtime statements.
- Fail closed on unsupported constructs. Preserve host-bound authority, explicit uncertainty and runtime resource enforcement.
- Keep changes small and verify the behavior affected. Do not invent package versions, test results or implemented capabilities.
- Protect the user's environment: use isolated project tooling. Obtain explicit approval before changing installed tools, global configuration, PATH, persistent environment variables, integrations or user services. Ordinary repository edits and isolated tests are authorized by the task.
