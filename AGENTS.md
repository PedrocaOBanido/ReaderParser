# AGENTS.md

Instructions for AI coding agents working on this repository.

This is a personal-use Android app: a unified reader for webnovel and manhwa
sites. Each site is a **source plugin** behind a common `Source` interface;
the rest of the app is site-agnostic. UI is Jetpack Compose; HTTP is Ktor.

Read `architecture.md` first if you have not already. It is the source of
truth for layering, models, and module structure.

---

## 1. Non-negotiables

These rules are absolute. Do not violate them. If a user request would require
violating one, push back and propose an alternative.

1. **Domain layer has zero Android dependencies.** Code under `domain/` must
   compile against plain JVM. No imports from `androidx.*`, `android.*`,
   `io.ktor.*`, `androidx.room.*`, or `androidx.compose.*`.
2. **Ktor calls live only inside `Source` implementations or repositories.**
   Never call `HttpClient` from a ViewModel, use case, or composable.
3. **ViewModels never reference `SourceRegistry` or any concrete `Source`.**
   They go through repositories.
4. **The novel reader and manhwa reader are separate screens.** Do not unify
   them. They share navigation actions and the `ChapterContent` sealed type;
   nothing else.
5. **`ChapterContent` stays a sealed interface with exactly two variants:**
   `Text(html)` and `Pages(imageUrls)`. Adding a third variant is an
   architectural decision that requires explicit human sign-off.
6. **Domain models are immutable `data class`es.** No `var`, no mutable
   collections. Update via `.copy(...)`.
7. **No `runBlocking` in production code.** Tests only.
8. **Compose previews target stateless `*Content` composables, not `*Screen`.**
   `*Screen` wires the ViewModel; `*Content` takes `(state, onAction)`.
9. **Series and chapter identity is `(sourceId, url)`.** Never use
   auto-incrementing IDs as foreign keys. URLs are stable across sessions;
   row IDs are not.
10. **Downloads go to app-private storage** (`context.filesDir`). Never request
    `MANAGE_EXTERNAL_STORAGE` or write to shared storage without an explicit
    user-initiated export action.

---

## 2. Where things go

When adding code, place it according to the table below. If you are not sure,
ask before guessing.

| Adding…                              | Goes in                                     |
| ------------------------------------ | ------------------------------------------- |
| New site plugin                      | `sources/<sitename>/<SiteName>.kt`          |
| Domain model                         | `domain/model/`                             |
| Cross-repo operation                 | `domain/usecase/` (only if non-trivial)     |
| Repository contract                  | `domain/` (interface) + `data/repository/`  |
| Room entity, DAO, migration          | `data/local/database/`                      |
| File-system helper                   | `data/local/filesystem/`                    |
| DataStore prefs                      | `data/local/prefs/`                         |
| Ktor / JSON / cookie config          | `data/network/`                             |
| `Source` interface, `HtmlSource`     | `data/source/`                              |
| Hilt module                          | `core/di/`                                  |
| Generic util / extension             | `core/util/`                                |
| New screen                           | `ui/<screen>/<Screen>Screen.kt` + ViewModel |
| Reusable composable                  | `ui/components/`                            |
| Theme tokens                         | `ui/theme/`                                 |
| Background worker                    | `workers/`                                  |

A new screen is **never** a single file. It is at minimum:
`<Screen>Screen.kt`, `<Screen>Content.kt`, `<Screen>ViewModel.kt`,
`<Screen>UiState.kt` (containing `UiState`, `Action`, `Effect`).

---

## 3. The `Source` contract

Every site plugin implements `Source` (see `architecture.md` §3.3). When
adding a source:

- Extend `HtmlSource`, not `Source` directly, unless the site is JSON-based.
- Compute `id` via `computeSourceId(name, lang, type)`. Never hand-pick a
  numeric ID.
- Set `type` correctly — `NOVEL` or `MANHWA`. The reader screen branches on
  this; getting it wrong routes users to the wrong reader.
- Override `chapterTextParse` for novels **or** `chapterPagesParse` for
  manhwa, never both. Leave the other as `error("...")`.
- Register the new source in the `SourceModule` provider in `core/di/`.
- Sources **throw** on error; they do not log, do not catch broadly, and do
  not return null sentinels for missing data. Use nullable fields on the
  domain model when data is genuinely optional from the site.

A source is "done" when it can: list popular, list latest, search, fetch
series details, fetch the chapter list, and fetch one chapter's content.
Test each of these with a real fixture (saved HTML in `src/test/resources/`)
plus `MockEngine`.

---

## 4. State management

Each screen's ViewModel exposes:

```kotlin
val state: StateFlow<XUiState>          // single source of truth for UI
val effects: Flow<XEffect>              // one-shot side effects
fun onAction(action: XAction)           // single entry point for UI events
```

