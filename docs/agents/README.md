# Agents using Keir

Keir has two product-facing agent personas. Both are host integrations over Keir's contracts; core remains usable without an agent.

| Persona | User and task | Guide |
| --- | --- | --- |
| Host integration assistant | Helps the host developer define the domain, DCM, hooks, capabilities, adapters and contracts | [Host integration](host-integration-assistant.md) |
| Customer extension assistant | Helps a customer describe a customization, author an extension/scenarios and review simulation before requesting activation | [Extension authoring](extension-assistant.md) |

For instructions to supply to a customer-facing model, use the [Keir customer authoring guide v1](customer-authoring-guide-v1.md). Pair it with the host's exact published DCM and scoped tools. The [release plan](../../roadmap/features/public-package-release.md) tracks packaging the guide with compatible schemas and examples.

The current [specification](../specs/README.md) defines the `effect/4` host-bound contract and earlier profiles. The host owns permissions and native effects. See [verification](../testing.md#effect4-verification) for the tested scope.

For offline read/2 checks, use the shared [simulation operator guide](read-simulation.md) and its exact-artifact examples. Scenarios supply data and independent expectations; authority and consumer behavior remain host-owned.

Each guide explains the system, task, inputs, allowed actions and approvals. Both reference one [specification set](../specs/README.md); prompts must not contain a competing language/DCM specification. As APIs are implemented, provide structured schemas, scoped discovery, diagnostics and valid/invalid examples from the same contracts.

These personas are separate from [our engineering team roles](../../roles/README.md). Host-specific tools, credentials and policy belong to the host. Discovery, compilation, simulation and activation requests are authoring operations; they are not automatically runtime capabilities callable by a customer extension.
