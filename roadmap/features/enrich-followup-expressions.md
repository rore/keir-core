---
id: "enrich-followup-expressions"
title: "Add bounded collection, absence and elapsed-time expressions"
status: "done"
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

- [x] Publish the selected type, value, absence and time rules, diagnostics, bounded CEL operations, and accepted and rejected examples.
- [x] Verify the compiler and both runtimes agree on artifact bytes and outcomes for neutral examples that use bounded membership, optional values, a Boolean host read and 168-hour arithmetic.
- [x] Cover list bounds, absence, timestamp overflow, adapter failures, worst-case membership work, unsupported expressions, and existing scalar behavior.
- [x] Ship matching specifications, schemas, examples and agent instructions with the verified compiler and runtimes.

## Notes

The packaged CLI reproduced the three frozen Inventory artifacts byte for byte. Java and Node agreed on 28 scenarios and 51 validation vectors. Resource qualification exercised cold bind and prepare, confirmed writes, maximum-list no-match invocations, and pending reads. In both runtimes an early read timeout prevented a write and a later read timeout preserved an earlier confirmed receipt. The measurements used serial invocations and simulated hosts. Hosted checks passed, and the downloaded release assets matched the locally qualified bytes. [Core 0.2.0](https://github.com/rore/keir-core/releases/tag/v0.2.0) · [Authoring companion v4](https://github.com/rore/keir-core/releases/tag/authoring-v4) · [Hosted contract checks](https://github.com/rore/keir-core/actions/runs/42258542838) · [Workflow expression specification](../../docs/specs/workflow-expressions.md) · [Shared conformance corpus](../../conformance/keir-2/README.md)

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md); [bound-cel-execution](../features/bound-cel-execution.md).

**Design input:** [Language/dataflow/CEL, sections 3-5 and 10-12](../../docs/internal/design-baseline/public/keir-language-v0.md); [Selected types and query contracts, sections 2-3](../../docs/internal/design-baseline/public/domain-model-v0.md); [Baseline invoice scenario, section 4A](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing runnable enhancement outcome. The native richer-query/due-date integration is rich-task-customizations. Selected type/schema/compiler/runtime/resource work is explicit here; broader type families remain a separate proposal. This card does not block native CRUD, real reads or the first scalar effect. Production containment claims still require runtime-resource-qualification.
