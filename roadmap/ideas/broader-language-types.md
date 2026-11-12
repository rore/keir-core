---
id: "broader-language-types"
title: "Proposal: extend types and safe dataflow beyond the selected example"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Decide which additional value families and expression operations deserve supported portable contracts after concrete use cases exist.

## In Scope

- Evaluate additional guarded-result proofs beyond the selected scalar slice, optional/null forms, exact int64/units/decimals and collection map/filter/sort operations not required by the committed example.
- Choose wire encodings, nominal overlays, precision/overflow and missing-value semantics per selected feature.
- Require real CEL and resource evidence in both runtimes; do not treat the draft schema or full CEL surface as implemented scope.

## Out of Scope

Implementing all draft v0/CEL constructs, dynamic casts, arbitrary functions or effectful iteration.

## Done When

- [ ] Decision: select a concrete scenario and bounded operation/type subset, or explicitly defer it.
- [ ] Before adoption: define grammar, DCM/artifact compatibility, diagnostics, resource analysis and shared conformance obligations.
- [ ] If adopted: create independently deliverable implementation work with an observable scenario and regression proof.

## Notes

**Delivered subset:** The owner accepted [guarded-result scalar dataflow](../features/guarded-result-dataflow.md) as an independently deliverable Core increment; it is now complete as a qualified candidate. After a successful guarded call, the selected slice permits scalar fields only in the true arm of a conditional using that exact immutable guard identifier. For example, conditionally create an acknowledgement and return its actual reference, otherwise return an existing reference from input. Optional fields retain the existing same-expression presence proof; failed calls terminate rather than selecting a fallback. The bounded contract, implementation, shared checks, independent source reviews, installed Java/Node package checks and edition-8 candidate companion are complete. The linked feature owns the actual evidence and combined acceptance. General predicate implication, proof transfer from a different alias, implicit availability in later guarded-call arguments, nullable records and list/record-valued conditionals are not adopted.

The [compiler](../../compiler-java/src/main/java/dev/keir/compiler/KeirCompiler.java), [artifact contract](../../docs/specs/compiler-artifact.md#lowered-steps-and-expressions) and both runtimes implement the exact-guard scalar rule at the `0.4.0` candidate boundary. Independently reconstructed guard provenance, lazy branches and bounded retained values have source-level evidence, supplemented by passed actual installed-package and companion qualification. Historical artifact identities remain unchanged, and older engines reject unsupported new semantics. This is not registry publication or demo adoption. Do not restart the completed scalar implementation or qualification from this umbrella proposal.

Exact integer arithmetic remains a separate proposed quantity/threshold increment; list length depends on its numeric result contract. Bounded predicates and transformations need their own comprehension, aggregate-cost and allocation evidence. Multiple effects are explicitly not the next increment. Only the linked guarded-result feature is adopted; the remaining umbrella proposal stays uncommitted. Evidence for that selected slice does not implement or qualify these other proposals.

**Dependencies:** [enrich-followup-expressions](../features/enrich-followup-expressions.md).

**Design input:** [Broader types/CEL, sections 2-5 and 11](../../docs/internal/design-baseline/public/keir-language-v0.md); [Type families, section 2](../../docs/internal/design-baseline/public/domain-model-v0.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
