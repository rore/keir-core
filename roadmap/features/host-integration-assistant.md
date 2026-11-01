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
- Provide a future `pre-design-time` branch/checkpoint with the same working native app before Keir host configuration. A visitor's coding agent uses the host-design guidance to define the DCM, hooks, invokers and conformance evidence; the host owner reviews the result.
- Keep `main` as the eventually configured customer-customization demo. Back the named starting branch with an immutable commit/tag, pinned setup/core compatibility and a deliberate checkpoint refresh procedure; do not continually merge completed integration into it or create it before the native baseline exists.

## Out of Scope

An agent in core, automatic certification, mandatory API importers or a separate full chat product.

## Done When

- [ ] Design/spec: define tool schemas, scope/effects, the host-owner publication boundary and a small supported integration-change scenario.
- [ ] Implementation: a live developer request produces a validated, reviewable proposal and a separately approved published revision.
- [ ] Verification: missing evidence, forbidden publication, customer credentials and budget exhaustion fail safely; approved definitions become discoverable by customer authoring.
- [ ] Walkthrough: document the pre-design-time checkout, native startup, coding-agent task and expected integration checks; verify it from a clean checkout using a compatible pinned core release. Main retains the configured customer journey.

## Notes

**Dependencies:** [real-task-reads](../features/real-task-reads.md); [machine-readable-contracts](../features/machine-readable-contracts.md).

**Design input:** [Host discovery and ownership, sections 1-3](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Agent integration, section 5](../../docs/internal/design-baseline/public/hooks-and-tenant-capabilities.md).

Preserves the existing queued feature and its acceptance obligations. Reuse the simulation/conformance seam as useful; a scripted transcript is preparation, not proof of a live assistant.
