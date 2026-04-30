# Project structure

Layout for the reader app, combining the standard Android/Gradle layout with
opencode's project-local configuration conventions (`.opencode/`,
`opencode.json`, nested `AGENTS.md` files).

---

## 1. Full tree

```
reader-app/                              # repo root
│
├── .opencode/                           # opencode project-local config
│   ├── agent/                           # specialized subagents
│   │   ├── source-author.md             #   writes new Source plugins
│   │   ├── screen-author.md             #   scaffolds new Compose screens
│   │   ├── room-migration.md            #   handles Room schema changes
│   │   └── reviewer.md                  #   read-only code review
│   ├── command/                         # custom slash commands
│   │   ├── new-source.md                #   /new-source <SiteName>
│   │   ├── new-screen.md                #   /new-screen <ScreenName>
│   │   ├── verify.md                    #   /verify  → build + lint + tests
│   │   └── add-migration.md             #   /add-migration <description>
│   └── skills/                          # injectable knowledge packs (optional)
│       └── jsoup-parsing/
│           └── SKILL.md
│
├── .github/                             # CI (optional, recommended)
│   └── workflows/
│       └── ci.yml
│
├── app/                                 # the only Gradle module (for now)
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── schemas/                         # Room exported schemas — CHECK IN
│   │   └── com.example.reader.data.local.database.AppDatabase/
│   │       ├── 1.json
│   │       └── 2.json
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/example/reader/
│       │   │   ├── App.kt                                 # @HiltAndroidApp
│       │   │   │
│       │   │   ├── ui/
│       │   │   │   ├── AGENTS.md                          # Compose-specific rules
│       │   │   │   ├── library/
│       │   │   │   │   ├── LibraryScreen.kt
│       │   │   │   │   ├── LibraryContent.kt
│       │   │   │   │   ├── LibraryViewModel.kt
│       │   │   │   │   └── LibraryUiState.kt              # state, action, effect
│       │   │   │   ├── browse/
│       │   │   │   │   ├── SourceListScreen.kt
│       │   │   │   │   ├── BrowseScreen.kt
│       │   │   │   │   └── …
│       │   │   │   ├── series/
│       │   │   │   ├── reader/
│       │   │   │   │   ├── novel/
│       │   │   │   │   │   ├── NovelReaderScreen.kt
│       │   │   │   │   │   ├── NovelReaderContent.kt
│       │   │   │   │   │   ├── NovelReaderViewModel.kt
│       │   │   │   │   │   └── NovelReaderUiState.kt
│       │   │   │   │   └── manhwa/
│       │   │   │   │       ├── MangaReaderScreen.kt
│       │   │   │   │       ├── MangaReaderContent.kt
│       │   │   │   │       ├── MangaReaderViewModel.kt
│       │   │   │   │       └── MangaReaderUiState.kt
│       │   │   │   ├── downloads/
│       │   │   │   ├── settings/
│       │   │   │   ├── components/                        # shared composables
│       │   │   │   │   ├── Cover.kt
│       │   │   │   │   ├── ChapterRow.kt
│       │   │   │   │   ├── StatusPill.kt
│       │   │   │   │   └── ErrorState.kt
│       │   │   │   ├── navigation/
│       │   │   │   │   ├── NavGraph.kt
│       │   │   │   │   └── Destinations.kt
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt
│       │   │   │       ├── Type.kt
│       │   │   │       └── Theme.kt
│       │   │   │
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   ├── Series.kt
│       │   │   │   │   ├── Chapter.kt
│       │   │   │   │   ├── ChapterContent.kt
│       │   │   │   │   ├── ContentType.kt
│       │   │   │   │   ├── Filter.kt
│       │   │   │   │   └── SeriesPage.kt
│       │   │   │   └── usecase/                           # only when non-trivial
│       │   │   │       └── MarkChapterReadUseCase.kt
│       │   │   │
│       │   │   ├── data/
│       │   │   │   ├── repository/
│       │   │   │   │   ├── SeriesRepository.kt            # interface
│       │   │   │   │   ├── SeriesRepositoryImpl.kt
│       │   │   │   │   ├── ChapterRepository.kt
│       │   │   │   │   └── ChapterRepositoryImpl.kt
│       │   │   │   ├── local/
│       │   │   │   │   ├── database/
│       │   │   │   │   │   ├── AGENTS.md                  # Room migration rules
│       │   │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   │   ├── entities/
│       │   │   │   │   │   │   ├── SeriesEntity.kt
│       │   │   │   │   │   │   ├── ChapterEntity.kt
│       │   │   │   │   │   │   └── DownloadQueueEntity.kt
│       │   │   │   │   │   ├── dao/
│       │   │   │   │   │   │   ├── SeriesDao.kt
│       │   │   │   │   │   │   ├── ChapterDao.kt
│       │   │   │   │   │   │   └── DownloadQueueDao.kt
│       │   │   │   │   │   ├── migrations/
│       │   │   │   │   │   │   └── Migration_1_2.kt
│       │   │   │   │   │   └── mappers/
│       │   │   │   │   │       ├── SeriesMappers.kt
│       │   │   │   │   │       └── ChapterMappers.kt
│       │   │   │   │   ├── filesystem/
│       │   │   │   │   │   ├── DownloadStore.kt
│       │   │   │   │   │   └── Paths.kt
│       │   │   │   │   └── prefs/
│       │   │   │   │       ├── SettingsStore.kt
│       │   │   │   │       └── Keys.kt
│       │   │   │   ├── source/                            # Source contract & base
│       │   │   │   │   ├── AGENTS.md                      # Source-contract rules
│       │   │   │   │   ├── Source.kt
│       │   │   │   │   ├── HtmlSource.kt
│       │   │   │   │   └── SourceRegistry.kt
│       │   │   │   └── network/
│       │   │   │       ├── HttpClientFactory.kt
│       │   │   │       ├── PersistentCookieJar.kt
│       │   │   │       └── Json.kt
│       │   │   │
│       │   │   ├── sources/                               # per-site plugins
│       │   │   │   ├── AGENTS.md                          # how to add a source
│       │   │   │   ├── examplemanhwa/
│       │   │   │   │   └── ExampleManhwa.kt
│       │   │   │   └── examplenovel/
│       │   │   │       └── ExampleNovel.kt
│       │   │   │
│       │   │   ├── workers/
│       │   │   │   ├── ChapterDownloadWorker.kt
│       │   │   │   └── LibraryUpdateWorker.kt
│       │   │   │
│       │   │   └── core/
│       │   │       ├── di/                                # Hilt modules
│       │   │       │   ├── NetworkModule.kt
│       │   │       │   ├── DatabaseModule.kt
│       │   │       │   ├── SourceModule.kt
│       │   │       │   └── RepositoryModule.kt
│       │   │       ├── result/
│       │   │       │   └── Failure.kt
│       │   │       └── util/
│       │   │           ├── Hashing.kt
│       │   │           ├── Dates.kt
│       │   │           └── Extensions.kt
│       │   │
│       │   └── res/
│       │       ├── drawable/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │
│       ├── test/                        # JVM unit tests
│       │   ├── kotlin/com/example/reader/
│       │   │   ├── fakes/                                 # hand-rolled fakes
│       │   │   │   ├── FakeSeriesRepository.kt
│       │   │   │   ├── FakeChapterRepository.kt
│       │   │   │   └── FakeSource.kt
│       │   │   ├── sources/
│       │   │   │   └── examplemanhwa/
│       │   │   │       └── ExampleManhwaTest.kt
│       │   │   ├── data/repository/
│       │   │   └── ui/                                    # ViewModel tests
│       │   └── resources/                                 # HTML fixtures
│       │       └── fixtures/
│       │           └── examplemanhwa/
│       │               ├── popular.html
│       │               ├── series.html
│       │               └── chapter.html
│       │
│       └── androidTest/                 # instrumented tests
│           └── kotlin/com/example/reader/
│               ├── data/local/database/
│               │   └── MigrationTest.kt
│               └── ui/                                    # Compose UI tests
│                   └── library/
│                       └── LibraryContentTest.kt
│
├── gradle/
│   ├── libs.versions.toml               # version catalog
│   └── wrapper/
│
├── AGENTS.md                            # root agent rules (already written)
├── architecture.md                      # architecture spec (already written)
├── opencode.json                        # opencode project config
├── README.md
├── build.gradle.kts                     # root build script
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── .editorconfig
```

