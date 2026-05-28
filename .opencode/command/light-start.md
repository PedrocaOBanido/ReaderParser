---
description: Lightweight direct kickoff — small tasks, minimal delegation.
agent: orchestrator
---

# Light session kickoff

User request: $ARGUMENTS

Run the lightweight workflow:

1. **Restate** the request in one sentence.
2. **Classify**: is this a read-only question, a bugfix, or an implementation
   task? If read-only, answer directly. If implementation, proceed to step 3.
3. **Scout**: read only the root `AGENTS.md`, `memory-bank/activeContext.md`,
   `memory-bank/progress.md`, the relevant nested `AGENTS.md`, and the
   directly implicated files. Load `memory-bank/projectbrief.md`,
   `memory-bank/productContext.md`, `memory-bank/systemPatterns.md`, or
   `memory-bank/techContext.md` only when the task needs them.
4. **Execute**: for single-file changes and other small, bounded edits,
   work directly. Use `task` only when the work needs broad discovery,
   a specialist-only lane, or 3+ coordinated file edits where delegation
   is clearly cheaper than doing it yourself. The parent/orchestrator keeps
   ownership of memory-bank reads and writes; pass specialists only a short
   memory summary and, when needed, one relevant memory file. If anything is
   ambiguous, stop and ask one clarifying question before proceeding.
5. **Verify**: use direct checks for trivial config/docs changes. Run
   `@runner` only when source or build files changed and verification adds
   value, or use the `/verify` command.

Refuse upfront if the request would violate a root `AGENTS.md`
non-negotiable. Skip the planning-and-approval gate when the task is
trivially scoped — just do the work and report.
