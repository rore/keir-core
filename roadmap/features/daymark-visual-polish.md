---
id: "daymark-visual-polish"
title: "Make Daymark consistently compact and polished"
status: "active"
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
- [ ] Build, focused and adequate combined regression, independent rendered review and backed approved delivery pass.

## Notes

The [Work Record](../../.agent-workflow/tasks/daymark-visual-polish.md) owns scope and verification. The earlier tested candidate was visually rejected. The accepted working-document composition is now implemented across the application. Independent code and rendered reviews cover the shared result, with a final bounded scroll/focus delta under review. The user authorizes publication and a backed local update after acceptance.

Qualification reconciles all 298 original browser cases plus one new short-screen case, with zero unresolved failures. This is cumulative evidence, not one all-green run. The final affected checks pass 40/40 and the last navigation/reading cases pass 2/2 on identical frozen source. Node regression passes 678 with 13 existing skips. Final acceptance, publication and preservation-verified delivery remain; the retained app is unchanged and freshly backed up.
