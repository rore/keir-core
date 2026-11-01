---
id: "machine-readable-contracts"
title: "Publish current schemas and agent-consumable contracts"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-core"
---

## Summary

Make the actual supported language, DCM, artifact, simulation and diagnostics usable by authoring tools without copying draft prose into prompts.

## In Scope

- Publish profile-specific schemas, valid/invalid examples, capability-discovery inputs and stable diagnostics/API references.
- Reconcile current parsers, semantic checks and schema limits; structural schema validation never substitutes for authority or semantic validation.
- Maintain compact separate host-design and customer-authoring guidance; consider skill packaging only as a delivery format.

## Out of Scope

A second language specification in prompts, an agent framework in core or an automatic schema importer.

## Done When

- [ ] Design/spec: identify the normative contract for each current profile and the fields a host tool may expose.
- [ ] Implementation: tools can consume versioned machine-readable definitions and errors; both persona guides use those same contracts.
- [ ] Verification: examples/schemas match the actual validators, diagnostics are repairable and scoped, and forged/unsupported authority stays rejected.

## Notes

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Structural schema boundaries](../../docs/internal/design-baseline/public/schemas/extension-ir-v0.schema.json); [Discovery, section 3.3](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Types and validation, sections 2 and 7](../../docs/internal/design-baseline/public/domain-model-v0.md).
