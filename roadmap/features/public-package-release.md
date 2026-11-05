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

Core 0.1.0 establishes the first supported public formats for Keir source, the domain capability model, compiled artifacts, and verification scenarios. Define their compatibility independently of package versions. Use these formats throughout Core and Daymark before releasing Core 0.1.0.

## In Scope

- Use Apache 2.0. Set package coordinates, version and support policy, and publication authority.
- Publish Java compiler/runtime and Node runtime packages, checked quickstarts and a supported dependency/profile compatibility matrix.
- Ship a versioned authoring package with the matching agent guide, schemas and examples. Record compatible compiler and runtime releases, profiles and DCM formats so a host can pin the package alongside its exact published model.
- When a release changes what an agent can author, update the affected guidance, schemas and examples with the feature. Verify the packaged guide can teach an agent to use that behavior through the published model and host tools.
- Test clean isolated installs, artifact integrity, compiler-free execution, rejection of obsolete formats, and release provenance.

## Out of Scope

Supporting or migrating development formats, requiring AI or every optional proposal for the first release, or labeling an unqualified adapter production-safe.

## Done When

- [ ] Design/spec: define the supported public format combination, producer and environment pins, and user-facing limits.
- [ ] Implementation: clean Java and Node consumers compile once and run the same artifact through documented package interfaces.
- [ ] Verification: reproducible install and package checks, rejection of obsolete formats, and shared Java and Node conformance pass. Daymark creates and runs a new customization from fresh isolated demo data using the published compiler, runtime, model and matching authoring companion. Repository visibility is a separate launch decision.

## Notes

**Private release delivered:** [Core 0.1.0](https://github.com/rore/keir-core/releases/tag/v0.1.0) and [authoring companion v3](https://github.com/rore/keir-core/releases/tag/authoring-v3) are published in the private repository. The Core prerelease uses the Keir 1 source, model, artifact and verification formats. All eight published files matched the verified downloads, and the companion verifier checked the three Core package digests. [Daymark](https://github.com/rore/keir-demo-app/commit/e496f82c4b3ef97b3eaf0e946e256002960e0e41) consumes the exact published runtime, compiler and guide. A clean install and both scripted-provider Studio journeys passed through example checks, separate approval and activation, and confirmed native task creation. The local preview uses a fresh database, with the previous data available for rollback. Public launch still requires the privacy review and a separate repository visibility decision.

**Release channel in use:** [GitHub Releases](https://github.com/rore/keir-core/releases) is the distribution channel. The [release checklist](../../docs/releasing.md) defines when to ship and how to verify each package. [Authoring companion v2](https://github.com/rore/keir-core/releases/tag/authoring-v2) is available with its license and checksums. A download from the release passed the verifier against the expected digest. Daymark pins the exact guide from this release and supplies it to both supported authoring providers alongside its own model and tools. Isolated tests verified guide integrity, saved review continuity and the same-project follow-up against the pinned compiler and runtime.

**Packaged runtime verification:** Keir Core uses Apache 2.0. The compiler package contains third-party notices. Isolated Java and Node consumers ran the same compiled rule and matched creation, no-op and uncertain outcomes. Both packaged runtimes matched the shared domain model boundary cases. The published assets match the qualified package bytes.

**Authoring companion available from source:** Build and verify the versioned guide, schemas and examples as a deterministic ZIP. The verifier checks a trusted archive digest before inspecting its contents. The example compiled to the pinned artifact and its three cases match Java and Node conformance. [Authoring companion guide](../../docs/agents/authoring-companion.md)

**Second companion edition verified:** Version 2 packages the updated authoring guide. The original default commands still reproduce and verify the first archive. The second archive passed repeat-build, pin, inventory, schema and compiler checks. Its examples match the shared Java and Node outcomes.

**Earlier offline consumer check:** Using staged development packages from `98d1901`, isolated Java and Node consumers ran the same artifact after one compilation. Their full outcomes and traces matched for creation and no-op cases. Both passed the original request key to the host implementations. Java consumed the thin runtime JAR with its matching parent POM, and Node installed the tarball with the pinned lock integrity. Both execution environments excluded the compiler.

**Dependencies:** [machine-readable-contracts](../features/machine-readable-contracts.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [SDK and compatibility, sections 7-10](../../docs/internal/design-baseline/public/compiler-artifact-and-runners-v0.md); [Trusted artifact and boundaries, section 6](../../docs/internal/design-baseline/public/keir-architecture.md).

An experimental release can retain clear limits. Production claims additionally require the relevant resource and real-host evidence; milestone ordering is not a waterfall dependency.
