# Read-and-decide implementation rationale

Status: implemented local checkpoint for `portable-core-first-slice`; compiler, both runtimes, shared expectations and independent packaged hosts pass verification. Product CI/protected-contract installation and production qualification remain open. Normative behavior lives in [the specifications](../../specs/README.md).

## Decisions

- Retain authoritative Java compilation, checked CEL, pinned descriptors, both runtimes, host-owned grants/identity, agent independence, the same artifact for real/fake adapters, and separate execution/consumption outcomes.
- Support unguarded call/let/return, required scalar fields and READ/DISPLAY. Reject Int64, time, collections and optional values until separately specified and verified. Nominal/enum identity is a compiler refinement over strings; validate required fields before proto3 projection.
- Use one canonical JSON artifact containing the DCM, resolved plan and base64 protobuf. This avoids archive/path assembly. The independent host-held expected digest covers every artifact byte; artifact-local hashes do not authenticate compiler output. Profile-specific formats make no compatibility claim with provisional baseline schemas.
- Use native descriptor APIs, not generated business code. Descriptor naming/numbering and JSON field names are defined in [the artifact contract](../../specs/compiler-artifact.md). Revise one shared contract if a bridge fails; never substitute incompatible per-runtime representations.

Inputs: relevant [baseline documents](../design-baseline/README.md), accepted decisions 0001/0003 and [the development-plan review](development-plan-review.md).

## Qualification and remaining gates

[The committed scalar probe](../../../conformance/cel-profile/README.md), integrated as `790ff35`, records 12 checked expressions, 24 Java/Node comparisons, 16 expression rejections and 17 invalid-input rejections per projection gate. Exact AST 64/65, depth 16/17, source 512/513 UTF-8 bytes and string 256/oversize checks passed. Its LF-preserved bundle SHA-256 is `2bd898abefa7b6baa16f7518779ee96c341a3142a1e27abf0671728d00a63e33`; this identifies a probe fixture, not a Keir artifact trust anchor. Node replay passed after integration; Java build/execution evidence is recorded in the probe report.

The [artifact specification](../../specs/compiler-artifact.md) pins the qualified CEL/protobuf environment, including Java's required heterogeneous-comparison runtime option while numeric ASTs remain banned. Missing proto3 fields become defaults without validation; CEL-ES ignores erased checked maps. Neither behavior removes Keir's own validation duties.

Raw nominal-string equality in the probe remains unapproved without the implemented Keir overlay. Both runtimes now execute the full read-profile corpus and supplemental scalar/two-call artifacts, including variable/branch provenance, admission and bounded failure behavior. The [combined verifier](../../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) checks deterministic compiler bytes, complete shared results and packaged hosts. Review added consumer-ID, record-conditional, canonical-DCM overflow and isolated expiry-clock regressions. Runtime-local artifact helpers preserve separate source/runtime boundaries; no shared module was needed. Worker batch termination remains supervision evidence only. Hard synchronous deadlines, complete memory isolation and production containment remain unproven.

## Implementation boundaries

Keep code, schemas and executable fixtures with the compiler/runtimes/conformance suite. Reconcile discoveries into the existing specifications and acceptance matrix. Preserve the tested scalar path while adding one capability at a time. Qualification and document review do not complete the broader language or runtime features.


## Original pre-operation rollout notes

Historical implementation/release planning captured from the [pre-simplification contract](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/specs/pre-operation-decisions.md). Current normative behavior is in the [decision contract](../../specs/pre-operation-decisions.md).

At the inspected baseline, existing custom enums/records can already express ALLOW/DENY and additional bounded output fields; `effectGrant:0` can already prohibit writes; Outcome validation can distinguish completion from failure. What they cannot express is a supported pre-operation lifecycle: `compiler-java/.../Dcm.java`, Java `Artifact.java` and Node `validation.js` explicitly require EVENT/AFTER_COMMIT/EVENT_REPORT. Invoking that declared event before commit would contradict its contract. The change is this lifecycle/consumer alternative plus its validation, not a new execution mechanism. The artifact schema also fixes `modelRef.format` to `keir-dcm/2`; a new DCM contract therefore changes the artifact's accepted model-reference structure. The scenario schema only admits event-report consumption. These concrete constraints justify the version changes below; status labels alone would not.

## Compatibility and releases

