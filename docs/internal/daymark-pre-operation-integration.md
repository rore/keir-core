# Daymark task-create guard integration

Status: **proposed; host-specific companion to [pre-operation decisions](../specs/pre-operation-decisions.md)**. Inspected host baseline: `16c7f5c`; Core baseline: `a79ceba`. Owning feature: [task-create-guard](../../roadmap/features/task-create-guard.md). This document assigns no Daymark fields, storage, routes or authorization logic to Core.

## Exact seam

At the inspected baseline, `src/data.mjs` has two creation paths: `taskService(db).createTask(owner,input)` and `taskEffectService(db).createTaskOnce(owner,key,input,invocation)`. They share private `insertTask`, but enter transactions separately. `src/app.mjs` calls the first; `src/task-effects.mjs` binds the second as `tasks.create`. Guarding only the HTTP route would leave an automation bypass. Putting an asynchronous guard inside `insertTask` would hold a transaction across evaluation.

Move only creation orchestration to an asynchronous shared host path. Keep normalization, ownership checks, private synchronous insertion, event capture and native receipt logic in `data.mjs`. Both exported creation methods must use that path and return a promise; update every production caller and test fixture to await it. The dispatcher adapter must use `return await effects[method](...)` inside its existing try/catch so asynchronous errors retain correct effect classification. Update operations need no new guard in this slice.

The host constructs this service with its guard evaluator during startup, using its existing lifecycle and exact prepared handles. Omitted/unavailable evaluator wiring is a startup/configuration failure, not no active rule. Read methods may retain the existing service view, but an independently constructed service must not expose an unguarded create method. No exported raw insertion, `skipGuard`, client-origin flag or alternate internal create route. Seed/migration construction is explicitly offline data setup, never a route for creating a live user task.

Proposed sequence:

```text
validate caller and authored request
if authenticated native receipt already exists:
    validate capability/version/payload; return saved result unchanged
normalize and freeze proposed native task
select current TaskBeforeCreate rule (or verified absence)
evaluate read-only guard outside write transaction
if DENY or unavailable: end without task/event insertion
open existing native write transaction
    recheck receipt (concurrent duplicate may have committed)
    recheck native authority, same proposed values, selection and read facts
    insert task once
    keep existing origin-specific event behavior
    atomically store native effect receipt when this is an automation write
commit
```

The second receipt lookup wins over a stale evaluation: a matching already-saved receipt is replay, not a new write needing new guard approval. Mismatch still rejects. Never return a newly computed denial in place of an existing CONFIRMED receipt. No await occurs inside the native write transaction. Concurrent same-key calls recheck under that transaction before insertion.

## Hook and projection

Expose one optional `TaskBeforeCreate` selection per owner/project scope using the existing selection/approval lifecycle. Genuine absence permits the ordinary path; broken active rules block. Recheck the selection generation at commit even when the original selection was absent, so a newly enabled rule cannot be bypassed by an in-flight create.

Project normalized `projectId:ProjectId`, `priority:Priority`, `tags:Tags`, `hasNotes:Boolean` and `hasDueDate:Boolean` into the hook input. Reuse current priority values/default and the existing bounded Tags list (at most eight normalized strings). Human/API tags use native normalization; automation's existing `tagsText` uses the existing parser to obtain the same list. Do not join all native tags into a new 256-byte string: valid native tag sets can exceed that combined size, while each element fits the existing list type. No string splitting or new CEL operation is needed.

`hasNotes` is true exactly when the normalized native notes have non-whitespace content; `hasDueDate` is true exactly when native validation has produced a non-null valid due instant. These general presence facts avoid copying long notes and retain native timezone/date validation. They are not customer-specific predicates. Actor, request key, origin and invocation depth remain trusted host context. Freeze the full native proposal privately, including title and values not visible to the rule. All native field limits remain unchanged; never truncate to fit a guard projection.

A customer rule reads approved project data and decides whether the proposed creation is allowed. Add `projects.get@1`, READ, agent argument `projectId:ProjectId`, protected `actorId`, result `{id:ProjectId,name:String}`, and declared `PROJECT_NOT_FOUND`/`ACCESS_DENIED` errors. Reuse the existing owner-scoped native project lookup; its name limit is 80 UTF-16 units, within Core's 256-byte limit for well-formed values. Malformed legacy values fail projection, never silently normalize. This operation is needed because current `tasks.get` and `tasks.openMatch` cannot read project metadata. No new project properties or per-customer handler are needed.

The three required examples are ordinary authored conditions, not a host catalog of special handlers:

| Example | Authored denial condition | Example message |
| --- | --- | --- |
| Client-tagged tasks need a due date | `"client" in input.tags && !input.hasDueDate` | Add a due date for this client task. |
| High-priority tasks need notes | `input.priority == "high" && !input.hasNotes` | Add a note for this high-priority task. |
| A chosen project requires the review tag | The authorized `projects.get` result ID equals a typed customer-selected ProjectId and `!("review" in input.tags)` | Add the review tag for this project. |

Tags uses the existing String element type; project constants use existing nominal literals. An author may combine the conditions in one program, choosing a deterministic first denial message. There remains exactly one active rule for the trigger. Project selection comes from the authorized host catalog, never a name guessed by the assistant. The adapter only reads project data; the project choice and condition belong in the program.

Daymark declares output `{decision:Decision,message:String}` using ordinary DCM fields; ALLOW uses an empty message, DENY supplies the correction text. A blank DENY message uses safe generic host text. Display bounded messages as plain text only, associated with the exact owner/submitted attempt/selected revision. Do not interpret message text as a field identifier or permission to bypass a guard. No new correction-field protocol is needed: keep the detailed creation form editable and show the message. Native validation/permission errors and unavailable checks keep their own host text and status.

