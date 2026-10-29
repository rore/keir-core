# Java runtime

Initial-scope embedded runtime for Java hosts. Load and prepare compatible trusted artifacts, execute bounded instructions through host adapters, and expose deterministic simulation and traces with fake adapters.

Consume the same artifacts and conformance scenarios as the Node runtime. Do not parse source IR or require the compiler at execution time. No runtime implementation exists yet.

The JDK 21 Maven scaffold is independent of the compiler module: it declares no compiler dependency. From the repository root, in a disposable PowerShell process:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl runtime-java -am verify
```

On POSIX:

```sh
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl runtime-java -am verify
```

See the [compiler build notes](../compiler-java/README.md) for pinned tooling, isolated caches and the root reactor command. A successful build currently verifies the scaffold only; runtime behavior and conformance remain unimplemented.
