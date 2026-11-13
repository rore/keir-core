<a id="host-integration-assistant"></a>
# Design a host integration with a coding assistant

Give this guide to a coding assistant working with you on application code. It can help design the model, implement adapters and add conformance cases in an authorized workspace. Use [Embed Keir](../embedding.md) for the implementation path and the [Keir specification](../specs/README.md) for the exact contracts. For an assistant that runs inside the application and helps customers author extensions, see the [separate integration guide](extension-assistant.md).

## Task and inputs

Start with these inputs:

- This guide, [Embed Keir](../embedding.md), and the matching [language](../specs/language.md) and [DCM](../specs/domain-model.md) references.
- The Keir package version and its [contract versions](../specs/compiler-artifact.md#source-and-artifact-identity).
- Your application's model, hook lifecycle, service interfaces and authorization rules. If the model does not exist yet, supply the requirements needed to draft it.
- The expected result: a reviewed model, adapters, `bindModel` / `prepareProgram` / `invokeProgram` integration, result handling and tests for success, rejection and uncertain writes.

Supply the assistant with authorized host requirements and interface or source descriptions. Ask it to propose domain types, hooks, result consumers, the capability catalog, protected bindings, adapters and limits. An API schema describes inputs and outputs. Use evidence from the service for permission, atomicity and duplicate-handling guarantees.

Expose reusable native operations, not the customization being demonstrated: `tasks.create` creates a task; the extension supplies its project/title and decides that creation is a follow-up. Do not hide the program's eligibility or composition in a special adapter. Domain-specific atomic operations remain valid when they are actual host contracts. See the [capability ownership rule](../specs/generic-hook-capability-contract.md#responsibility-boundaries).

Design hooks around inputs, outputs, lifecycle and write limits. Use the [DCM reference](../specs/domain-model.md) for meaning and the [format matrix](../specs/compiler-artifact.md#source-and-artifact-identity) for matching schemas.

An adapter can call a local service or a remote API. Keep routing, credentials, trusted identity propagation, deadlines and result mapping in the host. A timed-out remote write may still commit. Preserve that uncertainty for the host to reconcile. See the [dispatch boundary](../specs/generic-hook-capability-contract.md#capability-invocation-across-services).

## Workflow

Preserve the host-defined payload, including fields this extension does not use. Keep customer project and title choices in the rule. [Nominal literals](../specs/generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority) give fixed identifiers a type. The host checks whether the customer can access the resource.

1. Clarify where customization is allowed and how the host invokes and consumes hook results.
2. Draft the scoped catalog and generic hook separately. Identify protected parameters, effect/lifecycle ceilings, aggregate limits, compatibility and unresolved guarantees. Add named capability restrictions only for an explicit host requirement; do not infer them from one proposed program.
3. Connect the complete catalog using the [binding API](../specs/host-model-binding.md). Propose conformance cases for the real adapters, including denial and uncertain-write outcomes.
4. Use the [compiler library or CLI](../../compiler-java/README.md) and [shared conformance fixtures](../../conformance/README.md) to check the proposal with the developer.
5. Submit an exact proposed revision for host-owner approval and publication; identify outstanding native guarantees without presenting engineering evidence as a runtime credential.

## Pre-operation decisions

Use REQUEST/BEFORE_COMMIT/DECISION for a supported authoritative decision. Discover the hook and operations in the host model; do not assume business-specific capabilities. The output is a closed host record with required `decision`, an enum containing exactly ALLOW and DENY. Other declared bounded scalar fields can carry plain-text explanations. The hook permits no possible WRITE, including false-guarded calls.

Use the existing binding and invocation methods. Authenticate and snapshot the proposed operation in host code, consume the host's own outcome, and block DENY or any failed evaluation. Handled BUSINESS_REJECTION with ALLOW is still failed evaluation. Authoritative no binding follows the native path; selected-but-unavailable blocks.

After ALLOW, recheck selection, approval, permissions, business invariants and mutable read facts at native commit. Keep original context, reserve native time within the enclosing deadline, and close cancelled attempts so late results cannot continue. Require actual read consistency evidence for authoritative remote facts. Observe native attempts and persistence separately from the decision. [Walkthrough](../decision-walkthrough.md) · [Complete contract](../specs/pre-operation-decisions.md)

## Adopting guarded-result scalar dataflow

Guarded-result scalar dataflow changes conditional-result availability, not the host API or native guarantees. Admit its exact [producer tuple](../specs/compiler-artifact.md#source-and-artifact-identity) only with matching compiler/runtime packages and qualification. Do not teach an older installed host this capability, silently migrate approved bytes, or rewrite old companion editions.

Keep the complete native result contract and validate it normally. An earlier successful guarded result is retained per invocation; a skipped result is genuinely absent. Source must prove existence with its exact Boolean BIND guard in a conditional true arm and prove optional-field presence separately. Supply no sentinel/default result and add no projection callback, consumer field or operation solely to hide this distinction.

Test identical compiler-produced bytes in Java and Node for true/skipped/failed calls, optional scalar presence, actual returned references, prepared-program reuse and effect receipts. Include unknown outcomes and invalid values after confirmation; neither may become fallback success. Preserve host-bound original keys, protected arguments, approval, deadlines and all static limits. Authoritative decisions remain zero-write and still require fresh native checks after ALLOW. Actual packaged-consumer evidence and deliberate host adoption are separate from passing source tests.

## Integrating numeric values

Development Core 0.5 supports the selected [numeric contract](../specs/domain-model.md#numeric-values); [independently installed candidate Java/Node packages](../specs/README.md#numeric-qualification-status) passed their pinned outcomes and helper checks. Public release, matching companion and final source/asset freeze remain pending. Published Core 0.4.0 is nonnumeric. Before adoption, verify the exact [tuple](../specs/compiler-artifact.md#source-and-artifact-identity), qualified asset identity and host definition. The [draft edition 9](customer-authoring-guide-v9.md) does not establish companion compatibility or authorize an upgrade.

- Use source `/3` + DCM `/4` for numeric types. Retain current `/2` + `/3` authoring for nonnumeric edits. Both new profiles emit producer 0.5.0/artifact `/4`; do not impersonate old producers. Preserve historical artifacts, models, selections and approvals. New compiler output needs fresh pins/approval even if its DCM is unchanged. Test retained nominal String types named Integer/Decimal.
- Keep bind/prepare/invoke signatures unchanged. Pass canonical strings for Integer/Decimal through business inputs, protected arguments, adapters and outcomes. Explicitly call [String normalization helpers](../specs/host-model-binding.md#exact-numeric-text-helpers) for text such as `100.00 → 100`; never coerce floats or round at admission. Keep metadata counts/deadlines in their existing representation. Validate complete records before exposing data to expressions or consuming a decision.
- Declare optional numerics using the existing optional-scalar form. Preserve zero as present and both field-presence/exact-guard proofs. Do not provide sentinel results or new callbacks to hide absence. The host consumer must validate every declared output field, including a Decimal amount; the teaching decision/message consumer is not a generic numeric consumer.
- Keep domain meaning and native constraints in the host: quantities, rate ranges, currencies, accounting rules, permissions and commit/replay guarantees. Core's exact Decimal representation supplies none of those guarantees. Supply real data; do not invent task cost/amount fields merely to demonstrate Decimal.
- Test strict/normalized text, precision/scale boundaries, exact versus rounded intermediates, optional/guarded paths, original keys, numeric protected/adapter fields, full receipts and both historical/new tuples. Compile once and execute the same bytes in both runtimes. Reuse immutable preparation, never invocation values across requests. Measure actual-runtime projection/parsing and concurrency; raw engine timings are not application benchmarks.

The [Core stock/quote programs and teaching commands](../specs/language.md#numeric-programs) have complete independent consumer/report evidence; the candidate package run also passed installed consumers. [Exact descriptor comparisons and bounded real-rule measurements](../specs/README.md#descriptor-and-bounded-runtime-evidence) now pass within their recorded scope. Matching companion and the complete tested Daymark tag-count/notes journey remain delivery obligations. Keep simulation, separate approval/activation, real native enforcement and live deployment evidence distinct. Package adoption does not authorize a live upgrade or provider request.

## Authority and tools

Use the coding assistant’s existing workspace tools for authorized source inspection, edits and tests. The [compiler](../../compiler-java/README.md) and [runtime APIs](../specs/host-model-binding.md) are available to those tools. Host-specific discovery, model review and publication workflows belong to the application. The host owner controls secrets, published definitions, permissions and approval.

<a id="legacy-read2-integration-example"></a>
<a id="walkthrough-design-and-verify-a-read2-host-integration"></a>
<a id="implementation-reference-earlier-read-only-integration"></a>
<a id="compatibility-walkthrough-for-catalog-bound-reads"></a>
<a id="run-the-packaged-hosts"></a>
<a id="hand-the-proposal-to-the-host-owner"></a>

## Archived walkthrough

See the [catalog-bound read integration walkthrough](../internal/history/catalog-bound-host-walkthrough.md) in documentation history.
