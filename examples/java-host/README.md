# Java host example

This command-line host prepares the shared Java-compiled artifact once using a digest supplied outside the artifact, then runs four controlled cases through that same prepared handle in one process. The simulation adapter supplies completed, open, missing and deliberately wrong-resource task data. The independent host consumer displays only a COMPLETED outcome with exactly one `classification` field whose value is `DONE` or `PENDING`.

From the repository root, build the host and its runtime dependency with JDK 21 in a disposable PowerShell process:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl examples/java-host -am package
java -jar examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar conformance/read-and-decide/artifact.json sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424
```

The two-argument CLI prints one JSON array of `{scenario,outcome,consumption}` records. Outcomes retain their canonical runtime representation; consumption is separately DISPLAYED or NOT_DISPLAYED:

| Scenario | Runtime result | Host consumption |
| --- | --- | --- |
| `completed` | COMPLETED, classification DONE | DISPLAYED |
| `open` | COMPLETED, classification PENDING | DISPLAYED |
| `missing` | BUSINESS_REJECTION, TASK_NOT_FOUND | NOT_DISPLAYED |
| `wrong-task` | FAILED, ADAPTER_EXCEPTION | NOT_DISPLAYED |

The wrong-resource fake returns an ID different from the requested task. The host adapter checks that relationship before returning SUCCESS; generic runtime validation cannot infer it. Every invocation supplies a fresh protected actor binding, current grant, exact invocation/capability authorization and monotonic deadline. These are fixed fictional simulation data, not live authorization or persistence.

The host accepts at most 1 MiB of artifact bytes. The expected artifact digest is an external trust input; do not derive it from the selected artifact. Admission independently pins the fixture's DCM digest, compiler, environment, entrypoint and DISPLAY consumer. A bad argument count exits 2; file, size or admission errors exit 1 with no result batch and a fixed diagnostic. The four controlled scenarios, including expected failures, form a successful batch and exit 0.

Adapter and authorization waits are deadline-bounded, while synchronous CEL and callbacks cannot be preempted; this example does not establish a hard execution deadline. It performs no live reads, persistence or task management. The package contains the runtime and its pinned runtime dependencies, with no compiler/parser dependency.

Focused in-process host tests exercise the complete batch, consumer validation, argument/file/digest failures, the inclusive file bound and adapter/authorization boundaries:

```powershell
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl examples/java-host -am -Dtest=JavaHostTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Host-correction verification on 2026-10-29: five host tests passed (0.705 seconds focused, 1.105 seconds with coverage). The coherent offline root coverage build passed 89 tests in 16.396 seconds. Host coverage is 64/66 lines (96.97%) and 52/52 branches (100%); the two missed lines are the thin `main`/`System.exit` wrapper. The packaged CLI is also exercised once as a four-case batch outside the coverage agent. Compiler/runtime behavior and the primary golden/shared expectations are unchanged.
