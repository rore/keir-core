# Implementation reference

This appendix records the exact strings used by the compiler, runtime, and serialized files. Start with the [Keir specification](../specs/README.md) for the rules those strings identify.

## Serialized identifiers and compiler selectors

Keir source uses `language: keir/v0`. The compiler selects an execution contract from trusted configuration. The CLI uses the selector shown below after the source and model paths. With only the two paths it selects the original read-only contract.

| Behavior | CLI selector | Java compiler environment | DCM format | Artifact format |
| --- | --- | --- | --- | --- |
| Current single-write | `--profile=keir-effect/4` | `KeirCompiler.Environment.effect4()` | `keir-dcm/effect-4` | `keir-artifact/effect-4` |
| Original read-only | Omit the selector. | `KeirCompiler.Environment.supported()` | `keir-dcm/read-1` | `keir-artifact/read-1` |
| Original simulated write | `--profile=keir-effect/1` | `KeirCompiler.Environment.effect()` | `keir-dcm/effect-1` | `keir-artifact/effect-1` |
| Catalog-bound read | `--profile=keir-read/2` | `KeirCompiler.Environment.read2()` | `keir-dcm/read-2` | `keir-artifact/read-2` |
| Catalog-bound simulated write | `--profile=keir-effect/2` | `KeirCompiler.Environment.effect2()` | `keir-dcm/effect-2` | `keir-artifact/effect-2` |
| Qualified write | `--profile=keir-effect/3` | `KeirCompiler.Environment.effect3()` | `keir-dcm/effect-3` | `keir-artifact/effect-3` |

Call the Java compiler as `new KeirCompiler().compile(sourceBytes, trustedDcmBytes, environment)`. The current scenario fixture format is `keir-scenario/effect-4`. Earlier write scenarios use `keir-scenario/effect-1`, `keir-scenario/effect-2` or `keir-scenario/effect-3` with the matching artifact. The current DCM assurance value is `HOST_BOUND_SINGLE_WRITE`. The [schemas](../../schemas/effect-4/README.md) document the structure of the serialized files, including the descriptor recipe `keir-record-descriptors/1`.

## Contract compatibility

| Boundary | Current contract | Earlier contracts |
| --- | --- | --- |
| Source | `keir/v0`; explicit nominal literals, effect guards, terminal handlers and bounded plain String concatenation | `/1` rejects explicit nominal annotations; `/2` and `/3` support them. Earlier profiles reject concatenation. |
| Model assurance and mode | `HOST_BOUND_SINGLE_WRITE`; homogeneous real or simulation implementations | Effect/1–2 use `SIMULATION_ONLY`; effect/3 uses `HOST_QUALIFIED_SINGLE_WRITE` and a qualification protocol for real mode. |
| Model and artifact structure | Reference model, optional DCM `capabilityPolicy` and `effectGrant` with default catalog eligibility and one possible WRITE | `/1` embeds the model and descriptors and requires grants. `/2`–`/3` use reference models and optional policies. |
| Host API | Fresh bindings, deadline, original non-null key and typed event. Implementations enforce permissions and key authenticity. | Legacy APIs retain authorizer, grants or narrowing policy, keyBinding and scope or qualification as versioned. |
| Write outcome and consumption | At most one potential WRITE, with NONE, CONFIRMED or UNKNOWN evidence and EVENT_REPORT consumption | Single-write contracts share the evidence and consumer rules. |

Each serialized artifact keeps its exact model, artifact and scenario identity. Select the contract explicitly when compiling and executing it.

## Compile the follow-up example

Use JDK 21 from the repository root. These commands build the Java compiler and host, then compare the compiler output with the pinned artifact used by the [example hosts](../../examples/README.md).

```sh
mkdir -p .tools/examples
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl compiler-java,examples/java-host -am -DskipTests package
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar conformance/generic-hooks/effect4/source.yaml conformance/generic-hooks/effect4/dcm.json --profile=keir-effect/4 > .tools/examples/follow-up.json
cmp .tools/examples/follow-up.json conformance/generic-hooks/effect4/artifact.json
```

On Windows, use PowerShell 7.4 or later for byte-preserving compiler output:

```powershell
New-Item -ItemType Directory -Force .tools/examples | Out-Null
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl compiler-java,examples/java-host -am "-DskipTests" package
if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar conformance/generic-hooks/effect4/source.yaml conformance/generic-hooks/effect4/dcm.json --profile=keir-effect/4 > .tools/examples/follow-up.json
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
if ((Get-FileHash .tools/examples/follow-up.json).Hash -ne (Get-FileHash conformance/generic-hooks/effect4/artifact.json).Hash) {
    throw 'Compiled output differs from the checked-in artifact'
}
```

## Compile the earlier read-only example

Build the compiler using the setup in [Compile the follow-up example](#compile-the-follow-up-example). From the repository root, use PowerShell 7.4 or later:

```powershell
New-Item -Force -ItemType Directory .tools/manual-authoring | Out-Null
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar `
  conformance/read-simulation/task-advice/source.yaml `
  conformance/read-simulation/task-advice/dcm.json `
  --profile=keir-read/2 > .tools/manual-authoring/artifact.json
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
$compiled = Get-FileHash .tools/manual-authoring/artifact.json -Algorithm SHA256
$expected = Get-FileHash conformance/read-simulation/task-advice/artifact.json -Algorithm SHA256
if ($compiled.Hash -ne $expected.Hash) { throw 'Compiled bytes differ from the checked-in artifact' }
```

On POSIX, use the same CLI arguments and redirect stdout. Check the compiler exit status before comparing the output with `cmp`.

## Pinned companion and runtime package files

The first authoring companion reads [guide v1](../agents/customer-authoring-guide-v1.md) and the other committed inputs at the source revision pinned in its manifest. [Guide v2](../agents/customer-authoring-guide-v2.md) is a separate source for a future archive. Packaging it requires updating the source pin and verifying a reproducible archive with a new digest.

The [Node.js runtime package README](../../runtime-node/README.md) ships inside the runtime tarball. Its headings, profile labels, examples and API descriptions describe the pinned release bytes. Changes to that README require repacking and verifying the example host's archive integrity pin. Public examples use the maintained [Node host guide](../../examples/node-host/README.md).

The [single-write schema index](../../schemas/effect-4/README.md) and [read-only schema index](../../schemas/read-2/README.md) list the literal schema IDs used by validators. Fixture paths, archive names and existing Markdown anchors keep their exact spelling so commands and links continue to resolve.

## Earlier contracts and examples

See the [compatibility reference](compatibility-reference.md) for the original read-only and simulated-write contracts, the catalog-bound extensions, and the earlier qualification protocol.

<a id="original-compiler-examples"></a>
## Build the original read and simulated-write artifacts

## Implementation reference for earlier examples

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

<a id="compile-original-simulation"></a>
## Reproduce the original write simulation artifact


After building the [compiler](../../compiler-java/README.md), run from the repository root:

```sh
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar conformance/single-effect/source.yaml conformance/single-effect/dcm.json --profile=keir-effect/1
```

<a id="check-one-single-write-document"></a>
## Validate one single-write document

Select the profile explicitly to check one decoded document.

```sh
python scripts/check-read2-contracts.py --profile keir-effect/4 source conformance/effect4-structural/source.json
```
