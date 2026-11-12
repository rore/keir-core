# Bounded numeric engine qualification

Task identity: `agent-workflow:numeric-engine-qualification`. Owning planning item: `roadmap/features/numeric-values-and-rules.md` in the manager-owned planning checkout. That unpublished proposal remains provisional and is not included in this delivery. Source baseline: `6983654ad4e1bdde444bcea0e9023f171432c505`.

<!-- agent-workflow:start -->
**Outcome:**
An independently reviewed, bounded hosted probe supplies honest Java/Node engine and component-performance evidence for numeric contract decisions before full Keir implementation.

**Target:**
keir-core

**Scope:**
Standalone Java/Node checked-expression probe, experimental exact string-normalization/arithmetic helpers, independent fixtures, finite supervision/reporting, one new manual-only workflow and this sole Work Record. Exact six paths are listed below. Deliver only their reviewed commit to main from an isolated checkout and dispatch one disposable Linux qualification job.

**Constraints:**
No production compiler/runtime/schema/API/dependency changes, public numeric contract selection, historical fixture/asset edits, full numeric implementation, release or deployment. Preserve pending documentation and live application state. Local Maven/npm/JDK/build/install/cache and prior fixture/control holds remain. Numeric code runs only in the approved disposable hosted job; no legacy stress/control workflow, retry, timeout/memory increase, shared cache or persistent credential. Whole job <=20 minutes, evidence <=10 MiB; workload bounds below are separate. Manager owns commit/push/dispatch after independent source review.

**Completion criteria:**
The same Java-emitted checked bytes run in both pinned engines with independently asserted exact normalization, Decimal custom dispatch/equality/BIND/registry, exact Integer division, zero/presence and lazy/error vectors, including the required small-rate product and candidate-limit decisions. Bounded nonnumeric/Integer/Decimal preparation, conversion, warm and concurrent measurements record actual workload/hardware/latency/throughput/memory with isolation checks and honest limits. Source/runtime results or a concrete failed assumption are independently reviewed, with no full-product or whole-web-app claim.

