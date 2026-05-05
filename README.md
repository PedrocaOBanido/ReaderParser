# Reader

A personal-use Android app that unifies webnovel and manhwa reading across
multiple sites. Each site is a **source plugin** behind a common interface —
the rest of the app doesn't know or care which site it's talking to.

> Personal project. Not for distribution. Not affiliated with any site this
> app reads from.

---

## What it does

- Browse and search across configured sites (popular, latest, full-text search).
- Add series to a local library.
- Read novels (vertical text) and manhwa (paged or webtoon) in dedicated readers.
- Download chapters for offline reading.
- Track reading progress per chapter.
- Auto-refresh the library in the background to surface new chapters.

---

## Tech stack

| Concern        | Choice                                     |
| -------------- | ------------------------------------------ |
| UI             | Jetpack Compose, Material 3                |
| Async          | Kotlin Coroutines + Flow                   |
| HTTP           | Ktor Client (OkHttp engine)                |
| HTML parsing   | Jsoup                                      |
| JSON           | kotlinx.serialization                      |
| Database       | Room                                       |
| Preferences    | Jetpack DataStore                          |
| Image loading  | Coil 3                                     |
| Background     | WorkManager                                |
| DI             | Hilt                                       |
| Min SDK        | 26 (Android 8.0)                           |
| JVM target     | 17                                         |

---

## Documentation

| File                  | What's in it                                          |
| --------------------- | ----------------------------------------------------- |
| `architecture.md`     | Layering, models, contracts, full code templates      |
| `AGENTS.md`           | Rules for AI coding agents (and humans, honestly)     |
| `project-structure.md`| Full directory tree + opencode setup details          |
| `kickoff-prompt.md`   | Session-start prompt template for agentic work        |

Read `architecture.md` first if you're reasoning about anything beyond a
single file. The other docs assume you've seen it.

---

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Verification before any handoff or commit:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:ktlintCheck
```

There's a `/verify` opencode command that runs the whole sequence.

---

## Adding a new source

The fastest path is the `/new-source` opencode command:

```
/new-source ExampleManhwa https://example.invalid MANHWA en
```

That scaffolds the directory, the class extending `HtmlSource`, the registry
entry, and placeholder test fixtures. You then fill in the CSS selectors —
the agent will not invent them, by design.

Manual path, if you'd rather:

1. Create `app/src/main/kotlin/com/opus/readerparser/sources/<sitename>/<SiteName>.kt`.
2. Extend `HtmlSource`. Override `chapterTextParse` for novels **or**
   `chapterPagesParse` for manhwa — never both.
3. Register the new source in `core/di/SourceModule.kt`.
4. Add HTML fixtures under `app/src/test/resources/fixtures/<sitename>/`.
5. Add a `MockEngine`-backed test.

Full rules in `app/src/main/kotlin/com/opus/readerparser/sources/AGENTS.md`.

---

## Working with the opencode agent

This repo is set up for opencode. The relevant files:

- `opencode.json` — project config: instruction loading, permissions.
- `AGENTS.md` (root) — non-negotiables for agents.
- `app/src/main/kotlin/**/AGENTS.md` — narrower rules close to the code.
- `.opencode/agent/` — specialized subagents (source-author, screen-author,
  room-migration, reviewer).
- `.opencode/command/` — custom slash commands (`/new-source`, `/new-screen`,
  `/add-migration`, `/verify`).

Start a new session with the kickoff prompt in `kickoff-prompt.md`. It primes
the agent with the right context and working norms before any code is
touched.

---

## Storage layout

App-private storage only. Clean uninstall removes everything.

```
filesDir/downloads/<sourceId>/<seriesUrlHash>/<chapterUrlHash>/
  meta.json
  content.html              # novels
  001.jpg, 002.jpg, ...     # manhwa, zero-padded
```

URL hashes are SHA-1 truncated to 16 hex chars. Stable across runs.

---

## What this app is not

- Not a sync service. No accounts, no cloud, no cross-device library sync.
- Not a content host. It reads from sites; downloads stay on-device.
- Not multi-user. One user, one device, one library.
- Not a browser shell. The novel reader may use a `WebView` internally for
  rendering — that's an implementation detail, not a feature.
- Not iOS, desktop, or web. Android only.

---

## License

Personal use. No license granted.