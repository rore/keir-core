# Shared agent guidance

Load only your selected role and this guide at role entry. For assignments, read the owning feature, relevant contracts and affected code; load other guides, baseline sections and skill references on demand.

## Simplicity

- Choose the simplest architecture, code and process satisfying current requirements. Reuse existing/standard capabilities. Justify extra layers, abstractions, dependencies or infrastructure with a concrete need.
- Start from the smallest end-to-end host example. For each added public input, callback, abstraction or service, identify the current requirement, why existing DCM/artifact/host code cannot satisfy it, and the concrete failure prevented. Remove unjustified additions; existing code/tests, consistency and hypothetical use do not suffice. Independent plan/result review checks actual signatures and call sites and records a short verdict in the owning feature or optional Work Record. Keep compatibility machinery outside the normal host API.
- Find opportunities to remove complexity and improve architecture/testability while developing. Make small relevant refactors with behavior checks. Report larger opportunities with target, proposed change and testing benefit; do not silently expand scope. Manager records them in the owning feature or roadmap idea and schedules follow-up. Chat-only notes are insufficient.
- Preserve required behavior, correctness, security, host guarantees and meaningful verification.

## Contracts and evidence

- Mature only the next runnable increment: architect resolves gaps, developers implement/test, manager accepts combined evidence. Consult on ambiguities; continue unaffected work.
- Normative contracts: `docs/specs/`; product assistant guides: `docs/agents/`; rationale: `docs/internal/`. New supplied drafts go in `docs/internal/design-baseline/public/` with index updates; never a separate draft-design folder. Current decisions/feature scope govern historical drafts.
- Core stays domain-independent; host adapters/AI belong to the host/demo. CEL is internal to Keir. Java and Node share one artifact. Product value is accepted; no frozen-catalog/competitor gate. Proposals, mocks and partial tests do not certify real guarantees.
- Design hook defaults for composition over the host-approved catalog, subject to authorization; never bake a sample program's capability list into the required hook contract. The host defines hook data; customer-specific constants belong in authored programs. Capability filters are advanced opt-in restrictions. Keep effect safety and aggregate resource budgets separate from capability selection.
- Follow [testing guidance](../docs/testing.md): add regression coverage, keep default tests fast, investigate slowdowns and select slow suites explicitly. Preserve protected behavioral obligations.

## Coordination

- Manager alone writes the canonical core roadmap for both repos. Public features own scope/status/evidence; private checkout/routing details stay in ignored local notes. Specify repo, checkout, file ownership and acceptance. Isolate concurrent edits; reserve shared writes and preserve others' work.
- Keep the existing local demo current after verified changes. Reuse its URL and data, back up migrations and verify served assets and preserved state. Routine demo updates/restarts are authorized; obtain approval for changes outside that scope.
- Use collection-coordination for assignments/handoffs and optional participation. Obtain acceptance, not just delivery. Use exact verified work references; tool gaps do not block ordinary work.
- After accepted integration, recoverably archive completed task worktrees. Retain only active work or a concrete upcoming assignment; record the reason and revisit point in ignored coordination notes. Preserve required uncommitted/ignored artifacts. Manager verifies cleanup or justified retention before closing work.
- Prefer Relay for team coordination; use authorized fallback when needed, preserving settings. Do useful independent work or yield for peer results; no busy polling or ACK loops.
- Before assigning a new Relay name, publish its unique mapping to a readable guide inside `roles/`. Verify identity with persistent-agent-roles; never guess/take over a name. Role adoption grants no task or settings authority.

## Privacy, cost and context

- Both repos are intended to become public: treat tracked content and Git metadata as public regardless of current visibility. Exclude local absolute paths, private user/workplace identifiers, emails, session pointers and operational details. Use relative references, public URLs or generic placeholders; inspect changes and commit identity before publishing. Resolve historical disclosures before making a repo public; history rewrite requires explicit authorization.
- Preserve user-selected models/effort; report mismatches. Use cheaper bounded helpers when briefing/review savings justify it; Luna low suits simple work. Keep consequential judgment at suitable capability; reuse reliable evidence.
- Keep persistent guides short, imperative and deduplicated. Store durable rules here, temporary state in its owning record; link detailed references instead of copying them into startup context.
- Follow `AGENTS.md` for environment protection and advisory, opt-in Agent Workflow. Do not require a Work Record for ordinary changes or demo work. Deliver on main without PRs; roles add no approval gate.
