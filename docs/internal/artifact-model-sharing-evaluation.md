# Sharing approved models across compiled programs

Status: **shared-model direction adopted on 2026-10-31; binding target reviewed, implementation pending**. The [owning feature](../../roadmap/features/generic-hook-capability-contract.md) remains active. The [binding contract](../specs/host-model-binding.md) defines `/2` exact model references and host-supplied immutable model/invoker handles. Existing `/1` embedded packaging remains unchanged.

## Recommendation and alternatives

For hosts running many programs against relatively few models, recommend artifacts referencing an exact immutable DCM supplied through a host-owned model handle. Share model validation and descriptor preparation, not authority. No registry service, network lookup or configuration subsystem is necessary. Preserve the [ownership contract](../specs/generic-hook-capability-contract.md#responsibility-boundaries): host-defined payload/catalog, customer-authored constants/composition, optional restrictive policies, current authorization, aggregate budgets and default single WRITE.

| Option | Advantage | Cost/decision |
| --- | --- | --- |
| Full embedded DCM | Self-contained offline export; simplest deployment and debugging. | Repeats model bytes and, currently, preparation. Keep existing formats; storage compression/deduplication and internal caching may already suffice for small deployments. |
| Exact reference plus supplied model | Reuses immutable models across programs; explicit version coexistence and bounded preparation. | Host must distribute, retain and approve dependencies. Recommended scalable target, subject to parity/security tests. |
| Per-program minimal model closure | Smaller self-contained payload. | Adds closure generation, identity, validation and compatibility rules; loses much sharing when closures differ. Reject for this increment. |

For offline portability, export a transport bundle containing unchanged reference artifacts and their exact model snapshots once. This is packaging around the same execution format, not a second source of authority or a precedence rule between embedded and external models.

## Evidence and reuse boundary

The [artifact contract](../specs/compiler-artifact.md) embeds complete DCM and deterministic descriptors. Inspection of [KeirCompiler](../../compiler-java/src/main/java/dev/keir/compiler/KeirCompiler.java) confirms both are emitted. [Java Artifact](../../runtime-java/src/main/java/dev/keir/runtime/Artifact.java) and [Node validation](../../runtime-node/src/validation.js) validate DCM digest/schema and independently check instructions, capability versions, arguments and expression environments.

[Java CheckedCel](../../runtime-java/src/main/java/dev/keir/runtime/CheckedCel.java) reconstructs expected descriptors, compares bytes, builds a file descriptor/CEL runtime and prepares each expression. [Node CEL](../../runtime-node/src/cel.js) similarly compares deterministic descriptors, creates a registry and plans expressions. These paths are called per preparation; they do not implement cross-program model handles. Existing Prepared objects already amortize work across invocations of one program.

Share the validated immutable DCM, deterministic descriptor bytes/objects, and model type lookup/registry. Descriptors describe all DCM records, so they are model-derived rather than program-specific; generate them once using a pinned recipe. Retain per-program source digest, exact model reference, profile/compiler/environment pins, entrypoint, instructions, constants, checked expressions, variable/result types, guards/handlers and return. Plan checks and CheckedExpr admission remain per-program; a trusted model cannot validate arbitrary checked bytes by association. The subsequent binding contract puts adapter registrations in the host-bound model handle; narrowed policy and consumer selection remain per-program admission data. Input, identity, keys, deadlines, authorization results and execution state remain fresh invocation data.

Identical names such as `keir.read.Task` cannot identify a shared descriptor globally. Key reusable type state by exact DCM digest, DCM/profile format, descriptor-recipe identity and full qualified CEL/protobuf environment. Default to isolated host approval namespaces as well. Use separate immutable registries, not a process-global registry overwritten by the latest model. Expression plans belong to their program/model/environment; cross-program sharing needs separate proof. Java thread safety and Node concurrent-invocation isolation require tests, not an inference from immutable wrappers.

## Trust and API boundary

The subsequent [binding contract](../specs/host-model-binding.md) specifies the coherent public boundary below, superseding this evaluation's earlier independently supplied model/adapters sketch. These names are not shipped methods:

```text
compiler: compile(source, trustedDcm, pinnedEnvironment) -> referenceArtifact
host/runtime: bindModel(approvedDcmBytes, trustedApproval, completeInvokers) -> BoundModel
host/runtime: prepareProgram(artifactBytes, BoundModel, trustedAdmission) -> Prepared
host/runtime: invoke(Prepared, hostPayload, freshProtectedContext) -> Outcome
```

The compiler still consumes the complete trusted DCM. The artifact references domain ID, domain version, DCM format and canonical content digest; labels alone are insufficient. Admission independently pins artifact digest, model identity, profile/compiler/environment and hook/consumer. Model registration bounds bytes before parsing, verifies the externally approved digest, validates the full schema and deterministically prepares types. Program preparation authenticates artifact bytes first, then requires exact identity equality and verifies the plan against that handle. Changed content under the same version label is a different model; reject mismatch, never choose “latest.”

The host explicitly supplies a handle approved for the invocation's tenant/application scope. A digest proves content identity, not publication, ownership or permission. Unknown, missing, wrong-scope, unapproved or disallowed retired models fail before execution; no remote fetch or executable resolver from artifact fields. One handle cannot transfer another scope's adapter credentials or approval. Hosts may later deduplicate inert bytes across scopes, but approval must remain separate.

Current invocation and per-call authorization remain mandatory. The host authorizer must check the selected model/activation is still permitted as well as current user/resource access; adapters enforce native invariants. A cached Prepared or matching DCM cannot restore revoked access. Keep exceptions redacted and failure diagnostics deterministic in Java and Node; exact new diagnostic names require contract review.

## Versions, retirement and bounds

Keep multiple exact snapshots concurrently. Old extensions can run without recompilation while their exact model, supported environment, compatible adapter implementations and consumer remain available and authorized. Even an additive catalog change produces a different digest; it does not rebind existing programs. Using the new snapshot requires explicit recompilation and selection. Breaking schema, nominal meaning, capability or consumer changes likewise require migration, never compatibility guessing.

Distinguish cache eviction, deprecation and revocation. Eviction releases unused preparation state, not approval. Deprecation may block new preparations while explicitly allowing existing activations; reject other uses. Revocation blocks new invocation and subsequent dispatch through current host checks, including existing Prepared handles. Already-dispatched effects cannot be recalled; retain honest receipts and let in-flight termination follow existing rules. Authorization/dispatch races and native cancellation guarantees remain host obligations. Do not mutate a model beneath running programs.

Prepared/in-flight references pin immutable model state until released. Retain approved historical snapshots for active programs and reproducible audit; a cold restart must restore exact dependencies or fail closed. Removing the last persistent copy can strand otherwise valid programs, so host retirement must enumerate dependencies first. Runtime garbage collection is not lifecycle policy.

Keep current individual DCM/descriptor/artifact/AST limits. Add host-configured finite limits on registered model count/bytes, pinned handles, prepared programs, concurrent preparations and queued work; reject excess before allocation and evict only unpinned cache entries. Bound decoding and registration time outside the invocation path. Resolution is a local supplied-handle check. Preloaded artifacts/models operate offline; missing dependencies cause bounded failure rather than a network wait.

## Size and cost

A direct byte inspection of the current [effect artifact](../../conformance/single-effect/artifact.json) gives 5,439 artifact bytes, 1,721 canonical DCM bytes, 380 Base64 descriptor characters (283 decoded bytes), six expressions, two capabilities and ten types. These are fixture measurements, not scale or latency benchmarks.

For N programs and K distinct models, raw embedded storage is approximately `N*(P+M+D)`; shared packaging is `N*(P+R)+K*(M+D)`. Here M is model bytes, D descriptor text bytes, P other program bytes and R incremental reference/framing overhead. With N=1,000,000, K=10 and illustrative R=100, this fixture estimates 5.439 GB embedded versus about 3.438 GB shared (decimal, ignoring framing differences). The assumed overhead is not a measured new format. Generating descriptors instead of storing them removes the small shared D term.

Transfer savings depend on model reuse at each destination and cold starts. Compression or database/blob deduplication may substantially reduce actual storage savings. CPU savings cover model/descriptor preparation only; expression validation/planning still scales with programs. Memory depends on live Prepared objects and decoded object sizes, not serialized bytes; reference packaging alone does not deduplicate memory. If K approaches N, benefits shrink and dependency management may cost more than embedding.

## Adoption gate

Do not silently strip DCM/descriptors from `/1` or reinterpret reviewed `/2` bytes. If adopted before `/2` implementation, revise its reviewed packaging contract explicitly; otherwise introduce a separately versioned artifact format. Preserve old loader paths and goldens. Migration re-emits/recompiles and reapproves exact artifacts; no automatic relinking.

Future acceptance: same artifact/model pair in Java/Node; missing/wrong digest/environment/scope rejection; simultaneous same-named record versions; registration mutation resistance; concurrent calls without identity leakage; revoked Prepared rejection; in-flight receipt retention; cold-start dependencies; bounded cache pressure and offline execution. Change compiler envelope emission, both artifact validators and CEL preparation seams, host preparation APIs, packaged hosts and shared fixtures together.

The proposed binding contract now specifies format allocation, descriptor recipe encoding and lifecycle diagnostics; host deployments must select finite cache/retirement limits. Review and cross-runtime implementation evidence remain required. Defer registries, closure artifacts and automatic compatibility. No implementation, benchmark, production readiness or new profile qualification is claimed.
