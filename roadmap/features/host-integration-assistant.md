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

Package the host-design persona as a skill for the visitor's own coding agent: inspect the native app, propose DCM/hooks/capabilities/adapters, validate them and submit an exact revision to the host owner. The demo does not need a second in-app host-design assistant.

## In Scope

- Offer authorized interface/catalog inspection, typed drafting/validation tools and host conformance evidence.
- Separate proposed definitions from publication; customer credentials and the assistant cannot certify or publish their own proposal.
- Package concise coding-agent instructions, discovery/validation commands and examples against current schemas/docs. Host design-time uses the visitor's coding agent and its own model/authentication; the demo's extension-authoring provider configuration is separate.
- Provide a future `pre-design-time` branch/checkpoint with the same working native app before Keir host configuration. A visitor's coding agent uses the host-design guidance to define the DCM, hooks, invokers and conformance evidence; the host owner reviews the result.
- Keep `main` as the eventually configured customer-customization demo. Back the named starting branch with an immutable commit/tag, pinned setup/core compatibility and a deliberate checkpoint refresh procedure; do not continually merge completed integration into it or create it before the native baseline exists.

## Out of Scope

An agent runtime in core, automatic certification, mandatory API importers or a separate in-app host-design chat product.

## Done When

- [ ] Design/spec: define the skill's inputs, supported commands/tools, scope/effects, host-owner publication boundary and a small supported integration-change scenario.
- [ ] Implementation: a developer loads the skill in their coding agent and produces a validated, reviewable host integration and a separately approved published revision.
- [ ] Verification: missing evidence, forbidden publication and customer credentials fail safely; approved definitions become discoverable by customer authoring. The skill reuses the coding agent's credentials and requires no embedded demo host-design provider.
- [ ] Walkthrough: document the pre-design-time checkout, native startup, coding-agent task and expected integration checks; verify it from a clean checkout using a compatible pinned core release. Main retains the configured customer journey.

## Notes

Skill/checkpoint distribution and a newly approved host-integration walkthrough remain unassigned and queued. The real-task customization increment is delivered, not an active dependency hold. The [current coding-assistant guide](../../docs/agents/host-integration-assistant.md) covers the maintained binding, authority and pre-operation contracts; its original catalog-bound runnable walkthrough is explicitly archived. Existing guide evidence remains valid for its recorded scope without claiming the unperformed visitor skill/checkpoint journey.

**Dependencies:** [real-task-reads](../features/real-task-reads.md); [machine-readable-contracts](../features/machine-readable-contracts.md).

**Design input:** [Host discovery and ownership, sections 1-3](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Agent integration, section 5](../../docs/internal/design-baseline/public/hooks-and-tenant-capabilities.md).

Preserves the existing feature and its acceptance obligations. Reuse the simulation/conformance seam as useful; a scripted transcript is preparation, not proof of a live assistant.

**Runnable guide increment delivered:** The [host-design walkthrough](../../docs/agents/host-integration-assistant.md) now gives one current read/2 path from generic hook/catalog and protected invokers through customer source, pinned compilation, independent scenarios and host-owner handoff. The documented isolated setup and verifier pass: 19 matching packaged Java/Node reports and two exact compiled artifacts. The guide distinguishes expected business error codes from redacted report codes, real-adapter obligations and the separately optional Python schema check. This is a verified guide increment; visitor skill/checkpoint distribution, an actual newly approved host integration and publication remain unfinished.
