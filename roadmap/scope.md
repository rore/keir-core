# Roadmap

This is the single roadmap for `keir-core` and `keir-demo-app`, organized as milestones containing feature cards. Each milestone describes an observable capability; each feature owns its outcome, repository, status and completion criteria. The manager owns direction, sequence, assignments and cross-repository acceptance. Implementation and tests stay in the repository whose code changes.

## Milestones

1. **Execute extensions in Java and Node.** Mature the initial language and DCM specifications with agent-consumable guidance, establish the admitted CEL profile and resource evidence, then compile once and execute the same artifact in both runtimes with shared conformance.
2. **Customize a real task application.** Deliver a useful native task app and demonstrate approved Keir behavior with real host adapters, result consumption, simulation and activation.
3. **Author and activate customizations through AI.** Let a business user describe behavior, inspect simulations and approve an exact revision for activation, with disable and rollback.

## How we manage work

The value of composing approved capabilities at host-defined hooks is an accepted product premise based on the user's prior product experience. A frozen-catalog exercise, comparative extensibility benchmark or separate proof of product value is not a prerequisite for implementation. Evolve the catalog with real implementation needs while verifying its technical contracts and compatibility.

Minimap opens grouped by each feature's `milestone` metadata; `roadmap.config.json` defines milestone order. `board.md` keeps one shared feature ordering under a neutral Features heading. It does not duplicate milestone membership. Use the repo and status filters to inspect where work belongs and what is active. Feature order expresses priority within each milestone.

Architectural review, implementation assignments and verification happen within the owning feature. Review the applicable baseline design for gaps before substantial implementation, maintain the agreed contract, and record delivered evidence and remaining scope in that same feature. Break a large feature down when needed for execution; do not introduce another roadmap level for slices or a separate release-version plan.

Specifications and agent-facing contracts are explicit deliverables. Review the affected draft, write the maintained contract and conformance examples, implement it, and reconcile findings into the contract. Public specifications live under `docs/specs/`; both product agent guides live under `docs/agents/` and begin with the first language features. Runnable host and customer agent integrations arrive later. Internal designs, decisions and setup records live under `docs/internal/`.

Independent demo work can proceed alongside core work once its scope and contracts are agreed; the milestone order does not require every developer to wait for the preceding milestone to finish. Feature completion must still satisfy its declared dependencies and combined acceptance criteria.

Agent Workflow is advisory and opt-in in core, with advisory CI on main pushes and PRs. The demo needs no second roadmap or Work Record process. Both GitHub repositories are initialized privately; license and build choices remain open.

The [internal design baseline](../docs/internal/design-baseline/README.md) contains the draft inputs. Current decisions cover [repository boundaries](../docs/internal/decisions/0001-repositories-and-runtimes.md), [product direction](../docs/internal/decisions/0002-product-proposition-and-evaluation.md) and [restricted CEL](../docs/internal/decisions/0003-restricted-cel-resource-policy.md). Public specifications, draft design approval and implementation evidence remain distinct.
