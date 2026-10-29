---
id: portable-core-first-slice
title: Run a read-and-decide extension in Java and Node
status: active
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Compile a handwritten extension for an on-demand read-only hook: look up a task, classify it from its status, and return a typed result. Run identical artifact bytes in Java and Node and show each sample host consuming the classification.

## Why

The first working result exercises source, DCM, compiler, portable artifact, both runtimes, an approved adapter and a result consumer together. It is an advisory request/result path, not yet an authoritative guard over a native transaction. Expression interoperability or a compiler-only test is not this demonstration.

## In Scope

Strict parsing; a minimal trusted DCM; bounded required records, IDs, enums, Boolean logic and the measured restricted CEL subset; one statically named task-read capability; unguarded call, immutable let and typed return; checked-expression and argument/return bindings; deterministic artifact/diagnostics; host-controlled admission; prepared execution; typed outcome/trace; and a consumer that displays results only on permitted completion. Both compiler and runtime reject disallowed calls and protected-field overrides before dispatch. Types and constructs outside the implemented profile are explicitly rejected.

Resolve only the contracts needed for this scenario, publish their supported subset under `docs/specs/`, and update both agent guides with a valid example, rejection diagnostics and authority boundaries. The specification and CEL features support this runnable increment; they are not a requirement to finish every future language construct first.

## Out of Scope

Guards over native transactions, extension-owned writes, when/onError constructs, optional-result dereference, collections, broad standard-library admission, production activation/signing infrastructure, a complete task UI, durable waits/retries, multiple effects and a compiler service. Integrity checks alone must not be described as authenticating an artifact; the sample host needs a defined trusted compiler-output admission path.

## Done When

- One documented command compiles the example and demonstrates different classifications for completed/open task inputs with the same artifact in Java and Node. Changing the source condition changes the result without changing host logic.
- Both samples show the approved read, typed returned classification and separate host-consumption result; neither claims to enforce a native business invariant.
- Shared conformance compares results, ordered calls, host-bound arguments and execution/failure outcomes. Missing/inaccessible task, transient read failure or invalid adapter result never produces a fabricated ordinary decision.
- Unknown capability, forbidden write, host-field override, invalid source/type, oversized input, and incompatible or tampered artifact cases fail closed with no unauthorized adapter dispatch.
- The admitted CEL profile, tested data/budget limits, artifact provenance boundary and remaining containment limitations are documented; no unverified production deadline guarantee is claimed.
- No task-specific behavior enters the generic compiler or runtimes.
- Fast default regression commands, component coverage baselines and relevant explicit slow checks are documented with timings. The [selected behavioral contract](../../docs/testing.md#initial-protected-contract) runs in a real CI check before workflow-level protection is activated; advisory governance does not make test failures advisory.

## Notes

First runnable increment. The [specification work](specify-language-and-domain-model.md) and [CEL work](bound-cel-execution.md) mature only the subset required here and expand alongside later scenarios. The [single-effect follow-up](single-effect-followup.md) carries the later guarded-write, business-rejection and uncertain-effect cases from the original broader first-core feature. Real pre-create guard semantics belong to [task integration](demo-task-integration.md). All samples initially use controlled fixtures/adapters; they do not certify real application guarantees.

The two core developers cover compiler, both runtimes, shared fixtures and Java/Node embedding samples through bounded manager assignments. Divide independent implementation and verification against agreed contracts; do not duplicate the artifact contract or force a permanent runtime split. The architect resolves the minimal language/DCM/artifact/outcome contracts and reviews conformance; the manager owns acceptance and dependency sequencing. Confirm exact module/build choices and checkout ownership in the implementation assignment.

### Current execution

The local read-and-decide checkpoint passes as of 2026-10-29. The Java compiler emits the same 2,874-byte artifact for both runtimes; two fresh compiler processes reproduce the golden digest `sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424`. Both independently packaged hosts demonstrate DONE/PENDING display and missing/wrong-resource failure consumption.

Canonical verification passed 92 Java tests (31 compiler, 56 runtime, 5 host) in a 35.40-second clean coverage build and 154 default Node tests in 3.80 seconds. The combined verifier compares the entire 61-case/52-invocation transport, fresh artifact bytes and both packaged hosts. Both module suites also execute the same compiled two-read artifact, covering grant exhaustion before second-call authorization, counter/identity reset and first-read failure. [Commands, coverage baselines and remaining misses](../../docs/testing.md) are recorded separately from cold dependency setup.

Review corrected consumer-ID validation, scalar-only conditional admission, canonical DCM overflow and cross-invocation timeout scheduling. The architect clarified independent raw/canonical DCM ceilings without widening the profile. Runtime-local artifact helpers keep compiler/source parsing out of runtime dependencies; no shared module or worker framework was added. Hard synchronous deadlines, complete process-memory isolation and real adapter guarantees remain unqualified.

Product CI is approved for installation and actual cloud verification. The [CI proposal](../../docs/internal/workflow/core-contract-ci-proposal.md) records the job and a separate proposed workflow-level test-contract block. The feature remains active until the real product check passes; no branch protection, required PR or mandatory Work Record is added. Broader language/effect/demo/agent work stays deferred.

### Acceptance and stop condition

Compile twice and compare deterministic artifact bytes; execute one emitted bundle in both hosts; compare normalized results, ordered calls, rejection/failure classes and consumption output. Include type/authority/profile bypass, forged artifact and oversized input/result tests. Measure actual admitted CEL behavior and enforce the declared POC limits. If parity or a necessary bound fails, reject that construct or return to design; do not silently ship Node alone. Stop after this scenario and its rejection corpus pass; do not expand into the task product or follow-up effect in the same assignment.
