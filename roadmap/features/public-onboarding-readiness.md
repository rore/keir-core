---
id: "public-onboarding-readiness"
title: "Make public onboarding reproducible and documentation current"
status: "done"
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

- [x] A clean checkout follows the literal published setup commands and runs both examples, including an edited title, without disabling integrity or relying on a CI-only workaround. Fresh Windows source replay passes all six host flows; hosted Linux setup, host and parity checks pass.
- [x] A consumer verifies release assets and runs a minimal compiler/Node/Java host outside the source tree using documented commands. Verification used qualified release copies and cached third-party dependencies; anonymous and empty-cache downloads remain unverified.
- [x] A host builder can obtain the canonical fingerprint for a new or changed DCM through a verified procedure; the reviewed standalone recipe matches six compiler-qualified vectors and rejects ten malformed cases. No runtime/compiler API was added.
- [x] Current documentation distinguishes implemented behavior, proposals and history; states the actual license, stack, distribution, provider requirements and consequential limits; and clearly links language, DCM, runtime and assistant guides.
- [x] Current conformance and test claims identify exact release/source evidence. Failed or incomplete current-head runs are not replaced by old successful counts.
- [x] The Daymark walkthrough matches accepted UI behavior and explains approval, activation, replacement and provider-free execution. Complete hosted verification passes at Daymark 840c5ad, and the running build is verified with saved data/settings preserved.
- [x] Literal commands and links are verified at delivered revisions. Anonymous landing-page, asset and cross-repository checks remain a separate post-visibility action after the user chooses to publish.

## Status

Core onboarding changes are accepted: `0827fb4` adds the annotated source/model tour, assistant entry guidance and current corpus/verification orientation; `e24a7c2` delivers stable installation instructions, the separately versioned consumer proof, the real shared setup command and the reviewed model-fingerprint recipe. Runtime installation does not require the authoring companion. General guides select package versions through release metadata and compatibility references. The manager verified all 15 frozen follow-up file hashes and the actual final Windows replay evidence. Changed-page links, privacy and whitespace checks pass. The [review disposition](../../.agent-workflow/tasks/expanded-public-doc-review.md) retains all 18 findings and their limits.

The shared setup implementation `41a11a5` passes independent plan/result review, 18 focused tests and six clean-source Node/Java host flows. It preserves exact manifest/lock restoration and existing CI coverage. [Hosted Linux contract verification](https://github.com/rore/keir-core/actions/runs/42619792483) passes at `cbc256c`; see [the setup record](../../.agent-workflow/tasks/public-onboarding-source-setup.md). Consumer installation remains qualified within its recorded cache/download boundary.

Daymark `840c5ad` publishes the accepted final guides and Studio source. [Complete hosted verification](https://github.com/rore/keir-demo-app/actions/runs/42626934091) passes 550 Node tests with 12 skips and all 249 browser cases. The existing running demo serves its exact reviewed build; saved native/customization data and provider settings are preserved. This bounded onboarding milestone is closed. The repositories remain private, and anonymous/empty-cache checks retain their stated limits. The two unconfirmed transient UI focus observations remain nonblocking and explicitly recorded in the [Studio delivery](extension-studio-design.md).
