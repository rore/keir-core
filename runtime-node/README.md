# Node runtime

Initial-scope embedded TypeScript/Node runtime. Load and prepare compatible trusted artifacts, execute bounded instructions through host adapters, and expose deterministic simulation and traces with fake adapters.

Consume the same artifacts and conformance scenarios as the Java runtime. Do not parse source IR or require Java for execution. Synchronous CEL evaluation needs an enforceable resource policy; a timer alone cannot interrupt it. No runtime implementation exists yet.
