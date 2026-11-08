---
id: "pre-operation-guard"
title: "Compile and run an authoritative pre-operation decision"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-core"
---

## Summary

Add a real guard contract distinct from advisory REQUEST/DISPLAY: a typed decision that a host must consume before its own native operation.

## In Scope

- Specify allowed decisions, no-binding/error/timeout fallback, input provenance, deadline reserve and consistency claims.
- Keep native mutation ownership with the host; extension evaluation cannot independently write before commit.
- Implement the supported compiler, artifact, Java/Node and sample consumer paths through an explicit compatibility boundary.

## Out of Scope

Patch consumers, policy-chain composition, native persistence or guarantees Keir cannot enforce.

## Done When

- [ ] Design/spec: publish the decision consumer, lifecycle and failure-policy contract without silently renaming an existing profile.
- [ ] Implementation: compile once and run allow/deny decisions through both runtimes and packaged consumers.
- [ ] Verification: shared cases cover invalid results, denied reads, timeout, missing binding, effect prohibition and native-consumption receipts.

## Notes

The authorized slice starts with independent technical design of the typed decision consumer and shared Java/Node acceptance cases. Core owns document formats, compiler/runtime behavior, read-only enforcement and bounded outcomes. Host-specific task fields, persistence, permissions, rule activation and save UX belong to Daymark. Implementation follows review of the smallest supported contract and its public interfaces.

Initial independent review confirmed that existing typed records, enum expressions, binding/invocation APIs and static zero-write enforcement can be reused. The remaining design revisions are explicit producer/model compatibility, separating runtime shape checks from host key authentication, and permitting ordinary host-defined result fields alongside the decision for useful explanations. Semantic implementation follows closure of those findings. The architect owns the contract, Core developers own the compiler and both runtimes, and the documentation owner aligns the packaged agent guide with the verified release.

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Synchronous lifecycle and guard pattern, sections 3-6 and 13A](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Hooks and consumers, section 2](../../docs/internal/design-baseline/public/hooks-and-tenant-capabilities.md).
