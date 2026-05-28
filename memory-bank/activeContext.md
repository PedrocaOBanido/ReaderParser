# Active context

Last updated: 2026-05-27

## Current objective

Keep the memory-bank workflow lean and authoritative. A task should start with
`AGENTS.md`, `memory-bank/activeContext.md`, and `memory-bank/progress.md`,
then load deeper memory only when it is directly relevant.

## Current state

- Core app structure is already in place: source plugins, repositories, Room
  persistence, ViewModels, Compose screens/content, workers, and journey
  tooling all exist in the repo.
- The memory bank was normalized on 2026-05-27:
  - `active-context.md` was renamed to `activeContext.md`
  - duplicated guidance was folded into `projectbrief.md`,
    `productContext.md`, `systemPatterns.md`, and `techContext.md`
  - startup workflow docs now point to the two-file core memory flow
  - historical investigation notes were moved out of `memory-bank/`

## Active decisions

- `activeContext.md` and `progress.md` are the only core task-start memory
  files.
- `projectbrief.md`, `productContext.md`, `systemPatterns.md`, and
  `techContext.md` are lazy-load references.
- The parent/orchestrator owns memory-bank reads and writes.
- Specialists should receive a short summary and only the single relevant
  memory file when needed.

## Known constraints

- Do not load the whole `memory-bank/` by default.
- Keep detailed investigations, one-off plans, and archival notes outside the
  core memory flow.
- Update `activeContext.md` and `progress.md` before ending a task.
- Update the stable memory files only when durable product, architecture, or
  tooling knowledge changed.

## Relevant files

- `AGENTS.md`
- `.opencode/command/start.md`
- `.opencode/command/light-start.md`
- `.opencode/opencode.json`
- `memory-bank/activeContext.md`
- `memory-bank/progress.md`
- `memory-bank/projectbrief.md`
- `memory-bank/productContext.md`
- `memory-bank/systemPatterns.md`
- `memory-bank/techContext.md`

## Next safe action

On the next task, read `AGENTS.md`, `memory-bank/activeContext.md`, and
`memory-bank/progress.md`; lazy-load the other memory files only if the task
needs them, then update the two core memory files before ending the session.
