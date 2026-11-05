<a id="shared-conformance"></a>
# Shared Keir specification conformance

The compiler and both runtimes use shared fixtures to verify that the same rule produces the same observable behavior. The suites use checked-in source, host models, compiled artifacts and independently written expectations. The checks compare results, calls, traces and any write evidence.

- [Run the current example](../examples/README.md) to compile and execute one rule in both runtimes.
- [Single-write corpus](generic-hooks/README.md) covers single-write behavior and its host protocol.
- [Combined verification](read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) runs the broader compatibility suite and checks independently packaged hosts.
- [Testing evidence](../docs/testing.md#effect4-verification) records the results and their scope.

<a id="earlier-profiles"></a>
## Compatibility and historical fixtures

The [read-and-decide fixtures](read-and-decide/README.md) contain maintained source, DCM, deterministic Java compiler output and a 61-case shared corpus executed by both runtimes. The combined verifier compares full results, calls, authorization, traces and consumption and checks independent packaged hosts. The supplemental two-read fixture covers grant exhaustion and prepared reuse in both module suites. The [CEL qualification](cel-profile/README.md) records the earlier expression bridge. Both runtimes consume the same artifact bytes in the installed product CI check.

The [single-write simulation fixture](single-effect/README.md) adds a Java-compiled artifact and 25 shared simulation scenarios. Local combined verification compares full outcomes, traces, ordered calls and EVENT_REPORT consumption across Java, Node and both packaged hosts. Real-mode admission remains rejected; fake adapters establish no persistence or duplicate-safety guarantee.

The [generic-hook fixtures](generic-hooks/README.md) retain earlier compatibility cases. Exact selectors and wire identifiers are documented in the [implementation reference](../docs/internal/implementation-reference.md).

Compare typed values, exact call order and arguments, guard decisions, effects and termination classes. Keep compiler rejection tests, expression interoperability, runner conformance and real host adapter guarantees distinct.

The [testing plan](../docs/testing.md) identifies protected read-and-decide and single-write obligations and their fast regression corpus. The CEL probe is qualification evidence with separate benchmarks, not the complete product contract. Conformance fixtures and independent expectations must evolve with accepted behavior, never be weakened to accommodate incomplete implementations.

The older design fixtures remain in [the baseline](../docs/internal/design-baseline/public/examples/). They are illustrative and have pending host certifications; they are not passing executable tests. Maintained fixtures here require explicit provenance and acceptance checks.
