## Requirements

### Requirement: Build agent SHALL NOT run git commands or verification tasks

The build agent (or any non-integrator agent) SHALL NOT run `git add`, `git commit`, `git push`, or any other git write command. Verification tasks (`./gradlew :app:assembleDebug`, `./gradlew :app:lintDebug`, `./gradlew :app:testDebugUnitTest`) belong to the `runner` lane, not to the build agent.

#### Scenario: Build agent attempts git commit

- **WHEN** the build agent is dispatched and the task involves `git add` or `git commit`
- **THEN** the build agent SHALL delegate to `integrator` instead of executing the git command directly

#### Scenario: Build agent runs verification

- **WHEN** the orchestrator needs to verify a build is green
- **THEN** the orchestrator SHALL dispatch `runner`, not `build`, for verification tasks

### Requirement: Integrator lane owns git staging, commit, branch, push, PR, CI, and merge

A dedicated `integrator` agent SHALL own all git write operations (staging, commit, branch creation, push), PR creation, CI check watching, and merge-on-green. No other agent SHALL perform these operations directly.

#### Scenario: Integrator groups commits by prefix

- **WHEN** the integrator is dispatched to prepare a change for PR
- **THEN** it SHALL group commits by their prefix (feat, fix, refactor, ci, cd, docs) and ensure each commit follows the repo's commit conventions

#### Scenario: Integrator creates branch and PR

- **WHEN** the integrator has grouped commits on a feature branch
- **THEN** it SHALL push the branch and create a PR with a title and body describing the change

#### Scenario: Integrator watches CI and merges on green

- **WHEN** a PR is created and CI checks are running
- **THEN** the integrator SHALL watch the checks and, when all pass, merge the PR using a merge commit (not squash) to preserve grouped commits

#### Scenario: CI checks fail

- **WHEN** CI checks fail on the PR
- **THEN** the integrator SHALL report the failure and pause, not auto-merge

### Requirement: Apply flow includes runner and reviewer correction cycle

After all tasks in an OpenSpec change are implemented, the apply flow SHALL dispatch `runner` (verification suite) and then `reviewer` (diff review). If either reports failures or blockers, the cycle SHALL loop: fix → re-verify → re-review, until both pass. The correction cycle SHALL have a maximum of 3 iterations before pausing and asking the user.

#### Scenario: Runner reports failures after implementation

- **WHEN** all tasks are checked off and runner is dispatched
- **THEN** runner SHALL run assembleDebug + lintDebug + testDebugUnitTest
- **AND** if any task fails, the apply flow SHALL dispatch a writer agent to fix the failure, then re-run runner

#### Scenario: Reviewer reports blockers after implementation

- **WHEN** runner passes and reviewer is dispatched
- **THEN** reviewer SHALL inspect the diff for policy, architecture, and test issues
- **AND** if any BLOCKER or SHOULD-FIX findings exist, the apply flow SHALL dispatch a writer agent to address them, then re-run the correction cycle

#### Scenario: Correction cycle exceeds maximum iterations

- **WHEN** the correction cycle has looped 3 times without both runner and reviewer passing
- **THEN** the apply flow SHALL pause and display the remaining issues to the user for guidance

#### Scenario: Both runner and reviewer pass

- **WHEN** runner reports green and reviewer reports "ready"
- **THEN** the apply flow SHALL declare "Implementation Complete — Ready to archive"

### Requirement: Archive flow includes integrator phase

After archiving the change directory and syncing delta specs, the archive flow SHALL dispatch `integrator` to prepare the change for merge into `origin/main`. The integrator SHALL group commits, create a feature branch, push, create a PR, watch CI, and merge on green.

#### Scenario: Archive triggers integrator

- **WHEN** the change directory is archived and delta specs are synced
- **THEN** the archive flow SHALL dispatch `integrator` to handle the git/PR/CI lifecycle

#### Scenario: Integrator creates feature branch

- **WHEN** integrator is dispatched after archive
- **THEN** it SHALL create a branch named `change/<change-name>` from the current HEAD

#### Scenario: Integrator creates PR and watches CI

