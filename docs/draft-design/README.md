# Later draft design proposals

Added 29 October 2026. These are supplied design proposals extending the preserved [v0.4 baseline](../design-baseline/README.md), not implemented APIs or certified guarantees. The source documents are retained verbatim.

| Document | Scope |
| --- | --- |
| [Host Integration and Hook Contracts v0.5](host-integration-and-hook-contracts-v0.5.md) | Hook definitions, trusted invocation, host-owned result consumption, activation composition, event delivery, simulation and host conformance. |
| [Host-Owned Idempotency and Multi-Effect Contracts v0.7](host-owned-idempotency-and-multi-effect-contracts-v0.7.md) | Original host-key pass-through, certified collision scopes, bounded effect sequences, cross-binding admission, and partial/uncertain outcomes. |

## Reading and reconciliation

- Read v0.5 for the host integration model, then v0.7 for its revised effect/key contract. V0.7 §10 explicitly revises v0.5 §7.2: per-step execution identities do not create distinct downstream idempotency keys. Keir forwards the original host key unchanged, subject to certified collision-scope admission.
- V0.7 supersedes a v0.6 multi-effect proposal that was not supplied here. Its inclusion does not imply that v0.6 is part of this repository.
- The accepted [repository/runtime decision](../decisions/0001-repositories-and-runtimes.md) overrides v0.5's Node-first/Java-later sequencing. Both Java and Node runtimes remain initial scope and must share semantics and conformance.
- The bounded multi-effect proposal changes the baseline's single-mutation restriction only under explicit host admission and compatible certified contracts. It does not establish atomicity, automatic replay or actual adapter certification.
- YAML and SDK examples remain illustrative proposals. Importing the documents does not adopt final schemas, change the roadmap, or establish implementation evidence. Reconcile affected contracts and conformance fixtures when implementing them.