**Requirement baseline:**
{"source":"owner-approved bounded qualification route","outcome":"An independently reviewed, bounded hosted probe supplies honest Java/Node engine and component-performance evidence for numeric contract decisions before full Keir implementation.","scope":"Standalone Java/Node checked-expression probe, experimental exact string-normalization/arithmetic helpers, independent fixtures, finite supervision/reporting, one new manual-only workflow and this sole Work Record. Exact six paths are listed below. Deliver only their reviewed commit to main from an isolated checkout and dispatch one disposable Linux qualification job.","constraints":"No production compiler/runtime/schema/API/dependency changes, public numeric contract selection, historical fixture/asset edits, full numeric implementation, release or deployment. Preserve pending documentation and live application state. Local Maven/npm/JDK/build/install/cache and prior fixture/control holds remain. Numeric code runs only in the approved disposable hosted job; no legacy stress/control workflow, retry, timeout/memory increase, shared cache or persistent credential. Whole job <=20 minutes, evidence <=10 MiB; workload bounds below are separate. Manager owns commit/push/dispatch after independent source review.","completion_criteria":"The same Java-emitted checked bytes run in both pinned engines with independently asserted exact normalization, Decimal custom dispatch/equality/BIND/registry, exact Integer division, zero/presence and lazy/error vectors, including the required small-rate product and candidate-limit decisions. Bounded nonnumeric/Integer/Decimal preparation, conversion, warm and concurrent measurements record actual workload/hardware/latency/throughput/memory with isolation checks and honest limits. Source/runtime results or a concrete failed assumption are independently reviewed, with no full-product or whole-web-app claim."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:numeric-values-and-rules"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:**
Pre-edit Redline is GRAY with no boundary violations. Experimental arithmetic, two engine bridges and a finite hosted workflow need independent review, but do not change a production trust/persistence/financial contract.

**Discovery:**
The existing CEL-profile POM and runtime-node lock pin CEL Java 0.14.0, CEL Node 0.6.1, protobuf Java 4.35.1 and protobuf Node 2.6.2. ScalarProbe already emits deterministic CheckedExpr/descriptors; Node profile.mjs decodes/plans those bytes. Production CheckedCel replaces selected standard bindings; Node environment sources expose function overrides, but numeric object dispatch/exact Integer division remain unrun assumptions. Existing qualify.mjs contains a 10,000,000-evaluation termination workload and is excluded. The current contract push workflow matches conformance/** and runs excluded legacy controls, while release-packaging is pinned to historical inputs. Use a new manual-only workflow and a reviewed commit carrying [skip ci], which GitHub documents as skipping push/pull_request but not workflow_dispatch. No workflow policy changes are needed.

**Material assumptions:**
1. Pinned engines support the fixed Decimal message, trusted custom functions/operators and exact Integer division overrides. Observe actual binding counters/type/ref maps and canonical outputs. A mismatch stops the affected choice; no engine upgrade, float fallback or extra interpreter.
2. Probe-local normalized precision <=38/scale <=18 is a candidate, not a settled product limit. Compare fixed scale 9/18 decision vectors and realistic chains; do not count scale-9 rejection as fulfilling the exact small-rate requirement. Each operation checks its own result. No silent rounding.
3. One hosted run can produce useful bounded evidence without local execution. Cold dependency/build time is unknown. Setup, oracle, runtime, resource or closure failure stops without automatic retry or cap changes, retaining compact failure evidence.
4. Main has not moved incompatibly before delivery. Check remote identity/delta before a non-force push; stop on conflict rather than overwrite others. The isolated checkout preserves the dirty planning checkout. Verify current personal Git identity before committing; no persistent identity/config changes.

**Plan:**
Use the exact scope and verification mapping below. First obtain the existing architect's independent bounded plan GO. Then the existing Core developer owns the five source/workflow paths and this record's implementation/evidence updates, with source/static checks only locally. Reuse the standalone POM unchanged and existing package exports/byte protocol, not executable stress entrypoints. Keep codecs and gate import-safe; use bounded BigInteger/bigint arithmetic, no new libraries or generic framework. Freeze source hashes for independent result review before manager commit/push. The manager verifies exact six-path scope, record, privacy, pending-work preservation and personal attribution, commits with [skip ci], pushes only reviewed source to main, reads back the remote identity and dispatches exactly once. Read actual evidence, distinguish setup/semantic/performance failure and return affected contract choices to design. No required numeric behavior is dropped to obtain a green report.

**Verification plan:**
Exact helpers/limits -> independent fixture strings for 100.00, signed zero, redundant zeros, fraction/range/scale/sign/tie/carry boundaries, length256/257 and malformed/non-ASCII/native-number rejection, with no rounding in normalization.
Pinned bridge -> same hashed checked bytes/descriptors consumed by both engines, observed custom-binding counters, every admitted arithmetic/comparison/rounding operation, exact versus inexact Integer division and overflow, Decimal equality/direct intermediate BIND, expression-only registry and absent versus present zero.
Errors/admission -> selected/unselected and AND/OR masked numeric errors in both orders, mixed temporal/numeric failures, malformed Decimal messages and bounded forged-map/overload/internal-selection rejection; intentionally wrong expectations must fail. This is a probe gate, not full Keir artifact security qualification.
Decision examples -> 0.00001 squared equals 0.0000000001 for the wider candidates; rate/quantity/percentage/amount chains, fixed-scale range trade-offs and variable precision/scale cliffs, exact/nonterminating and explicitly rounded intermediates.
Performance/isolation -> at most eight timed cases per engine, 100 warmups/1000 validated samples, separate first setup/preparation and 30 warmed preparations, direct normalization/projection/prepared-eval/eval-plus-result phases; four Java threads share one immutable program with fresh activations, and four Node same-isolate lanes are labelled interleaved/serialized CPU, not parallel. Assert independent lane values and failure recovery; record actual counts, nearest-rank latency/throughput, CPU, heap/RSS trends and supported allocation counters without arbitrary speed targets.
Supervision/workflow -> source-static check of manual-only event, read-only token, exact checked-out revision, ephemeral dependency/output paths, no cache restore/persisted credentials, allowed commands/outputs, stage/job/child limits and no legacy entrypoint. All actual compilation/dependency/numeric execution is hosted only. Pre-edit and final Redline, Work Record predicates, exact path inventory, syntax/data/whitespace/privacy checks and independent reviews precede delivery.

**Plan review:**
Agent technical review: independent non-implementer architect plan GO, 2026-11-12; see Plan review evidence below. Reviewed record SHA-256 82b7aa66e8fb0f874d7ea63d9586691de90d15bada2e400d6a84972e03d1630b was unchanged before/after review. No required corrections. Source/static implementation may proceed; frozen-source review and the one hosted run remain separate gates.

**Approvals:**
Approved by user 2026-11-12: "Approve one hosted pass". This answers the presented bounded route: standalone Java/Node probe, experimental exact-normalization helpers, fixtures and a new manual workflow; independent reviews; exact reviewed files and Work Record committed/pushed to main from an isolated checkout; one disposable Linux job, 20-minute maximum, bounded workloads and <=10 MiB sanitized evidence, hosted-only pinned dependency downloads. No local hold lift, retries, full implementation, release or deployment. No additional human approval is needed for that exact scope after technical gates pass.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Exact ownership

New paths only:

- `conformance/cel-profile/src/main/java/qualification/NumericProbe.java`
- `conformance/cel-profile/numeric-fixtures.json`
- `conformance/cel-profile/numeric-profile.mjs`
- `conformance/cel-profile/numeric.mjs`
- `.github/workflows/numeric-seam-qualification.yml`
- `.agent-workflow/tasks/numeric-engine-qualification.md`

No worker-isolate comparison in the first pass. Existing POM/lockfiles/production code, workflows, policy and historical fixtures are read-only. Report/classify a genuinely needed extra path before editing it.

## Workload and workflow bounds

At most eight timed cases per engine, 100 warmups plus 1000 samples per case, 30 warmed preparations per representative program. Correctness/hostility vectors execute once outside timing, with a finite fixture inventory validated before loops. Concurrency uses 100 total warmups and four lanes x250 invocations for each of three value families. No benchmark-until-convergence loop.

Each direct workload child: 45 seconds, 1 MiB combined captured output, 256 MiB engine heap and 512 MiB observed RSS screen. Workload phase: 135 seconds total, separate from setup/build. Track and close only task-owned children; no legacy termination/control stress. Native hosted step/job timeout is an independent final ceiling. RSS sampling is a screen, not hard address-space containment. Do not claim prompt preemption or absence of all memory spikes.

One manual-only Linux job, <=20 minutes. Use established Java21/Node24 setup actions, record actual versions, no cache restore/write or checkout credential persistence, runner-temporary Maven/npm/output roots. Build only the existing standalone probe POM; Node dependency installation uses the existing lock with ignore-scripts/no-audit/no-fund. No Core package build/install, companion, browser/provider, old controls or publication. Upload only sanitized allowlisted fixture/checked bytes, hashes, reports and bounded logs, aggregate <=10 MiB. Failure evidence remains a failure, not truncated success. Do not dispatch twice or retry a failed stage.

## Implementation

The manager created an isolated task checkout from the recorded fetched-main baseline. Explicit record resolver returned absent before creation. Intended-path Redline returned GRAY/no boundary violations; record excluded by existing policy. The first static checker invocation lacked its Python imports; reused already-present project-local static-checker dependencies with process-only import paths, without installation/config/cache changes. No numeric or build code ran.

Independent plan GO is received and accepted. Next: source-only implementation and static checks, then frozen-source result review. Branch `qual/numeric-engine-qualification`, baseline above; no commit yet. Dirty planning files are outside this checkout and delivery. This record remains the sole task record; public contract revision is separate manager-owned planning work.

Source implementation started within the exact six-path reservation. Only inert syntax/data/source checks are permitted locally. All engine, helper, benchmark, toolchain and dependency execution remains reserved for the single approved hosted pass. The probe will report per-operation parsing rather than imply that protobuf messages preserve a parsed coefficient cache.

## Plan review evidence

The independent non-implementer inspected the exact record and relevant pinned engine/CI source at the baseline. Verdict: GO, no required corrections; before/after record hash matched the Plan review field. The six-path scope, independent oracle, candidate bounds, actual binding counters, distinct preparation/conversion/warm phases and finite concurrent screen are proportionate. Sampled RSS is not hard containment, Node same-isolate lanes are not CPU-parallel, and unknown setup time may legitimately produce failure evidence.

Frozen-source review must confirm explicit `qualification.NumericProbe` classpath invocation rather than the retained ScalarProbe JAR manifest, independent fixture expectations rather than parity alone, actual overload/codec paths and temporary bounds, finite inventory/counters, child closure and aggregate allowlisted output limits. Manual-only workflow plus `[skip ci]` remains necessary to avoid excluded controls. No numeric behavior, public contract or performance success was established by this plan review. No reviewer edits or execution occurred.

## Evidence

Source implementation is complete and frozen for independent source review. The manager resolved the developer's static-interpreter limitation using existing tooling without installation or environment changes; see follow-up below. No numeric engine, normalization, performance, package or application evidence exists yet. Static classification and parsing checks are source evidence only.

### Source implementation

The five new files contain a standalone Java emitter/replayer, an import-safe Node codec/registry/gate, independent fixture strings, a finite hosted supervisor and a manual-only workflow. The probe compares fixed precision/scale 38/9 and 38/18 with normalized coefficient precision <=38/scale <=18. The required small-rate exact successes remain distinct from the scale-9 rejection. No public contract, producer identity, production export, dependency or old fixture changed.

The fixture inventory is 24 normalization helpers, 99 expression cases, 12 forbidden sources and three concurrent families. Eight cases have separately labelled prepared-engine, result-validation and activation-projection phases. Both implementations use independent expected values; parity is an additional check. Decimal message operands/results are reparsed, with explicit counters, rather than represented as a parse-once cache. A common eight-declaration activation is labelled as such, not as a production nonnumeric invocation. Java uses four real executor threads sharing one program; Node uses four interleaved same-isolate lanes, not CPU-parallel workers. Actual engine support and all measurements remain unrun assumptions.

The supervisor runs only the standalone POM and locked Node dependencies in ephemeral hosted roots. It does not import or invoke either legacy qualification entrypoint. It applies the recorded child/time/output/heap/RSS bounds, verifies task-owned process-group closure, and uploads only a bounded sanitized allowlist. Native job cancellation can prevent final evidence collection; absent or incomplete evidence must never be called a pass. No runtime, helper, worker, dependency or build operation ran locally.

### Actual local checks

| Check | Actual result |
| --- | --- |
| `node --check conformance/cel-profile/numeric-profile.mjs` | Passed, exit 0; nonloading syntax parsing only. |
| `node --check conformance/cel-profile/numeric.mjs` | Passed, exit 0; nonloading syntax parsing only. |
| PowerShell `ConvertFrom-Json` and inert fixture inventory | Passed: unique IDs, finite inventories, exactly eight timed cases, expression/BIND source strings <=512 UTF-8 bytes. No helper or expression evaluated. |
| Six-file strict UTF-8, trailing-whitespace and focused private-content scan | Passed. Includes untracked source files, which ordinary `git diff --check` does not cover. |
| Tracked baseline preservation | `git diff --exit-code 6983654` outside this record returned 0. Git status contains exactly the six reserved untracked paths. No staging or commit. |
| Python JSON/YAML static checks | Could not start: `python` command unavailable. No installation, PATH/config change, interpreter/version probe or environment repair attempted. JSON inventory was separately checked with native inert data parsing. |
| Developer final Redline / Work Record validation / YAML parser | Initially unrun because the developer's bare Python command was unavailable. Manager follow-up resolves this below; no check is waived. |
| Java compilation; all numeric/helper/oracle/engine/performance checks | Deliberately unrun locally. Reserved for the one approved hosted pass after independent source review and manager delivery. |

### Frozen source inventory

SHA-256 of the exact source-ready files:

| Path | SHA-256 |
| --- | --- |
| `.github/workflows/numeric-seam-qualification.yml` | `e20d3db8f18f6d17ceb4b36037604c99f1da9c67a462f7e2937829e78c6f45ef` |
| `conformance/cel-profile/numeric-fixtures.json` | `d900f19fdfc66cb441f71b0968aec64d616117591e7005697e3af6a3ad50df56` |
| `conformance/cel-profile/numeric-profile.mjs` | `b0a95677e04bc224c296e47a01719489bded17ae3e6cf2f18bde27a4546ee224` |
| `conformance/cel-profile/numeric.mjs` | `f68b56c25dbfac12d9a8b7892b0a17ee231f0375aae6321244ee0bf3cb4c1c51` |
| `conformance/cel-profile/src/main/java/qualification/NumericProbe.java` | `9ad96987c91e366a1ad7df75d76103bf1e7c8fc76f935275411612e526099b84` |

The record's digest is supplied separately at handoff to avoid a self-hash. The original Requirement baseline is unchanged. The task checkout is retained for the active source review and one hosted run; no other checkout or pending canonical file was changed.

## Result review

Agent technical review: independent non-implementer source GO, 2026-11-12. Original review requested exactly R1/R2; focused review of corrected Java/Node and record SHA-256 37f6962328bca39615440b2ff723b1429ca9c3a05f16961729591b63c221a535 verified unchanged before/after and closed both. Reuse the unchanged remainder; no further correction or approval is needed before the approved delivery/run.

Reviewed revision: original Java SHA-256 `2de37486dfb44a24d849e7b11a82887275a943a665bf017af95183dba9902089`, Node SHA-256 `461edcc67c2841acd51c5af122417c7a4411d95fdd1ec2ef56dbe57fafa49cf6`, the unchanged fixture/supervisor/workflow hashes in Frozen source inventory, and record SHA-256 `05da0db8a194623d45b0c8aecfe1d46e6d8bd34f6abffd40d5ac4b73d37bdf8c`, verified before/after the read-only review. Frozen source inventory now identifies the corrected sources awaiting focused delta closure.

Verification adequacy: source plausibility, fixture arithmetic/rounding/range expectations, candidate bounds, explicit classpath launch, metric limitations, finite supervision and workflow exclusions passed source review. Actual engine support remains unrun. Two missing checks must be corrected before the one hosted pass: direct reuse of an intermediate Decimal engine result, and a wrong fixture expectation exercised through the actual evaluation/check path. Source GO and runtime evidence remain pending; no commit/push/dispatch occurred.

The developer's static-tool blocker is resolved. The manager used the already-present interpreter and static-parser imports with process-only paths and bytecode disabled. Six frozen hashes matched, JSON inventories/source caps, YAML manual-only/read-only-token/checkout/cache controls, privacy/whitespace and fresh GRAY Redline/no boundary violations passed. No probe code was imported or evaluated. Independent frozen-source review is next; a missing command is not grounds for environment repair or duplicate user approval. No commit, push or dispatch has occurred.

### Targeted source corrections

R1, medium: Java evaluate and Node evaluate convert the first Decimal result to canonical text, parse it and construct another message for the next BIND activation. Validate the result but retain the actual Decimal object/payload in the next activation without text round-trip or reconstruction. Exercise the existing BIND fixtures through that path, preserving escaping-error behavior. This qualifies the direct engine bridge rather than merely codec round-tripping.

R2, medium: comparing two hard-coded different strings tests only the assertion library. Evaluate an existing successful fixture through the normal evaluation/check path with an intentionally wrong expected result and require that actual oracle comparison to fail in each engine. An evaluation/setup failure must not count as the expected oracle failure. Reuse existing code; no second evaluator or extra framework.

Only NumericProbe.java, numeric-profile.mjs and this sole record are released for the targeted correction. Fixture, supervisor and workflow hashes remain fixed. Run inert static checks only locally and report revised hashes for focused delta closure; do not rerun unchanged broad review, change the original baseline, commit, dispatch, add a local numeric test or ask for duplicate approval.

### Targeted correction implementation and verification

R1 is implemented in the shared evaluate path of both engines. Java evaluates the BIND program directly, validates its actual result with the existing Decimal reader, and puts that same object into `d`. Ordinary evaluation and BIND failures share the existing error classification, including temporal errors. Node preserves CEL errors, validates the actual reflected result, and puts its existing `message` payload into `d`. Neither path converts the intermediate result to text or constructs a replacement message. Existing BIND fixtures route through these paths in the hosted probe; they were not executed locally.

R2 is implemented using the existing successful `decimal-add` fixture. Each engine clones the fixture with a deliberately wrong expected result and evaluates through its normal evaluator. The actual result must first match the original successful expectation outside the negative catch. Only the subsequent existing oracle comparison against the wrong expectation may satisfy the negative check. Setup/evaluation failure cannot count as the expected rejection. These checks are source-complete but unrun locally.

Actual targeted static checks, 2026-11-12:

- Changed Node source passes `node --check` with exit 0, without loading imports.
- The approved existing Python interpreter, with bytecode disabled and process-only static-parser imports, passed JSON inventory/unique-ID/source-and-BIND-cap checks and YAML manual-only/read-only-token/nonpersistent-credential/no-cache-action controls. No probe module was imported or evaluated.
- All three locked fixture/supervisor/workflow hashes match Frozen source inventory. Original Requirement baseline SHA-256 remains `b1e17038455aae7f02619d147ce8023527355dcffccb50c54c4a8d693f891133`.
- Fresh final Redline is GRAY, exit 1 for review warnings, with no boundary violations or detected API/schema/security/runtime-configuration changes. This invocation classifies the exact path inventory; no diff/line evidence was supplied, so its zero line count is not a measured source delta or size audit.
- Work Record validation returned clean/exit 0 in State Ready for review. Its `review.agent_result_review_present` predicate accepts the recorded REQUEST CHANGES review as a review reference and explicitly does not judge verification adequacy. This is a structural checker result, not independent result GO or R1/R2 closure. The requirement-history predicate was skipped without PR history; the separate baseline digest check above verifies the original baseline text.
- Six-path UTF-8/whitespace/focused-privacy checks, exact reserved untracked inventory, unchanged tracked baseline/index and frozen source hashes are checked at handoff. The corrected inventory above replaces only Java/Node identities; original reviewed identities remain preserved under Result review.

Source corrections are ready for focused independent R1/R2 delta review. No static-tool blocker remains. Independent result GO, manager integration and the one approved hosted job remain pending. Reuse the accepted remainder of the original source review. No compilation, numeric/helper/oracle/benchmark execution, staging, commit, push or dispatch occurred. The isolated task checkout remains retained for that review/run; the manager owns delivery and broader roadmap updates.

Focused closure complete: R1 directly reuses the validated engine object/payload; R2 evaluates a successful fixture and isolates the deliberate oracle mismatch from evaluation failure. Independent SOURCE GO has no remaining correction. Five source hashes and the reviewed record matched before/after. Manager accepted closure and proceeds with the already-approved exact six-path commit/push and single hosted dispatch. Compilation, numeric behavior and performance remain unrun; this is not runtime GO.

Delivery: reviewed source commit `7c8823578c4dbf43a6c89e39ec3a349d26e4a109` was pushed to main with `[skip ci]` and verified remotely. GitHub initially had not registered the new workflow, then rejected dispatch with HTTP 422: `runner.temp` is unavailable in job-level `env`. The workflow's run listing was empty: no hosted job or numeric attempt occurred. Corrected only environment placement to the first step, persisting the same isolated values through `GITHUB_ENV`; GitHub's official context-availability table permits `runner` in step env, not job env. Commands, dependencies, numeric source, workload/safety limits and single-run authority remain unchanged. This is a pre-execution wiring correction, not a retry of a failed job. Reuse the reviewed isolation/supervision design; verify exact context placement before the first admitted run.
