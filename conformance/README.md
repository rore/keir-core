# Shared conformance

The [read-and-decide fixtures](read-and-decide/README.md) contain maintained source, DCM and deterministic Java compiler output. The [CEL qualification](cel-profile/README.md) records the actual expression bridge. Shared runtime scenarios and complete Java/Node comparisons are still in progress. Both runtimes must execute the same artifact bytes; compiling a different artifact for each runtime does not prove portability.

Compare typed values, exact call order and arguments, guard decisions, effects and termination classes. Keep compiler rejection tests, expression interoperability, runner conformance and real host adapter guarantees distinct.

The [testing plan](../docs/testing.md) identifies protected read-and-decide obligations and their fast regression corpus. The CEL probe is qualification evidence with separate benchmarks, not the complete product contract. Conformance fixtures and independent expectations must evolve with accepted behavior, never be weakened to accommodate incomplete implementations.

The older design fixtures remain in [the baseline](../docs/internal/design-baseline/public/examples/). They are illustrative and have pending host certifications; they are not passing executable tests. Maintained fixtures here require explicit provenance and acceptance checks.
