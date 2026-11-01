---
id: "public-package-release"
title: "Release installable Java and Node libraries with compatibility guidance"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Release an embeddable Keir library"
repo: "keir-core"
---

## Summary

Let an external developer install and embed a documented, explicitly supported Keir release without this workspace.

## In Scope

- Choose license, coordinates/version policy, support scope and publication authority; keep experimental versus production claims explicit.
- Publish Java compiler/runtime and Node runtime packages, checked quickstarts and a supported dependency/profile compatibility matrix.
- Test clean isolated installs, artifact integrity, compiler-free execution, old-profile migration and release/security provenance.

## Out of Scope

Requiring AI or every optional proposal before an experimental release, silently granting a license, or labeling an unqualified adapter production-safe.

## Done When

- [ ] Design/spec: settle license/release authority, supported profiles, compatibility/deprecation policy and user-facing limits.
- [ ] Implementation: clean Java and Node consumers compile once and run the same artifact through documented package interfaces.
- [ ] Verification: reproducible install/package checks, upgrade conformance and examples pass; publishing uses a separately authorized release action.

## Notes

**Dependencies:** [machine-readable-contracts](../features/machine-readable-contracts.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [SDK and compatibility, sections 7-10](../../docs/internal/design-baseline/public/compiler-artifact-and-runners-v0.md); [Trusted artifact and boundaries, section 6](../../docs/internal/design-baseline/public/keir-architecture.md).

An experimental release can retain clear limits. Production claims additionally require the relevant resource and real-host evidence; milestone ordering is not a waterfall dependency.
