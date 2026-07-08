---
description: Create a pull request, then babysit it.
---

Use the `integrator` agent to create a PR for `$ARGUMENTS` using the repo's commit conventions and approved branch.

Return the PR URL or number, then load `babysit-pr` for the watch / fix / merge loop.
Do not merge unless explicitly approved.
