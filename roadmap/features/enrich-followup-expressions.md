---
id: enrich-followup-expressions
title: Add bounded tags, search and due dates to the follow-up example
status: queued
priority: medium
commitment: committed
milestone: Customize a real task application
repo: keir-core
---

## Summary

Extend the working scalar follow-up to the baseline invoice scenario: bounded get/search results, an invoice-sent tag condition, and a follow-up due 168 elapsed hours later. Keep the same program portable across Java and Node.

## Why

New language capabilities should extend an already runnable example. Collections and time add real semantic/resource work and are not prerequisites for seeing the first guarded effect.

## In Scope

Specify and measure bounded lists, required optional encodings, size, one non-nested exists, timestamps and literal elapsed durations. Admit only the constructs used by this scenario, including precision/range/overflow and enum/presence rules. Preserve actual call/data dependencies and immutable source-controlled conditions; do not replace composition with a task-specific policy adapter.

## Out of Scope

General sorting, nested comprehensions, unrestricted maps, all/filter/map unless another feature needs them, timezone calendars, multi-effects or treating search as atomic duplicate prevention.

## Done When

- The maintained get -> search -> conditional create example runs identical compiled bytes in both runtimes, with the original call order visible.
- Qualifying tag, no-match scan, existing follow-up, absent/oversized/incomplete result, time overflow and adapter failure cases have shared expected outcomes.
- Actual CEL evidence supports the newly admitted collection/time bounds, including worst-case no-match scans and aggregate costs. Unsupported shapes fail closed.
- Same-artifact simulation, structured diagnostics, public specs and both agent examples cover the additions; the simpler read-only and scalar-effect corpora still pass.
- The host's real uniqueness/idempotency guarantee remains a separately tested adapter obligation.

## Notes

Depends on [the scalar effect path](single-effect-followup.md). Coordinate with [real task integration](demo-task-integration.md), but do not delay a real scalar host integration merely to complete this enhancement. This carries the original baseline example forward without making it the minimum first-write implementation.
