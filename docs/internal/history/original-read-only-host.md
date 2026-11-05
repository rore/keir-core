<a id="read-and-decide-host-contract-and-acceptance"></a>
# Original read-only host examples and acceptance cases

This page preserves the host examples and acceptance matrix for the [original read-only contract](compatibility-reference.md#original-read-source). For current behavior, see [Embed Keir](../../embedding.md) and the [Keir specification](../../specs/README.md).

<a id="corrected-host-bound-single-write-initialization"></a>
## Current host initialization

The [host binding reference](../../specs/host-model-binding.md) defines current initialization, preparation and invocation. The [DCM reference](../../specs/domain-model.md) defines what the host exposes to extensions.

<a id="legacy-read1-example"></a>
## Implementation reference: original read-only example

The [original read-only fixture and packaged hosts](../../../conformance/read-and-decide/README.md) cover completed, open, missing and wrong-resource cases.

<a id="definition-invocation-and-consumption"></a>
### Definition, invocation and consumption

The host owner approves the [original read-only DCM](compatibility-reference.md#original-read-model), adapter registration, caller scope and DISPLAY consumer. The host constructs trusted input and protected bindings, selects the artifact through an independently held digest, prepares once, and invokes through the [original read-only runtime API](compatibility-reference.md#earlier-runtime). Only a validated COMPLETED result is displayed. Failure displays a safe error. The result carries no authority to perform a native operation.

Sample input is `{"taskId":"task1"}`. Trusted host bindings independently include `{"actorId":"user1"}`. Fake lookup success `{"id":"task1","status":"COMPLETED"}` yields `{"classification":"DONE"}`; status OPEN yields PENDING. Expected consumption is DISPLAYED with the same record. Fake TASK_NOT_FOUND or ACCESS_DENIED yields BUSINESS_REJECTION and NOT_DISPLAYED. These are examples, not real access-control evidence. Simulator uses the same artifact/runtime with explicit fake registration and never substitutes fake certifications into a real host.

<a id="two-product-assistant-surfaces"></a>
## Assistant integration

See [Use Keir with an assistant](../../agents/README.md) for developer integration and in-app authoring. This historical read-only example returns a DISPLAY result. For the earlier simulated-write example and its EVENT_REPORT, see the [original simulated-write host API](compatibility-reference.md#original-write-host-api).

## Acceptance fixtures

This is the acceptance matrix for the original read-only host. See [verification and testing](../../testing.md) for measured results and the [conformance guide](../../../conformance/README.md) for current coverage. Each scenario contains host-selected artifact/model references, business input, trusted fixture bindings and ordered adapter responses. The scenario loader keeps protected fixture bindings outside customer-authorable production context. The trusted test adapter captures exact agent and protected arguments separately from redacted public traces. Missing, extra, out-of-order or unused response stubs fail the test.

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
