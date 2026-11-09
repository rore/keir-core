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

Expose one optional `TaskBeforeCreate` selection per owner using the existing selection/approval lifecycle. The authored rule can restrict its behavior to a chosen project through the proposed project ID. Genuine absence permits the ordinary path; broken active rules block. Recheck the selection generation at commit even when the original selection was absent, so a newly enabled rule cannot be bypassed by an in-flight create.

Project normalized `projectId:ProjectId`, `priority:Priority`, `tags:Tags`, `hasNotes:Boolean` and `hasDueDate:Boolean` into the hook input. Reuse current priority values/default and the existing bounded Tags list (at most eight normalized strings). Human/API tags use native normalization; automation's existing `tagsText` uses the existing parser to obtain the same list. Do not join all native tags into a new 256-byte string: valid native tag sets can exceed that combined size, while each element fits the existing list type. No string splitting or new CEL operation is needed.

`hasNotes` is true exactly when the normalized native notes have non-whitespace content; `hasDueDate` is true exactly when native validation has produced a non-null valid due instant. These general presence facts avoid copying long notes and retain native timezone/date validation. They are not customer-specific predicates. Actor, request key, origin and invocation depth remain trusted host context. Freeze the full native proposal privately, including title and values not visible to the rule. All native field limits remain unchanged; never truncate to fit a guard projection.

The three required rules need no new project metadata READ. Resolve the customer's project choice through the existing authorized authoring host, then compare its typed ID with the authoritative proposed `input.projectId`. Native project authorization remains mandatory before commit, including for a no-call rule. Do not add `projects.get` or a Project result record for this slice. Existing `tasks.get` and `tasks.openMatch` remain available for customer READ composition and its qualification.

The combined current `/3` catalog stays within the existing **16 named types**: start with the 14-type `/2` model, replace identical `CreatedTask` and `UpdatedTask` records with one `TaskReference` record `{id:TaskId}` (13), then add `TaskCreateProposal`, `Decision` and `TaskCreateDecision` (16). Update both write capability result references only in the new model; their wire result remains `{id}`. Retain all nominal identifiers, both distinct status enums, and the complete `TaskEvent` including required `occurredAt:Timestamp`. Old `/2` model/artifact/approval bytes remain unchanged. No Core cap increase, new capability or weakened time behavior is needed.

The three required examples are ordinary authored conditions, not a host catalog of special handlers:

| Example | Authored denial condition | Example message |
| --- | --- | --- |
| Client-tagged tasks need a due date | `"client" in input.tags && !input.hasDueDate` | Add a due date for this client task. |
| High-priority tasks need notes | `input.priority == "high" && !input.hasNotes` | Add a note for this high-priority task. |
| A chosen project requires the review tag | `input.projectId` equals a typed customer-selected ProjectId and `!("review" in input.tags)` | Add the review tag for this project. |

Tags uses the existing String element type; project constants use existing nominal literals. An author may combine the conditions in one program, choosing a deterministic first denial message. There remains exactly one active rule for the trigger. Project selection comes from the authorized host catalog, never a name guessed by the assistant. The project choice and condition belong in the program, not a customer-specific host handler.

Daymark declares output `{decision:Decision,message:String}` using ordinary DCM fields; ALLOW uses an empty message, DENY supplies the correction text. A blank DENY message uses safe generic host text. Display bounded messages as plain text only, associated with the exact owner/submitted attempt/selected revision. Do not interpret message text as a field identifier or permission to bypass a guard. No new correction-field protocol is needed: keep the detailed creation form editable and show the message. Native validation/permission errors and unavailable checks keep their own host text and status.

## Consistency without a new transaction framework

The first guard catalog supports bounded local native READs whose values/predicates can be rechecked synchronously. Capture each reached READ's exact normalized arguments and returned projection in the private invocation closure (at most four entries). Before insert, rerun those same authorized native queries inside the existing write transaction and compare their complete typed values. A changed result blocks the attempt as stale; do not automatically rerun the guard. Recheck native project/owner permissions as well. This covers negative existence predicates, not just rows that happened to exist. The input snapshot must also be the one inserted.

