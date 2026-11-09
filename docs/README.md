# Keir documentation

<a id="learn-and-run-keir"></a>
## Get started

- [Getting started](quickstart.md) explains the follow-up extension and shows how to run, change, and compile it.
- [Install released packages](installation.md) sets up the compiler and both runtimes outside this repository.
- [How Keir works](guided-tour.md) explains the source and application model together.

## Integrate with an application

- [Embed Keir](embedding.md) covers the model, compiler, runtime calls and application responsibilities.
- [AI assistants](agents/README.md) covers developer integration, customer authoring, and the packaged guide.
- [Pre-operation decisions](decision-walkthrough.md) explains how an extension can allow or deny a native operation.

## Reference

- [Keir specification](specs/README.md) defines the language, the DCM, compiler output, binding, and runtime behavior.
- The [contract matrix](specs/compiler-artifact.md#source-and-artifact-identity) identifies the matching source, model, artifact and scenario schemas. Use the semantic references to understand what a structurally valid document means.
- [Java compiler](../compiler-java/README.md), [Java runtime](../runtime-java/README.md) and [Node.js integration](../examples/node-host/README.md) cover component-specific interfaces.
- [Conformance fixtures](../conformance/README.md) give both runtimes the same inputs and independent expected outcomes. [Run the Java and Node.js conformance examples](../examples/README.md).
- [Verification and testing](testing.md) records tested scope and links to resource qualification.

The [compiler reference](specs/compiler-artifact.md) documents the current formats and compiler invocation.

## For contributors

See [Contributing](../CONTRIBUTING.md) for development checks and the [roadmap](../roadmap/scope.md) for planned work. [Internal engineering records](internal/README.md) cover development decisions, process, and [documentation history](internal/history/README.md).
