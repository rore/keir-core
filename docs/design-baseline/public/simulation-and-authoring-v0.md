# Keir authoring integration, simulation, approval, and testing v0

**Package baseline:** v0.4 (public working draft, 28 October 2026). The language remains at semantic profile v0; the pre-release source identifier is now `keir/v0`. No new execution instruction is introduced.

**Draft product/runner protocol | architecture v0.4 | 28 October 2026**

**Status:** Proposed behavior for the Node task-management POC. No complete agent authoring workflow, Keir runner/simulator, or customer approval system has yet been built. The Java-to-Node CEL expression interchange has separate verified CI evidence, but **does not test this protocol end-to-end**.

## 1. Authoring happens in the SaaS web application

Design time is not a developer's build process. A customer chats with an agent **inside the web product** and asks for a new behavior. The Node backend operates the agent loop, retains the session, fetches the host-approved DCM, and invokes the bundled Java compiler CLI on each candidate IR. The Java CLI is a **local child process in the single application deployment**, not an HTTP microservice.

```text
Customer: natural-language requirement
    -> Node backend agent requests closed IR only
    -> Java compiler against immutable, host-selected DCM
          | errors: structured diagnostics -> bounded agent repair
          | success: immutable, portable, unsigned artifact
    -> agent proposes test scenarios and expected effects
    -> Node runner simulates exactly the compiled artifact using fake adapters
    -> customer compares expected behavior with actual trace and revises if needed
    -> human/customer approves an exact artifact and selected scenario revision
    -> host creates trusted approval/signature and activates the extension
    -> task events execute the same artifact with real adapters
```

The backend MUST bound agent iterations, tool calls, wall time, token spend and artifact sizes. When an agent cannot resolve diagnostics, it must report the precise blocker instead of retrying indefinitely. The AI agent cannot create new domain operations, edit the approved DCM, certify adapter guarantees, choose its own trust key, or approve activation on behalf of the customer.

**Approval is not a side effect of successful compilation or passing agent-authored tests.** The user reviews a proposed extension and expected behavior independently. Later reauthoring results in a new version requiring recompile, retest and reapproval.

### Agent-independent core, agent-first experience

An agent is **not a Keir library component**. A developer can author IR manually and use the same compiler and simulation APIs; a host may also provide a visual designer. AI is the preferred business-user authoring interface because it can clarify ambiguity, elicit examples, generate valid IR, act on structured compiler diagnostics and explain simulation behavior. The host controls the agent's permissions, iteration limits and tools. Only hosts that explicitly support endpoint-registration proposals may expose such a tool; Keir never registers an endpoint.

Keir MAY publish an optional *agent skill* and thin CLI/MCP wrappers for capability discovery, compilation and simulation. These resources do not introduce an LLM dependency, endpoint registration or authoring lifecycle into the core compiler/runner.

### Hook decision testing

For a host-defined `PRE_OPERATION` decision hook, scenarios must cover `ALLOW`, `REJECT`, missing facts, invalid patch/result, typed business rejection, transient timeout and the host's preconfigured fallback. Tests must assert the host *actually prevents* a denied native operation and revalidates any approved patch; merely returning `REJECT` from the interpreter is insufficient. For response transforms, verify completeness and protected-field preservation; for post-commit hooks, verify a failed reaction cannot retroactively cancel the original commit. Host-only endpoint security, egress and real timeout enforcement belong in integration tests, not fake-adapter simulations. See [Hook contract](hooks-and-tenant-capabilities.md).

## 2. Scenario specification: data, not executable logic

A scenario provides deterministic invocation input, fake capability outcomes, clock and expected observable execution. It does **not** add new program steps, run code, resolve arbitrary URLs, grant capabilities or alter the runner's semantics. The same compiled artifact is used in both simulation and production, and the **same Node interpreter** executes it.

Example (the complete fictional fixture is in [examples/followup-created.scenario.yaml](examples/followup-created.scenario.yaml)):