---

## 2. Why nested `AGENTS.md` files

opencode walks up from the file being edited and merges every `AGENTS.md` it
finds along the way. The most-specific one wins on overlapping rules, and
unique rules in higher-up files still apply. Use this to keep the root
`AGENTS.md` short and push narrow rules close to the code they govern.

| Location                                | Scope                                  |
| --------------------------------------- | -------------------------------------- |
| `AGENTS.md` (root)                      | Cross-cutting non-negotiables          |
| `app/.../ui/AGENTS.md`                  | Compose, ViewModel, navigation         |
| `app/.../data/source/AGENTS.md`         | The `Source` contract itself           |
| `app/.../sources/AGENTS.md`             | How to write a per-site plugin         |
| `app/.../data/local/database/AGENTS.md` | Room migrations, schema changes        |

Keep each nested file short — under ~50 lines. They are reminders, not
re-statements of the architecture.

---

## 3. `opencode.json`

Drop this at repo root. It pulls in `architecture.md`, every nested
`AGENTS.md`, and tightens permissions on destructive commands.

```jsonc
{
  "$schema": "https://opencode.ai/config.json",

  // Pull architecture doc + every nested AGENTS.md into context.
  // Root AGENTS.md is loaded automatically; the glob covers nested ones.
  "instructions": [
    "architecture.md",
    "app/src/main/kotlin/**/AGENTS.md"
  ],

  // Auto-allow safe commands; ask for anything that mutates the world.
  "permission": {
    "edit": "allow",
    "webfetch": "allow",
    "bash": {
      "./gradlew *":                  "allow",
      "git status":                   "allow",
      "git diff *":                   "allow",
      "git log *":                    "allow",
      "git add *":                    "allow",
      "git restore *":                "allow",
      "git commit *":                 "ask",
      "git push *":                   "ask",
      "git reset --hard *":           "deny",
      "rm -rf *":                     "deny",
      "adb *":                        "ask",
      "*":                            "ask"
    }
  },

  "autoupdate": "notify"
}
```

