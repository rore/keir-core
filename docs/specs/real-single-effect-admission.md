<a id="real-single-effect-admission"></a>
# Single-write host contract

<a id="current-host-bound-contract-effect4"></a>
<a id="effect4-contract"></a>
## Keir's host-bound single-write contract

The [binding API](host-model-binding.md#effect4-binding-and-preparation) defines initialization and preparation for the current single-write contract. [Runtime execution](runtime.md#effect4-execution) defines fresh invocation context and limits. The [implementation reference](../internal/implementation-reference.md) lists the exact serialized identities and assurance values.

The host selects and approves the exact bytes and verifies their integrity before preparation. Host implementations own current permissions, key authenticity and native commit rules. An implementation denial is an attempted call and is definite NONE only with truthful no-effect evidence. Exceptions, timeout or lost response after dispatch remain UNKNOWN. Later denial preserves CONFIRMED. A program that reaches no capability may finish without native authorization or key-authentication checks. Runtime input, context, type, deadline and resource validation still applies.

Java uses `BoundEffectAdapter`, `BoundEffectContext.deadline()`, `BoundEffectInvocation` and `invokeBoundEffectProgram`. The implementation receives the same absolute expiry and trusted clock. The host checks the deadline before native commit. Implementations may call local or HTTP/RPC services. See the [verification record](../testing.md#effect4-verification) for the tested scope.

## Effect outcomes and key identity

The host supplies one original immutable, non-null native `requestKey`. Its implementations validate key authenticity. Earlier catalog-bound and qualified write bindings also supply a `keyBinding` validator. Pass the exact original value or object to each adapter separately from business and protected arguments. Preserve the key without hashing, stringifying, adding a step suffix or substituting source data. The host owns persistent serialization and rehydration. Rehydrated values must represent the same original native identity, and every dispatch in an invocation receives the same original instance or value.

Use the WRITE envelopes defined in [single-effect semantics](single-effect.md):

| Adapter result | Effect evidence |
| --- | --- |
| SUCCESS with valid outer CONFIRMED | Native commit or authoritative matching duplicate receipt |
| BUSINESS_REJECTION / TRANSIENT_FAILURE with NONE | Definite no effect for this attempted dispatch |
| UNKNOWN_COMMIT with UNKNOWN | Commit cannot be established |

After dispatch, timeout/exception/malformed outer envelope cannot establish NONE. Winning valid outer evidence settles before value/deadline validation; malformed result after CONFIRMED remains failed-with-CONFIRMED. Timeout winning first remains UNKNOWN; later evidence cannot rewrite the immutable runtime outcome or resume execution. Host read-only reconciliation is a separately dated native observation, not a replacement outcome or replay instruction. EVENT_REPORT never executes an action or infers an effect from a program-authored label.

<a id="legacy-effect3-qualification"></a>
<a id="compatibility-reference-qualification-and-admission"></a>
<a id="compatibility-boundary"></a>
<a id="host-qualification-input"></a>
<a id="validation-and-revocation"></a>
<a id="same-program-separate-execution-authority"></a>
<a id="required-shared-acceptance"></a>

[Earlier qualified-write admission](../internal/compatibility-reference.md#earlier-qualification).
