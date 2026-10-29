# 0002: Product proposition and value evaluation

Date: 2026-10-28

Status: accepted framing in project discussion.

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

## Evaluation

Test three questions:

1. Can one approved catalog express realistic, materially different customer requests?
2. What does it cost the host to publish useful hooks, capabilities, types and contracts, and how much of that cost is reused?
3. Do existing extension mechanisms, including vendor-prewired hooks, rules or workflow builders, and scripting or webhook hooks, meet the same needs with less total effort?

Track marginal host code and added host operations per customization, plus catalog reuse rate and one-time catalog cost. Keep comparisons on the same customer requests and host data. A working compiler alone does not establish economic value.

The task demo should include behaviors with different operation or dependency shapes. A second condition on the same `get → search → create` sequence does not by itself prove the composition thesis. CEL portability and resource limits remain technical release gates regardless of product differentiation.
