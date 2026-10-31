# Adopt Agent Workflow while retaining direct-main delivery

<!-- agent-workflow:start -->
**Outcome:** Agent Workflow checkpoints and a task-specific core-owned Work Record are standard for code and mixed changes; documentation-only and roadmap-only work is exempt, with direct-main delivery.
**Target:** Keir engineering workflow across core and demo.
**Scope:** Core workflow and Redline applicability policy, concise contributor/agent guidance, Work Record navigation and a current process note; demo guidance points to core-owned records and documentation exemptions.
**Constraints:** Keep direct-main delivery, existing advisory CI and Redline shadow mode; no hooks, protection, CI changes, global tooling changes, second demo workflow installation or fabricated historical records.
**Completion criteria:** Code and mixed changes require a core-owned record; approved documentation-only and roadmap-only paths are exempt; direct-main tasks use explicit task identities; both repos agree; core and demo command paths pass focused positive/negative checks; plan/result technical reviews are recorded.
**Requirement baseline:**
{"source":"user-request","outcome":"Agent Workflow checkpoints and a task-specific Work Record are standard for all Keir repository change work, without requiring PRs.","scope":"Core workflow config, concise contributor/agent guidance, Work Record navigation and a current process note; demo guidance points to core-owned records.","constraints":"Keep direct-main delivery, existing advisory CI and Redline shadow mode; no hooks, protection, CI changes, global tooling changes, second demo workflow installation or fabricated historical records.","completion_criteria":"All change work, including docs and roadmap, requires a core-owned record; direct-main tasks use explicit task identities; both repos agree; config and current record pass focused checks; plan/result technical reviews are recorded."}
**Relationships:**
{"schema_version":1,"sources":[],"pull_requests":[]}
**Behavior changes:**
[{"target":"task-context.outcome","classification":"requirement-change","before":"Agent Workflow checkpoints and a task-specific Work Record are standard for all Keir repository change work, without requiring PRs.","after":"Agent Workflow checkpoints and a task-specific core-owned Work Record are standard for code and mixed changes; documentation-only and roadmap-only work is exempt, with direct-main delivery.","reason":"User explicitly corrected the initial interpretation to preserve documentation and roadmap exemptions.","impact":"Documentation-only and roadmap-only changes skip the record process; code, mixed and governance changes retain it.","alternatives":"Requiring records for every edit was rejected by the user.","authority":{"scope":"task","name":"task-owner"},"approval":{"by":"user","reference":"User scope correction in this task","verbatim":"documentation and roadmap should not need this - this is why we have the option to define what to skip in agent workflow"}},{"target":"task-context.scope","classification":"requirement-change","before":"Core workflow config, concise contributor/agent guidance, Work Record navigation and a current process note; demo guidance points to core-owned records.","after":"Core workflow and Redline applicability policy, concise contributor/agent guidance, Work Record navigation and a current process note; demo guidance points to core-owned records and documentation exemptions.","reason":"User explicitly corrected the initial interpretation to preserve documentation and roadmap exemptions.","impact":"Documentation-only and roadmap-only changes skip the record process; code, mixed and governance changes retain it.","alternatives":"Requiring records for every edit was rejected by the user.","authority":{"scope":"task","name":"task-owner"},"approval":{"by":"user","reference":"User scope correction in this task","verbatim":"documentation and roadmap should not need this - this is why we have the option to define what to skip in agent workflow"}},{"target":"task-context.completion_criteria","classification":"requirement-change","before":"All change work, including docs and roadmap, requires a core-owned record; direct-main tasks use explicit task identities; both repos agree; config and current record pass focused checks; plan/result technical reviews are recorded.","after":"Code and mixed changes require a core-owned record; approved documentation-only and roadmap-only paths are exempt; direct-main tasks use explicit task identities; both repos agree; core and demo command paths pass focused positive/negative checks; plan/result technical reviews are recorded.","reason":"User explicitly corrected the initial interpretation to preserve documentation and roadmap exemptions.","impact":"Documentation-only and roadmap-only changes skip the record process; code, mixed and governance changes retain it.","alternatives":"Requiring records for every edit was rejected by the user.","authority":{"scope":"task","name":"task-owner"},"approval":{"by":"user","reference":"User scope correction in this task","verbatim":"documentation and roadmap should not need this - this is why we have the option to define what to skip in agent workflow"}}]
**Risk:** Elevated
**Complexity:** Large
**Reason:** Workflow policy is a Redline red path; the change adjusts contributor behavior in two repositories. No product, security or persistence behavior changes.
**Discovery:** Current core policy makes records and Redline optional and exempts roadmap/navigation changes; demo guidance excludes workflow. Installed operating mode supports explicit Work Record identities and changed-file record discovery, so no checker or CI modification is needed for main delivery.
**Material assumptions:** The existing checker accepts explicit task references on main and changed-record discovery; verify with the resolver/checker before adopting. If unsupported, stop the affected policy change and report the gap rather than inventing a new harness. Demo records can stay in core with demo revision/test evidence; no demo installation is necessary.
**Plan:** Require records and fresh Redline verdicts for code/mixed/governance changes. Configure docs/, roadmap/, README.md and CONTRIBUTING.md as whole-change exemptions, including design/spec text per the user instruction (no runtime/test obligation waiver); mirror these in Redline blue paths and remove the conflicting red documentation-decision path while retaining both policy files as red. Preserve shadow mode and existing CI. Update core AGENTS.md, CONTRIBUTING.md, roles/shared.md, roles/demo-developer.md, task README, docs/testing.md and internal navigation; add a concise current process note. Update demo AGENTS.md, CONTRIBUTING.md and README.md to share core-owned records. Resolve explicit agent-workflow:adopt-agent-workflow-main from core; checker validation uses --slug adopt-agent-workflow-main. For demo work, collect its complete changed paths/diff in demo, run the shared core Redline reporter/policy from demo, then validate the core record with the core-root checker, explicit slug and that demo verdict/path evidence. Record both repo revisions/tests. No demo config, CI, hook or vendor changes. Keep the existing directDefaultBranchAllowed=false automatic-eligibility setting: branch metadata was readable but ruleset proof is unavailable. The user separately authorizes direct-main delivery; do not claim automated protection clearance or change remote protection. Stop if the installed tools cannot support this without a new harness.
**Verification plan:**
- Approved docs/roadmap-only changes are exempt -> both policy schemas plus reporter/checker probes for ordinary docs, docs/specs, roadmap and root navigation, and accurate distinction between documentation exemption and unavailable automatic direct-main protection clearance.
- Code and mixed changes require records -> missing-record negative probes for code, docs-plus-code and agent instructions; validate the actual changed-path set with a fresh Redline report.
- Core-owned demo task identity works -> core-root resolver with explicit reference, core-root checker with explicit slug and freshly collected demo path/report evidence; no second config or implicit main record.
- Guidance agrees across repositories -> focused text/link/privacy review of both diffs; confirm no CI/hooks/protection changes.
- Reviews and evidence are preserved -> independent plan and result review; inspect this record and final revisions.
**Plan review:** Agent technical review: independent plan review documented under Plan review in this record; approved after the user scope correction and explicit demo verification path.
**Approvals:** User scope correction: "documentation and roadmap should not need this - this is why we have the option to define what to skip in agent workflow". User authorized the process change and direct-main delivery: "also, can we start using agent workflow process for all our work? i don't mean opening prs - let's skip the pr process for now and work on main, but still keeping the process and work record". No additional human approval required at Elevated risk.
**Exceptions:** —
**State:** Ready to implement
<!-- agent-workflow:end -->

