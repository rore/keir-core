# Keir specification

The specification defines the documents Keir accepts and how a compiled extension runs. For a walkthrough, [get started](../quickstart.md) or [embed Keir](../embedding.md).

<a id="current-profile"></a>
## Current behavior

The current single-write contract runs after a host event commits. An extension can read data and contain at most one possible write, including writes behind guards. Both Java and Node.js can run the same compiled bytes with real or simulated host implementations. The host owns permissions and native commits.

The compiler and runtime use exact, versioned formats. Use the [current compiler selector and format identities](compiler-artifact.md#effect4-identity) when compiling or validating documents.

| Topic | Contract |
| --- | --- |
| Source, types, guards, and data flow | [Language](language.md) |
| Hooks, capabilities, and call and write policy | [Domain capability model](domain-model.md) |
| Compilation, diagnostics, and serialized artifact | [Compiler and artifact](compiler-artifact.md) |
| Node and Java APIs, adapter context, and native key | [Host binding and invocation](host-model-binding.md) |
| Execution, limits, outcomes, and traces | [Runtime semantics](runtime.md) |

The [schemas](../../schemas/effect-4/README.md) validate document structure. This specification defines semantic rules such as matching types, available values and permitted effects. The [shared conformance suite](../../conformance/README.md) checks compiler and runtime behavior against independent expectations. See [verification and resource evidence](../testing.md#effect4-verification) for the tested scope.

For the current contract, the [structural suite](../../conformance/effect4-structural/test_effect4_contracts.py) checks document shape, the [compiler tests](../../compiler-java/src/test/java/dev/keir/compiler/GenericHookCompilerTest.java) check source and model semantics, and the [shared scenarios](../../conformance/generic-hooks/effect4/scenarios.json) check invocation outcomes and write evidence in both runtimes. The [scenario protocol](single-effect.md#ordered-simulation-protocol-and-acceptance) describes the fixture format.

## Compatibility

Use [documentation history](../internal/history/README.md) to look up earlier formats and APIs. To adopt a different contract, select it explicitly and recompile against the corresponding model.

## Maintaining a contract

Specify feature changes in the affected versioned contracts. Update the applicable schemas, fixtures, and Java and Node conformance evidence together. Keep design rationale in [engineering records](../internal/README.md), future scope in the [roadmap](../../roadmap/scope.md), and usage guidance in the [documentation index](../README.md).
