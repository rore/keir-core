# Core 0.5.0

Status: [Core 0.5.0](https://github.com/rore/keir-core/releases/tag/v0.5.0) was published on 2026-11-14 at 08:58:37 UTC as a normal public release, not a draft or prerelease. Tag `v0.5.0` resolves to `3a383c2d0c6227eedc9c03927c2dd1910360d5b9`.

## Exact numbers in portable rules

Core 0.5.0 adds signed int64 Integer and exact Decimal values to the compiler and both runtimes. Decimal uses normalized coefficient precision up to 38 and scale 0–18, not fixed scale 9. Arithmetic checks each intermediate result. Division is exact unless the author explicitly selects a rounded operation, scale and mode. There is no implicit numeric promotion or silent rounding.

Business numbers cross host boundaries as canonical strings. Java and Node expose opt-in exact text-normalization helpers: `100.00` can become `100` without rounding. Supplied bounded-list size supports rules such as requiring notes when a new task has more than three normalized tags. Hosts still own normalization, permissions, native validation, units, currencies and accounting policy.

The [stock and quote examples](https://github.com/rore/keir-core/blob/main/examples/numeric/README.md) show quantity thresholds and explicitly rounded percentages. The [installation guide](https://github.com/rore/keir-core/blob/main/docs/installation.md) covers the released ZIP/tarball layouts. Java compiler/runtime use JDK 21; the Node runtime uses Node.js 24. Maven resolves the Java consumer dependencies. Python 3.12 is used by the supplied build/verification tools, not by ordinary runtime invocation.

## Supported profiles and adoption

| Authoring profile | Source / model | Artifact / scenario | Descriptor recipe |
| --- | --- | --- | --- |
| Numeric | `keir/3` / `keir-dcm/4` | `/4` / `/4` | `keir-record-descriptors/3` |
| Retained nonnumeric | `keir/2` / `keir-dcm/3` | `/4` / `/4` | `keir-record-descriptors/2` |

Both use producer `keir-compiler-java/0.5.0`. Retained nominal String names such as Integer and Decimal keep their original meaning. New runtimes retain the exact historical 0.2.0/0.3.0/0.3.1/0.4.0 execution tuples; older runtimes reject the new profiles. See the [complete compatibility matrix](https://github.com/rore/keir-core/blob/main/docs/specs/compiler-artifact.md#source-and-artifact-identity).

Adoption is explicit. Recompiling creates new artifact bytes requiring fresh pins and approval, even for retained authoring. Existing artifacts, model snapshots, selections and approvals are not rewritten or transferred. The one-possible-write ceiling and zero-write pre-operation decisions remain unchanged. This publication does not deploy Daymark, change repository visibility, publish registry packages or authorize a live provider request.

## Assets and provenance

Release tag `v0.5.0` targets the actual original Core package source `3a383c2d0c6227eedc9c03927c2dd1910360d5b9`. These are the unchanged qualified package bytes, not a rebuild from mutable documentation main. Corrected companion edition 9 is a separate archive built from `9435dfef2ade61af2d7617754c761775502d68b5`; its manifest maps to those exact Core packages. GitHub's generated source archives are not the binary assets or the complete companion source input.

| Asset | Bytes | SHA-256 |
| --- | ---: | --- |
| `keir-compiler-cli-0.5.0.zip` | 8109727 | `ec36271caaa55714257601c2f8bc7aa19f2ced76858f02b15021dacd3a138e76` |
| `keir-java-runtime-maven-0.5.0.zip` | 102558 | `88e9f903b32df239832e7c0a91b2db3a445a8e31bb5066f54578773c8b890e77` |
| `keir-runtime-node-0.5.0.tgz` | 26305 | `9c59e4722f4d85bcd0e57580a1cef9526f7ceb52670e345576e39ca29d489736` |
| `LICENSE` | 11558 | `1eb85fc97224598dad1852b5d6483bbcf0aa8608790dcc657a5a2a761ae9c8c6` |
| Core `SHA256SUMS` | 362 | `58b025d40c8e43c9a7ce18ad7f000e45a2414fe0a318545dc9542f645d1922df` |
| `keir-authoring-companion-v9.zip` | 246443 | `c1ca8b6f2f3c5089afca5e134d0b21ed2477a570141c36fcc1ea55a2f6c41867` |
| `keir-authoring-companion-v9.sha256` | 72 | `fb5f43a31f2f642a598786f9808f98a4af65c0e959ae08b57fe8ad46d7f5ebe9` |

Core `SHA256SUMS` covers the three Core archives and license. The companion has its own trusted pin sidecar; the Core checksum file is unchanged. Companion manifest SHA-256 is `a98c013fe2221073f93b280f8bc0223b49bd298ff7bc910e69a2dec97387a74b`. All 35 payloads, including the revised guide and its linked specification index, match the qualified input. Frozen guides retain preparation-time wording; the [live catalog](https://github.com/rore/keir-core/blob/main/docs/agents/authoring-companion.md) records publication. No earlier archive or hash is replaced.

## Qualification and limits

- [Exact-source package qualification](https://github.com/rore/keir-core/actions/runs/43438170269) used Core source `3a383c2`, distinct from workflow source `bfeaae3`. Installed Java and Node each matched 130 complete independent outcomes and their callable helper checks.
- [Final full qualification](https://github.com/rore/keir-core/actions/runs/43449029316), source `3bcba648`, passed 229 Java tests, 528 Node tests, 19 setup tests and 72 packed-host tests. Its package hashes match this release exactly. It also checked historical-runtime rejection, numeric boundaries, forged artifacts, receipt/error behavior and bounded runtime measurements. This does not relabel the recovered packages' original source.
- [Corrected companion qualification](https://github.com/rore/keir-core/actions/runs/43451868303), source `9435dfef`, separately passed 23 recipe tests, actual build, offline verification, payload/source checks and exact-package checks. It did not rerun the prior Core matrix.
- [Daymark's accepted journey](https://github.com/rore/keir-demo-app/actions/runs/43474838743) passed 606 Node tests and all 261 browser tests, with 12 unchanged opt-in skips. It covers scripted authoring, real compilation/simulation, approval, separate activation, retained selections and native task creation paths using these same candidate bytes. Publication does not imply a new live rollout or paid-provider qualification.

Existing installed-package results are reused only because release bytes are identical. Release transport is verified separately. Full numeric qualification is manually dispatched; a historical passing run does not certify every future change. Decimal operand/result reparsing remains uninstrumented. Bounded samples, Java threads, nonparallel Node interleaving and sampled memory are not production capacity, engine-ranking or hard-containment guarantees.

## Dependency advisory assessment

A 2026-11-14 independent review checked the exact shipped compiler dependencies, Java consumer dependency closure and four Node dependencies against public OSV advisories, then inspected supported Keir paths and shaded class references. It found no reachable unmitigated advisory path in those inspected interfaces. The following vulnerable dependency classes remain distributed; this is not a claim of zero vulnerabilities:

| Advisory | Dependency | Assessment for inspected Keir interfaces |
| --- | --- | --- |
| [GHSA-72hv-8253-57qq](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-72hv-8253-57qq), Moderate | Jackson core 2.19.2 | Async number-length bypass; Keir uses bounded synchronous String parsers. |
| [GHSA-r7wm-3cxj-wff9](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-r7wm-3cxj-wff9), High | Jackson core 2.19.2 | Chunked async integer accumulation; no supported Keir async-parser entry. |
| [GHSA-7hhh-6rmp-j9qf](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-7hhh-6rmp-j9qf), High | Jackson core 2.19.2 | Unbounded DataInput error token; Keir does not use that parser entry. |
| [GHSA-p6pp-m3f8-5c89](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-p6pp-m3f8-5c89), High | Jackson core 2.19.2 | Numeric-string coercion regex; inspected Keir and shipped transitive paths do not call it. |
| [GHSA-xxph-c9ww-hj94](https://github.com/google/guava/security/advisories/GHSA-xxph-c9ww-hj94), High in the current database | Guava 33.6.0-jre | Unsafe native Java deserialization; supported Keir ingress is bounded JSON/YAML/protobuf, not untrusted ObjectInputStream. |

GitHub dependency-alert details were unavailable to the release credential. This public advisory assessment does not claim those alerts were read, cleared or dismissed. Public database coverage and bounded static reachability are incomplete evidence, not a formal security proof. Hosts using untrusted native Java deserialization, other Jackson parser APIs or dependency overrides must assess and mitigate those paths separately. An application sharing the Java classpath does not inherit a security approval from this Keir review.

## Distribution verification

Draft release `445150285` contained exactly the seven assets above. Every independently authenticated draft download matched its trusted pre-upload size/hash, original Core checksums and companion map. The existing Core checksum-only verifier and companion offline verifier with exact Core assets passed on those downloads.

After publication, unauthenticated public API readback confirmed the same release, unchanged notes, exact seven-asset inventory, normal published status and latest-release selection. The resolved public Git tag matches the selected source. All seven independently downloaded public assets again matched the trusted pre-upload sizes/hashes; both inert verifiers passed on those public downloads. Accepted installed-runtime qualification is reused for the byte-identical packages, not claimed as a new runtime run. No registry publication, visibility change, live demo update or deployment occurred.
