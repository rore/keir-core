---
id: "tenant-capability-overlays"
title: "Proposal: admit tenant catalogs and registered endpoints"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Evaluate separately approved tenant capabilities without turning customer source into endpoint or credential authority.

## In Scope

- Define effective-catalog precedence, conflicts, scope/digest/version identity, compatibility, discovery and revocation.
- Keep registration, credentials, destination/SSRF/redirect/DNS/egress controls, disclosure projections and quotas host-owned.
- Map endpoints through ordinary invokers with declared use/effects; HTTP method alone does not classify safety.

## Out of Scope

Arbitrary source URLs/SQL, automatic registration, built-in IAM or requiring tenant overlays for ordinary internal HTTP/RPC invokers.

## Done When

- [ ] Decision: select a concrete tenant integration and hosting/registration trust policy, or defer it.
- [ ] Before adoption: specify catalog identity/merge rules, endpoint/data security and native key/timeout contracts.
- [ ] If adopted: test isolation, conflicting IDs, revoked endpoints, destination changes and projected data disclosure in core contracts and a host.

## Notes

**Dependencies:** [machine-readable-contracts](../features/machine-readable-contracts.md); [extension-approval-lifecycle](../features/extension-approval-lifecycle.md).

**Design input:** [Effective catalogs and endpoints, sections 3-4 and 6](../../docs/internal/design-baseline/public/hooks-and-tenant-capabilities.md); [Endpoint/data boundaries, section 9](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Tenant overlay, section 4.2](../../docs/internal/design-baseline/public/domain-model-v0.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