- **WHEN** the feature branch is pushed
- **THEN** integrator SHALL create a PR and watch CI checks until they complete

#### Scenario: CI passes and integrator merges

- **WHEN** all CI checks pass on the PR
- **THEN** integrator SHALL merge the PR using a merge commit and report success

#### Scenario: User confirmation before merge

- **WHEN** CI is green and integrator is ready to merge
- **THEN** integrator SHALL surface the PR URL and ask the user to confirm before merging

### Requirement: Permission model reflects lane boundaries

The `.opencode/opencode.json` permission configuration SHALL enforce lane
boundaries at two levels: global defaults and per-agent overrides.

**Global defaults:** `git add *` and `git commit *` SHALL be `"ask"`. This
is the baseline — any agent that needs git writes must have an explicit
per-agent override.

**Per-agent `permission.bash` overrides:**

- **`build`**: `git *` (writes), `gh *`, `./gradlew :app:assembleDebug`,
  `./gradlew :app:lintDebug`, `./gradlew :app:testDebugUnitTest` SHALL be
  `deny`. Build implements code; it SHALL NOT verify, lint, test, or
  integrate.
- **`runner`**: `./gradlew :app:assembleDebug`,
  `./gradlew :app:lintDebug`, `./gradlew :app:testDebugUnitTest`,
  `./gradlew :app:ktlintCheck`, `./gradlew :app:ktlintFormat`,
  `./gradlew :app:detekt` SHALL be `allow`. `git status`, `git diff`,
  `git log`, `git show` SHALL be `allow` (read-only inspection). Runner
  owns verification.
- **`reviewer`**: `git status`, `git diff`, `git log`, `git show` SHALL
  be `allow` (read-only inspection). `./gradlew *` SHALL be `deny`.
  Reviewer reads diffs only; never runs builds.
- **`integrator`**: `git add`, `git commit`, `git push`, `git branch`,
  `git checkout`, `git switch`, `gh pr create`, `gh pr merge`,
  `gh pr checks`, `gh run view` SHALL be `allow`. Integrator owns the
  full git/PR/CI lifecycle.

#### Scenario: Build agent denied verification and git writes

- **WHEN** the build agent is dispatched
- **THEN** `./gradlew :app:assembleDebug`, `./gradlew :app:lintDebug`,
  `./gradlew :app:testDebugUnitTest`, `git *` (writes), and `gh *` SHALL
  be `deny` in its permission context

#### Scenario: Runner allowed verification and read-only git

- **WHEN** the runner agent is dispatched
- **THEN** the verification gradle tasks it owns SHALL be `allow`, and
  `git status`/`git diff`/`git log`/`git show` SHALL be `allow`

#### Scenario: Reviewer allowed read-only git, denied gradle

- **WHEN** the reviewer agent is dispatched
- **THEN** `git status`/`git diff`/`git log`/`git show` SHALL be `allow`
  and `./gradlew *` SHALL be `deny`

#### Scenario: Integrator allowed git/gh workflow commands

- **WHEN** the integrator agent is dispatched
- **THEN** `git add`, `git commit`, `git push`, `git branch`,
  `git checkout`, `git switch`, `gh pr create`, `gh pr merge`,
  `gh pr checks`, `gh run view` SHALL be `allow`

#### Scenario: Non-integrator agent attempts git commit

- **WHEN** any agent other than integrator runs `git add` or `git commit`
- **THEN** the permission system SHALL require user confirmation (`"ask"`)
  or deny the command, depending on the agent's per-agent override

#### Scenario: Integrator runs git commit

- **WHEN** the integrator agent runs `git add` or `git commit`
- **THEN** the permission system SHALL allow it without confirmation
  (`"allow"`)

### Requirement: AGENTS.md documents integrator lane

The root `AGENTS.md` file SHALL list `integrator` in the project specialists section with a description of its responsibilities: git staging, commit, branch, push, PR creation, CI watch, and merge-on-green.

#### Scenario: Integrator appears in specialists list

- **WHEN** an agent reads `AGENTS.md` to understand available lanes
- **THEN** `integrator` SHALL appear in the project specialists list with its routing keywords
