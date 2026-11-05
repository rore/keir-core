# Embed Keir in an application

An application that embeds Keir is called the **host**. The host decides when rules run, exposes the operations rules may call, and handles permissions and storage. Keir checks rules against the host’s contract and runs them within the selected profile’s limits.

The current Keir specification supports reads and at most one possible write per program. An application can restrict an entrypoint to reads. [Keir specification](specs/README.md#current-profile)

## 1. Define what the host exposes

Describe the hook, its input and result, and the available operations in a domain capability model (DCM). For the [follow-up example](../conformance/generic-hooks/effect4/dcm.json), the hook receives a task ID after the app saves the completion. The rule can read that task and request a new task. The host provides the authenticated actor separately from the rule’s arguments.

Implement each operation in host code. An implementation can call an existing local service or an internal API. The host checks current permissions, authenticates the original request key, and reports what happened to a write. [Model schema](../schemas/effect-4/README.md) · [Write outcomes](specs/real-single-effect-admission.md#effect-outcomes-and-key-identity)

## 2. Compile and review a rule

Write the rule in Keir, then compile it against the exact DCM. The compiler produces an **artifact**, the compiled rule the runtime executes. The artifact references the model it was checked against. [Compile and run the example](../examples/README.md#compile-the-rule)

Run the same artifact with simulated implementations before enabling it. Write down the expected outcome for each case before running the simulation. For the follow-up rule, check a matching completion, a completion in another project, and a lost response after a write request. The host records approval and activation as separate decisions for the exact reviewed artifact. [Run the sample cases](../examples/README.md#run-both-hosts)

## 3. Bind, prepare and run

| Step | Host responsibility | API |
| --- | --- | --- |
| Bind | Supply the model bytes and one compatible implementation for every operation. All registrations use the same mode. | `bindModel(dcmBytes, implementations)` |
| Prepare | Verify the selected artifact’s integrity in host code, for example with a trusted digest. Check diagnostics after binding and preparation. | `prepareProgram(artifactBytes, boundModel)` |
| Run | Pass the event input and fresh host context, including protected bindings, an absolute deadline, the original request key, and the event ID and source. | Node.js `invokeProgram`, Java `invokeBoundEffectProgram` |

The host can reuse a prepared rule across invocations. When it changes the model, verify compatibility and compile and review any rule that adopts the new model.

Read the rule result and write evidence separately. A result such as `CREATED` is a value returned by the rule. The write receipt records what the implementation reported. When the receipt is `UNKNOWN`, the write may already have happened. Reconcile it through the host’s services before deciding what to do next.

[Exact Java and Node.js API shapes](specs/host-model-binding.md#api-shapes) · [Example implementations](../examples/README.md#read-the-host-code)
