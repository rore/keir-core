# Customization Studio: review inside the conversation

Design proposal, 5 November 2026. This is a reviewable interaction model, not an implemented product guarantee. Rendered review uses a separate synthetic fixture built from this written specification in an isolated host-app checkout, with its normal layout, styles and HTTP test setup. The earlier standalone HTML is excluded from that fixture's inputs. All fixture results and actions are illustrative.

## Recommendation

Name the workspace **Customization Studio** consistently in its entry link, page title and header. Label the conversation speaker **Daymark assistant**, including response regions and the message input's accessible name. The workspace is a place; the assistant is the speaker. A separate character name adds no clarity here. The prototype starts inside the workspace; its entry link belongs to the host application's navigation.

Keep the conversation as the single working surface for this flow. Reveal two distinct visuals in order: **the saved behavior**, then **the saved example checks**. A separate canvas adds navigation without helping this small review. Do not delete a general canvas capability on this evidence; larger comparisons may need a dedicated view later. If a split view remains elsewhere, its divider must support pointer and keyboard resizing.

Use familiar task rows, restrained warm paper and ink, green for the principal action and matching results, amber for uncertainty. Avoid decorative dashboards, technical operation counts and branded comparisons. The useful cue from the pitch concept is a concrete proposed change inside the conversation, followed by a clear decision.

## First visual: what the saved change does

Show one sentence each for **When** and **Then**, followed by a recognizable task pair:

- You complete **Plan launch** in **Launch**.
- One unfinished task, **Plan launch - follow up**, would be added in **Launch**.
- The original task stays completed. The title suffix is highlighted; completion is expressed in text as well as the checkbox illustration.

Label this as an illustration, not an observed result. Show **Not in use** and **No tasks have changed** only when confirmed for this saved version and context. The primary action is **Review checked examples** when current saved checks exist. If none exist, use **Check examples** and explicitly perform that operation; never relabel an illustration as checked evidence.

## Second visual: what was actually checked

Use expandable rows with **Situation**, **You wanted**, **What happened in the example**, and a text result. Open the first row by default. Put **Example only · no real tasks changed** beside the results heading once, rather than repeating it in every row. Keep the coverage limit visible outside collapsed rows.

| Situation | You wanted | Illustrative observed result | State |
| --- | --- | --- | --- |
| Complete Plan launch in Launch | Add Plan launch - follow up in Launch | Exact title and project match | Matches |
| Complete Book the room in Operations | Add Book the room - follow up in Operations | Exact title and project match | Matches |
| Leave Plan launch unfinished | Add nothing | Nothing added | Matches |
| Completed task temporarily unavailable | Add nothing; explain the unavailable task | Nothing added; unavailable-task explanation | Matches |
| Original title plus suffix exceeds the allowed length | Preserve the full original title and suffix | Not run; outcome unknown | Not checked |

Use the result sentence **Four examples matched what you asked for. One situation has not been checked.** Place **Still unchecked: titles that become too long** beside the decision, outside all disclosures. It must remain visible with every row collapsed, including on a phone. Never say “all passed” or “ready for every situation.” Do not substitute truncation, a different title, or an easier expectation for the user's request. A long-title policy remains unresolved; choosing one requires an explicit product decision and relevant checks.

The prototype's **Approve this change** state assumes the host permits approval with the disclosed unchecked case. The client must derive eligibility from the existing host decision. If policy requires this case to be resolved first, replace approval with the actual permitted next step and explain why. This proposal does not introduce an approval-policy exception.

## Conversation continuity and decisions

| Event | Visible response | Permitted next action |
| --- | --- | --- |
| “I don't understand” before approval | Explain Plan launch → Plan launch - follow up in everyday language, without another full copy of either card | Show example checks, linking to the existing review |
| “So can we make this work?” with eligible saved evidence | Bring the same review back into view; say whether anything was rerun | Approve this change |
| Explicit approval succeeds | Approved · Still off. Explain the future effect and retain coverage limits | Turn on for my tasks |
| Explicit activation succeeds | On for my tasks; future matching events can create follow-ups | Turn off |
| User revises the title rule | New saved behavior, not checked; earlier results remain readable | Check this revised change |
| An example differs from the request | Show exact expected and observed difference | Fix this example |
| Saved evidence cannot be loaded | Keep the saved change; state that results cannot be confirmed | Check saved results again, as a read-only retrieval |

The assistant may explain or propose; it never approves or activates for the person. Repeated clarification must preserve the current approval and activation state, including after activation. It must not create another customization, rerun checks, replay task events or fabricate a fresh result. A revised draft does not silently replace a version already in use; show **Earlier version in use · Revised change not checked** when applicable.

