<a id="shared-conformance"></a>
# Shared Keir specification conformance

The compiler and both runtimes use shared fixtures to verify that the same rule produces the same observable behavior. The suites use checked-in source, host models, compiled artifacts and independently written expectations. The checks compare results, calls, traces and any write evidence.

- [Run the current example](../examples/README.md) to compile and execute one rule in both runtimes.
- [Keir 1 fixtures](keir-1/README.md) cover single-write behavior, verification scenarios and raw rejection vectors.
- [Contract workflow](../.github/workflows/keir-contract.yml) runs the structural checker, compiler and runtime tests, and the packaged host comparison.
- [Testing evidence](../docs/testing.md#current-single-write-behavior-verification) records the results and their scope.

<a id="earlier-profiles"></a>
## Compatibility and historical fixtures

The [read-and-decide fixtures](read-and-decide/README.md) preserve a prior compiler and runtime corpus, including grant exhaustion and prepared reuse. The [CEL qualification](cel-profile/README.md) records the earlier expression bridge.

The [original single-write simulation fixture](single-effect/README.md) records an earlier artifact and 25 simulation scenarios.

The [generic-hook fixtures](generic-hooks/README.md) retain earlier compatibility cases. Exact selectors and wire identifiers are documented in the [implementation reference](../docs/internal/implementation-reference.md).

Compare typed values, exact call order and arguments, guard decisions, effects and termination classes. Keep compiler rejection tests, expression interoperability, runner conformance and real host adapter guarantees distinct.

The [testing plan](../docs/testing.md) records the current checks and earlier evidence. Update fixtures and independent expectations together when the contract changes.

The older design fixtures remain in [the baseline](../docs/internal/design-baseline/public/examples/). They are illustrative and have pending host certifications; they are not passing executable tests. Maintained fixtures here require explicit provenance and acceptance checks.
