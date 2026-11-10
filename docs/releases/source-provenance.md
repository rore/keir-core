# Release provenance after the history cleanup

Published package hashes and embedded source revisions describe the packages that were actually built. Keep those original values in compatibility records, archive manifests and consumer provenance. Changing a Git commit ID does not produce a new package or qualify a rebuild.

## Original revisions and reachable source

The history cleanup changed commit metadata and ancestor IDs. The nine historical Core and companion source commits used by the packaging tools have byte-identical complete file trees in their reachable counterparts. [The source revision receipt](../../scripts/release-source-revisions.json) records both IDs and the shared tree ID. The receipt changes where source is read; it does not change the claimed origin of an existing binary.

For example, Core 0.3.0 was built from `b2329440dd2a25acc547f3ebfea214748d912022`. Its equivalent reachable source is `ec994bfa6df98714f334e95d8739cc412b1da503`. The released packages and Daymark's vendor provenance retain the original ID and original checksums. Use the reachable ID to inspect that same source in a fresh clone.

The companion builder resolves the receipt for Git reads and verifies its tree pin before use. Historical manifests still contain their original source and compatible Core IDs. Editions 1–5 retain their exact archive encoding and published hashes. Unknown unavailable revisions are rejected; the tools do not fetch old history or silently choose a branch tip.

Offline archive verification needs the independently trusted archive digest and no Git checkout. A release build still requires `--core-assets` and verifies every recorded package digest. Reconstructing a historical archive in an offline regression test establishes byte identity, not a new installation or package qualification.

## Licensed companion packaging

Historical companion ZIPs through edition 5 omitted the root project license. When redistributing those unchanged ZIPs, provide the [Apache 2.0 license](../../LICENSE) alongside them. Do not silently replace an existing published ZIP or change its trusted digest.

Edition 6 is an **unpublished candidate** that retains edition 5's Core 0.3.0 contracts, schemas and examples, adds the complete Apache license inside the ZIP, and gives the archive its own version and digest. Its candidate compatibility row makes no released-package qualification claim. See [building the companion](../agents/authoring-companion.md#licensed-candidate-edition-6).

Publishing a licensed companion requires reviewing and qualifying a new release, including its real package inputs. Existing releases, attached assets and original provenance remain unchanged.

## Rebuilds and remaining release checks

For a new Core build, supply an explicit reachable source commit to the [release tools](../releasing.md). Treat the output as a new build with its own source provenance, version, checksums and compiler/Java/Node qualification. Do not describe it as the original published package merely because the source trees match.

Before publication, verify the actual release archives and installed Java runtime consumer path with their original package bytes. Dependency resolution or a compiler/Node smoke test alone does not establish that Java package check. Review retained Actions artifacts separately, including visual content where applicable. Old pull-request refs and external copies can retain pre-cleanup history; a clean branch/tag scan does not clear them. Changing visibility and publishing releases are separate decisions.
