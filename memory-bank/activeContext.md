# Active context

Last updated: 2026-05-27

## Current objective

Keep the documentation split stable: `architecture.md` is the normative
architecture guide and `codemap.md` is the live repository atlas.
Keep the release workflow from publishing empty releases when signing secrets
are absent, while preserving the lean memory-bank startup flow.

## Current state

- Core app structure is already in place: source plugins, repositories, Room
  persistence, ViewModels, Compose screens/content, workers, and journey
  tooling all exist in the repo.
- The memory-bank workflow remains lean: `activeContext.md` and `progress.md`
  are still the core task-start files.
- The documentation split plan in
  `plans/2026-05-27-architecture-codemap-doc-split.md` has been implemented.
- `architecture.md` now focuses on durable rules, contracts, invariants, and
  decisions while `codemap.md` owns live structure and implementation mapping.
- `.github/workflows/release.yml` now records whether the build is signed or
  unsigned, prefers a signed APK when present, falls back to an unsigned APK,
  and fails the workflow if no release APK is produced.
- The memory bank was normalized on 2026-05-27:
  - `active-context.md` was renamed to `activeContext.md`
  - duplicated guidance was folded into `projectbrief.md`,
    `productContext.md`, `systemPatterns.md`, and `techContext.md`
  - startup workflow docs now point to the two-file core memory flow
  - historical investigation notes were moved out of `memory-bank/`

## Active decisions

- Release publication must never proceed with an empty APK path.
- The release workflow should publish a signed APK when available and fall back
  to the unsigned APK only when that is the artifact actually produced.
- `activeContext.md` and `progress.md` are the only core task-start memory
  files.
- `projectbrief.md`, `productContext.md`, `systemPatterns.md`, and
  `techContext.md` are lazy-load references.
- The parent/orchestrator owns memory-bank reads and writes.
- Specialists should receive a short summary and only the single relevant
  memory file when needed.
- `architecture.md` is the normative architecture document.
- `codemap.md` is the descriptive repository atlas.
- `AGENTS.md` routes humans and agents to the right document by need.

## Known constraints

- Do not load the whole `memory-bank/` by default.
- Keep detailed investigations, one-off plans, and archival notes outside the
  core memory flow.
- Update `activeContext.md` and `progress.md` before ending a task.
- Update the stable memory files only when durable product, architecture, or
  tooling knowledge changed.

## Relevant files

- `AGENTS.md`
- `architecture.md`
- `codemap.md`
- `plans/2026-05-27-architecture-codemap-doc-split.md`
- `memory-bank/activeContext.md`
- `memory-bank/progress.md`

## Next safe action

On future documentation updates, change only the owning document: structural
navigation in `codemap.md`, architectural rules and decisions in
`architecture.md`.
On the next release-related task, verify the workflow against a tagged build or
manual dispatch path, then continue using the two-file core memory startup flow
for normal work.
