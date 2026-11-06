---
id: "enrich-followup-expressions"
title: "Add bounded collection, absence and elapsed-time expressions"
status: "active"
priority: "medium"
commitment: "committed"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Extend the portable language and domain contract with the bounded collection, absence and elapsed-time operations needed for composable workflows. The contract and shared conformance cases use domain-neutral semantics. Each host declares its own event hooks, data and capabilities.

The first increment delivered bounded string concatenation in the compiler and both runtimes. It supports appending a fixed suffix to a title. The shared contract suite passed, and Daymark consumed the verified packages for its [title rule](rich-task-customizations.md). The invoice example remains queued. [Implementation record](../../.agent-workflow/tasks/bounded-followup-title-concatenation.md).

## In Scope

- Qualify bounded scalar-list membership, optional scalar fields and elapsed timestamp arithmetic with the pinned Java and Node engines.
- Let hosts express existence checks as Boolean reads. A host defines the query and its completeness. The language does not need to inspect a collection of query results for this increment.
- Keep the single-write limit and specify validation, execution limits, failures and version alignment across the language, model, compiler, Java and Node runtimes and release packages.

- Expose the work across language/DCM schemas, parser/type overlay, checked CEL/descriptor and wire-value encodings, compiler and both runtime validators/evaluators.
- Measure worst-case no-match scans, string/intermediate allocations, aggregate costs, precision/range/overflow and incomplete query results before admitting constructs.

## Out of Scope

List size, record-list queries, comprehensions, general sorting, arbitrary maps, calendar scheduling, multi-effects and query-based atomic uniqueness.

## Done When

- [ ] Design/spec: publish exact selected type/value/absence/time rules, diagnostics, bounded CEL operations and accepted/rejected examples; no whole-language freeze.
- [ ] Implementation: compiler and both runtimes agree on identical artifact bytes and outcomes for neutral examples that use bounded membership, optional values, a Boolean host read and 168-hour arithmetic.
- [ ] Verification: shared tests cover list bounds, absence, timestamp overflow, adapter failures, worst-case membership work and rejection of unsupported expressions. Keep the existing supported scalar behavior passing.
- [ ] Documentation and release: ship matching specifications, schemas, examples and agent instructions with the verified compiler and runtimes.

## Notes

The Keir 2 candidate compiles the three frozen Inventory artifacts byte for byte from the packaged CLI. The packaged Java and Node runtimes agree on 28 scenarios and 51 validation vectors. The authoring companion archive builds deterministically and verifies against the candidate package digests. Whole-artifact resource qualification, hosted verification, and private release publication remain open. [Workflow expression specification](../../docs/specs/workflow-expressions.md) · [Shared conformance corpus](../../conformance/keir-2/README.md).

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md); [bound-cel-execution](../features/bound-cel-execution.md).

**Design input:** [Language/dataflow/CEL, sections 3-5 and 10-12](../../docs/internal/design-baseline/public/keir-language-v0.md); [Selected types and query contracts, sections 2-3](../../docs/internal/design-baseline/public/domain-model-v0.md); [Baseline invoice scenario, section 4A](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing runnable enhancement outcome. The native richer-query/due-date integration is rich-task-customizations. Selected type/schema/compiler/runtime/resource work is explicit here; broader type families remain a separate proposal. This card does not block native CRUD, real reads or the first scalar effect. Production containment claims still require runtime-resource-qualification.
