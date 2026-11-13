<a id="customer-extension-assistant"></a>
# Build a customer authoring assistant

Implement the conversation and tools around Keir. The assistant proposes a rule; the application checks it and owns approval, activation and execution.

<a id="task-and-inputs"></a>
## Inputs

- The host's exact model, supported compiler/runtime profile and [matching authoring packet](authoring-companion.md).
- An allowlist of owner-scoped discovery, compilation, simulation and submission tools, with strict argument schemas.
- Saved draft/revision identity, independent expected examples and the current user's authorized scope.

<a id="guarded-results-require-matching-host-support"></a>
<a id="numeric-authoring-requires-the-numeric-profile"></a>
Use the matching guide for syntax, types, conditional results and numeric rules. Do not copy language rules into provider prompts from an unrelated edition or infer support from current repository source.

<a id="business-user-interaction"></a>
## Workflow

1. Ask for missing business choices. Resolve names through authorized discovery rather than inventing identifiers, operations or fields. Ask before replacing an unsupported request with another behavior.
2. Draft source and compile it against the host-selected model. Preserve diagnostic codes and paths so the assistant can repair the proposal. A changed source/model needs a new compile.
3. Agree on expected matching, nonmatching and failure cases independently of observed results. Simulate the exact compiled artifact with controlled service responses; no real writes belong in this step.
4. Compare complete expected and observed outcomes, calls, decisions and write evidence. Preserve rejection, skipped calls and uncertain effects as distinct states. A passing error-case example does not prove every failure safe.
5. Present what the rule will do, the exact checked revision, example results and coverage limits. The user approves; the host separately activates that exact version. Edits need new checks and approval. The assistant cannot approve or activate itself.

<a id="example-authoring-session"></a>
## Example

For “create a follow-up after an eligible completion,” discover the host's completion hook and approved read/create operations. Ask which project and title to use. Check a matching completion, another project and an unavailable read. Report simulated creations as examples, then hand the checked proposal to the host's review flow. Task services are this host's operations, not built-in Keir features.

<a id="authority-and-tools"></a>
## Tool and integration checks

- Validate every tool argument, current owner and exact draft/version on the server. Keep protected identity, original request key and deployment credentials out of authored arguments.
- Keep compiler output, saved evidence and approval bound to their exact model/artifact bytes. Reject malformed or incomplete evidence rather than turning it into eligibility.
- Bind real or simulated operations as the appropriate complete set. Runtime execution uses approved compiled bytes without another AI request.
- Test failed discovery/compilation, rejected examples, late responses, changed drafts and lost-response recovery. A reload must not repeat a paid turn or native write automatically.
- <a id="author-a-pre-operation-decision"></a> For before-save decisions, compare decision consumption and native save results separately. ALLOW still requires the host's own permission, consistency and commit checks.

<a id="earlier-examples"></a>
<a id="first-worked-customization-classify-a-task"></a>
<a id="implementation-reference-earlier-task-examples"></a>
Earlier examples are retained as [original read-only source](../internal/history/compatibility-reference.md#original-read-source), [simulated-write scenarios](../../conformance/single-effect/README.md), and a [catalog-bound follow-up](../internal/history/compatibility-reference.md#earlier-follow-up).

[Host integration](../embedding.md) · [Simulation contract](../specs/single-effect.md#ordered-simulation-protocol-and-acceptance) · [Before-save decisions](../specs/pre-operation-decisions.md)
