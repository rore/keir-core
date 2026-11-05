# Expose task titles to Daymark customizations

<!-- agent-workflow:start -->
**Outcome:** A Daymark rule can read the completed task's title and use it for exact matching or as the title of one new task.

**Target:** keir-demo-app

**Scope:** Publish a new task model and read result, preserve execution of prior pinned selections, and align simulation, Studio authoring and native verification. Integrate the suffix once the reviewed Keir package supports it.

**Constraints:** Preserve old model and package identities, selected rules, queued admissions, historic evidence and current owner authority. Keep existing task creation, replay keys, approval controls and data intact.

**Completion criteria:** An old active rule still runs. A new rule copies or compares a title in simulation and native execution with the same artifact. After the Keir expression update, Studio checks and runs a title plus suffix rule.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A Daymark rule can read the completed task's title and use it for exact matching or as the title of one new task.","scope":"Publish a new task model and read result, preserve execution of prior pinned selections, and align simulation, Studio authoring and native verification. Integrate the suffix once the reviewed Keir package supports it.","constraints":"Preserve old model and package identities, selected rules, queued admissions, historic evidence and current owner authority. Keep existing task creation, replay keys, approval controls and data intact.","completion_criteria":"An old active rule still runs. A new rule copies or compares a title in simulation and native execution with the same artifact. After the Keir expression update, Studio checks and runs a title plus suffix rule."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:rich-task-customizations"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Versioned host compatibility affects active rules and queued events. New and old result shapes need explicit native and simulation checks.

**Discovery:** The existing read result has a closed three-field shape. Native tasks store the title, while completion events identify the task. Title reads observe the saved value at dispatch.

**Material assumptions:** Two known exact model bindings can coexist without rewriting rows or weakening pin validation. Existing record and runtime bounds can report title overflow before writing.

**Plan:** Add title in a new exact task model and version the read operation. Retain the old model and implementation. Verify title copying and matching with real and simulated reads, keeping task creation unchanged. Qualify the later runtime package against existing pins before enabling suffix authoring.

**Verification plan:** Title rules → same-artifact synthetic and native title copy, exact match and no-op. Preservation → old active selection, queued admission, retained proof and owner fence. Bounds → native title boundaries and UTF-8 overflow. Suffix delivery → live Studio authoring, approval, activation and exact created title.

**Plan review:** Agent technical review: independent architecture review of the existing checked expression and task model paths accepted this split. The implementer checks exact paths, risk and compatibility before editing and returns material deviations for review.

**Approvals:** Not required at this risk level.

**State:** Ready for review
<!-- agent-workflow:end -->

## Title read candidate

Candidate `45ee19fb37d6f45f6636a1117997c3fa7c748954` adds the current title model alongside the exact previous model and its live read binding. The candidate exercises old active and queued rules, scoped reads, exact title copying and comparison, and overflow before any write. The manager verified both model hashes and the retained test logs. The initial broad run had one stale fixture expectation about the number of bindings. That case passed after correction, alongside 12 separate process cases. The focused browser run passed all 16 cases, and the final guidance suite passed all 53.

Independent review passed the title model, live binding, lifecycle and earlier-discussion recovery at the phase revision. The review also verified the revised wording for an oversized read. The message describes subsequent writes being skipped and preserves any earlier confirmed effect.

The title phase uses the prior Keir packages. The manager released the verified compiler and Node packages from the [integrated Keir revision](https://github.com/rore/keir-core/commit/9be8eb4af5694e14ab3efdc84c111c9449c0e68f) for isolated suffix integration. Verify a finite compatibility mapping for the exact prior model and package pins while qualifying the new package under its own provenance. Before deployment, verify that prior approvals and pending events still resolve and execute, that mismatched pins fail, and that a new approved rule creates the exact suffixed title in the live journey.

## Combined candidate

Candidate `a1551756618621fe76297515894203a570a5cf07` consumes the reviewed Keir packages and adds checked title suffixes. The captured old-package database exercised an existing queued completion and a new completion without rewriting the prior approval, proof or pins. Mixed or unknown identity pairs stayed held. A scripted assistant produced the suffix rule, kept its independent expectations, and after approval and activation the native app created the exact Unicode title. Runtime overflow and native title limits both prevented a write.

The manager verified the package and evidence hashes. The frozen run passed 346 default tests with 13 default skips, 12 separate process cases, 17 focused tests and six focused browser cases. Build and typecheck passed.

## Result review

Agent technical review: Independent review passed the title and suffix candidates, including installed package bytes, old and new model pins, authorization and retained approval paths.

Reviewed revision: [a155175](https://github.com/rore/keir-demo-app/commit/a1551756618621fe76297515894203a570a5cf07).

Verification adequacy: The live assistant authored the rule in one user turn without repair. Its saved proof covered two different matching titles and one nonmatch. After separate approval and activation, one matching native completion created the exact suffixed title with a confirmed receipt. Rechecking created no duplicate, and a different-project completion created nothing. The test rule was turned off. The live journey used one destination project, and the controlled suites cover the error and overflow cases.

The reviewed revision is published and serves the existing demo. The rehearsal and post-update comparison verified all 19 database tables, settings and exact served assets.

The [initial hosted run](https://github.com/rore/keir-demo-app/actions/runs/42001166087) passed build and 347 unit tests with 12 default skips. It passed 168 of 169 browser tests. The terminal deletion recovery test rendered the confirmation heading but timed out waiting for keyboard focus.

The [focus fix](https://github.com/rore/keir-demo-app/commit/b0e5e4294089c040f74dd0f00a6c1554092bbc63) moves focus after React commits the terminal heading. A diagnostic run reproduced the failure by moving the old callback before commit. The same diagnostic passed on the fix, as did all 12 deletion and nine nearby recovery tests. Independent review passed, and the rebuilt assets matched the reviewed build. The [hosted rerun](https://github.com/rore/keir-demo-app/actions/runs/42003899534) passed 347 unit tests with 12 expected skips and all 169 browser tests.

The title increment is delivered. The manager verified the restore-tested archive and cleanup supplement. Automatic approval review blocked removal of the isolated checkout and restart of the local preview, so both operations await direct authorization. The preview serves the verified title release.
