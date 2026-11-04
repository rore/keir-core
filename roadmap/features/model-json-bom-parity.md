---
id: model-json-bom-parity
title: Align native model JSON byte-order-mark handling
status: queued
priority: medium
commitment: committed
milestone: Execute extensions in Java and Node
repo: keir-core
---

## Summary

Reported portability gap: Node's native model parser strips a leading UTF-8 byte-order mark, while Java explicitly rejects it. Verify the compiler/native preparation boundary, confirm one policy and align acceptance with shared byte-level regression coverage. Prefer the existing Java/compiler rejection policy unless contract review identifies a reason to change it.

## Done When

- [ ] Confirm the same BOM-prefixed model's behavior through compiler and both native model-loading APIs.
- [ ] Align the native parser policy without weakening UTF-8, duplicate-key, canonical-artifact or resource checks.
- [ ] Shared boundary regression covers BOM rejection and unchanged valid model bytes/identities in Java and Node.

## Notes

Simulation tooling already rejects BOM-prefixed case/DCM inputs; its earlier delivery deliberately left native production parsers unchanged. This follow-up owns that remaining boundary, rather than reopening the completed simulation correction. Keep separate from [declared-error limit parity](declared-error-limit-parity.md).
