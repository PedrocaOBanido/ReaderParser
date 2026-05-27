---
description: Lightweight direct kickoff — small tasks, minimal delegation.
agent: build
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
4. **Execute**: for single-file changes and other small, bounded edits,
   work directly. Use `task` only when the work needs broad discovery,
   a specialist-only lane, or 3+ coordinated file edits where delegation
   is clearly cheaper than doing it yourself. If anything is ambiguous,
   stop and ask one clarifying question before proceeding.
5. **Verify**: use direct checks for trivial config/docs changes. Run
   `@runner` only when source or build files changed and verification adds
   value, or use the `/verify` command.

Refuse upfront if the request would violate a root `AGENTS.md` §1
non-negotiable. Skip the planning-and-approval gate when the task is
trivially scoped — just do the work and report.
