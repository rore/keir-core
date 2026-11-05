# Run your first Keir extension

This extension runs on the `TaskCompleted` hook. It reads the task and creates a follow-up titled “Follow up” if the task is complete and belongs to `project1`. Both example hosts use simulated task services and the same compiled artifact.

- The host's [domain capability model (DCM)](../conformance/generic-hooks/effect4/dcm.json) defines the `TaskCompleted` hook, types, and protected inputs. Its capability catalog includes `tasks.get` and `tasks.create`.
- The [extension source](../conformance/generic-hooks/effect4/source.yaml) chooses the project and follow-up title, and decides when to call `tasks.create`. The compiler checks it against the model and emits a portable artifact referencing that exact model. The `keir-effect/4` profile permits at most one potential write per program.
- The runtime executes the artifact and dispatches calls to the host's registered implementations. Those implementations supply the task data and report what happened to the write.

## Compile and run

On Windows, use JDK 21, Node.js 24 with npm, and PowerShell 7.4 or newer. Run the commands from the repository root in a dedicated shell. Maven and npm may fetch build dependencies and use the checkout's ignored `.tools` directory for caches.

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl compiler-java,examples/java-host -am "-DskipTests" package
if ($LASTEXITCODE -ne 0) { throw 'Build failed' }

$fixture = 'conformance/generic-hooks/effect4'
New-Item -ItemType Directory -Force .tools/quickstart,.tools/packages | Out-Null
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar "$fixture/source.yaml" "$fixture/dcm.json" --profile=keir-effect/4 > .tools/quickstart/artifact.json
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
if ((Get-FileHash .tools/quickstart/artifact.json).Hash -ne (Get-FileHash "$fixture/artifact.json").Hash) {
    throw 'Compiled artifact differs from the checked-in example'
}

npm.cmd pack ./runtime-node --pack-destination .tools/packages --cache .tools/npm-cache --ignore-scripts
if ($LASTEXITCODE -ne 0) { throw 'Node package failed' }
npm.cmd ci --prefix examples/node-host --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'Node host setup failed' }
node examples/node-host/generic.mjs $fixture
if ($LASTEXITCODE -ne 0) { throw 'Node host failed' }

java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.GenericHost $fixture
if ($LASTEXITCODE -ne 0) { throw 'Java host failed' }
```

PowerShell 7.4+ preserves the compiler's stdout bytes when redirecting to a file. The commands compare the fresh artifact with the checked-in copy. Both hosts then load that byte-identical copy and three companion artifacts from the fixture directory, verifying each artifact and the model against separate stored hashes. Each host prints a JSON array with one row per scenario. Find the cases below by `id` in the output or the [scenario file](../conformance/generic-hooks/effect4/scenarios.json).

| Scenario | Outcome | Write evidence |
| --- | --- | --- |
| `normal` | `COMPLETED`, result status `CREATED` | `CONFIRMED` |
| `open` | `COMPLETED`, result status `NO_OP` | No write dispatched |
| `write-rejected` | `BUSINESS_REJECTION`, error code `CREATE_REJECTED` | `NONE` |
| `lost-response` | `UNCERTAIN`, error code `ADAPTER_EXCEPTION`, no ordinary result | `UNKNOWN` |

Each row contains an `outcome`, captured `calls`, and a separate host `consumption` decision. In the outcome, read `classification`, `result` or `error`, and `effects` separately. The `zero` scenario returns `CREATED` with an empty `effects` array. The `response-expired` scenario exceeds its deadline and retains a `CONFIRMED` receipt. An `UNKNOWN` receipt means a write may have committed, so treat it as unresolved.

## Embed the same flow

1. **Bind the model.** Supply the host-selected DCM bytes and exactly one callable for every capability, even those the extension never calls. Register the matching version and native key type for each capability. Every implementation in this model uses the same mode. The examples register both task capabilities in `simulation` mode.
2. **Prepare the program.** Supply the host-selected artifact bytes and the bound model. The runtime checks model and artifact compatibility. The host selects approved bytes and checks their integrity against a separately stored digest, as the example hosts do. After both binding and preparation, confirm success before using the returned handle (`diagnostic === null` in Node, `succeeded()` in Java). Reuse a prepared handle with fresh context for each invocation.
3. **Invoke for the current event.** Supply the business input (here `{taskId: "task1"}`) and a separate host context. It carries protected `bindings` (here `actorId`), an absolute `deadline` measured on the supplied clock, the original non-null native `requestKey`, and a typed `event` with `id` and `source`. The runtime passes the key unchanged alongside the event and deadline. The callable receives `(args, protectedArgs, remainingMs, context)`.

| Step | Node | Java (`KeirRuntime`) |
| --- | --- | --- |
| Bind | `bindModel(dcmBytes, implementations)` with entries `{version, mode, keyType, invoke}` | `bindModel(dcmBytes, implementations)` with `BoundEffectAdapter` entries |
| Prepare | `prepareProgram(artifactBytes, binding.boundModel)` | `prepareProgram(artifactBytes, binding.boundModel())` |
| Invoke | `await invokeProgram(preparation.prepared, input, {bindings, deadline, requestKey, event})` | `invokeBoundEffectProgram(preparation.prepared(), input, new BoundEffectInvocation(bindings, deadline, requestKey, event))` returns a `CompletionStage<Outcome>` |

The [Node example](../examples/node-host/generic-simulation.mjs) builds the context in `runBoundEffectBatch`. The [Java example](../examples/java-host/src/main/java/dev/keir/examples/GenericSimulation.java) does the same in `boundEffectBatch` and its helper. See [host model binding](specs/host-model-binding.md#corrected-host-bound-4-binding) for the exact adapter and deadline types.

In a real integration, host code authenticates the event and caller, supplies protected bindings and the original key, and enforces current permissions and native key authenticity in the implementations. The host and its services own persistence, atomic commit, duplicate handling and reconciliation of uncertain writes. Keep the original key for reconciliation and avoid blind retries after an uncertain outcome. The example stubs only exercise the contract in simulation.
