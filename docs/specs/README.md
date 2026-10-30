# Keir specifications

The first maintained contract is **`keir-read/1`, an implemented experimental checkpoint** for REQUEST/read/classify/display. The Java compiler and both runtimes pass the same local and CI conformance corpus and packaged-host verification. [Testing evidence](../testing.md) records supported scope and limitations; production qualification and release remain open.

The [language/DCM specification feature](../../roadmap/features/specify-language-and-domain-model.md) owns the initial supported contract. Specifications mature with feature implementation and conformance, without a whole-system design freeze.

| Contract | What it must define | Current input |
| --- | --- | --- |
| [Language and CEL profile](language.md) | Strict source, scalar CEL, nominal/enum overlay and diagnostics | [Language draft](../internal/design-baseline/public/keir-language-v0.md) |
| [Domain capability model](domain-model.md) | Exact small DCM schema and complete sample | [DCM draft](../internal/design-baseline/public/domain-model-v0.md) |
| [Compiler and artifact](compiler-artifact.md) | Deterministic descriptor/plan format, digest bytes and external trust anchor | [Compiler/artifact draft](../internal/design-baseline/public/compiler-artifact-and-runners-v0.md) |
| [Runtime](runtime.md) | Prepare/invoke, protected parameters, outcomes, redacted trace and provisional limits | [Runner draft](../internal/design-baseline/public/compiler-artifact-and-runners-v0.md) |
| [Host integration](host-integration.md) | Result consumption, two assistant boundaries and acceptance fixtures | [Hook contracts](../internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md), [simulation draft](../internal/design-baseline/public/simulation-and-authoring-v0.md) |

The profile intentionally excludes writes, `when`, `onError`, optional values, collections, numbers and time. `keir/v0` is the source family, not a promise to accept every baseline construct. The sample DCM/artifact have explicit profile-specific formats. Unsupported shapes fail closed; no automatic migration or hidden coercion. [Design rationale and handoff](../internal/read-and-decide-design.md) records pending qualification rather than asserting untested guarantees.

These contracts narrow baseline language/DCM/compiler/simulation drafts for this increment, override Java-later wording under decision 0001 and retain decision 0003's evidence boundary. Developers own executable fixtures under `conformance/`; the [acceptance matrix](host-integration.md#acceptance-fixtures) describes required tests, not passing results. The owning feature remains open.

## Maintaining a contract

The separately versioned [single-effect profile](single-effect.md) is **accepted for implementation, not yet supported** for [one guarded follow-up](../../roadmap/features/single-effect-followup.md). It defines guards, terminal business handlers, original native host-key forwarding, retained effect receipts and simulation-only admission. It does not widen the implemented read profile or establish real write guarantees. See the [design and acceptance vectors](../internal/single-effect-followup-design.md).

For each affected feature: review the draft, specify the supported behavior, add accepted/rejected conformance examples, implement it, and reconcile discoveries. A maintained contract identifies status, supported scope, unresolved decisions, compatibility consequences, verification evidence and superseded baseline sections.

Keep normative rules here, rationale in [internal decisions](../internal/README.md), and task-oriented explanations in [agent guides](../agents/README.md). Schemas, catalog descriptions and examples must agree with the written rules. Structural validation does not prove semantic safety; shared [conformance fixtures](../../conformance/README.md) supply executable evidence.

Both agent personas use these contracts. Host assistants propose integration definitions for host review; extension assistants use only the host-approved catalog within customer scope. Draft examples, planned tool names and successful simulations do not grant authority or certify adapters.
