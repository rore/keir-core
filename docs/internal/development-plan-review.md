# Initial development plan: architecture review

Date: 2026-10-29. Status: architecture planning review, not an accepted specification or implementation claim. Manager follow-up accepts the three milestones, REQUEST read/classify/consume first, one possible write initially, and original host-key pass-through. The second increment below is reduced to scalar guarded creation; baseline list/tag/search/time behavior remains a later explicit enhancement.

## Recommendation

Keep the three outcome milestones. Deliver a small read-and-decide extension through the Java compiler and **both** runtimes first, then add the guarded follow-up effect through the same path. These are runnable increments within existing feature work, not a new planning hierarchy. A command-line sample host is enough to demonstrate the first two increments; neither depends on finishing the task application or building an agent chat UI.

Each increment includes affected draft review, maintained public contracts and accepted/rejected examples, implementation, executable evidence, and specification feedback. Do not finish all specifications before implementation or implement an entire compiler layer before executing a narrow program. Specifications describe supported behavior and explicit rejection boundaries; they do not silently adopt every baseline proposal.

Product value is accepted. There is no frozen-catalog exercise, competitor benchmark or market gate. Catalogs evolve through explicit revisions. Both product assistant personas begin with the first contracts and walkthroughs, while their host-owned live integrations arrive incrementally. Core remains usable without an agent.

## Review scope and source coverage

Read all files currently supplied under `docs/internal/design-baseline/`, including full document bodies and the schema/fixtures below. Paths in this table are relative to that directory. This is coverage of the supplied package, not an assertion that referenced external papers, old repositories or missing v0.6 material were reviewed.

| Sources read | Planning consequence |
| --- | --- |
| `README.md`; `grill-findings.md` | Preserve source/evidence distinctions; retain portability/resource findings, discard superseded value-proof and Java-later gates. |
| `public/keir-architecture.md` | Closed composition, separate source/artifact, small interpreter and host-owned agent/lifecycle. |
| `public/keir-language-v0.md` | Strict grammar, immutable dataflow, guarded calls, terminal business handlers, nominal/enum overlay and CEL admission. |
| `public/domain-model-v0.md` | Trusted catalog, protected arguments, semantic validation, required host evidence, digest and compatibility gaps. |
| `public/compiler-artifact-and-runners-v0.md` | Library/CLI compiler; portable checked expressions/descriptors; load/prepare/execute; missing wire contract and trust boundary. |
| `public/simulation-and-authoring-v0.md` | Same artifact/runner; deterministic fakes; independent expectations; approval separate from compilation. |
| `public/task-management-poc.md` | Useful standalone task product, real adapters, protected origins, authoring UX and scope deferrals. |
| `public/decisions-and-evidence.md` | Historical CEL evidence and exact recovery source; expression evidence is not whole-program conformance. |
| `public/hooks-and-tenant-capabilities.md` | Host result consumer, optional tenant overlay, data/endpoint ownership. |
| `public/host-integration-and-hook-contracts-v0.5.md` | Definition/invocation/consumption separation; placement, native effect ownership, discovery, admission, re-entry and consumer tests. |
| `public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md` | Original host key pass-through, actual collision scopes, cross-binding admission, partial/unknown outcomes; no automatic recovery. |
| `public/schemas/extension-ir-v0.schema.json` | Structural validation only; semantic checks and parser bounds remain required. |
| `public/examples/task-domain.yaml`; `task-domain.sha256` | Pending certifications, placeholder provenance, example-only canonicalization and incomplete size bounds cannot become production defaults. |
| `public/examples/followup-extension.yaml`; `followup-created.scenario.yaml` | Concrete guarded follow-up flow and expected arguments; new maintained fixtures need separate trusted key context and artifact binding. |

Also read accepted decisions `0001`, `0002`, `0003`; `docs/specs/README.md`; `docs/agents/README.md` and both persona guides; core/root and all component/example READMEs; `roadmap/scope.md` and all five feature cards; current architect/shared guidance; demo `README.md`, `docs/scope.md`, and component READMEs for `src/`, `keir/`, `tests/`. Current components report scaffolds, not runnable implementations. Historical CI results were read as reported evidence, not rerun or independently verified.

