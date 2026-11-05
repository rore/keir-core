<a id="host-examples"></a>
<a id="run-the-java-and-nodejs-host-examples"></a>
# Keir examples

The follow-up extension asks a task app to create a follow-up when someone completes a task in the launch project. The Java and Node.js hosts run the same compiled extension with simulated task services.

## Run both hosts

- [Run in Node.js](../docs/quickstart.md#run-the-precompiled-example)
- [Run in Java](../docs/quickstart.md#run-in-java)

Each host prints three outcomes. A matching completion creates a follow-up, a completion in another project skips the write, and a lost response leaves the write unconfirmed.

## Compile the rule

Read the [model](node-host/follow-up/dcm.json) and [extension source](node-host/follow-up/source.yaml), then [change the follow-up title and compile](../docs/quickstart.md#change-the-extension-and-compile-it).

## Read the host code

- [Node.js host](node-host/follow-up/run.mjs)
- [Java host](node-host/follow-up/FollowUp.java)

See [Embed Keir](../docs/embedding.md) to adapt the example to your application. The [shared conformance corpus](../conformance/keir-1/README.md) describes the fixtures that verify the compiler and runtimes.
