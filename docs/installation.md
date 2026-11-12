# Install the released compiler and runtimes

Keir packages are distributed through [GitHub Releases](https://github.com/rore/keir-core/releases). Choose and pin a release for your application. Its notes identify prerequisites, supported contracts, assets and any matching authoring companion. Keir packages are downloaded from GitHub; their third-party dependencies come from the normal npm and Maven repositories.

Install the runtime your application uses and the compiler if you need to compile extensions. The authoring companion is optional; it is not needed to install a runtime.

## Choose a release

Read the selected release's notes before installing. Use its required Java, Node.js and build tools, and check the [contract matrix](specs/compiler-artifact.md#source-and-artifact-identity) against your application's model. Record the release tag, exact asset names and checksums with your dependency configuration. Update these pins deliberately when adopting another release.

A published capability does not upgrade an installed host. Existing artifact/model approvals stay tied to their bytes; adopting packages does not automatically recompile, migrate or approve extensions.

The PowerShell examples below use one explicit release throughout. Start in an empty consumer directory outside the Keir source checkout.

## Download and verify

Enter the version from the release tag, without its leading `v`. The asset names follow the published package convention.

```powershell
$ErrorActionPreference = 'Stop'
$version = Read-Host 'Selected Core release version (without v)'
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Use an explicit release version' }
$release = "https://github.com/rore/keir-core/releases/download/v$version"
$assets = @(
  "keir-compiler-cli-$version.zip"
  "keir-runtime-node-$version.tgz"
  "keir-java-runtime-maven-$version.zip"
)
Invoke-WebRequest "$release/SHA256SUMS" -OutFile SHA256SUMS
$pins = @{}
foreach ($line in Get-Content SHA256SUMS) {
  if ($line -notmatch '^([0-9a-f]{64})  ([^/\\]+)$') { throw 'Invalid checksum record' }
  $digest, $name = $Matches[1], $Matches[2]
  if ($pins.ContainsKey($name)) { throw "Duplicate checksum: $name" }
  $pins[$name] = $digest
}
foreach ($name in $assets) {
  if (-not $pins.ContainsKey($name)) { throw "Missing checksum: $name" }
  Invoke-WebRequest "$release/$name" -OutFile $name
  if ((Get-FileHash $name -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pins[$name]) {
    throw "Checksum mismatch: $name"
  }
}
```

This downloads both runtimes and the compiler. Omit unneeded entries from `$assets` when installing only one component. Obtain `SHA256SUMS` from the selected release you trust, retain it with the archives, and verify before extracting or loading packages. Keep the included licenses and third-party notices when redistributing.

## Use the compiler

With the Java prerequisite from the release notes available:

```powershell
Expand-Archive "keir-compiler-cli-$version.zip" compiler
java -jar "./compiler/keir-compiler-java-$version-cli.jar" source.yaml dcm.json
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
```

Supply your extension source and the application's trusted model. The compiler writes artifact bytes to stdout, or diagnostics to stderr with a nonzero exit code. Preserve stdout as bytes when saving an artifact; the [versioned runnable example](examples/installation-core-0.3.0.md#compile-the-bundled-rule) demonstrates this in PowerShell.

<a id="install-and-run-in-nodejs"></a>
## Install the Node.js runtime

From your consumer application directory, with the verified tarball present:

```powershell
npm install --cache .npm-cache --no-audit --no-fund "./keir-runtime-node-$version.tgz"
if ($LASTEXITCODE -ne 0) { throw 'npm installation failed' }
```

npm records the local Keir package and resolves its third-party dependencies into the application's lockfile. Keep the verified archive at the recorded path for reproducible installs. Import `bindModel`, `prepareProgram` and `invokeProgram` from `@keir/runtime-node`; [embedding](embedding.md) explains the host implementations and trusted context they require.

<a id="configure-and-run-in-java"></a>
## Configure the Java runtime

The Java ZIP contains a Maven repository with the runtime JAR and its POMs. Extract it into your consumer project directory:

```powershell
Expand-Archive "keir-java-runtime-maven-$version.zip" java-runtime
```

Add the repository and dependency to your application's POM. Replace `SELECTED_VERSION` with the same release version chosen above:

```xml
<repositories>
  <repository>
    <id>keir-release</id>
    <url>file://${project.basedir}/java-runtime/maven</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>dev.keir</groupId>
    <artifactId>keir-runtime-java</artifactId>
    <version>SELECTED_VERSION</version>
  </dependency>
</dependencies>
```

Build the application with Maven as usual. Maven obtains Keir from the extracted repository and resolves third-party dependencies from their normal repositories. Retain the extracted repository with your build inputs. The runtime JAR alone does not include every dependency.

Maven may report missing individual checksum sidecars for the extracted Keir files; verify the containing ZIP against the release's published checksum before extraction. See the [Java consumer example](examples/installation-core-0.3.0.md#configure-and-run-in-java) for a complete POM, classpath and host.

## Optional authoring companion

For an authoring assistant, select the companion matching the host's packages and model using the [companion compatibility table](agents/authoring-companion.md#choose-an-edition). Download that edition's archive and its separately published checksum from its release record. The Core `SHA256SUMS` file covers Core packages; do not assume it includes the companion.

Supply your application's model and authorized tools separately. The companion provides authoring guidance, schemas and examples, rather than runtime installation or host authority.

<a id="verification-boundary"></a>
## Verified example and next steps

The [versioned installation example](examples/installation-core-0.3.0.md) records an actual outside-repository installation and compiler, Node and Java checks, including its exact hashes, tools and cache limitations. That evidence applies to those package bytes; it does not qualify every future release.

This page describes the installation process. Check each release's notes for its supported package layout, prerequisites and compatibility. Continue with [embedding](embedding.md) for your application, or the [source-checkout quickstart](quickstart.md) to build and change the repository examples.
