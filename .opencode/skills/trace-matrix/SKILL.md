---
name: trace-matrix
description: Enforces bidirectional traceability between user intent and code changes. Before writing: maps requirements to planned changes. After changes: maps every diff to an acceptance criterion with verification result. For code writing, refactoring, and code review sessions.
---

# Trace Matrix Protocol

This skill provides instructions for maintaining bidirectional traceability
throughout a coding task. Load it with `skill("trace-matrix")` when the task
involves writing, modifying, or analyzing code.

## When to load

| Situation | Load |
|---|---|
| Writing new code (source-author, screen-author, room-migration) | Before dispatch |
| Refactoring existing code | Before first edit |
| Code review (reviewer) | Before starting the review |
| Debugging or investigating code behavior | Before analysis |

## Core principle

Every code change must trace back to a user requirement, and every user
requirement must trace forward to a verification. No change without a
reason. No requirement without a check.

## Before action: establish the forward trace

Before writing or modifying any file, fill in the **Trace Matrix Worksheet**
below. This is the forward trace: user intent → planned changes → acceptance
criteria.

### Trace Matrix Worksheet

```
## REQUIREMENT
  <one-sentence restatement of the user's request>

## PLANNED CHANGES
  | # | File | Purpose (links to requirement) | Expected lines |
  |---|------|-------------------------------|----------------|
  | 1 | path/to/file.kt | <why this file is needed> | <~N> |
  | 2 | ...              |                               |      |

## ACCEPTANCE CRITERIA
  | # | Criterion (observable) | Maps to change # |
  |---|------------------------|------------------|
  | 1 | <testable outcome>     | 1, 3             |
  | 2 | <testable outcome>     | 2                |

## SCOPE BOUNDARY
  - Files explicitly NOT in scope:
  - Rules from AGENTS.md that constrain this task (cite section):
```

### Worksheet rules

- Every acceptance criterion must map to at least one planned change.
- Every planned change must map to at least one acceptance criterion.
- Zero-mapping rows in either direction signal either scope creep (a change
  with no criterion) or untestable requirements (a criterion with no code).
  Fix the plan before proceeding.
- The "Expected lines" column prevents scope drift. If a 20-line edit
  becomes 200 lines, stop and replan — something expanded.

## After action: close the backward trace

After all changes are made but before declaring done, complete the back-trace:

```
## BACK-TRACE
  | Change # | File (actual) | Diff size (lines) | Satisfies criterion # | Verified? |
  |----------|---------------|-------------------|----------------------|-----------|
  | 1        | path/file.kt  | +15, -3           | 1                    | YES (test passed) |
  | 2        | path/other.kt | +42               | 2                    | YES (lint green) |

## UNDECLARED CHANGES
  (Files in the diff that were not in the planned changes. List each with
  justification, or flag for revert.)
  - <none, or: path/surprise.kt — justified by...>

## UNVERIFIED CRITERIA
  (Acceptance criteria from the forward trace that could not be confirmed.
  State why.)
  - <none, or: criterion #3 — requires instrumented test, deferred>
```

### Back-trace rules

- **Undisciplined change protocol:** if the diff contains a file not in the
  planned changes, either justify it in UNDECLARED CHANGES or revert it.
  "I noticed it needed fixing" is not a justification — the user must
  approve scope expansion.
- Every criterion must have a verification observation (test pass, lint
  clean, manual visual check, etc.). "It should work" is not verification.
- If a criterion cannot be verified with available tools, record it under
  UNVERIFIED CRITERIA with the reason — do not silently drop it.

## For code review (R-class use)

When reviewing a diff, invert the protocol. The author produced a forward
trace; you audit the backward trace:

1. **Trace completeness:** does every changed file map to a stated
   requirement in the PR/commit description? Flag unmapped files.
2. **Verification quality:** for each acceptance criterion claimed as met,
   is the verification evidence credible? Flag hand-wavy claims.
3. **Scope creep:** are there changes that serve no stated requirement?
   Flag them.
4. **Missing coverage:** are there stated requirements with no changed
   file mapping to them? Flag as incomplete implementation.

## Integration with the orchestrator

When the orchestrator loads this skill for a dispatch:

- The **forward trace worksheet** aligns with the existing plan section
  (files in scope + acceptance criteria already map to this pattern).
- The **back-trace worksheet** replaces informal "what I did" summaries
  in the RETURN FORMAT — agents return a completed back-trace instead.
- The orchestrator's existing pre-dispatch checklist (item 4: "Acceptance
  is observable") and verification loop (item 2: "Sanity-check against
  acceptance") already align with this protocol. This skill makes the
  mapping explicit rather than implicit.

## Non-negotiables

- Never claim a change maps to a criterion without confirming the
  criterion is met. The back-trace "Verified?" column must be YES only
  after running the test, lint, build, or visual check.
- Never drop a criterion because verification is inconvenient. Move it
  to UNVERIFIED and explain.
- Never add unplanned changes silently. Every file in the diff that was
  not in the forward trace appears in UNDECLARED CHANGES or is reverted.
- The worksheet is not optional for tasks beyond ~20 LOC. For one-line
  fixes, a one-sentence trace ("Changed X to fix Y, verified by running Z")
  is sufficient.
