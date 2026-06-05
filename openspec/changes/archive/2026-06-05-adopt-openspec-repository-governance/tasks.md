# Tasks: Adopt OpenSpec Repository Governance

## 1. Audit and migrate durable content from memory-bank

- [x] 1.1 Read each `memory-bank/` file and identify unique durable content not
  already in `README.md`, `architecture.md`, `codemap.md`, or `AGENTS.md`
- [x] 1.2 Merge `memory-bank/commit-conventions.md` commit-prefix table and
  rules into `AGENTS.md` (no existing equivalent)
- [x] 1.3 Merge `memory-bank/techContext.md` test-utilities paths into
  `architecture.md` testing section if not already covered
- [x] 1.4 Merge `memory-bank/systemPatterns.md` screen-pattern and
  source-pattern details into `architecture.md` if any rules are not already
  present
- [x] 1.5 Merge any unique durable facts from `memory-bank/projectbrief.md`
  and `memory-bank/productContext.md` into `README.md` if not already covered
- [x] 1.6 Confirm `memory-bank/conventions.md`, `test-strategy.md`,
  `directory-map.md`, and `README.md` (inside memory-bank) are stubs/retired
  with no unique content to migrate
- [x] 1.7 Confirm `memory-bank/activeContext.md`, `progress.md`, and
  `decision-log.md` are transient/historical and need no migration

## 2. Establish the repository-governance spec

- [x] 2.1 Create `openspec/specs/repository-governance/spec.md` by copying
  `openspec/changes/adopt-openspec-repository-governance/specs/repository-governance/spec.md`
  to its canonical location
- [x] 2.2 Verify the copied spec is complete and matches the design decisions
  in `design.md` (requirements for OpenSpec-required work, trivial exemptions,
  canonical doc ownership, no transient stores, archive expectations)

## 3. Update canonical documentation files

- [x] 3.1 Rewrite `AGENTS.md`:
  - remove the entire "Memory Bank" section (lines 135–155)
  - remove `memory-bank/` references from "Context retrieval rule" (lines 156–171)
  - replace with a shorter context-retrieval rule: directly referenced file,
    nearest `AGENTS.md`, then `architecture.md`/`codemap.md` when needed
  - add a "Workflow" section pointing at OpenSpec for non-trivial changes and
    the `repository-governance` spec for policy
  - keep all non-negotiables, specialist lanes, placement rules, testing, and
    verification sections unchanged
- [x] 3.2 Update `README.md`:
  - add `codemap.md` and `openspec/specs/` to the Documentation table
  - remove references to `project-structure.md` and `kickoff-prompt.md` if they
    no longer exist or are not canonical
  - update the "Working with the opencode agent" section to reference OpenSpec
    workflow instead of `/start` and `memory-bank/`
- [x] 3.3 Update `.opencode/opencode.json`:
  - remove the `"instructions"` array that points at
    `memory-bank/activeContext.md` and `memory-bank/progress.md`
- [x] 3.4 Update `architecture.md` if any new content was merged from
  memory-bank files in step 1

## 4. Remove old workflow files

- [x] 4.1 Delete `.opencode/command/start.md`
- [x] 4.2 Delete `.opencode/command/light-start.md`
- [x] 4.3 Confirm `.opencode/command/run-plan.md` does not exist (already
  absent) — if present, delete it
- [x] 4.4 Delete the entire `memory-bank/` directory (all 12 files)
- [x] 4.5 Delete the entire `plans/` directory (all files)

## 5. Clean up stale references

- [x] 5.1 Grep the repo for remaining references to `memory-bank/` and remove
  or update any occurrences outside of `openspec/changes/`
  (also cleaned `.understand-anything/fingerprints.json` and
  `.understand-anything/knowledge-graph.json` stale entries in follow-up)
- [x] 5.2 Grep the repo for remaining references to `plans/` and remove or
  update any occurrences outside of `openspec/changes/`
  (also removed `plans/` from `.gitignore` in follow-up)
- [x] 5.3 Grep the repo for references to `/start`, `/light-start`, and
  `/run-plan` and update or remove them
- [x] 5.4 Grep for references to `activeContext.md`, `progress.md`,
  `projectbrief.md`, `productContext.md`, `systemPatterns.md`, and
  `techContext.md` as standalone paths and clean up stale pointers
- [x] 5.5 Update any `.opencode/skills/` files that reference `memory-bank/`
  or `plans/` paths

## 6. Validate coherence

- [x] 6.1 Read `AGENTS.md` end-to-end and confirm no dangling file references
- [x] 6.2 Read `.opencode/opencode.json` and confirm no deleted file paths
  remain
- [x] 6.3 Read `README.md` and confirm documentation table and agent-routing
  section are accurate
- [x] 6.4 Confirm `openspec/specs/repository-governance/spec.md` exists and
  is the sole source of governance policy
- [x] 6.5 Confirm `memory-bank/` and `plans/` directories no longer exist
- [x] 6.6 Confirm `.opencode/command/start.md` and `light-start.md` no longer
  exist
- [x] 6.7 Run `./gradlew :app:assembleDebug` to verify no build breakage from
  doc-only changes
