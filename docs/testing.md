# Testing instructions

Use [Contributing](../CONTRIBUTING.md#build-and-test) to build the repository and run the default Java and Node.js suites. The [shared conformance guide](../conformance/README.md) describes the checks that compare compiled artifacts and complete outcomes across runtimes. Run the [resource harness](../conformance/resource-qualification/README.md) separately when investigating resource limits.

<a id="effect4-verification"></a>
## Current single-write behavior verification

> Internal engineering reference: this page records implementation and compatibility evidence. See [the Keir specification](specs/README.md) for the supported behavior. Exact profile selectors and wire identifiers are implementation details; see [the implementation reference](internal/implementation-reference.md).

The current single-write implementation passed focused, shared, source/installed and packaged local verification on 2026-11-04. Java clean coverage build: 271 tests (66 compiler, 172 runtime, 33 host), zero failures/errors/skips, 28.134 seconds. Coverage: compiler 857/902 lines and 760/875 branches; runtime 1417/1429 lines and 1349/1563 branches; host 554/577 lines and 750/888 branches. Node: 454 tests, zero failures/skips, 4.8357868 seconds, 100% lines and 98.30% branches. Fourteen public-API tests against the installed tarball passed in 0.723 seconds; four installed-host tests passed in 0.723 seconds.

Combined verification passed with exact Java/Node/source/installed parity: 61 legacy read cases/52 invocations, 25 legacy effect, 9 generic read, 19 generic effect, 19 earlier qualified-write and 11 current single-write scenarios; 28 legacy qualification and 25 host-bound protocol cases; 30 fresh compiler processes and 12 packaged-host batches. Each Node and Java source/installed or packaged driver passed eight local/loopback native-host cases covering confirmed, denied, wrong-key and lost-response outcomes. Separate packaged read-simulation verification passed 19 reports and two recompiled artifacts; schema validation passed 29 fixtures/reports.

The eight Node archive files matched source and installed contents (six source files, README and package metadata). Independent result and simplicity review passed, and the reviewed implementation was integrated. On 2026-11-04, the hosted contract check passed on `decaa2820c35e496d56e0e81bbf99d7e4e5ef2a0`, including Java and Node regression and coverage, packaged hosts, shared runtime comparisons, read simulation and offline schema checks. [GitHub Actions run 41899243002](https://github.com/rore/keir-core/actions/runs/41899243002)

This evidence covers the tested source, packages and controlled hosts. Native-provider qualification, production resource containment and public package release have separate acceptance criteria. [Resource qualification](../roadmap/features/runtime-resource-qualification.md) · [Public release plan](../roadmap/features/public-package-release.md)

<a id="earlier-profile-results"></a>
<a id="historical-verification"></a>
<a id="historical-3-qualification-verification-2026-11-04"></a>
<a id="historical-host-protocol-qualification-2026-11-04"></a>
<a id="historical-2-and-1-verification"></a>
<a id="historical-compatibility-verification"></a>

See [earlier verification and coverage results](internal/history/verification.md) for measurements from previous contracts.

## Fast feedback

### Generic-hook regression

The [generic hook acceptance matrix](specs/generic-hook-capability-contract.md#examples-and-rejection-obligations) is exercised by separate [generic fixtures](../conformance/generic-hooks/README.md) and compiler/Java/Node component negatives; the legacy corpus remains unchanged. Preserve these regression obligations:

- Run two programs against one unchanged hook/DCM: select a different already-published compatible READ, then repeat READs within the aggregate bound. Omit advanced policy at hook, admission and invocation. Do not add sample-specific named grants to make these pass.
- Distinguish omitted/empty policy and quota maps from `allow: []`; prove inherited restrictions, explicit allow/deny/quota narrowing, zero quotas, rejected widening, unknown IDs, null/malformed controls and per-call authorization. No source-controlled policy or binding can widen scope.
- Exercise boundary/+1 aggregate counts independently of optional named quotas; reject a WRITE on a read hook, zero effect ceiling and a second potential WRITE even behind false/exclusive guards. Reject unsupported lifecycle and real effect admission.
- Preserve original required-map omission/rejection semantics and golden bytes. Reject mixed profile/format pairs and old `grants` in catalog-bound formats; test deliberate legacy-map conversion, including empty maps, separately from host-approved widening.
- Compile each new program once and execute those exact bytes through Java, Node and packaged hosts. Compare typed outcomes, ordered calls and protected arguments, traces, effect receipts and consumption. Preserve winner/clock/late-response/handler guarantees and native key identity; do not duplicate expensive race qualification solely for a policy change.
- Keep the legitimate host-defined payload unchanged across programs with different authored project/title constants. Test proposed explicit nominal literals: declared nominal String type, malformed/unknown/non-nominal annotation, wrong nominal assignment/equality, Unicode/byte bounds, rejection of dynamic casts and protected-key types, source/artifact digest changes and renewed selection, resource denial and Java/Node parity. Original fixture-injected `input.projectId/title` are not evidence for this authoring path; no configuration channel is added.
- Use the [complete ownership example](specs/generic-hook-example.md#acceptance-obligations) as a planned vector: changing `project1` to `project2` changes only source/artifact, keeps DCM/hook/input/context/host code fixed, and changes the original task's outcome from a possible write to NO_OP. Verify both runtimes use the same artifact for each revision and still enforce current authorization.
- Keep the task-creation capability generic: `tasks.create` receives project/title, while eligibility and follow-up intent stay in the program. Test exact create arguments and separate protected event/key context; do not require a follow-up-specific source-task argument or assume an undeclared relationship. Use the catalog-bound vectors and preserve the original compatibility goldens.

Combined-invalid regressions also preserve safety/policy-before-binding diagnostics, approval-field validation before model parsing, descriptor-safe accessor rejection and unused WRITE catalog selection without read WRITE authority. These are technical regression obligations, not a product-value experiment. Simulation does not establish real authorization, idempotency, atomicity or recovery.

### Execution feedback

- During development run affected tests; before integrating semantic changes run the complete fast regression suite.
- Default Maven/npm tests select deterministic unit/component/shared conformance cases. Use bounded fixtures, fake adapters and logical clocks; batch compiler/JVM startup.
- Initial warm targets: focused tests within 5 seconds; complete fast suite within 60 seconds. Measure duration/slowest cases; report cold dependency setup separately. These are investigation budgets, not timing assertions.
- Investigate slowdowns: redundant builds/startup, sleeps/network, oversized fixtures and leaked workers. Fix causes instead of increasing timeouts.
- Necessary slow tests use an explicit command/profile with a reason and trigger. Run relevant stress/fuzz/resource/real-infrastructure checks for affected changes and releases. Classify by measured cost, not test name. Moving required checks out of default regression cannot silently weaken coverage.
- Run the opt-in [single-write resource qualification](../conformance/resource-qualification/README.md) when collecting compiler and runtime resource evidence for the scalar profile.
- Product test failures fail their CI job; advisory governance does not make tests advisory.

## Coverage and regression

- Test observable behavior at the smallest useful boundary. Include valid, rejection, boundary/+1 and failure cases; reproduce bugs before fixes and retain regressions.
- Report component line/branch coverage; inspect uncovered authority/error paths. Establish measured baselines with first implementations; do not invent percentages or lower agreed thresholds to pass.
- Keep expectations independent of implementation. Refactor helpers without losing assertions; regenerating goldens does not approve new behavior.
- For UI changes, exercise actual entry points with existing saved work, not only empty fixtures or helper shortcuts. Verify visible outcomes and retained state. Use an unbriefed business task for substantial interaction changes; fix observed confusion and repeat it. Test totals and screenshots alone do not establish usability.

## Initial protected contract

Use the reviewed [original read-only acceptance matrix](internal/history/original-read-only-host.md#acceptance-fixtures):

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
