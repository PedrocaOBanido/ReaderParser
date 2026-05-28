# Progress

Last updated: 2026-05-27

## Completed

- Fixed `.github/workflows/release.yml` so release publication now prefers a
  signed APK, falls back to the unsigned APK when signing secrets are absent,
  and fails clearly if no APK artifact is produced.
- Historical build-out phases 0–8 are materially present in the repo: test
  infrastructure, source contract, first source plugin, repository layer,
  ViewModels, Compose screens/content, worker flows, edge-case coverage, and
  journey tooling.
- The memory-bank audit confirmed that Compose screen/content files,
  navigation, workers, and journey scripts already exist, so the old phase
  notes were stale.
- The memory-bank workflow was normalized:
  - `activeContext.md` and `progress.md` are now the core task-start files
  - `projectbrief.md`, `productContext.md`, `systemPatterns.md`, and
    `techContext.md` were added as lazy-load references
  - `AGENTS.md`, startup commands, specialist guidance, and OpenCode config now
    route through the lean memory flow
- Historical debug notes were moved out of `memory-bank/`, and duplicated rule
  files were retired from the core workflow.
- A concrete plan for splitting architecture documentation from codemap
  responsibilities was saved to
  `plans/2026-05-27-architecture-codemap-doc-split.md`.
- The documentation split is now implemented:
  - `AGENTS.md` explicitly routes doc usage by responsibility
  - `architecture.md` is a shorter normative guide focused on rules and
    contracts
  - `codemap.md` explicitly owns repository navigation and implementation maps

## In progress

- None.

## Blocked

- None.

## Known issues

- Build/test health should be re-verified on demand when code changes resume;
  stale blocker notes should not be carried forward without revalidation.
- The release workflow fix was validated by inspection and should be exercised
  on the next release run to confirm end-to-end asset upload behavior.

## Verification commands

- Docs/workflow consistency:
  - inspect `AGENTS.md`
  - inspect `architecture.md`
  - inspect `codemap.md`
  - inspect `plans/2026-05-27-architecture-codemap-doc-split.md`
  - inspect `.opencode/command/start.md`
  - inspect `.opencode/command/light-start.md`
  - inspect `.opencode/opencode.json`
  - inspect `memory-bank/`
- Release workflow:
  - inspect `.github/workflows/release.yml`
- App verification when source or build files change:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:lintDebug`
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:detekt`
  - `./gradlew :app:ktlintCheck`
