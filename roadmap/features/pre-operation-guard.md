---
id: "pre-operation-guard"
title: "Compile and run an authoritative pre-operation decision"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-core"
---

## Summary

Add a real guard contract distinct from advisory REQUEST/DISPLAY: a typed decision that a host must consume before its own native operation.

## In Scope

- Specify allowed decisions, no-binding/error/timeout fallback, input provenance, deadline reserve and consistency claims.
- Keep native mutation ownership with the host; extension evaluation cannot independently write before commit.
- Implement the supported compiler, artifact, Java/Node and sample consumer paths through an explicit compatibility boundary.

## Out of Scope

Patch consumers, policy-chain composition, native persistence or guarantees Keir cannot enforce.

## Done When

- [x] Design/spec: publish the decision consumer, lifecycle and failure-policy contract without silently renaming an existing profile.
- [ ] Implementation: compile once and run allow/deny decisions through both runtimes and packaged consumers.
- [ ] Verification: shared cases cover invalid results, denied reads, timeout, missing binding, effect prohibition and native-consumption receipts.

## Notes

The authorized slice starts with independent technical design of the typed decision consumer and shared Java/Node acceptance cases. Core owns document formats, compiler/runtime behavior, read-only enforcement and bounded outcomes. Host-specific task fields, persistence, permissions, rule activation and save UX belong to Daymark. Implementation follows review of the smallest supported contract and its public interfaces.

Initial independent review confirmed that existing typed records, enum expressions, binding/invocation APIs and static zero-write enforcement can be reused. The remaining design revisions are explicit producer/model compatibility, separating runtime shape checks from host key authentication, and permitting ordinary host-defined result fields alongside the decision for useful explanations. Semantic implementation follows closure of those findings. The architect owns the contract, Core developers own the compiler and both runtimes, and the documentation owner aligns the packaged agent guide with the verified release.

The revised contract passed independent review at design revision `9d2e593`; all initial Core findings are closed. Implementation is authorized with existing invocation APIs, ordinary bounded output fields, exact old/new execution pairs, and a single current authoring compiler. Compiler/Java and Node contributions have separate task records; combined acceptance still requires identical compiled artifact bytes, complete outcome/consumer parity, retained event behavior and packaged verification. Interactive save recovery remains a host-specific design dependency and does not block generic Core implementation.

Implementation QA caught a mixed-catalog teaching-harness defect: the invoked entrypoint, rather than the presence of any decision hook, must select the consumer. Regression fixtures cover both entrypoints in one model. Independent integration review also reproduced Node accepting a list-valued extra decision output field that the compiler and Java reject, plus inconsistent mixed-generation diagnostics. Both Node boundary fixes and the final teaching-runner delta are integrated; the combined fast runtime suite passes all 60 tests and all five focused example tests pass. Complete Java, source Node and installed Node reports match for the old 28-scenario/51-vector corpus and current 39-scenario/51-vector corpus, including all six invalid generation tuples. Combined structural checks pass for all three retained document generations. Independent result review, current release package smoke and agent-guide archive provenance remain open.

Independent architecture review passed the final Node teaching delta and found a Java teaching-consumer gap: malformed trace step identifiers could consume as ALLOWED in Java while Node blocked them. The compiler/Java owner added matching identifier checks and focused malformed/boundary regressions. All four consumer tests passed, and independent re-review closed the finding on the exact integrated two-file delta. Valid runtime-emitted traces are unaffected. The review found no other blocking issue in the inspected Core semantics or the minimal dual-example verifier change; official package verification remains required.

The public documentation and edition-5 guide-tool delta are integrated and reviewed. Four focused edition-5 tests pass in the combined checkout, including a negative proving that stale source bytes cannot be packaged beside an unchanged artifact. The current quickstart, module/example READMEs and release guide reference both concrete standalone hosts. Final guide links and compatibility hashes must come from the actual integrated commit and qualified package build.

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Synchronous lifecycle and guard pattern, sections 3-6 and 13A](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Hooks and consumers, section 2](../../docs/internal/design-baseline/public/hooks-and-tenant-capabilities.md).
