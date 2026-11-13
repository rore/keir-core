<a id="earlier-profile-results"></a>
# Historical verification

<a id="historical-3-qualification-verification-2026-11-04"></a>
### Historical host protocol qualification (2026-11-04)

The earlier qualification run passed 250 Java tests (64 compiler, 155 runtime, 31 host) and 424 Node tests, with no skips. Its transport checked 28 independently authored facts and compared full outcomes, calls, traces and consumer receipts between Java and Node using the same pinned programs in separate simulation and real bindings. Its simulation corpus had 19 rows. These controlled fakes do not qualify native SQLite or deployments.

Coverage for that protocol included bounded canonical escaping before snapshot, qualification descriptor/identity/mode validation, immutable capture, rejected native Promise containment, revocation during pending authorization, callback-time budget expiry with no effect intent, original key identity and retained CONFIRMED/UNKNOWN winner evidence. Compiler coverage was 853/898 lines and 743/856 branches; Java runtime 1341/1354 lines and 1258/1475 branches; Node source 100% lines and 97.95% branches. These figures cover the earlier protocol. See [current single-write verification](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/testing.md#effect4-verification) for host implementation permission checks.

<a id="historical-2-and-1-verification"></a>
### Historical compatibility verification

Local catalog-bound verification on implementation `933c5c8` (2026-10-31) passes alongside the original regression suite. The clean Java coverage build passes 216 tests (58 compiler, 128 runtime, 30 host) in 52.470 seconds. Default Node regression passes all 367 tests in 4.175 seconds, without failures or skips; all three installed-host tests pass in 0.967 seconds. The combined verifier checks 61 legacy read cases/52 invocations, 25 legacy effect scenarios, nine generic read/nineteen generic effect rows, sixteen fresh compiler processes and eight packaged-host batches. Independent technical result review approves this local scope; production qualification and release remain open.

Historical catalog-bound coverage (implementation `933c5c8`, 2026-10-31): compiler 848/893 lines and 724/835 branches; Java runtime 1117/1129 lines and 1043/1223 branches; Java host 490/494 lines and 678/784 branches; Node source 100% lines and 97.73% branches. These measurements predate the current single-write contract. Coverage does not certify real adapters, decoded-heap containment or production readiness.

The main-push [product contract CI](https://github.com/rore/keir-core/actions/runs/41280457363) and [Agent Workflow CI](https://github.com/rore/keir-core/actions/runs/41280457420) pass on `55c0f27`, including the generic alignment and preserved legacy corpus. The [effect-host check](https://github.com/rore/keir-core/actions/runs/41113166355) on `ff69257` and [read checkpoint](https://github.com/rore/keir-core/actions/runs/40921421530) remain evidence for the original contracts. Workflow-level contract-policy protection remains [proposed](../workflow/core-contract-ci-proposal.md#behavioral-contract); no enforcement setting changed.

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


## Core 0.3.0 verification baseline

The following historical release record was retired from the current testing entry page. Its source and scope are unchanged.

## Current single-write behavior verification

Package verification follows the [release checklist](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/releasing.md). Each release records its qualified source, package checks and verified downloads.

The historical [Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0) qualification baseline used source [`b232944`](https://github.com/rore/keir-core/commit/b2329440dd2a25acc547f3ebfea214748d912022). Its [contract CI run](https://github.com/rore/keir-core/actions/runs/42576060993) passed and compared 39 shared scenarios and 51 raw verification vectors across Java and Node, including post-commit effects and read-only pre-operation decisions. That run checked normalized complete outcomes for exact parity, alongside compiler, runtime and installed-host tests.

Use the [current corpus guide](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/conformance/keir-3/README.md) for fixture layout and commands. The retained [Keir 2 corpus](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/conformance/keir-2/README.md) checks the older execution contract; the [compatibility matrix](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/specs/compiler-artifact.md#source-and-artifact-identity) distinguishes current authoring from retained execution. Test totals belong to their source revision and run, rather than describing every later checkout.

The [release installation guide](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/installation.md#verification-boundary) records a separate check of the packaged compiler and both runtimes outside the source repositories. These checks cover controlled hosts. Real service guarantees and production resource containment require separate qualification; see [resource qualification](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/conformance/resource-qualification/README.md).

<a id="effect4-verification"></a>
### Historical Core 0.1 verification

The 2026-11-05 baseline passed 144 Java tests, 49 Node runtime tests, seven installed Node teaching-host tests, 31 structural fixtures and 19 rejection cases. Java and Node matched 20 scenarios, 45 raw verification vectors and eight packaged scenario groups. A Windows resource soak ran 3,000 timed invocations per runtime. These are historical measurements, not current test totals. [Original contract run](https://github.com/rore/keir-core/actions/runs/42072587793)

<a id="earlier-profile-results"></a>
<a id="historical-verification"></a>
<a id="historical-3-qualification-verification-2026-11-04"></a>
<a id="historical-host-protocol-qualification-2026-11-04"></a>
<a id="historical-2-and-1-verification"></a>
<a id="historical-compatibility-verification"></a>

See [earlier verification and coverage results](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/internal/history/verification.md) for measurements from previous contracts.
