---
description: Lightweight session kickoff — for small tasks, direct implementation.
agent: sisyphus
---

# Light session kickoff

User request: $ARGUMENTS

Run the lightweight workflow:

1. **Restate** the request in one sentence.
2. **Classify**: is this a read-only question, a bugfix, or an implementation
   task? If read-only, answer directly. If implementation, proceed to step 3.
3. **Scout**: read the root `AGENTS.md`, the relevant nested `AGENTS.md`,
   and `memory-bank/active-context.md`. Identify which files are in scope.
   Read-only — do not edit.
4. **Execute**: for single-file changes, work directly. For multi-file changes
   with clear scope, decompose and delegate via `task`. For anything
   ambiguous, stop and ask one clarifying question before proceeding.
5. **Verify**: run `@runner` when complete, or use the `/verify` command.

Refuse upfront if the request would violate a root `AGENTS.md` §1
non-negotiable. Skip the planning-and-approval gate when the task is
trivially scoped — just do the work and report.
