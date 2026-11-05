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

**State:** Ready to implement
<!-- agent-workflow:end -->
