# Read-and-decide implementation rationale

Status: implemented local checkpoint for `portable-core-first-slice`; compiler, both runtimes, shared expectations and independent packaged hosts pass verification. Product CI/protected-contract installation and production qualification remain open. Normative behavior lives in [the specifications](../specs/README.md).

## Decisions

- Retain authoritative Java compilation, checked CEL, pinned descriptors, both runtimes, host-owned grants/identity, agent independence, the same artifact for real/fake adapters, and separate execution/consumption outcomes.
- Support unguarded call/let/return, required scalar fields and READ/DISPLAY. Reject Int64, time, collections and optional values until separately specified and verified. Nominal/enum identity is a compiler refinement over strings; validate required fields before proto3 projection.
- Use one canonical JSON artifact containing the DCM, resolved plan and base64 protobuf. This avoids archive/path assembly. The independent host-held expected digest covers every artifact byte; artifact-local hashes do not authenticate compiler output. Profile-specific formats make no compatibility claim with provisional baseline schemas.
- Use native descriptor APIs, not generated business code. Descriptor naming/numbering and JSON field names are defined in [the artifact contract](../specs/compiler-artifact.md). Revise one shared contract if a bridge fails; never substitute incompatible per-runtime representations.

Inputs: relevant [baseline documents](design-baseline/README.md), accepted decisions 0001/0003 and [the development-plan review](development-plan-review.md).

## Qualification and remaining gates

[The committed scalar probe](../../conformance/cel-profile/README.md), integrated as `790ff35`, records 12 checked expressions, 24 Java/Node comparisons, 16 expression rejections and 17 invalid-input rejections per projection gate. Exact AST 64/65, depth 16/17, source 512/513 UTF-8 bytes and string 256/oversize checks passed. Its LF-preserved bundle SHA-256 is `2bd898abefa7b6baa16f7518779ee96c341a3142a1e27abf0671728d00a63e33`; this identifies a probe fixture, not a Keir artifact trust anchor. Node replay passed after integration; Java build/execution evidence is recorded in the probe report.

The [artifact specification](../specs/compiler-artifact.md) pins the qualified CEL/protobuf environment, including Java's required heterogeneous-comparison runtime option while numeric ASTs remain banned. Missing proto3 fields become defaults without validation; CEL-ES ignores erased checked maps. Neither behavior removes Keir's own validation duties.

Raw nominal-string equality in the probe remains unapproved without the implemented Keir overlay. Both runtimes now execute the full read-profile corpus and supplemental scalar/two-call artifacts, including variable/branch provenance, admission and bounded failure behavior. The [combined verifier](../../conformance/read-and-decide/README.md#verify-both-runtimes-and-packaged-hosts) checks deterministic compiler bytes, complete shared results and packaged hosts. Review added consumer-ID, record-conditional, canonical-DCM overflow and isolated expiry-clock regressions. Runtime-local artifact helpers preserve separate source/runtime boundaries; no shared module was needed. Worker batch termination remains supervision evidence only. Hard synchronous deadlines, complete memory isolation and production containment remain unproven.

## Implementation boundaries

Keep code, schemas and executable fixtures with the compiler/runtimes/conformance suite. Reconcile discoveries into the existing specifications and acceptance matrix. Preserve the tested scalar path while adding one capability at a time. Qualification and document review do not complete the broader language or runtime features.
