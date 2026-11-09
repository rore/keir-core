# Install the released compiler and runtimes

Use this path to consume **Core 0.3.0 without cloning Keir**. [GitHub Releases](https://github.com/rore/keir-core/releases/tag/v0.3.0) distributes the compiler ZIP, Node.js tarball and Java Maven repository ZIP. The Keir packages are not installed from npm or Maven Central. Their third-party dependencies still come from the normal npm and Maven repositories.

This walkthrough uses PowerShell 7, JDK 21, Node.js 24 with npm, and Maven. Have those tools available before starting. Run in a new disposable PowerShell process and an empty directory outside your application or the Keir repositories. Everything installed below stays in that directory, including npm and Maven caches. A fresh installation needs access to GitHub and the dependency registries.

## Download and verify

The fourth archive is the released authoring companion. It supplies the exact model, source and expected artifact for this small simulation. The literal SHA-256 pins below identify the released bytes; verify them before extracting anything. For another release, obtain its pins from a trusted release record rather than reusing these values.

```powershell
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory keir-consumer | Out-Null
Set-Location keir-consumer
$release = 'https://github.com/rore/keir-core/releases/download/v0.3.0'
$pins = [ordered]@{
  'keir-compiler-cli-0.3.0.zip' = '596ef85cbec91d9e459fcd67aff83e05099df9b7610fd0fca643e573d27d090c'
  'keir-java-runtime-maven-0.3.0.zip' = 'dfcbc22ed7b902b8c9c708da85de371afaeb6c74aa72aecc827931182188ac04'
  'keir-runtime-node-0.3.0.tgz' = '40d71748bf142872c832e7d48aa6e8f827d0759c6ca5006c1abfd19db52935cc'
  'keir-authoring-v5.zip' = '20249307192494acb1cf07cc002e7203bda4f6baf8a8c2257dd752ce1d8cfe44'
}
foreach ($name in $pins.Keys) {
  Invoke-WebRequest "$release/$name" -OutFile $name
  if ((Get-FileHash $name -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pins[$name]) {
    throw "Checksum mismatch: $name"
  }
}
Expand-Archive keir-compiler-cli-0.3.0.zip compiler
Expand-Archive keir-java-runtime-maven-0.3.0.zip java-runtime
Expand-Archive keir-authoring-v5.zip companion
Copy-Item companion/conformance/keir-3/dcm-guard.json dcm.json
Copy-Item companion/conformance/keir-3/source-guard.json source.json
Copy-Item companion/conformance/keir-3/artifact-guard.sha256 artifact.sha256
```

Keep the archives' license and third-party notice files with redistributed packages. Companion edition 5 uses source `keir/2`, model `keir-dcm/3` and artifact `keir-artifact/3`, matching these packages.

## Compile the bundled rule

The CLI takes the source path followed by the model path. It writes artifact bytes to stdout, or diagnostics to stderr with a nonzero exit code. Node's process API below preserves stdout as bytes across PowerShell versions and stops on compiler failure.

```powershell
node -e "const fs=require('node:fs'); const cp=require('node:child_process'); fs.writeFileSync('artifact.json',cp.execFileSync('java',['-jar','compiler/keir-compiler-java-0.3.0-cli.jar','source.json','dcm.json']));"
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
$expected = (Get-Content -Raw artifact.sha256).Trim()
$actual = 'sha256:' + (Get-FileHash artifact.json -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) { throw 'Compiled artifact differs from the released example' }
```

The expected digest is `sha256:8f0d93951b2026ccd82aaaf3ade53fc77fd755a5ed61f2b69719f8b9b8a5d0a1`. This comparison applies to the unchanged bundled source and model. After editing a rule, compile it again, test it and establish a new trusted artifact pin through your application's review process.

The source already contains the bundled model's canonical fingerprint. A raw file hash is not a general model fingerprint: formatting can change raw bytes without changing the canonical model. See the [model identity rules](specs/domain-model.md) and [canonical digest rules](specs/compiler-artifact.md#canonical-bytes-and-digest-rules) before changing a model.

## Install and run in Node.js

Install the verified local tarball. npm resolves its declared third-party dependencies and records the installation in the consumer's lockfile; it does not fetch the Keir package from npm.

```powershell
'{"private":true,"type":"module"}' | Set-Content -Encoding utf8NoBOM package.json
npm install --cache .npm-cache --no-audit --no-fund ./keir-runtime-node-0.3.0.tgz
if ($LASTEXITCODE -ne 0) { throw 'npm installation failed' }
@'
import assert from 'node:assert/strict';
import { createHash, randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { bindModel, prepareProgram, invokeProgram } from '@keir/runtime-node';

const artifact = readFileSync('artifact.json');
assert.equal('sha256:' + createHash('sha256').update(artifact).digest('hex'),
  readFileSync('artifact.sha256', 'utf8').trim());
const registration = { version: '1', mode: 'simulation', keyType: 'sample.OperationKey/1' };
const binding = bindModel(readFileSync('dcm.json'), {
  'policy.get': { ...registration,
    invoke: () => ({ kind: 'SUCCESS', value: { restricted: false } }) },
  'native.write': { ...registration, invoke: () => { throw new Error('Unexpected write'); } }
});
assert.equal(binding.diagnostic, null);
const program = prepareProgram(artifact, binding.boundModel);
assert.equal(program.diagnostic, null);
const clock = { nowMs: () => Math.floor(performance.now()) };
for (const restricted of [false, true]) {
  const outcome = await invokeProgram(program.prepared, { restricted }, {
    bindings: { actorId: 'sample-user' },
    deadline: { clock, expiresAtMs: clock.nowMs() + 1000 },
    requestKey: randomUUID(), event: { id: randomUUID(), source: 'sample-operation' }
  });
  assert.equal(outcome.classification, 'COMPLETED');
  assert.deepEqual(outcome.effects, []);
  assert.equal(outcome.result.decision, restricted ? 'DENY' : 'ALLOW');
  console.log(`${restricted} ${outcome.result.decision}`);
}
'@ | Set-Content -Encoding utf8NoBOM run.mjs
node run.mjs
if ($LASTEXITCODE -ne 0) { throw 'Node host failed' }
```

Expected output:

```text
false ALLOW
true DENY
```

## Configure and run in Java

The Java ZIP contains a Maven repository with `dev.keir:keir-runtime-java:0.3.0`, its parent POM and runtime JAR. Point your consumer at the extracted repository. Maven resolves the runtime's transitive dependencies from their normal repositories; the ZIP is not a standalone JAR with every dependency bundled.

Maven may warn that the extracted Keir POMs and JAR have no individual checksum files. Their integrity is covered by the ZIP digest verified before extraction.

```powershell
@'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>example</groupId><artifactId>keir-consumer</artifactId><version>1</version>
  <repositories>
    <repository><id>keir-release</id><url>file://${project.basedir}/java-runtime/maven</url></repository>
  </repositories>
  <dependencies>
    <dependency><groupId>dev.keir</groupId><artifactId>keir-runtime-java</artifactId><version>0.3.0</version></dependency>
  </dependencies>
</project>
'@ | Set-Content -Encoding utf8NoBOM pom.xml
$env:MAVEN_USER_HOME = "$PWD/.maven"
mvn "-Dmaven.repo.local=$PWD/.maven/repository" org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath '-DincludeScope=runtime' '-Dmdep.outputFile=classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'Maven dependency resolution failed' }
@'
import dev.keir.runtime.KeirRuntime;
import dev.keir.runtime.KeirRuntime.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.LongSupplier;
import static java.util.concurrent.CompletableFuture.completedFuture;

class Consumer {
  static void check(boolean condition) {
    if (!condition) throw new IllegalStateException("Consumer check failed");
  }
  public static void main(String[] args) throws Exception {
    var artifact = Files.readAllBytes(Path.of("artifact.json"));
    check(("sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(artifact)))
      .equals(Files.readString(Path.of("artifact.sha256")).strip()));
    var read = new BoundEffectAdapter("1", "simulation", "sample.OperationKey/1",
      (business, protectedArgs, remainingMs, context) -> completedFuture(
        Map.of("kind", "SUCCESS", "value", Map.of("restricted", false))));
    var write = new BoundEffectAdapter("1", "simulation", "sample.OperationKey/1",
      (business, protectedArgs, remainingMs, context) -> { throw new IllegalStateException("Unexpected write"); });
    var binding = KeirRuntime.bindModel(Files.readAllBytes(Path.of("dcm.json")),
      Map.of("policy.get", read, "native.write", write));
    check(binding.succeeded());
    var program = KeirRuntime.prepareProgram(artifact, binding.boundModel());
    check(program.succeeded());
    long start = System.nanoTime();
    LongSupplier clock = () -> (System.nanoTime() - start) / 1_000_000;
    for (boolean restricted : new boolean[] {false, true}) {
      var invocation = new BoundEffectInvocation(Map.of("actorId", "sample-user"),
        new Deadline(clock, clock.getAsLong() + 1000), UUID.randomUUID(),
        Map.of("id", UUID.randomUUID().toString(), "source", "sample-operation"));
      var outcome = KeirRuntime.invokeBoundEffectProgram(program.prepared(),
        Map.of("restricted", restricted), invocation).toCompletableFuture().join();
      check(outcome.classification().equals("COMPLETED") && outcome.effects().isEmpty());
      check(outcome.result().get("decision").equals(restricted ? "DENY" : "ALLOW"));
      System.out.println(restricted + " " + outcome.result().get("decision"));
    }
  }
}
'@ | Set-Content -Encoding utf8NoBOM Consumer.java
java -cp (Get-Content -Raw classpath.txt).Trim() Consumer.java
if ($LASTEXITCODE -ne 0) { throw 'Java host failed' }
```

The Java output is the same two lines as Node. Both hosts verify the artifact before preparation and check the complete successful outcome and empty effects. They register every model operation in simulation mode, including the write operation that this decision cannot call. They do not perform a native operation. A real host must retain [selection, authorization and commit checks](embedding.md#before-a-native-operation); ALLOW alone is not authority to commit.

## Verification boundary

This path was checked with the released Core 0.3.0 and companion edition 5 bytes, JDK 21, Node.js 24 and Maven 3.10.0 in a consumer directory outside either source repository. Compilation matched the bundled artifact digest, and both installed runtimes passed the checks above. The check used isolated caches seeded from existing third-party dependency caches, with normal registry resolution for npm. Maven resolved Keir from the extracted repository and used cached third-party dependencies. It establishes consumer package/API compatibility, not a complete fresh empty-cache installation or anonymous GitHub access. The commands above use normal online dependency resolution for a new consumer.

Continue with [embedding](embedding.md) to connect your own operations, or the [repository quickstart](quickstart.md) to explore the larger examples.
