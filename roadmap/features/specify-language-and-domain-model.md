---
id: specify-language-and-domain-model
title: Specify the initial language and domain capability model
status: active
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Turn relevant baseline drafts into maintained public specifications for the first supported language and domain capability model, with conformance examples and guidance for both product agent personas.

## Why

Drafts contain unresolved choices and overlapping terminology. Implementations and agents need one explicit contract distinguishing proposals, reviewed rules and verified support.

## In Scope

Review the initial subset's language and DCM drafts, resolve or bound gaps, and define syntax, types, call/dataflow rules, host bindings, effects, errors, compatibility and the restricted CEL profile. Publish under `docs/specs/`, identify superseded baseline sections and align schemas and valid/invalid examples. Refine both product agent guides and authoring/discovery requirements as contracts mature. Runtime experiments feed back into the specification; no whole-system design freeze.

## Out of Scope

Claiming production readiness from document review alone, implementing every proposed feature, a frozen-catalog product-value study, shipping autonomous agents in core, or packaging skills before contracts exist.

## Done When

- Maintained language/DCM specifications define the supported subset, rejection/error semantics and deferred constructs.
- The reviewed contract identifies host authority and distinguishes host-assistant proposals from customer authoring.
- Schemas and valid/invalid examples agree with written rules and identify required executable verification.
- Both agent guides explain task, authorized inputs/actions, required tools and approval boundaries without duplicating normative rules.
- Portable-core implementation can reference the contracts and report conformance; runtime/host guarantees require evidence.

## Notes

Original inputs remain in [the internal baseline](../../docs/internal/design-baseline/README.md). The [specification index](../../docs/specs/README.md) records contract status. This is engineering specification work, not proof of product value.

The architect completed a full source review in [the planning review](../../docs/internal/development-plan-review.md); that is a planning contribution, not completion of these specifications. Start with the contracts needed by [the read-only demonstration](portable-core-first-slice.md), then extend them for [one guarded effect](single-effect-followup.md). Do not require the full proposed language/DCM to be specified before starting the first runnable implementation. Later features update the same maintained contracts rather than copying them.

### Current assignment

Architect accepted on 2026-10-29: mature only the read-and-decide subset in public specifications, both agent guides and a focused internal implementation handoff. Isolated checkout: `C:/Users/WORK_ID/.codex/worktrees/keir-read-contract/keir-core`, starting at `4eadecb`. Reserve writes to `docs/specs/**`, `docs/agents/**` and `docs/internal/read-and-decide-design.md`; no roadmap or implementation edits. Incorporate the developers' real CEL/tooling evidence, distinguish proposed contracts from verified support, and report an isolated commit for manager integration. The authorized app fallback is in use because fresh Relay self/scope remains unavailable in the architect chat; no role registration or participation success is claimed. This bounded contract does not complete every future language/DCM requirement.

Contract checkpoint reviewed and integrated as `b9e20b9`, `827ac7b` and `4f56279`. Five maintained specifications, both agent walkthroughs and the focused handoff now define the implementation candidate. Manager checks passed for nine files' local link targets, sample DCM parsing and host/READ/DISPLAY boundaries. Required-value validation precedes protobuf projection; externally held expected digest anchors sample admission; authorization failures and host monotonic millisecond deadlines have explicit behavior. Developer-reported scalar bridge evidence and exact pins/overloads are attributed, with final qualification and boundary evidence still pending. This unblocks implementation against the candidate contract; it does not declare a supported runtime or close the broader specification feature.
