<a id="effect-4-structural-schemas"></a>
<a id="effect4-structural-schemas"></a>
# Structural schemas for single-write behavior

Internal engineering reference: these Draft 2020-12 schemas describe decoded JSON values for single-write behavior. Exact profile selectors and wire identifiers are implementation details; see the [implementation reference](../../docs/internal/implementation-reference.md).

| Instance | Schema | ID |
| --- | --- | --- |
| Source | [source.schema.json](source.schema.json) | `urn:keir:schema:effect-4:source:1` |
| Domain capability model (DCM) | [dcm.schema.json](dcm.schema.json) | `urn:keir:schema:effect-4:dcm:1` |
| Reference artifact | [artifact.schema.json](artifact.schema.json) | `urn:keir:schema:effect-4:artifact:1` |

The IDs version the schema documents. Source uses `keir/v0`; trusted compiler configuration selects the profile. Keep schema IDs and profile selectors out of source. The artifact schema records the compiler and dependency versions currently supported for preparation.

| Structural check | Native check |
| --- | --- |
| Closed records, local map and array limits, identifier and digest spelling | Strict JSON or YAML parsing as appropriate, duplicate keys, numeric token spelling, Unicode, UTF-8 byte limits and canonical bytes |
| Model and event metadata, optional capability policy and effect grant | Exact model identity, nominal types, reference resolution, policy narrowing and available host implementations |
| CALL and BIND shapes, guard spelling and handler envelopes | Prior Boolean BIND references, exact agent arguments, declared error codes, output types and dataflow |
| Artifact pins, descriptor recipe and base64 spelling | Bound-model equality, canonical base64, checked CEL protobuf and AST validation, aggregate call, write and resource limits |

The host selects and approves the exact artifact bytes. The runtimes enforce invocation limits and the host verifies current permission, the original request key and native commit behavior. Schema success supplies structural evidence only.

The [shared single-write examples](../../conformance/generic-hooks/effect4/) include the standard, no-call and title-composition artifacts. The [structural fixtures](../../conformance/effect4-structural/) add a guarded call with a declared error handler and cases that pass the schema but fail native preparation. Structural errors use `STRUCTURE_INVALID`. Native diagnostics report the rejecting phase and retain their existing code and path. For an undeclared handler, Java reports the error-code field and Node reports the handler map.

## Run the offline check

Use the existing [project-local setup](../read-2/README.md#isolated-development-check) and pinned development dependency. With that interpreter, run both structural suites.

```sh
python scripts/check-read2-contracts.py
```

[Validate one single-write document](../../docs/internal/implementation-reference.md#check-one-single-write-document).

The checker also accepts `dcm` and `artifact`. It resolves references locally. Use the native compiler and runtimes for the checks described in the [language](../../docs/specs/language.md), [model](../../docs/specs/domain-model.md), [artifact](../../docs/specs/compiler-artifact.md) and [binding](../../docs/specs/host-model-binding.md) contracts.
