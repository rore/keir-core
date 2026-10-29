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

Implementation started on 2026-10-29. The first contract is accepted under [the specification feature](specify-language-and-domain-model.md), and [scalar CEL qualification](bound-cel-execution.md) is reviewed. Compiler and Node checkpoints are implemented; Java runtime and combined acceptance remain open. The task application is a later increment.

Build checkpoint integrated as `4d7b925`: Maven wrapper 3.3.4 pins checksum-verified Maven 3.10.0; two independent Java modules target JDK 21. JDK 21.0.12, root `clean verify`, isolated compilation/execution smoke, standalone `-f` invocation and POSIX wrapper syntax checks passed. The POSIX wrapper is executable in Git; Linux/macOS execution remains untested. These are scaffold checks with empty jars, not compiler/runtime conformance.

The bounded contract through `4f56279` enables strict source/DCM validation, nominal/enum overlay, real CEL compilation, deterministic artifacts and compiler tests. Java runtime implementation follows the compiler checkpoint. Qualification is integrated as `790ff35`, with final digest evidence `a19e9ad` and YAML clarification `7e49bf7`; Node replay passed. Node work covers artifact admission, scalar interpretation, adapter/authorization/deadline boundaries and the host example. Shared model/source/compiled fixtures must remain consistent. The same Java-compiled artifact and complete Java/Node acceptance matrix remain the exit condition; expression probe success does not close this feature.

Compiler and Node checkpoints passed integration review against the closed host API in `4808a00`. The canonical offline Maven build passed 30 compiler tests in 20.61 seconds including compilation/packaging; the Java runtime module remains empty. Two fresh Java CLI processes emitted identical 2,874-byte golden artifacts with digest `sha256:7ba9478b9d8f47ddc1834b1e4a05ad98326f1a0c81f85e5549e12936c1ad4424`, and the separately installed Node package consumed that output successfully.

The Node suite was independently replayed: 82 tests passed in 0.78 seconds, with 100% source line and 95.42% branch coverage; the combined checkout replay also passed in 2.43 seconds. The packaged Node host passed in 0.27–0.46 seconds warm (9.54 seconds on its first observed worker startup). Review found and corrected a compiler/runtime consumer-ID mismatch with a red/green compiler regression, and fixed Windows tarball integrity drift with scoped LF checkout rules and a regenerated exact host lock pin. Runtime execution preserves the documented synchronous-CEL containment limits.

Java runtime and host implementation is accepted and in progress, including required build/coverage wiring and measured compiler/runtime coverage. Shared-conformance work and its test-only `keir-read-scenarios/1` interface are accepted: fixed reviewed artifact/selectors, one preparation per case, sequential invocations, exact callback queues and independent expected outcomes/calls/traces/consumption. Callback-boundary logical clocks keep shared cases deterministic; native timeout and wire/AST mutation obligations remain in module tests. The conformance owner records the agreed interface and coverage map beside the corpus and coordinates Java transport. Shared executable comparison and real contract CI follow. Packaged Java/Node demonstration and complete parity remain open; the compiler-to-Node path does not close this feature. See [fast regression commands and baselines](../../docs/testing.md).

Cross-runtime review found a Node admission gap: a conditional with record-valued branches can pass when a surrounding field selection returns a scalar. The compiler already rejects it under the scalar-only branch rule. A narrow Node guard/regression and exact packed-example pin update are authorized before continuing conformance; Java admission must enforce the same rule. This follow-up remains unverified until the patch and regression pass.

### Implementation prerequisites

Resolve the DCM-to-CEL type mapping; absent/null and exact numeric handling for any admitted type; source/DCM canonicalization and covered digest bytes; complete portable argument/return/error bindings; separate execution/business/consumption outcomes; and an explicit trusted sample loader. Never assume a self-hash authenticates compiler output. Define diagnostic codes/paths and trace redaction.

Recommended setup is JDK 21 with a repository-local Maven wrapper and an npm/TypeScript runtime package, subject to an initial dependency/tool compatibility check. The local inventory found JDK 21.0.12 and Node 24.14.1; Maven/Gradle are not on PATH. Reproduce historical library versions as evidence inputs, not automatically as the production support matrix. Do not change global tools to make the example run.

### Acceptance and stop condition

Compile twice and compare deterministic artifact bytes; execute one emitted bundle in both hosts; compare normalized results, ordered calls, rejection/failure classes and consumption output. Include type/authority/profile bypass, forged artifact and oversized input/result tests. Measure actual admitted CEL behavior and enforce the declared POC limits. If parity or a necessary bound fails, reject that construct or return to design; do not silently ship Node alone. Stop after this scenario and its rejection corpus pass; do not expand into the task product or follow-up effect in the same assignment.
