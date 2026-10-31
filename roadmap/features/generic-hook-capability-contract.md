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

The displayed DCM/source parse successfully and the independently recomputed canonical DCM matches the documented 1,593-byte fingerprint. These are documentation checks, not `/2` compiler or runtime evidence. A focused current-compiler probe confirms the missing nominal-literal support. Reviewed `/2` policy/versioning and typed-literal rules still require compiler, Java, Node, fixtures and host implementation under later bounded assignments. This feature remains active; no implementation is assigned. The earlier checkpoint remains passing `/1` evidence only.

The [model-sharing evaluation](../../docs/internal/artifact-model-sharing-evaluation.md) was accepted on 2026-10-31 as the upcoming `/2` packaging direction: reference artifacts plus explicit immutable host-approved model handles, sharing validated model/descriptor state while retaining per-program checks and fresh authorization. The host also supplies native callable capability implementations, bound against the exact model and capability versions; implementations remain host code, outside DCM/artifact serialization. Finalize the exact reference envelope, preparation/binding interface, descriptor identity and lifecycle diagnostics before implementation. Existing `/1` embedded packaging remains unchanged. No new runtime API is implemented, and no registry service or compatibility fallback is authorized.

Next architecture assignment: define one coherent host registration/binding interface tying an exact approved DCM snapshot to its capability invokers, with a reusable bound handle for program preparation. Specify complete catalog binding, version checks, remote dispatch/context/outcome semantics, immutable registration and model coexistence/retirement. Include concise Java and Node host examples and align the proposed `/2` packaging and walkthrough. The bounded design offer has been sent; acceptance and reviewed design are pending. No compiler/runtime implementation is assigned.
