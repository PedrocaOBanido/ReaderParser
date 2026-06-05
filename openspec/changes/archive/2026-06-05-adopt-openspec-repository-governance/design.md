# Design: Adopt OpenSpec Repository Governance

## Context

The repo currently runs two parallel workflows for structured changes:

1. **Custom system**: `/start` → `plans/` → `/run-plan`, with `memory-bank/` as
   transient session state (`activeContext.md`, `progress.md`, plus several
   convention and context files).
2. **OpenSpec**: proposal → design → specs → tasks under `openspec/changes/`,
   with archived specs under `openspec/specs/`.

This creates confusion about where durable truth lives, which workflow an agent
should follow, and where to find the current state of a change. The custom
system duplicates parts of OpenSpec's artifact trail without the spec or
traceability benefits.

The goal is to consolidate on OpenSpec as the single structured workflow for
non-trivial changes, while keeping raw direct handling for read-only and trivial
work.

## Goals / Non-Goals

### Goals

- OpenSpec is the **only** structured workflow for non-trivial changes.
- Durable repo truth lives exclusively in a small set of stable files:
  `README.md`, `architecture.md`, `codemap.md`, `AGENTS.md`, and OpenSpec
  specs under `openspec/specs/`.
- No repo-global transient context files remain (no `memory-bank/`, no
  `plans/`).
- The `repository-governance` spec codifies when OpenSpec is required and what
  constitutes a trivial exception.
- Agent routing (`AGENTS.md`, `opencode.json`, command files) reflects the new
  single-workflow model.

### Non-Goals

- Changing the project's Kotlin source structure, build system, or CI.
- Modifying the OpenSpec CLI or its schema definitions.
- Removing the specialist agent lanes (`source-author`, `screen-author`, etc.)
  — they remain valid execution paths within OpenSpec tasks.
- Restructuring the `.opencode/skills/` OpenSpec skills (they already work).

## Decisions

### D1: Remove `plans/` directory entirely

**Decision**: Delete all files under `plans/`. Historical plans are either
stale (superseded by OpenSpec changes) or archival artifacts that no agent
should read for current work.

**Rationale**: Keeping `plans/` invites agents to use the old `/start` workflow
by habit. The information in these files is either captured in OpenSpec changes
or is no longer actionable. OpenSpec changes under `openspec/changes/` and
archived specs under `openspec/specs/` replace `plans/` completely.

**Alternative considered**: Move `plans/` to an archive directory. Rejected
because it preserves a second plan store that agents might discover and follow.

### D2: Remove `memory-bank/` directory entirely

**Decision**: Delete all 12 files under `memory-bank/`. Durable information
already lives in `README.md`, `architecture.md`, `codemap.md`, and `AGENTS.md`.
Per-change transient context lives in OpenSpec artifacts. No repo-global
transient context file should exist.

**Rationale**: `activeContext.md` and `progress.md` were task-start and
task-end scratchpads. Their content is either (a) durable knowledge already
in the canonical docs, or (b) transient state that belongs in OpenSpec change
artifacts. The convention files (`commit-conventions.md`, `conventions.md`,
`test-strategy.md`, etc.) overlap with what `AGENTS.md` and `architecture.md`
already say.

**Migration detail**: Before deletion, audit each file:
- `activeContext.md` / `progress.md`: transient state — discard.
- `projectbrief.md` / `productContext.md`: merge any unique durable facts into
  `README.md` or `architecture.md` if not already covered.
- `systemPatterns.md` / `techContext.md`: merge unique patterns into
  `architecture.md`.
- `commit-conventions.md` / `conventions.md` / `test-strategy.md` /
  `directory-map.md` / `decision-log.md`: merge any rules not already in
  `AGENTS.md` or `architecture.md`.
- `README.md` (inside `memory-bank/`): discard — the repo root `README.md`
  is the durable readme.

### D3: Remove `.opencode/command/start.md` and `light-start.md`

**Decision**: Delete both files. The `/start` and `/light-start` commands
implement the old plan-first workflow. They reference `memory-bank/` paths that
will no longer exist.

**Rationale**: OpenSpec's `/openspec-propose` and `/openspec-apply-change`
skills replace the plan-first kickoff. The `/run-plan` command is also removed
(its file references `plans/` which will not exist).

**Retained commands**: `add-migration.md`, `new-screen.md`, `new-source.md`,
and `verify.md` stay — they are bounded specialist commands that do not depend
on `plans/` or `memory-bank/`.

