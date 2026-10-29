# Read-and-decide implementation handoff

Status: architect-reviewed candidate, pending manager review and executable qualification. Scope: `portable-core-first-slice`, contributing to `specify-language-and-domain-model`; not completion of either feature. Base checkout: `4eadecb5eed451b72e01bcd25c88b6f71041820d`.

## Decisions retained and narrowed

Retain authoritative Java compilation, checked CEL with pinned descriptors, both runtimes, host-owned grants/identity, no agent dependency, same artifact for fake/real adapters and distinct execution/consumption outcomes. Source baseline is `docs/internal/design-baseline/public/{keir-language-v0,domain-model-v0,compiler-artifact-and-runners-v0,simulation-and-authoring-v0,host-integration-and-hook-contracts-v0.5}.md`; accepted decisions 0001/0003 and the completed development-plan review govern precedence.

Narrow the first scenario to unguarded call/let/return, required scalar fields and READ/DISPLAY. Int64 and temporal/collection/optional support are rejected rather than partially implemented. This revises the planning review's tentative inclusion of int64: the scenario needs none, and numeric wire semantics should not delay scalar execution. Enums/nominals remain compiler refinements over CEL strings; required-field validation must precede proto3 projection.

Choose a single canonical JSON artifact with embedded base64 protobuf instead of a directory/archive. This eliminates path traversal and multi-file assembly from the first loader while keeping checked binary expressions. The external expected digest covers the complete bytes; no approval/self-hash field authenticates anything. Keep the complete tiny DCM in the artifact for deterministic runtime binding; no second per-runtime DCM reconstruction. Profile-specific DCM/artifact names explicitly avoid claiming compatibility with provisional baseline schemas.

Use native library descriptor APIs, not generated business source. Descriptor message/field names, numbering and json_name rules are normative in `docs/specs/compiler-artifact.md`. If pinned CEL libraries cannot execute that bridge, revise the one shared contract before implementation. Do not independently substitute maps/dyn in Node or weaken nominal constraints.

## Evidence and open gates

At authoring time the manager reports JDK 21.0.12 and Node 24.14.1 on PATH, with repository-local Maven wrapper plus npm/TypeScript selected for investigation. Core developer 1 owns build bootstrap; developer 2 owns real CEL qualification. Historical CEL-Java 0.14.0/CEL-ES 0.6.1 results are reproduction inputs, not current verified pins. No complete runner conformance or hard resource guarantee is claimed in these specifications.

The in-progress developer 2 probe was inspected read-only: its Maven input selects CEL-Java 0.14.0 and its Node package inputs select `@bufbuild/cel` 0.6.1, `@bufbuild/cel-spec` 0.6.1 and `@bufbuild/protobuf` 2.6.2. It uses `keir_read.proto`/`keir.read.Task`, the proposed descriptor conventions and explicit boundary validation. These are observed qualification inputs, not a passing report or accepted runtime dependency matrix. Resolve the actual Java protobuf version and capture all transitive dependencies in build/lock evidence. The qualification bundle is not the normative artifact format and its single `task` variable is a probe, not a replacement for the full compiler variable environment.

Before marking the profile supported, record exact CEL/protobuf dependency versions, observed AST/overload allowlist, dynamic descriptor/required-value tests, Boolean/equality/conditional parity, deterministic protobuf/JSON vectors and resource-bound evidence. `runtime.md` supplies explicit conservative candidate ceilings to implement and test, not measured maximum safe workloads. Both implementations must agree on any revisions. Production containment remains unclaimed even if local scalar tests pass.

Blocking implementation decisions are limited to any failure of the descriptor bridge, missing exact qualified dependency pins/overload set, or inability to enforce required bounds. Do not block ordinary source/DCM/plan development waiting for unrelated broad CEL benchmarks. No activation signatures, registration services or general type system are required for this increment.

## Ownership and handoff

### First scalar qualification checkpoint

