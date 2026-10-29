# Scalar CEL qualification

This standalone probe qualifies the first read-and-decide expression bridge. It is not a Keir compiler, artifact loader, runtime, DCM semantic checker or host adapter. See the [owning roadmap feature](../../roadmap/features/bound-cel-execution.md) for remaining scope.

The measured bridge works: Java dynamically constructs a proto3 record descriptor, checks expressions, serializes deterministic `dev.cel.expr.CheckedExpr` bytes and evaluates them. Node consumes those same descriptor/expression bytes using real CEL-ES. No Node source parser or substitute evaluator is used.

| Component | Tested pin |
| --- | --- |
| JDK / Node | OpenJDK 21.0.12 / 24.14.1 |
| Maven / wrapper | 3.10.0 / 3.3.4 |
| CEL-Java, including its protobuf bridge | `dev.cel:cel:0.14.0` |
| Java protobuf | `com.google.protobuf:protobuf-java:4.35.1` |
| Node CEL / CEL schemas | `@bufbuild/cel:0.6.1` / `@bufbuild/cel-spec:0.6.1` |
| Node protobuf | `@bufbuild/protobuf:2.6.2` |
| Fixture JSON reader | `jackson-databind:2.20.0` |

Node dependencies have a committed npm lockfile. Java dependency versions come from the pinned POMs; protobuf is also pinned directly. Build plugin versions are explicit. These are qualification pins, without a production support or dependency security claim.

Run from the repository root with the repository-local Maven wrapper, JDK and Node versions listed above. The probe has a standalone POM selected with `-f`. If the wrapper is unavailable, use an already provisioned Maven of the tested version; no global installation is required.

```powershell
# MAVEN_USER_HOME applies only to this build process; do not persist it.
$env:MAVEN_USER_HOME = "$PWD\.tools\maven"
.\mvnw.cmd -B -ntp -f conformance/cel-profile/pom.xml "-Dmaven.repo.local=$PWD\.tools\m2" package
java -jar conformance/cel-profile/target/scalar-probe-0.1.0.jar conformance/cel-profile/fixtures.json conformance/cel-profile/target/probe
npm.cmd ci --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
npm.cmd run --prefix runtime-node test:cel
```

The Java executable checks its results, rejected expressions and projection guards using explicit assertions that do not require `-ea`. Packaging alone does not execute the probe. For a Node-only replay of the committed Java-produced fixture, after installing the same lockfile:

```powershell
node conformance/cel-profile/qualify.mjs conformance/cel-profile/evidence/bundle.json
```

`fixtures.json` owns the inputs and expected values. `evidence/bundle.json` contains the shared descriptor, original checked bytes, Java results and rejection evidence. `evidence/node-report.json` and `java-metrics.json` retain one successful local run. Fresh runs write under ignored `target/`. Two independent Java processes produced identical bundle SHA-256 `2bd898abefa7b6baa16f7518779ee96c341a3142a1e27abf0671728d00a63e33`. The probe emits LF, and the local Git attribute preserves those fixture bytes on checkout. This is expression-fixture determinism, not full Keir artifact canonicalization or authentication.

The descriptor follows the architect's candidate recipe: file `keir_read.proto`, syntax `proto3`, package `keir.read`, record `Task`, fields sorted by ASCII name and numbered from one. Its fields are `enabled` (bool), `id`, `projectId`, `status`, `title` (string); each explicit `json_name` equals its field name. This probe constructs that descriptor directly; generation from a validated DCM remains implementation work.

The run passed 24 Java/Node value comparisons across 12 expressions, 16 rejected expressions and 17 invalid business inputs in each projection gate. It covers field access, status equality/inequality, Boolean operations, literal string enum classification, UTF-8, false values and short circuit. The closed fixture status values are OPEN/DONE and classification values ACTIVE/COMPLETED. Observed overload IDs are exactly `equals`, `not_equals`, `logical_and`, `logical_or`, `logical_not`, `conditional`.

The fixture `nominal_erasure` deliberately proves that a ProjectId-shaped string compares with a raw string at CEL level. Its `requiresKeirOverlay` flag marks this as underlying representation evidence, not an expression admitted by Keir's nominal rules. Protobuf strings do not retain nominal identity or closed enum membership. The compiler overlay and runtime boundary checks remain necessary; this probe does not implement them for arbitrary DCMs. Only literal enum conditional branches are qualified here, not the candidate's complete scalar assignment/branch provenance rules or multiple immutable variables.

