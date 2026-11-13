# Generic hooks and capability composition

The host publishes the operations an extension can use and owns their routing, credentials and permissions. Customer choices belong in the extension, within the published model and policy.

Use the [model contract](domain-model.md#entrypoints) for hooks and call limits, [host binding](host-model-binding.md) for implementations and authority, and [embedding](../embedding.md#1-define-what-the-host-exposes) for the integration sequence.

For historical catalog-bound admission/policy rules and acceptance vectors, use the [frozen contract](https://github.com/rore/keir-core/blob/53758e7a123311805135dcae48b96a692ad1d777/docs/specs/generic-hook-capability-contract.md) and [compatibility record](../internal/history/compatibility-reference.md).

<a id="capability-invocation-across-services"></a>
<a id="corrected-host-bound-effect4"></a>
<a id="effect-and-lifecycle-safety"></a>
<a id="effect4-contract-and-version-boundaries"></a>
<a id="event-facts-extension-choices-and-protected-authority"></a>
<a id="examples-and-rejection-obligations"></a>
<a id="migration-and-acceptance"></a>
<a id="optional-restrictive-controls"></a>
<a id="responsibility-boundaries"></a>
<a id="single-write-behavior-and-compatibility-boundaries"></a>
<a id="versioned-delta"></a>
