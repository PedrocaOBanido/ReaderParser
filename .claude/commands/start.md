---
description: Session kickoff — plan a task and dispatch it to the right specialized subagent.
argument-hint: <task description>
---

# Session kickoff

User request: $ARGUMENTS

Run the orchestrator workflow by invoking the `orchestrator` agent via
the `Agent` tool with `subagent_type: "orchestrator"`. Pass the user's
request verbatim and let the orchestrator handle:

1. **Restate** the request in one sentence.
2. **Identify** which subagent owns it (`source-author`, `screen-author`,
   `room-migration`, `refactor-renamer`, `reviewer`, `runner`,
   `journey-runner`, `researcher`, `domain-author`) — or say
   "answering directly" for a read-only question, or "no single owner"
   if it spans multiple subagents.
3. **Explore**: read root `AGENTS.md` (loaded via `CLAUDE.md`), the
   nearest nested `AGENTS.md`, `memory-bank/active-context.md`, and the
   implicated directory. Read-only — do not edit.
4. **Plan**: list files in scope, the AGENTS.md constraints that apply,
   acceptance criteria, and any clarifying questions for the user. Stop
   and wait for approval.
   - Skip the wait when: the request is read-only, or the user used a
     `/new-source`, `/new-screen`, or `/add-migration` command, or the
     task fits one subagent and all required inputs (selectors, content
     type, screen purpose, schema details) are already present.
5. After approval, **dispatch** via the `Agent` tool using the handoff
   template in the orchestrator's system prompt.
6. After the subagent reports done, **auto-dispatch `reviewer` and
   `runner` in parallel** against the uncommitted diff and surface
   findings grouped by severity. Do not auto-fix — that's a fresh turn.

Refuse upfront if the request would violate a root `AGENTS.md` §1
non-negotiable. Never edit files yourself. Never invent missing details
(selectors, nullability, defaults, screen state fields).
