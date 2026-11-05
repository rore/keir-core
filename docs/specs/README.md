# Keir specification

The specification defines the documents Keir accepts and how a compiled rule runs. Start with [Embed Keir](../embedding.md) for the integration workflow or [run the Java and Node.js examples](../../examples/README.md) to see it in code.

<a id="current-profile"></a>
## Current behavior

Rules written in Keir can read data and contain at most one possible write, including writes behind guards. The **domain capability model (DCM)** defines the operations and types available to a rule. The host owns permissions and native commits. Keir validates and executes the rule and reports the outcome and write evidence.

The compiler checks the rule against the selected DCM. The runtime checks the compiled artifact against the same model and a supported compiler environment. [Exact format IDs and compiler selectors](../internal/implementation-reference.md#serialized-identifiers-and-compiler-selectors)

| Topic | Contract |
| --- | --- |
| Hooks, capabilities, and ownership | [Generic hooks and capability composition](generic-hook-capability-contract.md#corrected-host-bound-effect4) |
| Source and compilation | [Compiler and artifact](compiler-artifact.md#effect4-identity), [Language](language.md), and the [follow-up example](../../conformance/generic-hooks/effect4/) |
| Binding and invocation | [Host model binding](host-model-binding.md#effect4-binding-and-preparation) and [runtime](runtime.md#effect4-execution) |
| Native effects and receipts | [Host-bound single-write contract](real-single-effect-admission.md#effect4-contract) |
| Structural validation | [Keir document schemas](../../schemas/effect-4/README.md) |
| Verified scope | [Conformance and test evidence](../testing.md#effect4-verification) and [resource qualification](../../roadmap/features/runtime-resource-qualification.md) |

## Compatibility

| Earlier contract | Implementation reference |
| --- | --- |
| Original read-only | [Source](../internal/compatibility-reference.md#original-read-source), [DCM](../internal/compatibility-reference.md#original-read-model), [artifact](../internal/compatibility-reference.md#original-compiler-envelope), and [runtime](../internal/compatibility-reference.md#earlier-runtime) |
| Original simulated write | [Source and artifact](../internal/compatibility-reference.md#original-write-formats), [model](../internal/compatibility-reference.md#original-write-model), and [host API](../internal/compatibility-reference.md#original-write-host-api) |
| Catalog-bound read and simulated write | [Model changes](../internal/compatibility-reference.md#catalog-delta), [binding](../internal/compatibility-reference.md#earlier-binding), [read-only schemas](../../schemas/read-2/README.md), and [worked example](../internal/compatibility-reference.md#earlier-follow-up) |
| Qualified write | [Qualification and admission protocol](../internal/compatibility-reference.md#earlier-qualification) |

Each earlier contract retains its input, artifact, and invocation rules. To adopt a different contract, select it explicitly and recompile against the corresponding model. The [read-only simulation contract](read-simulation.md) describes the separate scenario and report formats.

## Maintaining a contract

Specify feature changes in the affected versioned contracts. Update the applicable schemas, fixtures, and Java and Node conformance evidence together. Keep design rationale in [engineering records](../internal/README.md), future scope in the [roadmap](../../roadmap/scope.md), and usage guidance in the [documentation index](../README.md).
