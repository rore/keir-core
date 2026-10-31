---
id: generic-hook-capability-contract
title: Decouple hook contracts from capability selection
status: active
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Correct the program-specific capability grants embedded in the initial hook contract. A generic hook defines invocation, input/output, lifecycle and effect constraints; customer extensions compose the host-approved capability catalog without the host anticipating each program's calls.

## Product correction

The 2026-10-31 review rejects mandatory per-hook maps such as `tasks.get: 1` and `tasks.createFollowUp: 1` as the default extension model. Capability allowlists/denylists and tighter per-capability restrictions may be explicit advanced controls, not required hook configuration. Host catalog publication, protected bindings and current authorization remain authoritative; source cannot grant itself capabilities or permissions.

Reads may be composed and repeated within aggregate resource budgets. The default mutation policy permits at most one WRITE. Future multi-write support requires proven native adapter idempotency and correct original-key forwarding, plus explicit partial-failure/recovery semantics: duplicate-safe retries do not make several writes atomic. No multi-write execution is authorized by this correction.

## In scope

- Architect: revise maintained hook/catalog/source/admission contracts and both assistant guides; review the baseline against this product requirement. Define omitted/default versus explicit restrictive policy, trusted catalog scope, grants versus budgets, diagnostics, compatibility/versioning and migration from the existing experimental profiles.
- Publish a small coherent contract and concrete examples before compiler/runtime changes. Keep current implemented behavior distinct from the corrected target; changing documentation does not qualify implementation.
- Identify the exact compiler, Java, Node, artifact, host and fixture changes for a later bounded implementation assignment. Manager owns this feature and integration.
- Capture the enduring product invariant in shared role guidance, with regression obligations that detect program-specific hook assumptions.

## Done when

- Reviewed specifications define the generic default without requiring capability names/counts on each hook. Explicit advanced restrictions can only narrow host authority.
- A hook can host different programs selecting other already-published capabilities and repeating reads without editing its contract; no separate product-value or frozen-catalog gate is introduced.
- Compile/admission/execution preserve catalog membership, host bindings, current authorization, aggregate budgets and the default single-write restriction. Unauthorized access, source-controlled policy widening, optional-policy bypass and mutation overflow fail closed.
- Compiler and both runtimes implement the accepted versioned contract and pass shared positive/negative conformance for the same artifact. Existing profile compatibility and migration follow the reviewed decision.
- Public specifications, host examples and both agent guides agree with measured support. Tests retain honest CONFIRMED/NONE/UNKNOWN effects and partial-failure outcomes.

## Current execution

Manager accepted the product correction and is offering the architect a docs-only contract revision. Architecture acceptance is not yet confirmed; no compiler/runtime implementation is assigned. The earlier single-effect checkpoint remains historical passing evidence, not validation of the corrected default. Resolve this contract before using the old per-hook grants as the model for real host integration.
