# Daymark composed business capabilities: Stage 0

Identity: `agent-workflow:daymark-capability-proof`. Owning feature: [Compose new business capabilities on top of Daymark](../../roadmap/features/daymark-composed-business-capabilities.md). [Original draft](../../docs/internal/design-baseline/public/keir_daymark_team_process_feature_spec.md) §§4–11 supplies detail; the feature governs reconciled scope.

<!-- agent-workflow:start -->
**Outcome:**
Determine whether bounded Keir programs can compose new business functionality over one shared generic task catalog, without changing Keir Core.

**Target:**
Daymark isolated Stage 0 fixture. Demo base `79a4b885a2c657e5b17aaddf8fd32077f523fa53`; Core planning base `4b8aa0360262452ad76065b4d708488208e3e3bf`.

**Scope:**
One proposed task catalog, three hand-authored program definitions, and a runnable disposable-store proof for deliverable review and intake routing; update the Core work record and owning feature.

**Constraints:**
Stage 0 only. Use the pinned published compiler and Node runtime; no application, live data, schema, UI, AI provider, or Keir Core contract/runtime changes. Do not add a workflow engine, simulator framework, or production recovery system. Report a concrete expressibility or catalog gap rather than broadening the task.

**Completion criteria:**
Compile and invoke the three programs against identical model bytes and generic fixture adapters. Demonstrate linked review, early denial and later allowance, no cascade or unrelated effect, missing and uncertain effects with no retry, a version switch, and routing match/no-match. A deliberately wrong expected outcome must halt the proof. Preserve exact versions, model/source/artifact identities, command and outcomes; separate fixture evidence from native guarantees.

