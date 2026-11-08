---
id: "public-package-release"
title: "Release installable Java and Node libraries with compatibility guidance"
status: "done"
priority: "medium"
commitment: "committed"
milestone: "Release an embeddable Keir library"
repo: "keir-core"
---

## Summary

Let an external developer install and embed a documented, explicitly supported Keir release without this workspace.

The first public milestone supports at most one mutation per run with the existing host-owned key, authorization and outcome contract. Multi-mutation composition follows later. The documentation track begins with a source-checkout binding and runtime guide for Java and Node while release qualification proceeds. Opening the repositories and publishing registry packages are separate release actions.

Core 0.2.0 is the current private release, with Keir 2 source, domain capability model, artifact and verification formats, plus authoring companion v4. Daymark consumes the published packages. Format compatibility is versioned independently of package versions.

## In Scope

- Use Apache 2.0. Set package coordinates, version and support policy, and publication authority.
- Publish Java compiler/runtime and Node runtime packages, checked quickstarts and a supported dependency/profile compatibility matrix.
- Ship a versioned authoring package with the matching agent guide, schemas and examples. Record compatible compiler and runtime releases, profiles and DCM formats so a host can pin the package alongside its exact published model.
- When a release changes what an agent can author, update the affected guidance, schemas and examples with the feature. Verify the packaged guide can teach an agent to use that behavior through the published model and host tools.
- Test clean isolated installs, artifact integrity, compiler-free execution, rejection of obsolete formats, and release provenance.

## Out of Scope

Supporting or migrating development formats, requiring AI or every optional proposal for the first release, or labeling an unqualified adapter production-safe.

## Done When

- [x] Design/spec: define the supported public format combination, producer and environment pins, and user-facing limits.
- [x] Implementation: clean Java and Node consumers compile once and run the same artifact through documented package interfaces.
- [x] Verification: reproducible install and package checks, rejection of obsolete formats, and shared Java and Node conformance pass. Daymark creates and runs a new customization from fresh isolated demo data using the published compiler, runtime, model and matching authoring companion. Repository visibility is a separate launch decision.

## Notes

The current [Core 0.2.0](https://github.com/rore/keir-core/releases/tag/v0.2.0) and [authoring companion v4](https://github.com/rore/keir-core/releases/tag/authoring-v4) passed package verification and download comparisons. Java and Node agreed on 28 shared scenarios and 51 validation vectors. Daymark's four native workflows passed the integrated build, Node and Chromium qualification using these published assets. The running demo was updated and its served browser asset verified. Release packaging and verification are scripted, and the [release checklist](../../docs/releasing.md) includes updating the running demo. Public launch still requires the privacy review and a repository visibility decision. [Core qualification](https://github.com/rore/keir-core/actions/runs/42258542838) · [Daymark qualification](https://github.com/rore/keir-demo-app/actions/runs/42261861370)

The entries below retain earlier release evidence. Current dependencies and format guidance are documented in the [Keir 2 version matrix](../../docs/specs/workflow-expressions.md#version-matrix).

**Private release delivered:** [Core 0.1.0](https://github.com/rore/keir-core/releases/tag/v0.1.0) and [authoring companion v3](https://github.com/rore/keir-core/releases/tag/authoring-v3) are published in the private repository. The Core prerelease uses the Keir 1 source, model, artifact and verification formats. All eight published files matched the verified downloads, and the companion verifier checked the three Core package digests. [Daymark](https://github.com/rore/keir-demo-app/commit/e496f82c4b3ef97b3eaf0e946e256002960e0e41) consumes the exact published runtime, compiler and guide. A clean install and both scripted-provider Studio journeys passed through example checks, separate approval and activation, and confirmed native task creation. The local preview uses a fresh database, with the previous data available for rollback. Public launch still requires the privacy review and a separate repository visibility decision.

**Earlier release-channel check:** [GitHub Releases](https://github.com/rore/keir-core/releases) distributed [authoring companion v2](https://github.com/rore/keir-core/releases/tag/authoring-v2) with its license and checksums. A download passed the verifier against the expected digest. That Daymark revision pinned the exact guide and supplied it to both supported authoring providers alongside its own model and tools. Isolated tests verified guide integrity, saved review continuity and the same-project follow-up against the matching compiler and runtime.

**Packaged runtime verification:** Keir Core uses Apache 2.0. The compiler package contains third-party notices. Isolated Java and Node consumers ran the same compiled rule and matched creation, no-op and uncertain outcomes. Both packaged runtimes matched the shared domain model boundary cases. The published assets match the qualified package bytes.

**Authoring companion available from source:** Build and verify the versioned guide, schemas and examples as a deterministic ZIP. The verifier checks a trusted archive digest before inspecting its contents. The example compiled to the pinned artifact and its three cases match Java and Node conformance. [Authoring companion guide](../../docs/agents/authoring-companion.md)

**Second companion edition verified:** Version 2 packages the updated authoring guide. The original default commands still reproduce and verify the first archive. The second archive passed repeat-build, pin, inventory, schema and compiler checks. Its examples match the shared Java and Node outcomes.

**Earlier offline consumer check:** Using staged development packages from `98d1901`, isolated Java and Node consumers ran the same artifact after one compilation. Their full outcomes and traces matched for creation and no-op cases. Both passed the original request key to the host implementations. Java consumed the thin runtime JAR with its matching parent POM, and Node installed the tarball with the pinned lock integrity. Both execution environments excluded the compiler.

**Dependencies:** [machine-readable-contracts](../features/machine-readable-contracts.md); [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [SDK and compatibility, sections 7-10](../../docs/internal/design-baseline/public/compiler-artifact-and-runners-v0.md); [Trusted artifact and boundaries, section 6](../../docs/internal/design-baseline/public/keir-architecture.md).

An experimental release can retain clear limits. Production claims additionally require the relevant resource and real-host evidence; milestone ordering is not a waterfall dependency.
