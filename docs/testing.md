# Testing instructions

## Effect/4 local verification

The corrected `/4` implementation passed focused, shared, source/installed and packaged local verification on 2026-11-04. Java clean coverage build: 271 tests (66 compiler, 172 runtime, 33 host), zero failures/errors/skips, 28.134 seconds. Coverage: compiler 857/902 lines and 760/875 branches; runtime 1417/1429 lines and 1349/1563 branches; host 554/577 lines and 750/888 branches. Node: 454 tests, zero failures/skips, 4.8357868 seconds, 100% lines and 98.30% branches. Fourteen public-API tests against the installed tarball passed in 0.723 seconds; four installed-host tests passed in 0.723 seconds.

Combined verification passed with exact Java/Node/source/installed parity: 61 legacy read cases/52 invocations, 25 legacy effect, 9 generic read, 19 generic effect, 19 effect/3 and 11 effect/4 scenarios; 28 legacy qualification and 25 host-bound protocol cases; 30 fresh compiler processes and 12 packaged-host batches. Each Node and Java source/installed or packaged driver passed eight local/loopback native-host cases covering confirmed, denied, wrong-key and lost-response outcomes. Separate packaged read-simulation verification passed 19 reports and two recompiled artifacts; schema validation passed 29 fixtures/reports.

The eight Node archive files match source and installed contents (six source files, README and package metadata). Independent result/simplicity review and manager integration remain pending. These controlled hosts do not establish native-provider qualification; no hosted CI result is claimed.

### Historical `/3` qualification verification (2026-11-04)

The legacy `/3` local protocol run passed 250 Java tests (64 compiler, 155 runtime, 31 host) and 424 Node tests, with no skips. Its transport checked 28 independently authored facts and compared full outcomes, calls, traces and consumer receipts between Java and Node using the same pinned programs in separate simulation and real bindings. The `/3` simulation corpus had 19 rows. These controlled fakes do not qualify native SQLite or deployments.

Legacy `/3` coverage included bounded canonical escaping before snapshot, qualification descriptor/identity/mode validation, immutable capture, rejected native Promise containment, revocation during pending authorization, callback-time budget expiry with no effect intent, original key identity and retained CONFIRMED/UNKNOWN winner evidence. Compiler coverage was 853/898 lines and 743/856 branches; Java runtime 1341/1354 lines and 1258/1475 branches; Node source 100% lines and 97.95% branches. These figures do not cover `/4` host implementation permission checks and are not `/4` results.

Load for code/test/contract changes. Apply to core and demo; keep demo tests local and workflow management in core.

Local `/2` alignment verification on implementation `933c5c8` (2026-10-31) passes alongside preserved `/1` regression. The clean Java coverage build passes 216 tests (58 compiler, 128 runtime, 30 host) in 52.470 seconds. Default Node regression passes all 367 tests in 4.175 seconds, without failures or skips; all three installed-host tests pass in 0.967 seconds. The combined verifier checks 61 legacy read cases/52 invocations, 25 legacy effect scenarios, nine generic read/nineteen generic effect rows, sixteen fresh compiler processes and eight packaged-host batches. Independent technical result review approves this local scope; production qualification and release remain open.

Historical `/2` coverage (implementation `933c5c8`, 2026-10-31): compiler 848/893 lines and 724/835 branches; Java runtime 1117/1129 lines and 1043/1223 branches; Java host 490/494 lines and 678/784 branches; Node source 100% lines and 97.73% branches. These measurements predate `/4`. Coverage does not certify real adapters, decoded-heap containment or production readiness.

