# Tech context

## Stack

- Kotlin / JVM 17
- Jetpack Compose + Material 3
- Kotlin coroutines + Flow
- Ktor Client (OkHttp engine) + Jsoup
- Room
- DataStore Preferences
- Coil 3
- WorkManager
- Hilt

## Test shape

- JVM tests in `app/src/test/kotlin/` cover domain models, repositories,
  ViewModels, and source parsing.
- Instrumented tests in `app/src/androidTest/java/` cover Compose `*Content`,
  Room migration and DAO behavior, and workers.
- Journey XML under `journeys/` supports emulator-driven flow checks.
- Hand-rolled fakes are preferred for interfaces we control; `MockEngine` is
  required for network tests.

## Useful test utilities

- `app/src/test/kotlin/com/opus/readerparser/testutil/MainDispatcherRule.kt`
- `app/src/test/kotlin/com/opus/readerparser/testutil/KtorMockHelpers.kt`
- `app/src/test/kotlin/com/opus/readerparser/testutil/TestFixtures.kt`
- `app/src/androidTest/java/com/opus/readerparser/testutil/FakeCoilRule.kt`

## Verification commands

- `./gradlew :app:assembleDebug`
- `./gradlew :app:lintDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:detekt`
- `./gradlew :app:ktlintCheck`

## Tooling conventions

- Use `git` for local history and staging.
- Use `gh` for GitHub-side operations.
- Load `android-cli` only for emulator, device, APK, or journey work.
- See `memory-bank/commit-conventions.md` for commit prefixes.
