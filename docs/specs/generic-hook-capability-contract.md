# Generic hooks and capability composition

<a id="corrected-host-bound-effect4"></a>
<a id="effect4-contract-and-version-boundaries"></a>
<a id="single-write-behavior-and-compatibility-boundaries"></a>
The [Keir specification](README.md#current-profile) defines the current contract. Use the [domain capability model reference](domain-model.md) for the catalog, lifecycle, and restrictions. Use [host binding](host-model-binding.md) to connect implementations.

## Responsibility boundaries

The host defines a hook around a business event and publishes a catalog of operations for the customer’s scope. The hook defines which data enters the extension and how the host consumes its output. A customer can combine the published operations without asking the developer to add a hook for each combination.

Publish operations that express a host capability. In the follow-up example the host provides `tasks.create` with project and title arguments. The extension decides when to use it and what title to pass. A capability can also represent a domain-specific operation the host guarantees, such as an atomic business change.

The catalog is a selected snapshot. Adding a capability changes the model identity. Compile and review extensions against the new model before the host selects them. A customer can select a different already-published operation without changing the model.

## Capability invocation across services

An implementation can call a local service or bridge to a remote one. The host owns routing, credentials, permission checks, and the translation of service responses into [adapter outcomes](runtime.md#effect-evidence-and-outcomes). A model reference identifies the contract for an operation. The host chooses where and under whose authority it runs.

## Event facts, extension choices and protected authority

| Origin | Example |
| --- | --- |
| Event data | The host supplies the completed task’s ID. It decides what other event facts the extension can see. |
| Extension choice | The customer selects the project and title. Those choices belong in the extension source and are checked during compilation. |
| Protected host context | The host supplies authenticated identity, event provenance, and the original request key for the invocation. |

An extension can use a [typed identifier](language.md#bindings) to express a customer’s choice. The host implementation checks whether the requested resource exists and whether the customer can access it. See the [follow-up tutorial](../quickstart.md) for a working example.

<a id="optional-restrictive-controls"></a>
For the current policy and write ceiling, see [call and write policy](domain-model.md#call-and-write-policy). The [frozen hook contract](https://github.com/rore/keir-core/blob/57a74a57e6c5fec2e74d3561d4b2ad1da2e650c1/docs/specs/generic-hook-capability-contract.md#optional-restrictive-controls) records the earlier admission and invocation policy layers.

<a id="effect-and-lifecycle-safety"></a>
For post-commit execution and write receipts, see [entrypoints](domain-model.md#entrypoints) and [runtime semantics](runtime.md).

<a id="versioned-delta"></a>
[Catalog-bound model and artifact changes](../internal/history/compatibility-reference.md#catalog-delta).

<a id="examples-and-rejection-obligations"></a>
<a id="migration-and-acceptance"></a>
[Catalog-bound examples, migration and acceptance](../internal/history/compatibility-reference.md#catalog-examples).
