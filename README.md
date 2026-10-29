# Keir Core

Keir enables SaaS applications to offer safe, AI-assisted customization to business users.

The hosting application defines extension points and exposes approved capabilities. Keir provides a constrained programming language, compiler, simulation and runtimes that let customers compose those capabilities into new application behavior without executing arbitrary generated code. A host-integrated AI agent helps business users express requirements, clarify intended behavior, generate extensions, test scenarios and review results before activation. Keir's core remains usable without an agent.

**Status: the bounded read-and-decide compiler and Node runtime are implemented; Java runtime and combined conformance are in progress.** Both runtimes remain initial scope. This checkpoint is not a production release or complete support for the baseline language.

## Components

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

- [Public documentation](docs/README.md)
- [Specification status and contracts](docs/specs/README.md)
- [Host and customer agent guides](docs/agents/README.md)
- [Roadmap and milestones](roadmap/scope.md)
- [Contribution workflow](CONTRIBUTING.md)
- [Development team and role guides](roles/README.md)
- [Internal design and decisions](docs/internal/README.md)

Start with the [Java compiler commands](compiler-java/README.md), [Node runtime tests](runtime-node/README.md) and [separately packaged Node host](examples/node-host/README.md). The host runs the committed Java-produced artifact with fake READ adapters and displays DONE/PENDING only after successful execution and result validation. The [shared fixtures](conformance/read-and-decide/README.md) and [CEL qualification](conformance/cel-profile/README.md) provide checkpoint evidence; full Java/Node runtime parity remains open. Advisory workflow CI is installed; product contract CI is pending.

License selection and public release are pending. No open-source license grant is implied.
