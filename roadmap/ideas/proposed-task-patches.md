---
id: "proposed-task-patches"
title: "Proposal: let a hook propose validated native field changes"
status: "queued"
priority: "low"
commitment: "uncommitted"
milestone: "Extend host behavior safely"
repo: "keir-core"
---

## Summary

Evaluate a typed PATCH result that the host validates and applies as part of its own operation.

## In Scope

- Define patchable-field allowlists, input provenance, missing values, validation and consistency requirements.
- Keep the host as mutation owner; no second tasks.create call or independent pre-commit extension write.
- Set conflict/fallback semantics before allowing more than one patch binding.

## Out of Scope

Arbitrary response mutation, automatic patch conflict resolution and changes to native authority.

## Done When

- [ ] Decision: select a native defaulting/patch scenario and exact fields, or defer it.
- [ ] Before adoption: specify patch/result/merge/validation rules and the Java/Node consumer conformance matrix.
- [ ] If adopted: a valid patch persists only through native validation; invalid/protected-field patches or later native failure persist nothing.

## Notes

**Dependencies:** [pre-operation-guard](../features/pre-operation-guard.md); [task-create-guard](../features/task-create-guard.md).

**Design input:** [Patch ownership and example, sections 4, 6 and 13B](../../docs/internal/design-baseline/public/host-integration-and-hook-contracts-v0.5.md).

This is a visible proposal, not implementation authorization. Adoption requires a concrete use case, reviewed contract and explicit owner decision; otherwise record the reason to defer or reject it.
