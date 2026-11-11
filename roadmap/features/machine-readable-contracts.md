---
id: "machine-readable-contracts"
title: "Publish current schemas and agent-consumable contracts"
status: "done"
priority: "medium"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-core"
---

## Summary

The supported language, DCM, artifact, scenario and diagnostics are available to authoring tools through current structural schemas, typed compiler results, maintained specifications and versioned agent guidance. The [current format/producer matrix](../../docs/specs/compiler-artifact.md#source-and-artifact-identity) identifies the exact supported tuples; historical profile names below do not define additional current formats.

Daymark consumes the pinned shared companion guide alongside its host-owned model, scoped tools and policy. Structural validation remains separate from semantic checks and native authority; a valid schema document or an agent guide grants neither approval nor execution permission.

## In Scope

- Publish profile-specific schemas, valid/invalid examples, capability-discovery inputs and stable diagnostics/API references.
- Reconcile current parsers, semantic checks and schema limits; structural schema validation never substitutes for authority or semantic validation.
- Publish a compact, versioned customer-authoring guide that a host can supply to its agent. Teach discovery, typed composition from the DCM, compiler diagnostics, independent examples, and review. Keep host-specific vocabulary, tool implementations and policy in host context. Maintain separate host-design guidance.
- Version and release the guide, schemas and examples together under the [authoring package release contract](public-package-release.md). Keep host model identity separate so hosts can evolve their own catalog within a supported profile.

## Out of Scope

A second language specification in prompts, an agent framework in core or an automatic schema importer.

## Done When

- [x] Design/spec: the current source/model/artifact/scenario matrix links matching structural schemas and normative semantic contracts, while host-owned fields and authority remain explicit.
- [x] Implementation: versioned schemas, examples, compiler diagnostics and host/customer assistant guides use the supported contracts; Daymark supplies its own model and scoped tool context separately.
- [x] Verification: packaged examples, native rejection/conformance checks, companion inventory and trusted-pin verification, and actual Daymark guide consumption are recorded. Repair diagnostics remain bounded and scoped; schema success never replaces native authority checks.

## Notes

**Current contracts delivered:** Source `/2`, DCM `/3`, artifact `/3` and scenario `/3` schemas are linked from [compiler/artifact contracts](../../docs/specs/compiler-artifact.md). That reference also defines typed compilation results and diagnostic code/path/message fields. [Host integration](../../docs/agents/host-integration-assistant.md) and [customer assistant integration](../../docs/agents/extension-assistant.md) use the same maintained contracts. The [Core 0.3.1 record](../../docs/releases/core-0.3.1-candidate.md) retains installed Java/Node checks, compiler-produced examples, companion verification, consumer regressions and published-download comparisons.

**Companion boundary:** [Edition 7](../../docs/agents/authoring-companion.md#licensed-candidate-edition-7) packages the guide, schemas, examples and full license; its archive is available on the Core release and Daymark consumes its exact pinned guide. Its immutable compatibility row still says `candidate` with an empty package map. This card does not relabel that record or claim a new released-package compatibility row. Availability, native semantic qualification and host approval are distinct facts.

### Earlier schema evidence

**Historical single-effect schemas delivered:** [Source, DCM and artifact schemas](../../schemas/effect-4/README.md) describe decoded structure. The compiler and runtimes retain semantic and authority checks. The [Work Record](../../.agent-workflow/tasks/effect4-structural-contracts.md) records that compiler-produced fixture and native residual checks.

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**First increment delivered:** The independently reviewed [read/2 structural contracts](../../schemas/read-2/README.md) are on main: three schemas, examples, native residual checks, persona-guide links and a fast offline dev-only validator wired into product CI. Local verification passes ten schema tests, four focused Java tests and all 369 default Node tests; fifteen schema-pass/native-fail cases retain native parsing, semantic and authority boundaries. Hosted [product CI](https://github.com/rore/keir-core/actions/runs/41340118926) and [workflow CI](https://github.com/rore/keir-core/actions/runs/41340118947) pass on `d03c924`; the [Work Record](../../.agent-workflow/tasks/read2-structural-contracts.md) retains exact evidence and review. Schemas describe decoded values; success never grants native acceptance or host authority. Production semantics, older profiles and existing golden bytes are unchanged. Studio UX is independent.

**Scope boundary:** The earlier read-only increment did not alone complete this card; the current schemas, packaged guidance and actual host consumption above supply the later delivery. Broader reusable adapter/consumer tooling and native host-conformance kits remain under [reusable simulation](reusable-simulation.md), not missing Daymark authoring definitions. Host-specific discovery/tool APIs remain the host's responsibility rather than a second generic agent framework in Core.

**Design input:** [Structural schema boundaries](../../docs/internal/design-baseline/public/schemas/extension-ir-v0.schema.json); [Discovery, section 3.3](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Types and validation, sections 2 and 7](../../docs/internal/design-baseline/public/domain-model-v0.md).
