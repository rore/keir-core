# Documentation

- [Roadmap](roadmap.md): active milestone, acceptance criteria and proposed sequencing.
- [Repository and runtime decision](decisions/0001-repositories-and-runtimes.md) and [product evaluation decision](decisions/0002-product-proposition-and-evaluation.md): accepted changes to the baseline.
- [Restricted CEL and resource containment](decisions/0003-restricted-cel-resource-policy.md): selected POC direction, provisional profile and evidence gates.
- [Original design package](design-baseline/README.md): preserved v0.4 design input.
- [Design review](design-baseline/grill-findings.md): unresolved review findings, not automatically accepted design changes.

The baseline is a historical snapshot, not evidence of implementation. In particular, its references to a deferred Java runtime are superseded: both Java and Node runtimes are required from the first executable slice. Its task-management document is historical context; ongoing application scope belongs to `keir-demo-app`.

Keep the snapshot intact. As implementation settles a contract, write its maintained specification here and explicitly identify which baseline sections it supersedes. Do not duplicate normative specifications in the demo repository.
