---
id: bound-cel-execution
title: Measure a restricted CEL profile in both runtimes
status: queued
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Use real pinned CEL libraries to establish an admitted expression subset and resource policy in Java and Node.

## Why

CEL-Java's comprehension guard requires explicit configuration; the inspected CEL-ES fold lacks a comparable guard. Finite list and AST sizes alone do not prove a hard deadline.

## In Scope

Recover the interoperability harness as reviewable source. Run admitted and adversarial expressions in both runtimes, including actual CEL in a prewarmed Node worker. Measure parity, latency, memory and termination. Check Java guard and deadline behavior.

## Out of Scope

Treating 64 items or 120 nodes as certified limits, or claiming containment from the synthetic worker experiment.

## Done When

- The first allowed AST/function/overload profile and artifact identity are documented.
- Cross-language conformance and boundary/error cases run with pinned libraries.
- Runtime evidence supports the declared POC bounds; unsupported shapes fail closed.
- Hard production deadlines remain unclaimed until proven.

## Notes

See the [restricted CEL decision](../../docs/internal/decisions/0003-restricted-cel-resource-policy.md).

Scope evidence incrementally around the runnable examples: field access/equality/Boolean logic first, the scalar guarded-effect predicate next, and collections/time only for [their explicit enhancement](enrich-followup-expressions.md). Recover the prior harness when useful, but unavailable archive packaging must not block writing equivalent focused tests. Unsupported nodes/overloads are rejected; a smaller supported profile does not excuse unverified bounds for expressions actually admitted. This work proceeds with the compiler/runtimes rather than as a complete standalone research phase before any program runs.
