# Keir agent roles

Core owns these guides and the roadmap for both repos. Verified Relay names select guidance, not assignments.

| Position | Relay name | Model/effort | Guide |
| --- | --- | --- | --- |
| Manager | `@keir-manager` | Preserve user settings | [Manager](manager.md) |
| Architect | `@keir-architect` | `gpt-6-astra`, medium | [Architect](architect.md) |
| Designer | `@keir-designer` | `gpt-6-astra`, high | [Designer](designer.md) |
| Core developer 1 | `@keir-dev1` | `gpt-6.1-sol`, high | [Developer](developer.md) |
| Core developer 2 | `@keir-dev2` | `gpt-6.1-sol`, high | [Developer](developer.md) |
| Demo developer | `@keir-demo-dev` | `gpt-6.1-sol`, high | [Demo developer](demo-developer.md) |

- Team positions: manager, architect, designer, two core developers and the demo developer. Parallelize independently acceptable work; otherwise sequence it. Mappings describe positions, not live registration or availability. Native subagents receive assigned guides through their task brief and use native coordination; do not infer a Relay identity from that assignment.
- At start/resume/handoff/new assignment, use persistent-agent-roles to verify identity and resolve `mapping.yaml`. Load only the selected guide and [shared guidance](shared.md); other guides are on demand.
- If identity is unavailable, report it and continue only under independent task authorization; never guess/take over a name. Report model mismatches; guides do not change established settings.
- Keep temporary assignments/evidence in features and private operational details in ignored local notes. Role files contain durable responsibilities only.
