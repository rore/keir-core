---
id: bound-cel-execution
title: Measure a restricted CEL profile in both runtimes
status: active
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

### Current assignment

Accepted by `@keir-dev2` on 2026-10-29 for the first scalar scenario only; exact Minimap participation attachment succeeded. Isolated checkout: `C:/Users/I347041/.codex/worktrees/keir-cel-node/keir-core`, starting at `4eadecb`. The bounded qualification checkpoint is complete and accepted: source commit `685800f` integrated as `790ff35`; the architect's final evidence correction is `a19e9ad`. See [the reproducible probe and limits](../../conformance/cel-profile/README.md).

Evidence: 12 Java-compiled expressions, 24 Java/Node primitive comparisons, 16 rejected expressions and 17 invalid inputs rejected in each projection gate; exact AST 64/65, depth 16/17, source 512/513 UTF-8 bytes and string 256/oversize boundaries. The manager reviewed the source and independently replayed the committed Java fixture in the canonical checkout: 3,285 assertions passed and SHA-256 matched `2bd898abefa7b6baa16f7518779ee96c341a3142a1e27abf0671728d00a63e33`. The manager did not rebuild the Java probe. Nominal/enum overlay, multiple variables, complete artifact admission, parser/aggregate limits and full runtime conformance remain implementation gates; Java hard-deadline and production containment are unproven. The broader feature stays active.

Developer 2 retains its isolated checkout/cache. Its next offer is Node runtime implementation under [the portable-core feature](portable-core-first-slice.md), with participation moving to that exact item on acceptance. No demo, collection, time or effect work is added.
