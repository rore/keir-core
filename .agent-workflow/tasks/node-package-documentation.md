# Qualify the Node runtime README and archive notice

<!-- agent-workflow:start -->
**Outcome:** Node runtime readers can find and use the current API, and the archived authoring guide still has a stable public entry.
**Target:** Keir Core Node package documentation and archived authoring guide
**Scope:** The Node package README, the Core Node example integrity lock, implementation references, and the first guide's public notice.
**Constraints:** Keep runtime and package metadata, authority, Daymark vendor and v2 package scope fixed. Use an isolated clean pack and offline install. Preserve the first companion's source and archive pins.
**Completion criteria:** The new pack differs from the pinned predecessor only in its README. A clean offline install uses the new lock and passes the installed example checks. The v1 ZIP rebuilds byte for byte from its existing Git pin.
**Requirement baseline:**
{"source":"work-record-initial","outcome":"Node runtime readers can find and use the current API, and the archived authoring guide still has a stable public entry.","scope":"The Node package README, the Core Node example integrity lock, implementation references, and the first guide's public notice.","constraints":"Keep runtime and package metadata, authority, Daymark vendor and v2 package scope fixed. Use an isolated clean pack and offline install. Preserve the first companion's source and archive pins.","completion_criteria":"The new pack differs from the pinned predecessor only in its README. A clean offline install uses the new lock and passes the installed example checks. The v1 ZIP rebuilds byte for byte from its existing Git pin."}
**Relationships:**
{"schema_version":1,"sources":null,"pull_requests":[]}
**Risk:** Elevated
**Complexity:** Simple
**Reason:** The packaged README and the local example lock are Gray paths. Changing package bytes requires explicit installed-package verification.
**Discovery:** The README ships in the tarball. The Core lock authenticates it. The v1 companion reads its original guide via the pinned Git revision, while Daymark uses its own vendor tarball.
**Material assumptions:** The isolated pinned baseline package and dependency cache are available. The same Node and npm toolchain can pack the clean candidate deterministically. Historical public anchors can be retained at the original paths.
**Plan:** Rewrite runtime-node/README.md around current binding, preparation and invocation. Move older examples to a linked engineering reference and preserve old anchors. Replace the first guide at its public path with an archival link to v2. Stage from committed Core files with just the candidate README overlaid, compare tarball members, update only the example runtime integrity field, and verify a clean offline install. Stop if another package member changes or the archive pin drifts.
**Verification plan:**
Current API and stable links -> check the README against runtime signatures and the installed host examples.
Clean pack and lock -> reproduce the tarball, compare its exact members and verify the fresh offline example install and host checks.
Archive and vendor -> reproduce v1 from its committed source pin and compare the ZIP and trusted digest. Verify Daymark vendor bytes and tracked lock before and after.
**Plan review:** Agent technical review: [Plan review](#plan-review).
**Approvals:** Not required at this risk level.
**State:** Ready for review
<!-- agent-workflow:end -->

## Plan review

The independent plan reviewer passed the Elevated and Simple plan at `74d128f`. The reviewer confirmed all eight predecessor tarball members and the lock pin, the four cached dependency integrities, the first guide's Git source and archive pins, and the unchanged Daymark vendor and lock. The plan preserves the old heading anchors and uses repository URLs from the installed README.

## Implementation

Use a clean staged checkout for packaging. The predecessor used for comparison is the isolated archive authenticated by both locks.

The package README now explains binding, preparation, invocation and write receipts. Historical contracts and examples moved to the compatibility reference. The public v1 path points readers to v2 and to the archive instructions.

## Evidence

- Two offline npm packs from a clean staging tree matched byte for byte. The 22,202-byte tarball has SHA-256 `e2333b0a6baad9dd8bfefa94e9dccc4fa27142211c835265b758ef33be461694` and integrity `sha512-0kDO1B98cu7S5ziw/SgWhEqnUO7tcKGq9XZyhTrYHLEfzl+WtodzMRkxI8zsigtG4nsHsqS4PITllzG8zn7e7w==`.
- Comparison with the independently pinned predecessor found eight members in each archive and only `package/README.md` changed. Package metadata and all six JavaScript files are identical. The Core example lock changes only the runtime tarball's integrity.
- The clean offline `npm ci` installed five packages. All eight installed runtime files matched the new tarball. The four installed host tests and all 11 current quickstart cases passed. The code snippet from the installed README completed with a confirmed simulated write.
- Both public pages retain all seven previous anchors and their links resolve. Daymark's tracked lock and both vendor files matched their pre-task digests.
- The original v1 companion rebuilt byte for byte and verified against `sha256:8e365ff0d14fb0c479e43ace36e941562c94577f793a86b720d54e20622547b6`. Its guide member matches the original Git blob at the manifest's source revision. The archive builder and v2 source are unchanged.
- Redline classified the scope as Gray with zero boundary violations. Diff whitespace checks passed.

## Result review

Agent technical review: [Independent review](#independent-review)

Reviewed revision: `74d128f` plus the candidate with content fingerprint `e2f9c79ed7b005dfe6d8d9672abb1e89c6f1b44468a86d12cd604826302f2090`. The shared branch advanced through unrelated work during verification.

Verification adequacy: Pass. The clean offline install and comparison of archive and installed files exercise the updated integrity pin and package payload. The example and README snippet exercise the current public API. The v1 check uses its independent archive pin and exact source blob.

### Independent review

The reviewer verified the current API and the older references, all seven anchors, the one-field lock change, both candidate tarballs and their member bytes, the installed runtime, the pinned ZIP and Git guide, and Daymark's three file digests. The reviewer independently reran all four installed host tests. No findings.
