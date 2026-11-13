# Working in keir-core

This repository uses `persistent-agent-roles`. At start, resume, handoff, or a new assignment, load the skill to verify your current Relay name and mapped guidance. If guidance is unavailable, report the gap and continue authorized work. Missing guidance is not permission to install global tools; that requires explicit approval.

Role mappings live in `roles/mapping.yaml`; see [the team roles](roles/README.md) and linked shared guidance. Role guidance does not itself assign work or change model settings.

- Keep tracked content and commit identity free of private paths, identities and operational details. Use relative references or generic examples; follow the [public-content rule](roles/shared.md#privacy-cost-and-context).
- Keep the framework domain-independent. Task services, real task adapters and AI authoring belong to `keir-demo-app`.
- `roadmap/` is the single planning source for both repositories. Update the owning feature here when core or demo work changes its status or completion evidence. For roadmap work use `.agents/skills/minimap-roadmap/SKILL.md` (Claude: `.claude/skills/minimap-roadmap/SKILL.md`).
- Use [Agent Workflow](.agents/skills/agent-workflow/SKILL.md) by default for code, mixed changes and governance work in either repo. Keep one task-specific Work Record in core, including for demo work; supply its explicit `agent-workflow:<slug>` identity, never derive a shared `main` record. Follow the [current process](docs/internal/workflow/current-process.md). Documentation-only changes under `docs/` (including design/spec text), `roadmap/`, `README.md` or `CONTRIBUTING.md` are exempt through applicability; instructions, policies and mixed changes are not. This user-selected exemption overrides the skill's general normative-documentation default without waiving behavior approval or testing obligations.
- Work on `main`; PRs are not required. Keep workflow checkpoints, risk-based reviews and verification in the Work Record. CI findings remain advisory and Redline stays in shadow mode; no hooks, branch protection or required workflow checks are installed. Do not change remote protection to force a push.
- Both Java and Node runtimes are initial scope. A semantic feature is complete only when the compiler and both runtimes pass shared conformance for the same artifact.
- Update affected specifications, fixtures and compatibility rules together. Treat `docs/internal/design-baseline/` as historical input; current decisions and the roadmap take precedence over its deferred Java-runtime statements.
- Fail closed on unsupported constructs. Preserve host-bound authority, explicit uncertainty and runtime resource enforcement.
- Keep changes small and verify affected behavior under the [testing guidelines](CONTRIBUTING.md#testing-principles): fast regression by default, explicit slow suites, preserved behavioral contracts. Do not invent versions, results or implemented capabilities.
- Protect the user's environment: use isolated project tooling. Obtain explicit approval before changing installed tools, global configuration, PATH, persistent environment variables, integrations or user services. Ordinary repository edits and isolated tests are authorized by the task.

- Write for one reader and task. Human guides lead with purpose and an example; agent guides give inputs, actions and constraints. Consolidate before adding pages, link canonical contracts, and keep release/qualification logs out of usage instructions. Follow the [documentation guidance](roles/documentation.md).
