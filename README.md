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
| [docs/](docs/README.md) | Roadmap, decisions and historical design baseline |

Compile once, execute the same artifact in either runtime. Each runtime is separately consumable and does not require the compiler for execution. Task-specific services, persistence, AI authoring and activation UX belong to the separate `keir-demo-app` repository.

## Start here

- [Roadmap and first milestone](docs/roadmap.md)
- [Repository and runtime scope decision](docs/decisions/0001-repositories-and-runtimes.md)
- [Restricted CEL and resource decision](docs/decisions/0003-restricted-cel-resource-policy.md)
- [Design baseline and its status](docs/README.md)
- [Contribution workflow](CONTRIBUTING.md)
- [Development team and role guides](roles/README.md)

Build tools, package coordinates, runnable commands and CI will be added with the first executable slice. No dependencies need installing for this scaffold. The draft reports earlier CEL expression interoperability evidence; that harness has not yet been imported or independently verified here.

License selection and public release are pending. No open-source license grant is implied by this scaffold.
