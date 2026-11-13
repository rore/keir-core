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


## Authoring companion reproduction

Use the exact companion input and independently trusted archive pin. These identify historical bytes, not the current checkout or a grant of installed-host support. Published availability is maintained in the [companion catalog](../agents/authoring-companion.md).

| Edition | Companion input | Trusted archive SHA-256 |
| --- | --- | --- |
| 9, qualified staged packet | `9435dfef2ade61af2d7617754c761775502d68b5` | `c1ca8b6f2f3c5089afca5e134d0b21ed2477a570141c36fcc1ea55a2f6c41867` |
| 8 | `dca92178dad7a9a4f65312128afec15abf88ce67` | `4433908295095612c495b11447170fe542758db06acc4b1ce253359a10ac2676` |
| 7 | `bdd273112c7a86d255082c1de545a943cc2d87e6` | `8a4101a64fbbf4d09b59f936083a48df39256fcba8386cee66bd5f7bcad2c4a7` |
| 6, source candidate | `f6b51d4e36643b368452e7246fde6a78fda63999` | No trusted published archive pin; a build prints its own candidate pin. |
| 5 | `f988c2b147933fcdd8b8f74ce7920f78acd4eca5` | `20249307192494acb1cf07cc002e7203bda4f6baf8a8c2257dd752ce1d8cfe44` |
| 4 | `c6a6d171982514569c29c06de7a429d301d25b2a` | `4a9f4f30212ff1d722cf6c993902d0802bee7e7ffebc00304102a5964b5bec63` |

Build from the selected input rather than an arbitrary HEAD:

```sh
python scripts/authoring-companion.py build "$ARCHIVE" --version "$EDITION" --revision "$REVISION"
python scripts/authoring-companion.py verify "$ARCHIVE" --version "$EDITION" --sha256 "$PIN"
```

For a compatibility record with a nonempty package map (editions 4, 5 and 9), supply the exact three pinned Core package files with `--core-assets path/to/core-assets`. Candidate records 6–8 keep an empty package map and omit that option. The builder refuses to overwrite an archive. Verification checks the trusted digest before inventory, manifest and payloads; it works offline without Git.

The [accepted feature record](../../roadmap/features/numeric-values-and-rules.md#manager-accepted-core-contribution) links the completed package/companion checks and tested Daymark adoption. Frozen documents retain their preparation-time status. Reproducing that packet requires its recorded input revision, not this mutable documentation tree.

The corrected edition-9 packet includes the revised guide and all 35 payloads, including the guide-linked specification index. It maps the unchanged staged Core packages from `3a383c2d0c6227eedc9c03927c2dd1910360d5b9` to both numeric and retained authoring profiles. Its manifest's `release` status is package-compatibility metadata, not public publication. Earlier packet pins must not identify this replacement. See [Daymark provenance](https://github.com/rore/keir-demo-app/blob/main/docs/package-provenance.md#replacement-companion-9) for the exact manifest/guide hashes and recorded upstream verification.

Editions 1–5 retain their original bytes and source IDs; include the root [LICENSE](../../LICENSE) when redistributing a legacy ZIP that omitted it. Earlier recipes, semantic-reference notes and release-asset recording examples remain in the [pre-simplification catalog](https://github.com/rore/keir-core/blob/3eefc59183c93efe0bce6bb56e7100a43fa06bc1/docs/agents/authoring-companion.md). No historical revision or package pin is relabeled by a current documentation change.
