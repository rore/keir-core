# Agents using Keir

Keir has two product-facing agent personas. Both are host integrations over Keir's contracts; core remains usable without an agent.

| Persona | User and task | Guide |
| --- | --- | --- |
| Host integration assistant | Helps the host developer define the domain, DCM, hooks, capabilities, adapters and contracts | [Host integration](host-integration-assistant.md) |
| Customer extension assistant | Helps a customer describe a customization, author an extension/scenarios and review simulation before requesting activation | [Extension authoring](extension-assistant.md) |

**Status:** initial guidance for proposed workflows, not installed tools or a complete SDK. The corrected host flow uses explicit `/4` host-bound single-write semantics; `/1`–`/3` remain legacy contracts. Planned delivery includes a host-design skill used by a developer's coding agent and the demo's in-app customer extension-authoring UI. These are two personas and execution contexts; the host-design skill does not require a second in-app agent.

Both guides distinguish the explicit `/4` host-bound contract from legacy `/1`–`/3` behavior in the [generic-hook specification](../specs/generic-hook-capability-contract.md). Do not describe `/4` as verified until implementation evidence is confirmed. Generic hooks do not require a sample program's call list. Host implementations enforce current permission; simulation does not certify real adapters or production readiness.

For offline read/2 checks, use the shared [simulation operator guide](read-simulation.md) and its exact-artifact examples. Scenarios supply data and independent expectations; authority and consumer behavior remain host-owned.

Each guide explains the system, task, inputs, allowed actions and approvals. Both reference one [specification set](../specs/README.md); prompts must not contain a competing language/DCM specification. As APIs are implemented, provide structured schemas, scoped discovery, diagnostics and valid/invalid examples from the same contracts.

These personas are separate from [our engineering team roles](../../roles/README.md). Host-specific tools, credentials and policy belong to the host. Discovery, compilation, simulation and activation requests are authoring operations; they are not automatically runtime capabilities callable by a customer extension.