Review baseline HEAD observed: `2445cd70e1835c97acae2860eee9ebf754d86cac`. The shared checkout may advance under the manager. Existing dirty `roadmap.config.json` was not edited by this review.

## Components, ownership and dependencies

| Component | Responsibility and dependency |
| --- | --- |
| Core `compiler-java/` | Independent library plus thin CLI. Strict source/DCM parsing, semantic/type/authority/profile validation, deterministic diagnostics and portable artifact. Depends on maintained contracts and CEL/descriptor bindings, not demo services. |
| Core `runtime-java/`, `runtime-node/` | Independently packaged load/prepare/execute and simulation APIs. Validate trusted artifact compatibility, dispatch through host adapters, enforce bounds, preserve outcome/effect evidence. Neither interprets source IR or invokes the compiler during execution. |
| Core `conformance/` | Shared source/catalog/scenario fixtures, compiled bytes and expected observations. Runs both runtimes; keeps compiler rejection, expression parity, complete-program behavior and resource evidence distinct. |
| Core `examples/java-host/`, `examples/node-host/` | Tiny trusted host drivers with deterministic fake adapters, explicit admission and observable result consumption. Not two task products or production hosts. Domain names stay in fixtures/examples, never compiler/runtime logic. |
| Core `docs/specs/`, `docs/agents/` | One normative truth for supported contracts; two task-oriented assistant guides referencing it. Schemas, diagnostics and discovery descriptions agree with those contracts. |
| Demo `src/`, `keir/`, `tests/` | Real task services/UI/storage/authentication, task DCM/adapters, hook consumers, event admission, both assistant integrations, simulation review and activation. Consumes pinned packaged core outputs, not sibling source imports. |

Dependency spine: bounded contract and examples -> compiler + runtimes developed against those examples -> packaged sample execution -> real host admission/adapter evidence -> live assistant-mediated publication and activation. This is a dependency for accepting the combined result, not a prohibition on parallel development.

Use existing directories. Proposed public documents are `docs/specs/language.md`, `domain-model.md`, `compiler-artifact.md`, `runtime.md`, and `host-integration.md`; add sections as needed rather than per-increment specification copies. Keep shared schemas/fixtures in `conformance/` initially and link them. A separate common Java module is justified only by actual shared contract code, not anticipated reuse.

### Minimal build/package recommendations

Select one repository-local Java multi-module build for compiler/runtime/examples and one Node package build for the runtime. The manager reports JDK 21.0.12 and Node 24.14.1 on PATH, with neither Maven nor Gradle installed, and recommends a repository-local Maven wrapper plus npm/TypeScript. This review supports that approach without selecting unverified dependency versions or changing installed tools. Use library JARs, a CLI JAR distribution and an npm package/tarball; no compiler service, code generators for business code, workflow engine or registry publishing is needed to run the first examples. Integration may install local built packages with recorded digests before a release registry exists.

Recover the reported CEL harness as ordinary reviewable source, checking its recorded archive digest and provenance. The baseline pins CEL-Java 0.14.0/CEL-ES 0.6.1 and used Java 21/Node 22; use those as reproduction inputs, not a claim they are the current best supported production versions. Confirm available isolated tooling and dependency compatibility before selecting build versions. No global installs, PATH changes or upgrades are implied. Package coordinates, release/license policy, demo framework/database, and the production support matrix remain choices for the owners. They need not block a local sample; database/transaction choice must precede real replay claims.

## Reconcile before expanding implementation

