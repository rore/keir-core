---
id: "api-schema-import"
title: "Proposal: import API schemas into reviewable catalog candidates"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Evaluate a thin host-design aid only if manual catalog authoring exposes a concrete need.

## In Scope

- Map selected OpenAPI/AsyncAPI types and operations into candidate DCM definitions with source provenance.
- Retain explicit human/native review of permissions, effects, completeness, transactions and idempotency.
- Reuse core validators; importing a schema cannot publish or certify a capability.

## Out of Scope

An importer prerequisite for either assistant, automatic registration/certification or generated native business code.

## Done When

- [ ] Decision: establish a useful bounded importer scenario and supported schema subset, or defer it.
- [ ] Before adoption: define provenance, unsupported-input diagnostics and the host-owner review boundary.
- [ ] If adopted: a developer validates a candidate and explicitly approves publication; untrusted imports cannot grant runtime authority.

## Notes

**Dependencies:** [host-integration-assistant](../features/host-integration-assistant.md).

**Design input:** [Manual catalogs before importing, section 3](../../docs/internal/design-baseline/public/task-management-poc.md); [Host metadata and agent tools, sections 3.3 and 9](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
