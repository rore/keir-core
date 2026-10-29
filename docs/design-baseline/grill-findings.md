# Keir design review — handoff brief

**Artifact under review:** `C:\Dev\rore\KEIR\draft design\` — an 8-document design package (v0.4) for **Keir**, a "Controlled Extension IR." Start at `README.md`; architecture spine is `public/keir-architecture.md`.

**What Keir is (one paragraph):** A closed, host-defined way for untrusted authors (including AI agents) to write SaaS extensions without writing real code. Authors compose a small YAML step language (`let`/`call`/`when`/`onError`/`return`) plus restricted CEL expressions. A single **Java compiler** (used as a CLI by Node hosts, or as a library by Java hosts) checks it against a host-curated **Domain Capability Model (DCM)** and emits a portable compiled artifact. A **runner in the host's own language** (Node first, Java later — one language per host, not mixed) executes that artifact against real or fake adapters. An optional AI layer lets business users author in plain English. The first demo is a Node task-manager POC. The docs are honest and already list their unproven gaps.

**Review method:** grill-my-context (adversarial, one question at a time). Findings below. Four are High, two are Minor. Each has a recommended fix and the author's (Rotem's) stance where given.

---

## High-weight findings

### F1 — Demo examples don't prove the thesis
The design's core claim: Keir is only worth its complexity when customers *connect different operations*, not just change a condition. But the two first-slice examples (invoice follow-up, recurring review) are the identical recipe — `tasks.get → tasks.search → tasks.create` — differing only in the CEL condition and date math. That's exactly the degenerate case the docs admit doesn't need Keir: a vendor hand-placing one hook-with-a-predicate per customization point.

**Baseline correction (important):** the alternative to compare against is a *different extension architecture* (vendor-prewired hooks, rules engines, low-code/workflow builders, function/webhook hooks), **not CEL**. CEL is a component *inside* Keir (its expression evaluator); the prewired-hook alternative uses CEL too. So the comparison is "compose from an approved catalog" vs "vendor wires each extension point" — never "Keir vs CEL."

**Fix:** use examples with genuinely different shapes — e.g. **pick-an-action** (create a retest task *or* tag the project), **chained dependent reads** (task → project → lead → create), **block-before-create** (pre-operation ALLOW/REJECT), **read-only view** (focus list). Keep invoice follow-up as the intro; drop recurring-review (redundant). Promote at least one shape-different example into the committed first slice.

### F2 — The strongest example breaks Keir's own one-change-per-run rule
The most convincing "different composition" demo — "if needs-retest, create a task; else tag the project" — uses two different write capabilities. Keir's rule is "at most one mutation per invocation," and its stated analysis counts every `call` node as a potential write (guards don't reduce the count). So the compiler would reject this unless it can *prove* the two branches are mutually exclusive — analysis the docs say v0 deliberately avoids.

**Fix:** either add mutually-exclusive-guard analysis so "one write across all paths" is provable (then the example is legal and the thesis is demonstrable), or stay strictly one-write for v0 and carry the differentiator with reads + result-consumers (chained-reads + block-before-create), noting pick-an-action as the v0.1 target.

### F4 — Compiler (Java) and runner (Node) can silently disagree
The compiler is always Java; the Node runner evaluates expressions with its own CEL library and **ignores the type/reference info the Java compiler worked out** (documented in the evidence file — CEL-ES extracts the AST and resolves functions from its own environment; its checker is incomplete). So even a pure-Node host — the first thing being built — has a "Java decides, Node runs" seam. They agree today only because 25 small test expressions happened to match. Risk: a customer approves behavior seen in the Node simulator; production (also Node, but the gap is compiler-vs-runner) does something subtly different on big integers, string/emoji length, a new operator, or after a library upgrade. Author agrees this must be made consistent.

**Fix:** make "feature behaves identically in Java compiler and the runner" a **release-blocking gate** per allowed expression feature, not a nice-to-have; keep the allowed expression set small until proven; pin/lock both CEL library versions and fail loudly on change; long-term, remove Node's freedom to re-interpret (lock the exact function set or emit something lower-level).

### F5 — No safe way to stop a runaway expression
Node is single-threaded; a heavy expression (nested list comprehensions) blocks the whole app, and the docs admit a timer can't interrupt synchronous JS and the CEL library has no step/gas limit. Since authors are untrusted, one bad extension can take down every tenant. The only safe lever today — allow only provably-cheap expression shapes — shrinks the language toward the "just a condition" case that undercuts the value (ties back to F1). Author agrees this likely needs a spike.

**Fix:** for v0, cap expression *shape* up front (limit list nesting/size, forbid list-inside-list ops) so every allowed expression is provably cheap; reject the rest. Run a **spike** to decide whether to cap CEL or adopt a more controllable expression engine *before* committing. If heavier expressions are needed later, run them where they can be killed (worker/sandbox) — a separate project, not a v0 checkbox. Be explicit this caps v0 expressiveness.

---

## Minor findings

### F6 — Testing flow is solid; two small tightenings
On re-read, the layered test model (compiler checks, cross-language expression conformance, runner-agreement, customer-reviewed scenarios, separate real-adapter integration tests) is genuinely strong and *already* flags the "agent repeats its own misunderstanding" risk, correctly parking "did it match intent" on human review (intent can't be unit-tested).

**Fix (refinements only):** (a) generate the human-facing behavior preview from the **compiled artifact**, not the AI chat summary, so it can't lie; (b) source at least one expected-outcome check from something other than the authoring agent.

### F7 — Fleet re-approval is the host's job, not Keir's
When a host changes a capability, all dependent extensions need recompile/re-approval; at scale that's thousands of nontechnical customers re-approving. Author correctly notes this is host-owned (Keir doesn't own activation/approval/rollout).

**Fix:** make the boundary explicit — **library judges compatibility, host owns rollout and re-approval** — and ensure the compatibility rules distinguish a harmless change from a behavior-changing one (right now nearly any change is treated as breaking).

---

## Reconciled verdict — is Keir worth doing?

Settled product proposition (this is the foundation for all value evaluation going forward):

> A SaaS vendor exposes generic hooks and approved capabilities **once**. Customers then compose their own extensions with **no customer-specific vendor code**. AI makes this accessible to business users. Keir makes the resulting programs constrained, validated and executable. **CEL is a component of Keir, not its competitor.**

Three-question split:

| Question | Assessment |
|---|---|
| Is safe, AI-authored application extensibility a real problem? | **Yes** |
| Is Keir's architecture technically feasible? | **Increasingly convincing** |
| Does Keir deliver enough value over other extension mechanisms? | **Not proven** — this is the gating question |

Position: for a **commercial** bet, answer question 3 before investing heavily. For the stated **public/exploratory pet project**, proceed with a thin implementation — technical uncertainty is largely burned down and a prototype is the most direct way to test the concept — **but redefine the next milestone around differentiation, not compiler-building.**

The real evaluation axes (none are about CEL):
1. **Expressiveness** — can customers build what they want by composing the catalog?
2. **Host cost** — what does exposing a useful catalog (hooks, capabilities, types, contracts) cost, and does it amortize across many extensions? *(This is the most consequential and least-examined risk — if publishing the catalog is as hard as just building the customizations, Keir loses.)*
3. **Alternatives** — do existing extension frameworks (rules engines, low-code/workflow builders, function/webhook hooks) deliver the same more economically?

Measurable success bar (replaces "we built a compiler and runtime"):

> N materially different customizations run on **one frozen catalog**; **new host-side code per additional customization trends to zero**; catalog authoring cost is paid once and amortizes; and this beats the vendor-prewired-hook baseline measured by **lines of new vendor code and number of new host operations per customization.**

Next milestone (differentiation-first):
1. Implement several realistic customizations conceptually in **both** Keir and the prewired-hook baseline, same capabilities/data/hooks — measure marginal host code each needs. (Paper exercise, no compiler required.)
2. Show **different** customers selecting **different** operations/dependencies from the same frozen catalog, ideally within one entrypoint (fixes F1).
3. Run the F5 resource spike with real CEL-ES, bounded inputs, nested/adversarial comprehensions, to learn what expression power safely survives.

Keep **F4** (compiler/runner consistency) and **F5** (resource spike) as live gates running *alongside* the thin build, not waved through.

## Status / what's still open
- No findings have been written into the design docs themselves. A reviewing agent should **not** assume these are in the artifact.
- Author decisions captured during the session: both Java and Node runners are genuinely wanted (one language per host, not mixed); F4 consistency accepted; F5 spike accepted; F7 scoped to host; product proposition and the "CEL is a component, not a competitor" framing are settled.
- Not-yet-grilled target worth a follow-up: production observability for the runner (what tells you an extension misbehaves in prod). The DCM authoring-cost risk is now promoted into evaluation axis 2 above.
