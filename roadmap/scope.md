# Keir roadmap

This is the single roadmap for `keir-core` and `keir-demo-app`: delivered features, planned deliveries and visible uncommitted proposals. The manager owns sequence and combined acceptance; each card identifies the repository that owns implementation.

Core packages are distributed through [GitHub Releases](https://github.com/rore/keir-core/releases). Daymark retains its own [verified package pins](https://github.com/rore/keir-demo-app/blob/main/docs/package-provenance.md). The supported contract allows **at most one mutation per run**; a released package does not establish unmeasured production containment or authorize further publication or visibility changes.

- **Core delivered.** The compiler, Java and Node runtimes, structural schemas, compatibility matrix, installed-consumer checks and versioned authoring guidance are available. Published authoring uses source `keir/2`, model `keir-dcm/3`, artifact `keir-artifact/3` and scenario `keir-scenario/3`. The qualified numeric candidate adds source `keir/3` + model `keir-dcm/4` and retains source `keir/2` + model `keir-dcm/3`, both emitting artifact/scenario `/4`. Broader resource containment, scale evidence and the remaining reusable host-conformance work are separate obligations; multi-mutation composition remains a proposal.
- **Daymark delivered.** Native task organization and all three triggers work: checks before task creation and single-action workflows after creation or completion. Studio provides a continuing conversation, checked-artifact Summary, Simulation, explicit approval and separate activation, with saved work, history and recovery. The [customization walkthrough](https://github.com/rore/keir-demo-app/blob/main/docs/customization-walkthrough.md) demonstrates real compilation, simulation and native writes using explicitly scripted authoring transport.
- **Documentation delivered.** Core has package installation, Java/Node quickstarts, embedding and normative contracts. Daymark separates provider setup, the screenshot customer journey and lifecycle/API reference. These delivered guides are not another pending customization-process implementation.

## Completed numeric delivery

[Numeric values and rules](features/numeric-values-and-rules.md) is manager-accepted and done. Core Integer/Decimal implementation, exact normalization helpers, runnable examples, installed packages, measurements, reconciled acceptance coverage and the corrected companion pass. Daymark's hosted run passes 606 Node tests and all 261 browser tests, with 12 unchanged opt-in process skips. Final technical and documentation findings are closed, and final prose is delivered without changing the tested product. The real journey covers authoring, simulation, approval, separate activation, retained selections and native enforcement through every supported create path. Core 0.5 remains an unpublished candidate. Live environment changes, deployment and publication are not authorized by this delivery.

## Latest released Core increment

[Guarded-result scalar dataflow](features/guarded-result-dataflow.md) is delivered in the [published Core packages](https://github.com/rore/keir-core/releases). Implementation, independent source reviews, shared compiler/Java/Node checks, actual installed-package execution and the matching companion all passed. The feature card records immutable inputs, measured outcomes and combined documentation acceptance. The slice improves composition of existing typed operations while retaining the single-write ceiling; numeric types, collection operations and multiple effects are not included.

## Other remaining work, not current assignments

No implementation assignment carries over from the completed deliveries below. An unfinished umbrella card can contain substantial delivered behavior; its current acceptance and evidence, not an old unchecked box or historical offer, determine what remains.

- Complete the explicitly qualified live provider matrix for [conversational authoring](features/conversational-authoring.md). Scripted transports and a successful real-provider journey are distinct evidence; a second provider is not yet qualified.
- Native task organization, the Daymark approval/lifecycle contract and the current machine-readable authoring contracts are delivered. Do not rebuild approval, simulation, task fields or the documented customer journey to satisfy older roadmap prose.
- Keep [host-integration distribution](features/host-integration-assistant.md), [native model BOM parity](features/model-json-bom-parity.md), [reusable simulation](features/reusable-simulation.md), [resource qualification](features/runtime-resource-qualification.md) and [scale evidence](features/runtime-scale-observability.md) distinct from the delivered demo. Queued scope is not an instruction to start it all.
- Standalone Studio embedding remains a [proposal](https://github.com/rore/keir-demo-app/blob/main/docs/studio-embedding.md), not a released generic UI package or an assigned rewrite. Repository visibility and registry publication remain separately authorized launch decisions.

The [public release card](features/public-package-release.md) owns the release checklist. Repository visibility and registry publication require separate authorization after the supported scope, license and publication prerequisites are reviewed.

## Tracking progress

- **Milestones describe outcomes; features own delivery.** Board order expresses priority; cards name actual dependencies. Milestones are not serial gates: native app work, tooling and experimental packaging may proceed independently.
- **Status lives on each card.** `queued` means unassigned; `active`/`in-progress` means assigned; `blocked` names an actual blocker; `done` requires evidence. Checkboxes expose design/specification, implementation and verified demonstration. A completed design checkbox is not a completed feature.
- **Commitment is explicit.** `features/` holds planned/delivered work; `ideas/` holds uncommitted proposals with `Proposal:` titles and decision gates. Filter by repository or commitment. Milestone inclusion does not adopt every draft detail; unequal feature counts are not a percentage-complete estimate.
- **Mature only the next increment.** Review draft gaps, publish supported contracts/schemas and positive/negative cases, implement, then verify the end-to-end scenario and update the specifications and agent guides. Do not freeze all design or finish an entire compiler layer before showing working behavior.

## Outcome milestones

| Milestone | Observable result and work content |
| --- | --- |
| **Execute extensions in Java and Node** | Delivered compiler and both runtimes execute the same supported artifact with typed host binding, read/decide and single-mutation semantics. Current and retained compatibility tuples are explicit; customer constants and honest effect evidence remain unchanged. |
| **Customize a real task application** | Delivered native organization, packaged reads, real single-effect event execution and before-create enforcement, with native key/persistence and recovery evidence. |
| **Author and activate customizations through AI** | Delivered conversation-led authoring, exact review/simulation/approval and separate activation. The live provider matrix and distribution of the host-design coding-agent skill/checkpoint remain distinct unfinished scope; reusable tooling is not a missing Daymark workflow. |
| **Extend host behavior safely** | Delivered search/tag/168-hour enhancement and qualified Integer/Decimal rules with real app integration. Further types, projections, patches, multiple bindings, tenant endpoints and multiple effects remain individually visible proposals. |
| **Release an embeddable Keir library** | Production-ready single-mutation profile, supported interfaces and schemas, clean Java and Node consumers, measured containment for the stated workload, human-readable examples and authorized publication. |

The [delivered core](features/generic-hook-capability-contract.md) runs the same single-mutation artifact in Java and Node. Daymark has verified [native single-mutation execution](features/demo-task-integration.md) with host-owned authority and receipts. Broader [production containment](features/runtime-resource-qualification.md) still needs qualification.

## Delivered foundation

1. [Native task app](features/native-task-app.md), delivered: a polished clone-and-run local web app with persisted create/list/edit/complete and three seeded demo logins, with extensions and AI disabled. Each user represents a SaaS customer's isolated scope. The feature records the selected stack, measured checks, independent review and passing CI.
2. [Packaged real reads](features/real-task-reads.md), delivered: installed read-only programs classify actual scoped tasks, with two user-specific programs, validated advisory display and standalone clone/run evidence. Later native writes and Studio/AI integration are also delivered under their own cards; this earlier evidence retains its original scope.
3. [Real-effect admission](features/real-effect-admission.md) and [native event/effect integration](features/demo-task-integration.md) are delivered with host-owned authority, original keys and native receipt evidence. Simulation alone still cannot certify real writes.

The [core guard](features/pre-operation-guard.md) and [native guard enforcement](features/task-create-guard.md) are delivered. The [approval lifecycle](features/extension-approval-lifecycle.md) supports exact review, separate activation, disabling and restoration; do not treat the original sequencing discussion as an instruction to implement those flows again.

The demo must visibly support different extensions at the same hook for different users, with service-enforced scope and independent activation. The [host-design assistant](features/host-integration-assistant.md) also owns a future `pre-design-time` starting branch/checkpoint for a visitor's coding agent to configure the host; `main` becomes the configured customer-customization journey. Preserve one native app and a reproducible checkpoint rather than two independently maintained products.

The demo includes [AI-driven extension creation](features/conversational-authoring.md) with operator-supplied provider/model/credentials. Real-provider authoring has recorded successful journeys; the required second-provider qualification remains open. Native task management and execution of existing extensions need no AI calls. Host design-time uses the visitor's coding agent and its own authentication, independently of the demo server's provider setup. Ship no private keys.

The **Customization Studio is the demo's centerpiece**. The primary conversation contains the rule summary, simulations and explicit approval and activation controls. Supporting inspection is secondary and opens on request. Its [dedicated design/prototype feature](features/extension-studio-design.md) owns UX, interaction and visual quality. Each new capability needs a business-person walkthrough and rendered verification alongside its executable acceptance tests.

## Decomposition of earlier broad cards

| Existing card | Explicit owners of its obligations |
| --- | --- |
| [Demo task integration](features/demo-task-integration.md) | [Native CRUD](features/native-task-app.md), [organization/query fields](features/task-organization.md), [real reads](features/real-task-reads.md), [core real admission](features/real-effect-admission.md), the existing card's native event/effect/concurrency outcome, [enforced guard](features/task-create-guard.md) and [manual approval UI](features/extension-approval-lifecycle.md). |
| [Customer authoring](features/conversational-authoring.md) | [Reusable scenarios/reports](features/reusable-simulation.md), [machine-readable contracts](features/machine-readable-contracts.md) and [manual review/activation](features/extension-approval-lifecycle.md) are useful without AI. The existing card owns the additional live customer conversation/repair workflow. |
| [Expression enhancement](features/enrich-followup-expressions.md) | Retains the invoice outcome and explicitly names type/absence/time rules, schemas, compiler, descriptors/wire values, both runtime validators/evaluators, actual CEL costs and shared examples. [Rich native customization](features/rich-task-customizations.md) owns actual queries/dates/persistence; [broader types](ideas/broader-language-types.md) remain a proposal. |

Existing feature IDs and completed history are preserved. No original queued acceptance obligation is discarded merely to make a smaller card.

## Design coverage

The architect and bounded source readers covered all **17 supplied baseline files** against current specifications, decisions, the nine earlier cards and both repositories at `dead34c`. This is source coverage, not approval of every draft detail or certification of real host guarantees. Historical inputs below remain subordinate to maintained contracts. External references and the unsupplied v0.6 document are not additional assumed scope.

| Baseline source | Roadmap disposition |
| --- | --- |
| [Package index](../docs/internal/design-baseline/README.md) | Overall scope and precedence; delivered core, planned host work and explicit proposals. |
| [Design review findings](../docs/internal/design-baseline/grill-findings.md) | Delivered portability/CEL; remaining [resource qualification](features/runtime-resource-qualification.md), [simulation](features/reusable-simulation.md) and [approval](features/extension-approval-lifecycle.md). Superseded value gates are excluded. |
| [Architecture](../docs/internal/design-baseline/public/keir-architecture.md) | All five milestones: core, real host, two agent personas, selected expansion and reusable distribution. |
| [Language v0](../docs/internal/design-baseline/public/keir-language-v0.md) | [Initial subset](features/specify-language-and-domain-model.md) delivered; [selected enrichment](features/enrich-followup-expressions.md), [current schemas](features/machine-readable-contracts.md) and [broader type proposal](ideas/broader-language-types.md). |
| [Domain model v0](../docs/internal/design-baseline/public/domain-model-v0.md) | [Generic binding](features/generic-hook-capability-contract.md) delivered; [real assurance](features/real-effect-admission.md), selected types, schemas and [tenant catalog proposal](ideas/tenant-capability-overlays.md). |
| [Compiler/artifact/runners](../docs/internal/design-baseline/public/compiler-artifact-and-runners-v0.md) | Portable compilation/execution delivered; [containment](features/runtime-resource-qualification.md), [many-extension performance](features/runtime-scale-observability.md) and [SDK/release compatibility](features/public-package-release.md) remain planned. |
| [Simulation and authoring](../docs/internal/design-baseline/public/simulation-and-authoring-v0.md) | Fixed harnesses delivered; [reusable tooling](features/reusable-simulation.md), [manual lifecycle](features/extension-approval-lifecycle.md) and [customer assistant](features/conversational-authoring.md) remain explicit. |
| [Task POC](../docs/internal/design-baseline/public/task-management-poc.md) | Native app/organization, real reads/effects/guard, both assistants and [rich native customization](features/rich-task-customizations.md); [focus-list consumer](ideas/response-projection-consumer.md) remains a proposal. |
| [Decision/evidence register](../docs/internal/design-baseline/public/decisions-and-evidence.md) | Historical expression evidence is distinct from full-program/native evidence. Preserve delivered checks and qualify remaining claims per feature. |
| [Hooks/tenant capabilities](../docs/internal/design-baseline/public/hooks-and-tenant-capabilities.md) | [Guard](features/pre-operation-guard.md), [host assistant](features/host-integration-assistant.md), [tenant endpoints](ideas/tenant-capability-overlays.md), projection and patch decisions. |
| [Host integration v0.5](../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md) | [Native events](features/demo-task-integration.md) include admission/version pinning, ordering, causation/re-entry and failure. [Enforced guard](features/task-create-guard.md), [activation](features/extension-approval-lifecycle.md), [conformance](features/reusable-simulation.md), [patches](ideas/proposed-task-patches.md), [projections](ideas/response-projection-consumer.md) and [multi-binding](ideas/multi-binding-composition.md) have owners. |
| [Native keys/multi-effects v0.7](../docs/internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md) | Original-key single-effect behavior delivered; [real admission](features/real-effect-admission.md) and [native proof](features/demo-task-integration.md) remain. [Multi-effects/cross-binding collision admission](ideas/multi-effect-composition.md) and optional host key slots are uncommitted. |
| [Draft structural schema](../docs/internal/design-baseline/public/schemas/extension-ir-v0.schema.json) | Historical input; [current machine-readable contracts](features/machine-readable-contracts.md) must match real profiles and preserve semantic checks outside schema validation. |
| [Draft task DCM](../docs/internal/design-baseline/public/examples/task-domain.yaml) | [Selected enrichment](features/enrich-followup-expressions.md), [real admission](features/real-effect-admission.md) and [native tests](features/demo-task-integration.md); pending declarations are not certifications. |
| [Draft digest](../docs/internal/design-baseline/public/examples/task-domain.sha256) | Illustrative content identity, not approval authority; [exact selection](features/extension-approval-lifecycle.md) and current examples own executable use. |
| [Draft follow-up source](../docs/internal/design-baseline/public/examples/followup-extension.yaml) | [Language enhancement](features/enrich-followup-expressions.md) and [rich native scenario](features/rich-task-customizations.md), preserving generic capability/customer ownership. |
| [Draft scenario oracle](../docs/internal/design-baseline/public/examples/followup-created.scenario.yaml) | [Reusable simulation](features/reusable-simulation.md) and [rich native cases](features/rich-task-customizations.md); expectations remain independent of execution. |

## Decisions and boundaries

- Both runtimes are initial scope and implemented for the supported profiles. Do not reintroduce Java-later sequencing.
- CEL is Keir's pure evaluator, not its competitor. Product value is accepted; there is no frozen-catalog, economic benchmark or adoption-proof prerequisite.
- Published `/3` and qualified numeric `/4` models are exact host-held definitions bound to complete invoker catalogs; historical execution and new producer tuples follow the [compatibility matrix](../docs/specs/compiler-artifact.md). Customer constants stay in source; hook facts, scope, native keys, publication and current authority remain host-owned. No default named grants or hidden follow-up capability returns.
- The v0.7 original-key direction supersedes v0.5 derived per-step downstream keys. Idempotency does not establish atomicity, exactly-once business effects or safe automatic replay. Multiple WRITEs remain outside the accepted contract until separately adopted and proven.
- No built-in IAM, arbitrary customer HTTP/SQL/code, effectful loops, durable waits, scheduler, compensation, distributed transaction engine, second task product or mandatory compiler service. Due dates are data. Ordinary internal HTTP/RPC invokers need no tenant endpoint feature.
- Provider/credential/spend choices, real admission, license/coordinates and optional proposals require explicit owner decisions before their related actions. This roadmap does not authorize live environment changes or public package publication.

## Working process

Use the existing architect, two core developers and dedicated demo developer when assigned concrete work. Design review resolves the next increment's gaps; developers maintain conformance and feed discoveries back into the contract; the manager accepts combined outcomes and updates this roadmap. Concrete assignments and acceptance use the owning feature and core-owned task record; queued cards do not assign work by themselves.

Code/mixed/governance tasks use [Agent Workflow](../docs/internal/workflow/current-process.md), with core-owned records for both repos and direct-main delivery without required PRs. Documentation/roadmap-only work is exempt. Keep [fast regression and explicit slow qualification](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/testing.md), private operational evidence outside tracked content, and recoverable cleanup of completed task worktrees.