| Gap or conflict | Required resolution and timing |
| --- | --- |
| Java-later throughout baseline and grill; historical frozen-catalog/value comparison gates | Decisions 0001/0002 prevail immediately. Demo `docs/scope.md` still asks for catalog-cost/comparative evaluation in Later increments: manager should reconcile that live text. No such gate is proposed here. |
| v0.5/v0.7 hook shapes versus existing entrypoints; `ALLOW/REJECT`, `CONTINUE/DENY`; `SUCCESS` versus `COMPLETED` | Before increment 1, define one outcome envelope separating execution class, typed business result and host-consumption result. Recommend runtime `COMPLETED`, `BUSINESS_REJECTION`, `FAILED`, `LIMIT_EXCEEDED`, `UNCERTAIN`; adapter `SUCCESS` remains distinct. Business decision enums belong to each consumer, not special interpreter opcodes. Map old fixtures explicitly. |
| DCM digest is example JSON sorting; artifact wire format unspecified | Before increment 1, specify deterministic typed encoding, numeric/presence rules, covered bytes, descriptor generation, manifest ordering and trust checks. Version the envelope and feature requirements. Hashes alone never establish authority. Avoid circular self-hashing or embedded-signature ambiguity. |
| CheckedExpr contains type/reference maps that CEL-ES does not use for planning | Admit exact AST forms/overloads only after shared tests. Pin evaluator/environment/descriptors. Do not accept arbitrary checked protobuf merely because it parses or carries a signature. |
| Nominal/enum/unit/presence overlay absent | Increment 1 supports only explicitly implemented closed records, required fields, nominal ID propagation and finite enum refinement; reject unsupported constructions. Increment 2 reuses scalar fields and rejects references to absent guarded results. Bounded lists, optional-field representation and time semantics arrive with the later baseline-program enhancement; units and broader presence proofs wait for a scenario. |
| DCM semantic schema/bounds incomplete | Before accepting any DCM, reject unresolved references, cycles, unknown fields, unsafe counts and unbounded expression-visible data. Fixture nominal IDs/tag strings lack explicit length bounds; author maintained fixtures with complete ceilings. Structural JSON Schema is not sufficient. |
| Compilation diagnostics include uncertified contracts, but fixtures need simulation with pending guarantees | Separate static obligation checking from real activation certification. A simulation-only host can use explicit fake evidence through an isolated admission path; production admission must reject pending evidence. No source-controlled simulation flag grants real adapters. |
| Artifact trust alternatives differ: signed manifest versus controlled storage | Manager selects host-controlled compiler output plus an independently held expected digest for local samples; no public untrusted artifact endpoint. Artifact-local self-hashes or an accept-unsigned switch are not provenance. Real activation later needs exact approved storage/signature policy. |
| Resource proposals contain 64 items/120 AST nodes and fixture 10,000 cost/200 ms without validation | Measure actual pinned evaluators; record concrete profile limits and what is enforced. Enforce parser/preparation/input/result/aggregate bounds too. Node timer and Java interruption do not prove termination. No production multi-tenant/hard-deadline claim until containment evidence exists. |
| v0.7 permits multi-effects; roadmap and demo currently cap one write | Keep one possible write call in the first effect increment; reject two guarded writes without exclusivity analysis. Adopt unchanged host-key forwarding now. Manager must explicitly select a later multi-effect feature before widening this scope. |
| Older keys tied to event/revision/step; v0.7 original-key contract | Before first write, distinguish logical invocation identity from typed downstream idempotency value. Runner neither derives nor coerces the latter. Simulation fixture currently lacks that key. Host tests must cover collisions with other native consumers even with one extension. |
| Read/check/write and delivery claims exceed example evidence | Search does not establish uniqueness; real atomic origin constraint and replay/admission evidence belong to the demo. Choose event-admission binding time, payload mismatch, retention and unknown reconciliation before real effects. |
| Two assistant personas versus blanket “agent cannot define” and customer-only design time | Update maintained contracts/guides to distinguish draft integration edits from authoritative publication. Host assistant proposes definitions; human host owner approves exact revision against evidence. Extension assistant uses only approved scoped catalog. Neither certifies or activates its own work. |
| API/trace/approval safety incomplete | Define redacted diagnostics/traces and bounded CLI input/output/concurrency initially. Before live agent tools, define authentication, scope, tool effects and catalog freshness; treat descriptions and retrieved data as untrusted content, never permission. |

Defer broad API importers, tenant endpoint registration/transport, arbitrary patch chains, multi-binding inheritance, automatic replay, durable Keir workflows, scheduling, generalized timezone calendars and code generation. Later features must resolve their own relevant gaps; deferral is not approval of their current draft design.

## Ordered outcome milestones

These refine the existing milestones; the manager owns final card boundaries and sequencing. The first two increments below belong to milestone 1 and contribute to the existing specification, CEL-bound and portable-core cards.

