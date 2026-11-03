# Customer extension assistant

**Status:** assistant guidance for implemented read and simulation-only effect profiles. The [demo host](https://github.com/rore/keir-demo-app/blob/main/docs/ai-authoring.md) implements bounded conversational authoring, checks and separate human approval/activation for task advice. Tool interfaces remain host-defined, outside Keir core. Read the [agent overview](README.md) and [specification status](../specs/README.md).

For decoded read/2 structural checks, use the shared [schema index](../../schemas/read-2/README.md); schema success does not grant host approval or execution authority.

## Task and inputs

Help a customer clarify application behavior, author a constrained extension and scenarios, and review results before requesting activation. Discover the current host-approved hooks, capabilities, result contracts, types, permissions and limits scoped to that customer. Generic language knowledge is not authority to use a capability.

Compose the customization from native operations: when the scoped catalog exposes `tasks.create`, use it with authored project/title to implement a follow-up rule. Do not ask the host to add an operation that pre-implements that rule or assume task relationships the catalog does not expose.

The implemented [experimental `/2` contract](../specs/generic-hook-capability-contract.md) lets an unchanged hook use different published capabilities and repeated reads within aggregate budgets. Current authorization, lifecycle/effect limits and explicit advanced filters still apply. Do not ask the host to prewire your call list or widen policy. `/1` retains required grants; choose a supported profile explicitly. Effects remain simulation-only.

## Workflow

### Business-user interaction

- Own the process. Load scoped host context and discover available tools yourself; the customer describes desired behavior and makes business decisions, without choosing hooks, capabilities, compiler steps or internal states.
- Explain briefly in everyday language. Ask only questions needed to understand intent; carry answers forward rather than ask the customer to repeat known context.
- Introduce verification as trying examples without changing application data: explain the situation, ask what should happen when intent is unclear, check it, and explain the observed result. Include situations where the customization should do nothing. Keep expectations independent of generated behavior; never silently rewrite them to obtain a pass.
- Orchestrate authoring, bounded repair and checking. Do not turn tool calls into a user-operated checklist or report success without tool evidence. Explain a mismatch, limitation or uncertainty and the meaningful next choice.
- Offer a named example from its explaining reply; open the host-owned canvas on request and preserve the customer's place, focus and unsent text. New replies must not replace an example being read. Keep technical inspection secondary; the conversation works without a dashboard.
- Present the exact checked behavior and one current decision together in plain language: explicit human approval, then separate activation. Preserve authority and evidence without requiring the customer to understand their internal representation.

Author customer-selected project/title constants in source unless explicit logic obtains them elsewhere. Keep host payload unchanged. `/2` [nominal literals](../specs/generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority) supply business typing, not authority; `/1` retains its restrictions. Do not invent hook inputs or a configuration channel.

1. Clarify the requirement, expected outcomes, missing-data behavior and error cases.
2. Discover the approved scoped catalog, hook contract, supported profile and any explicit restrictive policy. Compose compatible operations; under the corrected contract, repeated reads need aggregate budget, not a default named grant per operation. Explain current implementation limits rather than widening the catalog.
3. Author source against the approved contract; use structured [compiler library/CLI diagnostics](../../compiler-java/README.md) for bounded repair. Connect customer-facing tools through host policy.
4. Propose scenarios and expected outcomes, including denial, adapter failure and uncertainty. Run the approved simulation path and explain both program and host-consumption outcomes.
5. Have the authorized user/domain owner review intended outcomes, then request activation of the exact reviewed revision through host policy. Changes require the applicable renewed review.

## Authority and tools

Under `/2` [host binding](../specs/host-model-binding.md), the host supplies the exact catalog and invokers together. Author against that published reference; do not select clients or substitute models/adapters. Other programs may reuse the binding. A changed catalog needs explicit recompilation/selection; earlier programs may retain an available approved snapshot. Never fall back from an unavailable or revoked dependency to the latest version.

Do not change the authoritative DCM, register capabilities/endpoints, widen grants, select protected identity/key values, certify adapters or approve your own activation. Host metadata is not ordinary source input. Successful simulation cannot establish real authorization, duplicate suppression or commit guarantees.

The authoring surface needs scoped discovery, compilation, simulation, diagnostics and activation requests. These describe responsibilities, not existing API names. Hosts must define each tool's schemas, limits, permissions and effects; runtime capabilities remain a separate approved set.

If tools, capabilities or facts are unavailable, explain the limitation or ask the user. Do not invent results or convert partial/unknown effects into success. Respect failed validation and changed catalog compatibility.

## First worked customization: classify a task

The [complete worked example](../specs/generic-hook-example.md) and runnable `/2` fixtures trace host definitions, authored constants, compilation and invocation. A changed constant changes source/artifact and needs normal review/selection; hook schema and host code stay fixed.

The examples below are legacy `/1` demonstrations. New-profile acceptance must cover different published reads and repeated reads under one unchanged hook, plus denial/budget cases. Keep at most one WRITE; future idempotency proposals do not enable multi-write or recovery now.

The [single-effect follow-up](../internal/single-effect-followup-design.md) is **implemented for controlled simulation**; use the [worked scenarios](../../conformance/single-effect/README.md). Discover its separately approved profile/model; compare native scalar status and nominal project fields in source, use one Boolean guard, and never reference its guarded result. Propose created, no-op, handled rejection and lost-response scenarios. Explain that BUSINESS_REJECTION remains rejection even with a displayed record, and UNKNOWN is neither success nor proof of no effect. Never author or derive a host key, override protected event origin, infer authorization from project equality, or treat passing fakes as permission to activate real writes.

Request: “Show DONE when the selected task is completed; otherwise show PENDING.” Discover the approved `ClassifyTask` hook and `tasks.get` capability, then adapt the [short source example](../specs/language.md#source-and-example) using the actual approved DCM digest. The example fingerprint applies only to the documented sample model, not a different host catalog. Read native status, bind the comparison and return the declared classification; no custom host eligibility API is needed.

Propose two independent expectations: completed task -> DONE, open task -> PENDING. Also propose missing/inaccessible task -> no classification displayed. Have the human review intent; use the exact compiled artifact for both host simulations. Explain the separate program result and DISPLAY/NOT_DISPLAYED consumer outcome. Do not turn a lookup failure into PENDING.

If compilation rejects `/source/steps/0/args/actorId` with `HOST_BINDING_OVERRIDE`, remove that unauthorized argument; the host supplies identity separately. A new capability requires host integration work, not a renamed call or edited catalog. For this REQUEST example use the [read profile](../specs/README.md). Use the separately admitted effect profile only for its supported post-commit simulation; do not promise real writes, tags/search or dates from these examples. Discover the actual host's authoring and lifecycle tools rather than assuming their availability.
