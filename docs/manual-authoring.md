# Write and check an extension without an agent

This developer example shows the Keir tools that a host-integrated authoring agent uses. You supply source and independent expected scenarios yourself. The [Daymark demo](https://github.com/rore/keir-demo-app) presents the customer experience through its assistant chat instead of a source editor.

Use the existing [task-advice example](../conformance/read-simulation/task-advice/). It classifies a saved task: OPEN produces PENDING advice; COMPLETED produces DONE advice. Advice does not change the task's native status. No AI account, provider key, task server or database is needed for this example.

## 1. Inspect the host contract and write source

The host supplies [dcm.json](../conformance/read-simulation/task-advice/dcm.json): a `ClassifyTask` hook, typed input/output and the generic `tasks.get` capability. The customer supplies [source.yaml](../conformance/read-simulation/task-advice/source.yaml), including its classification rule:

```yaml
steps:
  - id: source
    call: tasks.get
    args:
      taskId: {cel: input.taskId}
  - id: completed
    let: {cel: 'source.status == "COMPLETED"'}
return:
  record:
    classification: {cel: 'completed ? "DONE" : "PENDING"'}
```

This excerpt is the program body; use the linked complete source for its language, domain and entrypoint declarations. The customer chooses the rule, not credentials, protected actor identity or service routing. The host integration supplies those separately.

## 2. Give source and the trusted model to the compiler

First follow the [packaged-host setup](agents/host-integration-assistant.md#run-the-packaged-hosts) to build the Java compiler/host and install the locally packed Node runtime. It uses project-local caches and requires the documented JDK, Node and npm versions.

From the repository root, using PowerShell 7.4 or later for byte-preserving native redirection:

```powershell
New-Item -Force -ItemType Directory .tools/manual-authoring | Out-Null
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar `
  conformance/read-simulation/task-advice/source.yaml `
  conformance/read-simulation/task-advice/dcm.json `
  --profile=keir-read/2 > .tools/manual-authoring/artifact.json
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed; do not execute the output.' }
$compiled = Get-FileHash .tools/manual-authoring/artifact.json -Algorithm SHA256
$expected = Get-FileHash conformance/read-simulation/task-advice/artifact.json -Algorithm SHA256
if ($compiled.Hash -ne $expected.Hash) { throw 'Compiled bytes differ from this reviewed example.' }
```

On POSIX, use the same CLI arguments with ordinary native redirection and check its exit status. The [compiler reference](../compiler-java/README.md) also describes the library interface and safe process limits for an embedding host.

Success produces canonical portable artifact bytes. Expressions contain checked CEL metadata; the artifact references the exact host model rather than copying its complete catalog. Failure produces diagnostics, not an executable artifact. Compilation grants no permission to activate a program.

The hash comparison above establishes that this compilation produced the existing reviewed example. If you change source or the model, establish a new host-reviewed selection; do not silently replace trusted pins to make a mismatch disappear.

## 3. Supply independent examples to the simulator

Read [open.json](../conformance/read-simulation/task-advice/open.json) before execution. It supplies hook input, a stubbed `tasks.get` response and separately authored expectations: one call, a completed runtime outcome with PENDING classification, and a DISPLAYED/PENDING receipt. [completed.json](../conformance/read-simulation/task-advice/completed.json) predicts DONE. Neither expectation is inferred from a previous run.

The simulator receives the selected artifact/model, a host-owned fixture and one scenario document. Run the same open-task case through either packaged host:

```powershell
node examples/node-host/read-simulation.mjs conformance/read-simulation/task-advice normal conformance/read-simulation/task-advice/open.json
java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.ReadSimulationHost conformance/read-simulation/task-advice normal conformance/read-simulation/task-advice/open.json
```

Each report should have `status: PASS`, a `COMPLETED` native classification and a `DISPLAYED`/`PENDING` receipt. Replace `open.json` with `completed.json` to see DONE. Then use `mismatch.json`: the expected report is MISMATCH because that document deliberately predicts the wrong result. Successful execution and matching customer expectations are separate facts.

## 4. See how the runner connects to the host

Inspect [the Node simulation host](../examples/node-host/read-simulation.mjs). It imports `runReadSimulation` from the packaged runtime, reads the independent [selection](../conformance/read-simulation/task-advice/selection.json), supplies the exact DCM/artifact bytes and builds a trusted fixture containing authorization, protected bindings, a deadline and a result consumer. Scenario stubs stand in for capability invokers. The runner executes the compiled program; the consumer translates its validated classification into a display receipt.

A live host binds the complete approved DCM and its real invokers together. Those invokers can call local services or internal HTTP/RPC APIs. It supplies current authorization, identity and deadlines for each invocation, and owns approval and activation storage. See the [binding contract](specs/host-model-binding.md) and [three-stage guided tour](guided-tour.md). The command-line fixture does not implement a customer approval service or prove real service permissions.

## 5. Verify the complete example

```powershell
node conformance/read-simulation/verify.mjs
```

This recompiles two exact artifacts and compares 19 packaged Java/Node reports, including expected mismatches and rejected inputs. The [simulation guide](agents/read-simulation.md) explains the additional cases. Its optional Python schema check is a separate command.

The customer-facing agent automates drafting, compiler repair and scenario proposal around these tools. Human inspection, approval and activation remain explicit host decisions. Passing these finite simulated cases does not certify every possible input, real adapter guarantees or production containment.
