# Compact Studio design preview

Owning feature: [Extension Studio design](../../roadmap/features/extension-studio-design.md). Daymark baseline: `eb31d2541c350320fe99b2869c3e0bde42f6e985`.

<!-- agent-workflow:start -->
**Outcome:**
The user can try a compact conversation-first Studio design before production changes.

**Target:**
Standalone Daymark design preview, not the running application.

**Scope:**
One self-contained HTML preview, focused browser check and owning roadmap/record. Show populated chat, one Send, persistent target/status, inline review, separate simulated approval/activation and read-only current use.

**Constraints:**
Fictional, clearly scripted interaction only. No API/provider calls, credentials, real data/settings changes, production edits, dependencies, service restarts, commits or pushes. Preserve the demo and unrelated work. No natural-language authority implementation or production-safety claim from a mockup.

**Completion criteria:**
A separate local URL works at desktop, short-window and mobile sizes. Populated chat gains usable space without smaller body type; target/state and composer stay visible. Inline review distinguishes active use and draft; approval does not activate; current-use return preserves input. One runnable browser check covers transitions/responsive/keyboard behavior. Manager inspects renders before user review.

**Requirement baseline:**
{"source":"user approval of a small rendered design preview","outcome":"The user can try a compact conversation-first Studio design before production changes.","scope":"One self-contained HTML preview, focused browser check and owning roadmap/record. Show populated chat, one Send, persistent target/status, inline review, separate simulated approval/activation and read-only current use.","constraints":"Fictional, clearly scripted interaction only. No API/provider calls, credentials, real data/settings changes, production edits, dependencies, service restarts, commits or pushes. Preserve the demo and unrelated work. No natural-language authority implementation or production-safety claim from a mockup.","completion_criteria":"A separate local URL works at desktop, short-window and mobile sizes. Populated chat gains usable space without smaller body type; target/state and composer stay visible. Inline review distinguishes active use and draft; approval does not activate; current-use return preserves input. One runnable browser check covers transitions/responsive/keyboard behavior. Manager inspects renders before user review."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:extension-studio-design"}],"pull_requests":[]}

**Risk:** Routine
**Complexity:** Simple
**Reason:**
Isolated fictional design artifact; no production integration, persistence or authority. Prototype source stays in a documentation path.

**Approach:**
One dependency-free HTML file with native CSS layout/disclosures and in-memory scripted state. Reuse Daymark's visual language and the designer's compact target/status, transcript scroller and growing single-Send composer. Developer does mechanics; designer does not repeat the audit.

**Verification:**
Focused browser check → one Send, exact target, active/draft state, inline examples, separate simulated approval/activation, read-only current-use return, retained input, keyboard focus and no overflow at 1440×900, 1366×600 and 390×844. Manager rendered inspection → populated composition, readable type and clear scripted-preview label. No full product regression or paid model qualification for an isolated mockup.

**State:** Ready for review
<!-- agent-workflow:end -->

## Implementation

User authorized this preview, not production changes. Existing rendered audit and bounded designer synthesis are inputs. Developer owns preview mechanics/check only; manager owns this record and roadmap. No further expensive designer work is assigned.

The developer completed one self-contained HTML file, a small loopback-only static server and a focused browser check. Follow-up layout and card-density corrections leave the artifact at 16,932 bytes. Only the new preview is a tracked-path change in Daymark; production source and the retained demo are untouched. The serving checkout is retained for user review, not as a recovery copy. No commits or pushes were made.

## Evidence

Focused browser check passed at 1440×900, 1366×600 and 390×844. It exercised single Send, populated transcript, persistent target/status/composer, inline review, read-only current-use return with input/scroll/focus retained, separate simulated approval and activation, gaps blocking approval, composer growth, keyboard behavior, literal user-text insertion and dialog focus. No horizontal overflow, external requests or page errors were observed.

The short-window transcript measured 419.5px versus the earlier 256px, a gain of 163.5px (63.9%), with unchanged 16px body type. Desktop/mobile transcript heights were 719.5px/667.5px. These describe the specific rendered mockup, not a universal layout quota.

Manager inspected short-window, mobile and expanded-review captures and source, then independently tried active-version inspection/return, preserved unsent input, approval without activation, explicit simulated activation and mobile resizing through a fresh browser context. Those checks passed. Initial preview SHA-256: `943b601599f6f4ebd8be02546a42fae311fa7f76451e0e2410317225f0210e9a`.

The developer reported large-screen width feedback and first narrowed the aligned conversation/composer content from 804px to 608px. Further reported clarification concerned keeping the whole workspace together, not only narrowing text. The current preview centers a 1060px workspace with an 800px conversation/composer column and reduces inter-message spacing from 24px to 16px. Body type remains 16px and mobile stays fluid.

The focused check passed at 2000×1040 plus the three original sizes, including workspace/content centering and alignment, overflow, separate simulated approval/activation and the earlier interaction checks. Manager inspected the current large-screen render and verified by reconstructing the earlier source hash that only CSS layout/spacing changed; the script and content remain unchanged. Current source SHA-256: `63fbc73748c599d89765679e35551f8d6ec54121d42988c76d8ce93751b87d60`; served bytes match. The same loopback URL/process remains, with network connections forbidden by page policy. No behavior or production changes occurred.

The preview is ready for human design feedback, not production acceptance. It uses one fictional target and fixed scripted replies; no native task service, AI intent classification or live-provider behavior is implemented. Actual native browser zoom and exhaustive accessibility were not requalified in this small preview. The separate production demo remains running unchanged. Further designer work is not assigned; production implementation still needs user direction and the safe intent contract.

At the user's explicit request, the persistent designer inspected only the current wide, short-desktop and mobile renders. Its narrow visual verdict was positive: centering the whole workspace restores cohesion; margins, typography, sidebar, composer and mobile target/state should remain. One worthwhile adjustment remains: the collapsed proposal card competes with the conversation, especially in short/mobile windows. Recommend a compact title, concise behavior/scope, check status and Review, with full explanation in expanded review. Long desktop lines were noted as tolerable, not a reason for another width change. No general redesign or spacing pass is recommended. This is visual acceptance of the supplied preview states only; no correction or production work was started from that verdict.

The user's follow-up directed the card correction to proceed. The manager made the small preview edit directly: collapsed card now shows a short scope summary and check status; full behavior and exceptions are inside expanded review. No workspace, typography or composer changes were made. The existing focused browser check was extended to assert that full behavior is hidden while collapsed and visible with preserved exceptions when expanded. It passed at all four sizes, including separate simulated approval/activation, active-version return, input preservation and keyboard checks. Manager inspected the new short-window/mobile captures. Current SHA-256: `defa65b2fc95d408fb1a0ce6212384c7df90efde8621ddaf8493c8dd4b5bfabf`; exact served bytes and the existing URL are verified. No further designer round or production change occurred.

The user subsequently corrected the preview-only loop and directed improvement of the actual app. This preview task is complete as a design input; [production application work](studio-conversation-ui.md) is now authorized separately and is the active deliverable. No further mockup work or preview-approval stop is required. The preview remains available only as a reference until the real implementation has been compared with it.
