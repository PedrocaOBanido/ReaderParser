## MODIFIED Requirements

### Requirement: Agent routing lives in AGENTS.md

> **Previous:** AGENTS.md lists specialist lanes and routing rules.
>
> **Updated:** AGENTS.md lists specialist lanes including `integrator` for
> git/PR/CI work. Git write operations (staging, commit, branch, push) and
> PR/CI/merge work belong to the `integrator` lane. Build and other agents
> SHALL NOT run git write commands directly.

#### Scenario: Integrator owns git/PR/CI lifecycle

- **WHEN** a change needs to be pushed to `origin/main`
- **THEN** the `integrator` agent SHALL handle branch creation, push, PR
  creation, CI watching, and merge-on-green

#### Scenario: Build agent does not run git commands

- **WHEN** the build agent is dispatched for a task
- **THEN** it SHALL NOT run `git add`, `git commit`, `git push`, or any
  other git write command; it SHALL delegate to `integrator` if git
  operations are needed

#### Scenario: Agent permission model enforces lane boundaries

- **WHEN** `.opencode/opencode.json` is configured
- **THEN** each agent SHALL have per-agent `permission.bash` overrides
  that deny commands outside its lane and allow commands within its lane,
  and global `git add`/`git commit` defaults SHALL be `"ask"`
