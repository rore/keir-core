# Core contract CI proposal

Status: product CI workflow approved and installed on 2026-10-29. The [first cloud run](https://github.com/rore/keir-core/actions/runs/40921421530) passed on `69d01b6` in 47 seconds. The behavioral-contract policy block remains a separate governance proposal. The product check, `keir-contract`, runs compiler, both runtimes, shared expectations and packaged hosts. Test failures fail the job. Advisory governance, direct main pushes, optional Work Records and current branch settings remain in place.

## Workflow

Installed `.github/workflows/keir-contract.yml`:

```yaml
name: Keir contract
on:
  push:
    branches: [main]
    paths:
      - compiler-java/**
      - runtime-java/**
      - runtime-node/**
      - conformance/**
      - examples/**
      - docs/specs/**
      - pom.xml
      - .mvn/**
      - mvnw
      - mvnw.cmd
      - .github/workflows/keir-contract.yml
  pull_request:
    paths:
      - compiler-java/**
      - runtime-java/**
      - runtime-node/**
      - conformance/**
      - examples/**
      - docs/specs/**
      - pom.xml
      - .mvn/**
      - mvnw
      - mvnw.cmd
      - .github/workflows/keir-contract.yml
  workflow_dispatch:
permissions:
  contents: read
concurrency:
  group: keir-contract-${{ github.ref }}
  cancel-in-progress: true
jobs:
  keir-contract:
    name: keir-contract
    runs-on: ubuntu-latest
    timeout-minutes: 10
    env:
      MAVEN_USER_HOME: ${{ github.workspace }}/.tools/maven
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '21'
      - uses: actions/setup-node@v7
        with:
          node-version: '24'
          package-manager-cache: false
      - uses: actions/cache@v6
        with:
          path: |
            .tools/maven
            .tools/npm-cache
          key: ${{ runner.os }}-keir-${{ hashFiles('pom.xml', 'compiler-java/pom.xml', 'runtime-java/pom.xml', 'examples/java-host/pom.xml', 'runtime-node/package-lock.json', 'examples/node-host/package-lock.json', '.mvn/wrapper/maven-wrapper.properties') }}
          restore-keys: ${{ runner.os }}-keir-
      - name: Java regression and coverage
        run: ./mvnw -B -ntp -Dmaven.repo.local=.tools/maven/repository -Pcoverage verify
      - name: Install pinned Node dependencies
        run: npm ci --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
      - name: Node regression and coverage
        run: npm run --prefix runtime-node test:coverage
      - name: Pack and test the independent Node host
        run: |
          mkdir -p .tools/packages
          npm pack ./runtime-node --pack-destination .tools/packages --cache .tools/npm-cache --ignore-scripts
          npm ci --prefix examples/node-host --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
          node --experimental-test-coverage --test-coverage-include=examples/node-host/run.mjs --test examples/node-host/test.mjs
      - name: Compare artifacts, runtimes and packaged hosts
        run: node conformance/read-and-decide/verify.mjs
```

Source versions: official [checkout](https://github.com/actions/checkout), [Java setup](https://github.com/actions/setup-java), [Node setup](https://github.com/actions/setup-node) and [cache](https://github.com/actions/cache) usage. Java 21 and Node 24 match the implemented checkpoint; this adds no broader support matrix. Dependency bootstrap is separate from warm regression timing. Cache project tooling and package downloads; build current sources and retain integrity verification. Roadmap and ordinary documentation edits do not trigger this product job; specification edits do.

## Behavioral contract

After the real `keir-contract` check executes successfully, add this block to `agent-redline-policy.yaml` in a separate governance change, preserving existing zones and shadow mode:

```yaml
behaviorContracts:
  protection: workflow
  paths:
    - docs/specs/**
    - conformance/read-and-decide/**
    - conformance/single-effect/**
    - conformance/cel-profile/evidence/bundle.json
    - conformance/cel-profile/fixtures.json
    - compiler-java/src/test/java/dev/keir/compiler/StrictDataTest.java
    - compiler-java/src/test/java/dev/keir/compiler/DcmTest.java
    - compiler-java/src/test/java/dev/keir/compiler/KeirCompilerTest.java
    - compiler-java/src/test/java/dev/keir/compiler/SingleEffectCompilerTest.java
    - runtime-java/src/test/java/dev/keir/runtime/ArtifactTest.java
    - runtime-java/src/test/java/dev/keir/runtime/CheckedCelTest.java
    - runtime-java/src/test/java/dev/keir/runtime/InvocationClockTest.java
    - runtime-java/src/test/java/dev/keir/runtime/KeirRuntimeTest.java
    - runtime-java/src/test/java/dev/keir/runtime/RuntimeDataTest.java
    - runtime-java/src/test/java/dev/keir/runtime/SharedConformanceTest.java
    - runtime-java/src/test/java/dev/keir/runtime/MultiCallConformanceTest.java
    - runtime-java/src/test/java/dev/keir/runtime/ScenarioDriver.java
    - runtime-java/src/test/java/dev/keir/runtime/EffectAdmissionTest.java
    - runtime-java/src/test/java/dev/keir/runtime/EffectArtifactTest.java
    - runtime-java/src/test/java/dev/keir/runtime/EffectClockTest.java
    - runtime-java/src/test/java/dev/keir/runtime/EffectHandlerTest.java
    - runtime-java/src/test/java/dev/keir/runtime/EffectRuntimeTest.java
    - runtime-java/src/test/java/dev/keir/runtime/EffectScenarioDriver.java
    - runtime-node/test/canonical.test.js
    - runtime-node/test/cel.test.js
    - runtime-node/test/prepare.test.js
    - runtime-node/test/invoke.test.js
    - runtime-node/test/scalars.test.js
    - runtime-node/test/multi-call.test.js
    - runtime-node/test/effect-prepare.test.js
    - runtime-node/test/effect-invoke.test.js
    - runtime-node/test/effect-handler.test.js
    - runtime-node/test/fixtures/**
    - examples/java-host/src/test/java/dev/keir/examples/JavaHostTest.java
    - examples/java-host/src/test/java/dev/keir/examples/EffectConsumerTest.java
    - examples/java-host/src/test/java/dev/keir/examples/EffectHostTest.java
    - examples/java-host/src/main/java/dev/keir/examples/EffectConsumer.java
    - examples/java-host/src/main/java/dev/keir/examples/EffectSimulation.java
    - examples/node-host/test.mjs
    - examples/node-host/effect-consumer.mjs
    - examples/node-host/effect-consumer.test.mjs
    - examples/node-host/effect-simulation.mjs
    - runtime-node/package.json
    - pom.xml
    - compiler-java/pom.xml
    - runtime-java/pom.xml
    - examples/java-host/pom.xml
    - .github/workflows/keir-contract.yml
  verification: keir-contract
```

These paths cover behavior, expected vectors and test selection. Do not blanket-protect benchmarks or unrelated helpers. Preserve meaningful assertions during equivalent/coverage-only edits; weakening or redefining an obligation requires exact user approval under [testing guidance](../../testing.md#agent-workflow). Workflow-level protection adds no required PR, branch check, CODEOWNERS, hook or mandatory Work Record. Never register a nonexistent verification check.

## Acceptance

- Verify the exact local build, Node regression, package and comparison commands against final reviewed code.
- Validate the proposed YAML and policy schema without activating either.
- Obtain approval for the workflow and isolated follow-up contract block; install only the approved scope.
- Observe the actual CI check, repair concrete CI issues and verify its name/result before adding the contract block.
- Keep Linux/cloud execution unverified until that run succeeds. Keep production containment and future language features outside this checkpoint.