Keep one principal decision on the latest actionable card. Earlier cards retain their content, with historical actions replaced by a state label and a link to the current review. Reopening a review scrolls or links to the same evidence; it does not append conflicting approval buttons. The compact status bar offers a return to the latest saved change. Persist history across reloads and agent turns.

After successful approval, replace the approval action with **Turn on for my tasks** on the current decision surface. Distinguish **Approved · Still off** from **On for my tasks** using explicit text and a supporting visual treatment, never color alone. Read approval availability and check completion from authoritative evidence. The synthetic fixture must mark its evidence as simulated and must not imply that real host checks ran.

During a real check, announce **Checking examples…**, retain the submitted request and show completed versus pending examples without counting pending results as passes. Offer cancellation only when the host supports it; explain whether cancellation succeeded. Retrieval failure and execution failure have different recovery actions. After an uncertain approval or activation response, reload authoritative state before offering a repeat action.

## Evidence and implementation boundary

Bind every actionable review to the owner, customization, saved revision, draft version, example suite and evidence identity already used by Studio. Reconcile those values against current host state before enabling or accepting approval. A changed revision, changed suite, missing evidence or owner mismatch makes earlier evidence non-actionable. Keep older results readable, clearly labelled as earlier, without presenting them as current proof.

Plain-language intent is not proof of executable behavior. Generate the first visual from available saved metadata and label any unverified agent summary as proposed intent. The second visual must use host-provided expected and observed evidence; do not infer success from assistant prose. If required semantic information is missing, expose that limitation rather than adding a generic capability promise. Production work belongs in the host application; no core runtime or authorization changes are implied.

## Responsive and accessible behavior

- Desktop: a readable conversation column with distinct user messages, assistant prose and review cards. The composer stays available without covering the latest action.
- Narrow screens: stack the task pair and expected/observed values vertically; preserve full titles with wrapping. Keep result labels and coverage warnings readable without sideways scrolling. Avoid a second scrolling pane.
- Use native buttons, disclosure controls and named inputs. Tab order follows reading order; disclosures work with keyboard activation. Primary controls should be at least 44 CSS pixels high.
- Results always have text, never color alone. Target WCAG AA text contrast, visible focus and usable layout at 200% zoom and 320 CSS pixels wide.
- Announce new results and decision outcomes briefly through a polite status region, not by rereading the entire transcript. Move focus only after an explicit user action; do not steal focus during typing or background responses. Keep the old scroll position when the person is reading earlier messages.

## Acceptance and current verification

### Required UX iterations

Do not close design review after source checks or a single attractive screen. Complete the following passes before calling this proposal visually verified:

1. **First rendered walkthrough:** start without reading this specification, at desktop and phone widths. Evaluate the visible conversation as a business user would encounter it. Record whether the screen itself answers: What changes? Are these examples or my real tasks? What remains unchecked? What should I do next? Treat this as a designer's heuristic walkthrough, not a user test.
2. **Interaction walkthrough:** follow confusion → proceed → current review, then inspect a failed example and stale evidence. Complete explicit approval and separate turn-on. Record misleading copy, missing context, competing actions, hidden uncertainty, focus problems and clipping, with the scenario, viewport and screenshot for each finding.
3. **Revised pass:** fix findings that impair understanding or operation, capture the same states at the same widths, and report before/after observations. Recheck affected transitions. If a finding remains ambiguous, record it rather than declaring it resolved. If no change is warranted, give the evidence for that conclusion.

The acceptance sequence also requires **two fresh independent review rounds**, coordinated by the manager. Finish and hand off the baseline without waiting for a reviewer. Supply its exact fixture revision (including any uncommitted changes), viewable HTTP surface, desktop and phone renders, and steps to exercise the core flow. The first reviewer assesses that baseline and suggests improvements. Implement the useful changes and preserve a clear before/after record. A different fresh reviewer then assesses the revised fixture without receiving the first reviewer's conclusions. Supply neutral tasks and fixture limitations, not a verdict to confirm.

Both independent rounds cover visual hierarchy, typography, spacing and alignment, card density, mobile wrapping and workflow clarity. Track suggestions as applied, deferred with a reason, or unresolved; pass two must not silently discard remaining findings. These are independent design reviews, not a substitute for tests with business users. Keep the first review's findings outside the second reviewer's briefing.

