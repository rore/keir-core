# Read/2 structural schemas

These Draft 2020-12 schemas describe decoded JSON instances for the supported read/2 profile. The IDs identify these schema documents; they do not select a profile or imply native acceptance.

| Instance | Schema | ID |
| --- | --- | --- |
| Source | [source.schema.json](source.schema.json) | `urn:keir:schema:read-2:source:1` |
| DCM | [dcm.schema.json](dcm.schema.json) | `urn:keir:schema:read-2:dcm:1` |
| Reference artifact | [artifact.schema.json](artifact.schema.json) | `urn:keir:schema:read-2:artifact:1` |

The artifact schema's environment tuple records the current qualified snapshot: CEL Java `0.14.0`, CEL Node `0.6.1`, protobuf Java `4.35.1`, and protobuf Node `2.6.2`. Native pin and environment checks remain authoritative.

These maintained structural definitions accompany the [language](../../docs/specs/language.md), [DCM](../../docs/specs/domain-model.md), [compiler artifact](../../docs/specs/compiler-artifact.md), [generic `/2` delta](../../docs/specs/generic-hook-capability-contract.md) and [bound-model contract](../../docs/specs/host-model-binding.md). A changed schema meaning requires a new schema-document revision. Source remains `keir/v0`; select `keir-read/2` in trusted compiler configuration. Do not add schema IDs or profile fields to source. Read catalogs can publish unused WRITE operations, but a read program cannot call them. Optional policy is a restriction, not authorization.

Schema success checks structure only. It does not validate raw JSON/YAML syntax, duplicate keys, token spelling (including numeric token `1.0`), Unicode handling, parser depth, canonical bytes, UTF-8 byte limits, hashes or pins, CEL, types, references, dataflow, policy composition, host approval, authority, or native callbacks. These checks remain with the native compiler, runtimes, and host. No Python YAML decoder is used, and schemas add no runtime dependency.

| Boundary | Structural schema | Still required natively |
| --- | --- | --- |
| Records and local bounds | Required/unknown fields, variants, identifier/hash/base64 lexical forms, map/array ceilings and mathematical quotas | Strict decoding, duplicates, token spelling, Unicode and UTF-8 byte limits; `maxLength` counts code points |
| DCM and instructions | Declared shapes, READ/WRITE catalog labels, optional policy and CALL/BIND envelopes | Reference/type resolution, exact agent arguments, nominal provenance, dataflow, policy inheritance, aggregate call budgets and zero WRITE calls in read |
| Checked artifact | Current environment snapshot, modelRef shape and base64 spelling | Checked protobuf/AST metadata, canonical bytes, descriptors and independently selected model/artifact pins |
| Host boundary | No serialized approval, invokers, credentials or native context | Model binding, current authorization, protected bindings, deadlines and dispatch enforcement |

The [shared fixtures](../../conformance/read2-structural/) include JSON equivalents of current YAML sources, an actual Java-compiled literal program and structural-pass/native-fail counterexamples. The Java compiler tests check source equivalence and exact emitted bytes; Java and Node test the same artifact and residual expectations. The existing generic fixtures and old profile goldens remain unchanged. Fixture-controlled recomputed pins let semantic mutations reach their intended check; separate pin-negative cases test authentication. Schema diagnostics use `STRUCTURE_INVALID`, a pointer, keyword and fixed message; they do not replace Keir's native diagnostics or their ordering.

## Isolated development check

Create a project-local virtual environment and install the pinned development dependency:

```sh
python -m venv .tools/schema-venv
.tools/schema-venv/bin/python -m pip install -r scripts/requirements-read2-contracts.txt
.tools/schema-venv/bin/python scripts/check-read2-contracts.py
```

On Windows, use `.tools/schema-venv/Scripts/python.exe` in place of `.tools/schema-venv/bin/python`. The checker accepts decoded JSON fixtures and resolves schemas locally; it does not fetch references. Run the normal Java and Node regression commands as well, since structural validation does not replace native validation.

To inspect one decoded JSON document, run `python scripts/check-read2-contracts.py source conformance/read2-structural/source.json` using that isolated interpreter. Use `dcm` or `artifact` for the other documents. This is a developer check, not a host admission API.
