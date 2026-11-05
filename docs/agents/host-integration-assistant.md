<a id="host-integration-assistant"></a>
# Design a host integration with a coding assistant

Give this guide to a coding assistant working with you on application code. It can help design the model, implement adapters and add conformance cases in an authorized workspace. Use [Embed Keir](../embedding.md) for the implementation path and the [Keir specification](../specs/README.md) for the exact contracts. For an assistant that runs inside the application and helps customers author extensions, see the [separate integration guide](extension-assistant.md).

## Task and inputs

Supply the assistant with authorized host requirements and interface or source descriptions. Ask it to propose domain types, hooks, result consumers, the capability catalog, protected bindings, adapters and limits. An API schema describes inputs and outputs. Use evidence from the service for permission, atomicity and duplicate-handling guarantees.

Expose reusable native operations, not the customization being demonstrated: `tasks.create` creates a task; the extension supplies its project/title and decides that creation is a follow-up. Do not hide the program's eligibility or composition in a special adapter. Domain-specific atomic operations remain valid when they are actual host contracts. See the [capability ownership rule](../specs/generic-hook-capability-contract.md#responsibility-boundaries).

Design hooks around inputs, outputs, lifecycle and write limits. Use the [DCM reference](../specs/domain-model.md) for the current model and the [structural schemas](../../schemas/effect-4/README.md) for decoded documents.

An adapter can call a local service or a remote API. Keep routing, credentials, trusted identity propagation, deadlines and result mapping in the host. A timed-out remote write may still commit. Preserve that uncertainty for the host to reconcile. See the [dispatch boundary](../specs/generic-hook-capability-contract.md#capability-invocation-across-services).

## Workflow

Preserve the host-defined payload, including fields this extension does not use. Keep customer project and title choices in the rule. [Nominal literals](../specs/generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority) give fixed identifiers a type. The host checks whether the customer can access the resource.

1. Clarify where customization is allowed and how the host invokes and consumes hook results.
2. Draft the scoped catalog and generic hook separately. Identify protected parameters, effect/lifecycle ceilings, aggregate limits, compatibility and unresolved guarantees. Add named capability restrictions only for an explicit host requirement; do not infer them from one proposed program.
3. Connect the complete catalog using the [binding API](../specs/host-model-binding.md). Propose conformance cases for the real adapters, including denial and uncertain-write outcomes.
4. Use the [compiler library or CLI](../../compiler-java/README.md) and [shared conformance fixtures](../../conformance/README.md) to check the proposal with the developer.
5. Submit an exact proposed revision for host-owner approval and publication; identify outstanding native guarantees without presenting engineering evidence as a runtime credential.

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
