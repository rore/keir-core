# Numeric values and rules

**Status: delivered and manager-accepted Core 0.5 candidate.** Compiler, Java/Node runtimes, installed packages, exact helpers, runnable examples, measurements, corrected companion and the tested Daymark journey are complete. Core 0.5 remains unpublished; deployment, live-provider qualification and automatic host upgrades are not implied.

Owning feature: [Numeric values and rules](../../roadmap/features/numeric-values-and-rules.md). Final evidence is the [Core full run](https://github.com/rore/keir-core/actions/runs/43449029316), [corrected companion run](https://github.com/rore/keir-core/actions/runs/43451868303), [Daymark journey run](https://github.com/rore/keir-demo-app/actions/runs/43474838743) and [combined acceptance record](https://github.com/rore/keir-core/blob/348fcdf3536baa501979355ffc8e75a7cd27e7f0/.agent-workflow/tasks/daymark-core-050.md). Current normative behavior lives in [the specifications](../specs/README.md); runnable programs live in [the numeric examples](../../examples/numeric/README.md).

The remaining text preserves the reviewed design and its original implementation requirements, not an outstanding assignment. In particular, §2 and §7 describe pre-implementation baselines, §6 contains uncompiled design templates, §9.2 records only the earlier engine seam, and §10–12 preserve the original plan, acceptance obligations and risks. Later passing evidence in the owning feature closes those obligations. Component evidence alone never established full-artifact, package or application acceptance. Decimal operand/result reparsing remains an explicit measurement limitation; no parse-once or whole-application performance claim is made.

Decisions: signed int64; normalized Decimal coefficient precision ≤38 and scale ≤18; strict canonical boundaries with explicit exact normalization helpers; current authoring retained under the new producer and artifact identity. Fixed-scale candidates are comparison history only. The owner delegated end-to-end feature delivery; ordinary implementation choices do not need another owner decision round. Live environment changes and deployment remain separate.

## 1. Recommendation and release boundary

Deliver Integer and Decimal together in Core 0.5. A quantity-only release would leave percentages and exact amounts without a portable contract. Keep the existing host API shape, one portable checked artifact, reusable preparation, and fresh invocation authority.

| Include | Why |
| --- | --- |
| Signed 64-bit `Integer` throughout scalar positions | Quantities, thresholds, supplied-list length and exact count arithmetic. |
| Exact normalized `Decimal`, coefficient precision ≤38, scale 0–18 | Percentages, rates and amounts without binary floating-point loss, including the exact small-rate product. |
| Same-type comparison, `+`, `-`, `*`, exact `/`, unary `-` | Thresholds, shortages, adjusted amounts and exact ratios. Division never silently truncates. |
| Explicit `round`, `multiplyRounded`, `divideRounded` | Business rounding must state the output scale and mode. Rounded multiplication must not fail first because an exact intermediate is unrepresentable. |
| Explicit checked `toDecimal` and `toInteger` | Combine quantities with rates without implicit promotion or loss. |
| `size(list)` for an existing bounded list | Count the actual supplied entries, such as task tags; not a remote query total. |
| Existing optional and exact-guard scalar composition | Numeric values must work in inputs, protected/agent arguments, results, BINDs and root/handler output fields. |

Do not add floating-point numbers, arbitrary precision/scale configuration, numeric nominal types, unit algebra, currencies, aggregates, numeric list types, list transformations, multiple effects or generalized guard proofs. These are separate decisions, for reasons in §5. Decimal alone is not a money contract.

The selected range admits both `0.00001 × 0.00001 = 0.0000000001` and integers up to `10^38-1`. Fractional precision consumes the same 38-digit coefficient budget. Host domain constraints can narrow this range, never expand it.

## 2. Original baseline and implementation seams

[Core 0.4.0 is published](https://github.com/rore/keir-core/releases/tag/v0.4.0). Its implementation, installed-package qualification and publication were complete at design time. Some normative pages retained preparation-time “development/unreleased” prose; that wording did not reopen release acceptance. Numeric support was absent from this original baseline; the accepted 0.5 candidate now implements it.

| Evidence inspected | Current behavior / required change |
| --- | --- |
| [Language](../specs/language.md), [DCM](../specs/domain-model.md), [workflow expressions](../specs/workflow-expressions.md) | Boolean/String/nominal String/enum/Timestamp scalars; required bounded scalar lists; optional scalar presence. Numeric types and AST nodes are denied. |
| [Compiler `Dcm`](../../compiler-java/src/main/java/dev/keir/compiler/Dcm.java) | Closed type parsing, reserved names and independent descriptor construction. Record scalars become BOOL/STRING or Timestamp messages. Add the new model version and numeric scalar mappings. |
| [Compiler `Expressions`](../../compiler-java/src/main/java/dev/keir/compiler/Expressions.java) | `literal`, `nominalLiteral`, `celType` and `Walker.visit/assign/branches` establish semantic identity independently of CEL. Numeric constants currently fail the primitive allowlist. Keep that rejection for retained contracts. |
| [Compiler `KeirCompiler`](../../compiler-java/src/main/java/dev/keir/compiler/KeirCompiler.java) | Source shape/preflight, shared binding lowering, exact guard provenance and producer identity. Every scalar position routes through binding/type checks. Numeric support must cover all those callers. |
| [Compiler `StrictData`](../../compiler-java/src/main/java/dev/keir/compiler/StrictData.java) | Canonical JSON admits nonnegative safe-integer metadata, not numeric business values. Preserve this hashing recipe by using canonical strings for business numbers. |
| [Java `Artifact`](../../runtime-java/src/main/java/dev/keir/runtime/Artifact.java), [Node `validation.js`](../../runtime-node/src/validation.js) | Independently parse models, admit exact producer tuples, validate lowered plans and complete typed values. Extend both, including reserved identifiers, optional fields and decision scalar outputs. |
| [Java `CheckedCel`](../../runtime-java/src/main/java/dev/keir/runtime/CheckedCel.java), [Node `cel.js`](../../runtime-node/src/cel.js) | Reconstruct descriptors and inspect checked nodes/type/reference maps before planning. Activation and result conversion currently handle Timestamp specially. Decimal needs its own logical-scalar bridge; a message is not permission to select its internals. |
| [Java `KeirRuntime`](../../runtime-java/src/main/java/dev/keir/runtime/KeirRuntime.java), [Node `index.js`](../../runtime-node/src/index.js) | Validate input/context before steps, skip false-guard arguments, settle write evidence before validating successful payloads, and evaluate terminal handlers. Preserve that ordering for numeric errors. |
| [Host binding](../specs/host-model-binding.md), [runtime contract](../specs/runtime.md) | Bind once, prepare once, invoke with fresh inputs/identity/key/deadline; no new host callbacks or per-invocation model arguments needed. |

Java's heterogeneous-numeric CEL option is not a language grant. Native signed Integer division truncates; the trusted exact override passed the hosted probe in both engines, including binding counters. Mixed INT/DOUBLE ordering remains denied. Node resolves calls using its own environment, so checked metadata alone is insufficient. Keep both independent admission overlays and the tested custom registrations.

Architecture sources: [ownership](design-baseline/public/keir-architecture.md#3-logical-architecture-and-ownership), [host responsibilities](design-baseline/public/host-integration-and-hook-contracts-v0.5.md#1-scope-and-ownership), [original-key responsibility](design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md#3-responsibility-boundaries), [initial Java/Node scope](decisions/0001-repositories-and-runtimes.md), [restricted CEL/resource decision](decisions/0003-restricted-cel-resource-policy.md). Current contracts supersede historical Java-later and multiple-effect proposals. Keir owns typed bounded composition; the host owns operations, data, transport, native guarantees and activation. Neither a new hash nor a decimal type proves a host guarantee.

## 3. Exact values, syntax and representations

### 3.1 Integer

Range is `-9223372036854775808` through `9223372036854775807`, inclusive. No unsigned integer business type is added.

Canonical wire spelling is `0` or an optional `-` followed by a nonzero decimal digit and subsequent decimal digits. Reject `-0`, `+1`, leading zeros, whitespace, separators, exponent notation, decimal points and non-ASCII digits. Check at most 20 characters before parsing, then check the range. Bounds apply equally to literals and host values.

### 3.2 Decimal

Define a value as `C × 10^-s`, with integer `C`, `abs(C) <= 10^38-1` and integer `0 <= s <= 18`. Normalize by removing trailing coefficient zeros only while `s > 0`; zero is `(0,0)`. No negative scale hides integral digits. Maximum magnitude is `99999999999999999999999999999999999999`; minimum positive value is `0.000000000000000001`. At scale `s`, at most `38-s` integral coefficient digits fit. This is not 38 integral digits plus 18 fractional digits.

Wire spelling is ordinary ASCII decimal text without an exponent: `-?(0|[1-9][0-9]*)(\.[0-9]*[1-9])?`, followed by semantic range/scale checks and rejection of `-0`. The integer part has at most 38 digits; the optional fraction has 1–18 digits and ends in a nonzero digit. Remove the point and ignore leading zero digits when counting coefficient precision, but count all integral trailing zeros. Zero is exactly `0`. Examples: `0`, `-12`, `0.000000000000000001`, `19.95`. Reject `12.0`, `01.2`, `.5`, `1.`, `1e2`, `NaN` and infinities. Check the 40-ASCII-character cap before parsing. Grammar alone does not establish representability: `10^37 + 0.1` needs 39 coefficient digits.

Canonical input is mandatory at source/artifact/scenario and runtime boundaries. Never silently normalize or round there. Results strip fractional trailing zeros and omit the point if no fraction remains. Formatting is a host presentation concern; `round(decimal("12"), 2, "HALF_EVEN")` returns wire `"12"`.

Provide Java public static `String KeirRuntime.normalizeInteger(String text)` and `normalizeDecimal(String text)`, and Node root exports `normalizeInteger(text)` / `normalizeDecimal(text)` returning strings. Both accept `[+-]?[0-9]+(\.[0-9]+)?` with a 256-ASCII-character pre-parse cap. Trim representational leading/fractional trailing zeros and normalize signed zero, then enforce the selected type's range without rounding. `"+00100.00"` becomes `"100"`; Integer accepts `"100.00"` but rejects `"100.01"`. Decimal accepts `"1.0000000000000000000"` as `"1"`. Reject whitespace, exponents, separators, non-ASCII digits, `.5`, `1.`, null and non-string inputs. Java uses `IllegalArgumentException`, Node `TypeError`, with fixed safe messages that do not echo input. No options, adapter objects, floating-point overloads or caller-selected scales. The probe's exact helper vectors passed; public exports and error classes still need production/package tests.

### 3.3 Boundary and engine values

| Layer | Integer | Decimal |
| --- | --- | --- |
| JSON/YAML business input, scenario data, source typed literal, artifact literal, outcome | Canonical string | Canonical string |
| Existing Java `Map<String,Object>` / Node host API and callbacks | String, including protected arguments | String |
| CEL runtime value | Java `long` / Node `bigint` | Opaque `keir.numeric.Decimal` message containing canonical `value:string` |
| Protobuf record field | `int64` | `.keir.numeric.Decimal` message |
| Arithmetic implementation | Checked signed int64 operations | Java `BigInteger` / Node `bigint` bounded coefficient arithmetic |

The string boundary preserves exact JSON round trips and the existing canonical serializer. Reject native Number/Double/BigDecimal/bigint business API values rather than coercing them. Hosts explicitly call the exact helpers when accepting ergonomic text. Inside CEL, int64 follows the long/bigint rule. Host consumers need no wrapper object.

Numeric strings are not semantically String. The trusted DCM or explicit literal annotation supplies their type. No String concatenation, String ordering, implicit parse, enum refinement or nominal String annotation converts a number. Only fixed trusted runtime functions implement numeric computation; authors cannot register functions.

### 3.4 Source syntax

New numeric source identity is `keir/3` (§8). Add built-in names `Integer` and `Decimal` to identifier reservations for this profile only. The new compiler also accepts the unchanged current `keir/2` + DCM `/3` pair without numeric language features.

```yaml
- id: threshold
  let: {literal: '10', type: Integer}
- id: rate
  let: {literal: '0.075', type: Decimal}
- id: eligible
  let: {cel: 'input.quantity >= 10'}
- id: adjusted
  let: {cel: 'multiplyRounded(input.amount, decimal("1") - rate, 2, "HALF_EVEN")'}
```

`{literal: "10"}` remains String. Numeric typed literals require canonical string payloads. Existing nominal String annotations retain their rules. Source JSON numbers remain metadata only; `{literal: 10}` is rejected, rather than silently acquiring a new meaning.

Within CEL source, ordinary decimal integer constants and unary minus denote Integer; the signed minimum must be admitted as a checked int64 constant or validated equivalent unary form. Reject hex, uint suffixes, doubles and exponent notation as numeric authoring forms. Lexical restrictions must be checked against parsed-source locations as well as checked types; source text filtering alone is insufficient. `decimal("12.5")` accepts exactly one canonical String constant, never a computed String. No bare decimal float literal such as `12.5` is admitted. Both the annotated literal and the constructor produce the same logical type.

`toDecimal(i)` accepts only Integer and is exact over its entire range. `toInteger(d)` accepts only Decimal whose fractional part is zero and whose value fits int64. Neither accepts String, Boolean, Timestamp or a nominal value. There is no implicit promotion in assignments, comparison, arithmetic or conditional branches: `1 == decimal("1")` and `g ? 1 : decimal("1")` fail typing. Use `toDecimal(1)` explicitly.

### 3.5 Comparison history and decision vectors

The hosted probe compared all three candidates. Select normalized18: fixed9 fails the required small-rate result; fixed18 needlessly trades away integral range. Neither fixed alternative is a selectable production mode.

| Candidate | Value/range | Trade-off |
| --- | --- | --- |
| Fixed `(38,9)` | `C × 10^-9`, `abs(C) <= 10^38-1`; 29 integral places, minimum positive `10^-9` | Simple fixed lattice; rejects the requested `0.00001` square. |
| Fixed `(38,18)` | `C × 10^-18`, same coefficient bound; 20 integral places, minimum positive `10^-18` | Admits the small-rate case and all int64 values, but sacrifices nine integral places. |
| **Selected normalized precision ≤38, scale ≤18** | `C × 10^-s`, `0 <= s <= 18`, same coefficient bound; no trailing coefficient zero when `s > 0`; zero is `(0,0)` | Retains up to 38 integral digits at scale zero and small rates, with bounded alignment. It does **not** allow 38 integral plus 18 fractional digits together. |

Contract examples (the first product, rate-chain, quote, range, precision-cliff and quantum vectors passed the probe; full composed programs still need conformance):

| Calculation | Mathematical result / policy consequence |
| --- | --- |
| `0.00001 × 0.00001`, then multiply by `10000000` | `0.0000000001`, then `0.001`. Fixed scale 9 fails at the first operator even though the final answer fits. |
| `0.00001 / 100`, then multiply by `0.00001` | `0.0000001`, then `0.000000000001`. Both wider-scale candidates admit the exact chain. |
| `199.95 × (1 - 0.075)` | `184.95375`, explicitly rounded HALF_EVEN at scale 2 to `184.95`; all three candidates suffice. |
| `300000000000000000000 × 0.00001` | `3000000000000000`; fixed `(38,18)` rejects the quantity operand. This illustrates range, not a proven product need for that magnitude. |
| Selected contract: `10^37 + 0.1` | Needs 39 coefficient digits and fails precision. |
| `10^-18 × 10^-18` | Exact result `10^-36` exceeds both scale-18 candidates. Explicit rounded multiplication at scale 18 HALF_EVEN gives zero; UNNECESSARY fails. |
| `1 / 3` | Exact division fails for all candidates. Explicit rounded division at scale 18 HALF_EVEN gives `0.333333333333333333` where admitted. |

The selected arithmetic bounds are specified once in §4 and §9. Public normalization is specified in §3.2. Candidate-comparison tests stay historical; production tests exercise one selected contract.

## 4. Operations, rounding and failure behavior

All formulas use normalized `(C,s)` values from §3.2. Normalize each operation's result before enforcing precision and scale; never defer its checks to a later AST node.

### 4.1 Exact operators

| Operation | Integer | Decimal |
| --- | --- | --- |
| `==`, `!=`, `<`, `<=`, `>`, `>=` | Same-type mathematical comparison | Align coefficients to a common scale; compare mathematically |
| Binary `+`, `-` | Exact; reject int64 overflow | Align to `s=max(s_a,s_b)`, add/subtract coefficients, normalize and check |
| Binary `*` | Exact; reject int64 overflow | `(C_a*C_b,s_a+s_b)`, normalize and check precision ≤38/scale ≤18 |
| Unary `-` | Exact; minimum int64 negation fails | Exact, including zero canonicalization |
| Binary `/` | Exact integer quotient only; nonzero remainder fails | Divide `C_a*10^(18+s_b-s_a)` by `C_b`; require remainder zero, normalize quotient at scale 18 and check |

Division by zero fails for both types, including `0/0`. Integer minimum divided by `-1` overflows. Integer `5/2` does **not** return `2`; use explicit Decimal conversions for a fractional ratio. Decimal `1/8` is `0.125`; `1/3` fails rather than choosing a hidden rounding policy. A normalized result requiring more than 18 fractional places or 38 coefficient digits fails. Evaluate and check each operator: `(max + 1) - 1` fails at the addition. An unselected branch is different (§4.3).

Arithmetic uses widened exact temporaries only within one trusted operation. It never exposes a wider numeric type to another AST node. Ordinary `round(a*b, ...)` does not rescue an inexact/overflowing multiplication that already failed. The explicit rounded multiplication form exists to avoid that trap.

### 4.2 Explicit rounding

All functions below return Decimal. `scale` must be an Integer **literal** from 0 through 18. `mode` must be one of the exact String **literals** below. They are closed operation parameters, not general String-to-enum coercion or caller-controlled runtime configuration.

| Function | Mathematical operation before rounding |
| --- | --- |
| `round(d, scale, mode)` | Round the exact value `d`. |
| `multiplyRounded(a, b, scale, mode)` | Multiply two Decimals using a bounded wider temporary, then round once. |
| `divideRounded(a, b, scale, mode)` | Divide two Decimals using integer quotient/remainder, then round once. Reject zero divisor. |

| Mode | Rule when discarded digits are nonzero |
| --- | --- |
| `UNNECESSARY` | Fail; only an already-exact answer is allowed. |
| `DOWN` | Toward zero. |
| `FLOOR` | Toward negative infinity. |
| `CEILING` | Toward positive infinity. |
| `HALF_UP` | Nearest; ties away from zero. |
| `HALF_EVEN` | Nearest; ties to an even retained integer coefficient at the requested scale. |

No default mode exists. Reject unknown modes, negative/dynamic scales and dynamic modes at compilation and independent preparation. Other rounding modes are not admitted in this increment.

Define the algorithm using signed rational `N/D`, with `D > 0`, at requested scale `t`. Divide `abs(N)` by `D` into `q,r`. DOWN keeps `q`; FLOOR/CEILING increment its magnitude only in the indicated sign direction; nearest modes compare `2*r` with `D`, applying the stated tie rule. Restore the sign, normalize `(q,t)`, then check precision/scale. UNNECESSARY requires `r=0`. Rounding cannot hide output overflow. Zero becomes canonical `0`. Validate the closed mode/scale even when no digits are discarded.

For normalized operands `(A,s_a)` and `(B,s_b)`:

- `round`: if `s_a <= t`, return the already-exact value. Otherwise round `A / 10^(s_a-t)` to an integer at scale `t`.
- `multiplyRounded`: form `P=A*B`, `s=s_a+s_b`. If `s <= t`, normalize/check `(P,s)` directly. Otherwise round `P / 10^(s-t)` to scale `t`. Do not apply the exact multiplication result check before rounding.
- `divideRounded`: let `e=t+s_b-s_a`. Round `(A*10^max(e,0)) / (B*10^max(-e,0))` to scale `t`, normalizing denominator sign.
- Do not append padding zeros to an already-exact lower-scale result. Normalize before the final precision check, including carry results.

These formulas also specify a small independent oracle for future tests. No infinite digit expansion, precision context or retry loop is needed. Examples: `round(2.345,2,HALF_EVEN)=2.34`, `round(2.355,2,HALF_EVEN)=2.36`, `round(-2.345,2,HALF_UP)=-2.35`, and `divideRounded(1,3,2,DOWN)=0.33` (Decimal constructor syntax omitted in this explanatory sentence).

### 4.3 Errors, laziness and effect evidence

| Boundary | Required result |
| --- | --- |
| Invalid numeric literal/type/operator | Compiler `TYPE_MISMATCH` or `CEL_POLICY_DENIED` under the binding; no artifact. Invalid DCM declaration uses `DCM_INVALID`. |
| Forged checked metadata, internal Decimal selection, wrong type/overload | Preparation `ARTIFACT_INVALID`; incompatible tuple uses `PIN_MISMATCH`; no callback. |
| Malformed/noncanonical/out-of-range host input | `INPUT_INVALID`; protected value uses `HOST_BINDING_INVALID`; no step dispatch. |
| Invalid adapter numeric value | `ADAPTER_INVALID`; no successful continuation or handler. |
| Escaping numeric arithmetic/conversion error, including overflow, inexactness or zero division | `FAILED` with `EXPRESSION_FAILED` at the consuming binding. No newly invented numeric outcome fields. |
| Existing structural/byte/AST/deadline budget failure | Existing `BUDGET_EXCEEDED` / `LIMIT_EXCEEDED` behavior. |

Numeric representability is a value-domain failure, not a new resource budget. Mapping all pure numeric failures to `EXPRESSION_FAILED` also avoids inventing portable precedence among multiple CEL errors merged by Boolean evaluation. This deliberately does not change the existing Timestamp-overflow classification. Boundary values longer than their numeric lexical cap are invalid before parsing; overall existing business/record byte-limit failures still use `BUDGET_EXCEEDED`.

Pure numeric operations produce typed CEL errors, not a global failure flag or out-of-band state. Existing CEL semantics remain: `?:` evaluates only its selected arm; an erroneous condition propagates; false can mask an arithmetic error in `&&`, and true can mask it in `||`, in either operand order. For example, `(1 / 0 == 0) && false` may complete as false under this specified CEL rule. `false ? 1/0 : 1` returns 1. Both branches still pass static type/policy validation. There is no Boolean masking of invalid host data, deadline checks, authorization or adapter failures outside evaluation.

`when: g` skips numeric arguments and handlers if false, but never discounts static calls/writes. A successful guarded numeric result uses `g ? r.amount : decimal("0")`; an optional result uses `g ? (has(r.amount) ? r.amount : decimal("0")) : decimal("0")`. Zero is present, not absent. `null` remains invalid. A guard alias, arithmetic predicate or consuming CALL's `when` supplies no new proof. A rejected/uncertain CALL terminates; it cannot become numeric zero or fallback success.

Keep sparse activation and fresh invocation state. Validate complete successful records before retention, including numeric fields an expression does not use. A confirmed WRITE followed by invalid numeric output or later arithmetic failure keeps its CONFIRMED receipt. An unknown write keeps UNCERTAIN precedence. An arithmetic failure in a terminal business-rejection handler keeps the original rejection in `error.cause`. Numeric code adds no retries, traces containing raw values, host calls or effect authority.

## 5. Lists, nominal types and business units

Admit only global `size(list)` over an already-admitted named bounded list, returning Integer in `0..maxItems`. Do not admit String/map size, receiver syntax, lists constructed in expressions, indexing or aggregates. Ordered duplicates count separately; an empty list has size 0. The DCM cap stays 0–16 and the value must pass complete validation before evaluation. A truncated page's size is the number supplied, not the total matching rows. A total count requires a separately defined host capability with its own consistency/authorization contract.

Guarded list selection remains denied even inside an exact guard: `g ? size(r.items) : 0` does not expand the scalar-result proof to lists. Hosts can expose a true scalar count if they need that contract. Existing unguarded input/result lists can be counted. Numeric list elements remain deferred: the concrete list use is tags, and adding repeated Decimal bridge values/membership does not serve either numeric scalar example. This is an explicit release boundary, not a claim that numeric lists cannot be bounded.

Defer nominal numeric types and unit algebra. `Integer` quantities and `Decimal` rates have exact representation but no dimensional identity. Arithmetic cannot distinguish kilograms from items or one currency from another. Merely allowing `{kind: nominal, base: Decimal}` would leave multiplication/division result units ambiguous; a meaningful unit system needs explicit rules for quantity×rate, ratios and exchange conversions. A smaller nominal-only equality/assignment feature would not solve the proposed amount computation. Do not pretend it does.

The host owns field meanings, nonnegative/percentage bounds, currency matching, accounting policy, per-currency minor units, authorization and native validation. Source may implement additional customer business predicates, but cannot waive host checks. The examples explicitly use one quantity unit or one host-selected amount unit. Full money handling needs a separate requirement and contract, not a currency string that Core never checks.

## 6. Two complete proposed programs

These are **uncompiled source templates** for the selected contract. Each declares every source field and step. The DCM definitions fully specify their host surfaces. Replace the fingerprint token with the canonical digest of the exact JSON DCM during implementation; no hash or execution result is fabricated. Host operations are hypothetical approved capabilities, not services supplied by Core or Daymark.

### 6.1 Stock shortage: read stock and request one replenishment

Host DCM template:

```yaml
format: keir-dcm/4
domain: {id: example.stock, version: '1'}
hostKey: {source: requestKey, type: example.StockRequestKey}
types:
  ItemId: {kind: nominal, base: String}
  EventId: {kind: nominal, base: String}
  ActorId: {kind: nominal, base: String}
  StockInput: {kind: record, fields: {itemId: ItemId}}
  Stock: {kind: record, fields: {available: Integer}}
  Order: {kind: record, fields: {accepted: Integer}}
  StockStatus: {kind: enum, values: [REQUESTED, NO_OP, REJECTED]}
  StockReport: {kind: record, fields: {status: StockStatus, ordered: Integer}}
capabilities:
  stock.get:
    version: '1'
    effect: READ
    parameters:
      itemId: {type: ItemId, binding: agent}
      actorId: {type: ActorId, binding: host}
    result: Stock
    errors: [NOT_FOUND, ACCESS_DENIED]
  stock.request:
    version: '1'
    effect: WRITE
    parameters:
      itemId: {type: ItemId, binding: agent}
      quantity: {type: Integer, binding: agent}
      actorId: {type: ActorId, binding: host}
    result: Order
    errors: [ACCESS_DENIED, STOCK_CHANGED]
entrypoints:
  StockChanged:
    input: StockInput
    output: StockReport
    trigger: EVENT
    placement: AFTER_COMMIT
    consumer: {id: example.stock.report, version: '1', kind: EVENT_REPORT}
    event: {idType: EventId, sourceType: ItemId}
```

Proposed source:

```yaml
language: keir/3
domain: {id: example.stock, version: '1', fingerprint: '<canonical-stock-dcm-sha256>'}
entrypoint: StockChanged
name: replenish-below-ten
steps:
  - id: stock
    call: stock.get
    args: {itemId: {cel: input.itemId}}
  - id: low
    let: {cel: 'stock.available < 10'}
  - id: quantity
    let: {cel: 'low ? 10 - stock.available : 0'}
  - id: order
    call: stock.request
    when: low
    args:
      itemId: {cel: input.itemId}
      quantity: {cel: quantity}
    onError:
      STOCK_CHANGED:
        record:
          status: {literal: REJECTED}
          ordered: {literal: '0', type: Integer}
return:
  record:
    status: {cel: 'low ? "REQUESTED" : "NO_OP"'}
    ordered: {cel: 'low ? order.accepted : 0'}
```

Input is `{"itemId":"item1"}`. The host supplies `actorId`, original key, event and deadline separately. Negative available stock means backlog in this hypothetical host. The native request operation validates quantity, current authorization and its own concurrency/replay rules; the preceding read is not an atomic reservation. Customer threshold `10` belongs in source, not a new required hook field.

| Scenario | Expected result |
| --- | --- |
| READ returns `{"available":"7"}`; WRITE confirms `{"accepted":"3"}` | Args contain `quantity:"3"`; COMPLETED `{status:"REQUESTED",ordered:"3"}` and one CONFIRMED receipt. |
| Available `"10"` or larger | WRITE skipped; COMPLETED `{status:"NO_OP",ordered:"0"}`; no receipt. |
| Available `"0"` | Request 10; zero is valid data. |
| Available signed minimum | `10 - available` overflows before WRITE; FAILED/EXPRESSION_FAILED, no write receipt. |
| READ supplies JSON number `7` or string `"07"` | ADAPTER_INVALID; no WRITE. |
| WRITE reports STOCK_CHANGED with NONE | BUSINESS_REJECTION with handler result REJECTED/0 and NONE; not successful fallback. |
| WRITE confirms but returns out-of-range accepted quantity | Failure preserves CONFIRMED. |
| WRITE returns UNKNOWN_COMMIT | UNCERTAIN; no numeric fallback and no retry. |

### 6.2 Quote decision: round a percentage-adjusted amount explicitly

This host quotes one unit of account selected outside the extension. A discount rate is a fraction: `0.075` means 7.5%. It is not a money/currency model. The host performs native price and authorization checks after an eligible ALLOW.

```yaml
format: keir-dcm/4
domain: {id: example.quotes, version: '1'}
hostKey: {source: requestKey, type: example.QuoteAttemptKey}
types:
  AttemptId: {kind: nominal, base: String}
  QuoteId: {kind: nominal, base: String}
  QuoteInput:
    kind: record
    fields:
      subtotal: Decimal
      discountRate: {type: Decimal, optional: true}
  Decision: {kind: enum, values: [ALLOW, DENY]}
  QuoteDecision:
    kind: record
    fields: {decision: Decision, message: String, amount: Decimal}
capabilities: {}
entrypoints:
  QuoteBeforeAccept:
    input: QuoteInput
    output: QuoteDecision
    trigger: REQUEST
    placement: BEFORE_COMMIT
    consumer: {id: example.quote.decision, version: '1', kind: DECISION}
    event: {idType: AttemptId, sourceType: QuoteId}
    effectGrant: 0
```

```yaml
language: keir/3
domain: {id: example.quotes, version: '1', fingerprint: '<canonical-quote-dcm-sha256>'}
entrypoint: QuoteBeforeAccept
name: quote-under-limit
steps:
  - id: rate
    let: {cel: 'has(input.discountRate) ? input.discountRate : decimal("0")'}
  - id: valid
    let: {cel: 'input.subtotal >= decimal("0") && rate >= decimal("0") && rate <= decimal("1")'}
  - id: amount
    let: {cel: 'valid ? multiplyRounded(input.subtotal, decimal("1") - rate, 2, "HALF_EVEN") : decimal("0")'}
  - id: allowed
    let: {cel: 'valid && amount <= decimal("1000")'}
return:
  record:
    decision: {cel: 'allowed ? "ALLOW" : "DENY"'}
    message: {cel: 'allowed ? "" : "Check the amount, discount and approval limit."'}
    amount: {cel: amount}
```

| Input | Expected completed output / failure |
| --- | --- |
| `{"subtotal":"100","discountRate":"0.075"}` | ALLOW, amount `"92.5"`, empty message, empty effects. |
| `{"subtotal":"1000"}` | Absent rate defaults to zero; ALLOW, amount `"1000"`. |
| `{"subtotal":"1000.01","discountRate":"0"}` | DENY, amount `"1000.01"`; present zero remains present. |
| `{"subtotal":"2.345"}` / `{"subtotal":"2.355"}` | ALLOW with `"2.34"` / `"2.36"`, exercising ties to even. |
| `{"subtotal":"100","discountRate":"1"}` | ALLOW, amount `"0"`. |
| Negative subtotal or rate outside 0..1, but representable | DENY, amount `"0"`; selected validation branch avoids the calculation. |
| `discountRate:null`, `subtotal:100`, `subtotal:"100.00"`, or out-of-range text | INPUT_INVALID before steps; no decision eligible for consumption. |
| Maximum Decimal subtotal (`10^38-1`) with absent rate | Exact multiplication by 1 and scale-2 rounding preserve the integer; DENY with that same amount, not overflow. |
| `{"subtotal":"99999999999999999999.999999999999999999"}` | Rounds to `"100000000000000000000"`; valid under the selected normalized contract, then DENY by the amount limit. Fixed18's carry rejection does not apply. |

The host's result consumer validates all three declared fields before reading `decision`; the existing teaching consumer accepting only decision/message cannot be reused unchanged. DENY's amount is explanatory data only. An ALLOW does not set a price, authorize a payment or prove native commit.

## 7. Required tested Daymark release example

**Completed:** the [accepted journey](https://github.com/rore/keir-demo-app/actions/runs/43474838743) passes all 261 browser tests and 606 Node tests, with 12 unchanged opt-in skips. The baseline assessment and seven obligations below are preserved as design history, not pending adoption work. Final [package provenance](https://github.com/rore/keir-demo-app/blob/9eaddae0ed262f06727f440651204e746974979e/docs/package-provenance.md) records the adopted candidate and companion pins.

Refreshed source baseline: fetched Daymark `77feb10` pins compiler/runtime 0.4.0 and companion edition 8. The [catalog](https://github.com/rore/keir-demo-app/blob/77feb10/src/extension-catalog.mjs) retains explicit older selections, exposes TaskCreated, TaskCompleted and TaskBeforeCreate, and uses DCM `/3` with descriptor recipe `/2`. The model digest is `sha256:e7e250a249ea72f3261d7e52ac24a45fe42542ccf5fc5eb5de754069ca26f119`. This refresh reads fetched source without claiming the local checkout or running app was upgraded. The completed [0.4 adoption record](https://github.com/rore/keir-core/blob/6983654/.agent-workflow/tasks/daymark-core-040.md) records existing conditional-follow-up qualification; it is not a numeric journey result.

The current DCM projects proposed task tags, priority, project and hasNotes/hasDueDate for TaskBeforeCreate. [Tag normalization](https://github.com/rore/keir-demo-app/blob/main/src/task-workflow-values.mjs) deduplicates normalized tags and caps them at eight. [Native guard](https://github.com/rore/keir-demo-app/blob/main/src/task-create-guard.mjs) captures exact selection/pins and rechecks authority and read facts. [Rule descriptions](https://github.com/rore/keir-demo-app/blob/main/src/task-rule-description.mjs) do not yet establish numeric size-comparison support.

Reuse the [0.4 configured-authoring browser journey](https://github.com/rore/keir-demo-app/blob/77feb10/tests/e2e/studio-conditional-follow-up.spec.mjs) as the integration pattern: scripted offline transport drives real compilation, saved scenarios, simulation, summary, approval, separate activation, native Tasks and reopen. The [compatibility tests](https://github.com/rore/keir-demo-app/blob/77feb10/tests/core-candidate-compatibility.test.mjs) preserve authentic older rows in both cache orders. The adoption record reports build, 588 Node passes with 12 opt-in process/crash skips, and 260 pinned-browser passes in hosted run `43258837565`. These are recorded historical 0.4 results, not freshly verified numeric evidence or live-model qualification. Add new-profile/model and retained 0.4 coverage rather than repeat unchanged historical work.

Include a real tested 0.5 release example: **“Before saving a new task with more than three tags, require notes.”** The condition is `size(input.tags) > 3 && !input.hasNotes`, returning DENY with a correction, otherwise ALLOW. It counts the host-normalized list actually supplied. It needs no invented amount, cost, effort or database-count field and no capability call. It demonstrates Integer/list length, **not Decimal**. The runnable Core quote example must demonstrate Decimal. Test adoption in isolation; deployment remains separate.

The independent Daymark source assessment confirms that raw `[" Client ","client","REVIEW","internal"]` becomes three distinct normalized tags. Native normalization trims, NFC-normalizes, lowercases, NFC-normalizes, trims and deduplicates; each tag has a 1–32 UTF-16-unit limit. Omitted tags become an empty list. Nine distinct tags or invalid tags fail native validation before the hook. Core itself counts supplied duplicates; the host, not `size`, supplies distinctness here.

This is a **new-task creation** rule, including automation through `tasks.create`, not a global task invariant. Ordinary edits and `tasks.update` can later leave up to eight tags without this creation rule rerunning. Existing tasks are not retroactively checked. `tasks.openMatch` returns a Boolean and native search uses 25-row pages; neither is a total-count capability. Post-event input contains taskId/occurredAt, and a later task READ need not represent event-time data. The complete proposed-input list is the stronger fit.

The complete adoption gate is more than a runtime bump:

1. Qualify the new independent Core packages and matching companion through an authorized isolated workflow. Daymark's refreshed baseline uses guide edition 8. Add a new immutable host-definition generation/model pin and explicit source/scenario support. Preserve old selections, definitions and approved artifacts; do not rewrite the current definition in place. Existing 0.4 catalog retention is useful precedent but does not prove coexistence across the new numeric model/recipe. Resolve the exact selected definition into its matching bound/prepared handle, preserving per-invocation authority. Test catalog/lifecycle/simulation/scenario caches with old→new and new→old use orders and both selections.
2. Add the bounded size/comparison case to discovery examples, authoring instructions, verified-artifact rule descriptions and proposed/Studio/current-use summary and review surfaces, including `src/web/rule-description.ts`. Derive the summary from the admitted artifact, not source text or chat. No fallback summary may silently omit the tag threshold or notes condition.
3. In Studio, start with a natural-language request and exercise actual discovery, draft compilation and saving. Use one artifact for simulation and real invocation. Keep the human's chosen threshold explicit and editable in the draft. Existing mocked Studio projections and an API-compiled guard E2E do not prove this complete journey. A deterministic authoring transport can cover regression; paid/live provider qualification needs separate authority if claimed.
4. Simulate empty/three/four/eight tags, notes absent/present/whitespace-only, and normalization of duplicate/case-variant tags. More than eight tags fails native validation; it is not a successful rule case. Include the existing invalid-key/unavailable scenario so a blocked check is visible and testable, not hidden in a fixture helper.
5. Review independent expectations and actual outcomes. Approve the exact artifact/evidence, then use **separate activation**. Approval alone must not turn the rule on. Editing threshold after approval invalidates that revision's evidence/approval as today.
6. Through every existing supported task-create entry point, including automation, prove four tags without notes yield `NOT_CREATED`/`RULE_DENIED`, with no task row or TaskCreated event. The form retains values and shows the correction. Correcting to three tags with a fresh operation ID yields CREATED, one saved row and exactly one TaskCreated event. Four tags with notes also save once. Runtime/approval failure is `CHECK_UNAVAILABLE`, not DENY. Recheck selection changes, cancellation and deadlines around native commit. Keep post-commit event and prior saved-rule behavior intact.
7. Verify saved revision, summaries, evidence, selection and native results survive reload. Check cross-owner access rejection, unchanged other-hook selections, existing unknown-outcome reconciliation and that activation does not retroactively apply to old tasks.

Native denial must create no task or TaskCreated event. Interactive denial preserves the immutable `NOT_CREATED` operation record. Automation denial preserves its existing immutable original-key `NONE` receipt, guard details and replay identity; it must not erase no-effect evidence. Do not assert literally no database writes. Simulation and approval, by contrast, must not mutate native tasks or turn on any selection. A corrected form submission uses a new operation ID; a lost-response retry reconciles the original operation rather than creating another task. The `hasNotes` projection uses trimmed native notes, so whitespace-only notes are absent. Canonical scenario proposals test the projected Boolean; separate native tests prove projection and tag normalization instead of having the scenario runner normalize its own inputs.

Host seams are `src/extension-catalog.mjs`, `extension-lifecycle.mjs`, `task-create-guard.mjs`, `compiler.mjs`, `keir-scenario.mjs`, `task-effect-simulation.mjs`, `task-rule-description.mjs`, `src/web/rule-description.ts`, authoring discovery/agent/example modules and existing Studio/e2e coverage. No native database/schema migration is needed for this tag-count rule. Release needs the executed journey; a fixture or source assessment cannot satisfy it. Live environment changes, paid provider use and deployment remain separately scoped.

## 8. Compatibility and descriptor contract

### 8.1 Exact profile and producer tuples

Compiler/Java/Node target the coherent 0.5.0 release. Retain CEL Java 0.14.0, CEL Node 0.6.1, protobuf Java 4.35.1 and protobuf Node 2.6.2, which passed the numeric seam. New compiler producer is `keir-compiler-java/0.5.0` for **both** authoring profiles. No release/publication is implied by assigning that identity here.

| New compiler profile | Source | DCM/modelRef format | Artifact | Scenario | Descriptor recipe | Semantics |
| --- | --- | --- | --- | --- | --- | --- |
| Retained current authoring | `keir/2` | `keir-dcm/3` | `keir-artifact/4` | `keir-scenario/4` | `keir-record-descriptors/2` | Current 0.4 authoring/guards; no numeric constructs or list size |
| Numeric authoring | `keir/3` | `keir-dcm/4` | `keir-artifact/4` | `keir-scenario/4` | `keir-record-descriptors/3` | Current guards plus this numeric contract |

Source increments because an existing explicit type annotation gains built-in meanings and a new numeric expression surface. DCM changes its type vocabulary; artifact changes its semantic type/descriptor contract; scenario changes its supported tuple/value contract. Separate new schemas make retained format validators fail closed. Do not edit old schemas to admit numeric data under old identities, or stamp a newer compiler's bytes with an older producer.

Retain current authoring to avoid forcing a model migration for a nonnumeric edit. Select semantics from the exact source/DCM pair; reject cross-pairs and DCM `/2` compilation by the new compiler. Do not add a host option to emulate every historical compiler. Original compilers remain the way to reproduce exact historical authoring behavior. Source digest still hashes decoded source with verbatim CEL strings and canonical JSON; quoted numeric text needs no new JSON number codec.

For retained current authoring, preserve DCM content/digest and recipe `/2` bytes, nominal names, default grants, EVENT/REQUEST rules, enum literals and 0.4 exact guarded-result semantics. The new producer/artifact identity and normal fresh approval are intentional. Do not stamp old producers, rewrite old source/model/artifact bytes or transfer approvals. Hosts must register the new producer/definition pins even when their DCM stays `/3`. This source compatibility is selected design, **not** a result established by the raw CEL probe.

New runtimes retain four historical tuples unchanged: artifact/model `/2` with producer 0.2.0 and recipe `/2`; artifact/model `/3` with producers 0.3.0, 0.3.1 or 0.4.0 and recipe `/2`. Add exactly the two 0.5.0 rows above at the pinned environment. Keep `/1` unsupported. Match the entire tuple by identity, never semver ordering or “latest producer”. Old runtimes reject artifact `/4`.

Both 0.5 profiles use 0.4's exact-guard scalar behavior, complete declared-variable contexts and sparse guarded results. Historical producers keep their old rules. Replace hardcoded `producer == 0.4.0` checks with the small explicit set of admitted guard-capable tuples. Keep default effect grants EVENT=1 and DECISION=0, existing capability-policy defaults and explicit grant checks in both new profiles. Numeric features never alter those defaults.

Resolve semantic type by profile and DCM type kind before spelling. A retained `/3` nominal String type called `Integer` or `Decimal` stays a String nominal, including typed literals, record fields, equality and descriptor encoding. Reserve those names as built-ins only for the numeric pair. Tests must compile such unchanged models through 0.5, compare model digest/recipe bytes with 0.4, execute new output in both new runtimes, reject numeric constructs there, and preserve authentic old artifacts separately.

Add new closed schemas at `schemas/source-3/source.schema.json`, `schemas/dcm-4/dcm.schema.json`, `schemas/artifact-4/artifact.schema.json` and `schemas/scenario-4/scenario.schema.json`. Artifact `/4` admits only the two 0.5 tuples, pairing model `/3` with recipe `/2` and model `/4` with recipe `/3`. Its model `/3` branch retains old scalar vocabulary; structural schemas never replace independent semantic checks. Source `/2` and all old artifact/scenario schemas stay immutable. Scenario `/4` admits both new artifact tuples, with unchanged response/expectation structure and numeric strings only where the numeric DCM declares them. Old scenarios retain their exact old tuple routing. Reject relabeled old scenarios, mixed tuple fields and stale model digests rather than upgrading them on load.

### 8.2 Recipe `/3`

Keep ASCII-sorted record names/fields, consecutive field numbers, exact `json_name`, proto3 and `keir.read` record namespace. Add `int64` fields for Integer. Required Integer fields use the normal scalar label; optional Integer uses `proto3_optional` with the existing deterministic synthetic-oneof recipe. Requiredness and canonical wire syntax are validated before construction; protobuf default zero never proves a required value was supplied.

For Decimal, derive one fixed dependency `keir_numeric.proto`, package `keir.numeric`, syntax proto3, message `Decimal` with exactly `string value = 1` and `json_name = "value"`. Include it for **every DCM `/4` using recipe `/3`**, even when no record field or capability parameter uses Decimal. Model binding is program-independent; expression-only constructors, conversions and intermediate BINDs need the type before an artifact is selected. It has no imports, options, extensions or source information. Decimal record fields reference that message and use natural presence, like Timestamp. Their logical model type is scalar; no Decimal field selection, constructor of a protobuf object, message equality on arbitrary records, or `has(decimal.value)` is admitted.

FileDescriptorSet order is optional timestamp dependency, mandatory numeric dependency, then `keir_read.proto`; record-file dependency names follow that order and always import `keir_numeric.proto`. The compiler constructor/function checker, Java/Node type registries and direct Decimal BIND conversions all use this always-registered exact `keir.numeric.Decimal` type, not unspecified library well-known-type behavior. Timestamp descriptor and behavior remain unchanged, and retained recipe `/2` descriptor bytes remain exactly unchanged. Generate and pin exact descriptor bytes/hash during implementation; do not invent them here. Independently derive the same bytes in compiler and both runtimes.

Numeric host strings map to int64 or Decimal messages only after full validation. Node's existing `fromJson(recordSchema, value)` cannot consume a Decimal field's public string unchanged: build the typed internal projection with `{value: canonicalText}` first, preserving omission. Java must distinguish Decimal from its Timestamp-only message conversion. Retain a successful Decimal BIND's actual engine message in invocation-owned state for the next expression; do not serialize and rebuild it between BINDs. The probe passed Java object reuse and Node reuse of the reflected result's existing `message` payload. Map to canonical host strings only at external arguments/results/outcomes. Validate by declared semantic type and never expose an internal message to a host callback. Complete-record validation, including unused numeric fields and required/optional presence, still precedes retention.

### 8.3 Checked expressions and operation admission

Extend the existing CheckedExpr gate rather than add another interpreter. Integer uses CEL INT64 constants/types. Decimal uses the exact message type above and compiler-declared, runtime-registered trusted overloads. Comparisons must be mathematical, not String order or binary float comparison. Generic equality is allowed only after exact semantic-type checks; canonical Decimal message equality must match coefficient equality or be replaced by a qualified numeric equality binding.

The following operation table matches the checked bundle and successful custom-binding evidence. `I`, `D`, `S` and `B` mean Integer, Decimal, String and Boolean. Arity is the number of arguments shown. Literal-only restrictions from §3–4 also apply. Production artifact admission must independently enforce the table.

| Function | Arguments → result | Overload ID |
| --- | --- | --- |
| `decimal` | S → D | `keir_decimal_from_string` |
| `toDecimal` | I → D | `keir_decimal_from_integer` |
| `toInteger` | D → I | `keir_integer_from_decimal` |
| `_+_` | D,D → D | `keir_decimal_add` |
| `_-_` | D,D → D | `keir_decimal_subtract` |
| `_*_` | D,D → D | `keir_decimal_multiply` |
| `_/_` | D,D → D | `keir_decimal_divide` |
| `-_` | D → D | `keir_decimal_negate` |
| `_<_`, `_<=_`, `_>_`, `_>=_` | D,D → B | `keir_decimal_less`, `keir_decimal_less_equal`, `keir_decimal_greater`, `keir_decimal_greater_equal`, respectively |
| `round` | D,I,S → D | `keir_decimal_round` |
| `multiplyRounded` | D,D,I,S → D | `keir_decimal_multiply_rounded` |
| `divideRounded` | D,D,I,S → D | `keir_decimal_divide_rounded` |

Integer overloads are `add_int64`, `subtract_int64`, `multiply_int64`, `divide_int64`, `negate_int64`, `less_int64`, `less_equals_int64`, `greater_int64`, `greater_equals_int64`; signatures follow §4. Equality/inequality retain `equals`/`not_equals` under exact semantic checks; canonical Decimal equality passed both engines. `size` uses `size_list` for one named bounded list. The probe verified these IDs and overflow/exactness cases. Replace native Integer division with the tested exact override: filter Java's standard `DIVIDE_INT64` binding and register `divide_int64`; install Node's trusted INT/INT `_/_` override. Keep existing Boolean/String/Timestamp behavior and its error classification.

Pin the final observed/declared table before freezing new artifacts. Both runners reject missing, extra, ambiguous or wrong overload refs, hidden type-map entries, receiver forms, `dyn`, double/uint values, general constructors, internal Decimal field access and unsupported calls. Decimal operations are internal trusted profile functions, not user extension points. Node's planner must resolve the intended object overloads from its own environment, and Java must bind the same signatures. Do not rely on an overload string in metadata to redirect Node execution.

The pinned opaque-message seam passed. Production integration must preserve it and the existing independent gate rather than copy the probe gate as a complete artifact validator. An engine upgrade or representation change needs focused compatibility evidence; neither is needed for this design.

### 8.4 Host/scenario surfaces

Existing bind/prepare/invoke signatures stay unchanged. Export the helpers specified in §3.2. Document numeric strings and explicit helper use in host APIs, adapters, outcomes and consumers. Metadata counts/deadlines stay nonnegative safe integers. Scenario `/4` follows §8.1. Reports compare complete classification/result/error/effects/trace plus ordered calls and protected args.

Host definition/model changes require new pins and normal approval. A new runtime may execute old approved bytes under their original model; it must not silently recompile, relabel, deactivate or transfer approvals. Host catalog adapters and consumers must opt into numeric fields deliberately. Simulation uses those exact compiled bytes against separate fake bindings, not a numeric interpreter implemented by the UI.

## 9. Resource bounds and qualification gates

Keep all existing limits: eight steps, four calls, one possible event WRITE/zero decision WRITEs, 64 nodes and depth 16 per expression, 512 aggregate nodes, 512 source bytes, 16,384 checked bytes, 65,536 descriptor/DCM bytes, 8,192 business-value bytes and 64 trace events. Added constructors, conversions and rounded calls count as AST nodes with their arguments; literal mode/scale arguments count too. No hidden expression expansion may evade the aggregate limit.

Check lexical lengths before numeric parsing: Integer 20 ASCII characters, canonical Decimal 40, explicit normalization helper input 256. For helper input, trim representational zeros as text before constructing the final coefficient; the scan remains bounded even for 256 zeros. Arithmetic operands have ≤38 coefficient digits and scale ≤18. Alignment multiplies by at most `10^18`: aligned operands ≤56 digits, add/subtract temporary ≤57. Product ≤76 digits with provisional scale ≤36. Exact division numerator ≤74 digits (`18+s_b-s_a` is 0–36). Rounded division numerator ≤74 and denominator ≤56 digits; rounding a product uses powers no larger than `10^36`. Quotient rounding can add one carry digit before final normalization/range rejection. No general exponentiation, retry-rescaling or unbounded parse is allowed. Existing list admission caps traversal at 16; size is constant work after admission.

Prepared handles store immutable plans, not invocation values. Reuse descriptor loading, artifact validation and checked-expression preparation. Keep the same four-complete-result-record ceiling. Numeric projection must not materialize skipped results or retain values across requests. Structural arithmetic bounds do not establish CPU preemption or process-memory isolation; existing deadline limitations remain explicit.

The engine seam and initial component measurements passed. Production integration still owes the following evidence; reuse unchanged results and existing runners rather than introduce a second harness or broad reruns:

| Gate | Required evidence |
| --- | --- |
| Engine seam | Same Java-produced checked-expression bundle evaluates Integer min/max, exact versus inexact Integer division, Decimal fields/BINDs and expression-only registry, equality/comparison/custom operations, optional zero and sparse/lazy/error paths in both pinned engines. No float path. Distinguish this bridge test from full Keir artifact admission and installed-package acceptance. |
| Exact helpers and limits | Independently specified normalization vectors in Java/Node; realistic quantity/rate/percentage/amount chains and selected limits, including `0.00001` squared. Exercise exact and rounded intermediates, boundary overflow, zero/nonterminating division and rejected nonzero excess scale. No silent rounding to make a test pass. |
| Preparation and warm execution | Same-run actual Keir nonnumeric, Integer and Decimal rules with comparable shapes, including multi-operation multiplication/division/rounding. Measure bind, prepare, input/result validation/projection and warm invocation separately. Count numeric parses/projections and preparations rather than infer reuse from latency. Reuse the 100-warmup/1,000-sample bounded schedule and explicit total time/case limits; no benchmark-until-convergence loop. |
| Concurrency and isolation | Concurrent Java tasks sharing an immutable prepared plan; Node request-like interleaved invocations and, if used, separately labelled worker throughput. Record tasks/workers, queue/in-flight limits and batch wall time. Single-isolate CPU work is not parallel because promises are used. Interleave distinct owner/value sets and success/failure cases, verifying no leakage or sticky error state. No shared mutable values or unbounded cache. |
| Boundaries and adversarial input | Maximum allowed operands, permitted nodes/depth and four-result retention; malformed/sign/zero/overlength/non-ASCII/overflow text, cap+1 and bounded long-token rejection before parsing. Include selected, masked and unselected errors. Stop on a bound breach or semantic mismatch rather than extending timeouts. |
| Measurement report | Source/dependency revisions, OS/CPU/core count/RAM, runtime versions and flags, workload/case counts, cold versus warm/JIT/GC state, per-case p50/p95/max, throughput and memory or allocations. Separate sampled peak RSS/heap from before/after retained heap and bytes allocated; report unsupported measurements honestly. Record concurrent batch throughput and latency, repeated-batch memory trends and checksums of independently verified outcomes. |
| Investigation | Use results to find actual parsing/allocation/preparation/arithmetic bottlenecks. No arbitrary throughput or latency target, 2× threshold or flaky performance assertion. Runner/bridge timings do not predict a whole web app: host calls, databases and application concurrency may dominate. Any optimization needs measured benefit and preserved isolation. |
| CI fit | Measure full cold-cache job plus package/consumer/companion path against its configured ceiling before delivery; retain existing hard timeouts and stop on overrun. Do not add serial broad reruns after equivalent accepted evidence. |

Component timings and cross-runtime results are recorded below. Full-runtime, package and application claims await their own evidence. Both runtimes remain release requirements.

### 9.1 Smallest production adaptation

Keep one private numeric codec/kernel per runtime, reused by boundary validation, public helpers and CEL bindings. Reuse the qualified arithmetic and independent vectors, not the probe's evaluator, candidate switch, counters or supervisor as production code. Compiler canonical-literal validation stays independent; do not add a compiler→runtime dependency to share a few checks.

1. Extend each invocation's existing state with a small typed-projection map keyed by its immutable binding/result ID. Validate complete external records once on entry, preserving their safe canonical snapshot; project needed records/scalars once and reuse that projection across expressions. Let the private codec return the parsed value with canonical text to projection in the same pass, rather than parse again immediately after validation. Do not rebuild all eight probe declarations for a nonnumeric rule. Guard checks precede projection lookup; false guarded results create no slot. Fresh successful CALL results get new slots only after receipt settlement and full validation. Immutable prepared descriptors/programs remain reusable across concurrent invocations.
2. Parse Integer once to long/bigint and retain it internally. Preserve Decimal messages through direct BIND reuse. Keep canonical host output separately from the internal activation so public callbacks and outcomes receive only canonical strings. Do not pass host-owned mutable maps/messages into the engine or cache validation of a mutable external object.
3. **Explicit initial limit:** the only executed common Decimal bridge is the canonical protobuf `value:string` with operand/result reparsing. Keep that small bridge initially; do not claim that projection reuse makes coefficient parsing once-only. The reports count 49,641 plain-model operand/result parses per engine in the final normalized run. Per-expression literal constructors and computed Decimal results also incur parsing/formatting. The prototype does not isolate the time attributable solely to parsing.
4. In the bounded full-runtime slice, measure repeated use of the same Decimal input/BIND, conversion counters, allocation and warm invocation beside the nonnumeric/Integer controls. Remove repeated boundary projection first. If Decimal reparsing remains material, qualify only the smallest invocation-owned memo path described below against the same vectors and concurrent isolation checks. Do not build a generic cache or change wire/artifact identity for this private optimization.

Pinned source inspection narrows that follow-up. Java [`Program.eval(Map, CelFunctionResolver)`](https://github.com/google/cel-java/blob/v0.14.0/runtime/src/main/java/dev/cel/runtime/Program.java) and [`CelLateFunctionBindings`](https://github.com/google/cel-java/blob/v0.14.0/runtime/src/main/java/dev/cel/runtime/CelLateFunctionBindings.java) support per-evaluation bindings: a bounded invocation-owned identity map of Decimal message→normalized coefficient/scale could be captured by late-bound numeric operations and reused across that invocation's expressions. Verify planner resolution without prebound numeric functions and memo retention bounds before adopting it. This API path was not exercised by the probe.

Node [`plan`](https://github.com/bufbuild/cel-es/blob/v0.6.1/packages/cel/src/plan.ts) captures an environment and accepts only activation bindings at evaluation. [`celFunc`](https://github.com/bufbuild/cel-es/blob/v0.6.1/packages/cel/src/func.ts) receives reflected messages; [`toCel`](https://github.com/bufbuild/cel-es/blob/v0.6.1/packages/cel/src/value.ts) wraps the existing message without requiring a text round-trip. The smallest memo candidate is private parsed coefficient/scale metadata attached once to a runtime-created, then immutable Decimal message, reused through `ReflectMessage.message`; computed messages can carry their already-known coefficients. Qualify reflection/equality/projection preservation and forbid trusting such metadata on external input. No rebuilding plans per invocation, module-level value map, ThreadLocal, global weak cache or mutable prepared closure. These source-supported optimization candidates are not measured guarantees or prerequisites for the first production codec slice. Any retained reparsing must remain visible in final performance evidence.

### 9.2 Passing executable evidence

[Hosted run 43401342724](https://github.com/rore/keir-core/actions/runs/43401342724), revision `32dffe40be2fa31d1bc8de3a85902195e99cace9`, passed in a reported 34-second job; the workload report records `2380.992976 ms`. The receipt and all 16 listed evidence files were byte-length/SHA-256 verified during this design update. Bundle SHA-256: `cf5347eff8f249a5053bd4370ca25d521edca9e6cf1515e1091ba6699c15ca00`; fixture SHA-256: `d900f19fdfc66cb441f71b0968aec64d616117591e7005697e3af6a3ad50df56`.

- Linux, Intel Xeon 6975P-C, four visible/available logical CPUs, host RAM 16,463,405,056 bytes; Java 21.0.13 and Node 24.22.2, with §8.1 engine pins.
- Each engine passed 297 case outcomes (99 × three candidates), 24 normalization helpers, eight timed cases and three concurrent value families. Java rejected 12 forbidden sources; Node independently gated the emitted checked bundle. Both checked independent expectations, deliberate wrong-oracle rejection, custom invocation counters, exact division, Decimal equality, direct BIND, expression-only registry, optional zero and mixed/lazy errors. Parity was additional evidence.
- The same deterministic Java CheckedExpr bytes were consumed by Node. The bundle has 104 expressions, including BIND subexpressions and concurrent families. Required small-rate product was `0.0000000001` for fixed18/normalized18 and ERROR for fixed9. Selected normalized18 also passed the range/carry examples that fixed18 rejects.
- Each timed phase used 100 warmups and 1,000 validated samples; three representative programs had 30 warmed preparations. Java used four executor threads sharing one immutable Program; Node used four same-isolate lanes with serialized CPU work. Both checked distinct lane values and recovery after errors.
- Limits remained 45 seconds/direct workload child, 1 MiB captured output, 256 MiB engine heap, 512 MiB observed RSS screen, 135 seconds/workload, 20 minutes/job and 10 MiB sanitized evidence. Process reports passed closure checks. RSS/heap limits are screens, not hard memory containment.

Illustrative measured p50s, in microseconds (component phases, not production invocation):

| Measurement | Java | Node |
| --- | ---: | ---: |
| Exact helper `100.00` normalization | 1.921 | 0.558 |
| Common eight-declaration projection | 8.925 | 3.457 |
| Decimal-add prepared engine evaluation | 11.864 | 15.052 |
| Decimal-add evaluation plus result validation | 19.253 | 3.614 |
| Decimal-add projection/evaluation/result validation | 35.343 | 6.275 |
| Warm single Decimal-add preparation, including gate/registry setup | 642.336 | 156.414 |

The apparently faster later Node phase shows why these short, fixed-order/JIT-sensitive samples cannot isolate parsing cost or rank engines. All families include a common eight-declaration projection; even the scalar control is not a legacy whole-runtime invocation. Decimal operands/results are reparsed. Java allocation counts cover the measuring thread only; Node reports allocation unsupported. No application throughput, native-host guarantee or steady-state allocation conclusion follows. Reuse these measurements to target production counters, not set arbitrary latency thresholds.

The earlier [run 43294967050](https://github.com/rore/keir-core/actions/runs/43294967050) stopped on the harness's expected-rejection catch bug. Its failure record remains history; the corrected passing run supersedes that blocker. No local numeric/build execution was performed for this design update.

## 10. Original implementation and release plan — completed

The design choices are settled for implementation. The developer owns the production Work Record and integration; this design-only update creates none. Reuse completed engine/fixture review and qualify the new production paths. End-to-end feature authority removes routine approval loops, not verification, local execution holds or separate live-environment/deployment scope.

| Slice | Exact principal files / smallest change | Exit evidence and dependency |
| --- | --- | --- |
| 1. Profile/codec/compiler + minimal both-runtime vertical path | `compiler-java/src/main/java/dev/keir/compiler/{Dcm,Expressions,KeirCompiler}.java`; `runtime-java/src/main/java/dev/keir/runtime/{Artifact,CheckedCel,KeirRuntime,Profile}.java`; `runtime-node/src/{validation,model,cel,index}.js`; new private `runtime-java/src/main/java/dev/keir/runtime/NumericValues.java` and `runtime-node/src/numeric.js`; the four new schema files in §8.1 | One selected codec/kernel per runtime, public helper exports, both 0.5 tuples, exact descriptor reconstruction, literals/operators/conversions/rounding/list size. Compile once and run identical artifact bytes in both runtimes. Retained authoring and historical execution tests accompany tuple changes, not a later cleanup. Keep `StrictData`/canonical JSON unchanged. |
| 2. Complete all invocation and admission paths | Same runtime/compiler files; existing `compiler-java/src/test/java/dev/keir/compiler/{DcmTest,KeirCompilerTest,GuardedResultCompilerTest}.java`, `runtime-java/src/test/java/dev/keir/runtime/{CheckedCelTest,ProducerCompatibilityTest,GuardedResultRuntimeTest,PublicScenarioTest}.java` and matching `runtime-node/test/` suites; new `conformance/keir-4/` vectors | §11 full outcomes, forged admission, numeric optional/guarded values, protected args, complete adapter validation, handler/return and receipt precedence. Reuse existing scenario drivers. Add focused numeric tests only where existing tests cannot express the obligation. |
| 3. Invocation-local projection and bounded actual-runtime measurements | `KeirRuntime.java`, `CheckedCel.java`, `runtime-node/src/{index,cel}.js`; bounded numeric cases alongside existing `conformance/resource-qualification/` runtime drivers | §9.1 projection reuse and direct BIND, bind/prepare once, conversion/operand-parse counters, representative full-runtime warm/concurrent measurements and isolation. Retained reparsing stays explicit; optimize only measured cost using a focused pinned-API delta test. Never run excluded stress controls just to obtain these measurements. |
| 4. Runnable Core examples and scenario consumers | New `examples/numeric/` stock/quote model, source, artifact and scenario bundles; Java/Node host consumers under existing `examples/java-host/` and `examples/node-host/`; `examples/java-host/src/main/java/dev/keir/examples/{ScenarioData,ScenarioHost}.java`; `examples/node-host/{verify-scenarios,read-simulation,effect-simulation}.mjs`; `scripts/check-keir-contracts.py` and schema/corpus selectors | Both §6 examples compile and execute with pinned hashes; quote consumer validates the amount field. Scenario `/4` routes both 0.5 tuples; old scenarios retain old routing. Production normalization signatures/error classes/idempotence and all numeric boundaries pass outside raw CEL. |
| 5. Packages, companion and documentation | `scripts/{release-core,verify-core-release,authoring-companion}.py`, their existing tests/manifests, root/module POMs and Node package metadata, exact source-revision manifest, affected docs in §11 | Coherent 0.5 compiler/Java/Node packages; installed consumers outside source with no fallback; historical and both 0.5 profile smokes; matching immutable companion edition/payload. Measure cold job against its ceiling. Reuse release machinery and reviewed source; no new release framework. |
| 6. Daymark adoption and real journey | The exact host seams in §7 plus `tests/e2e/` and existing compatibility/native tests in `keir-demo-app` | Qualified packages precede adoption. New definition pins, natural-language Studio authoring, independent scenarios, simulation, approval, separate activation, native create/reload/isolation. Keep historical selections and every supported create path. Deployment remains separate. |

Reuse existing `scripts/check-keir-contracts.py`, conformance/parity drivers, release/consumer verifier, companion builder and their manifests. The source assessment identifies producer-0.4.0 special cases and hardcoded fixture producers in `scripts/verify-core-release.py`; add exact new-profile smoke selection while retaining the old guarded-result installed smoke. Add selected new corpus/schema entries and package-smoke outcomes; do not create a parallel qualification framework. Numeric behavior is incomplete until both runtimes execute identical artifact bytes. Release acceptance includes the tested Daymark journey, not a live deployment.

## 11. Acceptance matrix and documentation inventory

### Required executable obligations

Use the selected normalized contract throughout. The passing seam supplies representative arithmetic/bridge evidence; it does not replace complete Keir admission, receipts, retained authoring, packages or application journeys.

| Area | Cases and assertions |
| --- | --- |
| Exact representations and helpers | int64 min/max, max+1/min-1, values above `2^53`, zero/-0, Decimal `10^38-1`, ±`10^-18`, rejected nonzero 19th fractional place and 39th integral digit; accepted `10^-10` and 30-digit integers; rejected 39-digit normalized coefficient even when fractional. Canonical/invalid syntax and JSON/YAML equivalence; helper 256/257 tokens, leading plus/zeros, signed zero, trailing fraction zeros, idempotence and exact-value preservation; safe public error classes. Reject native Number/Double/BigDecimal/bigint API values. |
| Round trips and typing | Each type through input, protected and agent args, READ/WRITE results, BIND, handler and root return; exact canonical outcome text. Reject mixed Integer/Decimal, numeric/String, nominal casts and numeric list declarations. |
| Expression-only Decimal registration | Use a DCM `/4` with no Decimal field or parameter: Boolean/String output and a bounded String-list input. Compile `decimal("1") < decimal("2")`, bind `toDecimal(size(input.tags))` to an intermediate Decimal BIND `count`, then evaluate `count == decimal("2")`. With two supplied tags, both comparisons return true. Verify constructor/type registration, BIND activation and identical complete outcomes through compiler, Java and Node, then actual installed packages. Retained recipe `/2` descriptor bytes must remain unchanged. |
| Arithmetic | Every operator at normal/negative/zero/boundary values; minimum negation and division overflow; exact vs inexact quotient/product; nonterminating division; intermediate overflow that a later operation would cancel. |
| Rounding | Every admitted mode on positive/negative below-half/tie/above-half, zero, already-exact values, scale 0/18; normalized carry success and rounded multiplication/division output overflow; UNNECESSARY failure; dynamic/unknown mode and scale -1/19 rejection. Verify rounded multiplication directly, not via failing exact multiplication. |
| Conversion | Both directions, exact integral Decimal, fraction rejection, Decimal beyond int64 and round-trip equality; no implicit or String conversion. |
| Optional/guarded | Absent versus zero, required omission, `null`, direct proof versus separate-BIND proof, exact guard versus alias/negation, pre-CALL snapshots, false/true/false reuse and independent prepared handles. |
| Laziness/errors/effects | Selected/unselected arithmetic error; AND/OR masking in both operand orders; condition error; false CALL skips invalid arithmetic arguments; validation cannot be masked; invalid numeric success after CONFIRMED; unknown write; terminal handler failure with original cause. Assert full receipts/traces and no later dispatch. |
| Mixed temporal/numeric errors | Combine existing Timestamp overflow and new numeric arithmetic errors under AND/OR in both operand orders. Assert complete escaping classification/error parity and ordinary false/true masking against the existing policies, with no sticky global error flags or changed error policy. |
| List size | Empty, duplicate entries, cap and cap+1, maximum 16 and zero-cap lists; no String/map/receiver size; no guarded list escape; count is supplied entries only. |
| Artifact hostility | Forged metadata/type/ref maps, overload IDs, internal Decimal selectors/construction, extra descriptors/imports, raw uint/double constants, old-producer relabeling, reserved-name regression, all mixed tuples and environment mismatch. Reject before callback. |
| Compatibility | Execute unchanged producer 0.2.0/0.3.0/0.3.1/0.4.0 goldens and prior outcomes. Compile unchanged current source `/2` + DCM `/3` under 0.5 to artifact `/4`/recipe `/2`; preserve model digest/descriptor bytes and 0.4 defaults/guards. Include nominal String types named Integer/Decimal, old identifiers, rejected numeric operations and both scenario `/4` branches. New compiler output gets new pins/approval. Old runtime rejects it; crossed/relabelled tuples fail before callbacks. |
| Complete examples | Both §6 programs: typical, no-op/deny, boundary, invalid, rejection and uncertainty cases. Numeric scalar outcomes must work with matching host consumers, not only direct CEL evaluation. |
| Installed distribution | Actual packaged compiler produces the same pinned new artifacts; installed Java/Node execute numeric and historical cases from outside source with no sibling fallback. Compare complete independent expectations and original key forwarding, and verify package checksums/licenses. |

New test cases must assert independently written expectations, not regenerate expected outcomes from implementation. Use the existing compiler/runtime suites and shared corpus; add bounded arithmetic-oracle vectors rather than redundant per-helper suites. Default tests remain deterministic and fast; opt-in resource measurements stay separate. Follow [testing guidance](../testing.md) and [release process](../releasing.md).

### Documentation and schema surfaces

| Surface | Required update when implementing |
| --- | --- |
| `docs/specs/language.md`, `domain-model.md`, `workflow-expressions.md` | Numeric syntax/types/operators, bounds, exact division and explicit rounding; optional/guarded examples; unsupported units and list forms. |
| `compiler-artifact.md`, `host-model-binding.md`, `runtime.md` | Exact tuple/descriptor/overload table, canonical strings, host API examples, numerical error/lazy behavior and receipt preservation. |
| New versioned source/DCM/artifact/scenario schemas and schema checker | Closed identities, numeric literal forms and typed-value hooks; structural schemas do not replace semantic range checks. Preserve old schemas. |
| `docs/agents/` customer and host guides, authoring companion manifest/compatibility | A new supported edition with numeric examples and diagnostics. Preserve immutable older editions and archive bytes; include exact matching schemas and new fixture pins. No package compatibility row until real qualification. |
| `examples/README.md`, new human-readable Core examples, conformance README | Two complete pinned model/source/artifact/scenario bundles and expected outputs; concise business explanation, explicit host boundaries and source/package evidence labels. |
| Root/docs/specs indexes, installation/release/testing docs, module READMEs | Route to supported numeric contracts and correct packages; avoid release numbers in ordinary usage prose except actual compatibility/install guidance. |
| Core/demo AGENTS and role guidance | Audit for changed responsibilities; existing routing/ownership rules likely remain valid. Record why unchanged rather than editing policy for a type addition. |
| Daymark discovery, authoring examples/instructions, summaries/review cards | Include in the tested release journey after package/definition gates; advertise only qualified size support. Do not advertise Decimal fields that the host lacks. Deployment is separate. |

Measure completion by: (1) every admitted operation and scalar position has cross-runtime expected-outcome evidence; (2) all rejected forms fail at the specified boundary with no unauthorized dispatch; (3) unchanged historical bytes/outcomes pass; (4) resource screens have actual bounded reports and no unresolved regression; (5) actual installed packages and the version-matched companion pass; (6) independent technical and documentation review findings are resolved; (7) release/adoption claims match their actual evidence and authority.

## 12. Original risks and stop conditions — historical

No unresolved design decision blocks the first production slice. Numeric bounds, helper grammar, operation set and both 0.5 authoring tuples are selected above. Do not reopen them merely because production tests are not written yet.

The remaining obligations are concrete: production independent admission, retained-authoring/default/descriptor preservation, complete receipts and optional/guard behavior, public helpers/packages, actual-runtime conversion/performance and the tested Daymark journey. Parse-once Decimal coefficients are not yet qualified; §9.1 names the minimal initial bridge, exact remaining cost and bounded follow-up. This limitation must stay visible rather than becoming an untested optimization claim.

Return only a real contradiction to design: pinned APIs cannot implement a required semantic path, compatibility would change historical bytes/meaning, a performance fix would introduce unsafe shared state, or a new host/native guarantee is needed. Routine implementation fixes and measured local optimization stay within feature delivery. Preserve the opaque bridge, per-operation checks, exact tuple selection and fresh invocation authority without adding another interpreter, profile farm or host framework.
