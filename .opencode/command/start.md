---
description: Plan-first kickoff — create an approval gate and save approved plans under plans/.
agent: orchestrator
---

# Plan-first session kickoff

User request: $ARGUMENTS

Use this command for plan-first implementation work.

1. **Restate** the request in one sentence.
2. **Classify**:
   - If the request is purely read-only, answer directly and stop.
   - Otherwise continue with the plan-first workflow below.
3. **Explore minimally**: read only the root `AGENTS.md`,
   `memory-bank/activeContext.md`, `memory-bank/progress.md`, the nearest
   nested `AGENTS.md`, and the directly implicated files. Load
   `memory-bank/projectbrief.md`, `memory-bank/productContext.md`,
   `memory-bank/systemPatterns.md`, `memory-bank/techContext.md`,
   `architecture.md`, or `codemap.md` only when the task needs them.
4. **Choose the execution lane**: say whether the approved work should be
   implemented directly or by a specialist/subagent. Name the lane only when it
   clearly adds value.
5. **Draft an approval-ready plan**. Include:
   - objective or request restatement
   - recommended execution lane
   - files likely in scope
   - relevant `AGENTS.md` constraints
   - implementation phases or steps
   - verification commands
   - acceptance criteria
   - open questions or risks
   - a proposed save path under `plans/YYYY-MM-DD-<slug>.md`
6. **Stop and wait for approval.**
7. After approval:
   - save the approved plan under the proposed `plans/` path
   - include `Saved for implementation in the next session.` near the top
   - report the saved path
   - tell the user to run `/run-plan <saved-plan-path>` to start implementation
     in a fresh isolated session
   - do **not** start implementation in this session unless the user explicitly
     overrides the plan-then-run-plan flow

Refuse upfront if the request would violate a root `AGENTS.md`
non-negotiable. Do not invent missing details.