| Existing milestone | Ordered runnable outcomes | Acceptance and specification evolution |
| --- | --- | --- |
| Execute extensions in Java and Node | First: a host asks for a task classification, an extension selects a read and returns a decision, both sample hosts display the consumed result. Second: a completion event produces or skips a follow-up effect through deterministic adapters, including an uncertain write. | Same compiled bytes, observable parity and fail-closed admission. Maintained initial DCM/language/artifact/runtime/scenario contracts and both assistant walkthroughs. No claim of real persistence or live AI tools. |
| Customize a real task application | In parallel, grow native task create/list/edit/complete UI. Connect a reviewed follow-up artifact with real scoped reads/create and simulation. Then add a pre-create guard: a denied urgent task does not persist; allowed work still passes native validation. Run the host integration assistant against a proposed domain/hook change and host approval. | Native task app works with extensions disabled. Packaged core consumption; actual uniqueness, replay/admission, authorization and consumer tests. Host integration/discovery/approval contracts mature with real paths. An assistant-generated proposal cannot self-publish. |
| Author and activate customizations through AI | Customer describes a supported follow-up, reviews independent scenario expectations and artifact-derived behavior, approves and activates; revises it, disables it, and rolls back selection for new invocations. Unsupported customization gets an explanation rather than new grants. | Bounded generation/repair, scoped discovery, immutable approval links, same artifact for simulation/execution, current authorization and preserved in-flight admissions. Host and customer tool surfaces remain separate. |

After the first connected host, possible capability-expansion features include a bounded read-only view and v0.7 multi-effect execution. Each needs an end-to-end scenario: e.g. two effects in certified disjoint scopes versus shared-scope rejection and a confirmed-first/unknown-second receipt. Do not make all such breadth prerequisites for the AI milestone. Multi-effect implementation must land in compiler and both runtimes together, with real host scope evidence before real activation.

## Runnable increment 1: read, decide, consume in both hosts

### Outcome and entry conditions

A local command compiles an extension for a small `REQUEST`/on-demand sample hook. The extension calls one approved task lookup, binds a Boolean from the returned status, and returns a closed classification record. Java and Node hosts load the **same bytes**, invoke with the same business input and fake response, and render the result through a tiny consumer. Changing the source condition and recompiling changes the decision without changing host logic. This is an advisory result, not an authoritative pre-commit invariant or a full task product.

Entry conditions: manager accepts this smaller on-demand scenario before the baseline follow-up; owners agree the narrow envelope, DCM representation and observation fixture; isolated build tools are available. Resolve outcome vocabulary, trust/admission and exact encoding above. Recover existing expression evidence where available, but do not let unavailable archived packaging block writing focused equivalent tests. No whole-language freeze is required.

### Minimum supported contract

- One handwritten DCM, no tenant overlay/importer. One hook, one static read capability and one exclusive binding. Closed records with required bounded fields, Boolean/string/int64 and closed enums; nominal IDs pass through typed fields to approved arguments. Exact int64 transport must avoid JavaScript-number loss even if the main scenario only returns status.
- Strict YAML/JSON source parsing with duplicate/unknown-key rejection, size/depth limits and no aliases/merge/executable tags. IR supports unguarded `call`, immutable `let`, typed `return.record`; bounded ordered steps with no future references or shadowing. Explicitly reject `when`, `onError`, writes and unsupported DCM types for this increment rather than accepting inert fields.
- CEL supports only required field selection, typed equality/Boolean logic and finite literal enum conditionals. No macros, optional dereference, maps/dyn, regex, custom functions or arithmetic until admitted. A profile-feature list distinguishes this implementation from the full draft tagged `keir/v0`.
- Compiler validates references/grants/host-only arguments, types and CEL policy; produces complete resolved bindings, descriptors, CheckedExpr, immutable manifest and stable diagnostics. Runners validate host-established trust, digests, environment/consumer pins and supported instructions before preparation. No concrete protected identity enters the artifact or CEL input.
- Typed adapter envelope supports success, unhandled business rejection and transient failure; no regular result after failure. Unexpected adapter exceptions become a declared failed execution, not a success value. An uncertain write result is invalid for the read-only capability, not silently accepted.

### Specification and file/module outputs

Publish the supported portions of the five proposed public specifications and strict source/DCM/artifact/scenario examples in `conformance/`. Include exact int64 and absent/null encoding decisions, deterministic digest vectors, diagnostic paths, safe tracing and preparation/execution bounds. Both assistant guides gain worked examples: host assistant drafts the catalog and submits it for approval; customer assistant consumes the approved revision and drafts the extension plus expectations. Record intended tool input/output/permission contracts and explicitly identify what is not implemented. A manual scripted walkthrough exercises these boundaries; do not label it a live AI integration.

