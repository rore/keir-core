# Testing instructions

Load for code/test/contract changes. Apply to core and demo; keep demo tests local and workflow management in core.

Status: initial plan. [CEL qualification](../conformance/cel-profile/README.md) runs; complete product regression/coverage/contract CI remains implementation work.

Current checkpoint commands and measured evidence, before Java runtime conformance:

| Command from repository root | Result |
| --- | --- |
| `MAVEN_USER_HOME` set to `.tools/maven`; `./mvnw -o -Dmaven.repo.local=.tools/maven/repository -B -ntp clean verify` (Windows: `mvnw.cmd`) | 30 compiler tests; 20.61 seconds including first canonical compilation/packaging with populated isolated cache. Java runtime is still an empty scaffold. Compiler coverage baseline remains pending. |
| `npm run --prefix runtime-node test:coverage` | Default module + shared-corpus regression: 152 tests in 1.68 seconds; runtime source coverage 100% lines, 95.51% branches, 100% functions. |
| `node --experimental-test-coverage --test-coverage-include=conformance/read-and-decide/run-node.mjs --test conformance/read-and-decide/node.test.mjs` | 69 tests over 61 cases and harness regressions in 1.70 seconds; runner coverage 100% lines/branches/functions. |
| `node --test examples/node-host/test.mjs` after the documented pack/install | 1 packaged-host test; 0.26–0.51 seconds warm, 9.54 seconds on first observed worker startup. Fresh Java CLI output also passed this test. |

Offline Maven requires a previously populated local cache; dependency/bootstrap time is separate. The package's LF checkout rules and host lockfile preserve tarball integrity across checkouts; never disable integrity checks to make the sample install.

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

- `docs/specs/**` and `conformance/read-and-decide/**`.
- Compiler `StrictDataTest.java`, `DcmTest.java`, `KeirCompilerTest.java` in `compiler-java/src/test/java/dev/keir/compiler/`.
- Node `canonical.test.js`, `cel.test.js`, `prepare.test.js`, `invoke.test.js` in `runtime-node/test/`.
- Add exact Java runtime contract-test and selection/CI runner paths at implementation acceptance.

Do not blanket-protect helper tests or diagnostic benchmarks. Add later behavior only with accepted features; probe results do not certify full runtime guarantees.

## Agent Workflow

Planned in `agent-redline-policy.yaml`, **not active**:

```yaml
behaviorContracts:
  protection: workflow
  paths:
    - docs/specs/**
    - conformance/read-and-decide/**
    # Add exact module contract-test and selection-runner paths.
  verification: keir-contract
```

Activate after the actual proposed `keir-contract` PR check executes selected tests and the combined harness is qualified. Never register a nonexistent check. First-increment acceptance includes this wiring. Keep workflow protection advisory: no new branch protection, CODEOWNERS, required PRs or mandatory Work Records; it is not merge enforcement.

Classify protected edits as equivalent, coverage-only or requirement-change. Weakening/removing/skipping/deferring/redefining an obligation needs explicit user approval for exact before/after behavior. Record evidence through Agent Workflow when used. Never weaken a contract to accommodate incomplete implementation.
