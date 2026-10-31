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

Design review accepted the [revised contract](../../docs/specs/generic-hook-capability-contract.md) and [complete ownership walkthrough](../../docs/specs/generic-hook-example.md) as the next implementation target. The example keeps host payload `{taskId}` fixed and puts the eligible ProjectId and title in authored source; compilation packages those choices, while runtime authority and protected context remain host-owned. It introduces no configuration subsystem or required capability list. Both agent guides follow these boundaries.

The displayed DCM/source parse successfully and the independently recomputed canonical DCM matches the documented 1,534-byte fingerprint. These are documentation checks, not `/2` compiler or runtime evidence. A focused current-compiler probe confirms the missing nominal-literal support. Reviewed `/2` policy/versioning and typed-literal rules still require compiler, Java, Node, fixtures and host implementation under later bounded assignments. This feature remains active; no implementation is assigned. The earlier checkpoint remains passing `/1` evidence only.

The [model-sharing evaluation](../../docs/internal/artifact-model-sharing-evaluation.md) was accepted on 2026-10-31 as the upcoming `/2` packaging direction. The reviewed [host binding contract](../../docs/specs/host-model-binding.md) now defines the reference envelope, descriptor recipe, full-catalog registration, preparation/invocation boundary and lifecycle diagnostics. Exact approved DCM snapshots and compatible invokers form one reusable BoundModel; implementations remain host code outside DCM/artifact serialization. Per-program validation and fresh authority remain mandatory. Older approved snapshots may continue serving existing extensions without automatic migration. Existing `/1` embedded packaging remains unchanged. No new runtime API is implemented, and no registry service or compatibility fallback is authorized.

Architecture assignment complete: the manager reviewed the binding specification, equivalent Java/Node host sketches, exact reference/descriptor identity, registration failures, remote context/outcome semantics and model coexistence/retirement. The walkthrough and both assistant guides agree with this target; document links, privacy, source/model examples and whitespace are checked. These are design checks, not `/2` execution evidence. The feature remains active for later compiler/runtime/shared-conformance implementation; no implementation is assigned.

The public [guided tour](../../docs/guided-tour.md) has three stages: host design time, customer extension design and runtime. Using the follow-up scenario, it separates reusable host contracts/bindings from customer source/constants, compilation, simulation/activation, fresh invocation context, generic task creation and honest outcomes. Implementation status remains in the specifications rather than narrating legacy behavior in the tour. The guide is documentation evidence, not new runtime qualification.

Implementation alignment is now requested under the [alignment Work Record](../../.agent-workflow/tasks/align-generic-hook-runtime.md). The architect's gap review found no new product decision. A concrete API/fixture freeze and independent technical plan review remain pending; both core developers have confirmed availability. Proposed ownership separates compiler/Java/shared conformance from Node/Node host, with manager-owned integration and records. Code assignment awaits that freeze and review. Acceptance requires corrected read/effect programs compiled once and executed in both runtimes/packaged hosts, shared negative conformance and unchanged `/1` regression. The native demo app remains outside this increment.
