# Embed Keir in an application

An application that embeds Keir is called the **host**. The host decides when rules run, exposes the operations rules may call, and handles permissions and storage. Keir checks rules against the host’s contract and runs them within the selected profile’s limits.

The current Keir specification supports reads and at most one possible write per program. An application can restrict an entrypoint to reads. [Keir specification](specs/README.md#current-profile)

## 1. Define what the host exposes

Describe the hook, its input and result, and the available operations in a domain capability model (DCM). For the [follow-up example](../conformance/generic-hooks/effect4/dcm.json), the hook receives a task ID after the app saves the completion. The rule can read that task and request a new task. The host provides the authenticated actor separately from the rule’s arguments.

Implement each operation in host code. An implementation can call an existing local service or an internal API. The host checks current permissions, authenticates the original request key, and reports what happened to a write. [DCM reference](specs/domain-model.md) · [Write evidence](specs/runtime.md#write-evidence)

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

## Example Node host

This small simulated host uses the [task model](../conformance/generic-hooks/effect4/dcm.json) and the [compiled follow-up extension](../conformance/generic-hooks/effect4/artifact.json). Its task service uses three sample rows. Two are in the launch project. One write returns simulated confirmation, and the other reports an unknown commit. The host code leaves the project filter and title to the extension.

Install the package using [Getting started](quickstart.md#run-the-example). Save the following as `examples/node-host/.tools/follow-up.mjs` and run it from the repository root with the artifact path as the only argument.

```js
import { randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { bindModel, prepareProgram, invokeProgram } from '@keir/runtime-node';

const dcm = readFileSync('conformance/generic-hooks/effect4/dcm.json');
const artifact = readFileSync(process.argv[2]);
const tasks = {
  task1: { id: 'task1', projectId: 'project1', status: 'COMPLETED' },
  task2: { id: 'task2', projectId: 'project2', status: 'COMPLETED' },
  task3: { id: 'task3', projectId: 'project1', status: 'COMPLETED' }
};
const registration = { version: '1', mode: 'simulation', keyType: 'sample.RequestUuid/1' };
let created;

const { boundModel, diagnostic: bindError } = bindModel(dcm, {
  'tasks.get': {
    ...registration,
    invoke: ({ taskId }) => Object.hasOwn(tasks, taskId)
      ? { kind: 'SUCCESS', value: tasks[taskId] }
      : { kind: 'BUSINESS_REJECTION', code: 'TASK_NOT_FOUND' }
  },
  'tasks.create': {
    ...registration,
    invoke: ({ projectId, title }, _protected, _remainingMs, { event }) => {
      if (event.source === 'task3') return { kind: 'UNKNOWN_COMMIT', effect: 'UNKNOWN' };
      created = { projectId, title };
      return { kind: 'SUCCESS', value: { id: 'follow-up' }, effect: 'CONFIRMED' };
    }
  }
});
if (bindError) throw new Error(bindError.code);
const { prepared, diagnostic: prepareError } = prepareProgram(artifact, boundModel);
if (prepareError) throw new Error(prepareError.code);

const clock = { nowMs: () => Math.floor(performance.now()) };
for (const taskId of Object.keys(tasks)) {
  created = undefined;
  const outcome = await invokeProgram(prepared, { taskId }, {
    bindings: { actorId: 'demo-user' },
    deadline: { clock, expiresAtMs: clock.nowMs() + 1000 },
    requestKey: randomUUID(),
    event: { id: `completion-${taskId}`, source: taskId }
  });
  const receipt = outcome.effects[0]?.state ?? '-';
  if (outcome.classification === 'UNCERTAIN') {
    console.log(`${taskId} UNCERTAIN ${receipt}`);
    continue;
  }
  if (outcome.classification !== 'COMPLETED') throw new Error(outcome.error.code);
  const title = created ? ` ${JSON.stringify(created.title)}` : '';
  console.log(`${taskId} ${outcome.result.status} ${receipt}${title}`);
}
```

The simulator uses local artifact files and mock services. A real integration verifies the approved bytes, supplies the original native request key and authenticated actor, and checks permissions and native invariants in its implementations. Bind either real or simulation implementations as a set. Both modes can use the same compiled bytes.

## Use the same artifact in Java

Java uses the same model and artifact. Its adapters and invocation return `CompletionStage` values. It retains the original key as an object and supplies the clock as a `LongSupplier`.

Build the [Java example JAR](../examples/README.md#run-both-hosts) and save the following as `.tools/follow-up/FollowUp.java`.

```java
import dev.keir.runtime.KeirRuntime;
import dev.keir.runtime.KeirRuntime.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongSupplier;
import static java.util.concurrent.CompletableFuture.completedFuture;

class FollowUp {
    public static void main(String[] args) throws Exception {
        var model = Files.readAllBytes(Path.of("conformance/generic-hooks/effect4/dcm.json"));
        var artifact = Files.readAllBytes(Path.of(args[0]));
        var projects = Map.of("task1", "project1", "task2", "project2", "task3", "project1");
        var read = new BoundEffectAdapter("1", "simulation", "sample.RequestUuid/1",
            (business, protectedArgs, remainingMs, context) -> {
                var task = (String) business.get("taskId");
                var project = projects.get(task);
                if (project == null) {
                    return completedFuture(Map.of("kind", "BUSINESS_REJECTION", "code", "TASK_NOT_FOUND"));
                }
                return completedFuture(Map.of("kind", "SUCCESS", "value",
                    Map.of("id", task, "projectId", project, "status", "COMPLETED")));
            });
        var create = new BoundEffectAdapter("1", "simulation", "sample.RequestUuid/1",
            (business, protectedArgs, remainingMs, context) -> {
                if (context.event().get("source").equals("task3")) {
                    return completedFuture(Map.of("kind", "UNKNOWN_COMMIT", "effect", "UNKNOWN"));
                }
                return completedFuture(Map.of("kind", "SUCCESS", "effect", "CONFIRMED",
                    "value", Map.of("id", "follow-up")));
            });
        var binding = KeirRuntime.bindModel(model, Map.of("tasks.get", read, "tasks.create", create));
        if (!binding.succeeded()) throw new IllegalStateException(binding.diagnostic().code());
        var program = KeirRuntime.prepareProgram(artifact, binding.boundModel());
        if (!program.succeeded()) throw new IllegalStateException(program.diagnostic().code());

        long start = System.nanoTime();
        LongSupplier clock = () -> (System.nanoTime() - start) / 1_000_000;
        for (var id : List.of("task1", "task2", "task3")) {
            var invocation = new BoundEffectInvocation(
                Map.of("actorId", "demo-user"), new Deadline(clock, clock.getAsLong() + 1000),
                UUID.randomUUID(), Map.of("id", "completion-" + id, "source", id));
            var outcome = KeirRuntime.invokeBoundEffectProgram(
                program.prepared(), Map.of("taskId", id), invocation).toCompletableFuture().join();
            var receipt = outcome.effects().isEmpty() ? "-" : outcome.effects().getFirst().get("state");
            var status = outcome.classification().equals("COMPLETED")
                ? outcome.result().get("status") : outcome.classification();
            System.out.printf("%s %s %s%n", id, status, receipt);
        }
    }
}
```

From the repository root:

```sh
java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar .tools/follow-up/FollowUp.java conformance/generic-hooks/effect4/artifact.json
```

```text
task1 CREATED CONFIRMED
task2 NO_OP -
task3 UNCERTAIN UNKNOWN
```

Use the [exact API shapes](specs/host-model-binding.md#api-shapes) and [adapter responses](specs/runtime.md#adapter-responses) when connecting actual services.
