---
id: "public-onboarding-readiness"
title: "Make public onboarding reproducible and documentation current"
status: "active"
priority: "high"
commitment: "committed"
milestone: "Release an embeddable Keir library"
repo: "keir-core"
---

## Summary

Close the first-use and documentation gaps identified by the expanded review of Core `b038bf4` and Daymark `d1eaea9`. Preserve the existing Core README, quickstart, embedding and normative-specification structure. This follow-up does not reopen completed release qualification or authorize repository visibility changes.

## Scope and ownership

- Documentation owns both repositories' entry paths, consumer-installation guide, current-versus-historical guidance, supported examples and consequential limits, settled license/stack choices, setup/troubleshooting/privacy explanations and verified walkthroughs. Studio extraction remains a proposal; teaching-model helpers are described at their actual scope. Preserve released assets, compatibility material and notices.
- Core development owns the literal source-quickstart defect: the documented pack/install sequence omits the integrity refresh performed by CI. Reproduce it in isolation, provide one durable setup path shared with CI, and retain integrity checking. Verify both existing examples and the edited-title flow. Also verify the canonical DCM-fingerprint procedure and report a genuine tooling gap before proposing a new interface.
- Documentation and development coordinate on a stable, version-neutral release-installation guide. Consumers select an explicit release and use its published asset names, checksums, prerequisites and compatible contracts. Keep the verified 0.3.0 compiler/Node/Java consumer walkthrough as a separately versioned example and qualification record; routine releases should not require rewriting the installation process. Reuse existing release checksums and notes, without inventing metadata interfaces, commands or registry availability.
- The existing demo developer retains the Java-prerequisite CI failure and Studio corrections. Do not duplicate that assignment. Final walkthrough labels, screenshots and current-head evidence follow accepted implementation and hosted verification.
- The manager owns this roadmap, dependencies and combined acceptance. Independent technical review applies to engineering changes; documentation cannot silently change runtime contracts.

## Acceptance

- [ ] A clean checkout follows the literal published setup commands and runs both examples, including an edited title, without disabling integrity or relying on a CI-only workaround.
- [x] A consumer verifies release assets and runs a minimal compiler/Node/Java host outside the source tree using documented commands. Verification used qualified release copies and cached third-party dependencies; anonymous and empty-cache downloads remain unverified.
- [ ] A host builder can obtain the canonical fingerprint for a new or changed DCM through a verified procedure; any needed tool addition has an engineering review.
- [ ] Current documentation distinguishes implemented behavior, proposals and history; states the actual license, stack, distribution, provider requirements and consequential limits; and clearly links language, DCM, runtime and assistant guides.
- [x] Current conformance and test claims identify exact release/source evidence. Failed or incomplete current-head runs are not replaced by old successful counts.
- [ ] The Daymark walkthrough matches accepted UI behavior and explains approval, activation, replacement and provider-free execution. The existing owner supplies fresh resolved CI evidence.
- [ ] Literal commands and links are verified at delivered revisions. Anonymous landing-page, asset and cross-repository checks remain a separate post-visibility action after the user chooses to publish.

## Status

The reviewed documentation increment is integrated: Core `0827fb4` adds released-package installation outside the source repositories, an annotated source/model tour, assistant entry guidance, current corpus orientation and release-specific verification evidence. Daymark contribution `127f09f` corrects license/stack/setup/privacy and implementation-versus-proposal guidance; it is integrated with the pending Studio source, with its authoring paragraph reconciled to preserve the new behavior explanation and all three trigger choices. Changed-page link/anchor, privacy and whitespace checks passed. The [review disposition](../../.agent-workflow/tasks/expanded-public-doc-review.md) retains all 18 findings and their limits.

The three-file shared source setup/CI correction passed independent plan and result review and is integrated as `41a11a5`; see [the setup record](../../.agent-workflow/tasks/public-onboarding-source-setup.md). All 18 focused tests pass, including actual native Windows npm with spaces, stale cached tar replacement and idempotent refresh. A clean source archive passed the helper, source compiler/runtime builds and all six Node/Java follow-up, decision and edited-title runs. Exact manifest/lock restoration and existing CI coverage remain intact. Final published quickstart replay and actual hosted POSIX execution remain open. Released-compiler fingerprint evidence covers new/changed models, all control characters and stale pins, but its supported documentation procedure remains open. The documentation owner is separating stable installation instructions from versioned examples following the user's correction. Studio fixes, exact final UI documentation and fresh hosted evidence continue separately. The original running demo remains unchanged.
