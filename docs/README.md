# Keir documentation

<a id="learn-and-run-keir"></a>
## Get started

Start with [Keir rules by example](customizations.md): one language capability at a time, shown as simple logic rather than source syntax.

Then [run and change one extension](quickstart.md), or follow the [screenshot walkthrough](guided-tour.md).

- [Install released packages](installation.md) sets up the compiler and both runtimes outside this repository.

## Integrate with an application

- [Embed Keir](embedding.md) covers the model, compiler, runtime calls and application responsibilities.
- [AI assistants](agents/README.md) covers developer integration, customer authoring, and the packaged guide.
- [Pre-operation decisions](decision-walkthrough.md) explains how an extension can allow or deny a native operation.

## Reference

- [Keir specification](specs/README.md) defines the language, the DCM, compiler output, binding, and runtime behavior.
- The [contract matrix](specs/compiler-artifact.md#source-and-artifact-identity) identifies the matching source, model, artifact and scenario schemas. Use the semantic references to understand what a structurally valid document means.
- [Java compiler](../compiler-java/README.md), [Java runtime](../runtime-java/README.md) and [Node.js integration](../examples/node-host/README.md) cover component-specific interfaces.
- [Conformance fixtures](../conformance/README.md) give both runtimes the same inputs and independent expected outcomes. [Run the Java and Node.js conformance examples](../examples/README.md).
- [Guarded-result examples](../examples/README.md#guarded-result-scalar-dataflow) show actual scalar results from conditional calls. Check host support in the [compatibility matrix](specs/compiler-artifact.md#source-and-artifact-identity).
- [Verification and testing](testing.md) records tested scope and links to resource qualification.

The [compiler reference](specs/compiler-artifact.md) documents the current formats and compiler invocation.

## For contributors

See [Contributing](../CONTRIBUTING.md) for development checks and the [roadmap](../roadmap/scope.md) for planned work. [Internal engineering records](internal/README.md) cover development decisions, process, and [documentation history](internal/history/README.md).
