---
id: "real-effect-admission"
title: "Define and qualify admission for real single effects"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Customize a real task application"
repo: "keir-core"
---

## Summary

Establish the reviewed contract and executable evidence needed to admit a real single WRITE. Current effect/2 is intentionally SIMULATION_ONLY.

**Current assignment:** The architect is producing the smallest concrete real-profile/evidence/API contract for the task-completion scenario and local SQLite adapter, aligned with native event and keyed-create requirements. Define a separate compatibility boundary and preserve existing simulation-only enforcement. Compiler/Java/Node implementation follows reviewed contracts and shared cases; no unchecked mode switch is authorized. This design progresses in parallel with host-foundation discovery and Studio review.

## In Scope

- Define evidence identity/version/scope, current authority, native key compatibility, payload mismatch/retention and honest commit/unknown mapping.
- Choose a compatibility boundary explicitly; never reinterpret an existing simulation assurance or enable real mode with an unchecked flag.
- Implement compiler/admission/runtime checks in Java and Node; use concrete native adapter requirements from the demo while keeping core domain-independent.

## Out of Scope

Multiple writes, self-certifying declarations, exactly-once claims, automatic retries and general production certification.

## Done When

- [ ] Design/spec: publish the real-effect assurance/admission contract and required host evidence, including revocation and unsupported transports.
- [ ] Implementation: both runtimes admit only the selected supported real profile and reject missing, stale or incompatible evidence.
- [ ] Verification: shared conformance covers original-key identity, authority, definite NONE/CONFIRMED versus UNKNOWN, deadlines and rejection before dispatch.

## Notes

**Dependencies:** [generic-hook-capability-contract](../features/generic-hook-capability-contract.md).

**Design input:** [Native contracts and evidence, sections 3 and 8](../../docs/internal/design-baseline/public/domain-model-v0.md); [Host integration, sections 5-7 and 12](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md); [Keys and admission, sections 3-7](../../docs/internal/design-baseline/public/host-owned-idempotency-and-multi-effect-contracts-v0.7.md).

Design this alongside the native event/effect prototype, without a dependency cycle: the prototype supplies requirements; real execution is accepted only after this gate. Rich collections and AI are not prerequisites.