Rules:

- `UiState` is a single `data class` containing **everything** the screen
  renders, including `isLoading: Boolean` and `error: String?`. No multiple
  parallel `StateFlow`s for different parts of the screen.
- `Action` is a sealed interface. Every UI event produces exactly one action.
  No anonymous lambdas in the ViewModel surface.
- `Effect` is a sealed interface, sent through a `Channel<Effect>(BUFFERED)`,
  exposed as `receiveAsFlow()`. Use only for things that should happen
  exactly once: navigation, snackbars, toasts. Never put navigation in
  `UiState`.
- Compose collects via `collectAsStateWithLifecycle()`. Never `collectAsState`.
- Effects are collected in `LaunchedEffect(Unit) { vm.effects.collect { … } }`
  inside `*Screen`, never inside `*Content`.

---

## 5. Errors

- Repositories let exceptions propagate. They do not catch and return
  `Result<T>` unless there is a specific recovery path the caller needs.
- ViewModels catch at the boundary, map to a stable error message in
  `UiState.error`, and emit a `ShowError` effect if the user needs to be
  notified actively (e.g., a failed action). Background refresh failures
  go to `UiState` only.
- Never swallow exceptions silently. If you catch `Exception`, you log it
  and surface it to the user one way or the other.
- Network errors, parse errors, and storage errors should be distinguishable
  in logs. Don't `catch (e: Exception)` at the top of a source method —
  let Ktor's typed exceptions surface.

---

## 6. Database changes

Adding or changing a Room entity:

1. Bump `AppDatabase.version`.
2. Write an explicit `Migration` from the previous version. **No
   `fallbackToDestructiveMigration` in any release build configuration.**
3. Update the corresponding mapper between entity and domain model.
4. Update or add tests in the database test source set.
5. If the schema change affects identity (primary keys, FKs), audit every
   query that joins on the old keys.

`exportSchema = true` on `@Database`. Schemas are checked into
`app/schemas/`. Do not delete or hand-edit those files.

---

## 7. Networking

- One shared `HttpClient` provided by `NetworkModule`. Per-source customization
  goes inside the source class — wrap the shared client or add request
  interceptors there, do not create a second top-level client.
- Always set a timeout. Default request timeout is 30s; override per-call
  with `HttpRequestBuilder.timeout { ... }` if a site needs longer.
- Cookies go through the persistent cookie jar wired into the OkHttp engine.
  Do not parse `Set-Cookie` manually.
- HTML parsing is Jsoup. JSON is `kotlinx.serialization`. No Gson, no Moshi,
  no Jackson — pick one stack and stay.
- Sources should be resilient to: missing optional elements (use `selectFirst`
  + null-safety, not `select(...).first()`), whitespace in text nodes (always
  `.trim()`), relative URLs (always `absUrl(...)`).

---

## 8. Compose conventions

- Material 3. No Material 2 imports.
- Theme tokens via `MaterialTheme.colorScheme` / `.typography`. No hardcoded
  colors or font sizes outside `ui/theme/`.
- `*Screen` composables take `viewModel: XViewModel = hiltViewModel()` and
  immediately delegate to `*Content(state, onAction)`. `*Screen` is never
  previewed; `*Content` always is.
- Hoist anything used in more than one screen into `ui/components/`.
- Lists: `LazyColumn` / `LazyVerticalGrid`. Always provide stable `key`s.
- Images: Coil 3 via `AsyncImage`. Provide a `placeholder` and `error` for
  every cover and page.
- No business logic in composables. If you find yourself writing an `if` on
  a domain enum inside a composable to decide what to fetch, that decision
  belongs in the ViewModel.

---

## 9. Coroutines

- ViewModel work runs on `viewModelScope`.
- DB work uses Room's suspend DAOs (already on `Dispatchers.IO` internally).
  Don't wrap DAO calls in `withContext(Dispatchers.IO)`.
- File I/O in `DownloadStore` runs on `Dispatchers.IO`.
- Network is on `Dispatchers.IO` via Ktor's OkHttp engine.
- `Flow`s exposed as `StateFlow` are collected on the main thread; transform
  upstream (`map`, `combine`) before `.stateIn(...)`.
- Use `SharingStarted.WhileSubscribed(5_000)` for `stateIn` in ViewModels —
  not `Eagerly`, not `Lazily`.

---

## 10. Testing expectations

When you add code, you add tests. Tests live in `src/test/` (JVM) or
`src/androidTest/` (instrumented).

| Adding…              | Test type                                                |
| -------------------- | -------------------------------------------------------- |
| New `Source`         | JVM tests with HTML fixtures + Ktor `MockEngine`         |
| New repository       | JVM tests with fake `Source` + in-memory Room            |
| New ViewModel        | JVM tests with fake repository, using `Turbine` for flow |
| New `*Content`       | Compose UI test (`createComposeRule()`)                  |
| Room migration       | Migration test using `MigrationTestHelper`               |
| Background worker    | `WorkManagerTestInitHelper` test                         |

