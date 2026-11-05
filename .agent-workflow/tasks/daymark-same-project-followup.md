# Let Studio author follow-ups in the completed task's project

<!-- agent-workflow:start -->
**Outcome:** A person can ask Studio to create a follow-up in the same project as the completed task, using its title and a chosen suffix.

**Target:** keir-demo-app

**Scope:** Correct Studio's authoring guidance and verify the request through the existing compiler, simulations, approval and native task execution.

**Constraints:** Reuse the current model, `tasks.get` and `tasks.create`. Keep fixed-project rules, existing approvals, native permissions and exact review behavior. Use isolated data for verification.

**Completion criteria:** Studio recognizes a same-project request without asking the person to pick a fixed project, prepares a checkable draft with independent examples, and the approved rule creates one follow-up with the requested title in each of two source projects.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A person can ask Studio to create a follow-up in the same project as the completed task, using its title and a chosen suffix.","scope":"Correct Studio's authoring guidance and verify the request through the existing compiler, simulations, approval and native task execution.","constraints":"Reuse the current model, `tasks.get` and `tasks.create`. Keep fixed-project rules, existing approvals, native permissions and exact review behavior. Use isolated data for verification.","completion_criteria":"Studio recognizes a same-project request without asking the person to pick a fixed project, prepares a checkable draft with independent examples, and the approved rule creates one follow-up with the requested title in each of two source projects."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:conversational-authoring"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Changes to authoring guidance affect what Studio proposes for a real task mutation. Existing model, permissions and review gates remain the authoritative boundary.

**Discovery:** `authoring-agent.mjs` and `authoring-example.mjs` describe fixed destination project choices. The current model returns `source.projectId` as a `ProjectId`, matching `tasks.create`'s input. An isolated probe with the packaged compiler and runtime ran one artifact against two projects and verified the destination and literal title suffix.

**Material assumptions:** A same-project destination can use an existing typed expression. If another authoring boundary rejects it, identify that boundary before changing it.

**Plan:** In `src/authoring-agent.mjs` and `src/authoring-example.mjs`, use scoped supportedHooks for availability and the discovered DCM for capability inputs and results, types, and protected arguments. Teach the generic rule: trace a value from a hook or earlier capability result into a type-compatible business argument, within the selected profile and effect limits. For user-selected business values, use a literal when the user chooses a fixed value. Declared enum values and other program logic follow the existing language rules. Keep Daymark meanings and project discovery in host guidance and treat source/scenario examples as syntax. This applies to the reported request through `source.projectId` and `source.title` plus a confirmed suffix. Distinguish a fixed destination from a project condition or one that follows the completed task. Clarify an ambiguous separator without requiring an unnecessary project choice. Require independent examples with distinct source projects. Preserve fixed-project examples, ordinary create and host-owned validation. Stop if the change requires a model, native permission, approval, runtime or package change.

**Verification plan:**
- Authoring → replay the reported request through Studio with a synthetic provider and verify the delivered DCM-first guidance, exact draft, two project examples and existing fixed-project behavior. Verify native rejection of an undeclared field such as `source.projectID` and the declared `source.id` (`TaskId`) passed as `ProjectId`.
- Execution → compile once with the pinned compiler, run checked examples, approve and activate the exact draft on isolated data, then complete tasks in two projects and verify the created title, destination and native receipt. Include an unfinished or unavailable example with no write.
- Real interaction → verify the business-language request on a configured provider with synthetic task data if available. Report the exact exercised provider and distinguish synthetic replay from live AI evidence. Run focused fast and browser regressions before integration.

**Plan review:** Agent technical review: see [Plan review](#plan-review) below.

**Approvals:** Not required at this risk level.

**State:** Ready for review
<!-- agent-workflow:end -->

## Plan review

Clean-context review against Daymark `9b9c59d` confirmed the exact DCM types and native owner checks. The reviewer accepted the revised DCM-first composition guidance, and specified two distinct positive project examples, a no-write case, and native rejection of an undeclared field and a mismatched nominal type. The user controls approval and activation. Daymark `92aaa60` changes only reader documentation and leaves the reviewed authoring and native paths unchanged.

## Result

[Daymark implementation](https://github.com/rore/keir-demo-app/commit/8345a174fbd182c3935955ffb8345dae52e1df28) uses typed values from the existing DCM. The isolated replay compiled one rule, checked two distinct source projects and two no-write cases, approved and activated the exact draft, and verified one correctly titled native follow-up per completion. Replay created no duplicates. The compiler rejected an undeclared field and an incompatible nominal type. The fixed-project regression passed.

Independent result review accepted the change. The focused fast suite passed 58 checks and the browser suite passed 23 Chromium checks with synthetic provider responses. A separate OpenAI run on synthetic task data reached a compile call before the provider adapter reported `PROVIDER_UNAVAILABLE`. It produced no sample results or activation. Full live-provider verification remains open. The updated preview passed source and served-asset verification and preserved its database and settings byte for byte.
