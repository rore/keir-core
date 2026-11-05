# Java runtime

<a id="current-host-bound-profile"></a>
## Bind and run a rule

Use these `KeirRuntime` entrypoints for the current single-write contract:

1. Call `bindModel(dcmBytes, implementations)` with the exact host-selected model and one `BoundEffectAdapter` per capability. All registrations use the same mode and match the catalog's capability versions and native key type.
2. Verify the selected artifact against a trusted digest in host code, then call `prepareProgram(artifactBytes, boundModel)`. Check `succeeded()` on both binding and preparation before using their handles.
3. Call `invokeBoundEffectProgram(prepared, businessInput, new BoundEffectInvocation(bindings, deadline, requestKey, event))` to get a `CompletionStage<Outcome>`. Supply fresh context for every invocation. The host implementations check current permissions and authenticate the original key.

See [Embed Keir](../docs/embedding.md), the [runnable Java example](../examples/java-host/README.md), and the [exact adapter and deadline types](../docs/specs/host-model-binding.md#api-shapes).

<a id="legacy-2-apis"></a>
<a id="implementation-reference-for-earlier-apis"></a>
<a id="catalog-bound-read-and-simulated-write"></a>
<a id="legacy-1-apis"></a>
<a id="original-read-and-simulated-write"></a>

[Earlier Java runtime APIs](../docs/internal/compatibility-reference.md#earlier-java-apis).

## Verification

Async authorization and adapter waits have finite native timeouts even when an injected logical clock stays stationary. Expiry checks run on independent daemon workers so a blocking host clock does not occupy the shared scheduler; that clock can still retain its own worker/invocation. Expired or regressing clocks stop later work, and late completions are ignored. Synchronous CEL and synchronous host callbacks cannot be preempted. Checked-AST/data ceilings and before/after clock checks do not establish hard execution deadlines, CPU gas equivalence, memory isolation or production tenant containment. Stopping the wait does not cancel underlying host I/O.

From the repository root, in a disposable PowerShell process with project-local caches:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl runtime-java -am test
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" verify
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -Pcoverage verify
```

The opt-in coverage profile uses [JaCoCo 0.8.16](https://www.jacoco.org/jacoco/index.html). It reports actual component coverage in each module's `target/site/jacoco/` directory; it adds no coverage threshold or CI gate. Tests use local fixtures and simulated callbacks, with no live services. The runtime tests include checked-wire/AST mutations, fresh authority, exact traces, Unicode/false/empty values, and native timeout behavior. Shared scenario execution uses the same externally pinned golden bytes as Node; the batch test driver lives only in test sources.

Checkpoint verification on 2026-10-29: the canonical offline clean coverage build passed 31 compiler, 56 runtime and 5 host tests in 35.404 seconds. The combined verifier matched Node's complete normalized output for all 61 shared cases (52 invocations), including exact traces, callback requests, remaining budgets and consumption receipts. Both module suites execute the same compiled two-read fixture for fresh-grant exhaustion, identity/counter reset and first-read failure. The independent host JAR runs all four sample outcomes without compiler/parser classes.

| Component | Covered lines | Covered branches |
| --- | ---: | ---: |
| Compiler | 711/756 (94.05%) | 501/607 (82.54%) |
| Runtime | 697/704 (99.01%) | 600/717 (83.68%) |

Coverage review includes authority, callback failure and timeout paths. Uncovered paths include defensive checks behind preparation invariants, CEL planner/evaluator exception handling and native timeout rearming beyond a 2,147,483,647 ms chunk. Long-duration timer behavior and hard synchronous interruption remain unqualified; these measurements are regression evidence, not containment guarantees.

Effect module checkpoint on 2026-10-30: 95 runtime tests passed independent offline clean coverage verification in 23.05 seconds, preserving all 56 existing read tests. Coverage is 869/875 lines and 808/939 branches. Tests execute the pinned primary artifact and load Node's same compiled handler-output fixture through existing test resources, including exact 8,192-byte success and 8,193-byte rejection with NONE and the original cause retained. They cover native key identity, separate key-label namespaces, prepared reuse, admission/authority rejection, false guards, terminal handlers, trace reservation, winning response evidence before invalid/expired clocks, timeout-first late responses and outer-envelope mutation.

See the [compiler build notes](../compiler-java/README.md) for pinned tooling and POSIX cache isolation.