Core ownership: `compiler-java/` parser/model validator/checker/emitter/CLI; both runtime directories loader/preparer/interpreter/adapter interface; `conformance/` shared fixtures and comparison driver; `examples/` trusted drivers and fake consumer. No demo changes are required for acceptance. Demo developer may build native task UI/services in parallel using their own approved assignment.

### Demonstrations and evidence

| Demonstration | Required observation |
| --- | --- |
| Completed versus open sample task | Different typed classification; one ordered read; no effect; both consumer outputs match. |
| Missing/inaccessible task and transient read failure | Typed failure, no fabricated decision, sample consumer displays failure. Scope denial is not bypassed by compilation. |
| Unknown capability, wrong nominal ID, host-bound actor override, forward reference, unsupported macro, unknown field/duplicate key | Stable compile rejection and no runnable artifact. |
| Changed plan/expression/descriptor, mismatched DCM/profile, forged approval metadata, unknown opcode or wrong consumer | Load rejection before adapter dispatch; recomputing artifact-local hashes cannot bypass the external trust reference. |
| Oversized input/result, invalid enum, wrong result shape, exact integer beyond JS safe range | Matching typed acceptance/rejection across runtimes; no truncation or lossy conversion. |
| Runtime bound crossed and adapter exception | Stop with declared classification and no subsequent call. Assert counters and outcome; do not compare nondeterministic wall-clock values byte-for-byte. |

Compile twice and compare deterministic artifact bytes; run both hosts against one emitted bundle. Compare normalized typed results, call order/arguments, termination and consumer result. Record actual CEL preparation/evaluation latency and memory for the admitted bounded subset, adversarial input/AST rejection, and compile/preparation limits. Exercise actual CEL-ES in a terminable worker candidate and Java's configured controls; do not substitute an infinite JavaScript loop as CEL evidence. Fix numeric limits from evidence before acceptance; “TBD” limits are not an executable bound. Controlled clocks/counters can test budget semantics independently of machine-dependent deadlines.

Stop this increment when the command and rejection suite work on both runtimes and the maintained contract matches them. If parity or bounds cannot be established, reject the affected construct or pause that contract; do not ship only Node or silently weaken the guarantee. Local development examples may remain explicitly non-production while hard containment is unresolved, with that limit visible. Do not add the follow-up write, task database or agent loop to rescue a failing first increment.

## Runnable increment 2: scalar guarded creation and honest failure

### Outcome and entry conditions

Use the same compiler/runtime/package path for a deliberately smaller sample than the baseline invoice program. A sample host admits `TaskCompleted`; the extension reads the task, evaluates `source.status == "COMPLETED" && source.projectId == input.targetProjectId`, and conditionally calls `tasks.create` using a bounded title from source or business input. Both project fields have the same nominal `ProjectId` type. The create targets `source.projectId`; the host independently authorizes it. Protected origin comes from the admitted event, and the original key comes from trusted context. No task-specific eligibility capability or precomputed policy flag replaces this composition.

Both runtimes show created/no-op/rejection/uncertain outcomes from deterministic fake adapters. Original task completion is already committed in the host model and cannot be undone by a failed reaction. This does not implement the baseline tag/search/168-hour invoice program, promise snapshot-at-event semantics, or enforce a real transactional invariant.

Entry conditions: increment 1 accepted; manager retains the explicit one-write scope; guard/error/effect additions have agreed conformance cases; trusted key forwarding and simulation-only admission are specified. Publish a separate minimal sample DCM revision with scalar task projection and a create contract that does not require `dueAt`; do not silently alter the baseline DCM or claim its artifact compatibility. Define `targetProjectId` and proposed title as validated business inputs, not grants or protected identity. A real task database is not required to close this core increment. It is required before claiming the demo's uniqueness/replay guarantees.

### Minimum additions and contracts

