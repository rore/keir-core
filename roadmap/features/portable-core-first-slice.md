---
id: portable-core-first-slice
title: Compile once and run the same artifact in Java and Node
status: queued
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Compile one handwritten extension and execute the exact artifact with deterministic fake adapters in both initial-scope runtimes.

## Why

Expression interoperability alone cannot establish whole-program portability or bounded execution.

## In Scope

Strict parsing; trusted DCM and capability binding; checked CEL and needed type, dataflow and effect checks; deterministic artifact and diagnostics; artifact admission; prepared execution; typed outcomes and traces. Cover happy path, skipped write, business rejection, transient failure and unknown commit.

## Out of Scope

Production activation, unrestricted CEL, durable waits, retries, multiple writes and a compiler service.

## Done When

- One command compiles the source; Java and Node examples run identical artifact bytes.
- Shared conformance compares typed results, ordered calls, guards, effects and termination classes.
- Tampered or incompatible artifacts and unauthorized operations fail closed.
- No task-specific behavior enters the generic compiler or runtimes.

## Notes

The [CEL resource work](bound-cel-execution.md) supplies admitted expression semantics and bounds.
