# Real single-effect admission

Status: **core protocol implemented and locally exercised; native deployment not qualified**. This is the bounded contract for the first real task integration. It extends the [single-effect semantics](single-effect.md), [generic catalog contract](generic-hook-capability-contract.md) and [host model binding](host-model-binding.md). The [SQLite host handoff](../internal/real-task-effect-handoff.md) supplies the concrete first qualification. Compiler, Java and Node deterministic fixtures exercise the same artifacts and separate mode authority; independent delivery review and actual native qualification remain separate. This document grants no live execution authority.

## Compatibility boundary

Introduce exactly `keir-effect/3`, `keir-dcm/effect-3`, `keir-artifact/effect-3` and `keir-scenario/effect-3`. The compiler selects the profile explicitly (`--profile=keir-effect/3`, Java `Environment.effect3()`). No format detection by optional field presence and no automatic upgrade of existing bytes.

The DCM retains the effect-2 closed schema except that `format` is `keir-dcm/effect-3` and `assurance` is exactly `HOST_QUALIFIED_SINGLE_WRITE`. That value declares the required admission protocol; it is not evidence of host qualification. Entrypoints remain EVENT/AFTER_COMMIT with EVENT_REPORT consumption. REQUEST, BEFORE_COMMIT, multiple possible writes and unsupported transport guarantees remain rejected. Catalogs expose generic native operations, not customer rules.

The artifact retains the effect-2 reference envelope and executable semantics with profile/format/modelRef format changed to /3. Source remains `keir/v0`; its domain fingerprint identifies the /3 DCM. Existing checked-expression and descriptor recipe rules are unchanged. The compiler counts all potential WRITE calls, including mutually exclusive guards: maximum one; zero is valid. Effect ceiling defaults remain one and narrowing-only. No new expression construct, source key access or business configuration channel.

Every /1 and /2 format, schema, API behavior and rejection stays unchanged. In particular, effect/2 still requires SIMULATION_ONLY and rejects real mode. Read/2 does not acquire write permission. Unknown /3 assurance, mixed formats, stale compiler/environment pins and mismatched model references fail before a handle or dispatch is produced.

## Host qualification input

Reuse the binding/preparation/invocation API families. Add one optional final argument to `bindModel`; Java adds an overload rather than changing old callers:

```text
bindModel(dcmBytes, approval, invokers, qualification?)
qualification = {evidence, isCurrent}
```

`qualification` is required only for effect/3 with `approval.mode = "real"`. It is forbidden in simulation mode and all earlier profiles. Approval otherwise retains its existing exact fields, including scope, modelRef, environment, mode and native keyBinding. Invoker records retain `{version, mode, keyType, invoke}` for all capabilities in an effect catalog. Preparation and invocation signatures remain unchanged; Java effect/3 uses the existing effect invocation record's scope/bindings/event/key/authorization/deadline fields. Add profile-aware dispatch, not a second interpreter.

`evidence` is a closed, host-only data record:

```text
{
  contract: "keir-real-effect-evidence/1",
  id: String,
  revision: positive safe integer,
  scope: String,
  modelRef: existing exact ModelRef,
  environment: existing exact Environment,
  hostBuild: "sha256:<64 lowercase hex digits>",
  keyType: String,
  capabilities: [{id: String, version: String}],
  reportDigest: "sha256:<64 lowercase hex digits>"
}
```

Bound evidence to 16,384 canonical UTF-8 bytes before copying; reject unknown fields. `id` is nonempty, at most 128 UTF-8 bytes; scope/keyType retain existing 256-byte limits; all strings must be well-formed. Capability identifiers/versions use existing DCM validation and limits. Capabilities are sorted by ID, unique, and exactly enumerate the complete registered catalog, including reads and unused capabilities. ModelRef and environment exactly match approval; scope equals approval scope; keyType equals DCM hostKey.type and registered native key binding. `hostBuild` identifies the reviewed adapter/service/schema deployment. Changing those semantics or callable registrations requires new binding and qualification, never mutable substitution under an existing handle.

`isCurrent` is a trusted synchronous, non-I/O callback returning exactly Boolean. It closes over an independently approved deployment record and verifies this evidence revision/report, host build, scope and revocation/expiry state. Async/thenable/non-Boolean return or exception fails closed. The host implements it; it is never accepted from source, scenario, browser or artifact. Snapshot the evidence and callback reference with the binding. Do not cache its result.

The report identified by reportDigest must be reviewed qualification evidence for the deployed native service: original key preservation; atomic matching-key/payload receipts; mismatch rejection; retention without reuse; current owner/resource authorization; truthful commit evidence; crash, timeout and concurrent duplicate tests; hidden retry prohibition. The host deployment approval explicitly selects that report and build. Runtime checks identity and current approval; it cannot prove database atomicity or certify a dishonest host from a JSON claim. An allowlisted host-owned report is required; a customer-supplied digest, DCM assurance or successful simulation is insufficient. No remote report fetching, registry, signature infrastructure or generic transport package is introduced.

For the first qualification, supported real invokers use the local SQLite transaction boundary defined in the handoff. Remote service registrations are not qualified by that evidence, even if their method signatures match. Other hosts need separately reviewed reports and explicit support; no universal production claim.

## Validation and revocation

