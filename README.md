# Keir Core

Keir enables SaaS applications to offer safe, AI-assisted customization to business users.

The hosting application defines extension points and exposes approved capabilities. Keir provides a constrained programming language, compiler, simulation and runtimes that let customers compose those capabilities into new application behavior without executing arbitrary generated code. A host-integrated AI agent helps business users express requirements, clarify intended behavior, generate extensions, test scenarios and review results before activation. Keir's core remains usable without an agent.

**Status: the corrected `/4` host-binding contract is implemented and locally verified.** Java, Node, source/installed packages and shared conformance agree; [testing evidence](docs/testing.md) records the measured scope. Independent result/simplicity review and manager integration remain pending. Native-provider qualification and hosted CI are not claimed. Existing `/1`–`/3` contracts retain their documented behavior; `/4` is a distinct host-bound single-write contract.

## Components

The corrected contract uses explicit `keir-effect/4`, `keir-dcm/effect-4`, `keir-artifact/effect-4` and `HOST_BOUND_SINGLE_WRITE` identities. The host supplies the complete DCM and compatible implementations at binding; preparation derives compatibility checks internally; invocation supplies fresh host context. `/1`–`/3` remain separate legacy behavior. See the [generic-hook contract](docs/specs/generic-hook-capability-contract.md) and [model binding](docs/specs/host-model-binding.md).

| Path | Responsibility |
| --- | --- |
| [compiler-java/](compiler-java/README.md) | Validate source IR against a trusted domain model and emit a portable artifact; library and CLI |
| [runtime-java/](runtime-java/README.md) | Embed artifact execution and simulation in Java hosts |
| [runtime-node/](runtime-node/README.md) | Embed artifact execution and simulation in Node hosts |
| [conformance/](conformance/README.md) | Shared fixtures and expected behavior for both runtimes |
| [examples/](examples/README.md) | Minimal Java and Node host integrations |
| [docs/](docs/README.md) | Public specifications and guides, with separate internal engineering records |

Compile once, execute the same artifact in either runtime. Each runtime is separately consumable and does not require the compiler for execution. Task-specific services, persistence, AI authoring and activation UX belong to the separate `keir-demo-app` repository.

## Start here

- [Guided tour: host design, customer extension design and runtime](docs/guided-tour.md)
- [Public documentation](docs/README.md)
- [Specification status and contracts](docs/specs/README.md)
- [Host and customer agent guides](docs/agents/README.md)
- [Roadmap and milestones](roadmap/scope.md)
- [Contribution workflow](CONTRIBUTING.md)
- [Development team and role guides](roles/README.md)
- [Internal design and decisions](docs/internal/README.md)

Start with the [generic follow-up example](docs/specs/generic-hook-example.md), [combined verification command](conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) and [testing evidence](docs/testing.md). Recorded scenario counts describe earlier profiles and are not `/4` verification results. The [CEL qualification](conformance/cel-profile/README.md) records the earlier expression bridge. Governance CI remains advisory; product test failures fail their check.

License selection and public release are pending. No open-source license grant is implied.