The main-push [product contract CI](https://github.com/rore/keir-core/actions/runs/41280457363) and [Agent Workflow CI](https://github.com/rore/keir-core/actions/runs/41280457420) pass on `55c0f27`, including the generic alignment and preserved legacy corpus. The [effect-host check](https://github.com/rore/keir-core/actions/runs/41113166355) on `ff69257` and [read checkpoint](https://github.com/rore/keir-core/actions/runs/40921421530) remain historical `/1` evidence. Workflow-level contract-policy protection remains [proposed](internal/workflow/core-contract-ci-proposal.md#behavioral-contract); no enforcement setting changed.

Effect regression covers conservative single-write admission, guard availability, original key identity, fresh grants, native deadline races, retained receipts, handler/result boundaries, ordered simulation mismatches and consumer validation. The shared supplemental handler artifact is Java-compiled; both runtime suites consume the same bytes. The following older measurements remain historical read-profile baselines.

The [first combined effect CI attempt](https://github.com/rore/keir-core/actions/runs/41111171874) was cancelled after ten minutes inside `EffectRuntimeTest`. Its snapshot race test could block its own caller when a completion callback ran inline. The test now starts invocation independently and bounds the release latch, preserving all receipt/mutation assertions. All 12 affected tests pass locally in 1.10 seconds; the [replacement CI check](https://github.com/rore/keir-core/actions/runs/41113166355) passes. No runtime or CI timeout was changed.

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

### Generic-hook regression

The [`/2` acceptance matrix](specs/generic-hook-capability-contract.md#examples-and-rejection-obligations) is exercised by separate [generic fixtures](../conformance/generic-hooks/README.md) and compiler/Java/Node component negatives; the legacy corpus remains unchanged. Preserve these regression obligations:

- Run two programs against one unchanged hook/DCM: select a different already-published compatible READ, then repeat READs within the aggregate bound. Omit advanced policy at hook, admission and invocation. Do not add sample-specific named grants to make these pass.
- Distinguish omitted/empty policy and quota maps from `allow: []`; prove inherited restrictions, explicit allow/deny/quota narrowing, zero quotas, rejected widening, unknown IDs, null/malformed controls and per-call authorization. No source-controlled policy or binding can widen scope.
- Exercise boundary/+1 aggregate counts independently of optional named quotas; reject a WRITE on a read hook, zero effect ceiling and a second potential WRITE even behind false/exclusive guards. Reject unsupported lifecycle and real effect admission.
- Preserve `/1` required-map omission/rejection semantics and golden bytes. Reject mixed profile/format pairs and old `grants` in `/2`; test deliberate legacy-map conversion, including empty maps, separately from host-approved widening.
- Compile each new program once and execute those exact bytes through Java, Node and packaged hosts. Compare typed outcomes, ordered calls and protected arguments, traces, effect receipts and consumption. Preserve winner/clock/late-response/handler guarantees and native key identity; do not duplicate expensive race qualification solely for a policy change.
- Keep the legitimate host-defined payload unchanged across programs with different authored project/title constants. Test proposed explicit nominal literals: declared nominal String type, malformed/unknown/non-nominal annotation, wrong nominal assignment/equality, Unicode/byte bounds, rejection of dynamic casts and protected-key types, source/artifact digest changes and renewed selection, resource denial and Java/Node parity. Existing `/1` fixture-injected `input.projectId/title` are not evidence for this authoring path; no configuration channel is added.
- Use the [complete ownership example](specs/generic-hook-example.md#acceptance-obligations) as a planned vector: changing `project1` to `project2` changes only source/artifact, keeps DCM/hook/input/context/host code fixed, and changes the original task's outcome from a possible write to NO_OP. Verify both runtimes use the same artifact for each revision and still enforce current authorization.
- Keep the task-creation capability generic: `tasks.create` receives project/title, while eligibility and follow-up intent stay in the program. Test exact create arguments and separate protected event/key context; do not require a follow-up-specific source-task argument or assume an undeclared relationship. This updates planned `/2` vectors, not `/1` compatibility goldens.

Combined-invalid regressions also preserve safety/policy-before-binding diagnostics, approval-field validation before model parsing, descriptor-safe accessor rejection and unused WRITE catalog selection without read WRITE authority. These are technical regression obligations, not a product-value experiment. Simulation does not establish real authorization, idempotency, atomicity or recovery.

### Execution feedback

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
- For UI changes, exercise actual entry points with existing saved work, not only empty fixtures or helper shortcuts. Verify visible outcomes and retained state. Use an unbriefed business task for substantial interaction changes; fix observed confusion and repeat it. Test totals and screenshots alone do not establish usability.

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

- `docs/specs/**`, `conformance/read-and-decide/**`, `conformance/single-effect/**` and `conformance/generic-hooks/**`.
- Compiler `StrictDataTest.java`, `DcmTest.java`, `KeirCompilerTest.java`, `SingleEffectCompilerTest.java` in `compiler-java/src/test/java/dev/keir/compiler/`.
- Node `effect-prepare.test.js`, `effect-invoke.test.js`, `effect-handler.test.js`, `canonical.test.js`, `cel.test.js`, `prepare.test.js`, `invoke.test.js`, `scalars.test.js`, `multi-call.test.js` and their actual Java-generated fixtures.
- Java artifact/CEL/clock/invocation/data/shared/multi-call/effect tests, host consumers and their tests, simulation harness regressions and test-selection/parity runners. The [proposal](internal/workflow/core-contract-ci-proposal.md#behavioral-contract) lists exact paths.
- Generic compiler, artifact/context/runtime, model-binding, policy, reference-execution and combined-invalid preflight tests; generic host capacity/consumer/simulation tests. They run in default regression; this list does not activate the separate policy block.

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

The installed `keir-contract` job runs selected tests and the qualified combined harness on relevant main pushes and PRs. Its first main-push run passed; no PR run has been exercised. First-increment acceptance includes this CI wiring. Activating the separate policy block requires governance approval; do not register a nonexistent check. Governance CI remains advisory, without new branch protection, CODEOWNERS or required PRs. Work Records are required by the [working process](internal/workflow/current-process.md) for code/mixed/governance tasks, with documentation-only exemptions; this is not platform push enforcement.

Classify protected edits as equivalent, coverage-only or requirement-change. Weakening/removing/skipping/deferring/redefining an obligation needs explicit user approval for exact before/after behavior. Record evidence through Agent Workflow for in-scope work. Documentation applicability does not waive these obligations. Never weaken a contract to accommodate incomplete implementation.
