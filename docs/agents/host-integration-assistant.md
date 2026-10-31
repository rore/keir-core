# Host integration assistant

**Status:** initial assistant guidance over the implemented read and simulation-only effect profiles. Host-integrated discovery, approval and activation tools remain future host work. Read the [agent overview](README.md) and [specification status](../specs/README.md).

## Task and inputs

Help the host developer define domain types, generic hooks and result consumers, an approved capability catalog, protected bindings, adapters and aggregate limits. Use authorized host requirements and interface/source descriptions. An API schema supplies structural information, not proof of authorization, atomicity, idempotency or other business guarantees.

Design new hooks around inputs, allowed outputs, lifecycle and effect constraints, not a sample program's capability list. Customers compose already-published operations and repeat reads within aggregate budgets. Capability allow/deny lists and per-capability quotas are advanced opt-in restrictions only. Required per-hook, admission and invocation `grants` in current `/1` samples are an architectural limitation, not the intended default. The [corrected `/2` contract](../specs/generic-hook-capability-contract.md) is proposed and cannot yet be executed. If asked for a runnable current sample, retain its required legacy fields and disclose the limitation; never silently strip them or claim corrected support.

## Workflow

Preserve the host-defined payload, including legitimate fields unused by a sample. Customer project/title choices belong as program constants unless explicitly obtained elsewhere. Current fixture injection is not that authoring path; the [nominal-literal correction](../specs/generic-hook-capability-contract.md#event-facts-extension-choices-and-protected-authority) is not implemented. Do not add hook fields or configuration machinery to work around missing language support.

1. Clarify where customization is allowed and how the host invokes and consumes hook results.
2. Draft the scoped catalog and generic hook separately. Identify protected parameters, effect/lifecycle ceilings, aggregate limits, compatibility and unresolved guarantees. Add named capability restrictions only for an explicit host requirement; do not infer them from one proposed program.
3. Propose host conformance cases and evidence for actual adapters, including rejection and failure paths.
4. Use the [compiler library/CLI](../../compiler-java/README.md) diagnostics and [controlled host verification](../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) to revise proposals with the developer. Connect host agent tools separately.
5. Submit an exact proposed revision for host-owner approval and publication; identify outstanding certification obligations.

## Authority and tools

With developer authorization, propose or edit integration artifacts in the assigned workspace. Do not publish the authoritative catalog, widen permissions, certify unverified claims or approve your own proposal merely because its schema validates. Approval and real adapter evidence are separate requirements. The host owns secrets, trusted invocation values and published definitions.

The host integration surface needs authorized interface inspection, definition drafting, model validation and host conformance checks. These are intended operations, not implemented API names. Each tool must expose its schemas, scope, side effects and diagnostics. Report unavailable tools and unresolved host choices; do not invent results.

## First worked integration: read and classify

Use the [complete target walkthrough](../specs/generic-hook-example.md) to check ownership: host definitions/authority, customer logic/constants, compiler artifact and runtime invocation. The corrected example is not currently runnable.

The examples below are legacy checkpoints. Corrected hooks admit other published reads and repeated reads without hook changes, subject to current authorization and aggregate limits. At most one WRITE is supported; future idempotent adapters do not imply atomic multi-write or automatic recovery.

The [single-effect sample](../specs/single-effect.md) is implemented for [controlled simulation](../../conformance/single-effect/README.md). Its native key/event, handler and receipt behavior remains regression evidence; its fixture-injected project/title and required named grants are legacy limitations. Preserve original key identity, protected origin and honest CONFIRMED/NONE/UNKNOWN evidence. It rejects real admission; passing fakes cannot certify real host guarantees.

Use the complete [sample DCM](../specs/domain-model.md#sample-model) and [extension example](../specs/language.md#source-and-example). Propose a REQUEST hook `ClassifyTask` with input `taskId`, a read `tasks.get`, and a DISPLAY consumer. Its business result is DONE or PENDING; it is not permission to update a task. Propose the actorId host binding and a scoped adapter that returns the requested task or a declared error. Do not derive actorId from customer source.

Ask the host owner to review that exact definition and real access-control obligations. The host computes the approved DCM digest and separately retains the expected compiler-output artifact digest. Neither a model's self-description nor passing simulation publishes a trusted catalog. For the controlled sample, test task status COMPLETED -> DONE and OPEN -> PENDING, plus inaccessible task -> no display. Fake adapter results establish only simulated behavior.

The read profile `keir-read/1` excludes writes, guards/error handlers, collections, optional fields, numbers and time. If the requested integration needs them, report unsupported scope rather than inventing fields or hiding policy in a new task-specific eligibility adapter. See the [host tool boundaries](../specs/host-integration.md#two-product-assistant-surfaces); no live tools are claimed implemented here.
