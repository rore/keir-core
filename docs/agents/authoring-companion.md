# Choose and verify an authoring packet

<a id="semantic-references"></a>
Use the packet matching the host's actual compiler, runtime and model. The packet supplies language instructions, schemas and examples; the host separately supplies its trusted model, authorized tools and approval flow.

## Choose an edition

| Host packages/profile | Guide | Availability |
| --- | --- | --- |
| Core 0.5.0, numeric source/model `3/4` or retained `2/3` | [Edition 9](customer-authoring-guide-v9.md) | Corrected packet published with [Core 0.5.0](https://github.com/rore/keir-core/releases/tag/v0.5.0). [Exact packet/source record](../releases/core-0.5.0.md). |
| <a id="development-edition-8"></a> <a id="edition-8"></a> <a id="qualified-candidate-edition-8"></a> Core 0.4.0 | [Edition 8](customer-authoring-guide-v8.md) | Archive published with [Core 0.4.0](https://github.com/rore/keir-core/releases/tag/v0.4.0). |
| <a id="licensed-candidate-edition-7"></a> Core 0.3.1 | [Edition 7](customer-authoring-guide-v7.md) | Archive published with [Core 0.3.1](https://github.com/rore/keir-core/releases/tag/v0.3.1). |
| Core 0.3.0 | [Edition 5](customer-authoring-guide-v5.md) | Published with [Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0). <a id="licensed-candidate-edition-6"></a> Edition 6 is a separate unpublished licensed source candidate. |
| Core 0.2.0 | [Edition 4](customer-authoring-guide-v4.md) | Published; matching historical contract. |
| Core 0.1.0 | [Edition 3](customer-authoring-guide-v3.md) | Historical; use only with that host contract. |

Editions 1–2 describe earlier preview contracts. Use the [exact compatibility matrix](../specs/compiler-artifact.md#source-and-artifact-identity), not an edition number alone. Archive names and manifest statuses can retain preparation-time `candidate` or `release` wording; neither determines public publication. Frozen guides and archive bytes stay unchanged.

<a id="qualified-candidate-edition-9"></a>
The published edition-9 packet retains its qualified bytes and hashes. Its frozen guide retains preparation-time status; use this catalog and the exact [provenance record](../releases/source-provenance.md#authoring-companion-reproduction) for current selection.

## Verify before loading

Obtain the archive's SHA-256 independently from its trusted release or qualified packet record, then verify before extracting/loading it:

```sh
python scripts/authoring-companion.py verify "$ARCHIVE" --version "$EDITION" --sha256 "$PIN"
```

Verification checks the trusted digest, exact inventory, manifest and payload hashes. Match the manifest's contracts and producer to the host, then load only the named guide. Supply the model and tools separately; a packet grants no execution authority.

<a id="build-and-verify-the-effect4-authoring-companion"></a>
<a id="build-and-verify-the-authoring-companion"></a>
<a id="build-from-committed-inputs"></a>
<a id="record-release-assets"></a>
<a id="reproduce-historical-edition-4"></a>
[Reproduction inputs, pins and optional package checks](../releases/source-provenance.md#authoring-companion-reproduction) belong to the provenance record. [Daymark's package record](https://github.com/rore/keir-demo-app/blob/main/docs/package-provenance.md) identifies its adopted packet and packages.
