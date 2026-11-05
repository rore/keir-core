# How Keir works: a guided tour

Keir lets customers turn a host application's approved operations into custom behavior. The host defines where extensions run and what they can do. Customers choose the business logic, and Keir checks and runs it.

> When a task completes in project `project1`, create a follow-up titled “Follow up.” For other projects, do nothing.

This is the same `keir-effect/4` example used in the [Node and Java quickstart](quickstart.md). Follow it from [host design](#host-design-time) through [customer customization](#customer-extension-design) to [runtime](#runtime).

## Host design time

The application developer defines a **domain capability model (DCM)**. The `TaskCompleted` hook is the integration point. The host invokes it after task completion commits, supplies a `taskId` as input, and expects a program result with status `CREATED` or `NO_OP`. It reports the extension's outcome separately. The original completion stands even if the extension fails.

The DCM catalog makes these ordinary task operations available to extensions.

| Capability | Effect | Business arguments | Successful result |
| --- | --- | --- | --- |
| `tasks.get` | READ | Task ID | Task ID, project ID and status |
| `tasks.create` | WRITE | Project ID and title | Created task ID |

The host supplies protected values such as the authenticated actor's ID. The program supplies the business arguments. Calling an ordinary task creation operation at the right time is what makes the new task a follow-up.

The host selects the exact DCM bytes and binds them with `bindModel(dcmBytes, implementations)`. Each catalog entry gets one callable, including entries a particular program never uses. Registrations match the declared capability version and native key type and share a mode, either simulation or real. The callable can use a local implementation or call an internal service over HTTP or RPC. The host owns routing, credentials and response mapping. A host integration assistant can help design the model and bindings. The application owner approves them.

[Example DCM](../conformance/generic-hooks/effect4/dcm.json) · [Host model binding](specs/host-model-binding.md#corrected-host-bound-4-binding)

## Customer extension design

A customer can describe the rule to an authoring assistant, which helps turn it into a constrained program and test it. The program reads the task, checks its status and project, then calls `tasks.create` only when both match. The project and title are constants in the customer's source. Changing them leaves the hook and capability catalog reusable. [Example source](../conformance/generic-hooks/effect4/source.yaml)

The compiler checks the source against the host-selected DCM. It rejects a second possible WRITE even if the two calls have mutually exclusive conditions. A valid program can skip its one WRITE or reach it once. The portable artifact carries the program's instructions, constants and checked expressions, with a reference to the exact model. Java and Node can run the same artifact. [Compiled artifact](../conformance/generic-hooks/effect4/artifact.json)

The host selects the exact artifact bytes and checks their integrity against a separately stored digest. It calls `prepareProgram(artifactBytes, boundModel)` to check compatibility and get a reusable handle. This checks the program and its dependencies without calling a service. Approval and activation belong to the host. An active selection ties the reviewed artifact to its exact model, so a new catalog version requires a compatible, reviewed selection before it can be used.

In simulation, the same compiled artifact runs against controlled implementations. A completed task in `project1` should request a follow-up. An open task or a task in another project should skip the write. A lost response after dispatch should report uncertainty. These scenarios show what the program does with the supplied responses. In Daymark, users can review simulated examples, approve the exact checked revision and then enable it for future completions.

## Runtime

After `task1` completes, the host invokes the selected prepared program with `{ "taskId": "task1" }`. It supplies fresh context separately from that business input. The context contains protected bindings, one absolute deadline on the host's clock, the original native request key, and an event with an ID and source. Keir passes the key unchanged to each reached implementation.

Keir validates the input, context and call arguments, and enforces resource limits, the deadline and the single-write limit. The host's registered implementations enforce current permissions and authenticate the key. In Daymark's real mode, the task implementations check that the event can still run and that the owner can access the relevant resources. Task creation checks the original key and commits the task and its receipt in one database transaction. The quickstart uses simulated implementations with fixture data.

1. Keir evaluates `input.taskId` and passes `task1` to the registered `tasks.get` implementation. The returned task becomes the input to the program's condition.
2. The program compares the task's status and project with `COMPLETED` and its own `project1` constant.
3. If both match, Keir dispatches `tasks.create` with `{ "projectId": "project1", "title": "Follow up" }`. Otherwise the program skips the write.
4. Keir returns the program result and any write evidence separately. The host decides how to present or act on the outcome.

| Scenario | Program outcome | Write evidence |
| --- | --- | --- |
| Follow-up succeeds | `COMPLETED` with status `CREATED` | `CONFIRMED` receipt |
| Condition is false | `COMPLETED` with status `NO_OP` | No write dispatched |
| Response is lost after dispatch | `UNCERTAIN` with no ordinary result | `UNKNOWN` receipt |

A program's `CREATED` label expresses its own result. A confirmed receipt is the evidence for the write. When a receipt is unknown, the host can use the original key to reconcile the outcome before deciding how to proceed. Daymark offers a receipt lookup for that purpose.

- [Run the example in Node and Java](quickstart.md)
- [Read the hook and capability contract](specs/generic-hook-capability-contract.md)
- [Explore the host and customer assistant roles](agents/README.md)