Do not expose that read set in the DCM, artifact or result. Do not introduce generic database version vectors. Reads that cannot be rechecked under the native consistency boundary are unavailable for this first authoritative guard. An unavailable selected rule blocks; it never becomes a false lookup or no binding. No remote calls or asynchronous adapters run while holding the write transaction. Test concurrent changes from a separate connection, including an absent match becoming present and an activation changing during evaluation.

Qualify real READ composition with the existing `tasks.openMatch` operation using an authored bounded title and proposed project ID; `tasks.get` can also read an authorized existing task selected by the author. These qualification programs do not add a fourth required product rule or expose native title/notes. Revalidate their read facts immediately before insertion under the transaction lock. After insertion, recheck authority, selection, cancellation and deadline only: the intended insertion may itself change an `openMatch:false` result. Add a regression proving that expected self-change succeeds, while a conflicting external change before the transaction blocks. Use the real packaged compiler/runtime and native READ adapter, not only a stub.

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

Keep the existing `/2` event model, approved artifacts and receipts intact for execution/history when installing the new runtime. Publish one current `/3` authoring catalog containing the existing event entrypoints and the new guard, with compiler 0.3.0 as the only current authoring tool. New/edited event rules and guard rules compile against that catalog and require new ordinary approval; existing selected event rules keep their original exact `/2` execution binding until explicitly replaced. Do not rebuild old approvals or silently change event capability versions to add guard-specific error codes. Existing `tasks.create@2` adapters can map confirmed guard denial to their existing declared `TASK_INVALID`/NONE result with separately stored host diagnostic detail. A new public guard-specific error requires a new capability/model approval for future event rules; it cannot be injected into old artifacts. Confirmed unavailable-before-write can use existing `TRANSIENT_FAILURE`/NONE. Native receipt replay semantics remain unchanged. Host package checks must recognize the exact tested old/new model-producer pairs supported by runtime 0.3.0; no fabricated producer identities or old compiler authoring path.

## Host-audit findings and remaining recovery decision

The read-only audit at `16c7f5c` matches the two live creation paths above. Direct source inspection confirms the transaction helper commits immediately after its callback returns, so making that callback async would commit before guard completion. It also confirms title-only quick-add and a success handler that clears the title without checking whether the draft changed. The implementation must fence that handler by submitted draft/owner/project generation, preserving newer edits.

Interactive creation currently has no stable create key or result readback contract. The automation receipt design cannot be claimed for it. The following host-only proposal closes that gap without adding a generic recovery service. It requires independent review before implementation and does not hold up generic Core work.

## Interactive create identity and readback

Use one native terminal-result table for interactive/API creation. A task ID alone cannot prove a rejected attempt stayed rejected, distinguish a still-running request from absence, or compare the original complete proposal after the task is edited. In-memory deduplication is lost on restart. The minimum durable record is `(owner_id, operation_id)` as primary key, canonical normalized candidate bytes, and immutable terminal result JSON. No pending rows, claims, leases, queue, periodic recovery, expiry policy or generic operation framework.

### Identity and payload

The host UI generates a random UUID for an explicit Add attempt before sending it, keeping that UUID with the frozen submitted candidate. API clients supply the same operation metadata. Carry it in a required `X-Task-Create-Id` header on `POST /api/tasks`; keep the current business body shape. Pass it separately into the host service, never through source-visible fields or an optional bypass flag. A direct internal native-create caller supplies the same explicit metadata; the after-event adapter continues using its existing original event key and receipt path instead of this new table. Missing/malformed IDs fail before guard execution; client knowledge of an ID grants no authority.

Normalize once using existing native rules. The stored closed candidate is `{projectId,title,notes,tags,priority,dueAt}` with defaults materialized, canonical key ordering, exact string contents after existing normalization, ordered normalized tags and nullable native due instant. Compare complete canonical bytes, not title, a subset, a mutable task row or only a digest. Preserve existing native limits and Unicode behavior; use an encoding that losslessly round-trips every accepted native string rather than silently normalizing it further. Existing automation payload recipes remain untouched.