### D4: Create `repository-governance` spec

**Decision**: Create `openspec/specs/repository-governance/spec.md` as the
first permanent spec. It codifies:

1. **When OpenSpec is required**: any multi-file change, new source, new screen,
   new repository, Room migration, architectural change, or anything that
   benefits from a proposal/design/spec/tasks trail.
2. **When OpenSpec is not required** (trivial exceptions): read-only questions,
   single-file cosmetic fixes (typo, formatting), answering user questions
   without code changes, and directly running a single known command.
3. **Durable truth files**: `README.md`, `architecture.md`, `codemap.md`,
   `AGENTS.md`, and `openspec/specs/` — these are the only repo-global
   files agents should read for authoritative state.
4. **No repo-global transient files**: the repo must not contain session
   scratchpads, active-context files, or progress trackers outside of OpenSpec
   change artifacts.

**Rationale**: Without an explicit governance spec, future agents (or humans)
may reintroduce transient context files or revive the old workflow. The spec
is the single source of truth for how work is managed.

### D5: Update `AGENTS.md` to remove old workflow references

**Decision**: Rewrite the following sections in `AGENTS.md`:

- **Remove** the entire "Memory Bank" section (lines 135–155) — it routes
  agents to `memory-bank/` files that will not exist.
- **Remove** the "Context retrieval rule" section's `memory-bank/` references
  (lines 156–171). Replace with a shorter rule: read the directly referenced
  file, then the nearest relevant `AGENTS.md`, then `architecture.md` or
  `codemap.md` only when the task needs them.
- **Add** a brief "Workflow" section pointing at OpenSpec:
  - Non-trivial changes go through the OpenSpec propose → design → specs →
    tasks flow.
  - Trivial/read-only work proceeds directly.
  - The `repository-governance` spec defines the policy.
- **Keep** all non-negotiables, specialist lanes, placement rules, testing,
  and verification sections unchanged — they are orthogonal to the workflow
  change.

### D6: Update `.opencode/opencode.json`

**Decision**: Remove the `"instructions"` array that currently points at
`memory-bank/activeContext.md` and `memory-bank/progress.md`. These files will
not exist after migration.

**Rationale**: OpenCode loads `instructions` files at session start. Referencing
deleted files would cause errors or silent failures.

**Alternative**: Replace with references to `AGENTS.md` or a new instruction
file. Not needed — `AGENTS.md` is already loaded by the global persona, and
OpenSpec change artifacts are read on demand by the apply-change skill.

### D7: Remove `.opencode/command/run-plan.md`

**Decision**: Delete `run-plan.md` alongside `start.md` and `light-start.md`.

**Rationale**: `run-plan` executes plans saved under `plans/`. With `plans/`
removed and `/start` gone, there is nothing for it to run. OpenSpec's
`/openspec-apply-change` handles task execution.

## Risks / Trade-offs

### R1: Loss of session continuity without `memory-bank/`

**Risk**: Agents lose the ability to resume mid-task by reading
`activeContext.md`.

**Mitigation**: OpenSpec change artifacts (proposal, design, specs, tasks) are
more structured than `activeContext.md`. An in-progress change has explicit
task checkboxes and a design document. For truly interrupted work, the
change's `tasks.md` file shows exactly what was done and what remains.

### R2: Historical plans become inaccessible

**Risk**: Some `plans/` files contain decisions or context not captured
elsewhere.

**Mitigation**: Audit before deletion. Merge any unique durable facts into
the canonical docs (`architecture.md`, `codemap.md`, `AGENTS.md`). The
`decision-log.md` in `memory-bank/` and the plan files are the main candidates
for one-time merging.

### R3: Breaking agent routing during migration

**Risk**: If `AGENTS.md` is updated before `memory-bank/` is removed (or vice
versa), agents may reference non-existent files or follow stale paths.

**Mitigation**: Execute the migration atomically — all file deletions, doc
updates, and config changes land in a single commit. The tasks section of this
OpenSpec change enforces this ordering.

### R4: No `instructions` in `opencode.json` may change session startup

**Risk**: Removing the `instructions` array means OpenCode no longer injects
`activeContext.md` / `progress.md` at session start. This is intentional, but
some agents may have relied on that auto-injection.

**Mitigation**: The OpenSpec apply-change skill already reads all relevant
context files for a change. Agents following the `repository-governance` spec
will read `AGENTS.md` and the change's artifacts — no auto-injected scratchpad
is needed.
