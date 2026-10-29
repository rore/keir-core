# Internal engineering records

These records support Keir development. Public contracts and product guidance belong in [specs](../specs/README.md) and [agent guides](../agents/README.md).

- [Design baseline](design-baseline/README.md): all supplied designs and illustrative fixtures kept together, including host integration v0.5 and host-owned multi-effect contracts v0.7. They are review inputs, not implementation or certification evidence.
- [Decisions](decisions/0001-repositories-and-runtimes.md): accepted decisions and rationale, including [product direction](decisions/0002-product-proposition-and-evaluation.md) and [restricted CEL/resources](decisions/0003-restricted-cel-resource-policy.md).
- [Workflow](workflow/agent-workflow-bootstrap-summary.md): bootstrap history, advisory-policy proposal and CI setup. Historical excerpts retain their original paths; active repository configs determine current behavior.
- [Development-plan architecture review](development-plan-review.md): full supplied-design coverage, component/dependency analysis, early gaps and reasoning behind the runnable sequence. Feature state and the accepted execution sequence live in the roadmap.

Add supplied design specifications to `design-baseline/public/` and index them there. Do not create another draft-design directory. Keep implementation design and review rationale internal; publish agreed behavioral contracts in the maintained public specifications and identify the baseline sections they supersede.

The [roadmap](../../roadmap/board.md) owns feature state; internal documents are not a parallel tracker. Internal describes audience, not access control.
