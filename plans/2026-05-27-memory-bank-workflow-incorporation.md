# ReaderParser memory-bank workflow incorporation plan

Saved for implementation in the next session.

## Current gaps

- `AGENTS.md` only mentions `memory-bank/active-context.md` conditionally.
- `progress.md` is not part of the default workflow.
- No lazy-load matrix exists for memory-bank files.
- No end-of-task memory update protocol exists.
- `active-context.md` and `progress.md` currently contradict each other.
- `memory-bank/` mixes working memory, duplicated rules, and archival notes.

## Target model

- `AGENTS.md` = protocol and routing map.
- Always read first:
  - `memory-bank/activeContext.md`
  - `memory-bank/progress.md`
- Lazy-load only when needed:
  - `memory-bank/projectbrief.md`
  - `memory-bank/productContext.md`
  - `memory-bank/systemPatterns.md`
  - `memory-bank/techContext.md`
- Keep detailed investigations, logs, and old plans outside the core memory flow.

## File disposition

### Keep and normalize

- Rename `memory-bank/active-context.md` → `memory-bank/activeContext.md`
- Keep `memory-bank/progress.md`

### Add

- `memory-bank/projectbrief.md`
- `memory-bank/productContext.md`
- `memory-bank/systemPatterns.md`
- `memory-bank/techContext.md`

### Fold or retire from core workflow

- `memory-bank/conventions.md` → fold into `systemPatterns.md` and `techContext.md`
- `memory-bank/directory-map.md` → short summary into `systemPatterns.md`; deep structure stays in `codemap.md`
- `memory-bank/test-strategy.md` → fold into `techContext.md`
- `memory-bank/decision-log.md` → keep only durable active decisions; archive the rest
- `memory-bank/debug-issue6-findings.md` → move to `docs/` or `plans/`
- `memory-bank/commit-conventions.md` → reference file only, not core task-start memory
- `memory-bank/README.md` → optional index only

## Implementation phases

### Phase 1 — reconcile current memory

1. Audit actual project state.
2. Rewrite `activeContext.md` and `progress.md` so they agree.
3. Remove stale or contradictory claims.

### Phase 2 — normalize the memory-bank structure

1. Rename `active-context.md` to `activeContext.md`.
2. Create the four stable memory files:
   - `projectbrief.md`
   - `productContext.md`
   - `systemPatterns.md`
   - `techContext.md`
3. Move durable content out of duplicated files into the canonical files.

### Phase 3 — wire workflow into project instructions

1. Add a dedicated `Memory Bank` section to `AGENTS.md`.
2. Define task-start behavior:
   - always read `memory-bank/activeContext.md`
   - always read `memory-bank/progress.md`
   - load other memory files only when task-relevant
3. Define end-of-task update rules:
   - update `activeContext.md`
   - update `progress.md`
   - update other memory only if durable knowledge changed

### Phase 4 — wire commands and session workflow

1. Update `.opencode/command/start.md`.
2. Update `.opencode/command/light-start.md`.
3. Make the parent/orchestrator own memory reads and writes.
4. Pass only minimal memory summaries to specialists.

### Phase 5 — optional always-loaded memory

Only after the two core files are short and accurate, consider adding to
`.opencode/opencode.json`:

```json
"instructions": [
  "memory-bank/activeContext.md",
  "memory-bank/progress.md"
]
```

Do not do this before cleanup.

## Workflow hooks

### Task start

1. Read `AGENTS.md`.
2. Read `memory-bank/activeContext.md`.
3. Read `memory-bank/progress.md`.
4. Summarize:
   - current objective
   - relevant files
   - known constraints
   - next safe action
5. Do not load other memory files unless needed.

### During task

- Before reading another memory file, state which one is needed and why.
- Search by symbol/path before broad repo exploration.
- Do not re-read already summarized memory unless exact wording matters.

### End of task

- Update `activeContext.md` with:
  - current state
  - active decisions
  - next step
  - last updated date
- Update `progress.md` with:
  - completed
  - in progress
  - blocked
  - known issues
  - verification commands
- Update stable memory only if product, architecture, or tooling knowledge changed.

### Specialist usage

- Parent session reads and updates memory-bank files.
- Specialists receive only:
  - a short summary
  - the single relevant memory file when needed
- Do not give specialists the whole memory-bank by default.

### Plan/build split

1. Planning session reads `activeContext.md` and `progress.md`.
2. Planning session writes a plan under `plans/`.
3. Update `activeContext.md` with the chosen objective and next step.
4. Start a fresh build session with:
   - the saved plan
   - `memory-bank/activeContext.md`
   - `memory-bank/progress.md`
5. After implementation, update memory before ending the session.

## Acceptance criteria

- A fresh task does not read the whole `memory-bank/`.
- `activeContext.md` and `progress.md` are enough to answer:
  - current objective
  - known blockers
  - next step
- Architecture work loads `systemPatterns.md` only when needed.
- Build/test/tooling work loads `techContext.md` only when needed.
- No contradictions remain between active and progress memory.
- Core memory files stay lean and do not become dump folders.
