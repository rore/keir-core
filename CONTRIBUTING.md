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

See the [testing guide](docs/testing.md) for shared conformance, package verification and opt-in resource checks.

## Release packages

Use [GitHub Releases](https://github.com/rore/keir-core/releases) to distribute Keir packages. Cut a release when a consumer such as Daymark needs a tested version, or when a fix or compatibility change is ready for users. Follow the [release checklist](docs/releasing.md).

## Prepare a change

- Keep each change focused. Describe what changes for users and include the checks you ran.
- For semantic changes, update the affected compiler and runtimes, shared fixtures and compatibility documentation together. Java and Node must agree on the observable outcomes from the same artifact.
- Keep framework changes here and application behavior in Daymark. Both repositories use the [shared roadmap](roadmap/board.md).
- Maintainers use the [development workflow](docs/internal/workflow/current-process.md) for review, records and integration. That guide defines which changes need a Work Record and how CI findings are handled.

License choice is pending. Resolve it before inviting public contributions or publishing packages.