```yaml
scenarioVersion: '0.1.0'
id: payment-followup-created
program: followup-extension.yaml
entrypoint: TaskCompleted
given:
  input: {taskId: task_invoice, completedAt: '2026-10-28T09:00:00Z'}
  clock: '2026-10-28T09:00:00Z'
  hostContext: {tenantId: tenant_demo, actorId: user_demo, invocationId: event_001}
stubs:
  - {step: source, capability: tasks.get, outcome: SUCCESS, value: '<complete typed Task>'}
  - {step: existing, capability: tasks.search, outcome: SUCCESS, value: {items: []}}
  - {step: created, capability: tasks.create, outcome: SUCCESS, value: '<complete typed Task>', effectState: CONFIRMED}
expect:
  termination: SUCCESS
  result: {status: CREATED}
  calls: [tasks.get, tasks.search, tasks.create]
  effects: [{step: created, kind: task.create, state: CONFIRMED}]
  branches: {created: true}
review:
  state: proposed
```

This abbreviated sketch is explanatory. In real fixtures, all `value`s MUST conform to the declared DCM types and the expected call sequence includes validated arguments, not just function names. The included sample file uses complete typed values for its small task model.

## 3. Deterministic fake-adapter behavior

- Match a fake response by `(stepId, capabilityId/version, invocation ordinal)` and verify its approved arguments. Missing, extra, out-of-order or type-incompatible calls are simulation failures, not implicit successes.
- Model `SUCCESS(value, evidence?)`, `BUSINESS_REJECTION(code, data?)`, `TRANSIENT_FAILURE(code)`, and `UNKNOWN_COMMIT(code, evidence?)`. Mock write effect evidence is `NONE`, `CONFIRMED` or `UNCERTAIN` and must match the chosen outcome.
- Use a deterministic host clock with explicit time semantics. Task completion timestamp is authoritative business input; wall time, event time and durable due date are not interchangeable.
- The simulator MUST NOT fabricate host idempotency, a committed write, authorization proof, deduplication, customer intent, or a rollback. Fake adapters simulate a contract but cannot **certify** it.
- Assert exact artifact version, plan digest and environment; never simulate a modified IR after activation and imply it was the reviewed compiled artifact.

## 4. Expected values, calls, effects, branches and trace

Simulation compares, in order: termination class; typed return or its absence; exact capability-call order, versions and authorized arguments; observed intended/confirmed/uncertain effects; guard outcomes and skipped calls; extra/unconsumed stubs. Report deterministic discrepancies rather than silently adjusting the test or rewriting the compiled plan.

A trusted runner emits trace events such as `STEP_START`, `GUARD_EVALUATED`, `CAPABILITY_CALLED`, `CAPABILITY_OUTCOME`, `EFFECT_RECORDED`, `STEP_SKIPPED`, `RETURN` and `TERMINATED`. Each includes a sequence number, plan/run IDs, step ID, approved capability ID/version, outcome class, appropriate evidence digest and aggregate budget counters. Traces SHOULD redact principal/tenant identifiers, secrets, raw task text and sensitive arguments by default. The runner records **write intent before the adapter call**; a timed-out or unknown response remains `UNCERTAIN` rather than being declared absent.

**Error semantics:** A handled business rejection may return a typed terminal record while the run classification remains `BUSINESS_REJECTION`. `TRANSIENT_FAILURE` stops without automatic retry. An uncertain mutation stops with `UNCERTAIN`, *no regular return*, no next step and no assumed rollback. Host-specific reconciliation of a truly uncertain remote mutation is outside the synchronous v0 language.

## 5. Minimum scenario suite for the public demo

