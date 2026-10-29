# Keir specifications

This is the public home for maintained Keir contracts. **No released, implementation-verified specification exists yet.** Supplied documents remain inputs in the internal baseline; moving files or adding this index does not resolve their design questions.

The [language/DCM specification feature](../../roadmap/features/specify-language-and-domain-model.md) owns the initial supported contract. Specifications mature with feature implementation and conformance, without a whole-system design freeze.

| Contract | What it must define | Current input |
| --- | --- | --- |
| Language and CEL profile | YAML/JSON syntax, types, scope/dataflow, evaluation order, allowed expressions, errors and rejection rules | [Language draft](../internal/design-baseline/public/keir-language-v0.md) |
| Domain capability model (DCM) | Domain types, hooks, capabilities, protected bindings, grants, effects, guarantees and compatibility | [DCM draft](../internal/design-baseline/public/domain-model-v0.md) |
| Compiler and artifact | Validation, structured diagnostics, portable artifact contents and admission | [Compiler/artifact draft](../internal/design-baseline/public/compiler-artifact-and-runners-v0.md) |
| Runtime | Java/Node observable semantics, dispatch, resource limits, typed outcomes and uncertainty | [Runner draft](../internal/design-baseline/public/compiler-artifact-and-runners-v0.md), [multi-effect proposal](../internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md) |
| Host integration and authoring | Invocation, result consumption, discovery, approval/activation, simulation scenarios and traces | [Hook contracts](../internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md), [simulation draft](../internal/design-baseline/public/simulation-and-authoring-v0.md) |

## Maintaining a contract

For each affected feature: review the draft, specify the supported behavior, add accepted/rejected conformance examples, implement it, and reconcile discoveries. A maintained contract identifies status, supported scope, unresolved decisions, compatibility consequences, verification evidence and superseded baseline sections.

Keep normative rules here, rationale in [internal decisions](../internal/README.md), and task-oriented explanations in [agent guides](../agents/README.md). Schemas, catalog descriptions and examples must agree with the written rules. Structural validation does not prove semantic safety; shared [conformance fixtures](../../conformance/README.md) supply executable evidence.

Both agent personas use these contracts. Host assistants propose integration definitions for host review; extension assistants use only the host-approved catalog within customer scope. Draft examples, planned tool names and successful simulations do not grant authority or certify adapters.