| Pass | Current evidence | Status |
| --- | --- | --- |
| Source review | Nine illustrative states checked; exact title expectation retained; approval and turn-on separate; key palette pairs meet calculated text contrast | Complete, limited to source behavior |
| First rendered desktop and phone walkthrough | Designer inspected synthetic fixture revision `915d9d1` at 1440×1000, 390×844 and 320×740 | Complete; findings below, not independent review acceptance |
| Revision following rendered findings | Designer compared `28aa946` with frozen baseline and intermediate builds at desktop and phone widths | Five findings resolved; final limits below |
| Fresh independent review, round one | Manager's Work Record records baseline findings and resulting changes | Complete for the synthetic fixture |
| Fresh independent review, round two | Manager's Work Record records a different reviewer's findings and recheck on `28aa946` | Complete for the synthetic fixture |
| Working implementation against this proposal | Separate review arranged by the manager | Not covered by this prototype |

Keep screenshots and operational evidence in private local records. Public handoff findings should reference the scenario and viewport without local machine paths. Unresolved questions include whether the unchecked-case warning is prominent enough at phone sizes, whether people understand approval versus turn-on, and whether bringing a saved review back into view preserves enough conversational context. Source checks cannot settle those questions.

### Initial critique to verify in the fixture

These are design-review findings, not observed browser results or visual acceptance. Preserve the first available rendered fixture before revising it; capture the same states afterward. Do not invent a before screenshot or claim a defect was observed when it was only anticipated from the specification.

| Initial proposal or concern | Required revision | Rendered evidence to collect |
| --- | --- | --- |
| Example-versus-real context could become repetitive | One example-only label at the results heading | Heading visible with open and collapsed rows |
| Bare count “4 match · 1 unchecked” requires interpretation | Full result sentence plus named unchecked case beside the decision | Desktop and phone, every row collapsed |
| Clarification could duplicate the review cards | Task-title pair and one Show example checks action to the existing review | Conversation before/after clarification; unchanged review count and evidence |
| Approval and activation could look like one state | Replace the primary action after approval; explicit Approved · Still off versus On for my tasks | Same decision surface before approval, after approval and after activation |

Before integration, walk the full explain → checks → clarification → resumed review → approve → turn on → turn off flow. Verify that exact titles and project identity survive both passing examples; unfinished and unavailable cases produce no additions; the overlong case remains unchecked. Exercise mismatch, missing evidence, revised draft with an older active version, duplicate clicks, reload and uncertain action responses. Assert that clarification performs no mutations and stale cards cannot approve or activate anything.

### Rendered designer walkthrough: baseline `915d9d1`

The separate synthetic HTTP fixture was accessible. The designer walked saved behavior → checked examples → confusion → return to the same review → approval → turn-on, plus failed, revised and unavailable-evidence scenarios. This was an informed designer review, not either of the manager's independent cold reviews. Thirteen screenshots were retained privately, including matching desktop and narrow-phone results states.

What worked: readable warm neutral styling, a recognizable task pair, clear expected/observed values, separate approval and turn-on actions, and no duplicate review after clarification. Failed evidence exposed the wrong project and offered repair without approval. Earlier evidence remained read-only after a revision. The unavailable state offered retrieval without approval. At 320 pixels, tested keyboard navigation had a visible focus outline and Enter expanded an example; the unavailable state had no horizontal overflow.

| Finding | Observed state and size | Bounded correction |
| --- | --- | --- |
| The saved rule reads as if it applies only to Plan launch in Launch | Saved behavior, desktop and phone | Describe completing a task in any of the person's projects, then label the concrete task pair as an example. Use “Your saved change” instead of “Saved behavior.” |
| Two amber summary bands compete, while the decision loses the unchecked context | Checks, desktop and both phone widths; all collapsed at 390 and 320 | Use a plain result sentence. Put one named unchecked warning immediately beside the decision, outside disclosures. Keep the example-only label once at the heading. |
| Clarification gives an explanation but no direct route to the existing review | Confusion after checks, desktop | Add one Show example checks action to the brief task-pair explanation; link to the existing review without duplicating it. |
| Result pills wrap at inconsistent positions | Collapsed checks, 390 and 320 | Give status a consistent column on desktop and a consistent position under the situation on narrow phones. Preserve readable titles and disclosure controls. |
| The wrong-project difference requires comparing whole sentences | Failed example, desktop | Emphasize the differing project names in expected/observed values while keeping the full text. Retain a plain explanation of the mismatch. |
| Activation state is less explicit than the approved/off state | After turn-on, desktop | Use On for my tasks consistently at the status and decision surface; keep Approved · Still off visibly distinct and the unchecked warning present. |

The baseline's fixture-only eligibility explanation is appropriate test metadata, but should not become product-facing copy. Keep policy assumptions in fixture context; the real decision surface should say what approval does and derive availability from host evidence. No claims about service execution or real task effects follow from this walkthrough.