The model field is intentionally omitted — set it once globally in
`~/.config/opencode/opencode.json` and let every project inherit. Override
per-project only when this project genuinely needs a different model.

---

## 4. Custom commands

Custom slash commands live in `.opencode/command/`. The filename becomes
the command name. Below are the four worth having on day one.

### `.opencode/command/new-source.md`

```markdown
---
description: Scaffold a new Source plugin from a template.
agent: source-author
---

Create a new source plugin for the site named `$1` (PascalCase) at base URL
`$2`, content type `$3` (NOVEL or MANHWA), language `$4` (ISO 639-1 code,
default "en" if not given).

Required steps:

1. Create directory `app/src/main/kotlin/com/example/reader/sources/${1,,}/`
   (lowercase the package).
2. Create `$1.kt` extending `HtmlSource`. Override only the methods listed in
   `app/src/main/kotlin/com/example/reader/sources/AGENTS.md`. Leave the
   non-applicable content method as `error("...")`.
3. Compute `id` via `computeSourceId("$1", "$4", ContentType.$3)`.
4. Add the source to the `SourceModule` provider in
   `app/src/main/kotlin/com/example/reader/core/di/SourceModule.kt`.
5. Create test scaffolding under `app/src/test/kotlin/.../sources/${1,,}/`
   with placeholder fixtures in `app/src/test/resources/fixtures/${1,,}/`.

Stop and ask the user for the CSS selectors before filling in parser logic.
Do not invent selectors.
```

### `.opencode/command/new-screen.md`

```markdown
---
description: Scaffold a new Compose screen with the standard four-file pattern.
agent: screen-author
---

Create a new screen named `$1` (PascalCase, no "Screen" suffix).

Generate exactly four files in `app/src/main/kotlin/com/example/reader/ui/${1,,}/`:

1. `$1Screen.kt` — entry point. Takes `viewModel: $1ViewModel = hiltViewModel()`,
   collects state with `collectAsStateWithLifecycle()`, collects effects in
   a `LaunchedEffect(Unit)`, and delegates to `$1Content`.
2. `$1Content.kt` — stateless. Signature `(state: $1UiState, onAction: ($1Action) -> Unit)`.
   Includes a `@Preview` composable.
3. `$1ViewModel.kt` — `@HiltViewModel`. Exposes `state: StateFlow<$1UiState>`,
   `effects: Flow<$1Effect>`, and `fun onAction(action: $1Action)`.
4. `$1UiState.kt` — contains the `$1UiState` data class, `$1Action` sealed
   interface, and `$1Effect` sealed interface.

Add a destination entry to `ui/navigation/Destinations.kt` and a composable
entry in `ui/navigation/NavGraph.kt`.

Do not implement business logic — leave TODOs. Ask for the screen's purpose
before writing any state fields.
```

### `.opencode/command/verify.md`

```markdown
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
```

### `.opencode/command/add-migration.md`

