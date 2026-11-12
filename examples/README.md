<a id="host-examples"></a>
<a id="run-the-java-and-nodejs-host-examples"></a>
# Keir examples

The Java and Node.js hosts run the same compiled bytes with simulated services. The checked-in examples use source `/2` and model/artifact `/3`; build their packages from the same checkout.

| Example | What it demonstrates |
| --- | --- |
| [Follow-up](node-host/follow-up/source.yaml) | After a task completion commits, read the task and request at most one follow-up write. |
| [Pre-operation decision](node-host/pre-operation/source.yaml) | Read policy before a native operation, return ALLOW or DENY, and block continuation on evaluation failure. |

## Run both hosts

- [Run in Node.js](../docs/quickstart.md#run-the-precompiled-example)
- [Run in Java](../docs/quickstart.md#run-in-java)

Each follow-up host prints three outcomes. A matching completion creates a follow-up, a completion in another project skips the write, and a lost response leaves the write unconfirmed.

[Run the decision in both languages](../docs/decision-walkthrough.md#run-the-example) to see ALLOWED, DENIED and BLOCKED with separate simulated native-attempt counts. The [walkthrough](../docs/decision-walkthrough.md) explains the host's own commit responsibilities.

## Compile the rule

Read the [model](node-host/follow-up/dcm.json) and [extension source](node-host/follow-up/source.yaml), then [change the follow-up title and compile](../docs/quickstart.md#change-the-extension-and-compile-it).

<a id="guarded-result-scalar-dataflow"></a>
## Guarded-result scalar dataflow (development preview)

The [source](../conformance/keir-3/source-guarded-result.json), [model](../conformance/keir-3/dcm-guarded-result.json) and [compiled artifact](../conformance/keir-3/artifact-guarded-result.json) show a conditional create returning the adapter's actual validated reference, or the input reference when skipped. The expression must prove the call's exact Boolean BIND guard; optional scalar fields need a separate presence proof. Failed or uncertain calls never select a successful fallback.

The [shared corpus guide](../conformance/keir-3/README.md) describes the matching guarded READ decision, later-handler and unused-result examples, their independent scenarios, and existing Java/Node execution commands. These domain-neutral fixtures do not run in the task-specific follow-up host. Use the [release checklist](../docs/releasing.md) for the separate shipped-package checks; source and local-package checks do not establish released-package qualification or Daymark adoption.

The matching [edition-8 source guide](../docs/agents/customer-authoring-guide-v8.md) and [companion recipe](../docs/agents/authoring-companion.md#edition-8) require explicit host support for producer `keir-compiler-java/0.4.0`. The [companion index](../docs/agents/authoring-companion.md#edition-8) records the published archive and its unchanged preparation-time metadata. Earlier installed consumers keep their supported versions and companion editions.

## Read the host code

- [Node.js host](node-host/follow-up/run.mjs)
- [Java host](node-host/follow-up/FollowUp.java)
- [Node decision host](node-host/pre-operation/run.mjs)
- [Java decision host](node-host/pre-operation/BeforeOperation.java)

See [Embed Keir](../docs/embedding.md) to adapt the examples to your application. The [current shared corpus](../conformance/keir-3/) records pinned artifacts and scenario expectations; the [conformance index](../conformance/README.md) separates current and historical inputs.
