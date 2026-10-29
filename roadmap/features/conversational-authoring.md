---
id: conversational-authoring
title: Add customer-facing extension authoring and approval
status: queued
priority: medium
commitment: committed
milestone: Author and activate customizations through AI
repo: keir-demo-app
---

## Summary

Let a business user describe behavior, inspect simulations and the compiled artifact's behavior, then approve an exact revision for activation.

## Why

The host-owned AI agent makes Keir accessible without requiring customers to write IR. Keir Core stays agent-independent.

## In Scope

Bounded generation and repair, compiler diagnostics, scenario proposals, artifact-derived behavior preview, independent expected-outcome review, immutable approval, activation, disable and rollback.

## Out of Scope

Generated UI, agent certification of host contracts, agent self-approval and endpoint registration.

## Done When

- A customer request produces candidate IR, deterministic simulations and reviewable trace/effect differences.
- Human approval binds the exact artifact and scenario revision; edits require renewed review.
- The active artifact can be disabled or rolled back.

## Notes

Implementation lives in `keir-demo-app`. This follows [real task integration](demo-task-integration.md).