```markdown
---
description: Add a Room migration for a schema change.
agent: room-migration
---

Schema change description: $ARGUMENTS

Required steps:

1. Read the current `AppDatabase.version` and the latest schema JSON in
   `app/schemas/`.
2. Bump `AppDatabase.version` by 1.
3. Create `Migration_${old}_${new}.kt` under
   `app/src/main/kotlin/com/example/reader/data/local/database/migrations/`
   with explicit SQL for the change. Do not use any auto-migration helpers.
4. Register the migration in `DatabaseModule`.
5. Add a migration test in
   `app/src/androidTest/kotlin/com/example/reader/data/local/database/MigrationTest.kt`
   using `MigrationTestHelper`.
6. Update affected entities, DAOs, and mappers.
7. Run `./gradlew :app:assembleDebug` to regenerate the schema JSON; verify
   the new file appears in `app/schemas/`.

Never use `fallbackToDestructiveMigration` to skip writing the migration.
```

---

## 5. Specialized subagents

Subagents live in `.opencode/agent/`. The filename is the agent name. Each
gets a focused prompt and is invoked either by `@source-author` in chat or
implicitly by a custom command.

### `.opencode/agent/source-author.md`

```markdown
---
description: Writes Source plugins for new sites. Knows HtmlSource, Jsoup, and the registration flow.
---

You write `Source` plugins for this app. The contract you implement is in
`app/src/main/kotlin/com/example/reader/data/source/Source.kt`. The base
class you extend is `HtmlSource` in the same directory.

You have three jobs and only three:

1. Implement a new class extending `HtmlSource`.
2. Override the minimum set of methods the base class requires.
3. Register the class in the Hilt `SourceModule`.

You do NOT:
- Modify the `Source` interface.
- Modify `HtmlSource` to fit a specific site.
- Add new dependencies.
- Catch exceptions inside source methods. Throw, don't log.

Before writing parser logic, ask the user for: the popular listing URL
pattern, CSS selectors for the listing, the series details selectors, and
the chapter content selectors. Do not invent selectors from the site name.

Sources are tested with `MockEngine` and HTML fixtures saved under
`app/src/test/resources/fixtures/<sourcename>/`. Generate placeholder fixture
files when scaffolding; the user will replace them with real captures.
```

### `.opencode/agent/screen-author.md`

```markdown
---
description: Scaffolds Compose screens following the four-file pattern.
---

You create Compose screens. Every screen is exactly four files:
`<Name>Screen.kt`, `<Name>Content.kt`, `<Name>ViewModel.kt`, `<Name>UiState.kt`.

Rules you do not break:
- `<Name>Screen.kt` is never previewed. `<Name>Content.kt` always has a `@Preview`.
- `UiState` is a single data class with everything the screen renders.
- `Action` is a sealed interface. One per UI event.
- `Effect` is a sealed interface, sent through a `Channel<Effect>(BUFFERED)`.
- Navigation goes through `Effect`, never through `UiState`.
- `collectAsStateWithLifecycle()` only. Never `collectAsState`.

You don't write business logic. You scaffold the structure and leave TODO
markers for the ViewModel body. Ask the user for the screen's purpose before
writing any state fields.

Reference: `app/src/main/kotlin/com/example/reader/ui/AGENTS.md`.
```

### `.opencode/agent/room-migration.md`

```markdown
---
description: Handles Room schema changes. Writes explicit migrations and tests.
---

You change Room schemas. You always:

1. Bump `AppDatabase.version` by exactly 1.
2. Write an explicit `Migration` class with hand-written SQL.
3. Add a migration test using `MigrationTestHelper`.
4. Update entities, DAOs, and the mapper functions.
5. Verify `app/schemas/<version>.json` is regenerated after the build.

You never:
- Use `fallbackToDestructiveMigration` in any configuration.
- Hand-edit files in `app/schemas/`.
- Skip the migration test.
- Change a primary key or foreign key without explicit approval — ask first.

Reference: `app/src/main/kotlin/com/example/reader/data/local/database/AGENTS.md`.
```

### `.opencode/agent/reviewer.md`

