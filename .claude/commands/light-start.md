---
description: Lightweight session kickoff — for small tasks, direct implementation without orchestrator.
argument-hint: <task description>
---

# Light session kickoff

User request: $ARGUMENTS

Run the lightweight workflow directly (do NOT invoke the orchestrator):

1. **Restate** the request in one sentence.
2. **Classify**: is this a read-only question, a bugfix, or an
   implementation task? If read-only, answer directly. If implementation,
   proceed to step 3.
3. **Scout**: read the root `AGENTS.md` (already loaded via `CLAUDE.md`),
   the relevant nested `AGENTS.md`, and `memory-bank/active-context.md`.
   Identify which files are in scope. Read-only — do not edit.
4. **Execute**: for single-file changes, work directly. For multi-file
   changes with clear scope, decompose and dispatch via the `Agent` tool
   to the relevant specialist (`source-author`, `screen-author`, etc.).
   For anything ambiguous, stop and ask one clarifying question before
   proceeding.
5. **Verify**: when complete, dispatch the `runner` agent or run the
   `/verify` command.

Refuse upfront if the request would violate a root `AGENTS.md` §1
non-negotiable. Skip the planning-and-approval gate when the task is
trivially scoped — just do the work and report.
