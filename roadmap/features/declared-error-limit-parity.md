---
id: declared-error-limit-parity
title: Reject oversized read/2 error catalogs during compilation
status: active
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

The read/2 compiler omits the 16-declared-error limit already enforced by its schema and both runtimes. Reject invalid models during compilation rather than emit an artifact whose model the host cannot load. Runtime rejection remains in place; the defect is compatibility and early validation, not an authorization bypass.

## Done When

- [ ] Reproduce the compiler/runtime mismatch at the 16/17 boundary.
- [ ] Apply the existing limit to read/2 compilation, preserving legacy read/1 behavior and existing effect-profile validation.
- [ ] Compiler, Java and Node boundary regressions agree; existing conformance passes without weakening admission or changing valid artifacts.

## Notes

Bounded follow-up to [generic hook contracts](generic-hook-capability-contract.md). The manager owns planning; this fix runs independently of demo UX and real-task scenario design.