| Candidate gate exercised | Evidence |
| --- | --- |
| 64 expanded AST nodes | Accepted exactly 64; rejected 65 |
| AST depth 16, root counted as one | Accepted 16; rejected 17 |
| CEL source 512 UTF-8 bytes | Accepted 512; rejected 513 |
| Business/literal string 256 UTF-8 bytes | Accepted 128 `é`; rejected 129 `é`, and 257 ASCII bytes |
| Required closed scalar record | Missing/null fields, wrong Boolean, invalid enum, empty ID, unknown field and malformed surrogate rejected |
| Decoded CheckedExpr 16,384 / descriptor 65,536 bytes | Size-plus-one buffers rejected; malformed CheckedExpr rejected |
| Serialized business value 8,192 bytes | Configured in both gates; exact valid saturation is not demonstrated by this five-field fixture |

These are probe admission gates. No whole-artifact aggregate limit, general protobuf decoding bound, raw JSON/YAML parser or full forged-artifact admission is qualified. The harness consumes trusted local Java output; changing its local hashes is not authentication. Rejection messages in the evidence are raw library diagnostics, not a proposed product error surface.

Important observed behavior:

- Without the strict gate, an omitted proto3 string field evaluates as `""` in both runtimes. Empty String and `false` must remain valid present values where the DCM permits them. The gate validates before message construction; it never relies on scalar protobuf presence.
- An absent `task` variable produces an evaluation error in both runtimes; `false && task.enabled` can still short circuit. Invalid invocation data must be rejected before evaluating even a short-circuited expression.
- Erasing Java's checked type/reference maps still permits CEL-ES evaluation. The probe's separate AST/type/overload gate rejects the corrupted structure. Java remains the checker, and the descriptor registry and function environment must be fixed by the admitted profile. The maps do not independently certify execution safety.
- CEL-Java's planner supplies heterogeneous numeric comparisons by default. Supplying a custom options object requires retaining `enableHeterogeneousNumericComparisons(true)`, or runtime construction fails. Numbers remain excluded by this probe's checked-AST policy.
- Repeated unary negations simplify during parsing. Source punctuation counts are not expanded AST node/depth counts.

Actual local measurements, without a latency guarantee:

| Workload | Observation |
| --- | --- |
| Node warmed classification, 1,000 samples | p95 3.3 microseconds |
| Node warmed 64-node expression, 1,000 samples | p95 12.9 microseconds |
| Node 256-byte late-mismatch string comparison, 1,000 samples | p95 1.4 microseconds |
| Java warmed scalar / 64-node expression, 1,000 evaluations each | 12.77 / 58.82 ms total |
| Node worker startup including imports/preparation | 242.39 ms |
| Prewarmed worker classification round trip, 200 samples | p50 0.0767 ms; p95 0.1750 ms |
| Worker termination of 10,000,000-call CEL batch | Supervisor timer observed after 28.70 ms; termination then took 4.57 ms; batch did not complete |

The worker batch executes actual approved CEL repeatedly; it is not one expensive admitted expression and does not establish a 20 ms per-expression deadline. Timing varies with machine load/JIT/GC. Memory reports are snapshots, not peaks; RSS covers the process rather than one worker. A 32 MiB old-generation worker setting is not complete process-memory isolation. No production CPU, memory, hard deadline or multi-tenant containment guarantee follows from these measurements.

Java sets `comprehensionMaxIterations(0)` defensively and disables macros. The pinned [option documentation](https://github.com/cel-expr/cel-java/blob/v0.14.0/common/src/main/java/dev/cel/common/CelOptions.java) describes a total synchronous comprehension iteration cap and excludes the async interpreter. Collections are not admitted or exercised here, so this is source-inspected configuration, not tested comprehension support. The synchronous planner API exposes no wall-clock deadline parameter; interruption cancellation is unverified. Node's worker contains only pure evaluation and owns no adapters.

The next dependency is acceptance of the exact public read profile and environment pins, followed by a separate runtime/compiler/conformance assignment. Keep int64, collections, time, effects, arbitrary functions and demo application work excluded. Existing broader roadmap resource and conformance requirements remain open.

Provenance: the earlier interop harness was recovered from [commit 3023f3e14199cb555f6ba247991438ba0358e5ea](https://github.com/rore/bear-account-demo/tree/3023f3e14199cb555f6ba247991438ba0358e5ea), archive `harness.tar.xz` SHA-256 `1a9318715ef0d5ace551cdd18564e04b2047b38bcb57c28c775a3890415e11e5`. Only its dynamic descriptor, CEL API and standalone POM patterns were adapted; its broad collection/time/int64/signature harness was not imported. Primary implementation references: [CEL-Java v0.14.0](https://github.com/cel-expr/cel-java/tree/v0.14.0), [planner option defaults](https://github.com/cel-expr/cel-java/blob/v0.14.0/runtime/src/main/java/dev/cel/runtime/CelRuntimeFactory.java), [CEL-ES v0.6.1 plan implementation](https://github.com/bufbuild/cel-es/blob/v0.6.1/packages/cel/src/plan.ts), and [package peer metadata](https://github.com/bufbuild/cel-es/blob/v0.6.1/packages/cel/package.json).
