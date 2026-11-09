# Java compiler

For the packaged CLI, follow [released-package installation](../docs/installation.md). The build commands below are for contributors working from source.

The Java library and standalone CLI compile Keir against the application’s domain capability model (DCM). The output is a compiled artifact that refers to the exact model used for validation. [Compile and run the follow-up rule](../examples/README.md#compile-the-rule)

The compiler accepts source `keir/2` with model `keir-dcm/3` and produces `keir-artifact/3`. That model supports post-commit event extensions and read-only pre-operation decisions. See the [decision walkthrough](../docs/decision-walkthrough.md) and [exact compatibility matrix](../docs/specs/compiler-artifact.md#source-and-artifact-identity).

<a id="legacy-profile-selection"></a>
For document identities and the Java compiler API, see the [compiler reference](../docs/specs/compiler-artifact.md).

## Build and run

Use the source-build prerequisites in [Contributing](../CONTRIBUTING.md#build-and-test). The [Maven wrapper](../.mvn/wrapper/maven-wrapper.properties) and [parent POM](../pom.xml) pin the build tools. Development builds use the `dev.keir` snapshot coordinates. Release packages carry their versioned compiler JAR and runtime Maven POMs.

From the repository root, in a disposable PowerShell process:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl compiler-java -am verify
```

On POSIX:

```sh
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl compiler-java -am verify
```

Omit `-pl compiler-java -am` to build all Java modules. The wrapper downloads its pinned, checksum-verified Maven distribution; these commands keep its cache and dependencies under ignored `.tools/` without a global Maven installation.

The compiler returns artifact bytes and their SHA-256 on success, or an ordered diagnostic list on failure. The caller owns finite compilation time and concurrency budgets. The host separately verifies and approves the selected artifact before activation. The [compiler POM](pom.xml) pins its dependencies; the [artifact contract](../docs/specs/compiler-artifact.md#descriptortype-bridge) identifies the matching execution environment. Unsupported environment/profile selections fail closed.

Packaging also creates `target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar` with its dependencies. The CLI takes the source and DCM paths in that order. On success the compiler writes canonical artifact bytes to stdout. On failure it writes safe JSON diagnostics to stderr and returns nonzero. Input reads stop at the raw-byte ceiling. A host launching the CLI must bound output, wall time and concurrency, terminate and reap the process on cancellation, and preserve stdout as bytes.

<a id="implementation-reference-for-earlier-examples"></a>

[Build the original read and simulated-write artifacts](../docs/internal/implementation-reference.md#original-compiler-examples).

Tests cover canonical byte/hash vectors, JSON/YAML rejection, Unicode, model and expression boundaries, host overrides, nominal/enum/dataflow failures, deterministic descriptors/checked metadata, and real CEL classification evaluation. They do not implement the runtimes or establish hard deadlines, complete memory isolation, production containment, or the broader language.
