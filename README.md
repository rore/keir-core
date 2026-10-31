# Keir Core

Keir enables SaaS applications to offer safe, AI-assisted customization to business users.

The hosting application defines extension points and exposes approved capabilities. Keir provides a constrained programming language, compiler, simulation and runtimes that let customers compose those capabilities into new application behavior without executing arbitrary generated code. A host-integrated AI agent helps business users express requirements, clarify intended behavior, generate extensions, test scenarios and review results before activation. Keir's core remains usable without an agent.

**Status: read-and-decide and one guarded follow-up effect run in Java and Node with independent packaged hosts.** Local combined verification and [product CI](https://github.com/rore/keir-core/actions/runs/41113166355) pass. Both profiles are experimental. Product CI is installed; workflow-level test protection, production qualification, broader language support and release remain open.

## Components

The [generic-hook contract correction](docs/specs/generic-hook-capability-contract.md) is proposed, not implemented: new profiles will remove mandatory program-specific capability grants while retaining authorization, aggregate budgets and the single-write ceiling. Current experimental profiles still enforce the old grant maps.

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

- [Guided tour: design time and runtime](docs/guided-tour.md)
- [Public documentation](docs/README.md)
- [Specification status and contracts](docs/specs/README.md)
- [Host and customer agent guides](docs/agents/README.md)
- [Roadmap and milestones](roadmap/scope.md)
- [Contribution workflow](CONTRIBUTING.md)
- [Development team and role guides](roles/README.md)
- [Internal design and decisions](docs/internal/README.md)

Start with the [combined verification command](conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts), [Java compiler](compiler-java/README.md) and [runtime tests](docs/testing.md). The read hosts display DONE/PENDING only after validated completion. The [follow-up simulation](conformance/single-effect/README.md) shows created, skipped, rejected and uncertain outcomes with explicit effect receipts. The verifier compiles each profile twice, compares all 61 read cases and 25 effect scenarios, and runs both profiles through both packaged hosts. The [CEL qualification](conformance/cel-profile/README.md) records the earlier expression bridge. Governance CI remains advisory; product test failures fail their check.

License selection and public release are pending. No open-source license grant is implied.