- Reuse scalar equality, Boolean composition, enum refinement and conditional return. Read a required-field projection containing task ID, project ID, status and bounded title; return only required scalar fields from create. No lists, search, tags, time arithmetic, optional-field dereference or new CEL macro is needed. Explicitly reject references to guarded results until a supported presence proof is implemented; this program does not need one.
- Add `when` referencing a preceding unconditional Boolean and terminal `onError` for declared business errors. Count all potential calls and writes conservatively, including guards; at most one possible mutating call. Confirmed success alone advances; skipped call has absent result/no effect; handled business rejection preserves its classification. Transient failure stops; unknown commit stops as uncertain without ordinary return or retry.
- Admit `tasks.get` and protected `tasks.create`. Runtime validates results and arguments; the read adapter must return the requested source task within current authorized scope. Source task ID derives only from trusted admitted event input, while actor/tenant/key remain separate metadata. The equality guard is customer policy, never authorization; the adapter rechecks project scope and native invariants at create. A protected origin relationship must be checked by the host, not inferred from a potentially stale read. Record effect intent before dispatch, then confirmed/none/uncertain evidence. If pure return evaluation later fails, preserve an already confirmed effect instead of presenting the whole run as having no effects.
- Specify original opaque host-key type/value pass-through; key is not invocation ID or step ID. Reject missing/incompatible key before effect dispatch, with no coercion. Host simulation supplies keys through trusted harness configuration, not an extension/scenario privilege. No multi-key slots, derived keys or cross-binding fanout in this increment.
- Keep the generic runtime single-attempt. Fake duplicate/retention/payload-mismatch behavior is contract simulation, not real replay certification. Production admission stays blocked for the original pending fixture. Real replay policy belongs to the subsequent host increment.

### Outputs, demonstrations and acceptance

Extend the same public specifications, both persona examples, and maintained conformance fixtures; preserve original baseline inputs. Define nominal comparison rules, bounded title validation, scalar projection, guard absence, outcome/effect transitions and protected origin/key bindings. Scenario protocol binds the compiled artifact digest, verifies ordered arguments/outcomes and fails on missing/extra/unconsumed stubs. Separate program trace from simulated host outcome. Redact protected metadata in user output; trusted test assertions can verify key equality without publishing secrets.

Demonstrate in **both** hosts:

1. Completed task in the target project: exactly `get -> create`, correctly propagated title/project, protected origin, unchanged host key, confirmed effect and created result. Exercise both source-title and input-title variants without changing host policy code.
2. Noncompleted task or project mismatch: no create, absent guarded result, no-op. No search-before-create is present or needed for this demonstration.
3. Missing source or duplicate-origin business rejection: declared terminal record, original business-rejection classification and no fabricated success.
4. Read failure: no create. Definite create rejection: no confirmed effect. Lost response: uncertain effect, no normal return, no next step and no automatic retry. Deadline after dispatch is not proof of no write.
5. Invalid/missing key, protected origin override, second guarded write, unsafe error handler, unsupported macro or absent-result access: rejection at the relevant boundary before an unauthorized effect.
6. Wrong nominal comparison/argument, oversized title, missing/invalid scalar result, read returning the wrong source, authorization revoked before create, library/profile mismatch and forged artifact: fail closed. Matching project IDs alone must never authorize a write.
7. Mock duplicate delivery with original versus changed effective arguments: demonstrate declared dedupe/payload-mismatch behavior while labeling it simulated. Do not infer real database uniqueness from this test.

Resource evidence extends increment 1 with bounded scalar/title payloads, worst-case two calls, guard/error transitions and aggregate invocation allocations. Retain actual evaluator containment checks and test adapter deadlines separately, especially deadline expiry after effect dispatch. Do not re-run unrelated expression surveys or add macro qualification before this scalar path works. Stop if either runtime differs on guard/outcome/key behavior; never complete the increment with one runtime or fabricated no-effect evidence.

Accept only when the new source compiles, identical artifact bytes pass both runtimes, failure receipts remain honest, and resource/authority limits are documented with executable evidence. This completes the proposed early portable-core behavior, not the overall language/DCM feature, real demo integration, multi-effect support or production certification.

### Later bounded enhancement: baseline invoice program

Retain the original `tasks.get -> tasks.search -> guarded tasks.create` invoice program as an explicit subsequent enhancement of this sample, not a prerequisite to the first write demonstration. It adds bounded tag/result lists, `size`, one non-nested `exists`, complete-origin search semantics, optional Task field representation and timestamp plus literal **168 elapsed hours**. Publish a new supported profile/DCM revision and reconcile the original fixture's key/outcome contracts; do not reuse its example digest after changes.

