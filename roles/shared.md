# Shared working guidance

## Project authority

The product enables hosts to expose hooks and approved capabilities once so customers can compose new behavior without customer-specific host code. CEL is the expression evaluator inside Keir. AI authoring belongs to the host. Both Java and Node runtimes are initial scope and share one artifact and conformance contract.

The user accepts this product value from prior experience. Do not require a frozen-catalog exercise, competitive bakeoff or separate value-validation stage before development. Let the catalog evolve through implementation; continue to verify actual host contracts, compatibility, authority, runtime bounds and cross-runtime behavior.

Read [current decisions](../docs/internal/README.md), the relevant [design baseline specifications](../docs/internal/design-baseline/README.md), and the owning [roadmap feature](../roadmap/board.md). The v0.4 baseline and later proposals are design inputs, not proof of implementation or host guarantees. Store additional supplied design specifications with the baseline in `docs/internal/design-baseline/public/` and update its index; do not introduce a separate draft-design folder. Existing accepted decisions prevail until explicitly revised. V0.7 revises v0.5's downstream-key rules; v0.5's Java-later wording does not supersede our two-runtime decision. Some roadmap items still retain the single-write restriction: resolve that scope with the manager before using the multi-effect proposal to expand an assignment.

## Simplicity

Choose the simplest architecture and implementation that fully satisfies the requirements. Prefer clear, direct solutions with few moving parts, using existing code and standard capabilities where they fit. Add abstractions, layers, dependencies or infrastructure only for a concrete current need; defer hypothetical future flexibility. When complexity is necessary, explain which requirement or observed constraint makes the simpler option insufficient. Simplify the solution without dropping required behavior, correctness, security, host guarantees or meaningful verification. Apply this principle to designs, code, documentation and the development process.

## Design and development loop

1. The manager selects a bounded feature or iteration with observable value, identifies its owning roadmap item, and defines acceptance evidence and dependencies.
2. The architect examines the applicable draft design against that feature, challenges assumptions and failure paths, and produces a maintained implementation design or a focused revision. Keep rationale/internal design under `docs/internal/`, normative contracts under `docs/specs/`, and the two product agent guides under `docs/agents/`. Identify source sections, decisions retained or revised, alternatives, unresolved questions and conformance evidence needed.
3. The manager aligns product scope and sequence. Surface consequential product changes, weakened guarantees and unresolved human decisions to the user. The architect owns the technical recommendation; authoring a recommendation does not grant authority to expand scope.
4. Developers implement the agreed slice and verify observable behavior. They can consult the architect directly when implementation exposes an ambiguity. Pause only the affected portion; continue independent work. Record the conclusion in the maintained design or decision record and notify the manager of scope or sequencing effects.
5. The manager reviews delivered revisions and evidence, obtains independent review where the risk warrants it, reconciles the combined result across repositories, and updates the owning roadmap item. Close only the delivered scope; an unfinished feature remains unfinished.

Use the smallest complete iteration. A design need not settle every future capability before an executable slice. Separate what must be decided before coding from what a bounded experiment should answer. Keep pending questions visible without making each routine edit a ceremony.

## Coordination and checkout ownership

Before introducing or assigning a new team Relay name, the manager must add its exact `@name` entry to `roles/mapping.yaml`, create or select its guide, and validate that the mapping has unique keys and resolves to readable Markdown within `roles/`. Publish the guidance before starting the new session. The receiver verifies its current name and reloads the mapped guide; naming alone does not complete role setup.

Use Pallium Relay for independent sessions when available. The user authorizes manager/developer/architect assignment and consultation messages within this Keir team. Use the collection-coordination skill for offers, acceptance, dependencies and handoffs. Delivery is not acceptance. Respect existing assignments; obtain availability rather than inferring it from an idle label. Consultations do not silently transfer ownership.

The manager is the only writer of the canonical core roadmap checkout during team work. Workers report exact item references, status changes, revisions, checks and blockers to the manager. Use isolated checkouts for concurrent edits; core and demo assignments need explicit repository paths and conflict boundaries. Do not alter or clean another worker's checkout. A single bounded task can use the shared checkout only when the manager has reserved it against concurrent writes.

Use verified task/feature references from the owning tools and Pallium participation when available; do not invent references or make tool availability a blocker. Keep temporary assignments in the existing roadmap/task records. No parallel task registry or copied demo roadmap.

## Budget and context

- Keep the architect on Astra medium and developers on Sol 6.1 high as requested. Preserve the manager's selected model. These are role settings, not a reason to send every small task to an expensive session.
- Delegate bounded mechanical edits, inventory, focused research and routine verification to a cheaper capable subagent when the expected savings exceed briefing and review costs. Start with Luna low for simple work; use a stronger model or effort when uncertainty or risk warrants it. Native subagents handle these tasks; independent team sessions communicate through Relay.
- Architecture judgments, security/effect guarantees and consequential review stay with a capable reviewer. Never omit required validation or independent review to save tokens.
- Brief workers with the exact files, decisions, scope, expected evidence and stop condition. Send sufficient relevant context rather than whole transcripts. Reuse trustworthy results and avoid duplicate research, repeated broad tests or status ping-pong.
- Do small tasks directly when delegation costs more. Do not start idle sessions, polling loops or background work without a useful assignment. State why a more expensive investigation is needed and bound it before expanding.
- At natural task boundaries, refresh sessions when stale or crowded context impairs work. Follow collection-coordination's handoff procedure with the exact roster; do not replace a live owner or hand off from a generic recap.

## Lightweight workflow and environment

Agent Workflow remains opt-in, its CI findings advisory, and direct-to-main development available under repository rules. Roadmap and the named ordinary-documentation updates need no Work Record. Team roles add no mandatory PR or approval gate. Match review and verification to the actual risk.

Keep experiments and tools isolated. Changes to the user's installed tools, applications, global settings or services need the explicit environment approval required by `AGENTS.md`. Ordinary repository edits and isolated tests are authorized within the assignment.