Same owner + ID + same candidate means the same attempt. Same owner + ID + different candidate is `409 CREATE_PAYLOAD_MISMATCH`, with no new mutation or disclosure of stored fields. Different IDs identify separate attempts even when candidates match. Do not merge them by content or reuse one ID after editing. The server derives owner from the current authenticated session, never the body/header. Every POST/readback requires the existing session-context and owner checks; recheck that captured session remains current immediately before native commit using the same host-local current-session pattern already used by authoring. No Core authenticator is added.

### Terminal result and atomicity

Store one of these closed host results; these are native attempt outcomes, not Core consumer statuses:

| Result | Meaning |
| --- | --- |
| `{state:"CREATED",task:TaskDto}` | Actual task snapshot returned by the committed creation. Save it atomically with task insertion and the ordinary TaskCreated event. |
| `{state:"NOT_CREATED",code,message}` | Terminal confirmed no-create. `code` is one of `RULE_DENIED`, `CHECK_UNAVAILABLE`, `STALE_ATTEMPT`, `NATIVE_REJECTED`; messages follow existing safe native text or the validated rule message. |

Authenticate and normalize, then check for a matching saved result before evaluating the guard. If absent, evaluate outside any transaction. Concurrent requests may both evaluate; this is safe because guards have no WRITE authority and each attempt has the existing work/deadline limits. For **either** proposed terminal result, enter the short existing native transaction and reread the terminal record first. A matching winning result is returned unchanged; mismatch rejects. If still absent, recheck current session/owner and candidate identity. For an ALLOW/no-binding path, also recheck deadline, selection/approval, native invariants and captured read facts before insertion. Save the task, event and CREATED result in that one transaction. For a definitive no-create, save NOT_CREATED instead, with no task or event. First successfully committed terminal result wins; later evaluations cannot change it. In particular a losing DENY must replay a concurrent CREATED result, not report no save.

If result persistence fails, the complete native transaction rolls back. Do not report a definitive outcome until COMMIT succeeds. A process crash or uncertain commit response is UNRESOLVED until readback finds a terminal row. Authentication failure returns the normal security error, not another owner's operation status. Native input that cannot be normalized returns ordinary validation failure before this protocol; it must not be described as terminal cancellation of another in-flight request with that ID. A valid normalized candidate rejected by native checks can receive NOT_CREATED/NATIVE_REJECTED. Changed session cannot commit either a task or an unauthorized terminal record.

Keep terminal records immutable and durable across ordinary restarts and task edits/deletion; never cascade them from task deletion or silently expire them. A later replay reports the original creation snapshot, not the task's current contents, and never recreates a deleted task. Current task access remains a separate lookup. Before either GET readback or matching POST replay returns any stored result, recheck the current session, authenticated owner and access to the original project from the stored candidate. A changed/invalid session returns the existing 401 security response; a deleted/inaccessible project returns a generic 404 without stored task, notes, candidate, rule message or terminal state. Do not substitute NOT_CREATED or UNRESOLVED for this access rejection and do not reevaluate/create on the replay path. Matching owner and UUID alone do not authorize disclosure.

A full destructive demo reset clears these records with tasks under its existing exclusive reset procedure. The inspected reset requires the app data lock and reseeds projects with new UUIDs; ordinary app restart loses its in-memory sessions. On session-context change, suspend any retained attempt until reauthentication and refresh of the authorized project list. Resume recovery only for the same owner and exact original project ID. If that project is absent (including after reset), invalidate the retained attempt/draft binding and disable its retry; never retarget it to the newly seeded/default project. A direct POST with the original candidate also fails native project authorization before creating a terminal record or task. Verify these transitions using the real reset path in an isolated test; do not assume a notification or session invalidation that the server does not perform. Backup, migration verification and reset inventory include this one table. No reset-generation framework is added. Do not modify old automation receipt rows.