Require no-match and maximum-size macro scans, aggregate allocation bounds, timestamp range/precision/overflow and Java/Node parity before admitting those constructs. Demonstrate tag match/miss, existing-follow-up no-op, exact due date, oversized/incomplete result rejection and the existing effect-failure suite. Search remains unconditional as in the source fixture and never proves atomic uniqueness. Real host uniqueness remains independently necessary with or without search. This enhancement remains feature work within the existing milestones, without a separate release or slice tracker.

## Parallel work and next decisions

With one core developer, keep compiler and both runners moving through each narrow scenario rather than assigning a complete layer at a time. Architect resolves the current contract and reviews evidence. Add a second core developer only after module boundaries and shared fixtures are stable enough to divide Java/Node work without duplicating the artifact contract. Do not create one now just to increase apparent throughput.

The demo developer can concurrently deliver task creation/listing/completion with extensions disabled and propose task-domain adapter contracts. After increment 1, they can integrate packaged read-only execution; after increment 2, connect the follow-up with real persistence and reviewed activation. Keep real database choice, transaction uniqueness, admission record/pinning, authorization and lost-response tests on that path. A minimal host-owned outbox/admission mechanism may be justified there; it is not a Keir runtime database. Choose first durable delivery admission as the default binding point unless product semantics require event-commit binding, and preserve that admission on redelivery.

The host assistant can first run as an authorized developer-facing integration with model validation and host conformance tools, without a separate chat product. Before using real credentials/model calls, choose provider, permissions and spend bounds through normal owner decisions. The customer assistant later reuses approved discovery/compiler/simulator tools in the app. Tests must prove that customer tool credentials cannot publish catalogs, that host-assistant proposals need human publication approval, and that neither assistant can self-approve activation. Live AI behavior needs its own bounded evaluation; canned transcripts are not proof it works.

Manager decisions requested before assignments:

1. Accepted in manager follow-up: REQUEST read/classify/consume first. Recommended reduced second increment: scalar guarded creation, preserving both runtimes throughout; retain the baseline invoice program as the later bounded enhancement above.
2. Accepted in manager follow-up: one-write initial execution with v0.7 key pass-through now; schedule multi-effect separately rather than importing its entire POC requirement.
3. Manager proposes repository-local Maven wrapper plus npm/TypeScript and selects host-controlled output with independently held expected digest for sample trust. Confirm dependency/toolchain compatibility on reported available runtimes; no installed-tool changes are authorized.
4. Reconcile live demo scope's obsolete comparative-value language and expose specification/agent deliverables on owning feature cards.

No estimates are asserted before the first executable integration reveals tooling and CEL/type-overlay cost. Review after each runnable increment; widen one capability at a time from evidence rather than promising all baseline features.

## Handoff and limits

Manager disposition (2026-10-29): reviewed the source-coverage and gap analysis, accepted REQUEST read/classify/consume followed by the reduced scalar effect as the first two runnable outcomes, and retained the original three milestones. Detailed acceptance and ownership are recorded in the existing portable-core card and the new single-effect card; baseline collection/time behavior is an explicit later enhancement. Host design-time agent integration is a distinct feature from customer authoring. The manager reconciled demo scope and recorded repository-local build recommendations; exact dependency versions and implementation contracts remain feature work. This is an initial development plan, not acceptance of unimplemented runtime or production guarantees.

This contribution belongs to `roadmap/features/specify-language-and-domain-model.md` (`id: specify-language-and-domain-model`). Supplied provider reference: `minimap`, scope `roadmap:v1:git:github.com/rore/keir-core#roadmap`, local `item:v1:specify-language-and-domain-model`. No fresh Pallium scope/self identity was available, so no participation association or Relay role verification is claimed; no recovery was attempted. The manager retains roadmap reconciliation and final plan ownership. This review does not complete the feature.

Only this review document was authored. No runtime implementation, dependency installation, tests, external benchmarking, commits, pushes or environment changes were performed. Drafts and reported historical evidence were inspected, not certified. Production safety, host correctness and the exact runnable scope remain subject to the stated implementation and evidence gates.
