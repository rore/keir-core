# Read/2 reusable simulation

Canonical task: `agent-workflow:read2-reusable-simulation`. Owning feature: [reusable-simulation](../../roadmap/features/reusable-simulation.md). This bounded increment starts with task advice; broader host-conformance and effect tooling remain on the feature.

<!-- agent-workflow:start -->
**Outcome:** Authoring tools can run a bounded read/2 extension against data-only simulated responses and compare actual behavior with independent expectations, using the same artifact and native runtime in Java and Node.
**Target:** keir-core
**Scope:** A read/2 scenario/report contract and schemas, domain-independent Java and Node simulation tooling, executable task-advice examples and shared conformance tests, compact product-agent documentation and this Work Record; manager owns roadmap updates.
**Constraints:** Preserve existing compiler/runtime semantics, profiles, artifacts and goldens. Use the native runtime rather than another interpreter. Scenarios cannot supply host authority or live invokers. Bound input, execution and reports; redact sensitive values. No live service calls or effects, Studio UI, AI integration, real-effect admission, global tooling changes, live preview/data changes or public package publication.
**Completion criteria:** One compiled task-advice program runs unchanged in Java and Node against open, completed, missing and denied read scenarios; expected success and deliberate mismatches produce bounded structured reports. Missing, extra and out-of-order stubs, malformed or authority-bearing scenarios and consumer failures are handled explicitly without live dispatch. Shared regression proves parity and preserved native validation/authorization boundaries; the runnable example and fast verification are documented.
**Requirement baseline:**
{"source":"https://github.com/rore/keir-core/blob/80ac339/roadmap/features/reusable-simulation.md","outcome":"Authoring tools can run a bounded read/2 extension against data-only simulated responses and compare actual behavior with independent expectations, using the same artifact and native runtime in Java and Node.","scope":"A read/2 scenario/report contract and schemas, domain-independent Java and Node simulation tooling, executable task-advice examples and shared conformance tests, compact product-agent documentation and this Work Record; manager owns roadmap updates.","constraints":"Preserve existing compiler/runtime semantics, profiles, artifacts and goldens. Use the native runtime rather than another interpreter. Scenarios cannot supply host authority or live invokers. Bound input, execution and reports; redact sensitive values. No live service calls or effects, Studio UI, AI integration, real-effect admission, global tooling changes, live preview/data changes or public package publication.","completion_criteria":"One compiled task-advice program runs unchanged in Java and Node against open, completed, missing and denied read scenarios; expected success and deliberate mismatches produce bounded structured reports. Missing, extra and out-of-order stubs, malformed or authority-bearing scenarios and consumer failures are handled explicitly without live dispatch. Shared regression proves parity and preserved native validation/authorization boundaries; the runnable example and fast verification are documented."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:reusable-simulation"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** New scenario/tooling/schema/test paths have a GRAY floor. Two native runtimes and trust-sensitive simulated response/report boundaries require an independently reviewed contract; existing execution semantics and authority remain unchanged.
**Discovery:** Pending architect inspection of current simulation harnesses, runtime APIs, maintained contracts and historical inputs.
**Material assumptions:** Existing native read/2 simulation seams can support reusable data-only scenarios without changing admitted semantics or trusting authored authority; if disproved, return affected scope to planning and report the smallest alternative.
**Plan:** Contract discovery first; resolve scenario input, host-owned fixture binding, strict stub matching, consumer expectations, resource limits and redacted reports. Record concrete design and implementation plan before source edits. Independent clean-context technical review precedes implementation. Use one accountable implementation owner, with bounded inherited-record helpers only for coupled runtime work; independently owned tasks require separate records. Preserve existing regressions, then independently review exact results, integrate on main, verify CI and recoverably clean task-created worktrees.
**Verification plan:** Shared positive and negative scenario fixtures through the actual Java and Node runtimes using identical artifact bytes; explicit stub/authority/consumer/report boundary tests, deterministic mismatch output and fast regression timing. Check scope, privacy, commit identity, complete diff-based Redline/Work Record compliance and independent result-review adequacy. Reuse reliable unchanged-artifact evidence.
**Plan review:** Pending contract discovery and independent clean-context technical review. Implementation is not authorized until these gates are recorded.
**Approvals:** Not required at declared Elevated risk. User approved this bounded slice with "Ok, go". A material requirement or native authority change returns to planning.
**Exceptions:** —
**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

- Context and immutable requirement baseline recorded before source discovery at main `80ac339`. Current phase: planning; no implementation changes. Manager owns this record until the contract and developer handoff are approved.
