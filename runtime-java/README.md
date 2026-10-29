# Java read-and-decide runtime

`keir-runtime-java` executes the shared `keir-read/1` artifact with JDK 21 and CEL Java 0.14.0. It has no dependency on the Keir compiler, CEL compiler/parser, ANTLR or YAML. Compilation belongs outside execution; see the independent [Java host](../examples/java-host/README.md) for a packaged simulation.

`KeirRuntime.prepare(bytes, admission, adapters)` returns a `Preparation` containing either an immutable `Prepared` handle or a safe `Diagnostic`. Admission supplies an externally trusted whole-artifact digest, DCM digest, entrypoint, consumer, compiler and environment pins, adapter mode, grants and optional narrowing limits. The whole-byte digest is checked before JSON parsing. Canonical JSON, the complete DCM and plan, generated descriptors, checked CEL metadata and the restricted expression profile are checked before CEL runtime preparation. Preparation invokes no host callback.

`KeirRuntime.invoke(prepared, businessInput, invocation)` returns a `CompletionStage<Outcome>`. Construct `Invocation` in trusted host code from fresh protected bindings, `Deadline(LongSupplier clock, long expiresAtMs)`, an asynchronous `Authorizer` and fresh grants. Clock readings use nondecreasing nonnegative integer milliseconds in the same arbitrary monotonic domain as expiry, bounded by 2^53-1. Omitted capability grants mean zero; positive counts may only narrow admission. Invocation data is validated and copied before asynchronous authorization.

Authorization receives `{entrypoint}`, then `{entrypoint,capability,version}` before each call. An `Adapter` registers an exact version, simulation/real mode and asynchronous function receiving separate immutable agent and protected argument maps plus remaining milliseconds. It returns the closed SUCCESS, BUSINESS_REJECTION or TRANSIENT_FAILURE records from the [runtime contract](../docs/specs/runtime.md). Host adapters retain responsibility for resource access and actual I/O deadlines.

`Outcome.toMap()` exposes a deeply immutable JSON-compatible record; `toJson()` serializes its canonical bytes. Only COMPLETED includes a result. Failures carry safe code/path pairs; traces expose event order and identifiers, without argument values or exception text. Validate results again in the selected host consumer.

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

See the [compiler build notes](../compiler-java/README.md) for pinned tooling and POSIX cache isolation.
