---
id: "host-integration-assistant"
title: "Help a host developer define and approve an integration"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Demonstrate the host-design persona proposing a real domain/hook/capability change, validating it and submitting an exact revision to the host owner.

## In Scope

- Offer authorized interface/catalog inspection, typed drafting/validation tools and host conformance evidence.
- Separate proposed definitions from publication; customer credentials and the assistant cannot certify or publish their own proposal.
- Choose provider, credentials and spend bounds before live calls; keep the integration host-owned and guided by current schemas/docs.

## Out of Scope

An agent in core, automatic certification, mandatory API importers or a separate full chat product.

## Done When

- [ ] Design/spec: define tool schemas, scope/effects, the host-owner publication boundary and a small supported integration-change scenario.
- [ ] Implementation: a live developer request produces a validated, reviewable proposal and a separately approved published revision.
- [ ] Verification: missing evidence, forbidden publication, customer credentials and budget exhaustion fail safely; approved definitions become discoverable by customer authoring.

## Notes

**Dependencies:** [real-task-reads](../features/real-task-reads.md); [machine-readable-contracts](../features/machine-readable-contracts.md).

**Design input:** [Host discovery and ownership, sections 1-3](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Agent integration, section 5](../../docs/internal/design-baseline/public/hooks-and-tenant-capabilities.md).

Preserves the existing queued feature and its acceptance obligations. Reuse the simulation/conformance seam as useful; a scripted transcript is preparation, not proof of a live assistant.
