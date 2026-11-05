# Real application design audit and correction plan

Status: **proposed corrections; implementation on hold**. Prepared on 2026-11-04 after the user requested a return to the guiding design. This report does not change the normative API or authorize a runtime release. The manager has adopted the hold on affected implementation, acceptance and rollout and is coordinating the developers. Preserve existing work and evidence.

## Conclusion

The mistake is not using SQLite in the demo or testing real host guarantees. It is promoting a particular deployment-verification mechanism into the generic real-effect contract, then building application gating and operational machinery around it. I authored and endorsed that direction. Reviewing its implementation against its own rules did not adequately test whether those rules belonged in Keir.

Keir sees a host-approved typed capability and a registered callable. That callable may use a local method, database, HTTP or RPC. The host owns the actual guarantee and evidence for it. Keir must enforce the declared contract, compatible approved bindings, current authority, bounds and truthful outcomes; it cannot establish the truth of an arbitrary host implementation by inspecting a report digest.

My initial conversational correction also went too far: the baseline explicitly requires host evidence/certification. Removing all host approval or verification would contradict it. The correction is to separate the required guarantee from the particular mechanism used to establish and approve it.

## Source map and retained principles

| Source | Principle to retain |
| --- | --- |
| [Architecture sections 1-4](../design-baseline/public/keir-architecture.md#1-decision-snapshot) | Closed, agent-independent language/compiler/runtime; host supplies capabilities, lifecycle, authority and real semantics. No compensation for incorrect host contracts. |
| [Architecture sections 6-7](../design-baseline/public/keir-architecture.md#6-compiler-portable-artifact-and-runner) | Prepare once and invoke repeatedly with fresh context; same artifact/runner with fake or real adapters; distinguish simulation from native integration tests. |
| [DCM sections 3 and 6](../design-baseline/public/domain-model-v0.md#3-capability-definitions) | Native and service-backed capabilities share a typed contract. Host substantiates guarantees; semantic changes require compatibility review. Certification references are host authority, not author assertions. |
| [Host integration sections 1, 5, 11 and 12](../design-baseline/public/host-integration-and-hook-contracts-v0.5.md#1-scope-and-ownership) | Host owns transactions, event delivery and approval; Keir enforces bounded dispatch. Host conformance is required, without confusing a trusted SDK object with authentication. |
| [Idempotency sections 3-7](../design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md#3-responsibility-boundaries) | Original host key passes unchanged; real deduplication/mismatch/retention semantics are host contracts. No automatic replay or invented certainty. |
| [Decision 0001](../decisions/0001-repositories-and-runtimes.md), [0002](../decisions/0002-product-proposition-and-evaluation.md), [0003](../decisions/0003-restricted-cel-resource-policy.md) | Both runtimes are initial scope; product proposition is accepted; resource/containment claims require actual evidence. Historical Java-later and market-validation prerequisites must not return. |
| [Generic capabilities](../../specs/generic-hook-capability-contract.md#capability-invocation-across-services), [model binding](../../specs/host-model-binding.md#invocation-and-remote-dispatch) | HTTP/RPC is a normal host integration; reuse a complete binding, retain semantic pins, and check fresh approval/revocation and resource authority on dispatch. |

The historical DCM even proposes a host certification registry. That supports genuine host approval of contracts, not a conclusion that Keir must prescribe a registry, database, report schema or machine identity. Current decisions and explicit user corrections govern the chosen mechanism.

## Findings and required corrections

### F1. The first demo's evidence boundary became a restriction in the generic contract

**Evidence:** [Real admission](../../specs/real-single-effect-admission.md#host-qualification-input) says supported real invokers use the local SQLite transaction boundary and other hosts require separately reviewed reports and explicit support. Its introduction names the SQLite handoff as the first qualification. The [roadmap item](../../../roadmap/features/real-effect-admission.md) couples core design to the local adapter. This conflicts with the established separation between a logical capability and its implementation when interpreted as the core's allowed host architectures.

**Correction:** Define generic admission in terms of a trusted, versioned capability contract and host-approved binding. State the same obligations for local and HTTP/RPC-backed adapters. Put SQLite transaction details and the limits of measured demo evidence in demo integration/test documentation. A remote adapter still needs truthful key, authorization, deadline and outcome behavior; do not claim it has been verified merely because the generic API admits it.

**Acceptance:** The core contract and embedding example contain no database/OS-specific prerequisite. A conforming host callable can bind without a database report. A local loopback service test can exercise response loss/UNKNOWN through a host-owned adapter on the same machine; it does not certify external providers or require a second host/network framework.

### F2. A fixed deployment-evidence protocol duplicates existing host authority

**Evidence:** [Real admission](../../specs/real-single-effect-admission.md#host-qualification-input) requires `{evidence,isCurrent}` with `hostBuild`, `reportDigest`, revision, model/environment/key identity and a repeated complete catalog. Node `runtime-node/src/model.js::qualificationSnapshot/requireCurrent` and Java `KeirRuntime::qualification/isCurrent` enforce this shape. Invocation paths add repeated qualification checks. The existing [binding contract](../../specs/host-model-binding.md#invocation-and-remote-dispatch) already makes host current approval/revocation part of authorization. The new hashes are opaque to core; neither runtime verifies SQLite behavior or contains SQLite-specific execution logic.

**Correction:** Remove the mandatory report/build schema from the corrected generic API. Reuse trusted binding approval, semantic model/capability/consumer/compiler pins, explicit real-versus-simulation registration and the existing fresh authorizer. The host must explicitly approve required guarantees before publishing a real binding and must enforce revocation on invocation and every reached call. Bind-time approval does not cache that authority. Keep certification/version references only where required by the semantic contract; the host resolves their truth and lifecycle. Do not replace the protocol with an untrusted `approved: true` flag or remove current-approval checks.

**API direction:** Restore the ordinary bind/prepare/invoke model for the corrected real profile; no generic report parser or extra evidence-currentness callback. Confirm equivalent revocation checks, including after asynchronous authorization and before actual native commit, before removing the duplicate path. Node's qualification-specific Promise cleanup then leaves with that callback; this is not a standalone weakening of existing callback/error handling.

**Acceptance:** Missing trusted binding, incompatible semantic pins, mixed modes, denied/revoked authority, wrong key, expired deadline and malformed outcomes still fail correctly with no unauthorized dispatch. A false or failed host authorizer cannot permit a write. Java and Node share identical artifacts and cases. A forged customer/scenario report grants nothing because that is not an authority input.

### F3. Whole-installation attestation became a prerequisite for using the demo

**Evidence:** The [integration record's qualification addendum](../../../.agent-workflow/tasks/real-task-customization-integration.md) requires a build/report/approval bundle and exact Node/SQLite/platform/architecture/schema/PRAGMA tuple. Inspected demo `src/host-qualification.mjs::executionBuild` hashes all backend modules, extension JSON, packages/compiler and recursive production dependency contents; startup compares that manifest exactly. `docs/task-effects.md` and `scripts/qualify-task-effects.mjs` make the bundle an activation prerequisite. My subsequent CI advice accepted a Windows-only approved tuple and separate qualified-browser lane as a consequence of this mechanism.

**Correction:** Keep package integrity, supported-runtime requirements, native integration tests and an honest release test matrix. Remove automatic feature disablement tied to every installed-file/platform fingerprint unless a concrete host requirement independently justifies it. Keep evidence as engineering/release evidence, not a universal runtime credential. Retain genuinely necessary database safety checks, such as required foreign-key enforcement, as host-native invariants. Do not label a platform supported without testing it; equally, do not make it unsupported solely because it differs from a report's fingerprint.

**Acceptance:** An ordinary supported clone/setup can run the reviewed demo after normal trusted host configuration and explicit customer approval/activation, without manufacturing report/approval JSON. Default isolated CI can test the actual runtime/native flow under test-owned authority. Test fixtures remain isolated and cannot authorize user data. Separately selected crash and real-provider/browser tests retain meaningful failures and zero-skip acceptance when selected. No second host or host installation change is required.

### F4. Deployment-proof fields leaked into native business services and durable state

**Evidence:** The [handoff](real-task-effect-handoff.md#durable-records) pins qualification/report/build into events. Demo `src/data.mjs::taskEffectService.current` checks those fields and a separate `isQualificationCurrent` callback before claim/create. `src/extension-lifecycle.mjs` stores the tuple in selection pins; `src/task-effects.mjs` compares it again. The qualification module also maintains a separate persistent revocation file and updates pending events/selections.

**Correction:** Keep native owner/resource access, original-key validation, atomic task/receipt commit, immutable selected artifact/contract revision and irreversible revocation of affected admissions. Remove mandatory deployment-report/build fields from the business-service authority contract. Let the host's existing lifecycle/authorizer represent current approved bindings. Consolidate redundant approval state only after mapping each denial/revocation guarantee to its retained check.

**Preservation:** Do not erase existing event/receipt/history fields or rewrite the shipped schema-8 migration. Retain old evidence as audit data. A migration or reader compatibility change must explicitly distinguish old/new admissions; unresolved old pending work remains held, and STARTED work is never replayed. Copying old approval into a corrected write contract is forbidden. Disabling one verification mechanism must not resurrect a previously revoked admission.

**Acceptance:** Cross-owner access, disable/delete/revoke, restart, selection replacement and matching/mismatched duplicate tests retain their outcomes. Saved CONFIRMED/UNKNOWN evidence remains inspectable under current read authorization. No test should require a report digest merely to prove a native task transaction is authorized.

### F5. Per-event binding and preparation abandoned reusable execution

**Evidence:** The [integration record](../../../.agent-workflow/tasks/real-task-customization-integration.md) explicitly accepted per-event binding. Inspected demo `src/task-effects.mjs::firstAttempt` constructs event-capturing invokers and a `candidate === key` validator, then binds and prepares on every event. This is a documented design compromise I endorsed, not a proven latency defect. It conflicts with the baseline's prepare-once direction and the reusable API already implemented.

**Correction:** Use stable host invokers and a host-owned authenticity/type validator for native keys; supply event/owner/original key/deadline/current authorizer through fresh invocation context. Reuse binding and Prepared for an exact approved scope/model/artifact/consumer/semantic-runtime identity, with bounded ownership/lifetime rather than a new generic cache framework. Validate event/key ownership in native code on each call. Preserve the same original key object within an invocation; that does not require a new binding for every object.

**Acceptance:** Two different events reuse preparation without sharing identity, keys, results, deadlines or authority. A revoked program is denied despite a cached Prepared handle. Original-key identity and cross-owner rejection remain tested. Count preparations/calls to establish reuse; measure cold/warm behavior before any performance claim. Keep this as separately reviewable work if it would obscure the authority correction.

### F6. Guidance and reviews failed to catch architectural drift

**Evidence:** [Architect guidance](../../../roles/architect.md) previously required contract/evidence review but no explicit check against guiding principles. The [assistant overview](../../agents/README.md) and both assistant guides still describe effects as simulation-only while the real-effect contract is implemented. The handoff still calls itself unimplemented; the roadmap combined old assignment prose with newer delivery evidence. My callback consultation and CI advice optimized a newly imposed mechanism instead of checking whether it was necessary.

**Correction:** Persist a short source-linked design check in the architect guide now. Reconcile normative specs, examples, product assistant guides and roadmap status with the accepted corrected contract together. For each new gate record the required outcome, responsible layer, existing mechanism and reason the added mechanism is necessary. Independent review must compare the proposed design with user intent and baseline/current decisions, not just compare code to its tests.

**Acceptance:** A new architecture review can locate governing principles without reconstructing chat. It distinguishes host obligation from core enforcement, trusted host code from untrusted authored programs, semantic identity from deployment mechanism, and measured scope from architectural support. Manager's records clearly keep technical evidence while marking the superseded design on hold. No second policy registry or recurring approval ritual is needed.

## What remains correct

- Host-approved generic `tasks.get` and `tasks.create`; project/title/conditions in authored source rather than a hidden follow-up adapter. Keep other useful host operations possible through normal catalog evolution.
- Exact artifact, compatible model/consumer/compiler and semantic runtime pins; separate trusted real/simulation bindings; current owner/resource authority and explicit customer approval/activation.
- Original opaque host keys, payload mismatch rejection, atomic task/receipt persistence in this demo, retained outcomes and no blind replay. SQLite is appropriate for implementing and testing this host contract.
- Native completion independent of reaction failure; owner-bound event selection; bounded first dispatch; no STARTED replay; no retroactive work on activation; honest NONE/CONFIRMED/UNKNOWN and separate reconciliation.
- Preservation of existing data, legacy read-only history and accounting during the user-requested advice retirement.
- The accepted first increment is scalar, one write, one completion hook. This audit does not restore tags/dates/search/multi-write or all historical examples as prerequisites. It does not reopen product value research or Java-later sequencing.
- No arbitrary host-code sandbox claim. Keep meaningful defenses and resource enforcement, but do not build generic containment machinery around a removable trusted-host callback.

## Concrete sequence and ownership

| Order | Owner and deliverable | Exit condition |
| --- | --- | --- |
| 0 | Manager holds affected implementation, acceptance and rollout; developers preserve WIP and evidence. | Hold adoption established; no rollback, data cleanup or live environment action. |
| 1 | Architect supplies this source-backed correction and exact revised generic contract; manager reconciles the owning records and obtains the applicable behavioral review. | F1-F4 authority mapping and compatibility policy are settled before code release. No approved guarantee silently disappears. |
| 2 | Core developer updates admission API/validation in both runtimes, compiler assurance/profile rules as needed, tests, examples and package docs. | Shared semantic/security cases pass; no database/report/platform prerequisite; /1-/2 unchanged. |
| 3 | Demo developer replaces runtime qualification gating with trusted semantic binding/lifecycle authority and normal release evidence; updates dispatcher/service/selection/setup/tests. | Retained authority/revocation/receipt cases pass; ordinary supported setup works; old state preserved. F5 reuse is a distinct checked change. |
| 4 | Manager integrates verified packages; developer/designer exercise actual authoring, review, activation, task completion and outcome. | Same artifact in simulation/real; matching/nonmatching scenarios, duplicate/revocation/UNKNOWN behavior, usable native CRUD and honest customer feedback. |
| 5 | Architect/manager reconcile specifications, assistant guides, examples and roadmap. | One current documented contract and measured limits; review confirms baseline alignment, not just internal consistency. |

### Compatibility decision before implementation

Effect/3 is already integrated in core; it cannot be silently reinterpreted as though its evidence protocol never existed. Inventory shipped consumers and retained demo states first. Default to an explicit corrected API/profile/package boundary while preserving old readers/behavior and rejecting unsupported mixed identities. A coordinated pre-release replacement is an alternative only if its consumer/data scope is verified and accepted; the `0.0.0` package label alone is not evidence of no consumers. Do not retrofit real permission into effect/2 or bypass effect/3 by passing fabricated evidence.

### Affected surfaces

Core: `docs/specs/real-single-effect-admission.md`, `host-model-binding.md`, `runtime.md`, `compiler-artifact.md`, `language.md` and the spec index; `docs/internal/real-task-effect-handoff.md`; both product assistant guides and overview; compiler `Dcm.java`; Node `model.js`, `index.js`, `validation.js`; Java `KeirRuntime.java`, `Artifact.java`; real-effect admission/scenario tests, `conformance/generic-hooks/effect3/`, its verifier, packaged examples and runtime READMEs. Update profile fixtures/digests as one compatible unit, not manually patched golden output.

Demo (paths relative to its repository): `src/host-qualification.mjs`, `task-effects.mjs`, `data.mjs`, `extension-lifecycle.mjs`, `app.mjs`, catalog/selection pins and affected authoring text; `scripts/qualify-task-effects.mjs`; `extensions/task-effect/`; qualification/dispatcher/runtime/native/lifecycle/preservation tests and fixtures; setup/run/task-effect docs and CI selection. Retain valuable native process tests and package provenance while removing their misuse as runtime credentials. Developer must run a caller/reference inventory before deletion; not every existing use of the word qualification refers to this protocol.

Manager-owned: `roadmap/features/real-effect-admission.md`, `demo-task-integration.md`, affected milestone/status notes, and their existing implementation Work Records. Preserve accepted test evidence as historical implementation evidence, not proof that the challenged design should remain.

## Evidence limits

Inspected baseline/current documents, core implementation integrated at `d59e375` (documentation checkout initially `f845fbe`), demo foundation `f02932b`, and uncommitted integration source based at `aa2126a`. The integration source is not claimed as a frozen reviewed revision. This is an ownership/design audit of these definitions and connected paths, not a whole-codebase bug audit. No runtime, browser, native-process or performance tests were run for this report. Existing test counts are not re-certified. The report proposes corrections; it does not establish that any proposed removal is already safe or implemented.
