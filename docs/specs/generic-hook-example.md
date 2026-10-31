# TaskCompleted: ownership from authoring to invocation

Status: **implemented `/2` example with local Java/Node and packaged-host verification**. The exact model/source, six program variants and 28 shared scenarios live in [generic-hook conformance](../../conformance/generic-hooks/README.md). This follows the [generic-hook contract](generic-hook-capability-contract.md); effects remain simulation-only. The `/1` fixture retains its older input/grant contract for compatibility.

## Host publishes the hook and catalog

For this example the host defines TaskCompleted input as `{taskId}` and emits it after native commit. This is a host choice, not a restriction that all hosts expose only taskId. Other legitimate payload fields remain host-defined regardless of what a customer program uses. The customer's eligible project and fixed title do not belong in this hook schema.

This complete DCM has no named hook grants or advanced filters. The catalog defines operations independently of this program; approving native adapters and current access remains the host's responsibility. The sample uses simulation-only effect admission.

The input projection and `FollowupResult` output below are this example host's choices, not mandatory Keir payload/result types. Every extension targeting this hook must honor that host-defined result contract.

```json
{
  "format":"keir-dcm/effect-2",
  "domain":{"id":"sample.tasks","version":"2"},
  "types":{
    "TaskId":{"kind":"nominal","base":"String"},
    "ProjectId":{"kind":"nominal","base":"String"},
    "UserId":{"kind":"nominal","base":"String"},
    "EventId":{"kind":"nominal","base":"String"},
    "TaskStatus":{"kind":"enum","values":["OPEN","COMPLETED"]},
    "FollowupStatus":{"kind":"enum","values":["CREATED","NO_OP"]},
    "TaskCompletedInput":{"kind":"record","fields":{"taskId":"TaskId"}},
    "FollowupResult":{"kind":"record","fields":{"status":"FollowupStatus"}},
    "Task":{"kind":"record","fields":{"id":"TaskId","projectId":"ProjectId","status":"TaskStatus"}},
    "CreatedTask":{"kind":"record","fields":{"id":"TaskId"}}
  },
  "capabilities":{
    "tasks.get":{"version":"1","parameters":{"taskId":{"type":"TaskId","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},"result":"Task","effect":"READ","errors":["TASK_NOT_FOUND","ACCESS_DENIED"]},
    "tasks.create":{"version":"1","parameters":{"projectId":{"type":"ProjectId","binding":"agent"},"title":{"type":"String","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},"result":"CreatedTask","effect":"WRITE","errors":["CREATE_REJECTED","ACCESS_DENIED"]}
  },
  "entrypoints":{
    "TaskCompleted":{"input":"TaskCompletedInput","output":"FollowupResult","trigger":"EVENT","placement":"AFTER_COMMIT","consumer":{"id":"sample.followup.report","version":"1","kind":"EVENT_REPORT"},"event":{"idType":"EventId","sourceType":"TaskId"}}
  },
  "hostKey":{"source":"requestKey","type":"sample.RequestUuid/1"},
  "assurance":"SIMULATION_ONLY"
}
```

Effect ceiling omission means one under this format; it never permits a second potential WRITE. Catalog declarations and simulation cannot certify real native behavior.

`tasks.create` is ordinary task creation. The extension's condition, selected project and title make the new task a follow-up in this scenario. There is no special follow-up service operation, implicit parent link or `sourceTaskId` creation argument. If the host exposes a generic relationship field, the program must supply it explicitly. Trigger provenance remains in protected event context and does not become a customer-controlled task field.

## Customer authors the program

The customer chooses project `project1` and title `Follow up` inside this source. The ProjectId annotation is supported under explicit `/2` compilation; `/1` retains its earlier literal restrictions. The fingerprint below is the canonical digest of the DCM above, not an authentication credential.

```yaml
language: keir/v0
domain: {id: sample.tasks, version: '2', fingerprint: 'sha256:68b72e4da41aa006a6a8182b5b8a6a4363a6734688da3c1e0c3807c0aff4ac04'}
entrypoint: TaskCompleted
name: follow-up-in-selected-project
steps:
  - id: eligibleProject
    let: {literal: 'project1', type: ProjectId}
  - id: source
    call: tasks.get
    args:
      taskId: {cel: input.taskId}
  - id: eligible
    let: {cel: 'source.status == "COMPLETED" && source.projectId == eligibleProject'}
  - id: created
    call: tasks.create
    when: eligible
    args:
      projectId: {cel: eligibleProject}
      title: {literal: 'Follow up'}
return:
  record:
    status: {cel: 'eligible ? "CREATED" : "NO_OP"'}
```

There is no read of the guarded `created` result. The ordinary return is reached only after successful calls; rejection or uncertainty cannot be relabeled CREATED. Four steps and two potential calls fit existing aggregate ceilings. The input task and protected event source must refer to the same native task under the host contract; the read adapter must return that requested task or reject. Project equality does not replace authorization.

## Compile, approve and prepare

