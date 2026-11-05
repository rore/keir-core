---
id: "public-package-release"
title: "Release installable Java and Node libraries with compatibility guidance"
status: "active"
priority: "medium"
commitment: "committed"
milestone: "Release an embeddable Keir library"
repo: "keir-core"
---

## Summary

Let an external developer install and embed a documented, explicitly supported Keir release without this workspace.

The first public milestone supports at most one mutation per run with the existing host-owned key, authorization and outcome contract. Multi-mutation composition follows later. The documentation track begins with a source-checkout binding and runtime guide for Java and Node while release qualification proceeds. Opening the repositories and publishing registry packages are separate release actions.

## In Scope

- Use Apache 2.0. Set package coordinates, version and support policy, and publication authority.
- Publish Java compiler/runtime and Node runtime packages, checked quickstarts and a supported dependency/profile compatibility matrix.
- Ship a versioned authoring package with the matching agent guide, schemas and examples. Record compatible compiler and runtime releases, profiles and DCM formats so a host can pin the package alongside its exact published model.
- When a release changes what an agent can author, update the affected guidance, schemas and examples with the feature. Verify the packaged guide can teach an agent to use that behavior through the published model and host tools.
- Test clean isolated installs, artifact integrity, compiler-free execution, old-profile migration and release/security provenance.

## Out of Scope

Requiring AI or every optional proposal before an experimental release, silently granting a license, or labeling an unqualified adapter production-safe.

## Done When

- [ ] Design/spec: settle release authority, supported contracts, compatibility and deprecation policy, and user-facing limits.
- [ ] Implementation: clean Java and Node consumers compile once and run the same artifact through documented package interfaces.
- [ ] Verification: reproducible install/package checks, upgrade conformance and examples pass. A reference host consumes the compatible pinned authoring package. Repository visibility is a separate launch decision.

## Notes

**Release channel in use:** [GitHub Releases](https://github.com/rore/keir-core/releases) is the distribution channel. The [release checklist](../../docs/releasing.md) defines when to ship and how to verify each package. [Authoring companion v2](https://github.com/rore/keir-core/releases/tag/authoring-v2) is available with its license and checksums. A download from the release passed the verifier against the expected digest. Daymark integration remains open.

**Core release candidate:** Keir Core uses Apache 2.0. The Core draft contains versioned compiler, Node and Java packages, including third-party notices for the compiler bundle. Isolated consumers installed the versioned candidates, ran one compiled rule and matched outcomes and traces. The package version was set in the staging checkout without changing the serialized compiler identity. Before publishing Core, finalize the reproducible build and compatibility policy, and qualify the exact release assets. Public readiness also requires clearance of reachable history and GitHub surfaces.

**Authoring companion available from source:** Build and verify the versioned guide, schemas and examples as a deterministic ZIP. The verifier checks a trusted archive digest before inspecting its contents. The example compiled to the pinned artifact and its three cases match Java and Node conformance. The first reference-host integration is still open. [Authoring companion guide](../../docs/agents/authoring-companion.md)

**Second companion edition verified:** Version 2 packages the updated authoring guide. The original default commands still reproduce and verify the first archive. The second archive passed repeat-build, pin, inventory, schema and compiler checks. Its examples match the shared Java and Node outcomes. Daymark will pin the updated guide as the first reference host.

**Offline consumer check passed:** Using staged development packages from `98d1901`, isolated Java and Node consumers ran the same `effect/4` artifact after one compilation. Their full outcomes and traces matched for creation and no-op cases. Both passed the original request key to the host implementations. Java consumed the thin runtime JAR with its matching parent POM, and Node installed the tarball with the pinned lock integrity. Both execution environments excluded the compiler. Repeat against the final release packages after release identities and compatibility policy are set.

**Dependencies:** [machine-readable-contracts](../features/machine-readable-contracts.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [SDK and compatibility, sections 7-10](../../docs/internal/design-baseline/public/compiler-artifact-and-runners-v0.md); [Trusted artifact and boundaries, section 6](../../docs/internal/design-baseline/public/keir-architecture.md).

An experimental release can retain clear limits. Production claims additionally require the relevant resource and real-host evidence; milestone ordering is not a waterfall dependency.