Fakes go in `src/test/kotlin/.../fakes/`. Do not use Mockito or MockK for
interfaces you control; write a hand-rolled fake. Mock only third-party
types you cannot replace, and prefer a wrapper interface even then.

A PR that adds a feature without tests is incomplete. Mention this in the
summary if you can't add a test for some reason; do not silently skip.

---

## 11. Build & verify

Before declaring a task done, the agent must:

```bash
./gradlew :app:assembleDebug         # compiles
./gradlew :app:lintDebug             # Android lint
./gradlew :app:testDebugUnitTest     # JVM tests
./gradlew :app:detekt                # if Detekt is configured
./gradlew :app:ktlintCheck           # if ktlint is configured
```

If any of these fail, fix them before handing off. Do not disable lint rules
or suppress warnings to make them pass — fix the underlying issue or ask.

When tests are flaky, fix the test. Do not add `@Ignore`.

---

## 12. Style

- `kotlin.code.style=official`. ktlint is the formatter.
- 4-space indent. 120-column soft limit, 140 hard.
- Imports: no wildcards, no unused imports.
- Public APIs require KDoc on the type and on any non-obvious member. Private
  members: doc only when intent isn't obvious from the name.
- Function names: `camelCase`. Composables: `PascalCase`. Constants: `UPPER_SNAKE`.
- Prefer expression bodies and trailing-lambda syntax where it reads better.
- No abbreviations except established ones (`url`, `id`, `db`, `vm`).

---

## 12b. Git & GitHub

- **Local operations** (`add`, `commit`, `branch`, `merge`, `restore`, `rebase`): use `git`
- **GitHub operations** (`push`, `pull`, `pr`, `release`, `issue`, `workflow`): use `gh` CLI
- Never use raw `git push` — use `gh` to interact with the remote
- Create PRs with `gh pr create`, merge with `gh pr merge`
- Create releases with `gh release create`
- See `memory-bank/commit-conventions.md` for commit prefix rules

---

## 13. What to ask before doing

Some changes are large enough that the agent should propose and wait for
confirmation rather than execute:

- Adding a new top-level layer or module.
- Changing the `Source` interface.
- Changing identity (primary keys, FKs) on existing entities.
- Adding a new permission to the manifest.
- Adding a new third-party library.
- Loading sources dynamically from APKs/DEX (Tachiyomi-style extensions).
- Replacing Hilt with another DI framework, or vice versa.
- Replacing Ktor's engine.

Routine work — adding a source, a screen, a use case, a repository method,
a migration — does not need pre-approval; just do it and follow the rules.

---

## 14. What this app is not

To save round trips on out-of-scope requests:

- **Not a sync service.** No accounts, no cloud, no cross-device library
  sync. Local-only.
- **Not a content host.** It reads from sites; it does not store or
  redistribute content beyond the user's own downloads.
- **Not multi-user.** One user, one device, one library.
- **Not a browser.** No general-purpose WebView shell. The novel reader may
  use a WebView internally for rendering; that's an implementation detail.
- **Not iOS / desktop / web.** Android only. Don't suggest KMP unless asked.

If a request implies any of the above, surface the conflict before writing
code.

---

## 15. Android CLI integration

The `android-cli` skill is available to all agents. Load it via
`skill("android-cli")` when:

| Situation | Use this |
|---|---|
| Writing Compose UI, unsure about API behavior | `android docs search <query>` |
| Verifying Compose output on a running emulator | `android layout` or `android screen capture --annotate` |
| Debugging a UI test failure | `android layout --diff` to see what changed |
| Checking SDK/environment state | `android info` |
| Provisioning an emulator for manual testing | `android emulator create` then `android emulator start` |
| Running an APK on a connected device | `android run --apks=<path>` |
| Executing journey tests | Read `journeys/*.xml`, follow steps with `android layout` + `adb shell input` |

**Emulator provisioning by agents:**

- Use `avd-config.json` for consistent AVD configuration.
- Check `android emulator list` first — do not create a duplicate AVD.
- Stop the emulator when done: `android emulator stop`.
- If an emulator is already running (via pipeline), use it rather than creating
  another.
- Pipeline provisioning takes precedence; agents provision only when no
  emulator is available.

**Journey tests:**

- Journey XML files live in `journeys/`. Run `scripts/run-journeys` to list or
  inspect them.
- An agent with `skill("android-cli")` loaded reads the XML and executes each
  `<action>` step-by-step against a running emulator.
- Use `android layout` to find UI elements, `adb shell input tap/swipe` to
  interact, `android screen capture` for visual verification.
- Report results as JSON per `journeys/README.md`.