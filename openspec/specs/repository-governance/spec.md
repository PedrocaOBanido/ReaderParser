# repository-governance

## Purpose

Defines the repo's change-management policy: which workflow applies to which
class of work, what the authoritative durable documentation files are, and what
constitutes a trivial exception exempt from the OpenSpec workflow.

---

## ADDED Requirements

### Requirement: Non-trivial work MUST start as an OpenSpec change

Any work that adds, modifies, or removes functionality, behavior, data flow,
or public interface MUST begin by creating an OpenSpec change under
`openspec/changes/` before code or documentation edits begin. The change MUST
include at minimum a `proposal.md` describing what and why.

#### Scenario: Feature development requires an OpenSpec change

- **WHEN** an agent or contributor intends to implement a new feature, fix a
  non-trivial bug, refactor existing behavior, or add/remove a repository
  capability
- **THEN** the agent MUST create an OpenSpec change directory with a
  `proposal.md` BEFORE writing any implementation code or modifying canonical
  documentation files

#### Scenario: Implementation begins only after artifacts exist

- **WHEN** an OpenSpec change has been created for non-trivial work
- **THEN** implementation MUST NOT begin until the change's
  `applyRequires` artifacts (as defined by the schema) are in `done` status
  per `openspec status --change "<name>" --json`

### Requirement: Trivial and read-only work MAY proceed without an OpenSpec change

The following categories of work are explicitly exempt from the OpenSpec
workflow. This list is exhaustive; any work not listed here is non-trivial and
MUST follow the OpenSpec workflow.

- **Read-only exploration**: answering questions, grepping the codebase,
  inspecting files, generating temporary knowledge graphs or dashboards
- **Direct conversation**: answering user questions about the codebase, architecture, or design without making changes
- **Single-file cosmetic fixes**: whitespace normalization, typo correction in
  comments or strings, import ordering — provided no behavioral change occurs
- **Dependency version bumps**: updating a dependency version in a build file
  when the API surface is unchanged, provided no code edits are required beyond
  the version string
- **CI/tooling configuration**: editing `.editorconfig`, linter configs, or
  similar non-functional tooling files when no behavioral change results

#### Scenario: Read-only codebase exploration

- **WHEN** an agent or contributor is asked to explain, search, or analyze code
  without making changes
- **THEN** no OpenSpec change is required, and the agent MAY proceed directly

#### Scenario: Typo fix in a comment

- **WHEN** an agent or contributor corrects a single typo inside a code comment
  or string literal with no behavioral change
- **THEN** no OpenSpec change is required, and the agent MAY commit directly

#### Scenario: Work outside the trivial list requires an OpenSpec change

- **WHEN** an agent or contributor intends to perform work that does not match
  any category in the trivial-exemption list
- **THEN** the agent MUST create an OpenSpec change before proceeding, even if
  the change is small in scope

### Requirement: Canonical documentation files have defined ownership

The following files are the authoritative durable truth for repository
conventions, architecture, and navigation. Each file has a defined scope. No
other file may redefine or duplicate the content owned by these files.

| File | Owner scope |
| --- | --- |
| `README.md` | Project purpose, setup instructions, quick-start guide |
| `architecture.md` | Normative architecture rules, layer contracts, invariants |
| `codemap.md` | Repository structure, directory maps, entry points, navigation |
| `AGENTS.md` | Agent routing, specialist lanes, non-negotiables, delegation rules |
| `openspec/specs/<capability>/spec.md` | Per-capability normative requirements and scenarios |
| `openspec/changes/<name>/` | Per-change proposal, design, tasks, and delta specs |

#### Scenario: Architecture decision lives in architecture.md

- **WHEN** a durable architectural rule, layer contract, or invariant is
  established or changed
- **THEN** the rule MUST be recorded in `architecture.md` and MUST NOT be
  duplicated in `README.md`, `codemap.md`, or `AGENTS.md`

#### Scenario: Repository structure lives in codemap.md

- **WHEN** a new directory, entry point, or navigation path is added or changed
- **THEN** `codemap.md` MUST be updated to reflect the current structure, and
  no other canonical file may redefine the repository map

#### Scenario: Agent routing lives in AGENTS.md

- **WHEN** an agent workflow, specialist lane, or delegation rule is added or
  changed
- **THEN** `AGENTS.md` MUST be updated, and no other canonical file may redefine
  agent behavior or routing

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

#### Scenario: Change artifacts live in openspec/changes/

- **WHEN** a non-trivial change is planned or implemented
- **THEN** all artifacts (proposal, design, tasks, delta specs) MUST reside
  under `openspec/changes/<name>/` and MUST NOT be placed in `plans/` or
  `memory-bank/`

### Requirement: Repo-global transient context stores MUST NOT be used

The directories `memory-bank/` and `plans/` MUST NOT be created, maintained, or
referenced as sources of truth. Durable context belongs in the canonical
documentation files. Per-change transient context belongs in OpenSpec change
artifacts.

#### Scenario: memory-bank directory must not exist

- **WHEN** an agent or contributor is working in the repository
- **THEN** the agent MUST NOT create a `memory-bank/` directory, write files
  into `memory-bank/`, or read from `memory-bank/` as a source of truth

#### Scenario: plans directory must not exist

- **WHEN** an agent or contributor is planning work
- **THEN** the agent MUST NOT create a `plans/` directory, write plan files
  into `plans/`, or read from `plans/` as a source of truth; plans MUST be
  authored as OpenSpec change artifacts under `openspec/changes/`

#### Scenario: Removal of legacy transient stores

- **WHEN** `memory-bank/` or `plans/` directories exist from prior workflows
- **THEN** they MUST be removed as part of adopting this governance policy, and
  any durable content they contain MUST be migrated into the appropriate
  canonical file or OpenSpec change artifact

### Requirement: OpenSpec changes have archive and completion expectations

When an OpenSpec change is complete, durable outcomes MUST be synced back into
the canonical documentation files before the change is archived. Delta specs
MUST be synced into main specs at `openspec/specs/<capability>/spec.md`.

#### Scenario: Durable outcomes sync to canonical docs before archive

- **WHEN** all tasks in an OpenSpec change are marked complete
- **THEN** any durable architectural, structural, or routing decisions made
  during the change MUST be synced into the appropriate canonical file
  (`architecture.md`, `codemap.md`, or `AGENTS.md`) before the change is
  archived

#### Scenario: Delta specs sync to main specs

- **WHEN** an OpenSpec change contains delta specs under
  `openspec/changes/<name>/specs/`
- **THEN** the deltas MUST be synced into the corresponding main specs at
  `openspec/specs/<capability>/spec.md` before or during archive

#### Scenario: Archive moves the complete change

- **WHEN** an OpenSpec change is archived
- **THEN** the entire change directory MUST be moved to
  `openspec/changes/archive/YYYY-MM-DD-<name>/`, and no artifacts or
  un-synced durable outcomes MAY be left behind
