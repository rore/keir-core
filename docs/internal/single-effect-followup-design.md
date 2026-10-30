# One guarded follow-up: design and implementation handoff

Status: **implemented experimental checkpoint, locally verified on 2026-10-30**; effect-host CI acceptance pending. Owning feature: [single-effect-followup](../../roadmap/features/single-effect-followup.md). Normative delta: [single-effect profile](../specs/single-effect.md). See [testing evidence](../testing.md) and [25 shared scenarios](../../conformance/single-effect/README.md). This record covers one post-commit effect and deterministic simulation, preserving the read profile; it certifies no real adapter.

## Decisions and evidence

Use source `keir/v0` with separately selected `keir-effect/1`, `keir-dcm/effect-1` and `keir-artifact/effect-1`. Keep scalar CEL and existing package/byte limits. A second guarded write is rejected even if guards appear mutually exclusive. No list/search/date work or customer-specific eligibility adapter is necessary: task status, nominal ProjectId equality and a bounded title suffice.

- [Language baseline, call/handler and availability rules](design-baseline/public/keir-language-v0.md#3-exact-step-grammar-and-execution-semantics) supports prior Boolean `when` and terminal typed business rejection. We choose the smaller admissible subset: **no guarded result references**, including same-guard references. [Java CheckedCel](../../runtime-java/src/main/java/dev/keir/runtime/CheckedCel.java) validates every declared context variable; [Node CEL](../../runtime-node/src/cel.js) constructs each declared record variable. Excluding unavailable results from the entire environment avoids dummy records and optional-value semantics, not just unsafe AST references.
- [Current compiler](../../compiler-java/src/main/java/dev/keir/compiler/KeirCompiler.java) emits ordered CALL/BIND and all previous bindings. [Node validation](../../runtime-node/src/validation.js) and [Java Artifact](../../runtime-java/src/main/java/dev/keir/runtime/Artifact.java) independently validate plans. Extend those checks for guards/handlers and definite environments; no new expression evaluator is needed.
- [Node runtime](../../runtime-node/src/index.js) and [Java runtime](../../runtime-java/src/main/java/dev/keir/runtime/KeirRuntime.java) currently terminate with result **or** error and call three-argument adapters. Keep that read API; the effect profile needs a protected fourth context and a handler result alongside rejection provenance. Its per-invocation effect ledger must be included on every exit, not only successful return.
- [Host v0.5](design-baseline/public/host-integration-and-hook-contracts-v0.5.md) separates post-commit reaction from native operation veto. [Host-owned keys v0.7 sections 1, 3, 6 and 7](design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md) supersede derived per-step keys: preserve native logical value/type, no hashing/stringification, honest unknown effects. This increment takes one key and one possible write; the wider multi-effect framework is deferred.
- [DCM baseline](design-baseline/public/domain-model-v0.md), [compiler baseline](design-baseline/public/compiler-artifact-and-runners-v0.md) and [simulation baseline](design-baseline/public/simulation-and-authoring-v0.md) require conservative effect admission, the same artifact path and ordered argument/effect assertions. Fakes cannot certify a real adapter. Restrict this checkpoint to simulation mode rather than invent a certification service or accept an unverified Boolean claim.

## Complete small sample model

New sample domain identity; the baseline fixture and read sample remain untouched. This JSON is the proposed complete `sample.followup` model, not a production catalog. The existing canonical serializer produces 1,721 bytes and fingerprint `sha256:d97c00f679ed0441817a610a71faeda949069c34fcb2a8d89774838d87a71e77`. Recompute the fingerprint if the model changes.

```json
{
  "format":"keir-dcm/effect-1",
  "domain":{"id":"sample.followup","version":"1"},
  "assurance":"SIMULATION_ONLY",
  "hostKey":{"source":"requestKey","type":"sample.RequestUuid/1"},
  "types":{
    "TaskId":{"kind":"nominal","base":"String"},
    "UserId":{"kind":"nominal","base":"String"},
    "ProjectId":{"kind":"nominal","base":"String"},
    "EventId":{"kind":"nominal","base":"String"},
    "TaskStatus":{"kind":"enum","values":["OPEN","COMPLETED"]},
    "FollowupStatus":{"kind":"enum","values":["CREATED","NO_OP","SOURCE_MISSING","REJECTED"]},
    "FollowupInput":{"kind":"record","fields":{"taskId":"TaskId","projectId":"ProjectId","title":"String"}},
    "Task":{"kind":"record","fields":{"id":"TaskId","projectId":"ProjectId","status":"TaskStatus"}},
    "CreatedTask":{"kind":"record","fields":{"id":"TaskId"}},
    "FollowupResult":{"kind":"record","fields":{"status":"FollowupStatus"}}
  },
  "capabilities":{
    "tasks.get":{"version":"1","parameters":{"taskId":{"type":"TaskId","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},"result":"Task","effect":"READ","errors":["TASK_NOT_FOUND","ACCESS_DENIED"]},
    "tasks.createFollowUp":{"version":"1","parameters":{"sourceTaskId":{"type":"TaskId","binding":"agent"},"projectId":{"type":"ProjectId","binding":"agent"},"title":{"type":"String","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},"result":"CreatedTask","effect":"WRITE","errors":["CREATE_REJECTED","ACCESS_DENIED"]}
  },
  "entrypoints":{
    "TaskCompleted":{"input":"FollowupInput","output":"FollowupResult","trigger":"EVENT","placement":"AFTER_COMMIT","consumer":{"id":"sample.followup.report","version":"1","kind":"EVENT_REPORT"},"grants":{"tasks.get":1,"tasks.createFollowUp":1},"effectGrant":1,"event":{"idType":"EventId","sourceType":"TaskId"}}
  }
}
```

## Source and observable example

The source pins the complete canonical model above. No due date or tags are required by this separately versioned signature. Its new profile is not yet implemented, so this is an implementation fixture candidate rather than a claimed successful compilation.

```yaml
language: keir/v0
domain: {id: sample.followup, version: '1', fingerprint: 'sha256:d97c00f679ed0441817a610a71faeda949069c34fcb2a8d89774838d87a71e77'}
entrypoint: TaskCompleted
name: create-project-followup
steps:
  - id: source
    call: tasks.get
    args:
      taskId: {cel: input.taskId}
    onError:
      TASK_NOT_FOUND:
        record:
          status: {literal: SOURCE_MISSING}
  - id: eligible
    let: {cel: 'source.status == "COMPLETED" && source.projectId == input.projectId'}
  - id: created
    when: eligible
    call: tasks.createFollowUp
    args:
      sourceTaskId: {cel: source.id}
      projectId: {cel: input.projectId}
      title: {cel: input.title}
    onError:
      CREATE_REJECTED:
        record:
          status: {literal: REJECTED}
return:
  record:
    status: {cel: 'eligible ? "CREATED" : "NO_OP"'}
```

The guarded `created` result is deliberately not referenced. CREATED is reached only after a validated SUCCESS/CONFIRMED response; the consumer still uses the receipt, not the label, as effect evidence. With input `{taskId:"task1",projectId:"project1",title:"Follow up"}`, a trusted TaskCompleted event whose source is task1, and read result `{id:"task1",projectId:"project1",status:"COMPLETED"}`, expect one read then one write. Both receive the original native request key and the same protected event context. Write success `{kind:"SUCCESS",value:{id:"task2"},effect:"CONFIRMED"}` yields classification COMPLETED, result `{status:"CREATED"}`, receipt `[{step:"created",capability:"tasks.createFollowUp",state:"CONFIRMED"}]` and REPORTED consumption. Exact traces are defined in the normative profile; the example abbreviates only the presentation.

Task status OPEN or a different nominal ProjectId yields COMPLETED/NO_OP with empty effects and no write stub consumed. CREATE_REJECTED/NONE yields BUSINESS_REJECTION with result REJECTED, original error and NONE receipt. Lost write response yields UNCERTAIN, no result and UNKNOWN receipt, even on deadline expiry. A later return failure retains CONFIRMED and does not retroactively undo task2.

The sample adapter must verify the requested source matches protected event.source, the read returns that source, and current actor permission/native invariants permit creation in the target project. The host fixture supplies original immutable UUID identity and protected event values; customer source cannot choose them. Matching a project is not an access-control decision. Simulation may model duplicate prevention by `(source, followup kind)` and original-key payload matching, but real atomic uniqueness, key retention and collision safety across bindings/native callers remain host evidence, outside this checkpoint.

## Acceptance vectors

These are implementation obligations, not passing test claims. Compile once and run identical artifact bytes on Java and Node; compare typed results, classifications, error/cause paths, ordered calls, branch trace, receipts and consumption. Preserve the entire old read corpus/golden/packaged-host behavior.

| Vector | Required result/evidence |
| --- | --- |
| Completed task and matching project | One read + one write; original native key unchanged at both adapters; COMPLETED/CREATED and CONFIRMED. |
| OPEN status or different ProjectId | Guard false; no write, argument evaluation or write authorization; COMPLETED/NO_OP, effects empty. |
| Change source condition | New artifact changes eligibility without adapter policy edits. |
| Missing source, declared handled code | BUSINESS_REJECTION with SOURCE_MISSING and original error; no later step/write. |
| Unhandled read rejection / transient / malformed read | No write or normal result; exact rejection/failure class. |
| Write definite business rejection | NONE receipt; terminal handled REJECTED or unhandled rejection; no root return. |
| Write TRANSIENT_FAILURE/NONE | FAILED/ADAPTER_TRANSIENT, no handler, receipt NONE. |
| Write UNKNOWN_COMMIT, throw, rejected future, timeout | UNCERTAIN with cause code; UNKNOWN receipt; no retry/result/later step; late success ignored. |
| Confirmed response with invalid/oversized result | FAILED/ADAPTER_INVALID or LIMIT_EXCEEDED/BUDGET_EXCEEDED with CONFIRMED retained. |
| Response wins, valid CONFIRMED or NONE envelope, completion clock equals expiry | In both runtimes, settle evidence before checking the clock; LIMIT_EXCEEDED/BUDGET_EXCEEDED retains CONFIRMED or NONE, no result/handler/later step. Exercise NONE for both business rejection and transient failure. |
| Response wins, valid CONFIRMED or NONE envelope, completion clock invalid or regressing | In both runtimes, FAILED/HOST_CONTEXT_INVALID retains settled evidence; no result/handler/later step. Exercise both NONE outcome kinds. |
| Timeout wins before later valid CONFIRMED or NONE response | In both runtimes, UNCERTAIN/BUDGET_EXCEEDED with UNKNOWN receipt remains unchanged; late response adds no result, handler, trace event or receipt update. |
| Return/trace/deadline failure after confirmation | Failure class and no result, CONFIRMED retained and reported; never rollback. |
| Handler expression/size failure | Failure with original business code/path in error.cause; retained effect evidence. |
| Guard references later/non-Boolean/call binding | No artifact; deterministic reference/type diagnostic. |
| Any guarded-result reference, even same-guard or unused declaration | Compile/prepare rejection; skipped record never reaches protobuf conversion. |
| Handler references failing/later result or undeclared error | Compile/prepare rejection; no artifact/dispatch. |
| Two writes with constant/mutually exclusive guards | Conservative compile and forged-plan prepare rejection. |
| Pre-commit placement, zero static effect grant, forged READ effect | Compile/prepare rejection before callbacks/dispatch. |
| Missing/wrong native key, validator false/throw, wrong event type | HOST_CONTEXT_INVALID before the first read; no conversion or key leak. |
| Adapter keyType or host keyBinding differs from DCM | ADMISSION_INVALID during prepare, before validator/authorization/adapter callbacks. |
| Key contract label equals a built-in or declared business type name | The separate metadata namespace permits equality. Exact keyBinding/adapter label matching and unchanged opaque-key identity still apply; source gains no key access. |
| Key/event/source overrides in source | Compile rejection; protected origin mismatch rejected by adapter before mutation. |
| Current call/effect grant zero or invalid/widened | Reached call denied or invocation invalid before dispatch; skipped call does not consume dynamic grant. |
| Real mode with fake assurance/registrations | Prepare rejection, no callbacks; no certification by scenario success. |
| Reuse Prepared for another invocation | Different key/event/grants stay isolated; adapters see each original key only. |
| Trace reserve at five available WRITE slots / one fewer | Dispatch only when intent, dispatch, settlement, success and terminal fit; no unrecorded effect. |
| Exact raw/canonical DCM, string/record/AST limits and +1 | Existing inclusive caps and diagnostic taxonomy; handler AST included. |
| Missing/extra/wrong-order/unconsumed scenario stub | Simulation fails independently of runtime classification; no rewritten expectation. |
| Different steps with identical capability/version/arguments | Reconcile actual captures with dispatch trace by ordinal; a wrong stub step fails simulation. Report the trace's actual step, never copy the expected label. |
| Simulated duplicate event and payload mismatch | Fixture-labelled duplicate confirmation or definite rejection; unchanged key; no claim of real duplicate safety. |
| Deterministic compile and old artifact compatibility | Same input/pins -> byte-identical artifact; old read format remains closed and unchanged. |

## Implementation boundary and remaining host decisions

Compiler work reaches source/DCM validation, definite environments, handler expression ordering, effect preflight and new-format lowering. Both runtime work reaches independent plan/CEL gates, a profile-specific host API, guard/handler evaluation, dispatch reservation, immutable invocation ledger and consumer fixtures. Sample/simulator work reaches trusted key/event fixture construction, ordered adapter assertions and human-readable rejection/uncertainty reporting. No environment, CI, roadmap or baseline edits are required by this design.

No further human product choice blocks the simulation proposal. Real admission remains deliberately closed: a host owner must later choose and prove actual key representation/source, deduplication scope/retention/payload rules, event admission/routing, cross-binding collisions, native authorization and source relationship. We do not invent those guarantees or a production certification registry. Stop after this design for manager acceptance; implementation must resolve any contrary measured seam before widening the contract.
