# Keir documentation

Keir lets a host expose hooks and approved capabilities that customers compose into application behavior. The host owns permissions, real adapter guarantees and result consumption. Keir validates and executes constrained extensions; host-integrated agents can assist with integration and authoring.

**Current status:** an experimental Java compiler and Java/Node runtimes execute read-and-decide and simulation-only single-effect profiles. Both have maintained specifications, shared scenarios and independent packaged hosts. See [testing evidence](testing.md) for local and CI status; production qualification and release remain open.

## Public documentation

- [Guided tour: how Keir works](guided-tour.md): follow host design time, customer extension design and runtime, from reusable contracts and customer intent to service calls and outcomes.
- [Specifications](specs/README.md): maintained language, domain capability model (DCM), artifact, runtime and host contracts, with review and implementation status.
- [Agent guides](agents/README.md): separate guidance for the host integration assistant and customer extension assistant. Runnable integrations and optional skill packaging come later.
- [Embedding examples](../examples/README.md): runnable Java and Node hosts for classification and follow-up simulation.
- [Conformance](../conformance/README.md): shared source, compiled artifacts and independent expectations for both runtimes.

## For contributors

Use the [testing guidelines](testing.md) for coverage, fast/slow suites and the initial behavioral-contract protection plan.

The single [Minimap roadmap](../roadmap/board.md) and [milestone guidance](../roadmap/scope.md) live in `roadmap/`. There is no second roadmap under `docs/`.

[Internal records](internal/README.md) contain design inputs, decisions and workflow/bootstrap notes. [Engineering team roles](../roles/README.md) govern our development agents and are distinct from the two product-facing agent personas.
