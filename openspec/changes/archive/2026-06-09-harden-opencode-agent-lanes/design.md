## Context

ReaderParser's OpenCode workflow currently has seven specialist lanes
(source-author, screen-author, room-migration, domain-author, runner,
reviewer, journey-runner) plus the global orchestrator and the "build"
subagent. The build subagent runs gradle tasks and can also run `git add`,
`git commit`, and verification tasks. The runner already handles verification
(build, lint, test), and the reviewer handles read-only diff review. But
there is no lane that owns the git/PR/CI lifecycle: branching, pushing,
PR creation, CI watching, and merge-on-green. That work either happens
ad-hoc in the orchestrator or gets skipped entirely.

The apply and archive commands (`opsx-apply`, `opsx-archive`) are the two
main entry points for executing a change. Neither currently enforces a
quality gate between "tasks done" and "change is ready to ship":

- `opsx-apply` marks tasks complete and suggests archive. The orchestrator
  must remember to dispatch runner and reviewer separately.
- `opsx-archive` moves the change directory and syncs delta specs. The
  orchestrator must handle git grouping, branch push, PR creation, CI
  watch, and merge manually.

This change hardens both flows and introduces the missing lane.

## Goals / Non-Goals

**Goals:**

- Every completed change passes through runner (verify green) and reviewer
  (diff review) before being declared implementation-complete.
- Every non-trivial change goes through branch → PR → CI → merge via a
  dedicated `integrator` lane.
- Build and other non-integrator agents lose direct `git add`/`git commit`
  permissions in the permission model.
- Apply and archive commands encode the full lifecycle so the orchestrator
  does not have to remember steps.

**Non-Goals:**

- Changing the Source interface, entity identity, or any application code.
- Replacing Hilt, Ktor, Room, or any framework.
- Adding new third-party dependencies.
- Changing the OpenSpec schema or CLI itself.
- Automating releases or CD (this change covers CI, not CD).

## Decisions

### D1: Integrator as a dedicated agent, not a runner extension

**Decision:** Create a new `integrator` agent (`.opencode/agent/integrator.md`)
that owns all git staging, commit, branch, push, PR creation, CI watch, and
merge-on-green work. The runner continues to own only gradle verification
tasks.

**Rationale:** Runner's scope is already well-defined: build, lint, test,
format. Adding git/PR/CI work to runner would blur its "verify gate" role.
A dedicated integrator keeps separation of concerns clean: runner says
"the code is green", integrator says "the change is on main".

**Alternative considered:** Extend the orchestrator to own git/PR/CI directly.
Rejected because the orchestrator should delegate, not execute, and
encoding the flow in a specialist makes it reusable and testable.

### D2: Per-agent permission hardening in opencode.json

**Decision:** Two-layer permission model in `.opencode/opencode.json`:

1. **Global defaults** — `git add *` and `git commit *` change from
   `"allow"` to `"ask"`. This is the floor: any agent that needs git
   writes must have an explicit per-agent override.

2. **Per-agent `permission.bash` overrides** — each agent gets a
   targeted allow/deny list matching its lane:

   | Agent | bash permissions | Rationale |
   |---|---|---|
   | `build` | `deny` for `git *` (writes), `gh *`, `./gradlew :app:assembleDebug`, `./gradlew :app:lintDebug`, `./gradlew :app:testDebugUnitTest` | Build implements code; it must not verify, lint, test, or integrate |
   | `runner` | `allow` for `./gradlew :app:assembleDebug`, `./gradlew :app:lintDebug`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:ktlintCheck`, `./gradlew :app:ktlintFormat`, `./gradlew :app:detekt`; `allow` for `git status`, `git diff`, `git log`, `git show` | Runner owns verification; read-only git for context |
   | `reviewer` | `allow` for `git status`, `git diff`, `git log`, `git show`; `deny` for `./gradlew *` | Reviewer reads diffs only; never runs builds |
   | `integrator` | `allow` for `git add`, `git commit`, `git push`, `git branch`, `git checkout`, `git switch`, `gh pr create`, `gh pr merge`, `gh pr checks`, `gh run view` | Integrator owns the full git/PR/CI lifecycle |

**Rationale:** Global `"ask"` is not enough. Without per-agent overrides,
the permission model cannot distinguish "build should never run lint" from
"runner should always run lint". Per-agent `permission.bash` blocks make
lane boundaries enforceable at the config level, not just in agent
prompts.

**Alternative considered:** Keep `allow` globally and rely on agent
definitions to self-police. Rejected because the permission model is
the enforcement mechanism; relying on agent prompts is softer than
relying on the permission config.

### D3: Apply flow gains runner+reviewer correction cycle

**Decision:** After all tasks are checked off in `opsx-apply`, the command
dispatches `runner` (verification suite: assembleDebug + lintDebug +
testDebugUnitTest) and then `reviewer` (diff review). If either reports
failures/blockers, the cycle loops: fix → re-verify → re-review, until
both pass. Only then does it declare "Implementation Complete — Ready to
archive."

**Rationale:** This front-loads quality gates into the apply flow instead
of relying on the orchestrator to remember. The correction loop ensures
that lint failures, test failures, and policy violations are caught before
the change reaches archive.

### D4: Archive flow gains integrator phase

**Decision:** After archiving the change directory in `opsx-archive`, the
command dispatches `integrator` to:
1. Group commits by prefix (feat, fix, refactor, ci, cd, docs).
2. Create a feature branch named `change/<change-name>`.
3. Push the branch.
4. Create a PR with the grouped commits.
5. Watch CI checks.
6. Merge on green (merge commit, not squash, to preserve grouped commits).

**Rationale:** The archive step syncs delta specs and moves the directory.
The integration step gets the change onto `origin/main`. Encoding this
in the archive command ensures every change goes through the same
lifecycle.

### D5: Delta spec for agent-lane-hardening

**Decision:** Create a delta spec at
`openspec/changes/harden-opencode-agent-lanes/specs/agent-lane-hardening/spec.md`
that captures the normative requirements for lane boundaries, the
integrator role, and the apply/archive correction and integration cycles.

**Rationale:** These are durable workflow rules that belong in a spec,
not just in task instructions. The delta spec will be synced to a main
spec at `openspec/specs/agent-lane-hardening/spec.md` during archive.

### D6: Modifying repository-governance spec

**Decision:** Add a delta to the existing `repository-governance` spec
capturing the new requirement that git/PR/CI work belongs to the
integrator lane, not to the build agent.

**Rationale:** The governance spec already owns agent routing rules.
The integrator addition is a routing change, not a new capability.

## Risks / Trade-offs

- **[Risk] Integrator agent adds token cost** → Mitigated by keeping the
  agent definition small and routing only git/PR/CI work to it. Most
  changes dispatch integrator once at the end, not per-task.

- **[Risk] Correction cycle in apply may loop** → Mitigated by a maximum
  of 3 correction iterations before pausing and asking the user. This
  prevents infinite fix→verify loops on persistent failures.

- **[Risk] Tightening git permissions may break existing workflows** →
  Mitigated by the fact that the orchestrator already delegates most
  git work. The change is from "any agent can commit" to "only
  integrator commits", which matches actual usage.

- **[Trade-off] Merge commit vs squash merge** → Chose merge commit to
  preserve grouped commits (feat, fix, refactor per commit). Squash
  would lose the per-prefix grouping that the commit conventions
  require.

- **[Trade-off] Integrator owns merge** → Could let the user merge
  manually after CI passes. Chose automated merge-on-green to
  close the loop completely, but the integrator will surface the
  PR URL and wait for user confirmation before merging.
