<a id="run-your-first-keir-extension"></a>
<a id="try-a-follow-up-rule"></a>
# Getting started with Keir

Run an extension that creates a follow-up whenever someone completes a task in the launch project. Then change the follow-up title and compile your version.

The app sends the completed task’s ID. The extension reads that task, checks that it is complete and belongs to `project1`, then requests a follow-up titled `Follow up`. The model defines the event and task operations. The host supplies the authenticated actor separately. [Extension source](../examples/node-host/follow-up/source.yaml) · [Readable model](../examples/node-host/follow-up/dcm.json)

The examples in this checkout target the Core 0.3.0 candidate: source `keir/2` with model and artifact `/3`. Use the [format matrix](specs/compiler-artifact.md#source-and-artifact-identity) to select matching packages.

Use a local checkout, **Node.js 24 with npm**, and **JDK 21** to compile. The precompiled run only needs Node.js and npm. The first build and install need access to the Maven and npm dependencies unless they are already cached.

<a id="compile-and-run"></a>
<a id="run-the-example"></a>
<a id="run-your-first-keir-rule"></a>
## Run the precompiled example

From the repository root, package the Node runtime and install it in the example app.

```sh
mkdir -p .tools/packages
npm pack ./runtime-node --pack-destination .tools/packages --cache .tools/npm-cache --ignore-scripts
npm ci --prefix examples/node-host --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
```

In PowerShell, use `New-Item -ItemType Directory -Force .tools/packages | Out-Null` for the first command and `npm.cmd` for the npm commands.

Run the [Node example host](../examples/node-host/follow-up/run.mjs) with the checked-in compiled extension.

```sh
node examples/node-host/follow-up/run.mjs
```

The first completion requests a follow-up. The second is in another project and skips the write. The third requests a write, but the simulated service cannot confirm whether it happened.

```text
task1 CREATED CONFIRMED | Follow up
task2 NO_OP -
task3 UNCERTAIN UNKNOWN
```

## Change the extension and compile it

Copy the source into `.tools` and change the title literal from `Follow up` to `Review launch task`.

```sh
mkdir -p .tools/follow-up
cp examples/node-host/follow-up/source.yaml .tools/follow-up/source.yaml
```

After editing the copy, build the compiler and run the new artifact. Each command runs only if the previous one succeeds. The compiler prints diagnostics on failure.

```sh
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl compiler-java -am -DskipTests package &&
  java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar \
    .tools/follow-up/source.yaml examples/node-host/follow-up/dcm.json > .tools/follow-up/artifact.json &&
  node examples/node-host/follow-up/run.mjs .tools/follow-up/artifact.json
```

For PowerShell 7.4 or later, use the following commands. Edit the copied source after `Copy-Item` and before running the compiler.

```powershell
New-Item -ItemType Directory -Force .tools/follow-up | Out-Null
Copy-Item examples/node-host/follow-up/source.yaml .tools/follow-up/source.yaml
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl compiler-java -am "-DskipTests" package
if ($LASTEXITCODE -ne 0) { throw 'Compiler build failed' }
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar .tools/follow-up/source.yaml examples/node-host/follow-up/dcm.json > .tools/follow-up/artifact.json
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
node examples/node-host/follow-up/run.mjs .tools/follow-up/artifact.json
```

```text
task1 CREATED CONFIRMED | Review launch task
task2 NO_OP -
task3 UNCERTAIN UNKNOWN
```

Both hosts use simulated services. By default they verify the pinned artifact against its checked-in digest. When you pass a path, choose the artifact you just compiled. In an application, the host independently verifies the approved bytes and supplies implementations that enforce its permissions and business rules.

## Run in Java

Use JDK 21. Build the runtime JAR and copy its runtime dependencies from the repository root.

```sh
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl runtime-java -am -Dmaven.test.skip=true package org.apache.maven.plugins:maven-dependency-plugin:3.11.0:copy-dependencies -DincludeScope=runtime
java -cp 'runtime-java/target/keir-runtime-java-0.1.0-SNAPSHOT.jar:runtime-java/target/dependency/*' examples/node-host/follow-up/FollowUp.java
```

Use these commands in PowerShell.

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl runtime-java -am "-Dmaven.test.skip=true" package org.apache.maven.plugins:maven-dependency-plugin:3.11.0:copy-dependencies "-DincludeScope=runtime"
if ($LASTEXITCODE -ne 0) { throw 'Runtime build failed' }
java -cp 'runtime-java/target/keir-runtime-java-0.1.0-SNAPSHOT.jar;runtime-java/target/dependency/*' examples/node-host/follow-up/FollowUp.java
```

The [Java host](../examples/node-host/follow-up/FollowUp.java) prints the same three lines as Node. Append `.tools/follow-up/artifact.json` to run the changed extension. Both examples stop on a binding or preparation diagnostic or an unexpected invocation outcome.

<a id="run-the-conformance-cases"></a>
The [current shared fixtures](../conformance/keir-3/) record exact artifacts and independent scenario expectations for conformance.

## Try a pre-operation decision

The second example decides whether the application may continue an operation. It reads the host's policy and returns ALLOW or DENY. A failed read blocks continuation. Open the [source](../examples/node-host/pre-operation/source.yaml), [model](../examples/node-host/pre-operation/dcm.json), [Node host](../examples/node-host/pre-operation/run.mjs) or [Java host](../examples/node-host/pre-operation/BeforeOperation.java).

After the Node setup above, run:

```sh
node examples/node-host/pre-operation/run.mjs
```

Or use the Java runtime and dependencies built above:

```sh
java -cp 'runtime-java/target/keir-runtime-java-0.1.0-SNAPSHOT.jar:runtime-java/target/dependency/*' examples/node-host/pre-operation/BeforeOperation.java
```

In PowerShell, use `;` between the JAR and dependency directory in that classpath.

Both hosts print:

```text
allow ALLOWED | native attempts=1
deny DENIED | native attempts=0 | Operation restricted.
unavailable BLOCKED | native attempts=0
```

This simulation counts native continuation separately from the decision. A real application rechecks permissions and consistency before using its own commit path. To edit and compile this extension, use its `source.yaml` and `dcm.json` in the compiler command above, then pass the resulting artifact path to either host. [Decision walkthrough](decision-walkthrough.md)

<a id="embed-the-same-flow"></a>
[Embed Keir in an application](embedding.md) · [Keir 2 workflow expressions](specs/workflow-expressions.md) · [Examples index](../examples/README.md)
