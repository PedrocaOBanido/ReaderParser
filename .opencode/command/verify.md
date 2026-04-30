---
description: Run the full verification suite before declaring a task done.
---

Run, in order, and report each result:

1. `./gradlew :app:assembleDebug`
2. `./gradlew :app:lintDebug`
3. `./gradlew :app:testDebugUnitTest`
4. `./gradlew :app:ktlintCheck` (skip if not configured)
5. `./gradlew :app:detekt` (skip if not configured)

If any step fails, stop and report the failure with file/line context. Do
not attempt to fix without explicit instruction. Do not suppress warnings,
add `@Suppress`, or disable lint rules to make a step pass.
