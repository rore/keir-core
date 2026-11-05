# Java compiler

The Java library and standalone CLI compile strict JSON/YAML source against a host-selected DCM. For `keir-effect/4`, use `new KeirCompiler().compile(sourceBytes, trustedDcmBytes, KeirCompiler.Environment.effect4())` or pass `--profile=keir-effect/4` after the CLI's source and DCM file arguments. The compiler accepts `keir-dcm/effect-4` and emits a `keir-artifact/effect-4` artifact referencing that exact model. The [quickstart](../docs/quickstart.md) shows the build, compilation and execution in both hosts.

## Legacy profile selection

The [`keir-read/1` profile](../docs/specs/language.md) remains the default. Select [`keir-effect/1`](../docs/specs/single-effect.md) explicitly with `KeirCompiler.Environment.effect()` or `--profile=keir-effect/1`. The legacy `keir-effect/3` profile uses `KeirCompiler.Environment.effect3()` or `--profile=keir-effect/3`.

The explicit [`/2` profiles](../docs/specs/generic-hook-capability-contract.md) add optional narrowing `capabilityPolicy`, an omitted effect ceiling of one, and authored nominal String literals such as `{literal: project1, type: ProjectId}`. Select `Environment.read2()` or `Environment.effect2()` in Java, or pass exactly `--profile=keir-read/2` or `--profile=keir-effect/2` to the CLI. These require the corresponding `keir-dcm/read-2` or `keir-dcm/effect-2` model and emit reference-only artifacts containing the exact `modelRef` and `keir-record-descriptors/1` recipe. The trusted model is still required at compilation; runtime model binding and execution are separate checks. Compiler-derived reference programs and variants live under [`conformance/generic-hooks`](../conformance/generic-hooks/).

## Build and run

The build scaffold uses JDK 21, Maven 3.10.0 through the repository wrapper, and Maven Compiler Plugin 3.14.1 with `release=21`. These versions are documented by [Apache Maven](https://maven.apache.org/download.cgi) and the [compiler plugin](https://maven.apache.org/plugins-archives/maven-compiler-plugin-3.14.1/plugin-info.html). The `dev.keir` Maven coordinates are local development coordinates; release coordinates remain undecided.

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

The compiler returns artifact bytes and their SHA-256 on success, or an ordered diagnostic list on failure. The caller owns finite compilation time and concurrency budgets. The host separately verifies and approves the selected artifact before activation. Dependency pins are CEL-Java `0.14.0`, protobuf-java `4.35.1`, Jackson Core `2.19.2` and SnakeYAML `2.4`. The selected environment also pins the Node CEL/protobuf versions for later artifact admission. Unsupported environment/profile selections fail closed.

Packaging also creates `target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar` with its dependencies. The CLI takes two file arguments in order (source, DCM) and an optional named third argument `--profile=PROFILE`. Without the selector, the CLI uses `keir-read/1`. On success it writes canonical artifact bytes to stdout. On failure it writes safe JSON diagnostics to stderr and returns nonzero. Input reads stop at the raw-byte ceiling. The CLI spawns no children. A host launching the CLI must bound output, wall time and concurrency, terminate and reap the process on cancellation, and preserve stdout as bytes.

For the legacy effect profile, add the exact named third argument `--profile=keir-effect/1`. After building the compiler, create the ignored `.tools/compiler-example` directory and run from the repository root:

```sh
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar \
  conformance/single-effect/source.yaml \
  conformance/single-effect/dcm.json \
  --profile=keir-effect/1 > .tools/compiler-example/effect-artifact.json
```

Select expected artifact/model digests through the host's independent admission path. This compiler checkpoint does not implement effect execution or qualify native keys, write receipts or simulation behavior in either runtime. Handlers are terminal typed records, and their expressions count toward the existing AST limits. Guarded results never enter later expression environments; every possible write counts toward the one-write ceiling even when guarded false. Scalar operators, protobuf descriptor recipe and qualified checked-expression metadata are inherited from the read compiler.

```sh
mkdir -p .tools/compiler-example
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar \
  conformance/read-and-decide/source.yaml \
  conformance/read-and-decide/dcm.json \
  > .tools/compiler-example/artifact-one.json
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar \
  conformance/read-and-decide/source.yaml \
  conformance/read-and-decide/dcm.json \
  > .tools/compiler-example/artifact-two.json
cmp .tools/compiler-example/artifact-one.json .tools/compiler-example/artifact-two.json
```

Use a byte-preserving process API or PowerShell 7.4+ native redirection on Windows; older PowerShell redirection can change encoding. Check the exit code before using output. Separately retain the expected artifact digest through a host-controlled admission path; a digest copied from customer-controlled artifact metadata grants no authority.

Tests cover canonical byte/hash vectors, JSON/YAML rejection, Unicode, model and expression boundaries, host overrides, nominal/enum/dataflow failures, deterministic descriptors/checked metadata, and real CEL classification evaluation. They do not implement the runtimes or establish hard deadlines, complete memory isolation, production containment, or the broader language.
