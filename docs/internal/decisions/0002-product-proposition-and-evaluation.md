# 0002: Product proposition and value evaluation

Date: 2026-10-28

Status: accepted framing in project discussion.

Updated 2026-10-29: the user confirmed the product value from prior product experience and removed the separate frozen-catalog/value-validation exercise from the implementation plan. This cancels that research prerequisite; it does not claim the proposed experiment was completed.

## Proposition

Keir enables SaaS applications to offer safe, AI-assisted customization to business users. The host defines extension points and exposes approved capabilities once. Customers compose those capabilities into new behavior without customer-specific vendor code or execution of arbitrary generated code.

The responsibilities are distinct:

| Role | Responsibility |
| --- | --- |
| Host | Decides where customization is possible, which capabilities are available, and what permissions and guarantees apply. Owns agent integration and activation. |
| Keir | Provides the constrained language, compiler, simulation and runtimes. Validates and executes customer-authored extensions within host-defined boundaries. |
| AI agent | Helps nontechnical users express requirements, clarify intent, generate extensions, propose test scenarios and review results before activation. It does not grant capabilities or approve its own work. |

The agent integration belongs to the host, not Keir's core library. Manual authoring remains supported. The host must enforce its real authorization and capability guarantees; Keir cannot certify them from declarations alone.

CEL evaluates expressions inside Keir. It may also be used inside another extension architecture. The comparison is between ways of providing extensibility, not between Keir and CEL.

## Implementation direction

Proceed with a small working core and real host integration. Composing approved capabilities at defined extension points is an accepted product premise. A frozen catalog, competitor comparison or proof of market/economic value is not required before implementation.

The catalog may evolve with implementation needs. Versioned catalog/artifact compatibility, host authority, expression limits, result consumption and failure semantics still require technical verification. This is distinct from freezing one catalog to validate the product proposition.

Use representative customizations to exercise the implementation and make embedding understandable. Do not turn those examples into a mandatory business-value study. CEL portability, Java/Node conformance and resource limits remain technical release gates. No adoption, ROI or production-safety claim is established by accepting the product direction.
