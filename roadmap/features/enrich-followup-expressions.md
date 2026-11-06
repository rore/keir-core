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

- For the workflow automation increment, qualify bounded membership, optional values and timestamp arithmetic first. Determine separately whether its host queries require collection-valued results or can use a bounded predicate.
- Keep the single-write limit and specify validation, execution limits, failures and version alignment across the language, model, compiler, Java and Node runtimes and release packages.

- Mature only the required bounded list, presence/optional, size, one non-nested exists, timestamp and elapsed-duration semantics.
- Expose the work across language/DCM schemas, parser/type overlay, checked CEL/descriptor and wire-value encodings, compiler and both runtime validators/evaluators.
- Measure worst-case no-match scans, string/intermediate allocations, aggregate costs, precision/range/overflow and incomplete query results before admitting constructs.

## Out of Scope

General sorting, nested comprehensions, arbitrary maps, calendar/timezone scheduling, multi-effects and search as atomic duplicate prevention.

## Done When

- [ ] Design/spec: publish exact selected type/value/absence/time rules, diagnostics, bounded CEL operations and accepted/rejected examples; no whole-language freeze.
- [ ] Implementation: compiler and both runtimes execute identical get -> search -> guarded-create artifact bytes with the source-owned condition and 168-hour arithmetic.
- [ ] Verification: tags, existing matches, absent values, oversized/incomplete lists, time overflow and adapter failures have shared outcomes; adversarial CEL costs are measured and unsupported forms rejected.
- [ ] Documentation: schemas and both persona examples match support; all simpler scalar and legacy regressions remain passing.

## Notes

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md); [bound-cel-execution](../features/bound-cel-execution.md).

**Design input:** [Language/dataflow/CEL, sections 3-5 and 10-12](../../docs/internal/design-baseline/public/keir-language-v0.md); [Selected types and query contracts, sections 2-3](../../docs/internal/design-baseline/public/domain-model-v0.md); [Baseline invoice scenario, section 4A](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing runnable enhancement outcome. The native richer-query/due-date integration is rich-task-customizations. Selected type/schema/compiler/runtime/resource work is explicit here; broader type families remain a separate proposal. This card does not block native CRUD, real reads or the first scalar effect. Production containment claims still require runtime-resource-qualification.
