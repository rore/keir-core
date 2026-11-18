---
id: "capability-combination-policy"
title: "Proposal: constrain capability combinations"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Evaluate whether host-declared restrictions on combining capabilities would usefully supplement native authorization. Individually permitted reads and writes can compose into unintended disclosure; the current [host checklist and illustrative scenario](../../docs/agents/host-integration-assistant.md#capability-composition) address this through host controls.

## In Scope

- Compare a simple forbidden-combination policy with explicit data-flow restrictions, using a concrete customer-read/notification case and a permitted control.
- For forbidden combinations, distinguish merely mentioning two operations from executing them on the same path. Assess conservative rejection of legitimate programs and whether narrower catalogs or adapters already solve the need.
- For data-flow tracking, define whether fields, derived values, conditions/control flow and returned outputs are covered. State untracked paths explicitly; do not claim general noninterference or data-leak prevention.
- If a policy is proposed, identify its host-owned declaration, model/artifact identity, compiler/runtime enforcement, compatibility and Java/Node parity implications before adoption.

## Out of Scope

Implementation now, a general information-flow security system, automatic inference of disclosure policy, replacing native authorization/destination checks, or guarantees across separate invocations and external services without explicit host enforcement.

## Done When

- [ ] Decision: demonstrate a concrete gap in the existing host controls, compare the smallest options and adopt, defer or reject the proposal.
- [ ] Before adoption: define the exact guarantee and counterexamples, permitted/rejected cases, false-positive tradeoffs and interaction with current capability filters.
- [ ] If adopted separately: qualify enforcement in the compiler, both runtimes and an actual host; preserve host authorization and honest effect outcomes.

## Notes

This is an uncommitted idea, not an implementation assignment. The existing [host-conformance work](../features/reusable-simulation.md) retains its native destination-policy scenario independently of this proposal.
