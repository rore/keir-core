---
id: "daymark-composed-business-capabilities"
title: "Compose new business capabilities on top of Daymark"
status: "queued"
priority: "medium"
commitment: "committed"
milestone: "Extend host behavior safely"
repo: "keir-demo-app"
---

## Summary

Show that a customer can go beyond customizing individual points in an app. A team project admin can compose several bounded Keir programs into a versioned business capability that Daymark did not ship, and simulate, review, approve, and separately activate it as one unit. Prove that the same trusted Daymark operations support two substantially different processes without process-specific host code. Daymark still owns data, permissions, native writes, and recovery.

## Why

The current demo shows what one customization can do at one hook. This feature tests a broader product idea: customers can build mini-apps in the sense of adding their own business capabilities on top of a trusted host. **Deliverable review is a candidate demonstration, not the definition of the feature.** A new screen is not required to show new functionality.

## In Scope

- **Personal and Team are distinct.** Personal projects keep their existing individual customizations. In a Team project, a project admin manages a capability that applies to the shared project and its members. Start with one seeded Team project, two users, and at most one active capability for new work.
- **One versioned capability.** Daymark manages the exact Keir programs, event bindings, and configuration as one immutable revision. One proposal and review cover the whole revision. Explicit approval binds the revision and simulation evidence; activation is a separate decision that switches the selection atomically.
- **Keep started work on its policy.** Capture the active revision when work starts, in the same native transaction that admits it. Later events use that pinned revision, even after a new activation. The new revision applies to new work only. A pin preserves policy, not permission: current authority and revocation still apply.
- **Keep authority and failure handling native.** Daymark enforces membership and admin permissions. Policy effects run under explicit, narrowly scoped authority, not borrowed admin rights. An unavailable gate blocks covered completion. The app rechecks the facts behind an allow decision, the pinned version, and current authority at native commit. Failed or uncertain creation of a required review task leaves a visible, recoverable condition. Reconcile uncertain effects before any repair; never blindly retry or silently bypass the gate.

### Delivery gates

| Stage | Outcome | Proceed when |
| --- | --- | --- |
| 0. Architecture proof | Hand-author two substantially different processes against one proposed catalog. Use the pinned published Keir compiler and runtime and a disposable store. A short script is enough. | Both pass against the same frozen catalog and host implementations, with no process-specific host code. Check a missing or uncertain effect and an old/new version switch. If a generic catalog change is needed, rerun both against the revised catalog. |
| 1. Native Daymark journey | Run one manually installed capability across two users in one seeded shared project using existing task screens and actions. Add only the generic host operations the journey needs. | Native authorization, commit-time checks, version pinning, and failure handling pass. Changing the customer rules changes what Daymark does without new host code for that policy. Personal behavior still works. |
| 2. Studio experience | Reuse Studio for one proposal, whole-capability simulation, explicit approval, and separate activation. Test AI authoring only after the behavior works. | A project admin can understand, review, and activate the exact revision, then complete the native journey without an AI call during execution. |

For the review candidate, show the full sequence: a deliverable gets its linked review task, early completion is denied, another user completes the review, and the deliverable can then be completed. This is a **review-completion gate**, not formal approval or sign-off. Saved task state connects the events; no Keir program waits. Intake routing is a candidate for the second process and needs only an executable proof, not a second polished demo.

## Out of Scope

- Custom screens or generated UI in the initial demo. Later, Daymark could supply approved React building blocks and let AI compose them declaratively with an existing tool such as `json-render`. That is a separate presentation layer, not a Keir Core responsibility. No arbitrary frontend code or generic UI framework.
- A hardcoded review product, generic data-model framework, broad team/workspace hierarchy, waiting workflow engine, scheduler, or multi-write Keir invocation. Keir Core stays unchanged unless the proof exposes a concrete, unavoidable limitation.

## Done When

- [x] Two substantially different processes execute in the isolated proof against the same frozen catalog and fixture implementations using the real compiler and runtime. Review combines multiple bounded programs. Neither process needs a process-specific adapter. Native implementation is still pending.
- [ ] Whole-capability simulation uses the exact pinned programs and shared disposable state across events. It shows expected versus observed outcomes for success, denial, and failure. Simulation is clearly separate from proof of native authorization, race safety, or real effects.
- [ ] Personal customizations remain unchanged. The Team capability works across real users and events in the existing UI. Native tests cover current authority, commit-time checks, pinned behavior across version switches, and visible failure and recovery.
- [ ] One team-level proposal and review cover the versioned programs, bindings, configuration, and simulation evidence. A project admin explicitly approves and separately activates that revision. Execution makes no AI calls; authoring evidence distinguishes scripted from live AI.

## Notes

**Stage 0 complete.** The [isolated proof](https://github.com/rore/keir-demo-app/commit/14bb4e4f17c135537199b72d6dfb47d38711b0d2) compiled and ran three programs with Keir 0.5.0 against one model and binding. Review denies early completion and allows it after the linked review; intake routes only matching tasks. Version switching and uncertain-write cases passed, and a deliberately wrong expectation failed. The [Stage 0 Work Record](../../.agent-workflow/tasks/daymark-capability-proof.md) records the reproduced results and exact package, catalog, and artifact identities. No Core gap was found in this experiment. Stage 1 is not assigned; no native authority, UI, or authoring claim is made.

The [original Daymark Team Processes draft](../../docs/internal/design-baseline/public/keir_daymark_team_process_feature_spec.md) is preserved unchanged. It contains the proposed catalog and task primitives, the program sketches, lifecycle and authority rules, the scenario matrix, and the handoff. This feature card controls current scope; the draft is design input, not a released contract or an implementation assignment.

**What changed after the draft:**

- The goal is a customer-defined layer of business functionality. Review and intake routing are examples. Both must pass the isolated same-catalog proof; only one needs the full native and Studio journey. A generic catalog revision is allowed during the proof, but both cases must pass again against identical catalog bytes and implementations. Custom UI is deferred to a separate host layer.
- The review/routing example needs **three** programs: review creation, the completion gate, and intake routing. Use the draft’s catalog (§§4–7) and scenarios (§10) as inputs. Keep no-cascade behavior, an independent expected outcome for each case, and a deliberately wrong expectation that visibly fails and blocks approval. The eventual Studio timeline should link each event to its pinned program and inspectable evidence.
- Pin native work from the first native slice, even if the capability is manually installed. Deactivation stops new admissions; pinned work continues unless revoked. Emergency revocation prevents subsequent effects and blocks covered gates. Preserve an audited native recovery path, not a policy bypass or automatic migration.
- Resolve what satisfies the review gate before implementing it: a tag is not proof that a child is the review the process requested. Prefer an immutable, host-owned link to the original effect if that is the promise. Specify how to reconcile an uncertain create by its original operation key before remediation. Keep these and the routing conflict rule open as design decisions; the current recommendation is not to overwrite a later human edit.
- Avoid a circular evidence identity: approval should bind the immutable executable revision and its evidence as a pair. Distinguish an intentionally absent hook from a required pinned hook that is unavailable. Keep the draft’s detailed authorization, transaction, and recovery requirements for native verification, not as claims of the fake-store proof.

**Next decision: Stage 1 native journey.** Use the proof as design evidence, not as production task or lifecycle code. Resolve project authority, commit-time consistency, and audited recovery before connecting it to real work. If a later experiment exposes a Core limitation, record the exact pinned model, source, and diagnostic or trace. Try a natural generic host operation before proposing a separate Core change.
