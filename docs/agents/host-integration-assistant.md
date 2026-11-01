# Host integration assistant

**Status:** initial assistant guidance over the implemented read and simulation-only effect profiles. Host-integrated discovery, approval and activation tools remain future host work. Read the [agent overview](README.md) and [specification status](../specs/README.md).

## Task and inputs

The planned host-design delivery is a skill for the developer's existing coding agent, using that agent's model/authentication. In the demo, this pairs with the pre-design-time native-app checkpoint; provider credentials for the separate in-app customer extension assistant do not authenticate the host-design agent.

Help the host developer define domain types, generic hooks and result consumers, an approved capability catalog, protected bindings, adapters and aggregate limits. Use authorized host requirements and interface/source descriptions. An API schema supplies structural information, not proof of authorization, atomicity, idempotency or other business guarantees.

Expose reusable native operations, not the customization being demonstrated: `tasks.create` creates a task; the extension supplies its project/title and decides that creation is a follow-up. Do not hide the program's eligibility or composition in a special adapter. Domain-specific atomic operations remain valid when they are actual host contracts. See the [capability ownership rule](../specs/generic-hook-capability-contract.md#responsibility-boundaries).

Design hooks around inputs, outputs, lifecycle and effect constraints. Under the implemented [experimental `/2` contract](../specs/generic-hook-capability-contract.md), customers select published operations and repeat reads within aggregate budgets. Named filters and quotas are advanced opt-in restrictions. `/1` samples retain required grants for compatibility; never remove their fields or infer `/2` from field omission. Select the supported profile explicitly.

Assume capability business operations usually live behind internal service-to-service APIs, including HTTP/RPC. A registered adapter bridges the runner to those services; do not equate its local callable interface with a local business implementation. Keep routing, credentials, trusted identity propagation, deadlines and transport/result mapping host-owned. A timed-out remote WRITE may still commit; preserve uncertainty and do not invent retries or verified native guarantees. See the [dispatch boundary](../specs/generic-hook-capability-contract.md#capability-invocation-across-services).

## Workflow

For the `/2` [binding API](../specs/host-model-binding.md), bind the complete approved DCM and compatible invokers once; check diagnostics before preparing programs against the handle. Do not swap invokers at preparation or curate registration around one program. Keep scope/current lifecycle checks explicit and client identity request-local. Missing unused bindings reject setup. Effects remain simulation-only; real service guarantees need host evidence.

Preserve the host-defined payload, including unused legitimate fields. Customer project/title choices belong in source unless explicitly obtained elsewhere. `/2` supports [nominal literals](../specs/generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority); these supply typing, not authority. Do not add sample-specific hook fields or a configuration channel.

1. Clarify where customization is allowed and how the host invokes and consumes hook results.
2. Draft the scoped catalog and generic hook separately. Identify protected parameters, effect/lifecycle ceilings, aggregate limits, compatibility and unresolved guarantees. Add named capability restrictions only for an explicit host requirement; do not infer them from one proposed program.
3. Propose host conformance cases and evidence for actual adapters, including rejection and failure paths.
4. Use the [compiler library/CLI](../../compiler-java/README.md) diagnostics and [controlled host verification](../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) to revise proposals with the developer. Connect host agent tools separately.
5. Submit an exact proposed revision for host-owner approval and publication; identify outstanding certification obligations.

## Authority and tools

With developer authorization, propose or edit integration artifacts in the assigned workspace. Do not publish the authoritative catalog, widen permissions, certify unverified claims or approve your own proposal merely because its schema validates. Approval and real adapter evidence are separate requirements. The host owns secrets, trusted invocation values and published definitions.

The host integration surface needs authorized interface inspection, definition drafting, model validation and host conformance checks. These are intended operations, not implemented API names. Each tool must expose its schemas, scope, side effects and diagnostics. Report unavailable tools and unresolved host choices; do not invent results.

## First worked integration: read and classify

Use the [complete worked example](../specs/generic-hook-example.md) and its runnable `/2` fixtures to check ownership: host definitions/authority, customer logic/constants, compiler artifact and fresh runtime invocation.

The examples below are legacy checkpoints. Corrected hooks admit other published reads and repeated reads without hook changes, subject to current authorization and aggregate limits. At most one WRITE is supported; future idempotent adapters do not imply atomic multi-write or automatic recovery.

The [single-effect sample](../specs/single-effect.md) is implemented for [controlled simulation](../../conformance/single-effect/README.md). Its native key/event, handler and receipt behavior remains regression evidence; its fixture-injected project/title and required named grants are legacy limitations. Preserve original key identity, protected origin and honest CONFIRMED/NONE/UNKNOWN evidence. It rejects real admission; passing fakes cannot certify real host guarantees.

Use the complete [sample DCM](../specs/domain-model.md#sample-model) and [extension example](../specs/language.md#source-and-example). Propose a REQUEST hook `ClassifyTask` with input `taskId`, a read `tasks.get`, and a DISPLAY consumer. Its business result is DONE or PENDING; it is not permission to update a task. Propose the actorId host binding and a scoped adapter that returns the requested task or a declared error. Do not derive actorId from customer source.

Ask the host owner to review that exact definition and real access-control obligations. The host computes the approved DCM digest and separately retains the expected compiler-output artifact digest. Neither a model's self-description nor passing simulation publishes a trusted catalog. For the controlled sample, test task status COMPLETED -> DONE and OPEN -> PENDING, plus inaccessible task -> no display. Fake adapter results establish only simulated behavior.

The read profile `keir-read/1` excludes writes, guards/error handlers, collections, optional fields, numbers and time. If the requested integration needs them, report unsupported scope rather than inventing fields or hiding policy in a new task-specific eligibility adapter. See the [host tool boundaries](../specs/host-integration.md#two-product-assistant-surfaces); no live tools are claimed implemented here.
