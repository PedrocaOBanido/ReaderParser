## Why

The current OpenCode workflow has two structural gaps that let work leak across
agent boundaries:

1. **Build lane owns too much.** The global prompt's "build" agent can run
   `git add`, `git commit`, `./gradlew`, and verification tasks in the same
   turn. This blurs the line between "make code compile" and "verify the
   change is green" and makes it impossible to enforce a clean review gate
   before commits land.

2. **Apply and archive stop too early.** `/opsx-apply` declares done when
   all tasks are checked off, but never runs a reviewer or runner pass. The
   orchestrator has to remember to dispatch those manually. `/opsx-archive`
   moves the change directory to archive but does not handle the git/PR/CI
   flow that every non-trivial change needs on `origin/main` (branch → PR →
   CI → merge).

The result is that reviewers and runners are invoked inconsistently, and the
git/PR/CI lifecycle is a manual afterthought rather than a gated step in the
workflow.

## What Changes

- **New `integrator` agent/lane** that owns all git staging, commit, branch
  push, PR creation, CI watch, and merge-on-green work. Build and other
  agents lose direct git write access.
- **`opsx-apply` (command + skill)** gains a post-implementation correction
  cycle: after all tasks are implemented, the orchestrator dispatches
  `runner` (verify green) and `reviewer` (diff review) in a loop until
  both pass, then marks implementation complete.
- **`opsx-archive` (command + skill)** gains a post-archive integration
  phase: after archiving the change directory, it dispatches `integrator`
  to group commits by prefix, push a branch, create a PR, watch CI, and
  merge on green.
- **`AGENTS.md`** adds `integrator` to the project specialists list.
- **`.opencode/opencode.json`** adds per-agent permission overrides:
  - `agent.build.permission.bash`: denies git writes, `gh`, and gradle
    verification tasks — build implements, it does not verify or integrate.
  - `agent.runner.permission.bash`: allows the verification gradle tasks
    it owns and read-only git inspection (`git status`, `git diff`).
  - `agent.reviewer.permission.bash`: allows read-only git inspection
    only; gradle remains denied.
  - `agent.integrator.permission.bash`: allows the git/gh workflow
    commands it owns (add, commit, push, branch, gh pr, gh run).
  - Global `git add *`/`git commit *` change to `"ask"` as the baseline.

## Capabilities

### New Capabilities

- `agent-lane-hardening`: Normative requirements for agent lane boundaries,
  the integrator role, and the apply/archive correction and integration
  cycles.

### Modified Capabilities

- `repository-governance`: Adds integrator lane to the agent routing table
  and clarifies that git/PR/CI work belongs to a dedicated lane, not to
  the build agent.

## Impact

- **Workflow config** (primary): new `integrator.md` agent definition,
  updated `opencode.json` permissions, updated `opsx-apply` and
  `opsx-archive` commands and skills.
- **Repo routing docs**: `AGENTS.md` specialist list gains `integrator`.
- **No application code changes**: this change only touches `.opencode/`
  configuration, workflow files, and OpenSpec artifacts.
- **No dependency or build changes**: Gradle, Hilt, Room, Ktor are
  unaffected.