Identities version independently. Core 0.3.0 and companion edition 5 are published together on the [Core 0.3.0 release](https://github.com/rore/keir-core/releases/tag/v0.3.0). Their contract identities are:

| Contract | Value | Reason |
| --- | --- | --- |
| Source | Keep `keir/2` | No source syntax/type/step change. |
| DCM | `keir-dcm/3` | New lifecycle/consumer semantics and conditional effect default. |
| Artifact | `keir-artifact/3` | Newly admitted entrypoint execution meaning; retain envelope/step representation. |
| Descriptor recipe | Keep `keir-record-descriptors/2` | No value or protobuf representation change. |
| Scenario | `keir-scenario/3` | New decision-consumption variants; retain outcome/call envelope. |
| Compiler/packages | Core `0.3.0`, producer `keir-compiler-java/0.3.0` | Released compiler/runtime pair for this contract. |
| Authoring companion | Edition `5` | Published with verified packages; covers pre-operation decisions and failure boundaries. |

Keep dependency pins unchanged unless implementation evidence requires a separate change. Compiler 0.3.0 is the published authoring compiler for this release. It accepts source `keir/2` with DCM `/3` only, producing artifact `/3` with its truthful 0.3.0 identity. It rejects DCM `/2`; it never stamps a 0.2.0 producer. Old `/2` artifacts are preserved for execution/history only, not a parallel authoring path. Runtime 0.3.0 admits exactly (artifact `/2`, DCM `/2`, producer 0.2.0, descriptors `/2`) and (artifact `/3`, DCM `/3`, producer 0.3.0, descriptors `/2`), with the existing exact environment pins in both. Reject mixed tuples and guard semantics in `/2`. Published Core 0.3.1 retains these exact cases and adds only producer 0.3.1 for artifact/model `/3`; see the [current matrix](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/specs/compiler-artifact.md#source-and-artifact-identity). It preserves source syntax, formats and environment pins.

Existing approved post-commit artifacts/model snapshots stay on their old exact pair when the runtime package is upgraded. Do not rewrite approvals or receipts. Publish the current host authoring catalog as `/3`, including unchanged EVENT/AFTER_COMMIT/EVENT_REPORT semantics alongside the new guard alternative. New or edited programs compile against that current catalog and require normal approval before replacing an old selected rule. The host may retain exact old model bindings solely to execute existing approved rules; this reuses existing model binding and is not multiple selected programs at one hook. Historical source can seed a new draft, but old approval never transfers to its recompiled bytes. Package/provenance validation must explicitly recognize the new runtime's support for the old pair rather than fabricate old package integrity. No silent deactivation or recompilation of selected rules during package installation.

Update domain-model, host-model-binding, runtime, compiler-artifact, spec index, schemas, example consumers and release compatibility matrix together during implementation. Update `docs/agents/host-integration-assistant.md`, `extension-assistant.md`, the new customer guide and companion compatibility record: discover the exact hook, compose READs, return a decision, test DENY/failure and native consumption, and never infer native commit from ALLOW. Core 0.3.0 includes these reference and package-record updates.

## Acceptance matrix and implementation split

Each runtime case uses one compiler-produced artifact/model pair in both runtimes; compare classification, result, effects, ordered calls and consumption. Forged artifacts are tested at preparation, not only compiler rejection.

| Cases | Required evidence |
| --- | --- |
| Constant/input-only ALLOW and DENY; READ-derived ALLOW/DENY | Same typed outcome and consumer status in Java/Node; effects empty. |
| All existing `/2` event cases | Unchanged result, call/write ceilings and receipts, including failed-after-CONFIRMED and UNKNOWN. |
| Wrong lifecycle tuple, wrong enum/output fields, effectGrant 1 | DCM/compiler/binding rejection. Both ALLOW/DENY enum orders are accepted; missing, duplicate or extra values reject. Reordering still changes the exact model digest. |
| WRITE, including false guarded WRITE, forged prepared plan | Reject before any adapter dispatch. |
| Denied/transient/malformed READ; handled rejection with ALLOW | BLOCKED; zero native mutation attempts. |
| Missing/extra/mistyped result, unknown decision, undeclared message, forged trace or effects | Consumer BLOCKED. Declared message round-trips safely as data in both runtimes. |
| Expiry before start/during READ/after result; regressing clock; cancellation and late ALLOW | No native continuation; late settlement cannot change terminal outcome. |
| Missing/mistyped input, required protected binding or event shape; null original key | Core validation error before dispatch. No-call plans legitimately have no actor binding. |
| Forged/non-null native key, wrong owner or unauthenticated provenance, including pure ALLOW | Host authentication/commit gate blocks native continuation; do not expect Core to authenticate them. |
| Compiler 0.3 with DCM `/2`; mixed artifact/model/producer pair; `/2` guard | Reject. Existing compiler 0.2 `/2` artifacts and new compiler 0.3 `/3` artifacts both run on runtime 0.3 with their exact models. |
| Explicit no binding versus selected-but-unavailable | Native path only for verified absence; unavailable blocks. |
| Changed input, approval, selection, native permission or read fact before commit | Host blocks; race tests prove no native commit. |
| Native failure after ALLOWED | ALLOWED retained only as decision consumption; native failure reported separately. |
| Nested read-only guard under a post-commit WRITE | Bounded host work/depth; one native write; outer evidence preserved. |

The generic host harness supplies an independently observed native-operation counter/result, separately from the consumer status. Tests assert DENIED/BLOCKED cause zero attempts and ALLOWED reaches native validation exactly once. Simulation proves the harness behavior only. Real-host acceptance must additionally observe native persistence/non-persistence and unchanged authorization, concurrency and replay semantics. No new Core durable receipt format is required.

Compiler/Java owner: DCM validation, compiler identity selection, shared fixtures/schema updates, Java binding/preparation/runtime checks and Java consumer/harness. Node owner: same exact model/artifact pairs, validation and runtime checks, Node consumer/harness, parity and packaged execution. Host owner: activation/absence semantics, creation paths, consistent READ adapters, deadline reserve, replay/nesting, real persistence and UI/API tests. Manager owns independent review, package allocation and combined release acceptance.

Core 0.3.0 has passed contract review and packaged Java/Node verification. Each host integration must still qualify its read-consistency mechanism, time reserved for native checks and commit, cancellation and native persistence. Shared simulation and packaged examples establish Core and harness behavior; real-host guarantees require host-specific evidence.
