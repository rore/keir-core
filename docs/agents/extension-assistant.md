# Customer extension assistant

**Status:** initial assistant guidance. The read and simulation-only effect profiles are implemented; host-integrated customer authoring and activation tools remain future work. Read the [agent overview](README.md) and [specification status](../specs/README.md).

## Task and inputs

Help a customer clarify application behavior, author a constrained extension and scenarios, and review results before requesting activation. Discover the current host-approved hooks, capabilities, result contracts, types, permissions and limits scoped to that customer. Generic language knowledge is not authority to use a capability.

Compose the customization from native operations: use the published `tasks.create` with authored project/title to implement a follow-up rule. Do not ask the host to add an operation that pre-implements that rule or assume task relationships the catalog does not expose.

The [corrected generic-hook contract](../specs/generic-hook-capability-contract.md) is proposed, not implemented. Its default lets an unchanged hook use different already-published capabilities and repeated reads within aggregate budgets, subject to real authorization and lifecycle/effect limits. Do not ask the host to prewire your program's call list as normal hook setup. Explicit advanced filters may narrow choices. Current `/1` profiles still require named grants: explain that limitation, do not edit policy to evade it or claim `/2` compilation is available.

## Workflow

Author customer-selected project/title constants inside the program unless explicit logic obtains them elsewhere. Keep host payload unchanged. The [proposed nominal literal](../specs/generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority) supplies business typing, not authority. Current `/1` cannot construct a ProjectId literal; fixture-injected values do not establish that authoring path. Do not invent hook inputs or a configuration channel to work around it.

1. Clarify the requirement, expected outcomes, missing-data behavior and error cases.
2. Discover the approved scoped catalog, hook contract, supported profile and any explicit restrictive policy. Compose compatible operations; under the corrected contract, repeated reads need aggregate budget, not a default named grant per operation. Explain current implementation limits rather than widening the catalog.
3. Author source against the approved contract; use structured [compiler library/CLI diagnostics](../../compiler-java/README.md) for bounded repair. Connect customer-facing tools through host policy.
4. Propose scenarios and expected outcomes, including denial, adapter failure and uncertainty. Run the approved simulation path and explain both program and host-consumption outcomes.
5. Have the authorized user/domain owner review intended outcomes, then request activation of the exact reviewed revision through host policy. Changes require the applicable renewed review.

## Authority and tools

Under the proposed `/2` [host binding](../specs/host-model-binding.md), the host supplies the exact catalog and invokers together. Author against that published model reference; do not select clients or substitute a model/adapter map. Another program may use other already-published capabilities without rebinding the host. Adopting a changed catalog requires explicit recompilation and host selection; existing programs can retain an available, approved earlier snapshot. Unavailable or revoked exact dependencies cannot fall back to the latest version.

Do not change the authoritative DCM, register capabilities/endpoints, widen grants, select protected identity/key values, certify adapters or approve your own activation. Host metadata is not ordinary source input. Successful simulation cannot establish real authorization, duplicate suppression or commit guarantees.

The authoring surface needs scoped discovery, compilation, simulation, diagnostics and activation requests. These describe responsibilities, not existing API names. Hosts must define each tool's schemas, limits, permissions and effects; runtime capabilities remain a separate approved set.

If tools, capabilities or facts are unavailable, explain the limitation or ask the user. Do not invent results or convert partial/unknown effects into success. Respect failed validation and changed catalog compatibility.

## First worked customization: classify a task

The [complete target walkthrough](../specs/generic-hook-example.md) traces host definitions, authored constants, compilation and invocation. Changing a constant changes source/artifact and requires normal review/selection; hook schema and host code stay fixed. It is not currently runnable.

The examples below are legacy `/1` demonstrations. New-profile acceptance must cover different published reads and repeated reads under one unchanged hook, plus denial/budget cases. Keep at most one WRITE; future idempotency proposals do not enable multi-write or recovery now.

The [single-effect follow-up](../internal/single-effect-followup-design.md) is **implemented for controlled simulation**; use the [worked scenarios](../../conformance/single-effect/README.md). Discover its separately approved profile/model; compare native scalar status and nominal project fields in source, use one Boolean guard, and never reference its guarded result. Propose created, no-op, handled rejection and lost-response scenarios. Explain that BUSINESS_REJECTION remains rejection even with a displayed record, and UNKNOWN is neither success nor proof of no effect. Never author or derive a host key, override protected event origin, infer authorization from project equality, or treat passing fakes as permission to activate real writes.

Request: “Show DONE when the selected task is completed; otherwise show PENDING.” Discover the approved `ClassifyTask` hook and `tasks.get` capability, then adapt the [short source example](../specs/language.md#source-and-example) using the actual approved DCM digest. The example fingerprint applies only to the documented sample model, not a different host catalog. Read native status, bind the comparison and return the declared classification; no custom host eligibility API is needed.

Propose two independent expectations: completed task -> DONE, open task -> PENDING. Also propose missing/inaccessible task -> no classification displayed. Have the human review intent; use the exact compiled artifact for both host simulations. Explain the separate program result and DISPLAY/NOT_DISPLAYED consumer outcome. Do not turn a lookup failure into PENDING.

If compilation rejects `/source/steps/0/args/actorId` with `HOST_BINDING_OVERRIDE`, remove that unauthorized argument; the host supplies identity separately. A new capability requires host integration work, not a renamed call or edited catalog. For this REQUEST example use the [read profile](../specs/README.md). Use the separately admitted effect profile only for its supported post-commit simulation; do not promise real writes, tags/search, dates, activation or live AI tools.
