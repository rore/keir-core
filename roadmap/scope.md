# Keir roadmap

This is the single roadmap for `keir-core` and `keir-demo-app`: delivered features, planned deliveries and visible uncommitted proposals. The manager owns sequence and combined acceptance; each card identifies the repository that owns implementation.

Current focus is the user-approved correction of the real-application design. The [published audit](../docs/internal/real-application-design-audit.md) guides the [corrective core work](../.agent-workflow/tasks/host-contract-alignment.md): exact generic binding/current-authority contract, real consumer/state inventory, explicit minimal compatibility choice, then reviewed core and demo changes. Retain host-certified guarantees, semantic pins, keys, current permission and honest outcomes; remove prescribed deployment-certification mechanisms. Superseded implementation/rollout stays held until the concrete corrected plan is released. Preserve source, evidence and user data. Unassigned follow-ups remain queued.

## Tracking progress

- **Milestones describe outcomes; features own delivery.** Board order expresses priority; cards name actual dependencies. Milestones are not serial gates: native app work, tooling and experimental packaging may proceed independently.
- **Status lives on each card.** `queued` means unassigned; `active`/`in-progress` means assigned; `blocked` names an actual blocker; `done` requires evidence. Checkboxes expose design/specification, implementation and verified demonstration. A completed design checkbox is not a completed feature.
- **Commitment is explicit.** `features/` holds planned/delivered work; `ideas/` holds uncommitted proposals with `Proposal:` titles and decision gates. Filter by repository or commitment. Milestone inclusion does not adopt every draft detail; unequal feature counts are not a percentage-complete estimate.
- **Mature only the next increment.** Review draft gaps, publish supported contracts/schemas and positive/negative cases, implement, then verify the end-to-end scenario and update the specifications and agent guides. Do not freeze all design or finish an entire compiler layer before showing working behavior.

## Outcome milestones

| Milestone | Observable result and work content |
| --- | --- |
| **Execute extensions in Java and Node** | Delivered experimental core: initial language/DCM, restricted CEL, read/decide, scalar effects and generic `/2` model binding. Same artifacts run in both hosts; customer constants stay in source and effect evidence remains honest. The five completed cards retain their evidence. |
| **Customize a real task application** | Useful native app/organization; packaged real reads; explicitly qualified real single-effect admission plus native event/key/persistence evidence; a core guard contract and actual host enforcement. |
| **Author and activate customizations through AI** | Manual exact-revision review/simulation/approval/activation, a host-design skill for the visitor's coding agent and an in-app customer extension-authoring UI using their configured AI provider. Supporting work includes machine-readable contracts and reusable scenario/conformance tools. Optional API import is not a prerequisite. |
| **Extend host behavior safely** | Selected search/tag/168-hour language enhancement and real app integration. Broader types, projections, patches, multiple bindings, tenant endpoints and multiple effects remain individually visible proposals. |
| **Release an embeddable Keir library** | Current schemas/API/compatibility references, clean consumers, authorized licensing/publication, measured resource containment, many-extension performance and safe operational evidence. Experimental release need not wait for AI or all optional features. |

The [delivered core](features/generic-hook-capability-contract.md) passes local and [CI verification](https://github.com/rore/keir-core/actions/runs/41280457363). It does not implement every type, hook, lifecycle service or assurance in the drafts. Effects are currently **simulation-only**; real adapters and production containment remain unqualified.

## First deliveries

1. [Native task app](features/native-task-app.md), delivered: a polished clone-and-run local web app with persisted create/list/edit/complete and three seeded demo logins, with extensions and AI disabled. Each user represents a SaaS customer's isolated scope. The feature records the selected stack, measured checks, independent review and passing CI.
2. [Packaged real reads](features/real-task-reads.md), delivered: installed read/2 programs classify actual scoped tasks, with two user-specific programs, validated advisory display and standalone clone/run evidence. Native writes and Studio/AI integration remain separate work.
3. Mature [real-effect admission](features/real-effect-admission.md) alongside requirements from [native event/effect integration](features/demo-task-integration.md). Accept real writes only after the assurance gate; do not flip the simulation flag or substitute fake success for native evidence.

The [core guard](features/pre-operation-guard.md) can be designed independently; [native guard enforcement](features/task-create-guard.md) follows the app and contract. Rich search/time work does not block the scalar path. Minimal trusted operator-selected artifacts are enough initially; the full [approval lifecycle](features/extension-approval-lifecycle.md) remains a separate outcome.

The demo must visibly support different extensions at the same hook for different users, with service-enforced scope and independent activation. The [host-design assistant](features/host-integration-assistant.md) also owns a future `pre-design-time` starting branch/checkpoint for a visitor's coding agent to configure the host; `main` becomes the configured customer-customization journey. Preserve one native app and a reproducible checkpoint rather than two independently maintained products.

The complete demo includes [AI-driven extension creation](features/conversational-authoring.md), with operator-supplied provider/model/credentials and at least two qualified provider configurations. Native task management and execution of existing extensions do not require AI calls; that fallback is not the complete authoring demonstration. Host design-time uses the coding-agent skill and its existing authentication, independently of the demo server's provider setup. Ship no private keys.

The **Extension Studio is the demo's centerpiece**: primary agent chat with a supporting, agent-directed canvas for relevant visual and interactive UI. Canvas actions feed user input back into the conversation; there is no forced workflow wizard. Its [dedicated design/prototype feature](features/extension-studio-design.md) owns detailed UX, interactions, visual design and a user-reviewed end-to-end walkthrough before Studio UI implementation. Design can begin before backend completion; native CRUD stays a small foundation. Authoring/lifecycle features own the later executable behavior, not a competing experience design.

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
- `/2` uses exact host-held models bound to complete invoker catalogs. Customer constants stay in source; hook facts, scope, native keys, publication and current authority remain host-owned. No default named grants or hidden follow-up capability returns.
- The v0.7 original-key direction supersedes v0.5 derived per-step downstream keys. Idempotency does not establish atomicity, exactly-once business effects or safe automatic replay. Multiple WRITEs remain outside the accepted contract until separately adopted and proven.
- No built-in IAM, arbitrary customer HTTP/SQL/code, effectful loops, durable waits, scheduler, compensation, distributed transaction engine, second task product or mandatory compiler service. Due dates are data. Ordinary internal HTTP/RPC invokers need no tenant endpoint feature.
- Provider/credential/spend choices, real admission, license/coordinates and optional proposals require explicit owner decisions before their related actions. This roadmap does not authorize live environment changes or public package publication.

## Working process

Use the existing architect, two core developers and dedicated demo developer when assigned concrete work. Design review resolves the next increment's gaps; developers maintain conformance and feed discoveries back into the contract; the manager accepts combined outcomes and updates this roadmap. Concrete assignments and acceptance use the owning feature and core-owned task record; queued cards do not assign work by themselves.

Code/mixed/governance tasks use [Agent Workflow](../docs/internal/workflow/current-process.md), with core-owned records for both repos and direct-main delivery without required PRs. Documentation/roadmap-only work is exempt. Keep [fast regression and explicit slow qualification](../docs/testing.md), private operational evidence outside tracked content, and recoverable cleanup of completed task worktrees.