### API and UI behavior

Retain `201 TaskDto` for CREATED, including a same-attempt replay, so existing success rendering can consume the actual saved task. Return confirmed terminal no-create with `operationId`, `state:"NOT_CREATED"`, `code` and `message`; use 422 for rule/native rejection, 503 for unavailable and 409 for stale attempt. Those HTTP statuses alone are not proof: only a validated terminal envelope for the matching attempt is. Bare server errors, lost responses or a 409 payload mismatch do not mean no task was saved.

Add one owner-authenticated read-only endpoint, `GET /api/task-create-operations/:operationId`. It returns the saved terminal result with `operationId`, or exactly `{operationId,state:"UNRESOLVED"}` when absent. It never starts a create. Absence cannot prove no commit: an original request may still be evaluating or waiting for its transaction. Never display absence as NOT_CREATED. A matching POST may safely be explicitly retried with the **same** ID and identical frozen payload; it either replays the winner or becomes the sole committed winner. No automatic resubmission loop, new ID or edited-payload retry while the original result is unresolved.

After NOT_CREATED, retain the draft; a user-requested correction/retry is a new attempt with a new ID. After CREATED, clear only the matching submitted draft generation and refresh the authorized current task list. The returned DTO proves the original save, not current task state: never append/merge a recovered snapshot into the live list, resurrect a deleted task, or overwrite later edits. Say the original save was confirmed; display/open a task only from fresh authorized current data. If it is now absent, explain that distinction without recreating it. Use the same refresh behavior for ordinary success so same-attempt replay needs no extra response flag.

After uncertainty, retain both the current editable draft and the separate frozen attempt ID/payload, offer readback, and do not let newer edits overwrite the unresolved attempt. A user may explicitly retry that frozen attempt after readback remains unresolved; label this as checking/retrying the same attempt, not adding another task. An owner/project switch must not render a late result in the new view or leak another owner's saved draft. Access rejection suspends recovery rather than confirming no-save. This slice does not promise durable browser drafts across page reload; losing the ID removes automatic recovery and is never permission to silently create again.

### Focused acceptance

- Suppress a response after commit, restart the server, reauthenticate as the same owner and read back/retry the same ID: same saved DTO, one task, one TaskCreated event, one terminal record.
- Pause two connections after evaluation, race the same ID/candidate through ALLOW/ALLOW and ALLOW/DENY: one immutable winner and matching replay; no false NOT_CREATED after a winning creation. Different payload with the same ID rejects.
- Crash/fail before and during result insertion: task/event/result all roll back or all commit. An absent readback remains UNRESOLVED while an original request can still finish.
- Reuse after NOT_CREATED, change draft, edit/delete the resulting task, switch owner/session, lose project authority, and exhaust deadline: no duplicate, rewritten result, leaked snapshot or stale UI clearing. New explicit attempts after confirmed no-create use new IDs.
- Read back and POST-replay CREATED after task edit/deletion: confirm the original save, then refresh current state without restoring the deleted row or replacing edits. After project deletion/access loss, both endpoints return the generic access rejection with no saved data; a different owner gets no result. Verify session changes during response handling cannot disclose or render stale data.
- Run the actual exclusive reset against isolated data while retaining a browser attempt: reauthentication/project refresh finds a different project UUID, invalidates the old binding and disables retry. The old candidate cannot create in the new default project; ordinary restart without reset still permits same-owner/same-project recovery.
- Preserve existing automation key/payload/version mismatch and replay bytes exactly. The new table is not consulted for automation and never changes its UNKNOWN/CONFIRMED/NONE evidence.

The host implementer owns the exact migration/table schema and validates these cases in isolated native persistence tests. The reviewer checks this concrete protocol before code-facing adoption; no user permission gate or new Keir feature is required.

After independent review and implementation tests, the manager owns delivery to the existing running demo: preserve URL/settings/data, back up any required migration and verify served assets plus real ALLOW/DENY behavior and existing post-commit rules. This design makes no live changes and does not certify that delivery.