### Revised designer comparison: `e6a1632`

At 1440×1000, 390×844 and 320×740, the revised fixture improved the clarification path and the pre-approval explanation. The clarification reply now has a working **Return to saved checks** action that focuses the existing review. The decision now says that approval leaves the change off and the long-title outcome is unknown. Fixture metadata moved into the header, and the redundant return control is absent in the initial state. Keyboard approval at 320 pixels moved focus visibly to the checks heading and exposed the separate turn-on action.

The following findings remain open in this exact revision:

- **Rule versus example:** the heading remains Saved behavior and When still reads as completing Plan launch in Launch, rather than the general rule.
- **Result hierarchy:** the bare count and two amber bands remain. The named unchecked condition is not retained beside the decision after approval; the new nearby explanation applies only before approval.
- **Mobile alignment:** result pills still wrap inconsistently in collapsed rows at 390 and 320 pixels.
- **Mismatch emphasis:** the wrong project remains embedded in otherwise similar expected/observed sentences without emphasis on the differing value.
- **Active-state wording:** activation still shows In use instead of On for my tasks.

Corresponding screenshots were retained privately for behavior, checks, clarification, approval, activation, failed evidence and phone results. This comparison is not the second independent review. Keep the revised build frozen for that review, then reconcile remaining suggestions explicitly as applied or deferred with a reason before closing the design work.

For the consolidated correction, use one quiet sentence for passing coverage, one distinct warning next to whichever decision is currently available, and a consistent second line for case status on narrow phones. Retain the generic rule above its specific example and emphasize the differing project in a failed result. Keep the independent review brief free of these conclusions until that review finishes.

Final evidence must name the exact revised build and disposition all five open findings. Retain corresponding 320- and 390-pixel captures. Verify keyboard order, visible focus and action transitions; perform an actual 200% browser-zoom check rather than claiming a narrow viewport is equivalent, and restore the temporary zoom afterward. Inspect accessible names for result disclosures, approval/off and active states, and the message input. Distinguish an accessibility-tree inspection from a screen-reader test; report any remaining untested behavior.

Check desktop, 390-pixel and 320-pixel layouts, 200% zoom, keyboard navigation, focus visibility and screen-reader announcements in a rendered implementation. No external testers are available: these checks provide design and implementation evidence, not proof that ordinary users will understand the flow.

### Polished fixture disposition: `28aa946`

The designer verified the clean source revision `28aa946c6413795b74771fb42ac7a6f221163c14` and its separate HTTP fixture. The five findings from `e6a1632` are resolved in the rendered states checked:

| Finding | Verified result |
| --- | --- |
| Rule versus example | Your saved change describes completing a task in any of the person's projects; Plan launch is explicitly an example below the general rule. |
| Result hierarchy and coverage | A plain result sentence replaces the count and duplicate amber bands. One named unchecked warning remains beside approval, turn-on and turn-off decisions. |
| Mobile alignment | Result statuses occupy a consistent second line at 390 and 320 pixels. |
| Mismatch emphasis | Launch and Operations are emphasized in the failed example's expected and observed values, with the full explanation preserved. |
| Active-state wording | On for my tasks is distinct from Approved · Still off, in the status and decision content. |

The clarification action returns to the existing review. Keyboard activation verified approval, turn-on and turn-off, including visible focus at the review heading and approval remaining saved after turn-off. The failed-example repair moved to a revised, unchecked state; explicit recheck produced new synthetic results before approval became available. Accessible labels expose the assistant input, example situation with result and expansion state, approval/off state, and active state. This is accessibility-tree and keyboard evidence, not a screen-reader test.

Desktop evidence uses 1440×1000; phone evidence includes 390×844 and 320×740. The manager's Work Record records both independent rounds and the second reviewer's verification of long-title answers and recovery actions on this build. Those review results are reported from that record, separately from the designer's own walkthrough.

**Remaining verification:** actual 200% browser zoom was not completed. Computer Use stopped when it could not establish the native browser's current URL with sufficient confidence. No zoom change was made; temporary viewport overrides had already been reset. Screen-reader testing also remains unperformed. The recorded first-action fit at 390×844 has only a two-pixel margin, so do not infer robust magnification behavior from the default-size screenshots. These limits prevent an unconditional accessibility or production-readiness claim.

The synthetic fixture has no service calls, persistence or real approval effects. Its scenario menu loads illustrative examples. The earlier standalone local-file route remains excluded. The design corrections and two independent review rounds are recorded; zoom, screen-reader and working-application verification remain separate outstanding checks. Production integration and acceptance remain with the manager.
