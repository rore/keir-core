# Keir Core

Keir enables SaaS applications to offer safe, AI-assisted customization to business users.

The hosting application defines extension points and exposes approved capabilities. Keir provides a constrained programming language, compiler, simulation and runtimes that let customers compose those capabilities into new application behavior without executing arbitrary generated code. A host-integrated AI agent helps business users express requirements, clarify intended behavior, generate extensions, test scenarios and review results before activation. Keir's core remains usable without an agent.

**Status: experimental `/1` and `/2` read and single-effect profiles run in Java and Node with independent packaged hosts.** Local combined verification and [product CI](https://github.com/rore/keir-core/actions/runs/41280457363) pass, including generic hooks, source-authored constants and shared model binding. Effects remain simulation-only. Workflow-level test protection, production qualification, broader language support and release remain open.

## Components

The [generic-hook contract](docs/specs/generic-hook-capability-contract.md) is implemented under explicit `/2` profiles: extensions compose the host-approved catalog without mandatory named grants. Optional restrictions only narrow authority; aggregate budgets and the single-write ceiling remain enforced. [Model binding](docs/specs/host-model-binding.md) ties an exact host-held DCM to its complete invoker catalog; artifacts reference that model rather than copying it. `/1` APIs and golden bytes remain compatible.

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

Start with the [generic follow-up example](docs/specs/generic-hook-example.md), [combined verification command](conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) and [testing evidence](docs/testing.md). The verifier preserves 61 legacy read cases and 25 effect scenarios, adds nine generic read/nineteen generic effect scenarios, and runs sixteen compiler processes and eight host batches. The [CEL qualification](conformance/cel-profile/README.md) records the earlier expression bridge. Governance CI remains advisory; product test failures fail their check.

License selection and public release are pending. No open-source license grant is implied.
