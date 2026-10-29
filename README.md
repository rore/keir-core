# Keir Core

Keir enables SaaS applications to offer safe, AI-assisted customization to business users.

The hosting application defines extension points and exposes approved capabilities. Keir provides a constrained programming language, compiler, simulation and runtimes that let customers compose those capabilities into new application behavior without executing arbitrary generated code. A host-integrated AI agent helps business users express requirements, clarify intended behavior, generate extensions, test scenarios and review results before activation. Keir's core remains usable without an agent.

**Status: initial project structure. No working Keir compiler or runtime is included yet.** Both Java and Node runtimes are in the initial implementation scope.

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

Application build tools, package coordinates and runnable commands will be added with the first executable work. Advisory workflow CI is installed; no dependencies need installing to read this scaffold. The internal baseline reports earlier CEL expression interoperability evidence; that harness has not yet been imported or independently verified here.

License selection and public release are pending. No open-source license grant is implied by this scaffold.
