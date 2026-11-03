---
id: "conversational-authoring"
title: "Author customer extensions through a bounded AI workflow"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Deliver the in-app AI-driven extension creation UI on top of the manual exact-revision review/activation flow. This is part of the complete demo; the customer need not write IR. The person running the app supplies their own AI provider configuration and credentials.

## In Scope

- Discover only the approved scoped catalog, clarify intent and generate/repair source using compiler diagnostics.
- Implement the reviewed Extension Studio as primary agent chat plus a contextual canvas. The agent shows useful visual/interactive components as the conversation evolves; canvas edits and confirmations return explicit user input to chat. Preserve structured behavior/scenario review, draft/active status and recovery without a forced workflow wizard or arbitrary generated application code.
- Bind conversations, tool access, scenarios and activation requests to the signed-in demo user; switching users must not expose another user's customization or conversation state.
- Propose scenarios but keep expected outcomes independently reviewable; explain artifact-derived behavior and simulation differences.
- Request exact activation through host policy, with bounded calls/cost and renewed approval after changes.
- Support configurable provider/model selection through one host-owned authoring workflow; evaluate existing multi-provider libraries versus a small adapter boundary before choosing dependencies. Qualify at least two providers rather than assuming every model or compatible endpoint supports the required tools and structured outputs.
- Keep operator-supplied keys server-side in local environment/ignored configuration; ship placeholders only. Never include keys in browser bundles, source, fixtures, logs or exported scenarios. Missing configuration shows a clear setup state; native tasks and already activated extensions remain usable.
- Let the local operator configure AI from that setup state through an explanatory dialog. Save provider/model, a write-only key and a finite shared allowance in ignored server-side storage; saving configuration makes no provider call.
- Keep customer creation conversational. Manual source/scenario authoring belongs in the [core tools example](../../docs/manual-authoring.md), rather than a developer editor or manual fallback in the demo Studio. Preserve saved customizations, exact evidence inspection and explicit approval/activation.
- Offer one clearly labeled scripted creation example without AI credentials. It illustrates the conversational process and uses the actual supported behavior; it does not claim live AI generation or replace the personal-request flow. The [Studio design feature](extension-studio-design.md) owns the user-requested usability revision.

## Out of Scope

Arbitrary generated application UI/code, agent certification, automatic approval, endpoint registration and an AI provider dependency in core. Contextual canvas components are part of the Studio.

## Done When

- [x] Design/spec: define discovery/compile/simulate/review tools, prompt/data trust boundaries and provider/spend policy; select a small provider/model support matrix and document local configuration, outbound AI data and limits.
- [ ] Implementation: a live business-user request produces a candidate, simulation and approved exact activation; manual authoring remains available in the core tools example.
- [x] Verification: prompt/data injection, forbidden catalog edits, self-approval, stale revisions and exhausted budgets are rejected; activation can be disabled or rolled back through the lifecycle flow.
- [ ] Provider compatibility: the same create/clarify/compile-repair/simulate/review flow works with at least two explicitly qualified provider/model configurations. Fast fake-provider regression covers errors, invalid tool arguments, cancellation and absent/invalid credentials; live provider checks are opt-in with the operator's own keys and explicit spend limits. No provider code enters core.

## Notes

**Current revision in verification:** `agent-workflow:studio-agent-led-experience` implements trusted business context, per-example explanations, conversation-first Studio and a clearly labeled example using the real compiler and simulator without AI calls. Repeated rendered and interaction checks corrected clutter, internal labels, stale approval controls after activation, ambiguous scripted/personal requests, interrupted-example recovery and missing-result presentation, while verifying preservation of unsaved task edits. Final browser regression and presentation review remain in progress. Preserve explicit approval and separate activation; another prototype or responsibility-model approval is not a prerequisite.

**Live qualification blocked:** A bounded trial with a configured provider completed discovery, clarification and real source compilation, then received HTTP 429. One explicit retry after the recovery update also received HTTP 429; paid testing stopped. The saved proposal and conversation were preserved. This verifies a real failure/recovery path, not successful live scenario generation, simulation or activation. Complete that journey when the provider permits requests; qualification of both supported provider configurations remains open. Keep credentials, allowance details and operational evidence private.

**Earlier setup increment delivered:** The [core manual example](../../docs/manual-authoring.md) and demo [local AI setup and conversational-only creation](https://github.com/rore/keir-demo-app/commit/6e0038a375c7311b675f99e2663e389f70f332bc) are pushed. Configure opens an explanatory dialog; settings stay in ignored server-side storage and Save makes no provider call. Technical and rendered reviews pass, with 157 local fast passes, one platform-specific skip and 69 browser passes. The [final hosted build and regression checks](https://github.com/rore/keir-demo-app/actions/runs/41626497707) pass after correcting a redundant click in a short-landscape test fixture. Existing saved behavior and data are preserved, and completed task worktrees are recoverably archived and removed. This extends the read-only flow; live-provider qualification and new runtime semantics remain outside this increment.

**Dependencies:** [extension-studio-design](extension-studio-design.md); [extension-approval-lifecycle](../features/extension-approval-lifecycle.md); [machine-readable-contracts](../features/machine-readable-contracts.md); [reusable-simulation](../features/reusable-simulation.md).

**Design input:** [Customer authoring and approval, sections 1 and 6-7](../../docs/internal/design-baseline/public/simulation-and-authoring-v0.md); [Application authoring UX, section 5](../../docs/internal/design-baseline/public/task-management-poc.md).

Preserves the existing feature ID. Manual immutable approval, activation/disable/rollback and its storage tests are owned by extension-approval-lifecycle; reusable scenario/report tooling is owned by reusable-simulation. This card owns the additional live AI behavior, not duplicate completion promises.

Qualify a provider-neutral adapter against the supported configurations before selecting a dependency.

**Read-only implementation delivered:** Demo [435d509](https://github.com/rore/keir-demo-app/commit/435d509f0a6d77a28d47dbda4de742510a2ac76d) connects Studio chat to two native provider adapters, durable conversations and shared finite reservations, actual compilation and independent scenarios, simulation, exact human review and separate activation. Cancellation, lost responses, restart and user changes preserve honest recovery. Native/manual flows work without AI configuration. The production build, 137 fast tests, 61 browser journeys and [hosted CI](https://github.com/rore/keir-demo-app/actions/runs/41534269475) pass. Independent technical review approves the exact source; rendered checks validate the scoped corrections. The [single Work Record](../../.agent-workflow/tasks/bounded-ai-authoring.md) records the component ownership and combined acceptance.

**Remaining live acceptance:** Automated journeys use scripted provider responses with the real compiler, simulator, storage and browser. Paid attempts are recorded above; neither provider/model configuration has completed live qualification. The live-request completion and provider-compatibility checks remain open. Follow the [configuration and qualification guide](https://github.com/rore/keir-demo-app/blob/main/docs/ai-authoring.md) with operator-supplied credentials and a finite allowance. No complete live-provider compatibility or full feature completion is claimed.
