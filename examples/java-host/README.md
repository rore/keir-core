# Java host example

The separate `dev.keir.examples.GenericHost` entrypoint executes the [generic `/2` corpus](../../conformance/generic-hooks/README.md). It accepts one trusted fixture directory, `conformance/generic-hooks/read` or `conformance/generic-hooks/effect`, and loads fixed program filenames plus independent pins. For example: `java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.GenericHost conformance/generic-hooks/effect`. Each batch binds the complete approved catalog once and prepares three programs against that same handle. It supplies fresh native context and current authorization per invocation, validates actual dispatch independently of expected calls, and enforces finite lexical harness reservations. It uses no compiler, YAML parser, network or real adapters. The legacy entrypoints and fixtures below remain separate compatibility checks.

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

## Post-commit effect simulation

The additional `dev.keir.examples.EffectHost` entrypoint runs the fixed 25-case [single-effect corpus](../../conformance/single-effect/scenarios.json). The original JAR manifest and two-argument read CLI remain unchanged. After the same package build, run from the repository root:

```powershell
java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.EffectHost `
  conformance/single-effect/artifact.json `
  sha256:d441413373a59a0a40a509143146fef8fe9c1c2426bee430965efddee591f31c `
  sha256:d97c00f679ed0441817a610a71faeda949069c34fcb2a8d89774838d87a71e77
```

The artifact and both independent pins are external admission inputs. Only the fixed scenario catalog is packaged as a resource. The whole batch may exceed 65,536 bytes; each closed scenario is bounded to 65,536 bytes and four stubs, with at most 64 cases per batch. The complete batch is validated/executed before stdout receives one array of `{id,outcome,calls,consumption}` rows. Invalid arguments exit 2; invalid files, pins, scenarios or simulation assertions exit 1 with no partial batch and a fixed diagnostic.

Trusted fixtures supply the native `java.util.UUID`, event, protected actor, grants, authorization and logical clock. Scenario input cannot override them. Both adapters receive the exact original UUID object. Arguments and protected origins are checked before a stub is consumed; captured calls are matched to actual `CALL_DISPATCHED` events by ordinal, taking step IDs from those events. A separate violation ledger prevents a runtime-caught adapter mismatch from passing as an expected business failure. No source interpretation, alternate execution engine or compiler dependency is used.

The independent EVENT_REPORT consumer checks the closed outcome, trace, error/cause, result and effect shapes. A terminal handler may return any typed output enum value, including NO_OP or CREATED; classification and error remain BUSINESS_REJECTION, and the label establishes no effect evidence. Uncertainty has no ordinary result, and a CONFIRMED receipt can accompany failed execution or rejection of a later read. Trace evidence must distinguish an earlier confirmed write from the rejected call; a rejected write still requires NONE. Malformed outcomes return `{status:NOT_REPORTED,code:OUTCOME_INVALID}`. Duplicate/payload-mismatch cases use explicitly preseeded fake responses; they do not establish real persistence, atomic deduplication, retention, authorization or recovery guarantees. No real adapter mode is admitted.

The test-only `dev.keir.runtime.EffectScenarioDriver` emits the same full rows using the runtime module's pinned resources and this packaged host. The combined verifier supplies both the host JAR and runtime test classes on its classpath. A small reflective call avoids adding a reverse runtime-to-host dependency or duplicating the simulator. On Windows:

```powershell
java -cp "examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar;runtime-java/target/test-classes" dev.keir.runtime.EffectScenarioDriver
```

Module checkpoint on 2026-10-30: offline host/runtime coverage verification passed 95 runtime and 22 host tests in 12.83 seconds. Host coverage is 339/342 lines and 456/514 branches, including all lines in the effect simulator, consumer and JSON transport. Both fresh packaged effect entrypoints matched all 25 complete expected rows; the old four-case read CLI and four packaged effect CLI rejection cases also passed. The JAR retains the read manifest, contains the fixed catalog and excludes Keir/CEL compiler, CEL parser, ANTLR and YAML classes. Combined Node/Java verification and CI acceptance are separate manager-owned checkpoints.
