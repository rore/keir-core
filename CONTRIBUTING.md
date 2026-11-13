# Contributing

Start with the [quickstart](docs/quickstart.md) to see a rule run. Check the [Keir specification](docs/specs/README.md) for the behavior you are changing and the [shared roadmap](roadmap/board.md) for planned work.

## Build and test

Use JDK 21 and Node.js 24 with npm. The Maven wrapper and npm can use checkout-local caches. On a POSIX shell, run the default suites from the repository root:

```sh
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository verify
npm ci --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
npm test --prefix runtime-node
```

On Windows, run from PowerShell:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" verify
npm.cmd ci --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
npm.cmd test --prefix runtime-node
```

See [conformance](conformance/README.md) for shared corpus and package checks, and the testing principles below for opt-in resource checks.

## Testing principles

- During development run affected tests; before integrating semantic changes run the complete fast regression suite.
- Default Maven/npm tests select deterministic unit/component/shared conformance cases. Use bounded fixtures, fake adapters and logical clocks; batch compiler/JVM startup.
- Initial warm targets: focused tests within 5 seconds; complete fast suite within 60 seconds. Measure duration/slowest cases; report cold dependency setup separately. These are investigation budgets, not timing assertions.
- Investigate slowdowns: redundant builds/startup, sleeps/network, oversized fixtures and leaked workers. Fix causes instead of increasing timeouts.
- Necessary slow tests use an explicit command/profile with a reason and trigger. Run relevant stress/fuzz/resource/real-infrastructure checks for affected changes and releases. Classify by measured cost, not test name. Moving required checks out of default regression cannot silently weaken coverage.
- Run the opt-in [single-write resource qualification](conformance/resource-qualification/README.md) when collecting compiler and runtime resource evidence for the scalar profile.
- Product test failures fail their CI job; advisory governance does not make tests advisory.


- Test observable behavior at the smallest useful boundary. Include valid, rejection, boundary/+1 and failure cases; reproduce bugs before fixes and retain regressions.
- Report component line/branch coverage; inspect uncovered authority/error paths. Establish measured baselines with first implementations; do not invent percentages or lower agreed thresholds to pass.
- Keep expectations independent of implementation. Refactor helpers without losing assertions; regenerating goldens does not approve new behavior.
- For UI changes, exercise actual entry points with existing saved work, not only empty fixtures or helper shortcuts. Verify visible outcomes and retained state. Use an unbriefed business task for substantial interaction changes; fix observed confusion and repeat it. Test totals and screenshots alone do not establish usability.

Classify protected edits as equivalent, coverage-only or requirement-change. Weakening/removing/skipping/deferring/redefining an obligation needs explicit user approval for exact before/after behavior. Record evidence through Agent Workflow for in-scope work. Documentation applicability does not waive these obligations. Never weaken a contract to accommodate incomplete implementation.

The [original protected-contract proposal](docs/internal/workflow/core-contract-ci-proposal.md#behavioral-contract) records the accepted paths and obligations. It is not an instruction to activate new protection. Use the [conformance guide](conformance/README.md) for current corpus commands and run resource/provider checks only through their explicit opt-in paths.

## Release packages

Use [GitHub Releases](https://github.com/rore/keir-core/releases) to distribute Keir packages. Cut a release when a consumer such as Daymark needs a tested version, or when a fix or compatibility change is ready for users. Follow the [release checklist](docs/releasing.md).

## Prepare a change

- Keep each change focused. Describe what changes for users and include the checks you ran.
- For semantic changes, update the affected compiler and runtimes, shared fixtures and compatibility documentation together. Java and Node must agree on the observable outcomes from the same artifact.
- Keep framework changes here and application behavior in Daymark. Both repositories use the [shared roadmap](roadmap/board.md).
- Maintainers use the [development workflow](docs/internal/workflow/current-process.md) for review, records and integration. That guide defines which changes need a Work Record and how CI findings are handled.

Keir Core is licensed under the [Apache License 2.0](LICENSE).
