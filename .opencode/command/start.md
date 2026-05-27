---
description: Complex session kickoff — plan a task and use specialists only when they add clear value.
agent: orchestrator
---

# Session kickoff

User request: $ARGUMENTS

Run the orchestrator workflow:

1. **Restate** the request in one sentence.
2. **Identify** whether it is best handled directly or by a specialist.
   Name a subagent (`source-author`, `screen-author`, `room-migration`,
   `reviewer`) only when it clearly adds value. Say "answering directly"
   for read-only questions or small, bounded edits.
3. **Explore**: read root `AGENTS.md`, the nearest nested `AGENTS.md`,
   `memory-bank/active-context.md`, and the implicated directory.
   Read-only — do not edit.
4. **Plan**: list files in scope, the AGENTS.md constraints that apply,
   acceptance criteria, and any clarifying questions for the user. Stop
   and wait for approval.
   - Skip delegation and execute directly when the task is small, bounded,
     and clear (for example 1–2 files, no broad discovery, no architectural
     decision, no specialist-only lane).
   - Skip the wait when: the request is read-only (answer directly), or
     the user used a `/new-source`, `/new-screen`, or `/add-migration`
     command, or the task fits one subagent and all required inputs
     (selectors, content type, screen purpose, schema details) are
     already present.
5. After approval, **execute directly or dispatch via the `task` tool**.
   Dispatch only when the specialist adds net value over doing the work
   yourself.
6. **Auto-dispatch `@reviewer` only** for risky, multi-file,
   architecture-sensitive, or policy-heavy code changes. Skip reviewer for
   trivial, config-only, or clearly bounded edits.

Refuse upfront if the request would violate a root `AGENTS.md` §1
non-negotiable. Never invent missing details (selectors, nullability,
defaults, screen state fields).
