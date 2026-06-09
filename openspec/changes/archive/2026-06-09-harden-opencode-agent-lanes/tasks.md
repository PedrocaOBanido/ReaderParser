## 1. Integrator agent definition

- [x] 1.1 Create `.opencode/agent/integrator.md` with frontmatter: description ("Owns git staging, commit, branch, push, PR creation, CI watch, and merge-on-green. The integration lane that gets changes onto origin/main."), mode (subagent), permission block (edit: deny, skill: deny, task: deny), and agent metadata (class: I, owns: git staging/commit/branch/push/PR/CI/merge, reads: git status/log/diff, routing: git, commit, branch, push, PR, CI, merge, integrator)
- [x] 1.2 Write the integrator body: workflow sections for (1) identify the change and group commits by prefix, (2) create feature branch `change/<change-name>`, (3) push branch, (4) create PR with grouped commit summary, (5) watch CI checks, (6) merge on green with user confirmation, (7) report result
- [x] 1.3 Add hard rules to integrator.md: never edit source files, never run gradle tasks, never merge without user confirmation, never force-push, always use merge commit (not squash) to preserve grouped commits

## 2. Permission model update

- [x] 2.1 In `.opencode/opencode.json`, change `"git add *": "allow"` to `"git add *": "ask"`
- [x] 2.2 In `.opencode/opencode.json`, change `"git commit *": "allow"` to `"git commit *": "ask"`
- [x] 2.3 In `.opencode/opencode.json`, add `"integrator": "allow"` to the `permission.task` block
- [x] 2.4 In `.opencode/opencode.json`, add `agent.build.permission.bash` block: `"git *": "deny"`, `"gh *": "deny"`, `"./gradlew :app:assembleDebug": "deny"`, `"./gradlew :app:lintDebug": "deny"`, `"./gradlew :app:testDebugUnitTest": "deny"` — build implements, never verifies or integrates
- [x] 2.5 In `.opencode/opencode.json`, add `agent.runner.permission.bash` block: `"./gradlew :app:assembleDebug": "allow"`, `"./gradlew :app:lintDebug": "allow"`, `"./gradlew :app:testDebugUnitTest": "allow"`, `"./gradlew :app:ktlintCheck": "allow"`, `"./gradlew :app:ktlintFormat": "allow"`, `"./gradlew :app:detekt": "allow"`, `"git status *": "allow"`, `"git diff *": "allow"`, `"git log *": "allow"`, `"git show *": "allow"` — runner owns verification and read-only git inspection
- [x] 2.6 In `.opencode/opencode.json`, add `agent.reviewer.permission.bash` block: `"git status *": "allow"`, `"git diff *": "allow"`, `"git log *": "allow"`, `"git show *": "allow"`, `"./gradlew *": "deny"` — reviewer reads diffs only, never runs builds
- [x] 2.7 In `.opencode/opencode.json`, add `agent.integrator.permission.bash` block: `"git add *": "allow"`, `"git commit *": "allow"`, `"git push *": "allow"`, `"git branch *": "allow"`, `"git checkout *": "allow"`, `"git switch *": "allow"`, `"gh pr create *": "allow"`, `"gh pr merge *": "allow"`, `"gh pr checks *": "allow"`, `"gh run view *": "allow"` — integrator owns the full git/PR/CI lifecycle

## 3. AGENTS.md specialist list

- [x] 3.1 Add `- \`integrator\` — git staging, commit, branch, push, PR creation, CI watch, merge-on-green.` to the project specialists list in root `AGENTS.md`, after the `journey-runner` entry

## 4. Apply command and skill update

- [x] 4.1 In `.opencode/commands/opsx-apply.md`, add step 7 (after "Implement tasks" loop): dispatch `runner` with verification suite (assembleDebug + lintDebug + testDebugUnitTest). If any task fails, report failures and pause for fix before continuing.
- [x] 4.2 In `.opencode/commands/opsx-apply.md`, add step 8: dispatch `reviewer` with the current diff. If BLOCKER or SHOULD-FIX findings exist, report them and pause for fix before continuing.
- [x] 4.3 In `.opencode/commands/opsx-apply.md`, add step 9: if both runner and reviewer pass, declare "Implementation Complete — Ready to archive". If correction cycle exceeded 3 iterations, pause and show remaining issues.
- [x] 4.4 In `.opencode/skills/openspec-apply-change/SKILL.md`, mirror the same post-implementation correction cycle (runner + reviewer loop) as steps 7-9

## 5. Archive command and skill update

- [x] 5.1 In `.opencode/commands/opsx-archive.md`, add step 6 (after archive + spec sync): dispatch `integrator` to handle the git/PR/CI lifecycle
- [x] 5.2 In `.opencode/commands/opsx-archive.md`, add step 7: integrator creates branch `change/<change-name>`, groups commits by prefix, pushes, creates PR, watches CI, and merges on green with user confirmation
- [x] 5.3 In `.opencode/commands/opsx-archive.md`, update the output templates to include integration status (PR URL, CI status, merge result)
- [x] 5.4 In `.opencode/skills/openspec-archive-change/SKILL.md`, mirror the same post-archive integration phase as steps 6-7

## 6. Repository governance spec delta

- [x] 6.1 Create delta spec at `openspec/changes/harden-opencode-agent-lanes/specs/repository-governance/spec.md` with MODIFIED requirements: add integrator to agent routing table, clarify that git/PR/CI work belongs to integrator not build
