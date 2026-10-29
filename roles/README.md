# Keir development team

These guides define the team working across `keir-core` and `keir-demo-app`. Core owns the shared roadmap and role guidance. Runtime identity is the session's verified current Pallium Relay name; mapping a name does not start a session or assign work.

| Position | Relay name | Model and effort | Guide |
| --- | --- | --- | --- |
| Manager | `@keir-manager` | Preserve the user's selected session settings | [Manager](manager.md) |
| Architect | `@keir-architect` | Astra (`gpt-6-astra`), medium | [Architect](architect.md) |
| Developer 1 | `@keir-dev1` | Sol 6.1 (`gpt-6.1-sol`), high | [Developer](developer.md) |
| Developer 2 | `@keir-dev2` | Sol 6.1 (`gpt-6.1-sol`), high | [Developer](developer.md) |

The two developer names are available slots, not a requirement to run both. Start sessions when there is independent work. The manager owns `roadmap/` in the shared core checkout; temporary assignments and evidence belong in the owning feature, not in these role guides.

All roles load [shared working guidance](shared.md). Model choices are the user's defaults for these positions; role files do not change a session's model automatically. Report a mismatch and arrange the requested session settings before substantive role work. Do not change another established session's settings without explicit user authorization.

When starting, resuming, handing off, or accepting an assignment, use the installed `persistent-agent-roles` skill to verify your exact current Relay name and load its mapping. If identity discovery is unavailable, report that limitation and continue only under independently supplied task instructions; do not guess an identity. Never take over an occupied name without the user's authorization.