```markdown
---
description: Read-only code reviewer. Does not edit files. Use before merging or as a sanity check.
---

You are a code reviewer. You do not edit files. You produce a review.

Check, in order:

1. **Layering** — does any file import across forbidden boundaries? (domain
   importing Android types, ViewModels touching `SourceRegistry`, composables
   calling `HttpClient`).
2. **Identity** — do new entities and DAOs use `(sourceId, url)` as the key?
3. **State** — does each new screen follow the four-file pattern? Is there
   exactly one `UiState` data class?
4. **Errors** — are exceptions caught only at the ViewModel boundary?
5. **Tests** — does each new repository, ViewModel, and `Source` have a test?
6. **Style** — ktlint, no wildcard imports, no `runBlocking` outside tests.

Report findings grouped by severity: blocker / should-fix / nit. Quote the
file path and line number for each. Do not propose code; describe the issue
and let the user decide.
```

---

## 6. Nested `AGENTS.md` examples

Short, targeted reminders. Each lives next to the code it governs.

### `app/src/main/kotlin/com/example/reader/ui/AGENTS.md`

```markdown
# UI rules

- Material 3 only. No Material 2 imports.
- Every screen = four files: `*Screen.kt`, `*Content.kt`, `*ViewModel.kt`, `*UiState.kt`.
- `*Screen` wires the ViewModel and collects effects. Never previewed.
- `*Content` is stateless. Always has a `@Preview`.
- Collect state with `collectAsStateWithLifecycle()`. Never `collectAsState`.
- Navigation goes through `Effect`, never `UiState`.
- Hardcoded colors and dp values belong in `ui/theme/`, nowhere else.
- Hoist anything used in 2+ screens into `ui/components/`.
```

### `app/src/main/kotlin/com/example/reader/data/source/AGENTS.md`

```markdown
# Source contract

- The `Source` interface is stable. Changing it requires explicit human approval.
- `HtmlSource` is the base class for HTML sites. Don't add site-specific logic to it.
- `SourceRegistry` is a `Map<Long, Source>` populated by Hilt. No dynamic loading.
- Source IDs come from `computeSourceId(name, lang, type)`. Never hand-pick.
- Sources throw on error. They do not log, do not catch, do not return null sentinels.
```

### `app/src/main/kotlin/com/example/reader/sources/AGENTS.md`

```markdown
# Adding a source

1. One directory per site, lowercase: `sources/<sitename>/`.
2. One file: `<SiteName>.kt` extending `HtmlSource`.
3. Override `chapterTextParse` for novels OR `chapterPagesParse` for manhwa, never both.
4. Register in `core/di/SourceModule.kt`.
5. Add HTML fixtures under `app/src/test/resources/fixtures/<sitename>/`.
6. Add a test using `MockEngine` and the fixtures.

Use `selectFirst(...)` + null-safety, not `select(...).first()`. Always
`absUrl(...)` for hrefs and image sources. Always `.trim()` text nodes.
```

### `app/src/main/kotlin/com/example/reader/data/local/database/AGENTS.md`

```markdown
# Database rules

- Bump `AppDatabase.version` by 1 per schema change. Never skip versions.
- Every version change ships with an explicit `Migration` class. No exceptions.
- `fallbackToDestructiveMigration` is forbidden in every build configuration.
- `app/schemas/` is auto-generated. Don't hand-edit. Commit changes to it.
- Identity is `(sourceId, url)`. Never use auto-incrementing IDs as foreign keys.
- Migration tests are required. They live in `androidTest/`, not `test/`.
```

---

## 7. `.gitignore` additions

opencode does not need anything excluded — `.opencode/` is meant to be
committed so the team (or future you) shares the same workflow rules. Same
for `AGENTS.md` and `opencode.json`.

Standard Android `.gitignore` entries still apply:

```
*.iml
.gradle/
local.properties
.idea/
build/
captures/
.externalNativeBuild/
.cxx/
.kotlin/
```

---

## 8. Bootstrap order

When setting up the repo from scratch:

1. Create the Android project with Android Studio's "Empty Activity (Compose)"
   template. Pick the package name; the rest of this doc uses
   `com.example.reader` as a placeholder.
2. Replace `app/src/main/kotlin/com/example/reader/MainActivity.kt` with the
   directory layout from §1. Empty packages are fine; create files as needed.
3. Drop in `architecture.md`, `AGENTS.md`, and `opencode.json` at the repo root.
4. Create `.opencode/agent/`, `.opencode/command/` and populate from §4 and §5.
5. Run `opencode` in the repo and verify with a small task — e.g., ask it to
   `/new-source TestSite https://example.invalid MANHWA en` and check that the
   scaffolded files match the conventions in `sources/AGENTS.md`.
6. Drop in the nested `AGENTS.md` files from §6 once the corresponding
   directories have any code in them.

```