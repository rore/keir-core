# Keir development team

These guides define the team working across `keir-core` and `keir-demo-app`. Core owns the shared roadmap and role guidance. Runtime identity is the session's verified current Pallium Relay name; mapping a name does not start a session or assign work.

| Position | Relay name | Model and effort | Guide |
| --- | --- | --- | --- |
| Manager | `@keir-manager` | Preserve the user's selected session settings | [Manager](manager.md) |
| Architect | `@keir-architect` | Astra (`gpt-6-astra`), medium | [Architect](architect.md) |
| Core developer 1 | `@keir-dev1` | Sol 6.1 (`gpt-6.1-sol`), high | [Developer](developer.md) |
| Core developer 2 | `@keir-dev2` | Sol 6.1 (`gpt-6.1-sol`), high | [Developer](developer.md) |
| Demo developer, deferred | `@keir-demo-dev` | Sol 6.1 (`gpt-6.1-sol`), high | [Demo developer](demo-developer.md) |

The current team has one architect and two core developers alongside the manager. Divide independent core tasks when contracts are clear; otherwise pair implementation with focused verification or sequence dependent work. The dedicated demo developer builds the actual task application later, once useful packaged core behavior is available to integrate. No demo implementation is currently assigned. Sessions do not start implementation merely by joining the team. The manager owns `roadmap/` in the shared core checkout; temporary assignments and evidence belong in the owning feature, not in these role guides.

All roles load [shared working guidance](shared.md). Model choices are the user's defaults for these positions; role files do not change a session's model automatically. Report a mismatch and arrange the requested session settings before substantive role work. Do not change another established session's settings without explicit user authorization.

When starting, resuming, handing off, or accepting an assignment, use the installed `persistent-agent-roles` skill to verify your exact current Relay name and load its mapping. If identity discovery is unavailable, report that limitation and continue only under independently supplied task instructions; do not guess an identity. Never take over an occupied name without the user's authorization.
