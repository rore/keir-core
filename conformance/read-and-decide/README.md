# Read-and-decide golden fixture

`source.yaml` and its trusted `dcm.json` describe the controlled read/classify sample. The Java compiler emits `artifact.json`; both runtimes must consume these same bytes. `artifact.sha256` contains the separately held `sha256:` digest for this curated test fixture. It does not authenticate arbitrary customer output or certify a real adapter.

The artifact uses experimental profile `keir-read/1` and compiler identity `keir-compiler-java/0.1.0-SNAPSHOT`. Local Java/Node conformance passes for this checkpoint. See the [compiler commands](../../compiler-java/README.md), [acceptance contract](../../docs/specs/host-integration.md#acceptance-fixtures) and [testing evidence](../../docs/testing.md).

Compiler tests read this directory and require byte-for-byte agreement with the golden artifact. Regenerate through the Java compiler after an intentional reviewed source/model/environment change, independently verify the resulting digest, and update all four files together. Do not recompile separately for Java and Node.

## Executable scenarios

From the repository root, after installing the pinned `runtime-node` dependencies:

```sh
node --test conformance/read-and-decide/node.test.mjs
node conformance/read-and-decide/run-node.mjs
```

The first command checks the shared corpus and the harness's own failure paths. The second runs the same batch in one process and writes the complete normalized JSON array to stdout; a mismatch exits nonzero. Neither command compiles an artifact, uses a network adapter or sleeps. Dependencies can be installed with `npm ci --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund` (`npm.cmd` in PowerShell).

The normal `npm test --prefix runtime-node` command also selects this corpus and its harness checks.

`scenarios.json` is a closed **test-only** transport, `keir-read-scenarios/1`. Its controls are trusted fixtures, never customer-authorable authority or a production configuration API. The runner fixes the reviewed artifact/model digests, entrypoint, consumer, compiler, environment and simulation adapter registration outside scenario data. It uses only the public Node `prepare`/`invoke` boundary.

| Field | Meaning |
| --- | --- |
| `format`, `cases` | Format identifier and ordered cases with unique `id` values. |
| Case `prepare` | Optional `grants`/`limits` admission overrides. Invalid or widening values exercise preparation rejection; no artifact paths or mutation instructions are accepted. |
| Case `runs` | Ordered invocations sharing one prepared handle. Each supplies `input`, trusted `bindings`, fresh `grants`, `clock`, `authorizations` and `calls`. There are no implicit invocation defaults. |
| Run `clock` | Integer `nowMs` and `expiresAtMs` in the same host logical-clock domain. Ordinary runs have a generous 10,000 ms budget. Invalid/regressing values are deliberate negative fixtures. |
| Authorization stub | Exact `request`, `action` (`ALLOW`, `DENY`, `THROW`, `NON_BOOLEAN`), optional `atMs`. Requests are invocation `{entrypoint}` then capability `{entrypoint,capability,version}` when execution reaches that check. |
| Adapter stub | Exact `request` containing `capability`, `version`, separate `agentArgs`/`protectedArgs`, and `remainingMs`; exactly one `response` JSON value or `throw:true`; optional `atMs`. Malformed response payloads deliberately reach the runtime boundary. |
| Case `expected` | `prepare` and `runs` expectations. Preparation is `{status:"PREPARED"}` or `{status:"REJECTED",code,path}`. Requested/expected run counts agree; rejected preparations have zero runs. |
| Expected run | Entire public `outcome` including exact trace, ordered `calls`, ordered `authorizations`, and independent host `consumption`. |

The fake clock changes only inside a callback with `atMs`, before that callback immediately returns or throws. Adapter `remainingMs` is captured before its own `atMs` change. Clock reads themselves never advance time, so runtimes may perform different numbers of checks. These fixtures exercise inclusive expiry before work and deadline/clock checks after authorization or adapter completion without wallclock comparisons. Native timeout and post-termination settlement are separate module tests.

Each queue must match requests in order and be fully consumed. Missing, extra, out-of-order, mismatched or unused stubs fail the harness outside runtime exception handling. A callback mismatch cannot be accepted merely because the runtime safely converts its exception into an expected failure. Full-result comparison retains every outcome and trace field; it does not project away unexpected public data. Trusted call captures contain fixture business/actor values, while exact public traces contain only the contract's safe event metadata.

The DISPLAY consumer independently accepts only the complete `{classification:"DONE"|"PENDING"}` result shape after `COMPLETED`. Every failure is `NOT_DISPLAYED` with its safe code. This consumer and the scripted adapter do not certify a real application adapter or access policy.

## Coverage and Java hookup

Expectations are hand-authored from the contract, not approved snapshots of runner output. The primary golden files remain unchanged; tests compare exact artifact bytes and repository text with Git text line-ending conversion accounted for. The following map separates shared executable scenarios from existing module obligations.

| Acceptance obligation | Executable evidence |
| --- | --- |
| Completed/open, declared missing/inaccessible task, transient/exception/malformed adapter response | Shared `done`, `pending`, `task_not_found`, `access_denied`, `transient`, `adapter-*` cases. |
| Typed input/protected binding, no customer actor override | Shared `input-*`, `binding-*` cases; compiler forbidden host-argument tests and Node `prepare.test.js` cover authored overrides. |
| Fresh/admission grant omission, invalid counts, widening and multi-call exhaustion | Shared `fresh-grant-*`, `admission-grant-*`; the separately Java-compiled [two-read fixture](multi-call/README.md) verifies exhaustion before second-call authorization, counter/identity reset and first-read failure in both module suites on identical bytes. |
| Invocation/capability denial, thrown/nonboolean checks | Shared `invocation-auth-*`, `call-auth-*`; native authorization timeout is checked in Node `invoke.test.js`; Java equivalent required. |
| Exact trace, reservation before dispatch, minimum/exact/one-short quotas | Shared `trace-2`, `trace-4`, `trace-7`, `trace-8`; malformed/widened/lowered quotas also have preparation cases. |
| Inclusive expiry, post-check/post-adapter expiry, negative/regressing clock, valid advance and exact remaining budget | Shared `deadline-*`, `expiry-*`, `clock-regression`, `clock-advance`; fractional/NaN/throwing Node clocks, finite native waits and late settlement stay in `runtime-node/test/invoke.test.js`. |
| Reused Prepared across different users/tasks | Shared `prepared-reuse`; concurrent reuse and immutable snapshots are checked in Node module tests; Java equivalents required. |
| Empty/non-ASCII/control strings; exact 256/257-byte scalar boundary | Shared `empty-string`, `nonascii-control`, `*-bytes-*`; Boolean false, scalar returns and 8,192-byte record limits use actual supplemental Java artifacts in Node `scalars.test.js`. |
| Changed authored condition with unchanged host policy | Java compiler tests and Node `scalars.test.js`, using `runtime-node/test/fixtures/changed-condition.artifact.json`. |
| Wrong requested resource returned | Host-owned adapter check in `examples/node-host/test.mjs`; the generic scenario runner adds no task-specific runtime rule. |
| Parse/AST/type restrictions, external pins/tampering, descriptor/checked metadata, adapter registration mutation | Compiler tests; Node `canonical.test.js`, `cel.test.js`, `prepare.test.js`. These byte/profile obligations are not duplicated as a scenario mutation language. |
| Parser/AST/record ceiling and overflow, deterministic Java compiler output | Compiler tests, Node module fixtures and CEL qualification. Shared scenarios exercise scalar/trace/host quota bounds only. |

The Java test driver should read this same file, prepare once per case and emit the same ordered array of `{id,prepare,runs}` objects. Each normalized run is `{outcome,calls,authorizations,consumption}`. Preparation diagnostics compare `code`/`path`; human message text is excluded. Callback requests and logical time follow the table above; exception text and wall timestamps are never compared. Java driver code belongs to `runtime-java`, and the embedding example remains separate.

Both runtimes pass this corpus and the combined verifier compares their entire normalized outputs. Component coverage and independent packaged hosts are checked locally. Read-profile CI passes; effect-host CI acceptance and production qualification remain separate. See [current testing evidence](../../docs/testing.md). See the complete [acceptance matrix](../../docs/specs/host-integration.md#acceptance-fixtures) and [testing guidance](../../docs/testing.md).

## Verify both runtimes and packaged hosts

With JDK 21 and Node 24 available, build the Java modules and install pinned Node packages using project-local caches. In PowerShell, from the repository root:

```powershell
$env:MAVEN_USER_HOME = "$PWD/.tools/maven"
.\mvnw.cmd "-Dmaven.repo.local=.tools/maven/repository" -Pcoverage verify
npm.cmd ci --prefix runtime-node --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
npm.cmd test --prefix runtime-node
New-Item -ItemType Directory -Path .tools/packages -Force | Out-Null
npm.cmd pack ./runtime-node --pack-destination .tools/packages --cache .tools/npm-cache --ignore-scripts
npm.cmd ci --prefix examples/node-host --cache .tools/npm-cache --ignore-scripts --no-audit --no-fund
node conformance/read-and-decide/verify.mjs
```

After setup, the final command compiles each profile twice, requires identical golden bytes and independently held digests, compares all 61 read cases and 25 effect scenarios across Java/Node, and runs all four packaged-host batches with those fresh bytes. Completed/open inputs display DONE/PENDING; missing/wrong-resource inputs are not displayed. Java uses the shaded host runtime plus test-only scenario classes, with compiler/parser classes excluded. Installed Node source must match reviewed package source. Subprocesses/output are bounded; the verifier accepts no configuration arguments and writes generated bytes only under ignored `.tools/contract-verification/`.