| Boundary | Concrete input/output |
| --- | --- |
| Compile input | The complete source above, separately trusted DCM bytes above, and explicit `Environment.effect2()` or CLI `--profile=keir-effect/2`. No settings/configuration argument. |
| Compiler checks | Strict shape, pins, nominal constant/dataflow, protected parameter exclusion, catalog/effect/policy constraints, bounded CEL and aggregate limits. No artifact on failure. |
| Compiler output | One canonical `keir-artifact/effect-2` artifact with profile `keir-effect/2`, source identity, exact `modelRef`, checked expressions, four lowered steps and return. The [reference envelope](host-model-binding.md#exact-reference-artifact) omits embedded DCM/descriptors; descriptors derive from the bound model. `eligibleProject` lowers to `{literal:"project1",type:"ProjectId"}`; title remains `{literal:"Follow up",type:"String"}`. See the exact [generated artifact](../../conformance/generic-hooks/effect/artifact.json). |
| Host selection | Review intended behavior, then retain the exact produced artifact digest independently with expected DCM digest, entrypoint TaskCompleted, consumer `sample.followup.report` version 1 and compiler/environment pins. A self-reported digest in customer data cannot authorize activation. |
| Binding | Call `bindModel(dcmBytes, approval, invokers)` once and check its diagnostic before using `boundModel`. Approval selects host scope, exact modelRef, profile/environment, `mode:simulation` and native keyBinding. Register both catalog capabilities, not just whichever a particular extension calls. |
| Preparation | Call `prepareProgram(artifactBytes, boundModel, admission)` and check its diagnostic before using `prepared`. External pins and simulation mode must match; no invoker map or keyBinding override. Omitted policy, effect ceiling and limits inherit safe defaults. These host-owned records are never customer authority. |

Both adapter registrations use `{version:"1",mode:"simulation",keyType:"sample.RequestUuid/1",invoke:<host function>}`. Preparation validates metadata and the artifact without calling adapters or authorization. Both loaders reconstruct and validate the typed literal environment instead of trusting forged annotations. The [host sketches](host-model-binding.md#equivalent-host-sketches) show the Java/Node wrappers; complete runnable integrations are linked there.

The exact modelRef is `{domainId:"sample.tasks",domainVersion:"2",format:"keir-dcm/effect-2",digest:"sha256:68b72e4da41aa006a6a8182b5b8a6a4363a6734688da3c1e0c3807c0aff4ac04",descriptorRecipe:"keir-record-descriptors/1"}`. See [equivalent Java/Node host sketches](host-model-binding.md#equivalent-host-sketches). The host-held scope and invokers are not serialized into this reference.

## Host invokes with unchanged facts and protected context

Business input is exactly `{"taskId":"task1"}`. Trusted invocation supplies:

- `scope` matching the bound model's host approval namespace, selected from authenticated host context. The current authorizer checks that binding remains permitted.

- `bindings: {actorId:"user1"}` from authenticated host scope, not source.
- `deadline: {clock:<host monotonic function>, expiresAtMs:<same-domain expiry>}` and `authorize:<current invocation/per-call function>`.
- Original immutable `requestKey` of the registered native key type, and `event: {id:"event1",source:"task1"}` from the host event. No key conversion or source access.
- No `grants`, configuration map, customer-selected project or title. Optional policy/effect ceilings are omitted; this does not omit current authorization.

The host selects the approved artifact for this invocation. The runtime executes it against those facts/context; it does not re-author source or fetch implicit settings. Entry authorization precedes execution. Each reached call receives separate agent/protected arguments, remaining deadline and protected key/event context. The host authorizer and native adapter remain responsible for current resource access and invariants; a typed constant can still refer to a nonexistent, wrong-scope or revoked project.

With read result `{id:"task1",projectId:"project1",status:"COMPLETED"}`, the generic create receives agent `{projectId:"project1",title:"Follow up"}` and protected `{actorId:"user1"}`; original key/event information remains in the separate native context. A valid SUCCESS/CONFIRMED response with `{id:"task2"}` permits COMPLETED/CREATED with a CONFIRMED receipt. A different project or OPEN status skips the write and returns COMPLETED/NO_OP with no effect. An inaccessible resource rejects; a dispatched write with unknown commit terminates UNCERTAIN, no normal result. EVENT_REPORT preserves these distinctions and cannot undo the native event.

## Run the example

After the [build/package setup](../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts), run either host from the repository root:

```sh
node examples/node-host/generic.mjs conformance/generic-hooks/effect
java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.GenericHost conformance/generic-hooks/effect
```

Each emits the same nineteen verified rows, including creation, no-op conditions, changed project/title constants, denial, rejection and uncertain write responses. Use the `read` directory for the nine read scenarios, including repeated and alternate reads. The [combined verifier](../../conformance/generic-hooks/README.md) checks complete outcomes, ordered calls, traces, effects and consumption.

## Acceptance obligations

Change only the source literal from `project1` to `project2`: DCM bytes/digest, hook schema, host code, input and protected context remain unchanged. Canonical source and artifact digests change; normal review and selection of the new artifact are required. With the original project1 task, the new program skips the write. Changing only the title similarly changes source/artifact, not the hook. Compare Java and Node executing each exact compiled artifact, not independently regenerated variants.

Reject wrong nominal types, unknown/non-nominal annotations, dynamic casts, bounds overflow, forged literal metadata, protected argument overrides, unauthorized capabilities and a second potential WRITE. Keep `/1` fixture bytes and literal rejection behavior unchanged. Regression covers these obligations and current authorization after source review; see [measured evidence](../testing.md). Native authorization, idempotency and production readiness still require separate host proof.
