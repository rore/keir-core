# How Keir works: a guided tour

Keir lets customers compose a host application's approved capabilities into new behavior. The host defines where extensions run and what they can access. Keir validates and executes the resulting programs. A host-integrated AI agent helps a customer describe, test and review a customization without writing code.

This tour explains the intended model in two parts: [design time](#design-time), when the host integration and customer program are defined, and [runtime](#runtime), when an event executes the selected program. For exact schemas and language rules, use the [specifications](specs/README.md) and [complete worked example](specs/generic-hook-example.md).

## The scenario

A task-management application wants customers to customize what happens after a task completes. One customer asks:

> When a task completes in project `project1`, create a follow-up task titled "Follow up". For other projects, do nothing.

The host exposes a generic `TaskCompleted` event hook. The extension reads the task, checks its status and project, and conditionally requests a follow-up. It runs after the original completion commits, so it cannot undo that completion.

| Role | Owns |
| --- | --- |
| Host application | Hook payload and result contract, approved capability catalog, service integrations, permissions, activation and result consumption |
| Customer | The customization's business logic, selected project and follow-up title |
| Keir | Source validation, compilation, simulation execution, runtime checks and execution outcomes |
| Host-integrated agents | Help the host developer define the integration, or help the customer author and test a program; neither grants itself authority |

## Design time

### 1. The host defines the hook and domain model

The host describes its domain in a **domain capability model (DCM)**: types, capabilities, hooks and their contracts.

For this example, the host defines:

| Hook property | Host choice |
| --- | --- |
| Name | `TaskCompleted` |
| Timing | Event, after the task completion commits |
| Business input | A record containing `taskId` |
| Program output | A record containing `status`, either `CREATED` or `NO_OP` |
| Result consumer | An event report; it cannot veto the completed operation |

These payload and result types are this application's choices. Another host can define different inputs and outputs.

The customer-selected project is not part of the event payload. The event identifies what happened; the customer's program defines which project interests them.

### 2. The host publishes capabilities and binds their invokers

The host publishes approved operations independently of any particular extension:

| Capability | Effect | Business arguments | Successful result |
| --- | --- | --- | --- |
| `tasks.get` | READ | `taskId` | Task ID, project ID and status |
| `tasks.createFollowUp` | WRITE | Source task ID, project ID and title | Created task ID |

The host also declares errors and supplies protected context such as authenticated actor identity. A program cannot choose another actor or supply credentials.

The DCM describes logical contracts. The host binds an exact approved DCM snapshot together with compatible capability invokers into a reusable bound host model. See the [binding interface](specs/host-model-binding.md) for the technical contract. An invoker bridges the runner to an internal service, usually through a host-owned HTTP/RPC client:

```text
tasks.get instruction -> registered invoker -> internal task service
```

Routing, credentials, trusted identity propagation and response mapping belong to the host integration. A capability's business implementation need not run in the runner's process. The host can reuse the bound model and service clients across many extensions without retaining a customer's identity in shared program state.

The hook does not need a list of the calls this particular program will make. Extensions compose the published catalog, subject to current authorization and resource bounds. Explicit capability filters can narrow access. Reads can be repeated within aggregate budgets; the default write policy permits at most one mutation.

A **host integration assistant** can help the developer define these contracts and bindings. The host owner approves publication and remains responsible for the services' actual guarantees.

### 3. The customer authors the extension

A separate **customer extension assistant** clarifies the request, discovers the approved contract and generates a constrained Keir program. It can help resolve a project name to an ID, but the customer's chosen ID becomes a program constant.

The program body looks like this:

```yaml
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
    call: tasks.createFollowUp
    when: eligible
    args:
      sourceTaskId: {cel: input.taskId}
      projectId: {cel: eligibleProject}
      title: {literal: 'Follow up'}
return:
  record:
    status: {cel: 'eligible ? "CREATED" : "NO_OP"'}
```

The [complete source](specs/generic-hook-example.md#customer-authors-the-program) also identifies its language, domain model and hook. The `ProjectId` annotation makes the constant a business identifier of the declared type; it does not establish that the project exists or that the caller may access it.

Changing `project1` to another project changes the customer's source. It does not change the host's hook payload, service code or catalog.

### 4. The compiler validates the source and emits an artifact

The compiler receives the source, a separately trusted DCM and the selected compiler profile/environment. There is no actual task event at compile time. `input.taskId` means a typed value that a future invocation will supply.

The compiler checks source structure, types, capability calls, protected argument boundaries, expressions and resource/effect constraints. Invalid source produces diagnostics rather than an executable artifact.

Successful compilation produces a portable artifact containing the program's instructions, constants, checked expressions and exact model/environment references. Java and Node execute the same artifact. The DCM is shared host-supplied model data rather than copied into each program; each program references its exact snapshot, not whichever version is newest.

CEL expressions are parsed and type-checked into a structured expression representation, not arbitrary generated Java or JavaScript. The artifact does not contain service implementations, endpoints or credentials.

The host prepares the artifact against its bound model, checking the artifact's identity, compatible environment and capability bindings. Preparation binds an executable plan; it performs no business service calls and does not cache current user authorization.

### 5. Simulation checks intended behavior

The authoring flow runs the same compiled program through the runner with simulation invokers. These return controlled service responses and record requested writes without performing real mutations.

| Scenario | Expected behavior |
| --- | --- |
| Completed task in `project1` | Request a follow-up titled `Follow up`; return `CREATED` |
| Completed task in another project | Skip the write; return `NO_OP` |
| Task with status OPEN | Skip the write; return `NO_OP` |
| Read service rejects with declared `ACCESS_DENIED` | Stop with a business rejection |
| Write dispatched but its commit outcome is unknown | Report uncertainty, not success or proof of no effect |

The customer and agent inspect outcomes, call arguments and traces. Simulation checks program behavior under supplied scenarios. It does not certify real service authorization, idempotency or commit behavior; those need host integration tests.

### 6. The host activates the reviewed revision

After the required review, the host records the exact artifact revision, hook and customer scope to activate, together with its approved bound model. The agent can request activation through host tools; approval and activation follow host policy.

The host persists and routes this selection. Keir does not become an activation database. Changing source requires compiling and reviewing the new revision. Publishing a new DCM does not silently relink an existing program; its exact approved dependency must remain available or preparation fails closed.

**Checkpoint:** before any live event, we have a host contract, service bindings, a compiled customer program and a reviewed activation selection. No actual event data has been needed to compile it.

## Runtime

### 1. A real event supplies facts and fresh authority

Task `task1` completes and commits. The host reaches `TaskCompleted`, selects the customer's active extension and invokes its prepared program with business input:

```json
{"taskId":"task1"}
```

Separately, the host supplies fresh trusted context: authenticated actor/scope, current authorization, deadline, event identity/provenance and the original native idempotency key required by the write contract. These values are not customer-authored source or CEL variables.

The selected project and title are already inside the program. The host does not infer them from the event or inject them as customer-specific payload fields. Each invocation has fresh execution state; prepared plans and compatible model data can be reused.

### 2. The runner binds the constant and reads the task

The runner validates the input/context and checks invocation authorization. It then binds `eligibleProject` to the typed constant `project1`. This local assignment makes no service call.

For `tasks.get`, it evaluates `input.taskId`, validates the arguments, checks policy/budget/deadline and current capability authorization, then dispatches through the registered invoker. The host service enforces actual task access and returns the requested task or a declared rejection.

Suppose the adapter reports success with:

```json
{"id":"task1","projectId":"project1","status":"COMPLETED"}
```

The runner validates the record against the DCM and stores it in the variable `source`. The adapter must ensure it represents the requested task; type checking alone cannot prove that relationship.

### 3. The program evaluates its condition

The checked expression compares the returned status with `COMPLETED` and the returned project with the authored constant. Both match, so `eligible` becomes true. This is local computation.

For another project, or an OPEN task, `eligible` is false and the guarded write is skipped. This condition is business logic, not an authorization decision.

### 4. The runner requests the follow-up

With `eligible` true, the write's evaluated business arguments are:

```json
{"sourceTaskId":"task1","projectId":"project1","title":"Follow up"}
```

The runner validates these arguments, checks current authority, the remaining deadline and the single-write allowance, then dispatches. Protected identity and original native key/event context travel through the host-owned channel rather than customer arguments. The service remains responsible for resource access, business invariants and its declared native guarantees.

Suppose the adapter returns:

```json
{"kind":"SUCCESS","value":{"id":"task2"},"effect":"CONFIRMED"}
```

The runner validates the response and retains the confirmed effect evidence. A timeout or lost response after a possible commit means UNKNOWN/UNCERTAIN unless the native contract establishes a definite outcome. Stopping the wait does not undo a remote write; Keir does not automatically retry it.

### 5. The runner returns an outcome; the host consumes it

After a successful call, the return expression produces `{"status":"CREATED"}`. The runner validates that output and returns it with execution classification, effect evidence and trace:

| Part | Successful path | Write skipped |
| --- | --- | --- |
| Classification | `COMPLETED` | `COMPLETED` |
| Program result | `{"status":"CREATED"}` | `{"status":"NO_OP"}` |
| Effect evidence | Confirmed write | No write dispatched |
| Trace | Reached steps and call outcomes | Reached steps without write dispatch |

The application result and execution evidence are separate. Returning `CREATED` cannot establish a commit by itself. Rejection or uncertainty stops this ordinary success path; it cannot be relabeled by the return expression.

The trace records execution events without exposing credentials or protected argument values. The host consumes the event report, for example by recording it in an extension activity log. It decides presentation and follow-up handling. Failure does not roll back the original task completion.

**Checkpoint:** the host supplied facts and authority, the customer program chose the behavior, internal services performed the operations, and Keir enforced the execution boundary and reported what is known about the result.

## Continue reading

- [Complete example with DCM and source](specs/generic-hook-example.md)
- [Hook, catalog and ownership contracts](specs/generic-hook-capability-contract.md)
- [Guidance for the two host-integrated agent personas](agents/README.md)
- [Runnable embedding examples](../examples/README.md)
- [Specification and implementation status](specs/README.md)
