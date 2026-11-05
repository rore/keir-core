<a id="run-your-first-keir-extension"></a>
<a id="try-a-follow-up-rule"></a>
# Getting started with Keir

Suppose a team asks for a follow-up task whenever someone completes a task in the launch project. The app already knows when a task is completed and how to read and create tasks. The extension combines those operations for this team.

The app’s [domain capability model](../conformance/generic-hooks/effect4/dcm.json) exposes a `TaskCompleted` event with a task ID. It also exposes `tasks.get` and `tasks.create`. The [extension source](../conformance/generic-hooks/effect4/source.yaml) sets the team’s project and title, reads the completed task, and calls create only when the project and status match.

```yaml
- id: created
  call: tasks.create
  when: eligible
  args:
    projectId: {cel: eligibleProject}
    title: {literal: 'Follow up'}
```

The example represents the launch project as `project1`, a typed value in the extension. The guard, `when: eligible`, uses the result of an earlier Boolean expression. The title is also part of the extension, so changing it requires a new compilation and review while the same model and host implementations can be reused.

Use a local checkout, **Node.js 24 with npm**, and **JDK 21** to compile. The precompiled run only needs Node.js and npm. The first build and install need access to the Maven and npm dependencies unless they are already cached.

<a id="compile-and-run"></a>
<a id="run-the-example"></a>
<a id="run-your-first-keir-rule"></a>
## Run the precompiled example

From the repository root, package the Node runtime and install it in the example app:

```sh
mkdir -p .tools/packages examples/node-host/.tools
npm pack ./runtime-node --pack-destination .tools/packages --cache .tools/npm-cache --ignore-scripts
npm ci --prefix examples/node-host --cache .tools/npm-cache --ignore-scripts
```

In PowerShell, use `New-Item -ItemType Directory -Force .tools/packages, examples/node-host/.tools | Out-Null` for the first command and `npm.cmd` for the npm commands.

From the repository root, create `examples/node-host/.tools/follow-up.mjs` using the [complete simulated host](embedding.md#example-node-host). Run it with the precompiled artifact:

```sh
node examples/node-host/.tools/follow-up.mjs conformance/generic-hooks/effect4/artifact.json
```

The first completion requests a follow-up. The second is in another project and skips the write. The third requests a write, but the simulated service cannot confirm whether it happened.

```text
task1 CREATED CONFIRMED "Follow up"
task2 NO_OP -
task3 UNCERTAIN UNKNOWN
```

## Change the extension and compile it

Copy the source into `.tools` and change the title literal from `Follow up` to `Review launch task`. Keep the DCM, guard, and host unchanged.

```sh
mkdir -p .tools/follow-up
cp conformance/generic-hooks/effect4/source.yaml .tools/follow-up/source.yaml
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl compiler-java -am -DskipTests package
```

After editing the copied source, compile it against the same model and run the new artifact:

```sh
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar \
  .tools/follow-up/source.yaml conformance/generic-hooks/effect4/dcm.json \
  --profile=keir-effect/4 > .tools/follow-up/artifact.json
node examples/node-host/.tools/follow-up.mjs .tools/follow-up/artifact.json
```

For PowerShell 7.4 or later, use the following commands. Edit the copied source after `Copy-Item` and before running the compiler.

```powershell
New-Item -ItemType Directory -Force .tools/follow-up | Out-Null
Copy-Item conformance/generic-hooks/effect4/source.yaml .tools/follow-up/source.yaml
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl compiler-java -am "-DskipTests" package
if ($LASTEXITCODE -ne 0) { throw 'Compiler build failed' }
java -jar compiler-java/target/keir-compiler-java-0.1.0-SNAPSHOT-cli.jar .tools/follow-up/source.yaml conformance/generic-hooks/effect4/dcm.json '--profile=keir-effect/4' > .tools/follow-up/artifact.json
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
node examples/node-host/.tools/follow-up.mjs .tools/follow-up/artifact.json
```

```text
task1 CREATED CONFIRMED "Review launch task"
task2 NO_OP -
task3 UNCERTAIN UNKNOWN
```

The simulation checks the extension’s behavior with sample data. In an application, the host selects the exact approved compiled bytes and supplies implementations that enforce its real permissions and business rules.

## Run the conformance cases

To inspect the full 11-case JSON report used by the Java and Node.js conformance examples, run:

```sh
node examples/node-host/generic.mjs conformance/generic-hooks/effect4
```

<a id="embed-the-same-flow"></a>
[Embed Keir in an application](embedding.md) · [Keir specification](specs/README.md) · [Run the Java and Node.js conformance examples](../examples/README.md)
