# Testing instructions

Load for code/test/contract changes. Apply to core and demo; keep demo tests local and workflow management in core.

Status: read and effect profiles pass combined local verification. The full clean Java coverage build passes 164 tests (47 compiler, 95 runtime, 22 host) in 33.57 seconds. Default Node regression passes 274 tests in 2.05 seconds, and both installed-host tests pass. The combined verifier reproduces both artifacts with four compiler processes, compares 61 read cases/52 invocations and 25 effect scenarios, and checks all four packaged-host batches. Effect-host CI acceptance is pending; the [read CI checkpoint](https://github.com/rore/keir-core/actions/runs/40921421530) remains accepted. Workflow-level contract-policy protection remains [proposed](internal/workflow/core-contract-ci-proposal.md#behavioral-contract).

Effect checkpoint coverage: compiler 795/840 lines and 624/729 branches; Java runtime 869/875 lines and 808/939 branches; Node source 100% lines and 96.88% branches. The six-file Node runtime archive matches installed source with exact integrity and unchanged registry pins. Coverage does not certify real adapters, hard resource containment or production readiness.

Effect regression covers conservative single-write admission, guard availability, original key identity, fresh grants, native deadline races, retained receipts, handler/result boundaries, ordered simulation mismatches and consumer validation. The shared supplemental handler artifact is Java-compiled; both runtime suites consume the same bytes. The following older measurements remain historical read-profile baselines.

The [first combined effect CI attempt](https://github.com/rore/keir-core/actions/runs/41111171874) was cancelled after ten minutes inside `EffectRuntimeTest`. Its snapshot race test could block its own caller when a completion callback ran inline. The test now starts invocation independently and bounds the release latch, preserving all receipt/mutation assertions. All 12 affected tests pass locally in 1.10 seconds; replacement CI acceptance is pending. No runtime or CI timeout was changed.

Accepted read-profile baseline commands and measurements (2026-10-29):

| Command from repository root | Result |
| --- | --- |
| `MAVEN_USER_HOME` set to `.tools/maven`; `./mvnw -o -Dmaven.repo.local=.tools/maven/repository -B -ntp -Pcoverage clean verify` (Windows: `mvnw.cmd`) | 92 tests: 31 compiler, 56 Java runtime, 5 Java host; 35.40 seconds including clean compilation/packaging with populated isolated cache. |
| `npm run --prefix runtime-node test:coverage` | Default module + shared-corpus regression: 154 tests in 3.80 seconds; runtime source coverage 100% lines, 95.75% branches, 100% functions. |
| `node --experimental-test-coverage --test-coverage-include=conformance/read-and-decide/run-node.mjs --test conformance/read-and-decide/node.test.mjs` | 69 tests over 61 cases and harness regressions in 1.70 seconds; runner coverage 100% lines/branches/functions. |
| `node --experimental-test-coverage --test-coverage-include=examples/node-host/run.mjs --test examples/node-host/test.mjs` after the documented pack/install | 1 packaged-host test in 0.38 seconds; 81.63% lines, 75% branches, 100% functions. The CLI is exercised separately by combined verification. |
| `node conformance/read-and-decide/verify.mjs` | Identical fresh compiler bytes, complete Java/Node transport parity for 61 cases/52 invocations, both four-case packaged-host batches, installed Node source identity and Java compiler/parser exclusion passed. |

Offline Maven requires a previously populated local cache; dependency/bootstrap time is separate. The package's LF checkout rules and host lockfile preserve tarball integrity across checkouts; never disable integrity checks to make the sample install.

JaCoCo baselines: compiler 711/756 lines (94.05%), 501/607 branches (82.54%); Java runtime 697/704 lines (99.01%), 600/717 branches (83.68%); Java host 64/66 lines (96.97%), 52/52 branches (100%). Remaining misses include defensive invariant/CEL failure paths, the Java main wrapper and very long native timer rearming. Node host unit coverage excludes its CLI wrapper; the verifier exercises that wrapper without a coverage agent. No coverage floor or hard containment guarantee is inferred.

## Fast feedback

- During development run affected tests; before integrating semantic changes run the complete fast regression suite.
- Default Maven/npm tests select deterministic unit/component/shared conformance cases. Use bounded fixtures, fake adapters and logical clocks; batch compiler/JVM startup.
- Initial warm targets: focused tests within 5 seconds; complete fast suite within 60 seconds. Measure duration/slowest cases; report cold dependency setup separately. These are investigation budgets, not timing assertions.
- Investigate slowdowns: redundant builds/startup, sleeps/network, oversized fixtures and leaked workers. Fix causes instead of increasing timeouts.
- Necessary slow tests use an explicit command/profile with a reason and trigger. Run relevant stress/fuzz/resource/real-infrastructure checks for affected changes and releases. Classify by measured cost, not test name. Moving required checks out of default regression cannot silently weaken coverage.
- Product test failures fail their CI job; advisory governance does not make tests advisory.

## Coverage and regression

- Test observable behavior at the smallest useful boundary. Include valid, rejection, boundary/+1 and failure cases; reproduce bugs before fixes and retain regressions.
- Report component line/branch coverage; inspect uncovered authority/error paths. Establish measured baselines with first implementations; do not invent percentages or lower agreed thresholds to pass.
- Keep expectations independent of implementation. Refactor helpers without losing assertions; regenerating goldens does not approve new behavior.

## Initial protected contract

Use the reviewed [read profile acceptance matrix](specs/host-integration.md#acceptance-fixtures):

| Obligation | Regression assertions |
| --- | --- |
| Source/DCM | Strict parsing, types/nominals/enums, dataflow, grants/protected bindings, unsupported constructs |
| Artifact/admission | Deterministic bytes, external trust/pins, malformed/tampered rejection before dispatch |
| Portability | Same Java-compiled artifact; Java/Node results, ordered calls/arguments/traces and consumption |
| Authority/failures | Current grants, identity isolation, authorization, invalid/late results and safe failure |
| Bounds | Exact limits/overflow, deadline/clock behavior, no later dispatch or truncation |

Protect these paths when their verification is live:

- `docs/specs/**`, `conformance/read-and-decide/**` and `conformance/single-effect/**`.
- Compiler `StrictDataTest.java`, `DcmTest.java`, `KeirCompilerTest.java`, `SingleEffectCompilerTest.java` in `compiler-java/src/test/java/dev/keir/compiler/`.
- Node `effect-prepare.test.js`, `effect-invoke.test.js`, `effect-handler.test.js`, `canonical.test.js`, `cel.test.js`, `prepare.test.js`, `invoke.test.js`, `scalars.test.js`, `multi-call.test.js` and their actual Java-generated fixtures.
- Java artifact/CEL/clock/invocation/data/shared/multi-call/effect tests, host consumers and their tests, simulation harness regressions and test-selection/parity runners. The [proposal](internal/workflow/core-contract-ci-proposal.md#behavioral-contract) lists exact paths.

Do not blanket-protect helper tests or diagnostic benchmarks. Add later behavior only with accepted features; probe results do not certify full runtime guarantees.

## Agent Workflow

Planned in `agent-redline-policy.yaml`, **not active**:

```yaml
behaviorContracts:
  protection: workflow
  paths:
    - docs/specs/**
    - conformance/read-and-decide/**
    - conformance/single-effect/**
    # Add exact module contract-test and selection-runner paths.
  verification: keir-contract
```

The installed `keir-contract` job runs selected tests and the qualified combined harness on relevant main pushes and PRs. Its first main-push run passed; no PR run has been exercised. First-increment acceptance includes this CI wiring. Activating the separate policy block requires governance approval; do not register a nonexistent check. Keep workflow protection advisory: no new branch protection, CODEOWNERS, required PRs or mandatory Work Records; it is not merge enforcement.

Classify protected edits as equivalent, coverage-only or requirement-change. Weakening/removing/skipping/deferring/redefining an obligation needs explicit user approval for exact before/after behavior. Record evidence through Agent Workflow when used. Never weaken a contract to accommodate incomplete implementation.