## Consistency without a new transaction framework

The first guard catalog supports bounded local native READs whose values/predicates can be rechecked synchronously. Capture each reached READ's exact normalized arguments and returned projection in the private invocation closure (at most four entries). Before insert, rerun those same authorized native queries inside the existing write transaction and compare their complete typed values. A changed result blocks the attempt as stale; do not automatically rerun the guard. Recheck native project/owner permissions as well. This covers negative existence predicates, not just rows that happened to exist. The input snapshot must also be the one inserted.

Do not expose that read set in the DCM, artifact or result. Do not introduce generic database version vectors. Reads that cannot be rechecked under the native consistency boundary are unavailable for this first authoritative guard. An unavailable selected rule blocks; it never becomes a false lookup or no binding. No remote calls or asynchronous adapters run while holding the write transaction. Test concurrent changes from a separate connection, including an absent match becoming present and an activation changing during evaluation.

## Replay, origin and budgets

- All fresh live creation, including automation-origin creation, invokes the guard. Trusted origin controls existing post-commit event production only. Keep the current rule that automation-created tasks do not recursively emit TaskCreated; do not turn that suppression into a guard bypass. Human creation still captures TaskCreated atomically with the successful insert. No task or TaskCreated event is recorded on DENY/BLOCKED.
- Existing settled native receipts replay byte-for-byte without reevaluation. Existing uncertain/started operations continue to require reconciliation; a new guard does not make replay safe. Fresh automation denial returns a declared business rejection with `effect:NONE` only after the host establishes no native mutation. Keep guard-denied and guard-unavailable distinct in host diagnostics and UI; preserve the selected capability's existing declared response codes as described below. Do not disguise infrastructure failure as customer DENY. Unexpected errors after entering the write path retain UNKNOWN handling.
- A newly reached guard failure may be persisted as a terminal NONE native receipt under the existing original-key rules; later replay returns that same response. Retrying a changed proposal or rule requires a new host-authorized operation, not rewriting the receipt. Check for a winning concurrent receipt before persisting the failure as well.
- Maximum synchronous chain: one outer post-commit invocation and one nested before-create guard. No guard-to-guard or READ-to-hook recursion. Host-only context enforces depth at entry; callers cannot manufacture it. Existing max-four outer calls plus max-four guard READs gives at most eight capability dispatches in this chain. No automatic guard retry. Preserve one outer WRITE and existing async event-cycle suppression.
- Use the enclosing deadline for an automation create, with a smaller guard expiry that reserves native commit time. Human/API creation gets the same host-owned operation budget. Host tests must measure and select the reserve; never reset the outer budget. Cancellation closes the attempt; late output cannot insert. A timed-out outer write remains UNKNOWN until native evidence establishes its result.

## Required delivery evidence

Test UI/API creation, direct service creation and the real bound tasks.create invoker. Cover ALLOW, DENY, no binding, active-but-unavailable, bad output, denied READ, expired/cancelled invocation, late ALLOW, selection race, changed read facts and unchanged native permission/validation. Assert actual task/event/receipt rows, not just a consumer label. Preserve exact replay payload/version mismatch behavior, concurrent same-key single insertion, rollback on receipt failure, and outer CONFIRMED/UNKNOWN semantics.

For each of the three examples, test matching allow/deny, nonmatching input and unavailable execution; include empty/whitespace/long notes, absent/present/invalid due input, normalized duplicate tags, maximum native tag set, and another owner's project. Test pure ALLOW with forged non-null key or absent authenticated actor at the host gate: Core does not authenticate keys and a no-call plan needs no protected actor binding. No native continuation is permitted in those host cases.

Studio must describe this as before-create validation, show DENY separately from an unavailable rule, simulate the exact artifact with expected native continuation/non-continuation, and require normal approval before activation. Existing post-commit triggers keep their wording and behavior. Publish the supported projection in customer guidance. Use final official Core assets and verify package provenance before combined acceptance.

Reuse the native detailed creation fields for notes, tags, priority and due date so a denied quick-add can be corrected without saving a placeholder task. Preserve all submitted fields and newer edits after DENY/unavailable. The ordinary explicit Add action retries only confirmed no-save cases; an unconfirmed native response needs existing safe reconciliation/deduplication, never blind automatic retry. Creation-only scope excludes edits/completion. A rule message is not a native save receipt.

Keep the existing `/2` event model, approved artifacts and receipts intact when installing the new runtime. Add the `/3` guard model and its independently approved selection; do not rebuild old approvals against it or silently change event capability versions to add guard-specific error codes. Existing `tasks.create@2` adapters can map confirmed guard denial to their existing declared `TASK_INVALID`/NONE result with separately stored host diagnostic detail. A new public guard-specific error requires a new capability/model approval for future event rules; it cannot be injected into old artifacts. Confirmed unavailable-before-write can use existing `TRANSIENT_FAILURE`/NONE. Native receipt replay semantics remain unchanged. Host package checks must recognize the exact tested old/new model-producer pairs supported by runtime 0.3.0. Keep compiler 0.2.0 for old-catalog authoring and compiler 0.3.0 for guard authoring, or explicitly schedule migration of that authoring catalog; no fabricated producer identities.

After independent review and implementation tests, the manager owns delivery to the existing running demo: preserve URL/settings/data, back up any required migration and verify served assets plus real ALLOW/DENY behavior and existing post-commit rules. This design makes no live changes and does not certify that delivery.
