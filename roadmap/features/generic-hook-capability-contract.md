---
id: generic-hook-capability-contract
title: Decouple hook contracts from capability selection
status: done
priority: high
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Correct the program-specific capability grants embedded in the initial hook contract. A generic hook defines invocation, input/output, lifecycle and effect constraints; customer extensions compose the host-approved capability catalog without the host anticipating each program's calls.

## Product correction

The 2026-10-31 review rejects mandatory per-hook maps such as `tasks.get: 1` and `tasks.createFollowUp: 1` as the default extension model. Capability allowlists/denylists and tighter per-capability restrictions may be explicit advanced controls, not required hook configuration. Host catalog publication, protected bindings and current authorization remain authoritative; source cannot grant itself capabilities or permissions.

The host defines the hook payload independently of customer programs. A customer-selected project and any fixed follow-up title belong as constants inside the authored extension, unless its explicit logic obtains them from another approved source. Do not add sample-specific hook inputs or require an extension-configuration system to carry these choices. Define typed literal support rather than moving program constants into host input to work around compiler limitations.

The host's creation capability is generic `tasks.create`, with project/title arguments. The extension implements follow-up behavior through its condition and composition; the host must not pre-implement it as a specialized service operation. No source-task creation argument or implicit relationship is required by this scenario. Native event/key context remains protected and separate. Carry this correction through later `/2` fixtures and host adapters while preserving `/1` compatibility evidence.

Reads may be composed and repeated within aggregate resource budgets. The default mutation policy permits at most one WRITE. Future multi-write support requires proven native adapter idempotency and correct original-key forwarding, plus explicit partial-failure/recovery semantics: duplicate-safe retries do not make several writes atomic. No multi-write execution is authorized by this correction.

## In scope

- Architect: revise maintained hook/catalog/source/admission contracts and both assistant guides; review the baseline against this product requirement. Define omitted/default versus explicit restrictive policy, trusted catalog scope, grants versus budgets, diagnostics, compatibility/versioning and migration from the existing experimental profiles.
- Publish a small coherent contract and concrete examples before compiler/runtime changes. Keep current implemented behavior distinct from the corrected target; changing documentation does not qualify implementation.
- Identify the exact compiler, Java, Node, artifact, host and fixture changes for a later bounded implementation assignment. Manager owns this feature and integration.
- Capture the enduring product invariant in shared role guidance, with regression obligations that detect program-specific hook assumptions.
- Establish one compact ownership model and audit the worked source-to-execution example against it: host-defined payload/catalog/adapters/authority, customer-authored logic/constants, compiler artifact, runtime bindings and both authoring-agent roles. Account for each value's origin; do not accept isolated field fixes while those boundaries remain ambiguous.
- Treat internal service-to-service calls, including HTTP/RPC, as the normal capability integration. Define the adapter as a host-owned dispatch bridge, not a same-process business implementation requirement. Carry trusted identity, deadline and honest remote-failure/effect semantics through the final binding API without adding a transport framework.
- Before implementation, finalize the adopted shared-model packaging: exact immutable host-held DCM references, reusable model preparation and version-compatible callable capability registrations. Define identity/trust, version coexistence, revocation and missing-model behavior without a registry service or network lookup. Preserve existing `/1` embedded packaging.

## Done when

- Reviewed specifications define the generic default without requiring capability names/counts on each hook. Explicit advanced restrictions can only narrow host authority.
- A hook can host different programs selecting other already-published capabilities and repeating reads without editing its contract; no separate product-value or frozen-catalog gate is introduced.
- Changing a customer's project constant changes the authored program/artifact, not the hook payload or host implementation. Compiler typing and Java/Node conformance cover these constants without weakening nominal types.
- Compile/admission/execution preserve catalog membership, host bindings, current authorization, aggregate budgets and the default single-write restriction. Unauthorized access, source-controlled policy widening, optional-policy bypass and mutation overflow fail closed.
- Compiler and both runtimes implement the accepted versioned contract and pass shared positive/negative conformance for the same artifact. Existing profile compatibility and migration follow the reviewed decision.
- Public specifications, host examples and both agent guides agree with measured support. Tests retain honest CONFIRMED/NONE/UNKNOWN effects and partial-failure outcomes.

## Current execution

Implementation is locally verified at `933c5c8`: explicit `/2` compilation, nominal source constants, generic `tasks.create(projectId,title)`, reference artifacts, complete host-bound DCM/invoker registration and fresh scoped execution in Java/Node. The [worked example](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/specs/generic-hook-example.md) and [three-stage tour](../../docs/guided-tour.md) keep host payload/authority separate from customer logic/constants. Two models support six compiled programs, including alternate/repeated reads and source-only project/title variants. Existing `/1` APIs and goldens remain unchanged.

The [alignment Work Record](../../.agent-workflow/tasks/align-generic-hook-runtime.md) records accepted delivery. Final checks pass: 216 Java tests, 367 Node tests, three installed-host tests, complete legacy corpora and all nine generic read/nineteen generic effect rows through sixteen compiler processes/eight host batches. Package byte/integrity checks pass. Independent final technical review and human result review approved the result; see [testing evidence](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/testing.md).

Effects remain simulation-only. Real service authorization, native idempotency, transport guarantees, decoded-heap containment, production qualification and release remain open. No registry/activation service, multi-write execution or demo application was added.

Human result review approved delivery on 2026-11-01. Main contains the change at `55c0f27`; [product CI](https://github.com/rore/keir-core/actions/runs/41280457363) and [workflow CI](https://github.com/rore/keir-core/actions/runs/41280457420) pass. Both completed contributor worktrees are recoverably archived with required evidence preserved. Specifications and both product-assistant guides describe measured `/2` support. This feature is complete; subsequent independently progressing deliveries use distinct records under their owning features.
