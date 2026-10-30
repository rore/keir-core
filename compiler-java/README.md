# Java compiler

Java library and standalone CLI for the [`keir-read/1` profile](../docs/specs/language.md) and compiler support for [`keir-effect/1`](../docs/specs/single-effect.md). It validates strict JSON/YAML source against a host-selected DCM, checks grants, dataflow and nominal/enum types, compiles real CEL-Java checked expressions, and emits one canonical portable artifact or safe diagnostics.

Compilation does not approve activation, authenticate its own output, or certify host guarantees. Java/Node runtime conformance remains a separate checkpoint. Start with the [first milestone](../roadmap/board.md).

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

The normal module jar exposes `KeirCompiler.compile(sourceBytes, trustedDcmBytes, Environment.supported())`. Its result contains either immutable artifact bytes plus their SHA-256, or an ordered diagnostic list with no artifact. The caller owns finite compilation time/concurrency budgets. Dependency pins are CEL-Java `0.14.0`, protobuf-java `4.35.1`, Jackson Core `2.19.2` and SnakeYAML `2.4`; the supported environment also pins the qualified Node CEL/protobuf versions for later artifact admission. Unsupported environment/profile selections fail closed.

Explicitly select the effect compiler with `KeirCompiler.Environment.effect()`. It requires `keir-dcm/effect-1` and emits `keir-artifact/effect-1`; source stays `keir/v0`. The compiler never infers this selection from source or DCM content. The original API and two-argument CLI continue selecting only the read profile, with unchanged golden bytes.

Packaging also creates `target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar`, containing its dependencies. From the repository root, the CLI accepts exactly two file arguments and writes canonical artifact bytes to stdout only on success; failures write safe JSON diagnostics to stderr and return nonzero. Input reads stop at the raw-byte ceiling. It spawns no children; a host launching it must bound output, wall time and concurrency, terminate/reap the process on cancellation, and preserve stdout as bytes.

For the effect profile only, add the exact named third argument `--profile=keir-effect/1`. After building the compiler, create the ignored `.tools/compiler-example` directory and run from the repository root:

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
