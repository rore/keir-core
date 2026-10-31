# Read-and-decide DCM contract

This page preserves the implemented `/1` schema, including required grants. For generic hook defaults without named grants, see the [proposed `/2` correction](generic-hook-capability-contract.md). Do not remove fields from an existing `/1` DCM or treat its sample grant map as the product's default hook design.

Status: implemented experimental checkpoint. This is the explicit small model format `keir-dcm/read-1`, not compatibility with every field of baseline `modelVersion: 0.1.0`. A host selects the authoritative model; source may reference it but never supply replacement authority. See [language](language.md).

## Closed schema

This schema remains `keir-dcm/read-1`. The implemented [effect DCM](single-effect.md#closed-dcm-additions) has a separate format, explicit lifecycle/effect authority, one protected native-key contract and simulation-only assurance. Those fields must not be silently admitted into the read format.

All fields below are required; unknown fields fail. Maps have unique keys. Types and identifiers obey language identifier rules. Root: `{format,domain,types,capabilities,entrypoints}`. `format` is `keir-dcm/read-1`; `domain` is `{id,version}` with nonempty bounded strings. Types are named definitions of exactly one shape:

- `{kind:"nominal",base:"String"}`.
- `{kind:"enum",values:[nonempty unique bounded strings]}`.
- `{kind:"record",fields:{fieldName:typeName,...}}` with at least one field, all required scalar types.

`Boolean` and `String` are built-ins and cannot be redefined. Type names must resolve. No cycles, nested records, reserved-name collisions or unsupported kinds. The format's administrative arrays/maps do not admit collections into CEL.

A capability is `{version,parameters,result,effect,errors}`. `parameters` maps names to `{type,binding}` where binding is `agent` or `host`; all are required supported scalar types. `result` names a record. `effect` must be `READ`; writes and unknown effect classes fail. `errors` is a unique list of allowed business error codes matching `[A-Z][A-Z0-9_]{0,63}`. Transient failure is a generic adapter outcome, not a user-handleable error. A host-bound parameter is resolved by the same name from a separate trusted binding map, not a CEL expression or source-selected path.

An entrypoint is `{input,output,trigger,consumer,grants}`. Input/output name records. `trigger` is `REQUEST`. `consumer` is `{id,version,kind}` with kind `DISPLAY`; its complete value is compatibility-sensitive. Consumer `id` uses the capability/entrypoint identifier rule `[A-Za-z][A-Za-z0-9.]{0,99}` and excludes CEL reserved identifiers; `version` is a nonempty bounded string. `grants` maps allowed capability IDs to positive integer maximum call counts, bounded by the profile. No implicit grant. Source call counts are checked conservatively; runtime enforces them again. Profile limits are defined centrally, not overridable in source/DCM.

## Sample model

This is a simulated sample contract, not a certification of real task access. The host's adapter registration must separately declare whether it is fake or real and meet the current authorization contract.

```json
{
  "format":"keir-dcm/read-1",
  "domain":{"id":"sample.tasks","version":"1"},
  "types":{
    "TaskId":{"kind":"nominal","base":"String"},
    "UserId":{"kind":"nominal","base":"String"},
    "TaskStatus":{"kind":"enum","values":["OPEN","COMPLETED"]},
    "Classification":{"kind":"enum","values":["DONE","PENDING"]},
    "ClassifyInput":{"kind":"record","fields":{"taskId":"TaskId"}},
    "Task":{"kind":"record","fields":{"id":"TaskId","status":"TaskStatus"}},
    "ClassifyResult":{"kind":"record","fields":{"classification":"Classification"}}
  },
  "capabilities":{
    "tasks.get":{
      "version":"1",
      "parameters":{"taskId":{"type":"TaskId","binding":"agent"},"actorId":{"type":"UserId","binding":"host"}},
      "result":"Task","effect":"READ","errors":["TASK_NOT_FOUND","ACCESS_DENIED"]
    }
  },
  "entrypoints":{
    "ClassifyTask":{
      "input":"ClassifyInput","output":"ClassifyResult","trigger":"REQUEST",
      "consumer":{"id":"sample.classification","version":"1","kind":"DISPLAY"},
      "grants":{"tasks.get":1}
    }
  }
}
```

Compile context selects the trusted model and compiler/profile/environment pins. Source selects one entrypoint declared in that model and cannot widen its grants. The host independently pins the intended entrypoint and consumer at runtime admission and may narrow grants at admission/invocation. No tenant overlays, endpoint registration, certifications registry, activation routing or source imports are implemented by this format. A future addition needs an explicit format/profile compatibility decision, not ignored fields.

The host validates current actor/resource access on every adapter call. An artifact grant is a ceiling, not authentication or ownership evidence. `tasks.get` must return the requested task or reject; this relational obligation is host-specific and does not become a task-specific branch in the generic runtime. Host bindings are supplied separately from validated agent arguments; collisions are rejected, never resolved by merge precedence. Fake adapters may model outcomes but cannot establish real authorization.
