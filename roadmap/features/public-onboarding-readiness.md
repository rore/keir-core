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
- Documentation and development coordinate on the release-installation guide: verified 0.3.0 GitHub downloads, compiler invocation, Node tarball, included Java Maven repository and a minimal host outside the monorepo. No fictional commands or registry availability claims.
- The existing demo developer retains the Java-prerequisite CI failure and Studio corrections. Do not duplicate that assignment. Final walkthrough labels, screenshots and current-head evidence follow accepted implementation and hosted verification.
- The manager owns this roadmap, dependencies and combined acceptance. Independent technical review applies to engineering changes; documentation cannot silently change runtime contracts.

## Acceptance

- [ ] A clean checkout follows the literal published setup commands and runs both examples, including an edited title, without disabling integrity or relying on a CI-only workaround.
- [ ] A consumer verifies release assets and runs a minimal compiler/Node/Java host outside the source tree using documented commands.
- [ ] A host builder can obtain the canonical fingerprint for a new or changed DCM through a verified procedure; any needed tool addition has an engineering review.
- [ ] Current documentation distinguishes implemented behavior, proposals and history; states the actual license, stack, distribution, provider requirements and consequential limits; and clearly links language, DCM, runtime and assistant guides.
- [ ] Current conformance and test claims identify exact release/source evidence. Failed or incomplete current-head runs are not replaced by old successful counts.
- [ ] The Daymark walkthrough matches accepted UI behavior and explains approval, activation, replacement and provider-free execution. The existing owner supplies fresh resolved CI evidence.
- [ ] Literal commands and links are verified at delivered revisions. Anonymous landing-page, asset and cross-repository checks remain a separate post-visibility action after the user chooses to publish.

## Status

The documentation owner has already received the expanded review and is working on it. A bounded Core engineering offer covers the source setup defect and fingerprint-procedure investigation; acceptance is pending. The manager confirmed the quickstart/CI discrepancy in source. Studio implementation and its independent-review corrections continue separately; the original running demo remains unchanged.
