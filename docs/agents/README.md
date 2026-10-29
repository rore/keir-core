# Agents using Keir

Keir has two product-facing agent personas. Both are host integrations over Keir's contracts; core remains usable without an agent.

| Persona | User and task | Guide |
| --- | --- | --- |
| Host integration assistant | Helps the host developer define the domain, DCM, hooks, capabilities, adapters and contracts | [Host integration](host-integration-assistant.md) |
| Customer extension assistant | Helps a customer describe a customization, author an extension/scenarios and review simulation before requesting activation | [Extension authoring](extension-assistant.md) |

**Status:** initial guidance for proposed workflows, not installed tools or a complete SDK. Host integrations and optional skill packaging are future work.

Each guide explains the system, task, inputs, allowed actions and approvals. Both reference one [specification set](../specs/README.md); prompts must not contain a competing language/DCM specification. As APIs are implemented, provide structured schemas, scoped discovery, diagnostics and valid/invalid examples from the same contracts.

These personas are separate from [our engineering team roles](../../roles/README.md). Host-specific tools, credentials and policy belong to the host. Discovery, compilation, simulation and activation requests are authoring operations; they are not automatically runtime capabilities callable by a customer extension.
