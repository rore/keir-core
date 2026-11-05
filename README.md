# Keir Core

Keir lets application developers offer customer-written extensions over a host-defined set of operations. A Java compiler checks the extension and produces an artifact that runs in Java or Node. The host defines where extensions run, owns permissions and service behavior, and can integrate an AI assistant to help customers author and test them.

## Start here

- [Run your first extension](docs/quickstart.md). Compile a follow-up rule from a source checkout and run the same artifact in the packaged Node and Java hosts with simulated task services.
- [Take the guided tour](docs/guided-tour.md). Follow that rule from host design through customer customization to runtime.
- [Try Daymark](https://github.com/rore/keir-demo-app). The separate demo app shows how a task application uses Keir for approved customizations that can create real tasks.

## What runs today

The experimental `keir-effect/4` profile supports bounded reads and at most one potential write per program. The host binds the complete capability model and its implementations, selects the exact artifact, and supplies fresh context when an event invokes it. Keir checks compatibility, types and execution limits. The registered implementations enforce current permissions and own the write and its receipt. See the [host model binding contract](docs/specs/host-model-binding.md#corrected-host-bound-4-binding).

Local checks verified matching outcomes for 11 shared `effect/4` scenarios in Java and Node, including packaged hosts. The evidence covers local packages and controlled host integrations. Production CPU, memory and deadline containment are still being qualified. Public package compatibility and registry publication remain pending. See the [test evidence](docs/testing.md), [resource qualification](roadmap/features/runtime-resource-qualification.md) and [public release plan](roadmap/features/public-package-release.md).

## Documentation

- [Documentation index](docs/README.md) and [maintained specifications](docs/specs/README.md)
- [Java compiler](compiler-java/README.md), [Java runtime](runtime-java/README.md) and [Node runtime](runtime-node/README.md)
- [Host examples](examples/README.md) and [shared conformance fixtures](conformance/README.md)
- [Roadmap](roadmap/scope.md) and [contributing](CONTRIBUTING.md)

License selection and public release are pending. No open-source license grant is implied.
