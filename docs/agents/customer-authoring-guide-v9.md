# Customer authoring guide — draft edition 9

**DRAFT Core 0.5 source guidance.** Matching compiler/runtime packages and the edition-9 companion are not yet qualified. Use only when the host explicitly supports the exact tuple below. Published Core 0.4.0 is nonnumeric. This draft neither upgrades a host nor changes earlier editions, archives or approved artifact bytes.

## Discover the exact contract

Get the host-approved model, entrypoint, package/producer tuple, compiler configuration and scoped authoring tools. Read input/output types, capability results/errors, protected parameters, lifecycle and limits. Resolve resource names through authorized host tools. Ask only for unresolved business choices. If a needed operation, type or producer is unavailable, report the boundary instead of inventing a model field, adapter or upgrade.

Both development profiles use producer `keir-compiler-java/0.5.0`, artifact `keir-artifact/4` and scenario `keir-scenario/4`:

| Profile | Source | Model | Descriptor recipe | Added behavior |
| --- | --- | --- | --- | --- |
| Retained authoring | `keir/2` | `keir-dcm/3` | `keir-record-descriptors/2` | Existing 0.4 rules; no numeric constructs or size |
| Numeric authoring | `keir/3` | `keir-dcm/4` | `keir-record-descriptors/3` | Integer/Decimal operations and bounded-list size |

Reject cross-pairs. A retained model's nominal String type called Integer or Decimal stays nominal, not numeric. New output always needs its own pins and approval; old approval never transfers. Existing supported artifacts execute with their original model and producer identities. The [exact matrix](../specs/compiler-artifact.md#source-and-artifact-identity) preserves historical tuples; this compiler does not author against DCM `/2` or stamp old producers.

## Preserve host authority

EVENT/AFTER_COMMIT/EVENT_REPORT runs after a native operation and cannot veto it. REQUEST/BEFORE_COMMIT/DECISION is read-only before native commit. The host owns authentication, protected values, original keys, permissions, artifact approval/selection, native consistency, cancellation and commit. Simulation establishes none of those native guarantees. Never supply host-bound parameters in source or change the catalog yourself.

Use only declared data and capabilities. Preserve exact Boolean, String, nominal, enum, Timestamp and bounded-list identity. Use typed nominal literals for fixed identifiers; a runtime String cannot become nominal. Optional absence is not null: zero, false and empty String remain present. Lists are bounded supplied values, not computed collections. Timestamps use exact UTC millisecond strings; Duration is expression-only literal whole seconds.

## Numeric values and expressions

Only the numeric pair adds these types:

