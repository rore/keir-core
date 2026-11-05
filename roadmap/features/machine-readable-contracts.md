---
id: "machine-readable-contracts"
title: "Publish current schemas and agent-consumable contracts"
status: "active"
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
- Publish a compact, versioned customer-authoring guide that a host can supply to its agent. Teach discovery, typed composition from the DCM, compiler diagnostics, independent examples, and review. Keep host-specific vocabulary, tool implementations and policy in host context. Maintain separate host-design guidance.
- Version and release the guide, schemas and examples together under the [authoring package release contract](public-package-release.md). Keep host model identity separate so hosts can evolve their own catalog within a supported profile.

## Out of Scope

A second language specification in prompts, an agent framework in core or an automatic schema importer.

## Done When

- [ ] Design/spec: identify the normative contract for each current profile and the fields a host tool may expose.
- [ ] Implementation: tools can consume versioned machine-readable definitions and errors; both persona guides use those same contracts.
- [ ] Verification: examples/schemas match the actual validators, diagnostics are repairable and scoped, and forged/unsupported authority stays rejected. Daymark consumes the pinned shared guide, with host-specific context supplied separately.

## Notes

**Current discovery:** Identify the smallest structural schema and example set for the supported `effect/4` source, DCM and artifact. Compare it with the actual compiler and Java/Node loaders and identify checks that remain native. Broader cross-profile and API coverage remains queued.

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**First increment delivered:** The independently reviewed [read/2 structural contracts](../../schemas/read-2/README.md) are on main: three schemas, examples, native residual checks, persona-guide links and a fast offline dev-only validator wired into product CI. Local verification passes ten schema tests, four focused Java tests and all 369 default Node tests; fifteen schema-pass/native-fail cases retain native parsing, semantic and authority boundaries. Hosted [product CI](https://github.com/rore/keir-core/actions/runs/41340118926) and [workflow CI](https://github.com/rore/keir-core/actions/runs/41340118947) pass on `d03c924`; the [Work Record](../../.agent-workflow/tasks/read2-structural-contracts.md) retains exact evidence and review. Schemas describe decoded values; success never grants native acceptance or host authority. Production semantics, older profiles and existing golden bytes are unchanged. Studio UX is independent.

**Remaining:** Other supported profiles, simulation contracts, capability-discovery inputs and stable repairable diagnostics/API references still need machine-readable coverage and matching agent guidance. This read/2 increment does not complete the feature.

**Design input:** [Structural schema boundaries](../../docs/internal/design-baseline/public/schemas/extension-ir-v0.schema.json); [Discovery, section 3.3](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Types and validation, sections 2 and 7](../../docs/internal/design-baseline/public/domain-model-v0.md).
