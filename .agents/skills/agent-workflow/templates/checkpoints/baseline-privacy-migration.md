# Baseline privacy migration

Use only with human approval bound to the configured record path, full checker-selected anchor from actual base/head history, and exact public-safe replacements. Reuse approval covering that tuple; generic task approval does not suffice. Keep original evidence private; never publish private values or their hashes. Current-content cleanup leaves historical exposure; do not rewrite shared history without separate authorization.

Add this optional field inside either record shape:

```text
**Baseline privacy migration:**
{"schema_version":1,"record_path":".agent-workflow/tasks/example.md","anchor_commit":"<full checker-selected commit>","replacements":{"source":"<approved public-safe source>"},"reason":"Privacy redaction preserving authority and requirements.","approval":{"by":"user","reference":"<public-safe approval reference>","verbatim":"<truthful approval of this bound migration>"}}
```

Replace placeholders with genuine evidence. Keep exactly these top-level keys. The old source must exist; every replacement must be nonempty and differ.

| Version | Replacements and transition |
|---|---|
| 1 | Exactly source. Preserve all functional baseline/current values and parsed Behavior changes. |
| 2 | Exactly source, constraints, completion_criteria. Replace source plus both baseline/current functional pairs atomically. Preserve every other raw marker field and the exact raw chain. |

For v2, both current functional values must equal their baselines and the prior chain must not target either field. Preserve the SAME original designated owners, reviewers, holds and obligations at the bound revision, not current role holders; dynamic obligations stay dynamic. Older checkers reject v2. Other functional text and existing chain/approval redaction remain unsupported.

Commit all replacements and receipt together. Missing/malformed history, deletion/rename or ambiguous parents cannot be repaired. A prior committed redaction cannot be blessed by a later receipt: use a fresh reviewed branch from trusted pre-redaction history without restoring private text, or report the blocker. Revalidate advancing unmerged bases and approval bindings before mutation; never silently rebind.

Validate the committed result and full verdict:

```bash
python scripts/agent-workflow-check.py --repo-root . --slug <slug> --base-ref <base> --head-ref <head>
```

In this source repo use `python -m core.checker`. Both refs/shared history are mandatory for receipts, including local checks; no-ref callers cannot qualify them. Run required repository checks.

Retain the exact receipt, original anchor and migrated baseline in every descendant/later base. No removal, tamper/restore, second migration or in-place version upgrade. Later normal context changes use the existing change chain from the migrated baseline.

Review privacy, historical referents, semantic equivalence and approval authenticity separately; the checker proves structure and visible history, not those judgments or rewritten history. Full contract: SPEC §9.1.
