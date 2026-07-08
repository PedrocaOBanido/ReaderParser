---
description: Babysit a PR through CI, fixes, review, push, and optional merge
---

Use the `babysit-pr` skill.

Accept a PR number, PR URL, or current branch. If omitted, infer the PR only when it is unambiguous; otherwise ask a focused question before proceeding.

Follow the skill's workflow to watch CI, route failures through `runner`, `build`, `reviewer`, and `integrator`, and only merge when the user explicitly requested merge-on-green or automerge.
