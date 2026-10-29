# Keir Core

Keir enables SaaS applications to offer safe, AI-assisted customization to business users.

The hosting application defines extension points and exposes approved capabilities. Keir provides a constrained programming language, compiler, simulation and runtimes that let customers compose those capabilities into new application behavior without executing arbitrary generated code. A host-integrated AI agent helps business users express requirements, clarify intended behavior, generate extensions, test scenarios and review results before activation. Keir's core remains usable without an agent.

**Status: the bounded read-and-decide compiler and both runtimes pass local and CI conformance.** The same artifact runs in Java and Node with independent packaged hosts. Product CI is installed; workflow-level test protection, production qualification, broader language support and release remain open.

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

Start with the [combined verification command](conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts), [Java compiler](compiler-java/README.md) and [runtime tests](docs/testing.md). Both hosts display DONE/PENDING only after validated completion and decline to display missing/wrong-resource outcomes. The verifier compiles twice, checks identical bytes, compares all 61 shared cases and runs both packaged hosts. The [CEL qualification](conformance/cel-profile/README.md) records the earlier expression bridge. Product contract CI [passed on the implemented checkpoint](https://github.com/rore/keir-core/actions/runs/40921421530); governance CI remains advisory.

License selection and public release are pending. No open-source license grant is implied.
