---
id: host-integration-assistant
title: Help a host developer define and approve an integration
status: queued
priority: medium
commitment: committed
milestone: Customize a real task application
repo: keir-demo-app
---

## Summary

Demonstrate the host-design assistant helping the task-app developer propose a domain/hook/capability contract, validate it, inspect host conformance evidence and submit an exact revision for host-owner approval.

## Why

Host integration design and customer extension authoring are distinct tasks and authority levels. Both need usable Keir contracts; the developer-facing workflow should not be hidden inside the later customer chat feature.

## In Scope

A bounded host-owned developer integration using the public host-agent guide and core model validation/diagnostics. Inspect authorized definitions, propose DCM and adapter-binding changes, identify missing evidence and run approved conformance tools. Demonstrate a real proposal/validation/review loop against the task host; a scripted walkthrough is preparation, not proof of live agent behavior.

## Out of Scope

An autonomous agent in core, automatic certification/publication, general API importers, arbitrary source execution, customer permission to edit the catalog, and a separate chat product just for this developer workflow.

## Done When

- A host developer requests a supported integration change and receives a typed, reviewable proposed revision with explicit assumptions and certification obligations.
- Tools expose schemas, scope, effects, limits and diagnostics; proposals can be validated without publication.
- Host-owner approval applies to the exact revision and real adapter evidence. Schema validity and assistant statements do not certify guarantees.
- Customer credentials cannot publish the model; the host assistant cannot approve its own publication or extension activation.
- The approved revision can be discovered by customer authoring without copying internal designs into prompts.

## Notes

Core supplies reusable contracts, validation and conformance; host-specific tools and agent integration live in the demo. Provider, credentials and spend bounds require the normal owner decision before live calls. Both guides start in the first core increment; this feature delivers the host persona's live workflow.
