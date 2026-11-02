# Agents using Keir

Keir has two product-facing agent personas. Both are host integrations over Keir's contracts; core remains usable without an agent.

| Persona | User and task | Guide |
| --- | --- | --- |
| Host integration assistant | Helps the host developer define the domain, DCM, hooks, capabilities, adapters and contracts | [Host integration](host-integration-assistant.md) |
| Customer extension assistant | Helps a customer describe a customization, author an extension/scenarios and review simulation before requesting activation | [Extension authoring](extension-assistant.md) |

**Status:** initial guidance for proposed workflows, not installed tools or a complete SDK. Planned delivery includes a host-design skill used by a developer's coding agent and the demo's in-app customer extension-authoring UI. These are two personas and execution contexts; the host-design skill does not require a second in-app agent.

Both guides distinguish legacy `/1` contracts from the implemented experimental [generic-hook `/2` profiles](../specs/generic-hook-capability-contract.md). Local compiler, Java/Node and packaged-host checks pass; effects remain simulation-only. Generic hooks do not require a sample program's call list. Optional policy, aggregate budgets and current authorization remain separate; passing simulation does not certify real adapters or production readiness.

For offline read/2 checks, use the shared [simulation operator guide](read-simulation.md) and its exact-artifact examples. Scenarios supply data and independent expectations; authority and consumer behavior remain host-owned.

Each guide explains the system, task, inputs, allowed actions and approvals. Both reference one [specification set](../specs/README.md); prompts must not contain a competing language/DCM specification. As APIs are implemented, provide structured schemas, scoped discovery, diagnostics and valid/invalid examples from the same contracts.

These personas are separate from [our engineering team roles](../../roles/README.md). Host-specific tools, credentials and policy belong to the host. Discovery, compilation, simulation and activation requests are authoring operations; they are not automatically runtime capabilities callable by a customer extension.
