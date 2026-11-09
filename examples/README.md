<a id="host-examples"></a>
<a id="run-the-java-and-nodejs-host-examples"></a>
# Keir examples

The Java and Node.js hosts run the same compiled bytes with simulated services. The current examples use [Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0) with source `/2` and model/artifact `/3`.

| Example | What it demonstrates |
| --- | --- |
| [Follow-up](node-host/follow-up/source.yaml) | After a task completion commits, read the task and request at most one follow-up write. |
| [Pre-operation decision](node-host/pre-operation/source.yaml) | Read policy before a native operation, return ALLOW or DENY, and block continuation on evaluation failure. |

## Run both hosts

- [Run in Node.js](../docs/quickstart.md#run-the-precompiled-example)
- [Run in Java](../docs/quickstart.md#run-in-java)

Each follow-up host prints three outcomes. A matching completion creates a follow-up, a completion in another project skips the write, and a lost response leaves the write unconfirmed.

[Run the decision in both languages](../docs/quickstart.md#try-a-pre-operation-decision) to see ALLOWED, DENIED and BLOCKED with separate simulated native-attempt counts. The [walkthrough](../docs/decision-walkthrough.md) explains the host's own commit responsibilities.

## Compile the rule

Read the [model](node-host/follow-up/dcm.json) and [extension source](node-host/follow-up/source.yaml), then [change the follow-up title and compile](../docs/quickstart.md#change-the-extension-and-compile-it).

## Read the host code

- [Node.js host](node-host/follow-up/run.mjs)
- [Java host](node-host/follow-up/FollowUp.java)
- [Node decision host](node-host/pre-operation/run.mjs)
- [Java decision host](node-host/pre-operation/BeforeOperation.java)

See [Embed Keir](../docs/embedding.md) to adapt the examples to your application. The [current shared corpus](../conformance/keir-3/) records pinned artifacts and scenario expectations; the [conformance index](../conformance/README.md) separates current and historical inputs.
