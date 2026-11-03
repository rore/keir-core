# Agent-led Studio experience

The owning sources are [conversational authoring](../../roadmap/features/conversational-authoring.md), [Studio design](../../roadmap/features/extension-studio-design.md), the customer assistant guide and the demo interaction reset. This record owns full delivery, including backend, UI contributions, integration and repeated actual interaction verification.

<!-- agent-workflow:start -->
**Outcome:** A business user develops a supported customization through an agent-led conversation, understands checked examples, and explicitly approves then separately turns on the exact checked behavior.

**Target:** Keir demo application.

**Scope:** Trusted scoped business context and agent coaching, useful per-case simulation receipts, one honest no-AI scripted example using actual authoring and lifecycle services, conversational UI and contextual canvas, integration, documentation and repeated rendered/live-AI verification.

**Constraints:** Preserve saved customizations, independent expectations, exact evidence, owner/session isolation, revision checks, resource and cumulative spending limits, cancellation and uncertainty. Keep six existing tools and explicit human approval separate from activation. No manual source editor, pipeline dashboard, new native writes, arbitrary interpreter, secrets in browser/logs, preview data changes or global environment changes. Billable tests require an isolated dataset and settled bounded cap/ledger policy.

**Completion criteria:** A personal request and the labeled scripted example lead through meaningful clarification, positive and no-action examples, mismatch explanation/correction, exact human review and separate activation using real services. Multiple rendered interaction iterations verify usability and recovery; bounded real AI testing records actual results and limits. Fast and full browser regressions and independent technical/rendered reviews protect existing guarantees.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A business user develops a supported customization through an agent-led conversation, understands checked examples, and explicitly approves then separately turns on the exact checked behavior.","scope":"Trusted scoped business context and agent coaching, useful per-case simulation receipts, one honest no-AI scripted example using actual authoring and lifecycle services, conversational UI and contextual canvas, integration, documentation and repeated rendered/live-AI verification.","constraints":"Preserve saved customizations, independent expectations, exact evidence, owner/session isolation, revision checks, resource and cumulative spending limits, cancellation and uncertainty. Keep six existing tools and explicit human approval separate from activation. No manual source editor, pipeline dashboard, new native writes, arbitrary interpreter, secrets in browser/logs, preview data changes or global environment changes. Billable tests require an isolated dataset and settled bounded cap/ledger policy.","completion_criteria":"A personal request and the labeled scripted example lead through meaningful clarification, positive and no-action examples, mismatch explanation/correction, exact human review and separate activation using real services. Multiple rendered interaction iterations verify usability and recovery; bounded real AI testing records actual results and limits. Fast and full browser regressions and independent technical/rendered reviews protect existing guarantees."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"},{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Demo code and tests are unclassified under the core governance policy, imposing an Elevated floor. This is one coordinated demo delivery with several affected components; core changes are limited to its owning Work Record.

**Discovery:** The current six-tool loop already owns durable checkpoints, bounded repair, CAS, cancellation, cumulative reservations and exact review. Discovery returns a YAML source example despite the historical sourceJson argument name, but no scenario or business coaching. Simulation and review return only status arrays, preventing grounded mismatch explanation. Existing authenticated compile/scenarios/simulate APIs provide the entire no-AI preparation path; the customer need not operate them. Simulator evidence already includes independent expected outcomes, sanitized native/consumption results and mismatches. Source, owner task records and secrets must not be added to the browser transcript. No new provider/library/runtime semantics or persistence migration is needed. Demo implementation/test paths are GRAY; the core record is excluded. Canonical planning edits remain manager-owned.

**Material assumptions:** Existing authoring, simulation and exact lifecycle services can support the interaction without changing native runtime semantics. If that is false, return affected design to planning. Live testing must preserve a shared finite allowance; no paid calls before the manager settles fixture/ledger policy.

**Plan:**
1. Keep all six tool schemas, budgets, host identity and review/activation gates. Expand trusted agent instructions to own clarification, source/scenario preparation, bounded repair and ordinary-language explanations; treat customer/tool/source/scenario strings as data. Add static host business context (supported read-only advice, unavailable writes/follow-ups and explanation of checking), a compact worked scenario alongside the existing source example, and current exact draft handles through discover_catalog. Do not send native task records or names to providers.
2. In src/authoring-service.mjs, replace status-only simulation/review summaries with one shared bounded projection retaining statuses and adding cases: id, label, status, expected, actual native and consumption, and mismatches. Include exact evidence identity outside the summary as today. Read only owner-scoped immutable evidence, cap case/mismatch text and collection sizes, report truncation explicitly, and never derive or rewrite expectations from actual results. Preserve public transcript DTO privacy; full exact proof remains available through its existing authenticated endpoint.
3. Add a small host-owned example module and authenticated GET /api/authoring/example in src/app.mjs. DTO: label, description, source (existing Pending YAML), cases (five original JSON strings excluding host-denied). This is inert read-only example data without AI setup, provider calls or writes. UI collects explicit business confirmation and orchestrates existing compile, scenarios and simulate endpoints with returned CAS handles; partial failure reads saved state before explicit continuation. No new interpreter, workflow engine or write API. Real compiler, simulator, review, human approval and separate Use remain authoritative.
4. UI contributor owns src/web/** and browser suites: replace pipeline-oriented creation with a continuous conversation and only useful contextual canvas; explain ordinary situations, ask necessary business choices, preserve old evidence, selection, drafts, native editing and owner-switch recovery. The scripted example is clearly labeled and never interpreted as a personal live request. Existing saved/prepared choices, history and exact inspection stay available. Confirm this DTO before overlapping implementation; request review of material uncovered interface changes.
5. Owner implements backend/context/results/example and focused tests; bounded helper contributions may own nonoverlapping test or documentation files under this same record. Target src/authoring-agent.mjs, src/authoring-service.mjs, src/app.mjs, src/authoring-example.mjs, tests/authoring-context.test.mjs and affected existing regression expectations; operator documentation in docs/ai-authoring.md explains outbound static context and real/scripted differences. Integrate reviewed UI commits into the isolated source branch.
6. Iterate actual interactions multiple times, correcting usability and technical findings between runs. Cover ambiguous personal request, clarification, no-action case, mismatch and explicit correction, unsupported follow-up request, review/approval/Use, interruption, stale state and owner switch. Run fast and focused/full browser regressions; obtain independent technical and designer rendered review of the combined source. Only after manager settles an isolated cloned dataset with shared finite reservation accounting and a small per-run cap, run bounded real-AI interactions without reading/printing secrets or changing the operator allowance. Manager alone delivers canonical roadmap/push/preview. Recoverably archive task evidence and remove task worktrees after accepted integration/release.
Stop affected work on identity/evidence/budget assumption failure, material interface change or unsupported native effect. Preserve the requirement baseline; no planning-only completion or new human gate is introduced.

**Verification plan:**
- Grounded supported behavior: tests/authoring-context.test.mjs checks trusted context, worked source/scenario, unchanged six schemas and useful bounded per-case expected/actual/mismatch receipts against real immutable lifecycle proof, including wrong expectation and no automatic rewrite.
- Honest no-AI example: authenticated inert GET with no query fields, no setup/provider calls or mutations; exercise returned source/cases through real compiler, simulator and explicit approval/activation. Check stale CAS, owner/session/Origin rejection and unchanged active snapshot before separate Use.
- Conversation usability and preservation: focused browser journeys and several actual desktop/narrow/keyboard interactions cover personal and scripted paths, ambiguity, correction, mismatch, unsupported effect, older evidence, unsaved native edits, interrupted/lost response and owner switching. Designer reviews actual rendered evidence and owner fixes findings before the next interaction iteration.
- Regression: npm test, npm run build and full npm run test:e2e on combined exact source, with failures investigated and protections retained. Reuse reliable unchanged technical evidence only where applicable.
- Live AI: manager-approved isolated fixture/ledger/cap; multiple bounded real-provider interaction runs after corrections record source, safe model/status/counters/reservations, exact result and limits, without secret output or a claim of second-provider compatibility unless actually qualified.
- Independent result review compares baseline, complete diff and exact combined evidence; fresh workflow, Redline, whitespace, privacy and generic-identity checks precede manager integration. No core semantic change: existing shared Java/Node artifacts are consumed unchanged.

**Plan review:** Pending clean-context architect review of the completed plan.

**Approvals:** Not required at this risk level. The task authorizes full implementation and multiple UX/AI iterations; no planning-only handoff or additional human gate.

**Exceptions:** —

**State:** Blocked
<!-- agent-workflow:end -->

## Implementation

Context established before code discovery. Isolated implementation and record branches start from the current canonical bases. Discovery confirms one demo delivery using existing core artifacts and lifecycle services. Full-delivery plan is ready for clean-context architect review; code remains blocked only on that plan gate and UI DTO settlement. Live-provider testing separately awaits the manager fixture policy.

## Evidence

Minimap resolved both exact source tuples and Pallium successfully attached both current-session participation references. No paid provider call or live preview/data change has occurred.

### Live qualification allocation policy

The manager owns actual original-data allocation, preview cutover and billable execution; workers make no paid calls. After offline checks, the manager confirms the existing preview is fully settled and normally closed for its authorized update, backs up the original database, verifies validated settings identity and the unchanged operator cap, and atomically reserves at most the smaller of $5 or the remaining allowance in the existing original ledger. Record a private allocation proof. Reservations remain nonrefundable; never restore a backup over held/spent credit or increase the operator cap. One isolated qualification database persists across all iterations, configured in memory at no more than that allocation, with existing reservation enforcement. Never reset/restore its ledger or create parallel fresh pools. Load credentials only through validated server-side settings; no key copies, argv, persistent environment edits, browser exposure or secret logs. Stop before paid calls if settings changed or available allowance is insufficient. The existing per-invocation qualification fixture creates a fresh database/allowance, so it cannot be used unchanged for these repeated live runs. Use a small task-local fixture rather than adding a production budget API or framework.

## Result review

Pending implementation and combined verification.
