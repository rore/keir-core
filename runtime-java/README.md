# Java runtime

Use [released-package installation](../docs/installation.md#configure-and-run-in-java) to configure the included Maven repository and run a consumer outside this checkout.

<a id="current-host-bound-profile"></a>
## Bind and run a rule

Event extensions and pre-operation decisions use the same `KeirRuntime` entrypoints:

1. Call `bindModel(dcmBytes, implementations)` with the exact host-selected model and one `BoundEffectAdapter` per capability. All registrations use the same mode and match the catalog's capability versions and native key type.
2. Verify the selected artifact against a trusted digest in host code, then call `prepareProgram(artifactBytes, boundModel)`. Check `succeeded()` on both binding and preparation before using their handles.
3. Call `invokeBoundEffectProgram(prepared, businessInput, new BoundEffectInvocation(bindings, deadline, requestKey, event))` to get a `CompletionStage<Outcome>`. Supply fresh context for every invocation. The host implementations check current permissions and authenticate the original key.

See [Embed Keir](../docs/embedding.md), the [runnable Java example](../examples/java-host/README.md), and the [exact adapter and deadline types](../docs/specs/host-model-binding.md#api-shapes).

An EVENT/AFTER_COMMIT extension may contain at most one possible write. A REQUEST/BEFORE_COMMIT decision permits only reads. Consume its complete outcome before the native operation: only valid COMPLETED ALLOW with empty effects permits fresh host checks; DENY and failed evaluation stop continuation. ALLOW is not a commit receipt. The host retains authorization, read consistency, selection, cancellation and native commit responsibilities. [Decision walkthrough](../docs/decision-walkthrough.md) · [Supported version pairs](../docs/specs/compiler-artifact.md#source-and-artifact-identity)

<a id="legacy-2-apis"></a>
<a id="implementation-reference-for-earlier-apis"></a>
<a id="catalog-bound-read-and-simulated-write"></a>
<a id="legacy-1-apis"></a>
<a id="original-read-and-simulated-write"></a>

[Earlier Java runtime APIs](../docs/internal/compatibility-reference.md#earlier-java-apis).

## Exact numbers in the current source profile

Model `keir-dcm/4` uses canonical strings for Integer and Decimal business values, including capability arguments/results and rule outputs. Integer is signed int64. Decimal has at most 38 normalized coefficient digits and scale 0–18. Neither uses floating-point coercion or implicit rounding. Retained models keep their original type meanings.

For opt-in host text normalization, use `KeirRuntime.normalizeInteger("+00100.00")` or `KeirRuntime.normalizeDecimal("100.00")`; both return `"100"`. They accept at most 256 ASCII characters and reject invalid or inexact input with `IllegalArgumentException`. The helpers do not grant native price, payment or write authority. Bind/prepare/invoke signatures are unchanged. See the [runnable source examples](../examples/numeric/README.md); installed hosts need separately selected compatible packages.

## Verification

Async authorization and adapter waits have finite native timeouts even when an injected logical clock stays stationary. Expiry checks run on independent daemon workers so a blocking host clock does not occupy the shared scheduler; that clock can still retain its own worker/invocation. Expired or regressing clocks stop later work, and late completions are ignored. Synchronous CEL and synchronous host callbacks cannot be preempted. Checked-AST/data ceilings and before/after clock checks do not establish hard execution deadlines, CPU gas equivalence, memory isolation or production tenant containment. Stopping the wait does not cancel underlying host I/O.

From the repository root, in a disposable PowerShell process with project-local caches:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl runtime-java -am test
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" verify
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -Pcoverage verify
```

The opt-in coverage profile uses [JaCoCo](https://www.jacoco.org/jacoco/index.html), pinned in the [parent POM](../pom.xml). It reports actual component coverage in each module's `target/site/jacoco/` directory; it adds no coverage threshold or CI gate. Tests use local fixtures and simulated callbacks, with no live services. The runtime tests include checked-wire/AST mutations, fresh authority, exact traces, Unicode/false/empty values, and native timeout behavior. Shared scenario execution uses the same externally pinned golden bytes as Node; the batch test driver lives only in test sources.

[Verification results](../docs/testing.md#current-single-write-behavior-verification) identify the tested package versions, scenarios and raw validation cases. See the [conformance index](../conformance/README.md) for the versioned inputs and independent expectations.

See the [compiler build notes](../compiler-java/README.md) for pinned tooling and POSIX cache isolation.
