# Keir Core

Keir is an embeddable framework for customer-defined rules. An application exposes the events and operations a rule can use. Customers can combine them into behavior the application has no built-in setting for, such as creating a follow-up when a task is completed in a particular project.

Developers write rules in Keir, a controlled extension intermediate representation, and compile them for Java and Node.js runtimes. An AI assistant can help draft rules and check examples before a customer enables them. The application controls permissions and performs changes through its own services.

## Start here

- [Guided tour](docs/guided-tour.md). Follow one rule from the application contract through authoring, review, and execution.
- [Quickstart](docs/quickstart.md). Run the follow-up rule on sample data.
- [Embed Keir](docs/embedding.md). Expose application operations and run a compiled rule from your own code.
- [Explore Daymark](https://github.com/rore/keir-demo-app). See how a task application uses Keir for customer authoring, review, and execution.

<a id="supported-contract"></a>
## Current scope

A rule can read data and request at most one change per run. Java and Node.js run the same compiled format. See the [Keir specification](docs/specs/README.md) for supported behavior, [verification](docs/testing.md) for measured results and their limits, and the [release plan](roadmap/features/public-package-release.md) for package availability.

## Documentation

- [Documentation index](docs/README.md)
- [Contributing](CONTRIBUTING.md)
- [Roadmap](roadmap/scope.md)

Keir Core is licensed under the [Apache License 2.0](LICENSE). Public package releases are planned.
