# Documentation

- [Roadmap](roadmap.md): active milestone, acceptance criteria and proposed sequencing.
- [Repository and runtime decision](decisions/0001-repositories-and-runtimes.md) and [product evaluation decision](decisions/0002-product-proposition-and-evaluation.md): accepted changes to the baseline.
- [Restricted CEL and resource containment](decisions/0003-restricted-cel-resource-policy.md): selected POC direction, provisional profile and evidence gates.
- [Design baseline](design-baseline/README.md): the v0.4 design package and subsequent specifications, including host integration v0.5 and host-owned idempotency/multi-effect contracts v0.7, with precedence and reconciliation notes.
- [Design review](design-baseline/grill-findings.md): unresolved review findings, not automatically accepted design changes.

The baseline contains draft design inputs, not evidence of implementation. In particular, its references to a deferred Java runtime are superseded: both Java and Node runtimes are required from the first executable slice. Its task-management document is historical context; ongoing application scope belongs to `keir-demo-app`.

Add supplied design specifications to `design-baseline/public/` and index them in `design-baseline/README.md`; do not create a separate draft-design folder. Preserve supplied text and record precedence or conflicts in the index. As implementation settles a contract, write its maintained specification here and explicitly identify which baseline sections it supersedes. Do not duplicate normative specifications in the demo repository.