## Implementation

- Initial inspection preceded record creation under the existing optional policy. This record starts before policy/guidance edits; no earlier checkpoint sequence is claimed.
- Redline classification: RED because `agent-workflow.yaml` changes; architecture-review scope is explicitly authorized by the user. No PR label is invented; shadow reporting remains advisory.
- Initial independent plan review requested an explicit, tested demo command path. The revised plan provides it; the user also corrected applicability before implementation. No policy edits have begun. Independent revised-plan review is approved; implementation may start.

## Evidence

Pending implementation and focused verification.

## Result review

Pending independent technical review.

## Scope correction

The user rejected the initial all-edits interpretation before implementation. The original baseline remains intact; approved task-context changes above preserve this history. Documentation exemptions use configured paths, with governance/instruction paths remaining protected.

## Discovery update

Classic branch metadata reports no protection, but the applicable-ruleset query is unavailable. Preserve the existing automatic direct-default eligibility flag rather than claiming proof. The explicit user instruction to work on main remains the delivery authority; remote rules are never disabled or bypassed. This does not prevent the documentation-only Work Record exemption.

Skill feedback trigger 2 dropped: the user corrected my interpretation of scope; this is not an upstream defect.

## Plan review

Agent technical review: independent clean-context plan reviewer, 2026-10-31.
Reviewed revision: core 08da4199195d297b9136984ca8efdabbc4645a13 and demo d6a71a4655cc0fb503ce50566c0726cf4419e5b0, plus this task's pre-edit plan.
Verification adequacy: approved after adding an explicit core-root resolver/checker path for demo work and positive documentation/specification exemption probes. The user-specific documentation exemption overrides the skill's general preference to track normative text; behavior approval and test obligations remain intact. The reviewer inspected the installed checker; execution evidence will be collected during verification.
