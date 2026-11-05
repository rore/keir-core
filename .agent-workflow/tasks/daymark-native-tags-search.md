# Add native task tags and project search to Daymark

<!-- agent-workflow:start -->
**Outcome:** A Daymark user can tag tasks and find matching tasks in the selected project with extensions and AI disabled.

**Target:** keir-demo-app

**Scope:** Native task details, project task search, the authorized API and additive SQLite storage, with focused tests and native app documentation.

**Constraints:** Preserve current tasks, customization data, receipts, normal task lists and exact runtime projections. Keep production data and the existing preview untouched during implementation.

**Completion criteria:** Tags persist across restart. Search returns matching open and completed tasks in stable bounded pages, keeps owner and project scope, and handles empty results and retries. A populated existing database upgrades without changing prior data.

**Requirement baseline:**
{"source":"work-record-initial","outcome":"A Daymark user can tag tasks and find matching tasks in the selected project with extensions and AI disabled.","scope":"Native task details, project task search, the authorized API and additive SQLite storage, with focused tests and native app documentation.","constraints":"Preserve current tasks, customization data, receipts, normal task lists and exact runtime projections. Keep production data and the existing preview untouched during implementation.","completion_criteria":"Tags persist across restart. Search returns matching open and completed tasks in stable bounded pages, keeps owner and project scope, and handles empty results and retries. A populated existing database upgrades without changing prior data."}

**Relationships:**
{"schema_version":1,"sources":[{"provider":"minimap","scope_ref":"roadmap:v1:git:github.com/rore/keir-core#roadmap","local_ref":"item:v1:task-organization"}],"pull_requests":[]}

**Risk:** Elevated

**Complexity:** Moderate

**Reason:** Native source and test paths are Gray under shared Redline. The additive migration touches existing data and the new query must preserve owner scope. The test plan includes isolated upgrade and recovery evidence.

**Discovery:** The existing task list is unpaged and shared by native navigation and completion. The owner/project index and versioned migration ladder provide the starting point. Baseline native data, API and effect-storage tests passed on b0e5e429.

**Material assumptions:** Bounded per-task tags fit an additive JSON column. A literal scan of one authorized project is adequate for local demo data. Check a sparse-query plan and measurement before adding search infrastructure. Recheck the exact demo base and scope at pickup.

**Plan:** In `src/data.mjs`, add an additive transactional migration to store up to eight distinct tags, each up to 32 UTF-16 units. Trim, apply NFC, lowercase, apply final NFC and deduplicate. Use the same normalization for writes and lookup. Reject malformed values, controls and commas before writes. Native edit preserves omitted tags and clears with `[]`. A tags-only save sends only tags and updates only tags and the modification timestamp, preserving stored title and notes bytes. Existing full text edits retain their input shape. Use explicit insert columns and preserve existing runtime projections, native authority and task/receipt writes. In `src/app.mjs`, add an authorized project search route with strict scalar `q`, `tag` and `after`. Require at least one nonblank filter. Match trimmed text up to 120 units as a literal title or notes substring with ASCII case folding and exact non-ASCII case, including text after an embedded NUL. A local SQLite `instr(lower(...), lower(...))` probe found text after NUL. Match normalized tag entries exactly and combine filters with AND. Apply all predicates before fetching 26 matches ordered by `(created_at,id)`. Return up to 25 and use a bounded owner/project-scoped anchor. Invalid syntax is a bad request, and missing or foreign anchors use the same not-found response. Pages use current data after the anchor. New matches before the anchor appear after refresh, and later matches can appear in subsequent pages. In `src/web/main.tsx` and existing styles, add tags in task details and explicit Search/Clear/Load more in the selected project. Keep ordinary task state separate from search pages. Preserve rows and cursor on a failed Load more, fence late owner, project and filter responses, and refresh after confirmed changes without clearing a newer draft. Restore focus to the opening row or search heading if the result disappears. Document in `docs/native-task-app-design.md`. Stop for review if native effect projections, existing database integrity or package contracts require changes. Studio exposure is a separate slice.

**Verification plan:**
- Storage and ownership → test tag-only preservation of existing text bytes, final-NFC equivalence, omit/preserve/clear, blank/invalid filters, literal matches after NUL, 25/26+ pages, sparse late matches, timestamp ties and foreign access/cursors. Record query plan and timing for a stated synthetic project size.
- Migration and compatibility → use a populated isolated v8 copy and compare original fields and receipts byte-for-byte. Inject failure after the new DDL and verify schema version, schema and rows after reopen. Make synthesized historical fixtures genuinely pre-tags, update current-version assertions and repair positional inserts so they still test their intended behavior. Check reopen/reseed and run existing native effect regressions.
- Business-user flow → test tagging, search, clear, Load more, retry with existing rows and cursor, task edits and focus fallback on desktop and phone with AI and extensions disabled. Exercise delayed search responses across owner, project and filter changes and refresh without discarding newer drafts. Run the build and fast suite.

**Plan review:** Agent technical review: see [Plan review](#plan-review) below.

**Approvals:** Not required at this risk level. Updating the running preview and its data requires separately scoped authorization.

**State:** Ready to implement
<!-- agent-workflow:end -->

## Plan review

Clean-context review against Daymark `b0e5e429` required explicit text-byte preservation, historical fixture repair and rollback, query completeness, and UI retry and focus checks. The reviewer accepted the revised plan after those corrections. The pinned SQLite probe matched text after an embedded NUL. Daymark `60d0853` changes the authoring route in the shared app module and leaves the native route, task service and Tasks UI unchanged. The reviewed plan applies to that base.
