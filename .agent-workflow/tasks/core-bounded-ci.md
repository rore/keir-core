# Bounded resource qualification in CI

Task identity: `agent-workflow:core-bounded-ci`. Branch `chore/core-bounded-ci` starts at d484c46. Relay discovery is unavailable; no role is assumed. Manager owns reconciliation into `roadmap/features/runtime-resource-qualification.md`.

<!-- agent-workflow:start -->
**Outcome:** Run the two bounded Core resource qualifications in the existing contract CI job and merge the validated integration.
**Target:** keir-core .github/workflows/keir-contract.yml and resource qualification documentation.
**Scope:** Add default and concurrent qualification steps after existing package/conformance checks, document their CI execution and measure hosted cost without changing production or harness behavior.
**Constraints:** Reuse the existing job, tools and dependencies; retain shallow checkout and omit the optional historical baseline; keep longer load/soak tests opt-in. Preserve resource bounds, failure status, current checks and unrelated work. No deployment, release, credentials/security changes, force push or model API spending.
**Completion criteria:** Both qualification commands and existing checks pass on the exact final head; independent review approves the change; report directly observed added phase time and total job variation versus prior CI, without claiming a flakiness rate from one run; merge safely and preserve compact evidence.
**Requirement baseline:** {"source":"work-record-initial","outcome":"Run the two bounded Core resource qualifications in the existing contract CI job and merge the validated integration.","scope":"Add default and concurrent qualification steps after existing package/conformance checks, document their CI execution and measure hosted cost without changing production or harness behavior.","constraints":"Reuse the existing job, tools and dependencies; retain shallow checkout and omit the optional historical baseline; keep longer load/soak tests opt-in. Preserve resource bounds, failure status, current checks and unrelated work. No deployment, release, credentials/security changes, force push or model API spending.","completion_criteria":"Both qualification commands and existing checks pass on the exact final head; independent review approves the change; report directly observed added phase time and total job variation versus prior CI, without claiming a flakiness rate from one run; merge safely and preserve compact evidence."}
**Relationships:** {"schema_version":1,"sources":null,"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** Workflow and conformance paths are unclassified gray. CI uses existing bounded commands, but hosted timing and failure propagation need review.
**Discovery:** Current main d484c46 contains reviewed default/concurrent qualification, 45-second child watchdogs and 1MiB capture. Concurrent total budget135seconds; job ceiling10minutes. Prior contract run42865710009/job130003354619 passed; log activity spanned114.553seconds, while previously quoted rounded workflow duration was about118seconds. Both local phases previously took about19seconds with optional baseline enabled.
**Material assumptions:** Existing Java coverage verification packages compiler/host JARs and Node installation supplies runtime dependencies; hosted CI disproves this if a prerequisite is missing. No baseline env/ref is set in CI, so shallow history is sufficient. Stop and fix any resource/failure regression without widening limits.
**Plan:** Use two named Bash steps after scenario comparison, running existing default and --concurrent entry points with pipefail-preserving tee into ignored build reports and CI logs. Do not add runners, dependencies or full-history fetch. Update conformance README. Reuse previously approved harness/source review; independently review this wiring, record exact-head hosted timings, inspect checks and native review threads, then expected-head merge. Manager owns roadmap reconciliation.
**Verification plan:** - Command prerequisites and failure propagation -> YAML/static diff inspection and Bash pipefail failure probe.
- Current behavior/resources -> both hosted commands plus existing full contract and governance CI on exact final head.
- Cost and flakiness indicators -> report JSON phase durations, watchdog/memory observations, all validated counts/negative replays and hosted log span compared with prior job; state limited sample size.
- Delivery -> independent plan/result review, fresh Redline/Work Record checks, native review/thread checks and expected-head merge.
**Plan review:** Agent technical review: core_hardening_review. Existing command/harness result approval from PR12 is reused; focused CI-only plan approved at0fd5a9f before implementation.
**Approvals:** User explicitly approved the presented bounded CI integration with “Ok”; commits and validated merges are already authorized. This satisfies the previously pending CI-change permission.
**Exceptions:** —
**State:** Ready for review
<!-- agent-workflow:end -->

## Plan review
Prior command implementations and failure paths were independently approved in PR12. This contribution adds only CI execution and documentation; a focused independent wiring review is requested before edits. The user approved the previously prepared proposal; omit its historical baseline/full-history portion as directed.


## Implementation and local verification
The independent plan reviewer approved0fd5a9f before edits. Added only two named Bash steps after existing conformance, running default and --concurrent with tee reports. No baseline variable, fetch-depth change, new job/dependency, widened resource allowance or load/soak command is added. README explains the bounded CI subset and logs.

YAML parsed successfully with the existing PyYAML installation. Structural comparison proves the prior configuration is identical after removing the two added steps. An isolated Bash invocation using the explicit Actions shell flags (-e -o pipefail) ran a Node command emitting a JSON failure and exit7 through tee: exit7 and exact report output were both preserved. Diff whitespace checks passed. Existing harness code is unchanged from independently reviewed PR12, so no redundant local dependency installation or full build was performed. Exact-head hosted command execution and result review remain required before merge.

Role discovery remains unavailable; no Relay role or private identity was assumed. Current branch is chore/core-bounded-ci; this implementation starts from planning commit0fd5a9f on maind484c46. Manager owns roadmap reconciliation; this contribution closes only CI execution of the bounded subset.

## Result review
Agent technical review: core_hardening_review.
Reviewed revision: staged implementation above0fd5a9f; workflow SHA2562eb85267d51e4fb4264165e1c3ee6b5f04bb335dc4cd57fe27c9795e64e48918.
Verification adequacy: approved with no blocking findings after independent source inspection. Exactly two existing commands run after conformance; pipeline failure is preserved and no prior checks, harnesses, dependencies, history settings or limits change. Structural comparison, exit7 pipeline probe and whitespace checks are adequate for local wiring. Exact-final hosted command execution, timings and native merge checks remain required.
