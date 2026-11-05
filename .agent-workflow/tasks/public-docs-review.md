# Public documentation review follow-up

<!-- agent-workflow:start -->
**Outcome:** A new reader can understand Keir, run the example, and find its public contracts and integration guidance.

**Target:** Core public documentation at `db0a4a3`.

**Scope:** Revise the README, teaching and embedding guides, compatibility reference, authoring entry points and Java verification notes. Reproduce and hand off the two runtime diagnostics separately.

**Constraints:** Keep the current contract, package archives, test fixtures and runtime implementations intact. Work from the reviewed revision in isolation.

**Completion criteria:** The README explains the host boundary and optional AI authoring. The runnable example explains its model. The version policy, host adapter mapping and authoring walkthrough match the current interfaces. Semantic references are available alongside the companion. The compile commands fail clearly and links resolve.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A new reader can understand Keir, run the example, and find its public contracts and integration guidance.","scope":"Revise the README, teaching and embedding guides, compatibility reference, authoring entry points and Java verification notes. Reproduce and hand off the two runtime diagnostics separately.","constraints":"Keep the current contract, package archives, test fixtures and runtime implementations intact. Work from the reviewed revision in isolation.","completion_criteria":"The README explains the host boundary and optional AI authoring. The runnable example explains its model. The version policy, host adapter mapping and authoring walkthrough match the current interfaces. Semantic references are available alongside the companion. The compile commands fail clearly and links resolve."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:public-package-release"}],"pull_requests":[]}

**Risk:** Routine

**Complexity:** Moderate

**Reason:** Multiple reader journeys change, with no change to an executable interface or release artifact. The Java runtime README is outside the documentation exemption.

**Discovery:** The published docs include the Keir 1 specs and a runnable example in both languages. The v3 archive packages its guide and schemas from a committed input revision. The public spec describes the compiler identities but omits the approved policy for changing them.

**Material assumptions:** The documentation follows the approved normalization decision and current public API. Any discrepancy with runtime behavior goes to the contract owners before a new normative promise is added.

**Plan:** Lead with the extension framework and one concrete use. Explain the readable model and existing example commands. Publish the approved version policy beside the specification index. Add semantic references beside the pinned companion, and provide a worked host service and authoring flow. Replace historical Java counts with the current evidence link. Keep runtime probes and their fixes separate.

**Verification plan:** Check links and anchors, the executable quickstart with isolated package inputs, the compiler failure behavior, adapter envelopes against the runtime reference and scoped diff privacy. Capture actual Node and Java bind diagnostics for the owners.

**Plan review:** Self-review against the pinned documentation review, published example and approved public contract.

**Approvals:** Not required at this risk level.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

Rewrote the public opening, explained the example model and version policy, and added a task service mapping and worked authoring flow. The companion setup points to immutable language and model references alongside its archive. Its archived guide and inputs are intact.

Refined the opening to lead with AI-assisted authoring and added a diagram that uses the follow-up example to show the handoff from the compiler to the app's services.

## Evidence

- Checked 98 repository and pinned links. Confirmed the archive input diff is empty.
- Installed the pinned Node package in isolation. Node and Java both matched the quickstart output for the pinned example and edited title. Removing the nominal annotation reproduced `NOMINAL_MISMATCH` at the project comparison. Git Bash returned a failure and skipped the Node command when compilation failed.
- Used the packaged public binding APIs to reproduce the two runtime discrepancies. Node accepts a model with a UTF-8 BOM and Java rejects it with `ADMISSION_INVALID`. Both accept 16 valid declared errors and reject 17. Node reports `ADMISSION_INVALID` and Java reports `BUDGET_EXCEEDED` for the latter, both at `/binding/dcm`. The contract owners received the reproductions and proposed shared boundary tests.
- Rendered the README SVG at its native size and inspected the flow, labels and contrast. The README image link resolves to a self-contained SVG with descriptive alternative text.
