<a id="host-integration-assistant"></a>
<a id="design-a-host-integration-with-a-coding-assistant"></a>
# Connect Keir to a host application

Use this checklist with a coding assistant when implementing a host. Keep application data, services and authority in the host; Keir supplies constrained rule compilation and execution.

<a id="task-and-inputs"></a>
## Inputs

- The actual operation/event boundary and desired user customization.
- Native authentication, authorization, transaction, replay and deadline contracts.
- The installed package pins and supported [compiler/runtime profile](../specs/compiler-artifact.md#source-and-artifact-identity).
- Existing host services and a model describing approved hooks, inputs, types and operations.

<a id="workflow"></a>
## Implement

1. Choose the lifecycle. After-commit event rules may make at most one possible write. Before-save decisions are read-only; ALLOW permits native checks to continue rather than proving a save.
2. Expose [reusable native operations](../embedding.md#1-define-what-the-host-exposes); keep eligibility and follow-up composition in the extension, while retaining genuine host-guaranteed atomic operations. Publish the model for the customer's scope. Keep event facts, customer-authored choices and protected host authority distinct. Adding an operation changes model identity; choosing another already-published operation does not.
3. <a id="authority-and-tools"></a> Implement adapters with actual native services. Authorize each call, preserve the original key and report NONE, CONFIRMED or UNKNOWN according to what the service can establish. A READ declaration is a promise of a real read, not a sandbox around arbitrary host code.
4. Bind the complete implementation set, verify the approved artifact's trusted bytes and prepare it. Reuse preparation only for the same exact model/artifact; supply fresh input, actor, event/attempt, original key and absolute deadline on each invocation.
5. Keep simulation separate from real adapters. Compare the same compiler-produced bytes in Java and Node with independent expected outcomes, ordered calls, protected arguments, traces and receipts.
6. Add host-owned review, separate activation and saved-result recovery. Recompilation creates a new revision; it never transfers an earlier approval or silently replaces active bytes.

<a id="pre-operation-decisions"></a>
## Before-save boundary

Authoritative absence takes the ordinary native path. A selected but unavailable/incompatible rule blocks the attempt. Only a valid completed ALLOW with empty effects permits continuation; DENY, handled rejection, failure, malformed output, cancellation and expiry block it.

Snapshot the proposal. Recheck approval/selection, permissions, mutable read facts and business invariants at the native commit boundary, including for no-call plans. Reserve time for native validation/commit and carry the enclosing absolute expiry through nested work. Late output cannot reopen a cancelled attempt. A prior remote read alone is not a consistency guarantee; qualify an actual host protocol.

## Acceptance

Test real ownership, original-key authenticity, native persistence/non-persistence, races, cancellation, revocation and uncertain-write reconciliation separately from simulation. Core validates a non-null key; the host authenticates its native meaning. A successful source result is not a write receipt, and a timeout is not proof of no effect.

<a id="adopting-guarded-result-scalar-dataflow"></a>
<a id="integrating-numeric-values"></a>
For conditional results and numeric inputs, use the selected profile's [language rules](../specs/language.md#steps-and-data-flow) and [exact-number host helpers](../specs/host-model-binding.md#exact-numeric-text-helpers).

<a id="archived-walkthrough"></a>
<a id="compatibility-walkthrough-for-catalog-bound-reads"></a>
<a id="implementation-reference-earlier-read-only-integration"></a>
<a id="legacy-read2-integration-example"></a>
<a id="walkthrough-design-and-verify-a-read2-host-integration"></a>
Earlier catalog-bound read integration remains in the [historical walkthrough](../internal/history/catalog-bound-host-walkthrough.md#compatibility-walkthrough-for-catalog-bound-reads).

<a id="run-the-packaged-hosts"></a>
<a id="hand-the-proposal-to-the-host-owner"></a>
Its [packaged-host commands](../internal/history/catalog-bound-host-walkthrough.md#run-the-packaged-hosts) and [host-owner handoff](../internal/history/catalog-bound-host-walkthrough.md#hand-the-proposal-to-the-host-owner) describe that earlier contract.

Use the [embedding guide](../embedding.md), [host API](../specs/host-model-binding.md), [decision contract](../specs/pre-operation-decisions.md) and [runtime contract](../specs/runtime.md). Select a [matching authoring packet](authoring-companion.md) for language rules; do not invent callbacks or extra model fields to hide unsupported behavior.
