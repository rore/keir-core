# Keir documentation

<a id="learn-and-run-keir"></a>
## Get started

- [Guided tour](guided-tour.md) explains how an application exposes operations, how a customer defines a rule, and what happens when it runs.
- [Quickstart](quickstart.md) runs a follow-up rule against sample data.

## Integrate with an application

- [Embed Keir](embedding.md) covers the model, compiler, runtime calls and application responsibilities.
- [Run the Java and Node.js examples](../examples/README.md) to compile the follow-up rule and run it through both runtimes.
- [Integrate an authoring assistant](agents/README.md) for scoped discovery, drafting, simulation and review.

## Reference

- [Keir specification](specs/README.md) defines the language, the DCM, compiler output, binding, and runtime behavior.
- [Structural schemas](../schemas/effect-4/README.md) describe the source, model and artifact documents.
- [Java compiler](../compiler-java/README.md), [Java runtime](../runtime-java/README.md) and [Node.js integration](../examples/node-host/README.md) cover component-specific interfaces.
- [Conformance fixtures](../conformance/README.md) give both runtimes the same inputs and independent expected outcomes.
- [Verification and testing](testing.md) records tested scope and links to resource qualification.

The [implementation reference](internal/implementation-reference.md) lists exact wire identifiers and compiler selectors. The [read-only tutorial](manual-authoring.md) shows how to write and test a rule without an assistant.

## For contributors

See [Contributing](../CONTRIBUTING.md) for development checks and the [roadmap](../roadmap/scope.md) for planned work. [Engineering records](internal/README.md) preserve design rationale and earlier proposals.
