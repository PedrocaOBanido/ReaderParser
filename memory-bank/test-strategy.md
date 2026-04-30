# Test strategy

## Phased test plan

| Phase | What's tested | Scope | Tools |
|---|---|---|---|
| 0 | Test infrastructure (itself) | JVM | Gradle compile check |
| 1 | Domain models + Source contract | JVM (`src/test/`) | JUnit, Truth, `runTest` |
| 2 | First source plugin | JVM | `MockEngine`, HTML fixtures, Truth |
| 3 | Repositories | JVM | Fake `Source`, in-memory Room DAO |
| 3 | Room migrations | `androidTest/` | `MigrationTestHelper` |
| 4 | ViewModels | JVM | Fake repositories, Turbine, `MainDispatcherRule` |
| 5 | Compose `*Content` composables | `androidTest/` | `createComposeRule()`, Compose testing APIs |
| 6 | WorkManager workers | `androidTest/` | `WorkManagerTestInitHelper` |
| 7 | Edge cases (downloads, hashing, prefs) | JVM | Standard JUnit/Truth |

## Available test utilities

Located in `app/src/test/java/com/opus/novelparser/testutil/`:

| Utility | Purpose |
|---|---|
| `MainDispatcherRule` | JUnit `TestRule` — replaces `Dispatchers.Main` with `UnconfinedTestDispatcher` for ViewModel/coroutine tests |
| `KtorMockHelpers.mockHttpClient(block)` | Creates `HttpClient` backed by `MockEngine`. Each request forwarded to the handler block. |
| `KtorMockHelpers.readFixture(path)` | Reads a classpath resource (e.g. `"fixtures/examplemanhwa/popular.html"`) as `String` |
| `KtorMockHelpers.respondHtml(html, status)` | Convenience responder returning HTML `MockResponse` with correct headers |

## Fake / mock policy

- **Interfaces we control** (`Source`, `SeriesRepository`, `ChapterRepository`): hand-rolled fakes in `app/src/test/java/.../fakes/`. No Mockito, no MockK.
- **Third-party types we can't replace** (Room DAO internals, WorkManager internals): test via real in-memory instances (Room) or `*TestInitHelper` (WorkManager). No mocking.
- **Network**: `MockEngine` from `ktor-client-mock` always. Never real HTTP calls in tests.

## Key testing conventions

- **No `runBlocking`** in test code. Use `runTest { }` from `kotlinx-coroutines-test`.
- **ViewModel tests**: apply `MainDispatcherRule`, use `Turbine` to assert `StateFlow` emissions and `Channel` effects.
- **Source tests**: load HTML from fixtures via `readFixture()`, pass through `mockHttpClient` with content, assert parsed domain models.
- **Repository tests**: `FakeSource` returns canned data; in-memory Room DAO for persistence. No DI container needed in JVM tests.
- **Content composable tests**: render stateless `*Content(state, onAction)` with fake state; verify UI elements, not ViewModel wiring.
- **Migration tests**: use `MigrationTestHelper` from `room-testing`. Create DB at version N, insert known data, migrate to N+1, verify.

## What tests are NOT written (by design)

- No integration tests covering full screen → repository → source → network. Covered by the layered test pyramid (unit + UI).
- No end-to-end tests. Personal-use app; manual smoke testing is sufficient.
- No performance benchmarks unless a specific bottleneck is identified.
