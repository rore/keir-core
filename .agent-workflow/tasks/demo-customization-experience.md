# Task-app identity and customization experience

Canonical task: `agent-workflow:demo-customization-experience`. Owning feature: [Extension Studio design](../../roadmap/features/extension-studio-design.md).

<!-- agent-workflow:start -->
**Outcome:** The demo behaves and reads as a coherent task application with its own identity and a general customization workspace, with usable scrolling and a related but distinct Studio.
**Target:** keir-demo-app
**Scope:** The demo host-app branding, navigation and customization entry/management flow; native and Studio layout, styling and interaction; affected browser regressions and concise design/running documentation. Core owns this record and roadmap.
**Constraints:** Preserve native task CRUD, user/session isolation, exact simulation/evidence/approval/activation/disable/restore semantics, resource limits and unsaved edits. Present implemented read-only examples honestly; do not imply live AI, arbitrary authoring, multiple active extensions or write effects. No core runtime, persistence or authorization change; return material expansion to planning. Keep existing apps, previews, data and global tools/configuration untouched; qualification uses isolated data and servers. Public content remains free of private paths, identities and session details.
**Completion criteria:** A person can discover Customizations from the normal task app, enter the chat/canvas Studio and understand the supported customization without a task-specific advice entry point. The host app has its own coherent working identity with secondary Powered by Keir attribution; Studio shares the visual family with a clear workspace separation. Desktop/narrow/keyboard flows remain usable; scrolling content does not move unrelated navigation, and long content keeps controls reachable. Actual simulation, separate explicit approval/activation, disable/restore, user isolation and retained native edits still work. A designer reviews the rendered functional app, affected regression and independent technical reviews pass, and the delivered preview and documentation match the implemented scope.
**Requirement baseline:**
{"source":"User product and design review, 2026-11-02","outcome":"The demo behaves and reads as a coherent task application with its own identity and a general customization workspace, with usable scrolling and a related but distinct Studio.","scope":"The demo host-app branding, navigation and customization entry/management flow; native and Studio layout, styling and interaction; affected browser regressions and concise design/running documentation. Core owns this record and roadmap.","constraints":"Preserve native task CRUD, user/session isolation, exact simulation/evidence/approval/activation/disable/restore semantics, resource limits and unsaved edits. Present implemented read-only examples honestly; do not imply live AI, arbitrary authoring, multiple active extensions or write effects. No core runtime, persistence or authorization change; return material expansion to planning. Keep existing apps, previews, data and global tools/configuration untouched; qualification uses isolated data and servers. Public content remains free of private paths, identities and session details.","completion_criteria":"A person can discover Customizations from the normal task app, enter the chat/canvas Studio and understand the supported customization without a task-specific advice entry point. The host app has its own coherent working identity with secondary Powered by Keir attribution; Studio shares the visual family with a clear workspace separation. Desktop/narrow/keyboard flows remain usable; scrolling content does not move unrelated navigation, and long content keeps controls reachable. Actual simulation, separate explicit approval/activation, disable/restore, user isolation and retained native edits still work. A designer reviews the rendered functional app, affected regression and independent technical reviews pass, and the delivered preview and documentation match the implemented scope."}
**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Moderate
**Reason:** Pre-edit complete intended presentation/browser/documentation paths are GRAY with no boundary/checkpoint finding. Navigation, nested scrolling, responsive Studio and preserved lifecycle/unsaved state require several coordinated components and independent review.
**Discovery:** Initial scope inspection found Customize my advice embedded in task details, Studio inside the native task dialog, separate CSS scroll containers and host branding under Keir. The designer reproduced a 520-pixel task-list wheel scroll moving the left navigation by the same distance. Developer discovery confirms GET /api/extensions already supplies owner-qualified selection/draft/evidence/history, while TaskDetail owns Studio and the native shell remains in document scroll flow. A general customization destination and dedicated Studio appear possible without server/runtime changes; concrete interaction/scroll/return rules and the implementation plan remain under review. No implementation is authorized yet.
**Material assumptions:** The current server lifecycle can support a general customization destination without new backend semantics. If multiple active customizations, new hooks, AI authoring or new authority/persistence is needed, stop affected work and return to planning. The existing chat/canvas direction is a reference, not visual acceptance of the current implementation.
**Plan:** Planning phase: the standing designer reviews the actual host app and Studio, proposes one coherent working host identity and concrete information architecture, layout/scroll/focus rules and supported-state design. The existing demo developer inventories the smallest safe integration and affected callers/tests read-only. Manager records the consolidated implementation plan; clean-context technical review precedes source edits. One implementation owner then builds the functional correction, designer inspects rendered desktop/narrow states, fast/affected browser checks verify it, and independent result review precedes main delivery and recoverable cleanup.
**Verification plan:**
- General customization entry -> actual browser journey from normal app navigation to its customization overview and chat/canvas Studio without opening task details.
- Host identity and related Studio -> designer rendered desktop/narrow review, visible branding/attribution checks and concise implemented-design documentation.
- Scroll ownership and reachable controls -> wheel/keyboard browser regressions with overflowing native/chat/canvas content; assert stable navigation geometry and intended content scroll, including narrow screens.
- Preserved native and lifecycle behavior -> fast native/backend regression plus actual browser simulation, separate approval/use, disable/restore, owner-switch, delayed-response and unsaved-edit paths.
- Honest supported scope -> inspect UI states/copy and docs; unavailable authoring/effects are not represented as working tools.
- Delivery integrity -> complete-path risk/workflow/privacy/whitespace checks, independent plan/result review, hosted CI and recoverable task-worktree cleanup.
**Plan review:** Pending consolidated designer/developer discovery and clean-context technical plan review; source implementation must not start.
**Approvals:** Not required at Elevated risk. User requested this product/design correction; no live-environment change is authorized.
**Exceptions:** —
**State:** Blocked or returned to planning
<!-- agent-workflow:end -->

## Implementation

Initial task context and requirement baseline recorded for the user-requested correction. Demo base `df0684301ff4a3909d7b71d019e5c63d93d23193`; core base `deb9a6086d7d2dd2c974f10a23386c6079f7996c`. Manager owns this record and canonical roadmap during discovery. Source edits await a concrete reviewed plan; no implementation or new qualification result is claimed.

Read-only discovery is active with the standing designer and demo developer. The scrolling defect is reproduced and the existing API appears sufficient; manager awaits the concrete design brief and technical inventory before consolidated plan review. No source edits, live data changes or preview restarts are part of discovery.

## User review requirements

- Simulate a real host application with customization as a normal feature, rather than presenting one technical scenario as the product.
- Fix scrolling that moves the left navigation with unrelated content.
- Give host app and Studio a coherent visual family while making the Studio a distinct workspace.
- Name the host app independently; retain Keir as secondary powered-by attribution.
- Perform a full functional/rendered design review, rather than a label-only patch or another disconnected scripted prototype.

