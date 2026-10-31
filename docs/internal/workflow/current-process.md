# Current development process

Use Agent Workflow for code, mixed changes and governance work in both Keir repositories. Work directly on `main`; PRs are not required. CI findings remain advisory and Redline remains in shadow mode. Do not add hooks or change remote protection to deliver a task.

## Applicability

A change wholly within `docs/`, `roadmap/`, `README.md` or `CONTRIBUTING.md` needs no Work Record. This includes design/specification text under `docs/`, as selected by the user. The complete changed-path set must qualify; adding code, tests, configuration, policies, agent instructions or a Work Record keeps the task in scope. Existing behavior approval, privacy and testing obligations still apply. Standalone read-only questions and explanations do not need records.

The applicability paths and matching low-risk zones are configured in `agent-workflow.yaml` and `agent-redline-policy.yaml`. They decide whether a record is needed, not whether a push is authorized. Direct main is the user-authorized delivery policy; remote rules still apply. Neither an exemption nor a green check grants authority to change behavior or bypass protection.

## One record per task

Keep records in core at `.agent-workflow/tasks/<slug>.md`, including demo code tasks. Assign the explicit identity `agent-workflow:<slug>` at task pickup; never derive a shared `main` record. Delegated agents use the same task record. Record exact owning roadmap relationships when applicable, without copying roadmap state into a second backlog.

1. Establish the outcome, scope, constraints, acceptance and baseline before implementation. Classify risk and complexity using fresh Redline evidence. Routine/simple work uses the compact template; other work uses the expanded template.
2. Plan and obtain the skill's risk-appropriate review. Elevated/high work needs an independent technical reviewer; high work also needs human approval. Preserve existing authorization rather than requesting it again.
3. Implement within scope. Update the record at checkpoints; failed assumptions return affected work to planning.
4. Run focused tests/checks and assess their adequacy. Record actual revision/evidence and result review in the Work Record, which replaces the absent PR evidence surface.
5. Commit the record with the change, reconcile the owning roadmap, and push authorized changes to main. Keep `State` within the installed skill's allowed values; record delivery and remaining work in prose rather than inventing a `Done` state. Recoverably archive completed task checkouts.

For core, resolve and validate from its checkout:

```text
python scripts/agent-workflow-check.py --repo-root . --resolve-work-record --work-record-ref agent-workflow:<slug>
python scripts/agent-workflow-check.py --repo-root . --slug <slug> --changed-files-z <core-paths.z> --redline-verdict <fresh-core-verdict.json>
```

The reference option is resolver-only; validation uses `--slug`. Collect complete paths from the trusted task base through committed and working changes, including untracked files. Generate a fresh report with `scripts/agent-redline-report.py`, the configured policy and `--flow-mode push`; never reuse stale verdicts. Shared CI already discovers changed task records, so main needs no branch-derived record or workflow modification.

## Demo tasks

Keep app code and tests in demo, with its Work Record in core. No demo workflow installation/config or local record copy is needed. Supply the core checkout and task identity in assignments.

Collect the demo's complete changed paths and diff from its own task base. From demo, run the shared reporter with core's policy:

```text
python <core-checkout>/scripts/agent-redline-report.py --policy <core-checkout>/agent-redline-policy.yaml --changed-files-z <demo-paths.z> --flow-mode push --json-out <fresh-demo-verdict.json>
```

Then run core's resolver/checker from core using the same task identity:

```text
python scripts/agent-workflow-check.py --repo-root . --resolve-work-record --work-record-ref agent-workflow:<slug>
python scripts/agent-workflow-check.py --repo-root . --slug <slug> --changed-files-z <demo-paths.z> --redline-verdict <fresh-demo-verdict.json>
```

Evidence placeholders denote accessible files, not literal arguments. Shared policy does not make unclassified app paths safe; preserve the gray risk floor and raise risk for actual security, persistence or contract effects. Record tests and revisions for each affected repository separately; a core checker pass is not a demo test result. For exempt demo docs, evaluate its complete path set with the same core policy/applicability but omit `--slug` so no task record is selected.