- Integer is signed int64, `-9223372036854775808..9223372036854775807`. Its canonical String is `0` or `-?[1-9][0-9]*`, at most 20 ASCII characters and within range.
- Decimal is normalized `C × 10^-s`, coefficient precision ≤38 and scale `0..18`. Fractional trailing zeros are removed; integral zeros still count. Maximum magnitude is `10^38-1`, minimum positive value `10^-18`. Precision is shared, not 38 integral plus 18 fractional digits. Canonical wire is ordinary decimal text with no leading plus/zeros, exponent, whitespace or fractional trailing zeros; zero is exactly `0`. It is at most 40 ASCII characters. See [exact grammar and bounds](../specs/domain-model.md#numeric-values).

Host/scenario/literal numeric values are strings, not JSON numbers. `{literal: "10"}` is String; `{literal: "10", type: Integer}` is Integer. `decimal("0.075")` accepts one canonical String literal, never a computed String. Ordinary decimal Integer constants and unary minus are admitted in CEL; hex, leading-zero tokens, uint suffixes, floating-point literals and exponents are not.

Use same-type comparison `==`, `!=`, `<`, `<=`, `>`, `>=`, arithmetic `+`, `-`, `*`, exact `/` and unary `-`. No implicit promotion or String parsing exists. `toDecimal(i)` converts Integer exactly; `toInteger(d)` requires an integral Decimal within int64. `1 == decimal("1")` is invalid; use `toDecimal(1)`. No numeric nominal types, numeric lists, unit algebra or currency enforcement are supplied. The host owns rate ranges, amount units and accounting policy.

Exact division never truncates: Integer `5/2` fails; Decimal `1/8` is `0.125`, while `1/3` fails. Each operator checks its own normalized result. `0.00001 × 0.00001` is representable as `0.0000000001`; `10^-18 × 10^-18` is not. Overflow, inexactness and zero division produce typed errors, not hidden rounding.

Use `round(d, scale, mode)`, `multiplyRounded(a, b, scale, mode)` or `divideRounded(a, b, scale, mode)` only on Decimals. Scale is an Integer literal `0..18`; mode is an exact String literal: UNNECESSARY (fail if inexact), DOWN (toward zero), FLOOR, CEILING, HALF_UP (nearest, ties away from zero) or HALF_EVEN (nearest, ties to even). There is no default or dynamic mode/scale. Rounded multiplication computes before rounding; `round(a*b, ...)` cannot rescue a multiplication that already failed.

For a host declaring Decimal `subtotal`, this source fragment applies a 7.5% discount with explicit two-place HALF_EVEN rounding:

```yaml
- id: rate
  let: {literal: '0.075', type: Decimal}
- id: amount
  let: {cel: 'multiplyRounded(input.subtotal, decimal("1") - rate, 2, "HALF_EVEN")'}
```

For subtotal `199.95`, the expected amount is `184.95`. This is syntax/expected-behavior guidance, not a complete compiled program or packaged execution result. Output remains canonical: a whole `100` is `"100"`, not `"100.00"`. The host can explicitly normalize exact ergonomic text using its String helpers; helpers are not CEL functions and runtime admission does not normalize. Formatting belongs to the host.

Global `size(input.tags)` counts an existing named bounded list, including duplicates, and returns Integer. Empty is zero; the declared cap is at most 16. It is not a query total or a distinct count. No String/map/receiver size, indexing, aggregates or constructed lists are allowed. Do not invent Decimal fields in Daymark or another host that lacks them.

## Presence, guarded results and limits

Use direct `has(record.field)` and prove optional presence in the same expression. A separate Boolean BIND does not transfer proof. If CALL `r` has `when: g`, only that exact earlier unconditional Boolean BIND proves its result exists in `g ? r.field : fallback`. An alias of another guard, equivalent predicate, negation, `g && ...` or later CALL guard gives no proof. If the CALL itself names an alias, use that exact identifier.

Combine both proofs for optional numeric results: `g ? (has(r.amount) ? r.amount : decimal("0")) : decimal("0")`. Proof stays inside the true arm, including nested expressions, and does not escape to siblings or later expressions. A valid scalar BIND becomes an ordinary available value. Bare guarded records and their list fields remain forbidden, including `g ? size(r.items) : 0`. Arguments and rejection handlers see pre-CALL state, never their own result. A rejection handler terminates as BUSINESS_REJECTION even if its result contains ALLOW.

Count every possible call/write, including skipped calls: eight steps, four calls, one possible event WRITE and zero decision WRITEs. Expressions have 64 nodes, depth 16, 512 source bytes; total nodes are at most 512. Existing checked/value/model/trace ceilings remain. No retries, multiple effects, arbitrary functions, dynamic types, record/list construction or generalized guard inference are introduced. Compile against the exact trusted model and repair only from diagnostic codes/paths while preserving the customer's request.

## Simulate and submit

Agree on complete independent expectations before simulation. Run the exact compiled bytes with host-provided simulated implementations. Cover true/skipped calls, optional absent versus zero, bounds and inexact division, rounded ties/carry, unselected versus selected arithmetic errors, rejected/uncertain calls and malformed values after confirmation. CEL conditional laziness and Boolean error masking remain; host data validation and authorization cannot be masked. No failed CALL becomes absence, numeric zero or successful fallback.

Show business output, ordered calls, trace and receipts separately. CONFIRMED is reported commit evidence, NONE definite no effect, UNKNOWN unresolved attempted effect. A later numeric/value/deadline failure preserves a confirmed receipt; unknown effect keeps UNCERTAIN precedence. An escaping numeric error normally reports FAILED/EXPRESSION_FAILED; existing Timestamp/resource classifications remain unchanged. Native reconciliation is separate and never a silent retry.

Only valid COMPLETED ALLOW with empty effects permits further native checks. DENY, malformed/failed evaluation and handled BUSINESS_REJECTION block. Present the exact model/artifact and expected/observed evidence to the authorized reviewer. Approval and activation are separate host decisions. Editing a revision needs fresh checks and approval. Observe native attempts and saved effects separately from simulations and decisions.

## Evidence and matching references

The [maintained specification index](../specs/README.md) identifies the source checkpoint and remaining qualification. The [stock/quote programs](../specs/language.md#numeric-programs) have shared Java/Node source-runtime case evidence, not standalone teaching-consumer or installed-package qualification. Raw CEL/component probes do not prove complete Keir artifacts, packages or a Studio-to-native journey. This draft has no qualified companion/package mapping; only the eventual verified manifest and compatibility record can establish one. Do not invent archive hashes, release rows or executable results. Preserve earlier guide editions and their exact bytes.

Use the [language](../specs/language.md), [DCM](../specs/domain-model.md), [operations](../specs/workflow-expressions.md), [artifact](../specs/compiler-artifact.md), [runtime](../specs/runtime.md) and [decision](../specs/pre-operation-decisions.md) references with their matching schemas. Schemas check shape, not native authority or all semantic bounds. Supply the host's exact approved model, policy and scoped tools separately.
