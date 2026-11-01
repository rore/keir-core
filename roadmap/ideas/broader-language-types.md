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

- Evaluate guarded-result presence, additional optional/null forms, exact int64/units/decimals and collection map/filter/sort operations not required by the committed example.
- Choose wire encodings, nominal overlays, precision/overflow and missing-value semantics per selected feature.
- Require real CEL and resource evidence in both runtimes; do not treat the draft schema or full CEL surface as implemented scope.

## Out of Scope

Implementing all draft v0/CEL constructs, dynamic casts, arbitrary functions or effectful iteration.

## Done When

- [ ] Decision: select a concrete scenario and bounded operation/type subset, or explicitly defer it.
- [ ] Before adoption: define grammar, DCM/artifact compatibility, diagnostics, resource analysis and shared conformance obligations.
- [ ] If adopted: create independently deliverable implementation work with an observable scenario and regression proof.

## Notes

**Dependencies:** [enrich-followup-expressions](../features/enrich-followup-expressions.md).

**Design input:** [Broader types/CEL, sections 2-5 and 11](../../docs/internal/design-baseline/public/keir-language-v0.md); [Type families, section 2](../../docs/internal/design-baseline/public/domain-model-v0.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
