# Agent Workflow advisory bootstrap proposal

Date: 2026-10-29

Status: Phase 3 draft approved and installed as an advisory subset on 2026-10-29. No hooks or CI installed.

## Inspection

- `keir-core` has `AGENTS.md`, `CLAUDE.md`, a shared Minimap roadmap, three decisions, and the installed Agent Workflow skill package for Codex and Claude.
- It has no commit, remote, compiler/runtime source, build file, test suite, CI workflow, CODEOWNERS file, Agent Workflow config, Redline policy or existing hook configuration.
- The current design decisions and roadmap are the authoritative product and planning sources. No behavior-contract test path can be certified yet.
- The requested development flow is direct pushes to `main` when useful. No PR-only requirement, required status check or branch protection may be introduced by this bootstrap.
- Redline can classify current governance files in shadow mode, but no dependency boundary can be tested until source structure exists. Do not invent one.

## Proposed configuration

`agent-workflow.yaml`:

```yaml
version: 1
project:
  name: keir-core
workRecord:
  backend: local
  local:
    taskPath: ".agent-workflow/tasks/{slug}.md"
  requiredForBranchChanges: false
redline: optional
redlineVerdictPath: build/redline-verdict.json
applicability:
  documentationOnly:
    paths:
      - roadmap/
      - docs/README.md
      - docs/roadmap.md
    workflowRequired: false
    directDefaultBranchAllowed: false
```

`agent-redline-policy.yaml`:

```yaml
version: 1
project:
  name: keir-core
zones:
  red:
    - path: agent-workflow.yaml
      reason: workflow policy changes
      checkpoint: architecture-review
    - path: agent-redline-policy.yaml
      reason: risk policy changes
      checkpoint: architecture-review
    - path: docs/decisions/**
      reason: architecture and product decisions
      checkpoint: architecture-review
  blue:
    - path: roadmap/**
      reason: shared planning updates
    - path: docs/README.md
      reason: documentation navigation
    - path: docs/roadmap.md
      reason: roadmap navigation
checkpoints:
  architecture-review:
    description: Review structural policy or decision changes
    satisfiedBy:
      - label: architecture-reviewed
modes:
  default: shadow
excludes:
  - ".agent-workflow/**"
```

These paths describe files that exist today or will be created by this bootstrap. There is no language extension, boundary rule, behavior contract, PR-size threshold or protected default branch in this proposal. Recalibrate when source, tests and release practice exist. A Work Record and Redline verdict are optional in this phase. A change confined to `roadmap/`, `docs/README.md`, or `docs/roadmap.md` needs no Agent Workflow Work Record; a mixed documentation-and-code change is evaluated as a whole. This is an explicit ordinary-documentation list, not a blanket `docs/` exemption: `docs/decisions/` contains accepted decisions, `docs/design-baseline/` contains design specifications and a schema, and this proposal is governance. The design snapshot and examples remain gray rather than assuming all future content in those folders is low risk. Add new ordinary documentation paths when they exist and their role is clear. `directDefaultBranchAllowed` remains false because there is no remote or protection state to verify; this checker setting does not install a push restriction. The architecture-review label is advisory metadata for a future PR, not a condition for direct work on `main`. Redline's `shadow` setting makes its verdict advisory when run; the absence of push/PR hooks and CI means it cannot block a direct push. Agent Workflow's skill guides a session that explicitly invokes it, and the root `AGENTS.md` will state that its process is opt-in until enforcement is separately approved.

## Approved local-file scope

1. Write the two configurations above and validate them against the installed schemas.
2. Vendor the checker and Redline reporter from the already installed, pinned skill package, plus their runtime adapters and schema. Add a Work Record directory with a README. Keep the existing `AGENTS.md` content and `CLAUDE.md` import intact; append only a short advisory usage note.
3. Run a local checker/reporter probe with explicit evidence and report any prerequisites that are unavailable. The current uncommitted repository prevents a complete Git-diff probe; do not claim one succeeded.
4. Leave `.claude/settings.json`, `.claude/hooks/`, `.codex/hooks.json`, `.github/workflows/`, `.github/CODEOWNERS`, branch protection, pre-push hooks, and global tool settings untouched. Write no CI workflow proposal that implies it has been approved for installation. Revisit enforcement separately when the user wants it.

The installed files and probe results are recorded in [the bootstrap summary](agent-workflow-bootstrap-summary.md).

This is an intentional advisory subset of the upstream bootstrap. Its ordinary full path assumes PR-running CI and installs native hooks; those would conflict with the current direct-to-`main` requirement. Skill files are present, but this proposal does not call that a completed full integration.

## Phase 3 adaptation review

- The documentation-only fragment was generated by the installed checker from the approved exact paths `roadmap/`, `docs/README.md` and `docs/roadmap.md`, with protection status `unavailable`. It returned `workflowRequired: false` and `directDefaultBranchAllowed: false`.
- Red zones target only policy files and accepted decisions. These are structural changes rather than paths expected in routine feature work. There are no commits or recent PRs for history-based zone calibration; revisit after the first implementation window.
- No language extension, dependency boundary, API, persistence or security path can be grounded in this repository yet. No reviewer team, CODEOWNERS, CI or PR-size threshold is invented.
- The policy is `shadow`; its architecture-review label is future PR metadata. Neither config adds a hook or a main-branch restriction.
