---
id: "daymark-visual-polish"
title: "Make Daymark consistently compact and polished"
status: "done"
priority: "high"
commitment: "committed"
milestone: "Author and activate customizations through AI"
repo: "keir-demo-app"
---

## Summary

Give Daymark one visibly better working-document composition across the whole application, inspired by the original deck mocks. Use purposeful whitespace, strong content, quiet supporting controls and coherent alignment. Preserve every supported interaction and saved work.

## Why

The current working interface remains too large and visually uneven. The delivered context-handling correction was a separate functional fix, not completion of this visual request.

## In Scope

- Shared shell/navigation, Tasks and task forms, Customizations collection, Studio conversation/Summary/Simulation/More, configuration and other existing dialogs.
- Typography, spacing, alignment, borders, surfaces, emphasis, control sizing and responsive density in the current visual language.
- One deck-informed visual language across every named surface: restrained sage/neutral palette, coherent typography, surfaces and controls, while retaining the approved compact chat-first allocation.
- A runnable, realistic cross-application visual proposal reviewed with the user before production UI edits, followed by implementation and actual rendered regression.

## Out of Scope

A new product workflow/wizard, removing supported controls or required explanations, shrinking text or hit targets past accessibility basics, blanket page scaling, a new design framework/dependency, backend/Core/schema behavior, paid provider calls and proposed functional slices. The accepted composition may regroup existing navigation, panes and controls without changing their meaning or authority.

## Done When

- [x] The user accepts one coherent revised visual direction across Tasks, collection, Studio and a dialog.
- [x] Shared styling is implemented consistently without changing domain behavior or saved work.
- [x] Navigation, Tasks, collection, Studio, forms and dialogs are reviewed together for one coherent visual language, including the pitch-deck feedback.
- [x] Desktop, narrow and short layouts retain keyboard/focus, scrolling, reading/input, complete explanations and separate approval/activation.
- [x] Build, focused and adequate combined regression, independent rendered review and backed approved delivery pass.

## Notes

The [Work Record](../../.agent-workflow/tasks/daymark-visual-polish.md) owns scope and verification. The earlier tested candidate was visually rejected. The accepted working-document composition is implemented and published in [Daymark `c4d503a`](https://github.com/rore/keir-demo-app/commit/c4d503a4c3f3c4898f1df30cdecb05fd7f3b2d25). Final independent code/coverage and visual/interaction reviews approve the exact delivered build. The existing local app is updated with preserved data/settings.

Local qualification reconciled all 298 original browser cases plus one new short-screen case. The final affected checks pass 40/40 and the last navigation/reading cases pass 2/2 on identical frozen source. Subsequent [hosted CI](https://github.com/rore/keir-demo-app/actions/runs/43939613525) passes the full **299/299 browser suite** and **679 Node tests with 12 skips**, zero failures, in a 13m38s job. Fresh backup, exact served-asset checks and actual saved-work desktop/mobile readback verify all 22 tables, schema and settings unchanged. Task previews and the completed app task checkout are removed after compact evidence preservation; the retained live checkout remains.

## Screenshot documentation follow-up

The application delivery above is complete. A later audit found that updated walkthrough prose still used six older screenshots, with four unchanged copies in the Core tour. The user authorized replacing relevant screenshots and accompanying text in isolated repository edits only: no commit, push or live-environment change.

The first refresh captured the same follow-up journey on published app `c4d503a`, using fictional offline authoring with the real compiler, simulator and native task service. Both guides, provenance and copied-image hashes were aligned and independently reviewed. The later mixed usability correction below supersedes affected captures; historical records and the original capture evidence remain preserved.

- [x] Replace all six walkthrough images and four Core copies for published `c4d503a`.
- [x] Verify that capture set's captions, provenance, 89 local links, 14 fragments and responsive image embeds.
- [x] Finish independent documentation review of that frozen capture set and retain the uncommitted edits.

## Owner usability correction

Later owner feedback identifies nested person selection, an untreated scrollbar scrolling the whole Tasks page, and reduced useful chat space. The [usability correction Work Record](../../.agent-workflow/tasks/daymark-usability-followup.md) owns this bounded mixed task. Keep the accepted visual direction, but make task content scroll independently of heading/tools/quick-add/search and verify chat against the pre-redesign build with identical content, window and reading position.

This correction initially allowed only repository edits and isolated tests. The owner later authorized publication in both repositories, then corrected release completion to include the backed existing local demo update. Original delivery and its CI evidence above remain historical evidence; the follow-through below verifies this correction's local delivery.

- [x] Correct direct person selection, task-only scrolling and scrollbar treatment.
- [x] Demonstrate useful chat space with matched before/after measurements and rendered review.
- [x] Pass focused behavior/focus/input/reading regression and refresh affected documentation.
- [x] Update the existing local demo and verify served assets and preserved saved work/settings.

The correction is complete and published in [Daymark `79a4b88`](https://github.com/rore/keir-demo-app/commit/79a4b885a2c657e5b17aaddf8fd32077f523fa53), with synchronized Core documentation. At 1440×1000 the centered transcript grows from the earlier published 678×610px to 958×724px; full Summary remains directly available without a permanent duplicate sidebar. Native project tools and task content retain usable independent scrolling at desktop, narrow and short sizes.

Independent technical, rendered and documentation reviews accept the bounded outcome. Browser qualification reconciles all 314 current cases: 263 unaffected broad passes plus 51 final affected passes, after fixing the complete run's one remaining narrow-menu failure. Node checks pass 678 with 13 skips and zero failures. Build, real follow-up capture/readback, all six app screenshots and four exact Core copies, 98 local links, 15 fragments and desktop/390px image rendering pass. The Work Record preserves exact review/build identities, failure reconciliation and qualification limits.

Publication follows the owner's explicit commit/push authorization. Core's four image copies pin the exact app commit. Hosted [Demo CI](https://github.com/rore/keir-demo-app/actions/runs/44057758799), [Core CI](https://github.com/rore/keir-core/actions/runs/44058190265) and [Core workflow](https://github.com/rore/keir-core/actions/runs/44058190465) passed.

The existing local demo now runs `79a4b88` at its unchanged URL with the same data/settings/dependencies. A fresh verified backup precedes the scoped restart. All three served asset hashes match the qualified build. Actual saved-work desktop/390px Tasks/collection/Studio, Summary-return and retained unsent-input readback passes with zero page errors or domain/authoring/provider writes. All 22 tables, schema and settings matched the backup immediately after restart/readback. Later live task-completion activity is preserved separately, as recorded in the Work Record. No unrelated environment change occurred.

The clean completed Demo task worktree was removed without force after compact evidence preservation. Core's task checkout remains through publication and hosted-check reconciliation for the manager-role improvement and these local-delivery notes. The manager role now makes verified authorized local rollout, or a specific reported blocker, part of release finish.
