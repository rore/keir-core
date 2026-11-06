# Keir 2 conformance fixtures

The compiler produces the artifacts and digest pins. The scalar regression groups retain the independent expected behavior of [Keir 1](../keir-1/README.md) and use freshly compiled Keir 2 source and model pairs. The original corpus remains versioned separately.

| Group | Source | Artifact | Model |
| --- | --- | --- | --- |
| inventory | source-inventory.json | artifact-inventory.json | dcm-inventory.json |
| inventory-algebra | source-inventory-algebra.json | artifact-inventory-algebra.json | dcm-inventory.json |
| inventory-enum | source-inventory-enum.json | artifact-inventory-enum.json | dcm-inventory-enum.json |

The Inventory groups use a trusted simulation with a fresh opaque request key, event id `event1`, event source `item1`, and an initial clock of zero with a 10,000 ms deadline. The groups with possible calls use protected actor `user1`. The selected program and adapter catalog come from the host fixture. Expected calls do not supply authority or select a program.

The main Inventory cases cover semantic list membership, explicit false, absent optional values, elapsed time, a lazy overflowing branch, escaping overflow before a WRITE, handler output omission, and maximum list no-match. The algebra case checks both operand orders for Boolean masking, then records a confirmed WRITE before a conditional condition produces an escaping overflow. The outcome retains that receipt. The enum case checks direct member-literal refinement and returns the declared nominal list in its original order, including duplicate elements. The raw vectors keep the existing [byte recipe semantics](../keir-1/README.md#raw-scenario-vectors). The nested model-binding corpus verifies binding outcomes and zero dispatch through the public API.
