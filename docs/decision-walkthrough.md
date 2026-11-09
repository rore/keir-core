# Allow or deny an operation

A pre-operation extension returns a decision before the application performs a native operation. It can use the proposed input and approved reads. The application consumes the result, checks its own permissions and business constraints, then decides whether it can commit.

This walkthrough describes [Core 0.3.0](https://github.com/rore/keir-core/releases/tag/v0.3.0). See the [version matrix](specs/compiler-artifact.md#source-and-artifact-identity) for package compatibility and the [decision specification](specs/pre-operation-decisions.md) for the complete host obligations.

[Run the standalone Java or Node example](quickstart.md#try-a-pre-operation-decision) to see ALLOWED, DENIED and BLOCKED with separate simulated native-attempt counts.

## Discover the hook

Start with the exact model published by the host. Find an entrypoint with `trigger: REQUEST`, `placement: BEFORE_COMMIT` and `consumer.kind: DECISION`. Read its input, output and available capabilities. Capability names and parameters come from that model; Keir defines no business service for this hook.

For a small example, the host exposes `BeforeOperation` with input `{restricted: Boolean}`. Its output uses these declared types.

```json
{
  "Decision": {"kind": "enum", "values": ["ALLOW", "DENY"]},
  "DecisionResult": {"kind": "record", "fields": {"decision": "Decision"}}
}
```

The output is a closed record with a required `decision` field. The enum contains exactly `ALLOW` and `DENY`, in either order. A host can also declare bounded scalar fields, such as a plain-text denial message. The extension cannot add undeclared fields or use them to override the decision.

The decision hook has a write allowance of zero, including when `effectGrant` is omitted. An extension cannot call a WRITE even behind a false guard. The host may publish WRITE capabilities for other hooks in the same catalog.

## Draft and compile

A customer asks to block restricted operations. With the input-only model above, the decision can be expressed by this source fragment.

```yaml
steps:
  - id: restricted
    let: {cel: 'input.restricted'}
return:
  record:
    decision: {cel: 'restricted ? "DENY" : "ALLOW"'}
```

A complete source document also needs its language, exact domain identity, entrypoint and name. The checked-in [input-only source](../conformance/keir-3/source-guard-input.json) and [READ-based source](../conformance/keir-3/source-guard.json) supply those fields and a declared plain-text message using the [example model](../conformance/keir-3/dcm-guard.json). See [Source document](specs/language.md#source-document). Compile against the host-selected `/3` model using compiler 0.3.0. Use diagnostic codes and paths to repair the draft. A changed model or source requires a new compile.

If the request needs a lookup, discover an approved READ in the model and call it with its declared arguments. Use the returned typed field to select the decision. The host supplies protected parameters and authorizes the lookup. Do not invent a capability or hide a mutation behind a READ adapter.

## Simulate and review

Agree on expected behavior before running the exact compiled artifact with simulated implementations.

| Case | Decision consumption | Native behavior to observe separately |
| --- | --- | --- |
| Unrestricted input, valid completed ALLOW | `ALLOWED` | Reach fresh native validation once. Commit only if it succeeds. |
| Restricted input, valid completed DENY | `DENIED` | No native operation attempt. |
| Denied, failed or malformed READ | `BLOCKED` | No native operation attempt. |
| Handled business rejection returning ALLOW | `BLOCKED` | No native operation attempt. |
| Expired or cancelled attempt, including a late ALLOW | No continuation | No native operation attempt. |
| Permission or read fact changes after ALLOW | Prior decision remains a decision | Host blocks at the commit boundary. |
| Native validation or commit fails after ALLOW | `ALLOWED` | Native failure reported separately. |

The shared scenarios include [input ALLOW](../conformance/keir-3/scenarios/guard-input/guard-input-allow.json), [input DENY](../conformance/keir-3/scenarios/guard-input/guard-input-deny.json), [READ ALLOW](../conformance/keir-3/scenarios/guard/guard-allow.json), [READ DENY](../conformance/keir-3/scenarios/guard/guard-deny.json) and [a handled rejection returning ALLOW](../conformance/keir-3/scenarios/guard-handler/guard-handler-allow.json). That last case consumes as BLOCKED.

For a READ-based extension, include both lookup values, denial and failure. If the output declares a message, test its type and plain-text rendering. Record outcomes, calls, consumption and independently observed native results separately. Simulation proves the behavior of the harness; real persistence and race guarantees need host integration tests.

Present the exact artifact, model identity and expected and observed results for review. Approval and activation are separate host-owned decisions. A recompiled artifact needs its own approval.

## Connect the native operation

Use the existing [bind, prepare and invoke APIs](specs/host-model-binding.md#api-shapes). The host keeps this sequence in its own control flow.

1. Authenticate the request and snapshot the proposed values. Resolve the current approved selection. Authoritative absence takes the ordinary native path; a selected extension that cannot be loaded or prepared blocks the attempt.
2. Invoke with the business input and fresh protected context. For this REQUEST hook, `event.id` identifies the operation attempt and `event.source` its host-defined scope. Neither asserts that an operation has committed.
3. Consume the host's own invocation outcome. The example `consumeDecision(outcome)` returns `{status: "ALLOWED", result}`, `{status: "DENIED", result}` or exactly `{status: "BLOCKED"}`. It validates the full outcome and declared result, empty effects, classification and consistent trace. Only valid `COMPLETED` ALLOW permits continuation. A handled `BUSINESS_REJECTION` still blocks.
4. Before native commit, recheck selection and approval, permissions, proposed values, business invariants and mutable facts used by the decision. Changed facts block this attempt. Then use the application's existing native commit path once.

`ALLOWED` is permission to continue. It is not a commit receipt. Store or report the decision and native result separately. Never accept a customer-supplied outcome as the result of the host's invocation.

Reserve time for the native checks and commit. If the enclosing expiry is `D` and the host reserve is `R > 0`, guard expiry is at most `D - R`. Block when no positive guard budget remains. Carry the original absolute expiry through nested work; do not restart the budget after a lookup. Cancellation closes the attempt, and a late result cannot reopen it.

READ callbacks must be actual reads. The host owns their permissions and consistency with the native write. Recheck local predicates inside the native atomic transition. A remote fact requires a real host consistency protocol; a successful earlier read alone is insufficient. Do not hold a long-lived write transaction open while evaluating a remote extension.

[Embedding guide](embedding.md) · [Decision contract](specs/pre-operation-decisions.md) · [Authoring assistants](agents/extension-assistant.md)
