# Commit conventions

Every commit uses one of the prefixes below. A commit never mixes prefixes.

| Prefix | When to use |
|---|---|
| `feat:` | New file, new screen, new source plugin, new capability |
| `fix:` | Bug fix to previously committed code (compile errors, crashes, wrong behavior) |
| `refactor:` | Restructuring committed code without changing behavior |
| `ci:` | CI pipeline, pre-push hooks, test scripts, Gradle verification tasks |
| `cd:` | Release pipeline, signing config, deployment scripts, environment bootstrap |
| `docs:` | `AGENTS.md`, `architecture.md`, `memory-bank/*`, README, KDoc |

## Rules

1. **A fix only exists relative to a prior commit.** If a `feat:` commit introduces code that doesn't compile, the compilation fix is part of that same `feat:` commit — it was never committed broken.

2. **Refactors don't change behavior.** If you improve code structure and add a feature in the same commit, it's a `feat:`. If you fix a bug while restructuring, it's a `fix:`.

3. **CI vs CD.** CI covers quality gates (test, lint, assemble). CD covers delivery (signing, release, artifact upload). When in doubt, prefer `ci:` for automation that runs on every push and `cd:` for automation that publishes.

4. **One commit = one verb.** If the diff adds a new screen *and* its tests, that's one `feat:` commit. If the diff adds a screen *and* fixes a database migration bug, that's two commits: `feat:` then `fix:`.

5. **Subject line:** imperative mood, present tense, ≤ 72 characters. Body explains *why*, not *what*.

6. **Pre-push hook validates unit tests** before every push. Run `scripts/ci-check` before opening a PR for the full suite.
