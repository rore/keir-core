# Java compiler

Planned independently consumable Java library and CLI. Accept untrusted IR plus a trusted DCM, check syntax, CEL policy, nominal/unit types, dataflow, grants and bounds, and emit a portable artifact or structured diagnostics.

Compilation does not approve activation or certify host guarantees. Start with the [first milestone](../roadmap/board.md).

The build scaffold uses JDK 21, Maven 3.10.0 through the repository wrapper, and Maven Compiler Plugin 3.14.1 with `release=21`. These versions are documented by [Apache Maven](https://maven.apache.org/download.cgi) and the [compiler plugin](https://maven.apache.org/plugins-archives/maven-compiler-plugin-3.14.1/plugin-info.html). The `dev.keir` Maven coordinates are local development coordinates; release coordinates remain undecided.

From the repository root, in a disposable PowerShell process:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -pl compiler-java -am verify
```

On POSIX:

```sh
MAVEN_USER_HOME="$PWD/.tools/maven" ./mvnw -Dmaven.repo.local=.tools/maven/repository -pl compiler-java -am verify
```

Omit `-pl compiler-java -am` to build both Java modules. The wrapper downloads its pinned, checksum-verified Maven distribution; these commands keep its cache and dependencies under ignored `.tools/` without a global Maven installation. No compiler implementation or tests exist yet; a successful build currently verifies the scaffold only.
