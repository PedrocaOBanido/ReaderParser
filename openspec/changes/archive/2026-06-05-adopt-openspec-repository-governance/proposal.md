## Why

The repo currently has two competing workflows for managing changes: a custom `/start` + `plans/` + `memory-bank/` system and the OpenSpec change workflow. This creates confusion about where durable truth lives, where transient context belongs, and which path agents should follow. OpenSpec already provides structured change artifacts (proposal → design → specs → tasks); the custom system duplicates parts of this without the spec or traceability benefits. Consolidating on OpenSpec as the single required path for non-trivial work eliminates the split, removes scattered transient context files, and gives every change a clear artifact trail.

## What Changes

- **Remove `plans/` directory** — all historical plan files are archived or deleted; OpenSpec changes under `openspec/changes/` become the sole plan store.
- **Remove `memory-bank/` directory** — active context, progress, and convention files are retired; durable truth stays in `README.md`, `architecture.md`, `codemap.md`, and `AGENTS.md`, while per-change context lives in OpenSpec artifacts.
- **Remove `.opencode/command/start.md` and `.opencode/command/light-start.md`** — the custom plan-first kickoff commands are replaced by the OpenSpec propose/apply workflow.
- **Add `repository-governance` capability** — a single internal OpenSpec spec that codifies when OpenSpec is required, what the durable repo truth files are, and the rules for trivial/read-only exceptions.
- **Retain trivial/read-only exemption** — directly answering questions, read-only exploration, and single-file cosmetic fixes can proceed without an OpenSpec change.

## Capabilities

### New Capabilities

- `repository-governance`: Defines the repo's change-management policy — which workflow applies to which class of work, what the authoritative durable files are, and what constitutes a trivial exception exempt from OpenSpec.

### Modified Capabilities

_No existing specs in `openspec/specs/`._

## Impact

- **Affected directories**: `plans/` (removed), `memory-bank/` (removed).
- **Affected commands**: `.opencode/command/start.md` (removed), `.opencode/command/light-start.md` (removed).
- **Affected agent routing**: `AGENTS.md` must be updated to remove references to `memory-bank/`, `plans/`, `/start`, and `/light-start`, and to point at the OpenSpec workflow and `repository-governance` spec instead.
- **No code changes**: no Kotlin, Gradle, or build-system files are affected.
- **No dependency changes**: no new libraries or build plugins.
- **Breaking**: agents relying on `/start` or `memory-bank/activeContext.md` will break until `AGENTS.md` routing is updated.
