# CLAUDE.md

This file is Claude Code's entry point. It defers to `AGENTS.md` so the
project speaks one source of truth across coding agents (opencode, Claude
Code, etc.). Project conventions, layer rules, and non-negotiables live
in `AGENTS.md` and the per-layer `AGENTS.md` files; this file only wires
them in.

## Global persona

@~/.config/opencode/AGENTS.md

## Project rules (root)

@AGENTS.md

## Architecture reference

@architecture.md

## Active session context

@memory-bank/active-context.md
@memory-bank/conventions.md
@memory-bank/progress.md

---

## Per-layer rules

Claude Code auto-loads `CLAUDE.md` files from any subdirectory you read
or edit in. The nested `CLAUDE.md` files in this repo each import their
sibling `AGENTS.md`, so every layer's specialized rules apply when you
work inside that layer. The current set:

- `app/src/main/java/com/opus/readerparser/ui/CLAUDE.md`
- `app/src/main/java/com/opus/readerparser/sources/CLAUDE.md`
- `app/src/main/java/com/opus/readerparser/data/source/CLAUDE.md`
- `app/src/main/java/com/opus/readerparser/data/local/database/CLAUDE.md`

If a new `AGENTS.md` is added under any package, add a sibling
`CLAUDE.md` next to it that imports it via `@AGENTS.md` so Claude Code
picks it up automatically.

---

## Tooling notes for Claude Code specifically

- The opencode-side orchestrator pipeline (planning → specialist
  dispatch → reviewer+runner verification) is mirrored in
  `.claude/agents/` and `.claude/commands/`. Use `/start <task>` for the
  orchestrator entry point and `/verify` for the build/lint/test gate.
- Skills are shared with the opencode setup via `.claude/skills/` →
  `.opencode/skills/` symlinks. Invoke `skill("trace-matrix")`,
  `skill("android-cli")`, `skill("jsoup-parsing")` exactly as the
  opencode agents do.
- The Claude `Agent` tool is the equivalent of opencode's `task` tool.
  Where opencode docs say "dispatch via `task`", read it as "dispatch
  via the `Agent` tool with `subagent_type` set to the specialist's
  filename stem".
