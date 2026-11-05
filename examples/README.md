<a id="host-examples"></a>
# Run the Java and Node.js host examples

Run the same compiled follow-up rule in Java and Node.js. The example hosts use simulated task services and compare their results with checked-in expectations.

Use **JDK 21** and **Node.js 24 with npm**. Run the commands from the repository root.

## Compile the rule

The [Rule source](../conformance/generic-hooks/effect4/source.yaml) describes when to request a follow-up. The [DCM](../conformance/generic-hooks/effect4/dcm.json) defines the operations it can use. To compile the source yourself and check the result, follow the [compiler commands](../docs/internal/implementation-reference.md#compile-the-follow-up-example). Both hosts use the same pinned, precompiled artifact.

## Run both hosts

Follow the [quickstart](../docs/quickstart.md#run-the-example) to package the Node.js runtime and run the Node.js host. To build the Java example, run:

```sh
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl examples/java-host -am -DskipTests package
```

On Windows, set `$env:MAVEN_USER_HOME = "$PWD/.tools/maven"` and use `.\mvnw.cmd` with the same arguments. Then run:

```sh
java -cp examples/java-host/target/keir-java-host-0.1.0-SNAPSHOT.jar dev.keir.examples.GenericHost conformance/generic-hooks/effect4
```

Both hosts print the same 11 cases as JSON. In the matching case the simulated write is confirmed. A completion in another project skips the write. The lost-response case reports uncertainty.

## Read the host code

- **Java** uses `boundEffectBatch` in [GenericSimulation.java](java-host/src/main/java/dev/keir/examples/GenericSimulation.java). See [Java host commands](java-host/README.md).
- **Node.js** uses `runBoundEffectBatch` in [generic-simulation.mjs](node-host/generic-simulation.mjs). See [Node.js host commands](node-host/README.md).

For the steps to adapt this flow to your own application, see [Embed Keir](../docs/embedding.md). For the complete compiler and runtime checks, see [shared conformance](../conformance/README.md).
