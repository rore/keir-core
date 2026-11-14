# Keir specification

Use these references when implementing or checking Keir behavior. For a runnable introduction, start with the [quickstart](../quickstart.md).

<a id="current-profile"></a>
<a id="current-behavior"></a>
<a id="current-contract"></a>
## Find a contract

| Question | Reference |
| --- | --- |
| What can an extension say? | [Source language and data flow](language.md) |
| What data, operations and hooks can an application expose? | [Application model](domain-model.md) |
| How do values, expressions, lists and time behave? | [Expression rules](workflow-expressions.md) |
| What does the compiler produce, and which versions work together? | [Artifacts and compatibility](compiler-artifact.md) |
| How does an application bind operations and invoke an extension? | [Host API](host-model-binding.md) |
| What happens during execution, failure or timeout? | [Runtime contract](runtime.md) |
| How can a rule check an operation before it is saved? | [Pre-operation decisions](pre-operation-decisions.md) |
| How are independent simulation cases checked? | [Simulation protocol](single-effect.md#ordered-simulation-protocol-and-acceptance) |

## Compatibility

Use the [exact profile matrix](compiler-artifact.md#source-and-artifact-identity) and the host's installed package pins. A schema describes document shape; it does not establish type correctness, permission or compatibility. Existing approved artifacts retain their exact model and bytes when packages are upgraded.

## Numeric qualification status

<a id="descriptor-and-bounded-runtime-evidence"></a>
Numeric support is published in [Core 0.5.0](../releases/core-0.5.0.md). The [accepted feature record](../../roadmap/features/numeric-values-and-rules.md#manager-accepted-core-contribution) owns final Core and Daymark results; [descriptor comparisons and bounded runtime measurements](../../roadmap/features/numeric-values-and-rules.md#exact-descriptors-and-actual-runtime-measurements) retain their original scope. The [companion catalog](../agents/authoring-companion.md) identifies supported packets and public availability. Publication does not upgrade a host or transfer approval.

<a id="maintaining-a-contract"></a>
For contribution and verification requirements, see [Contributing](../../CONTRIBUTING.md). Earlier contracts and design rationale are in [documentation history](../internal/history/README.md).
