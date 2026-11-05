---
id: "enrich-followup-expressions"
title: "Add bounded tags, search and due dates to the follow-up example"
status: "active"
priority: "medium"
commitment: "committed"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Extend the existing scalar program to the selected invoice example: get/search, tag membership, conditional create and a due date 168 elapsed hours later.

The active increment lets a task rule append a fixed suffix to a title using bounded string concatenation. Its compiler and runtime work can proceed alongside Daymark's versioned title read. The invoice example remains queued. [Implementation record](../../.agent-workflow/tasks/bounded-followup-title-concatenation.md).

## In Scope

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
