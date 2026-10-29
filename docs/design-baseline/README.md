# Keir: Controlled Extension IR - Design Package v0.4

**Updated 28 October 2026.** **Keir** is the project name; *Controlled Extension IR* describes the closed capability language, compiler and runtime. **Agent-independent core, agent-first optional SaaS experience.** Public-facing, domain-independent working design. **Markdown files are the source of truth.** This package contains only fictional task-management scenarios and publicly documented library/tooling references. It is not an implementation, security certification, or claim of commercial adoption.

## Start here

1. **[Keir Product Definition & Architecture](public/keir-architecture.md)**: walled-garden thesis, closed extension semantics, host/agent boundaries, compiler/runners, SaaS authoring and implementation roadmap.
2. **[Keir Extension IR v0](public/keir-language-v0.md)**: detailed agent-authored IR grammar, CEL profile, error/dataflow semantics and complete follow-up example.
3. **[Domain Capability Model v0](public/domain-model-v0.md)**: types, grants, host-bound fields, domain contracts, versioning and fictional DCM.
4. **[Compiler / Artifact / Runners v0](public/compiler-artifact-and-runners-v0.md)**: Java compiler CLI, portable compiled plan, checked CEL protobuf, Node runner, trust and limits.
5. **[Simulation & Authoring v0](public/simulation-and-authoring-v0.md)**: host-optional agent loop, fake adapters, scenarios, review/approval and trace/assertion layers.
6. **[Task Management POC](public/task-management-poc.md)**: useful single-deployment Node SaaS host with reusable task capabilities and concrete extension scenarios.
7. **[Decisions & Evidence](public/decisions-and-evidence.md)**: current decisions, verified CI evidence, unresolved blockers and next milestones.
8. **[Hooks and Tenant Capabilities](public/hooks-and-tenant-capabilities.md)**: host-defined synchronous interception/result contracts and optional tenant-scoped registered endpoint capabilities.

## Included runnable-design fixtures

- `public/examples/task-domain.yaml`: fictional typed DCM with **pending** host certifications.
- `public/examples/task-domain.sha256`: illustrative digest computed by an example-only canonicalization recipe.
- `public/examples/followup-extension.yaml`: full agent-/human-authored structural Keir IR program (`keir/v0`).
- `public/examples/followup-created.scenario.yaml`: simulation stub and expected trace/effects.
- `public/schemas/extension-ir-v0.schema.json`: draft structural JSON Schema. This is **not** a complete semantic validator.

## Confirmed interoperability result

[Successful GitHub Actions run #40698821668](https://github.com/rore/bear-account-demo/actions/runs/40698821668) demonstrates CEL-Java 0.14.0 -> standard checked protobuf -> CEL-ES 0.6.1 execution for the **tested restricted expression subset** (25 semantic fixtures, 46 harness checks). This does not certify the IR compiler, nominal/unit checking, runner resource isolation, signed activation or real domain adapters.

## Build-versus-reuse decision (provisional)

Implement a small interpreter for compiled `Bind`, `Guard`, `CapabilityCall`, terminal return and typed outcome transitions. Reuse existing CEL, Protobuf, parsing, schema and cryptographic libraries. Do not add a general-purpose workflow engine unless a concrete use case (durable state, parallelism, richer recovery) or comparative implementation demonstrates real net savings. This is an engineering direction, not a finished benchmark.

## Publication and maintenance

All packaged files have been reviewed for domain-specific case-study references and non-public example endpoints. The demo domain is fictional. Before releasing an implementation, conduct an ordinary code, dependency, license and security review; design approval does not certify runtime safety.

Keep selected decisions, source-evidenced findings, pending requirements and completed tests distinct. When a change affects compiler, artifact, runner or approval semantics, update the design register, normative documents, fixtures and conformance tests together.

**Change log:** v0.3.1: public-facing consolidation, minimal-runner reuse decision, updated interoperability status in task POC. v0.3: first integrated design and successful restricted-CEL CI.

**v0.4 changes:** Named Keir; documented the closed/walled-garden product thesis, agent-independent core, optional agent-first business-user interface, host-defined hooks/result consumers, optional tenant-specific capability overlays/registered endpoints, and a future `BeforeTaskCreate` interception example. Updated the draft IR language tag to `keir/v0` and retained the verified CEL/Node compiler architecture and public-only examples. No new runtime implementation or certification is claimed.
