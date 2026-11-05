# Keir 1 conformance fixtures

The compiler owns the source, canonical model, artifact and SHA-256 pins. Both runtimes use the same artifact bytes. Scenario expectations are independent of compiler output and are shared with the hosts.

| Scenario group | Source | Artifact | Model |
| --- | --- | --- | --- |
| `main` | `source.yaml` | `artifact.json` | `dcm.json` |
| `zero` | `source-zero.yaml` | `artifact-zero.json` | `dcm.json` |
| `after-write-read` | `source-after-write-read.yaml` | `artifact-after-write-read.json` | `dcm.json` |
| `no-call` | `source-no-call.yaml` | `artifact-no-call.json` | `dcm.json` |
| `handler` | `source-handler.json` | `artifact-handler.json` | `dcm.json` |
| `title` | `source-title.yaml` | `artifact-title.json` | `dcm-title.json` |
| `title-max` | `source-title-max.yaml` | `artifact-title-max.json` | `dcm-title.json` |
| `title-composed` | `source-title-composed.yaml` | `artifact-title-composed.json` | `dcm-title.json` |

Compile a row with the normal two-file compiler invocation, using its source and model columns. For verification, the trusted host selects that row's pinned artifact and model and supplies each file under `scenarios/<group>/` as a separate scenario document. The host binds the approved model and implementations, prepares the selected artifact, and invokes it with the fixture table's authority and clock. Compare ordered calls, full outcome including trace and effects, and consumer report. Check scenario IDs for uniqueness in the selected suite.

The fixture table supplies `normal`, `read-throws`, `lost-response`, and `response-expired`. The last three respectively throw on the initial READ, throw after WRITE dispatch, and expire the host clock as the confirmed WRITE response arrives. The ordinary fixture supplies the trusted event, deadline, native key and any required actor binding. Calls and protected arguments in the scenario are expectations to compare with observed dispatch.

## Raw scenario vectors

Each entry in [scenario-vectors.json](scenario-vectors.json) describes bytes to pass to a native scenario reader. Git preserves LF for the fixture data so byte recipes are portable across checkouts. Give each vector a fresh fixture scope so IDs reused in different vectors are independent.

- `utf8` encodes the literal string as UTF-8. `base64` decodes bytes directly for malformed UTF-8. These entries can stop before selecting an artifact.
- `scenario` starts with the bytes of `scenarios/<scenario>.json` and selects the matching trusted row above. Where `replace` is present, replace the first literal occurrence of its first string with the second string. Where `expand` is present, first replace the single `@` in the replacement string with the specified string repeated the specified number of times. Where `padToBytes` is present, append ASCII spaces to reach the exact byte count. Preserve numeric spellings, escapes and whitespace. Do these operations before parsing the scenario.
- `artifactFormat` and `modelFormat` replace the respective envelope's root format in the selected test bytes. `model` selects the alternate pinned model basename. The trusted harness constructs these mismatches independently of scenario bytes.

`accept` expects the full scenario to pass. Every rejection vector reaches the specified first failing check after passing the earlier checks. `lexical` checks raw JSON, `structural` checks decoded scenario shape including bounded trace sequence values, and `byte-fields` checks native UTF-8 field lengths. The safe integer and depth-16 rows must pass the lexical reader before structural rejection. `accept-byte-fields` only checks through native field lengths. `compatibility` checks the selected formats and model pair, `fixture` checks the trusted fixture table, `typing` checks the selected DCM, and `semantics` checks the trace and expectation relationships. Preflight rejection vectors dispatch zero stubs. The dotted fixture vector reaches fixture lookup, where the shared test table has no such fixture.
