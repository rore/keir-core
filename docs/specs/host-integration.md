# Read-and-decide host contract and acceptance

For new integration design, use the [generic hook responsibility boundaries](generic-hook-capability-contract.md#responsibility-boundaries): publish the approved catalog separately from hook input/output/lifecycle and optional restrictions. The `/2` contract is proposed; the executable `/1` sample and its required grant-map tests below remain unchanged. Do not generate sample-specific grants as the default design for a generic hook.

Status: implemented local REQUEST/read/classify/display checkpoint. Both packaged hosts exercise completed/open/missing/wrong-resource cases. A full task application, transactional guard, activation system and live agent integration remain later work.

## Definition, invocation and consumption

For the corrected target, the [complete TaskCompleted walkthrough](generic-hook-example.md) separates host payload/catalog/adapters/authority from customer logic/constants, compiler output and runtime invocation. It is proposed `/2`, not a replacement for the implemented `/1` example below. Host payload is defined by the application independently of each extension; a customer's project choice does not require a new hook field.

Host owner approves the [DCM](domain-model.md), adapter registration, caller scope and DISPLAY consumer. Host constructs trusted input and protected bindings, selects the artifact through an independently held digest, prepares once, and invokes through the [runtime API](runtime.md). Only a validated COMPLETED result is displayed. Failure displays a safe error, not a default classification. No result grants permission to perform a native operation.

Sample input is `{"taskId":"task1"}`. Trusted host bindings independently include `{"actorId":"user1"}`. Fake lookup success `{"id":"task1","status":"COMPLETED"}` yields `{"classification":"DONE"}`; status OPEN yields PENDING. Expected consumption is DISPLAYED with the same record. Fake TASK_NOT_FOUND or ACCESS_DENIED yields BUSINESS_REJECTION and NOT_DISPLAYED. These are examples, not real access-control evidence. Simulator uses the same artifact/runtime with explicit fake registration and never substitutes fake certifications into a real host.

## Two product assistant surfaces

The implemented [single-effect host contract](single-effect.md#trace-and-consumer) introduces EVENT_REPORT for post-commit simulation, including handled rejection and uncertain/confirmed effect receipts. It does not change this read DISPLAY consumer. [Ordered effect scenarios](single-effect.md#ordered-simulation-protocol-and-acceptance) use the same compiled artifact and runtime, and cannot grant real-mode admission.

Host integration assistant may draft the model/adapter/consumer definitions and tests in an authorized workspace. Human host owner approves the exact model revision and actual adapter evidence before publication. Customer extension assistant discovers only that approved scoped surface and proposes source/scenarios. It cannot publish a model, choose protected actor values or authenticate its own artifact. Both refer to these contracts; no agent runtime belongs in core.

Minimal future tool contracts: model validation accepts a proposed model and returns safe diagnostics (no publication); discovery accepts authenticated host scope and returns approved model digest, hook/capability descriptions and limits; compilation accepts source with a host-selected model, returning diagnostics or an unapproved artifact reference; simulation accepts an artifact reference and bounded scenario data, returning trace and consumer outcome. Tool authority comes from the host, not text in descriptions. These names describe operations, not shipped CLI/MCP methods. Activation requests and live agent repair loops are outside this increment.

## Acceptance fixtures

Developers own executable files under `conformance/` and examples; this matrix specifies required cases, not claimed results. Each scenario contains host-selected artifact/model references, business input, trusted fixture bindings and ordered adapter responses. The scenario loader cannot expose these protected fixture bindings as customer-authorable production context. Trusted test adapter captures exact agent/protected arguments separately from redacted public traces. Missing, extra, out-of-order or unused response stubs fail the test.

| Case | Expected evidence |
| --- | --- |
| Completed / open task | One read, exact taskId and separately bound actorId, DONE / PENDING, DISPLAYED in both hosts. |
| Change condition in source | Recompile changes behavior without host policy edits; a changed source requires a newly selected artifact digest. |
| Boolean false / empty String | Present values survive projection; missing/null remain rejected. |
| Non-ASCII scalar and escaped control in bounded String | Canonical bytes and equality agree; no Unicode normalization or surrogate acceptance. |
| Missing/inaccessible task | Declared BUSINESS_REJECTION; no result and NOT_DISPLAYED. |
| Transient/exception/invalid adapter result | FAILED; no fabricated decision or next call. |
| Wrong requested task returned | Sample host adapter conformance fails; do not add task-specific runner logic. |
| Unknown capability / write / excess calls / actor override | Compile or admission rejects before unauthorized dispatch. |
| Wrong nominal / undeclared enum / forward name / forbidden AST | Stable source diagnostic, no artifact. |
| Duplicate/unknown key, YAML alias/tag, null, numeric binding, collection | Strict parse/profile rejection. |
| Tampered checked bytes/plan/DCM/descriptor or forged expected digest inside artifact | Trusted external digest prevents load; no dispatch even if local hashes recomputed. |
| Environment/consumer/model pin mismatch, unknown instruction | Prepare rejects; no fallback reinterpretation. |
| Exact ceiling and ceiling+1 at parser/AST/value/trace boundaries | Enforced bounded acceptance/rejection; no truncation. |
| Deadline before dispatch / adapter late response | LIMIT_EXCEEDED, no later call/display; late response cannot change termination. |
| Authorization false / thrown or rejected check / check timeout | AUTHORITY_DENIED / AUTHORITY_CHECK_FAILED / BUDGET_EXCEEDED respectively; no adapter dispatch, no raw exception leak. |
| Clock equality at expiry / regression / invalid clock value | Expiry is inclusive; invalid host clock produces HOST_CONTEXT_INVALID, never a wallclock conversion. |
| Reused Prepared with different users | No identity/value leakage; authorization performed for each invocation/call. |
| Admission/current grant omission, exhaustion or widening | Omission never inherits permission; exhaustion blocks dispatch; invalid or widened current grants fail as HOST_CONTEXT_INVALID. |
| Adapter mode/version mismatch or mutated registration map | Prepare rejects incompatible bindings; later map mutation cannot change prepared bindings. |
| Lower trace quota and malformed/larger host quotas | Trace exhaustion stops before an unrecordable dispatch and preserves termination; invalid/widening quota configuration fails preparation. |
| Determinism | Same source/model/compiler/environment compiled twice -> identical bytes; run that one artifact in Java and Node. |

Compare typed result, execution class/error path, ordered calls/arguments, redacted event order and separate consumption result. Do not compare wall timestamps or claim test fakes certify real host behavior. Qualifications must record actual runtime/library versions, descriptor and CheckedExpr evidence, admitted overloads, limit measurements and limitations. Production readiness and the broader language/DCM roadmap feature remain open even when this increment passes.