Binding validates, in order: existing byte/schema/model approval rules; profile/mode compatibility; evidence shape and exact equality; all invoker registrations; then `isCurrent()`. It invokes no business adapter or existing per-user authorizer. Failure produces no BoundModel. Add `qualification` as a closed optional fourth argument; null is not omission.

Preparation retains external expected artifact digest before parse, exact artifact/model/compiler/environment/entrypoint/consumer validation, and ordinary plan checks. Real effect/3 preparation also requires the retained qualification to be current. Evidence belongs to BoundModel, never to source or artifact; its identity is retained with Prepared for audit. A later deployment using new evidence explicitly prepares a new binding; it cannot rewrite an old handle.

At invocation entry and before every reached READ or WRITE, require current qualification in addition to existing scope, current host authorization, policy, effect ceiling, argument, budget and deadline checks. Check qualification before invoking the per-user authorizer or adapter. Current authorization still closes over protected actor/event/resource identity. Qualification approval is not user permission, and program approval is not qualification.

Recheck the remaining deadline after the synchronous qualification callback and before dispatch. The callback has no permission to extend budgets or reserve an effect. A blocking host callback violates the host contract; neither runtime claims to preempt arbitrary host code.

If qualification changes during a call, that call's evidence still settles under the existing response/timeout winner rules. Recheck before any subsequent dispatch; do not erase a CONFIRMED receipt or claim rollback. The native adapter must check authority immediately before its own commit. No claim that callback revocation recalls work already dispatched.

Binding/preparation qualification failures use `ADMISSION_INVALID` at `/binding/qualification` or `/admission/qualification`; size excess uses `BUDGET_EXCEEDED`. Invocation/dispatch revocation or callback failure uses `AUTHORITY_DENIED`, with ordinary profile termination and retained effect evidence. No raw evidence, user IDs, keys, service exceptions or payloads enter diagnostics/trace.

## Same program, separate execution authority

An effect/3 artifact can be prepared in either simulation or real mode against the same exact /3 DCM/modelRef, compiler and environment. Simulation requires all invokers to be simulation-mode and rejects qualification; real requires all registrations to be real-mode and the current qualification above. Neither mode inherits a Prepared handle from the other. No fallback between modes on failure.

The effect/3 scenario envelope retains effect-2's strict input, trusted fixture selection, ordered stubs and expected outcome/calls/consumer fields, changing only format. Fixtures construct protected event/key values; they cannot supply real invokers or qualification. The simulation adapter records generic create arguments and returns fixtures without touching native task tables. Business expectations are independently chosen before observing results. Approval pins the exact artifact and scenario evidence; enabling the same artifact additionally requires real host qualification and current user authority. No recompile or behavior change between checked and real program bytes.

## Effect outcomes and key identity

Preserve the effect/2 native key channel. The host supplies one original immutable native requestKey with a validating keyBinding. Pass the exact original value/object to each adapter, separately from business/protected scalar arguments. No stringify/hash/derive/step suffix or source-controlled replacement. Persistent native key serialization and rehydration are the host key contract, not a runtime conversion. Rehydrated values must represent the same original native identity; within each invocation all dispatches receive the same original instance/value.

Use existing WRITE envelopes without extension:

| Adapter result | Effect evidence |
| --- | --- |
| SUCCESS with valid outer CONFIRMED | Native commit or authoritative matching duplicate receipt |
| BUSINESS_REJECTION / TRANSIENT_FAILURE with NONE | Definite no effect for this attempted dispatch |
| UNKNOWN_COMMIT with UNKNOWN | Commit cannot be established |

After dispatch, timeout/exception/malformed outer envelope cannot establish NONE. Winning valid outer evidence settles before value/deadline validation; malformed result after CONFIRMED remains failed-with-CONFIRMED. Timeout winning first remains UNKNOWN; later evidence cannot rewrite the immutable runtime outcome or resume execution. Host read-only reconciliation is a separately dated native observation, not a replacement outcome or replay instruction. EVENT_REPORT never executes an action or infers an effect from a program-authored label.

## Required shared acceptance

Run the same artifact, model and vector identities through compiler, Java and Node. Extend existing effect scenarios rather than duplicating language semantics.

1. /2 still rejects real mode; /3 rejects wrong/missing evidence, extra fields, oversized evidence, null, wrong model/scope/environment/key type/catalog/build approval and stale revision. Zero adapter calls.
2. Simulation rejects qualification and mixed/real invokers; real rejects simulation invokers. Same artifact passes the expected program scenarios in both properly admitted modes.
3. Current callback false, throw, async or invalid result rejects binding/preparation; revocation at invocation and between read/write denies before dispatch. Revocation after confirmed write retains CONFIRMED.
4. Two potential writes fail compile and forged-artifact preparation. Zero narrowed ceiling blocks a reached write; false guard dispatches nothing. Native key identity, protected actor/event source and original event remain intact.
5. Duplicate-confirmed native receipt, definite rejection, transient NONE, timeout/UNKNOWN, malformed outer envelope, malformed value after CONFIRMED and response/deadline winner retain existing semantics and consumer evidence.
6. Exact byte/model/consumer/compiler/environment mismatch and missing dependencies fail before dispatch; scopes sharing pure caches cannot share invokers or qualification decisions.

Native persistence/race qualification is separately required by the host handoff. Passing these deterministic vectors alone does not qualify SQLite or any deployment.