| Case | Expected outcome | Important evidence |
|---|---|---|
| Qualifying invoice task, no match | `CREATED` | `get` → `search` → one `create`; confirmed effect |
| Qualifying task, follow-up already exists | `NO_OP` | Two reads, `create` guard false, zero writes |
| Task without `invoice-sent` tag | `NO_OP` | No write; read/search may still occur in simple v0 program |
| Source task not found | Terminal typed `SOURCE_MISSING` | Preserve underlying `BUSINESS_REJECTION`; no create |
| Concurrent requests / repeat completion event | One follow-up at most | **Real adapter test** of DB atomic uniqueness and replay, not only fake simulation |
| `tasks.create` definite business rejection | Declared business terminal result | No fabricated successful effect |
| `tasks.create` lost response / `UNKNOWN_COMMIT` | `UNCERTAIN` | One attempted write, no automatic retry, no `CREATED` |
| Unapproved capability/host parameter | Compile rejection | Stable diagnostic, zero runnable artifact |
| Expensive/unbounded expression | Rejected or enforceably bounded | No unsupported runtime-cost promise |

A future `FocusListRequested` extension adds read-only projection tests. A Java runner, once built, MUST pass the same **compiled-artifact** conformance suite as Node, with equal observable results and classifications.

## 6. Customer review and activation record

Store separately but immutably linked:

- Customer request and reviewed intent summary.
- Agent-authored IR source and its digest, compiler diagnostics/revisions and the successful compiled artifact digest.
- Approved DCM/entrypoint, compiler/CEL policy and runner/adapter versions, host certifications and capability/effect grants.
- Scenario fixture version(s), customer's expected outcomes, simulation traces/diffs and reviewer identity/approval timestamp.
- Host-generated approval record and cryptographic signature, made **after** the customer explicitly approves the exact artifact.
- Activation/deactivation/version history and current pointer for the relevant tenant/entrypoint.

A changed IR, DCM, expression engine, artifact, contract binding or permissions may invalidate approval. The host keeps signing keys and trusted public-key config away from the agent. One signed artifact can still be revoked or made incompatible by current host policy; the runner checks live authorization on every invocation.

**UI minimum:** request/edit chat; IR or readable behavior preview; structured compiler diagnostics; proposed scenarios; expected-vs-actual result/effect trace; approve/reject/activate/disable. No generated UI builder or fancy workflow canvas is needed.

## 7. Testing is layered: do not confuse passing one layer with another

1. **IR structural and static compiler tests:** Closed syntax/duplicate keys, CEL-Java parse+check, nominal/unit overlay, forbidden fields/effects, entrypoint grants, error/quotas and provenance.
2. **CEL interoperability conformance:** Real Java-to-Node CheckedExpr fixtures covering the supported subset. This layer has already passed a limited suite in [CI #40698821668](https://github.com/rore/bear-account-demo/actions/runs/40698821668) (25 semantic fixtures, 46 harness checks), not the entire CEL language or complete extensions.
3. **Runner conformance:** Node now and Java later, loading the same compiled artifact, producing the same results/calls/effects/classifications across identical fakes.
4. **Business scenario simulation:** Customer-reviewed expected behavior and failure injection; same artifact and Node interpreter as production with deterministic fake adapters.
5. **Real host adapter integration/concurrency/security tests:** Atomic uniqueness, versioned write conditions, tenant access checks, replay scope, ambiguous outcome evidence, backend cardinality and timeouts. A fake adapter and a signed plan do not establish these properties.
6. **Authoring usability and economic value:** Measure agent repair cycles, approval comprehension and the number of genuinely distinct programs using unchanged capabilities, rather than only test pass counts.

## 8. Scope and deferred features

No durable wait/resume, scheduling, user-generated HTTP/SQL, automatic retries, effectful loops, compensation, or atomic transactions across independently committed operations. The host may deliver a new authoritative event at a later time; the extension itself stays a short invocation. Due dates are data fields, **not** scheduled extension execution.

The first POC is limited to multiple reads/calculations plus **one host-controlled mutation per invocation**. The core IR syntax can list sequential static calls, but any later expansion to multiple persistent writes needs an explicit crash/replay/partial-effect policy, independent of the expression-language decision.
