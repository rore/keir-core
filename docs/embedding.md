# Embed Keir in an application

An application that embeds Keir is called the **host**. The host decides when rules run, exposes the operations rules may call, and handles permissions and storage. Keir checks rules against the host’s contract and runs them within the executable contract’s limits.

Choose the lifecycle the application needs. An event extension runs after a native operation commits and can contain at most one possible write. A pre-operation decision can only read and returns `ALLOW` or `DENY` before the native operation. [Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0) supports both lifecycles. The [format matrix](specs/compiler-artifact.md#source-and-artifact-identity) identifies current authoring and historical execution pairs.

Start with [released-package installation](installation.md) for a small Node or Java consumer outside this repository. Use the [annotated example](guided-tour.md) to understand how the source and model fit together.

## 1. Define what the host exposes

Describe the hook, its input and result, and the available operations in a domain capability model (DCM). For the [follow-up example](../examples/node-host/follow-up/dcm.json), the hook receives a task ID after the app saves the completion. The rule can read that task and request a new task. The host provides the authenticated actor separately from the rule’s arguments.

Implement each operation in host code. An implementation can call an existing local service or an internal API. The host checks current permissions, authenticates the original request key, and reports what happened to a write. [Model rules](specs/domain-model.md) · [Write evidence](specs/runtime.md#write-evidence)

## 2. Compile and review a rule

Write the rule in Keir, then compile it against the exact DCM. The compiler produces an **artifact**, the compiled rule the runtime executes. The artifact references the model it was checked against. [Compile and run the example](../examples/README.md#compile-the-rule)

Run the same artifact with simulated implementations before enabling it. Write down the expected outcome for each case before running the simulation. For the follow-up rule, check a matching completion, a completion in another project, and a lost response after a write request. For a decision, check ALLOW, DENY and failed evaluation, including a handled rejection returning ALLOW. The host records approval and activation as separate decisions for the exact reviewed artifact. [Run the sample cases](../examples/README.md#run-both-hosts) · [Decision walkthrough](decision-walkthrough.md)

## 3. Bind, prepare and run

| Step | Host responsibility | API |
| --- | --- | --- |
| Bind | Supply the model bytes and one compatible implementation for every operation. All registrations use the same mode. | `bindModel(dcmBytes, implementations)` |
| Prepare | Verify the selected artifact’s integrity in host code, for example with a trusted digest. Check diagnostics after binding and preparation. | `prepareProgram(artifactBytes, boundModel)` |
| Run | Pass business input and fresh host context, including protected bindings, an absolute deadline, the original request key, and the event or operation-attempt ID and scope. | Node.js `invokeProgram`, Java `invokeBoundEffectProgram` |

The host can reuse a prepared rule across invocations. When it changes the model, verify compatibility and compile and review any rule that adopts the new model.

Read the rule result and write evidence separately. A result such as `CREATED` is a value returned by the rule. The write receipt records what the implementation reported. When the receipt is `UNKNOWN`, the write may already have happened. Reconcile it through the host’s services before deciding what to do next.

[Exact Java and Node.js API shapes](specs/host-model-binding.md#api-shapes) · [Example implementations](../examples/README.md#read-the-host-code)

## Before a native operation

A decision uses the same binding, preparation and invocation methods. The host selects a REQUEST/BEFORE_COMMIT/DECISION entrypoint and an approved artifact for it. The extension uses the model's declared input and approved READ capabilities. Its zero write allowance also prohibits false-guarded writes.

Consume the complete runtime outcome. Only valid COMPLETED ALLOW with an empty effects array permits continuation. DENY stops the native operation. A handled rejection, failure, malformed outcome, timeout or unavailable selected artifact blocks it. Authoritatively no active binding takes the ordinary native path; do not confuse absence with an unavailable selection.

Keep the proposed input fixed and reserve time for native validation and commit. After ALLOW, recheck selection, approval, authorization and mutable facts at the native commit boundary. Cancellation or expiry closes the attempt. The decision supplies no commit evidence; report the native result separately. [Full walkthrough](decision-walkthrough.md) · [Consistency and deadlines](specs/pre-operation-decisions.md#host-invocation-and-consistency-obligations)

## Example Node host

[Run the checked-in example](quickstart.md#run-the-example) or open the [Node host](../examples/node-host/follow-up/run.mjs) next to its [model](../examples/node-host/follow-up/dcm.json) and [extension](../examples/node-host/follow-up/source.yaml). The host binds the model, prepares the selected artifact once, and supplies fresh context for each task completion.

- The simulated read returns the task's ID, project and status. Its declared `TASK_NOT_FOUND` rejection demonstrates the adapter response shape.
- The simulated create receives the title supplied by the extension. It returns a confirmed result for the first completion. For the third completion it reports an unknown commit, so the invocation is `UNCERTAIN` with an `UNKNOWN` receipt.
- The host supplies `actorId`, event provenance, a deadline, and the original request key. The extension only receives the business task ID and any declared operation results.

Bind real or simulated implementations as a set. Both modes can use the same compiled bytes.

## Connect a task service

For a real `tasks.create` adapter, the extension supplies `projectId` and `title`. The host supplies the authenticated `actorId` and passes the original request key, event and deadline in the invocation context. Have the adapter call your task service under that authority. The service checks current project permissions and the authenticity of the key before attempting the write.

| What the task service can establish | What the adapter returns |
| --- | --- |
| The actor lacks permission and the service rejected the request before writing. | `{kind: 'BUSINESS_REJECTION', code: 'ACCESS_DENIED', effect: 'NONE'}` |
| The service committed the new task and returned its ID. | `{kind: 'SUCCESS', value: {id: taskId}, effect: 'CONFIRMED'}` |
| The connection dropped after dispatch and the service cannot establish whether it committed. | `{kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN'}` |

Use the original key to reconcile an uncertain attempt through your service. A timeout alone gives the host no evidence that the write was skipped. [Adapter response contract](specs/runtime.md#adapter-responses)

## Use the same artifact in Java

[Run the Java example](quickstart.md#run-in-java) and read [FollowUp.java](../examples/node-host/follow-up/FollowUp.java). It uses the same model, artifact, and cases as Node. Java registers `BoundEffectAdapter` values and invokes the prepared program with a fresh `BoundEffectInvocation`. Its calls return `CompletionStage` values. It supplies the clock as a `LongSupplier` and retains the original key as an object.

Use the [exact API shapes](specs/host-model-binding.md#api-shapes) and [adapter responses](specs/runtime.md#adapter-responses) when connecting actual services.