Developer 2 reported an actual passing run on 2026-10-29 from isolated `keir-cel-node`, still uncommitted while exact boundary measurements were being refined: 12 Java-emitted CheckedExpr expressions, identical descriptor bytes consumed by Node, 24 parity cases, 16 disallowed/ill-typed expression rejections and 17 invalid-input rejections at the Node gate. The reported descriptor uses `keir_read.proto`, proto3, `keir.read.Task`, sorted fields `enabled,id,projectId,status,title` numbered 1..5 and identical JSON names. Field selection, scalar equality/Boolean operations and enum-string literal conditionals passed, including `é😀` and present Boolean false.

Resolved pins: `dev.cel:cel:0.14.0`, transitive `dev.cel:protobuf:0.14.0`, `protobuf-java:4.35.1`; Node `@bufbuild/cel:0.6.1`, `@bufbuild/cel-spec:0.6.1`, `@bufbuild/protobuf:2.6.2`. Observed overloads: `conditional`, `equals`, `logical_and`, `logical_not`, `logical_or`, `not_equals`. Java planner options require heterogeneous numeric comparisons enabled, but the admitted AST still excludes numbers. The public contracts now record this requirement.

Missing `task` variable produced an error; an omitted raw proto3 string evaluated as empty, confirming that strict pre-projection validation is necessary. CEL-ES evaluated even after checked maps were erased; the separate gate rejected that corruption. These observations support fixed environment/admission checks, not intrinsic CheckedExpr safety. Nominal/enum identity still requires the Keir overlay. Source 512-byte, string 256-byte and depth-16 boundaries were reported tested; AST 62 nodes passed and 65 rejected, with exact 64-node testing still pending. No whole-runtime, hard deadline or complete containment result was reported. This record attributes developer evidence; the architect did not rerun the harness. The final committed qualification report remains an integration dependency.

### Refined and committed qualification evidence

Developer 2 subsequently reported the refined phase-A run: exactly 64 AST nodes accepted/65 rejected; depth 16/17; source 512/513 UTF-8 bytes; string 256 accepted and 257 ASCII or 258 UTF-8 bytes rejected. Both Java and Node projection gates rejected the same 17 invalid inputs. All 24 primitive parity cases and 16 expression rejections still passed. Two separate Java processes emitted identical qualification bundle SHA-256 `473d05ad3f78c3f589c3d7a6973dee39da4332366791020c623513f1c594c8c6`. This is the probe bundle digest, not a normative Keir artifact or production trust anchor.

The probe explicitly labels raw nominal-string equality `nominal_erasure` with `requiresKeirOverlay`: passing raw string comparison does not authorize cross-nominal equality. Only literal-branch enum conditionals were qualified; full semantic overlay, multiple CEL variables and general scalar branch provenance remain implementation acceptance work. Actual-CEL worker batch termination supplies supervision evidence only; Java hard-deadline evidence remains absent. Final source/evidence commit was still pending when this report arrived; manager must integrate its committed report from `conformance/cel-profile/README.md` and `evidence/`. The earlier pending exact-boundary statement is superseded by this refinement; unrelated parser/aggregate/runtime limits remain candidates.

Final qualification source/evidence is committed as `685800ff1962705b5b45d514fc0ed990a485d18c` in `keir-cel-node`, reported clean and retained. Its final LF-preserved bundle SHA-256 is `2bd898abefa7b6baa16f7518779ee96c341a3142a1e27abf0671728d00a63e33`, superseding the earlier Windows pretty-print bundle digest above; underlying checked expressions are reported unchanged. The stored reports retain 24 primitive parity cases/12 expressions, 16 expression rejections, 17 invalid-input rejections in each gate and the exact boundaries recorded above. This resolves the pending qualification-commit reference, not the broader semantic-overlay/runtime acceptance gates. Evidence is developer-reported and was not rerun by the architect.

### Implementation ownership

Architect owns these five specifications/index and two guide updates. Developers own build, code, schemas and fixtures in their reserved checkouts. Minimum handoff is the complete sample model/source, exact artifact/API and acceptance matrix, not an additional tracker. Manager owns roadmap and integration; this work changes no roadmap files. Optional Pallium association is unavailable because no fresh self/scope was injected; no identity or work association is inferred.

Focused documentation checks must validate sample model shape, Markdown links and consistency; runtime tests remain developer evidence. Merge these contract changes only through manager review, then reconcile implementation discoveries in the same documents. Do not describe candidacy as certification or shipping support.