**Requirement baseline:**
{"source":"Owner authorized Stage 0 after reviewing the owning feature and the short plan","outcome":"Determine whether bounded Keir programs can compose new business functionality over one shared generic task catalog, without changing Keir Core.","scope":"One proposed task catalog, three hand-authored program definitions, and a runnable disposable-store proof for deliverable review and intake routing; update the Core work record and owning feature.","constraints":"Stage 0 only. Use the pinned published compiler and Node runtime; no application, live data, schema, UI, AI provider, or Keir Core contract/runtime changes. Do not add a workflow engine, simulator framework, or production recovery system. Report a concrete expressibility or catalog gap rather than broadening the task.","completion_criteria":"Compile and invoke the three programs against identical model bytes and generic fixture adapters. Demonstrate linked review, early denial and later allowance, no cascade or unrelated effect, missing and uncertain effects with no retry, a version switch, and routing match/no-match. A deliberately wrong expected outcome must halt the proof. Preserve exact versions, model/source/artifact identities, command and outcomes; separate fixture evidence from native guarantees."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:daymark-composed-business-capabilities"}],"pull_requests":[]}

**Risk:** Elevated
**Complexity:** Simple
**Reason:**
Intended Demo test/fixture paths are GRAY under shadow Redline. No live or product path is in scope; the proof still needs independent technical review for diagnostic value and authority boundaries.

**Discovery:**
Demo vendors the 0.5.0 compiler JAR and Node archive. Its provenance records the original qualification; the published 0.5.0 release lists matching compiler ZIP and Node archive digests. Existing compiler tests establish the CLI and model fingerprint pattern. The numeric model already exposes event time and a read-only decision hook pattern. The proposed model adds no Core execution semantics.

**Material assumptions:**
The three bounded programs can express the required typed optional arguments, guarded writes and read-only completion decision against the same proposed catalog. The real compiler/runtime and independent fixture assertions test that assumption; stop with exact diagnostics if a natural generic catalog adjustment cannot resolve a failure. Fake-store tests do not establish native transactions, current authorization, or production recovery.

**Plan:**
Add `tests/fixtures/team-process-proof.mjs`, `tests/fixtures/team-process-model.json` and `tests/fixtures/team-process-programs.json` only. Keep the Java-dependent experiment opt-in, outside the default test glob. Use existing pinned packages and exact model bytes; stamp the model fingerprint into the source definitions and compile the three sources. Bind the same fixture task operations in both examples: get, create linked child, summarize admitted children, and patch the triggering task. Treat the original creation admission as immutable provenance for policy-created children; a manual child does not satisfy the example. Expose that through a bounded generic read, not a review-specific host decision. Use task state and an immutable captured selection, not program calls or waits. Drive events and completions explicitly. The host consumes complete effect-free decisions and treats a missing required binding as blocked; a valid routing selection without a gate is not an unavailable gate. Fixture writes are bounded, scoped, and observable. No process names or title/tag/priority policy in adapters.

**Verification plan:**
Exact shared model and package identity → check package and artifact digests from the real compiler/runtime run.
Review, intake, version pinning and uncertain effects → run `node --test tests/fixtures/team-process-proof.mjs` and assert independent expected states and write attempts.
Wrong expectation → rerun with `DAYMARK_PROOF_WRONG_EXPECTATION=1` and verify the intended assertion fails.
Isolated scope → inspect the complete changed paths and distinguish fixture evidence from native guarantees.

**Plan review:**
Agent technical review: PLAN GO after checking the exact Demo worktree and its lock/provenance, measured JAR/archive digests, and the official 0.5.0 release digests. Fail closed on mismatched inputs. The reviewer accepts the shared model and fixture-only scope.

**Approvals:**
Not required at this risk level. Owner authorized starting Stage 0 only.

**Exceptions:**
—

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Plan accepted. The pinned compiler JAR is `1561dd1cce36c2eb3521442dcedbfd3a0ba4f1781b1e052a079ecdab32366c36`; the release compiler ZIP is `ec36271caaa55714257601c2f8bc7aa19f2ced76858f02b15021dacd3a138e76`; the release runtime archive is `9c59e4722f4d85bcd0e57580a1cef9526f7ceb52670e345576e39ca29d489736`. The latter two match the published 0.5.0 assets.

The shared model, three source definitions, and disposable-store test are written in the Demo proof checkout. The review example uses a generic child summary filtered by host-owned originating event rather than a tag. Intake has an intentionally absent completion gate; a missing required pinned gate blocks. The test cases assert fixture behavior, not native safety or live authoring.

## Evidence

Shell execution recovered without a setting change. Full offline installation lacked unrelated packages, including after omitting dev dependencies. Reused only the already-installed Keir runtime and four required `@bufbuild` packages by copying them into the isolated checkout, checking all five versions against the Demo lock. The test checks the compiler JAR, runtime archive, and the installed runtime files against the pinned digests. The live installation and saved data were untouched.

Run from the Demo checkout after ordinary dependency setup, with Java 21 on the path:

```sh
node --test tests/fixtures/team-process-proof.mjs
```

Node `24.14.1`, Java `21.0.12.1`, real compiler/runtime `0.5.0`: four checks pass, zero skips, about 2.5 seconds. All three programs compile with no diagnostics and prepare under the same binding. The fixture reports expected and observed outcomes. The negative run with process-local `DAYMARK_PROOF_WRONG_EXPECTATION=1` exits 1 at the intended assertion (`DENIED` observed, `COMPLETED` expected).

Canonical model SHA-256 `a267fd21e15a8bfa5cf709d700a4d149a6beaf1e1d78dab2a5260be0182c3b75`. Raw file digests can vary with worktree line endings; the canonical model and generated source/artifact digests below are stable.

| Program | Source SHA-256 | Artifact SHA-256 |
| --- | --- | --- |
| `request-review` | `accff3a008b92ab65bbd4767524d2429007f5e2b127377cc0107581c50efe578` | `14aa35dfa29495d1bbf82f13ffe6d8a8407eb4a1c019bb375854b941959c4d18` |
| `gate-deliverable-completion` | `916aa3497ecfb15a3596e06d93ceb8cf9b3aeebcd1a19ebbef2d697687cb70c1` | `c6f8a3984ad2a4778b4877639c94ae48a2390a05a1924318632a44ef88c16ba5` |
| `route-intake` | `1a4560b6afced2a26031abd1c1ca95b2565f7daf98639f6330ac42c9f535246a` | `489e3a3066b4c3af2a07bcbd16d2fc5a2fcf5f90614b45b73c82d1c5601f892d` |

The review gate consumes a bounded child-status result scoped to the originating event; a manual child cannot claim that provenance. A committed child with a lost reply can later provide actual linked state without rewriting the original uncertain receipt or retrying the create. This is fixture evidence only. Native identity, idempotency, revocation races, reconciliation, and commit-time checks remain Stage 1 work. No Stage 1 approval is implied.

## Result review

Agent technical review: RESULT GO after independent inspection of the three fixture files and both execution logs. No unresolved findings.
Reviewed revision: Demo [`14bb4e4`](https://github.com/rore/keir-demo-app/commit/14bb4e4f17c135537199b72d6dfb47d38711b0d2).
Verification adequacy: four passing checks, no skips, matching model and package identities, and the intentional negative run failed at the expected assertion. The experiment establishes expressibility with the published packages, not native guarantees or AI authoring.
