---
name: babysit-pr
description: Watch an existing PR, route CI failures through fix/review/integrate loops, and optionally merge on green when explicitly requested.
license: MIT
---

# Purpose

Watch an existing pull request through CI, keep the loop moving, and only merge when the user explicitly asked for merge-on-green or automerge. Use this after PR creation; `integrator` opens the PR first.

# Inputs

- PR number, PR URL, or current branch.
- Optional merge request: explicit merge-on-green/automerge approval from the user.
- Not for creating the initial PR.

# Routing

- `runner`: run and interpret verification.
- `reviewer`: review diffs and proposed fixes.
- `build`: implement bounded fixes.
- `integrator`: create the PR, push changes, and hand off the watch loop here.

# Workflow

1. Resolve the PR from the input.
2. Confirm the PR already exists; if not, hand off to `integrator`.
3. Inspect current checks and identify the first actionable failure.
4. Route the failure to the right lane.
5. Re-run checks after each fix.
6. Stop when checks are green, or when the failure becomes ambiguous or high-risk.

# Failure triage

- Prefer the smallest actionable failure.
- If checks are red because of code, route to `build`.
- If a fix needs confirmation or is risky, route to `reviewer` before changing code.
- If the failure is unclear, contradictory, or outside the repo, stop and ask.

# Fix loop

- Cap fix attempts at 3 per failure cluster.
- Each loop must follow: inspect failed checks -> diagnose -> build fixes -> runner verifies -> reviewer reviews -> integrator pushes -> watch again.
- Do not expand scope while a bounded fix is still available.

# Merge policy

- Never merge unless the user explicitly requested merge-on-green or automerge.
- Prefer a merge commit when merging.
- Never force-push.
- If green but merge was not explicitly requested, report success and ask what to do next.

# Guardrails

- Stop on ambiguous, flaky, or high-risk failures instead of guessing.
- Do not use hidden retries to exceed the 3-loop cap.
- Do not poll from the orchestrator; `integrator` owns the watch loop.
- Keep changes bounded to the failure at hand.

# Output format

- Current PR state.
- Failed checks and likely cause.
- Lane routed to next.
- Any merge decision or blocker.
- Whether the PR is green, fixed, or needs user input.
