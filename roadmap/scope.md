# Roadmap

This is the single roadmap for `keir-core` and `keir-demo-app`, organized as milestones containing feature cards. Each milestone describes an observable capability; each feature owns its outcome, repository, status and completion criteria. The manager owns direction, sequence, assignments and cross-repository acceptance. Implementation and tests stay in the repository whose code changes.

## Milestones

1. **Execute extensions in Java and Node.** First run a read-and-decide request through one Java-compiled artifact in both sample hosts. Then add one guarded follow-up effect, same-artifact simulation and honest failure/uncertainty receipts. Both demonstrations use controlled adapters and visible traces; they do not certify a real host.
2. **Customize a real task application.** Grow a native task app, connect the packaged read-only and follow-up paths to real adapters, and add an authoritative pre-create guard. Enrich the example with bounded tags/search/due dates as the language supports them. Demonstrate the host integration assistant proposing a model/capability change that a host owner reviews and publishes.
3. **Author and activate customizations through AI.** A customer describes a supported customization, sees compiler feedback and simulation/host-outcome traces, reviews expected behavior and approves the exact revision. Trigger that behavior in the real app, revise it, disable it and roll back selection for new invocations.

Each milestone adds a user-observable scenario to the previous working path. Keep earlier scenarios runnable as regression demonstrations. Do not equate initial samples, mock results or document review with production readiness.

## First two runnable features

| Feature | Visible demonstration | Work that makes it complete |
| --- | --- | --- |
| [Read and decide](features/portable-core-first-slice.md) | Inspect a task through one approved read, compute a classification and display the same consumed result in Java and Node. Change the source condition without changing host logic. | Narrow public language/DCM/artifact/runtime contracts; strict compile/admission; actual restricted CEL; one portable artifact; typed adapter/result traces; negative conformance and measured POC bounds. |
| [One guarded follow-up](features/single-effect-followup.md) | An admitted TaskCompleted event reads actual task fields and either creates one follow-up or reports no-op. Inject rejection or a lost response and display honest effects on both runtimes. | when/terminal business errors; one possible write; protected event/origin/key forwarding; same-artifact simulation and scenario protocol; effect receipts, budget evidence and shared regression fixtures. |

The first scenario is an advisory request/result path; it does not yet implement a native transaction guard. The second intentionally uses scalar task fields. The full baseline invoice example with search, tags and 168 elapsed hours is [a later enhancement](features/enrich-followup-expressions.md), not an implicit prerequisite to seeing the first effect.

Resolve the contracts needed for the current feature, implement them through all layers and run the demonstration. Specification and CEL-bound cards are explicit supporting work, not sequential whole-system design/research phases. A command-line host harness is enough for the first two demonstrations. A complete task application and live agent orchestration are not prerequisites.

## Team execution and dependencies

The architect has [reviewed the complete supplied design package](../docs/internal/development-plan-review.md) and maps its gaps to the runnable sequence. Before each implementation assignment, resolve that feature's blocking type, authority, artifact, outcome and resource contracts; publish the supported specification and examples. Revisit them when execution exposes a gap.

- **Architect:** mature the current contracts, document decisions and review semantics/evidence. Broader proposals stay explicitly deferred.
- **Two core developers:** keep compiler and both runtimes moving through the same narrow scenario, with shared conformance and embedding samples. Divide independent work against agreed contracts and reserved files/checkouts. When boundaries are unsettled, use focused verification alongside implementation or sequence the dependent tasks; no permanent Java/Node split is implied.
- **Demo developer, later:** build the actual task application once useful packaged core behavior is available to integrate. No native demo implementation is assigned now. Begin with the agreed minimal native path and real adapters at that point; the complete language need not be finished.
- **Manager:** reserve checkouts, sequence dependencies, verify combined acceptance and update the owning feature. This plan does not itself start implementation or grant new environment permissions.

The initial implementation uses the pinned repository-local Maven wrapper, JDK 21 and a native ESM Node 24 package. Keep project tooling/caches isolated from everyday installations. Local built packages with recorded version/digest support integration before registry publication; hosts must not import sibling source as their normal dependency model.

The initial sample uses a trusted compiler-output path and a host-held expected digest outside the mutable artifact. Self-hashing is not authentication. Real activation needs a separately specified approved-storage/signature policy; pending simulation guarantees can never authorize real adapters.

## How we manage work

The value of composing approved capabilities at host-defined hooks is an accepted product premise based on the user's prior product experience. A frozen-catalog exercise, comparative extensibility benchmark or separate proof of product value is not a prerequisite for implementation. Evolve the catalog with real implementation needs while verifying its technical contracts and compatibility.

Minimap opens grouped by each feature's `milestone` metadata; `roadmap.config.json` defines milestone order. `board.md` keeps one shared feature ordering under a neutral Features heading. It does not duplicate milestone membership. Use the repo and status filters to inspect where work belongs and what is active. Feature order expresses priority within each milestone.

Architectural review, implementation assignments and verification happen within the owning feature. Review the applicable baseline design for gaps before substantial implementation, maintain the agreed contract, and record delivered evidence and remaining scope in that same feature. Break a large feature down when needed for execution; do not introduce another roadmap level for slices or a separate release-version plan.

Specifications and agent-facing contracts are explicit deliverables. Review the affected draft, write the maintained contract and conformance examples, implement it, and reconcile findings into the contract. Public specifications live under `docs/specs/`; both product agent guides live under `docs/agents/` and begin with the first language features. Runnable host and customer agent integrations arrive later. Internal designs, decisions and setup records live under `docs/internal/`.

The host integration assistant proposes DCM/hooks/capability/adapter definitions and evidence for a host developer's review; it cannot certify or publish its own proposal. The customer extension assistant discovers only the approved scoped catalog and proposes programs/scenarios; it cannot widen capabilities or approve activation. Both reference the same public contracts. Live agent/provider choices and spend bounds are made before those integrations, not during the first core harness.

Use both core developers for the initial core work. Schedule the dedicated demo developer when the core offers a useful integration target; after that, independent demo and core enhancements may proceed together. Feature completion must still satisfy its declared dependencies and combined acceptance criteria.

Agent Workflow is advisory and opt-in in core, with advisory CI on main pushes and PRs. The demo needs no second roadmap or Work Record process. Both GitHub repositories are initialized privately; license and public release remain open. Product contract CI is approved for installation, with no new merge gate.

The [internal design baseline](../docs/internal/design-baseline/README.md) contains the draft inputs. Current decisions cover [repository boundaries](../docs/internal/decisions/0001-repositories-and-runtimes.md), [product direction](../docs/internal/decisions/0002-product-proposition-and-evaluation.md) and [restricted CEL](../docs/internal/decisions/0003-restricted-cel-resource-policy.md). Public specifications, draft design approval and implementation evidence remain distinct.

## Beyond the initial demonstrations

V0.7 multi-effect execution, tenant capability overlays/endpoints, richer patch/projection consumers and multi-binding composition remain planned design inputs to prioritize as concrete needs arise. Each expansion needs its own visible scenario and contract review; none is silently admitted by the initial one-write profile. Durable scheduling, automatic Keir replay and distributed transactions remain outside the selected initial runtime model. Production resource/host guarantees stay unclaimed until their actual implementation and evidence exist.
