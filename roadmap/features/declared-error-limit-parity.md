---
id: declared-error-limit-parity
title: Reject oversized read/2 error catalogs during compilation
status: done
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

The read/2 compiler omits the 16-declared-error limit already enforced by its schema and both runtimes. Reject invalid models during compilation rather than emit an artifact whose model the host cannot load. Runtime rejection remains in place; the defect is compatibility and early validation, not an authorization bypass.

## Done When

- [x] Reproduce the compiler/runtime mismatch at the 16/17 boundary.
- [x] Apply the existing limit to read/2 compilation, preserving legacy read/1 behavior and existing effect-profile validation.
- [x] Compiler, Java and Node boundary regressions agree; existing conformance passes without weakening admission or changing valid artifacts.

## Notes

Bounded follow-up to [generic hook contracts](generic-hook-capability-contract.md). The manager owns planning; this fix runs independently of demo UX and real-task scenario design.

Implementation at 878e6c9 passes independent result review: 233 Maven tests, 391 Node tests and shared/packaged conformance preserve exact reference artifacts. Main integration fb48d7a retains the reviewed files; [product CI](https://github.com/rore/keir-core/actions/runs/41845245471) and [workflow CI](https://github.com/rore/keir-core/actions/runs/41845244950) pass. Recovery archive integrity/restore and task-only cleanup are verified, and completed participation is detached. No runtime-authority or profile limit was weakened.
