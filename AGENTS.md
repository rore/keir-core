# Working in keir-core

This repository uses `persistent-agent-roles`. At start, resume, handoff, or a new assignment, load the skill to verify your current Relay name and mapped guidance. If missing, install it globally from [Collection](https://github.com/rore/rore-collection/tree/main/skills/persistent-agent-roles).

Role mappings live in `roles/mapping.yaml`; see [the team roles](roles/README.md) and linked shared guidance. Role guidance does not itself assign work or change model settings.

- Keep tracked content and commit identity free of private paths, identities and operational details. Use relative references or generic examples; follow the [public-content rule](roles/shared.md#privacy-cost-and-context).
- Keep the framework domain-independent. Task services, real task adapters and AI authoring belong to `keir-demo-app`.
- `roadmap/` is the single planning source for both repositories. Update the owning feature here when core or demo work changes its status or completion evidence. Updates confined to `roadmap/` or `docs/README.md` do not need Agent Workflow or a Work Record. Agent Workflow is scoped to core work only; do not require its Work Records in the demo repo.
- For roadmap work, use the repo-local Minimap skill at `.agents/skills/minimap-roadmap/SKILL.md` (Claude: `.claude/skills/minimap-roadmap/SKILL.md`). [Agent Workflow](.agents/skills/agent-workflow/SKILL.md) is advisory and opt-in for now: use it when explicitly requested, but do not require a Work Record or PR for ordinary changes. The config makes records and Redline verdicts optional, and the Redline policy is in shadow mode. CI publishes advisory findings on main pushes and PRs; malformed policy or tool output fails CI. No hooks or required workflow checks are installed. Revisit enforcement when there is enough code and verification to support it.
- Both Java and Node runtimes are initial scope. A semantic feature is complete only when the compiler and both runtimes pass shared conformance for the same artifact.
- Update affected specifications, fixtures and compatibility rules together. Treat `docs/internal/design-baseline/` as historical input; current decisions and the roadmap take precedence over its deferred Java-runtime statements.
- Fail closed on unsupported constructs. Preserve host-bound authority, explicit uncertainty and runtime resource enforcement.
- Keep changes small and verify the behavior affected. Do not invent package versions, test results or implemented capabilities.
- Protect the user's environment: use isolated project tooling. Obtain explicit approval before changing installed tools, global configuration, PATH, persistent environment variables, integrations or user services. Ordinary repository edits and isolated tests are authorized by the task.
