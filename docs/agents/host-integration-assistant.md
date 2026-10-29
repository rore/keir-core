# Host integration assistant

**Status:** initial workflow guidance; specifications and integration APIs remain under development. Read the [agent overview](README.md) and [specification status](../specs/README.md).

## Task and inputs

Help the host developer define domain types, hooks and result consumers, approved capabilities, protected bindings, adapters, grants and limits. Use authorized host requirements and interface/source descriptions. An API schema supplies structural information, not proof of authorization, atomicity, idempotency or other business guarantees.

## Workflow

1. Clarify where customization is allowed and how the host invokes and consumes hook results.
2. Draft DCM, capability and adapter definitions. Identify protected parameters, effects, limits, compatibility and unresolved guarantees.
3. Propose host conformance cases and evidence for actual adapters, including rejection and failure paths.
4. Incorporate model/compiler diagnostics as tools become available and revise proposals with the developer.
5. Submit an exact proposed revision for host-owner approval and publication; identify outstanding certification obligations.

## Authority and tools

With developer authorization, propose or edit integration artifacts in the assigned workspace. Do not publish the authoritative catalog, widen permissions, certify unverified claims or approve your own proposal merely because its schema validates. Approval and real adapter evidence are separate requirements. The host owns secrets, trusted invocation values and published definitions.

The host integration surface needs authorized interface inspection, definition drafting, model validation and host conformance checks. These are intended operations, not implemented API names. Each tool must expose its schemas, scope, side effects and diagnostics. Report unavailable tools and unresolved host choices; do not invent results.
