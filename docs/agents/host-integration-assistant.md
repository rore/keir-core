# Host integration assistant

**Status:** initial assistant guidance over the implemented read profile. Host-integrated discovery, approval and activation tools remain future host work. Read the [agent overview](README.md) and [specification status](../specs/README.md).

## Task and inputs

Help the host developer define domain types, hooks and result consumers, approved capabilities, protected bindings, adapters, grants and limits. Use authorized host requirements and interface/source descriptions. An API schema supplies structural information, not proof of authorization, atomicity, idempotency or other business guarantees.

## Workflow

1. Clarify where customization is allowed and how the host invokes and consumes hook results.
2. Draft DCM, capability and adapter definitions. Identify protected parameters, effects, limits, compatibility and unresolved guarantees.
3. Propose host conformance cases and evidence for actual adapters, including rejection and failure paths.
4. Use the [compiler library/CLI](../../compiler-java/README.md) diagnostics and [controlled host verification](../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) to revise proposals with the developer. Connect host agent tools separately.
5. Submit an exact proposed revision for host-owner approval and publication; identify outstanding certification obligations.

## Authority and tools

With developer authorization, propose or edit integration artifacts in the assigned workspace. Do not publish the authoritative catalog, widen permissions, certify unverified claims or approve your own proposal merely because its schema validates. Approval and real adapter evidence are separate requirements. The host owns secrets, trusted invocation values and published definitions.

The host integration surface needs authorized interface inspection, definition drafting, model validation and host conformance checks. These are intended operations, not implemented API names. Each tool must expose its schemas, scope, side effects and diagnostics. Report unavailable tools and unresolved host choices; do not invent results.

## First worked integration: read and classify

Use the complete [sample DCM](../specs/domain-model.md#sample-model) and [extension example](../specs/language.md#source-and-example). Propose a REQUEST hook `ClassifyTask` with input `taskId`, a read `tasks.get`, and a DISPLAY consumer. Its business result is DONE or PENDING; it is not permission to update a task. Propose the actorId host binding and a scoped adapter that returns the requested task or a declared error. Do not derive actorId from customer source.

Ask the host owner to review that exact definition and real access-control obligations. The host computes the approved DCM digest and separately retains the expected compiler-output artifact digest. Neither a model's self-description nor passing simulation publishes a trusted catalog. For the controlled sample, test task status COMPLETED -> DONE and OPEN -> PENDING, plus inaccessible task -> no display. Fake adapter results establish only simulated behavior.

Current candidate `keir-read/1` excludes writes, guards/error handlers, collections, optional fields, numbers and time. If the requested integration needs them, report unsupported scope rather than inventing fields or hiding policy in a new task-specific eligibility adapter. See the [host tool boundaries](../specs/host-integration.md#two-product-assistant-surfaces); no live tools are claimed implemented here.
