---
name: openspec-apply-change
description: Implement tasks from an OpenSpec change. Use when the user wants to start implementing, continue implementation, or work through tasks.
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.3.1"
---

Implement tasks from an OpenSpec change.

**Input**: Optionally specify a change name. If omitted, check if it can be inferred from conversation context. If vague or ambiguous you MUST prompt for available changes.

**Steps**

1. **Select the change**

   If a name is provided, use it. Otherwise:
   - Infer from conversation context if the user mentioned a change
   - Auto-select if only one active change exists
   - If ambiguous, run `openspec list --json` to get available changes and use the **AskUserQuestion tool** to let the user select

   Always announce: "Using change: <name>" and how to override (e.g., `/opsx-apply <other>`).

2. **Check status to understand the schema**
   ```bash
   openspec status --change "<name>" --json
   ```
   Parse the JSON to understand:
   - `schemaName`: The workflow being used (e.g., "spec-driven")
   - Which artifact contains the tasks (typically "tasks" for spec-driven, check status for others)

3. **Get apply instructions**

   ```bash
   openspec instructions apply --change "<name>" --json
   ```

   This returns:
   - `contextFiles`: artifact ID -> array of concrete file paths (varies by schema - could be proposal/specs/design/tasks or spec/tests/implementation/docs)
   - Progress (total, complete, remaining)
   - Task list with status
   - Dynamic instruction based on current state

   **Handle states:**
   - If `state: "blocked"` (missing artifacts): show the missing artifacts and ask the user how to proceed
   - If `state: "all_done"`: congratulate, suggest archive
   - Otherwise: proceed to implementation

4. **Read context files**

   Read every file path listed under `contextFiles` from the apply instructions output.
   The files depend on the schema being used:
   - **spec-driven**: proposal, specs, design, tasks
   - Other schemas: follow the contextFiles from CLI output

5. **Show current progress**

   Display:
   - Schema being used
   - Progress: "N/M tasks complete"
   - Remaining tasks overview
   - Dynamic instruction from CLI

6. **Implement tasks (loop until done or blocked)**

   For each pending task:
   - Show which task is being worked on
   - Make the code changes required
   - Keep changes minimal and focused
   - Mark task complete in the tasks file: `- [ ]` → `- [x]`
   - Continue to next task

   **Pause if:**
   - Task is unclear → ask for clarification
   - Implementation reveals a design issue → suggest updating artifacts
   - Error or blocker encountered → report and wait for guidance
   - User interrupts

7. **Post-implementation verification: dispatch runner**

   After all tasks are checked off, dispatch the `runner` agent with the
   verification suite: `assembleDebug` + `lintDebug` + `testDebugUnitTest`.

   - If any task fails, report failures and pause for fix before
     continuing. Do not proceed to reviewer until runner reports green.
   - Track the number of correction iterations. Maximum: 3.

8. **Post-implementation review: dispatch reviewer**

   After runner reports green, dispatch the `reviewer` agent with the
   current diff.

   - If BLOCKER or SHOULD-FIX findings exist, report them and pause for
     fix before continuing.
   - If a fix is needed, re-run the correction cycle (runner → reviewer).
   - Track the number of correction iterations. Maximum: 3.

9. **Completion or correction loop**

   - If both runner and reviewer pass: declare
     **"Implementation Complete — Ready to archive"** and suggest
     `/opsx-archive`.
   - If the correction cycle exceeds 3 iterations: pause and show the
     remaining issues to the user for guidance. Do not declare success.

**Output During Implementation**

```
## Implementing: <change-name> (schema: <schema-name>)

Working on task 3/7: <task description>
[...implementation happening...]
✓ Task complete

Working on task 4/7: <task description>
[...implementation happening...]
✓ Task complete
```

**Output On Completion**

```
## Implementation Complete — Ready to archive

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete ✓
**Verification:** Runner green ✓
**Review:** Reviewer ready ✓

### Completed This Session
- [x] Task 1
- [x] Task 2
...

All tasks complete. Runner green. Reviewer ready.
Ready to archive this change.
```

**Output On Pause (Correction Cycle Exceeded)**

```
## Implementation Paused — Correction cycle limit reached

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 7/7 tasks complete
**Correction iterations:** 3/3

### Remaining Issues
<description of unresolved runner failures or reviewer findings>

**Options:**
1. Fix the remaining issues manually
2. Investigate further
3. Proceed with known issues (not recommended)

What would you like to do?
```

**Output On Pause (Issue Encountered)**

```
## Implementation Paused

**Change:** <change-name>
**Schema:** <schema-name>
**Progress:** 4/7 tasks complete

### Issue Encountered
<description of the issue>

**Options:**
1. <option 1>
2. <option 2>
3. Other approach

What would you like to do?
```

**Guardrails**
- Keep going through tasks until done or blocked
- Always read context files before starting (from the apply instructions output)
- If task is ambiguous, pause and ask before implementing
- If implementation reveals issues, pause and suggest artifact updates
- Keep code changes minimal and scoped to each task
- Update task checkbox immediately after completing each task
- Pause on errors, blockers, or unclear requirements - don't guess
- Use contextFiles from CLI output, don't assume specific file names
- After all tasks complete, always run runner → reviewer correction cycle
- Maximum 3 correction iterations before pausing for user guidance
- Never declare success while runner or reviewer report failures/blockers

**Fluid Workflow Integration**

This skill supports the "actions on a change" model:

- **Can be invoked anytime**: Before all artifacts are done (if tasks exist), after partial implementation, interleaved with other actions
- **Allows artifact updates**: If implementation reveals design issues, suggest updating artifacts - not phase-locked, work fluidly
