# System patterns

## Layering

`UI -> Presentation -> Domain -> Data -> Source plugins / Infrastructure`

- `UI`: Compose screens and stateless `*Content` composables
- `Presentation`: ViewModels, `UiState`, actions, and effects
- `Domain`: pure models and repository contracts
- `Data`: repository implementations coordinating sources and local storage
- `Source plugins`: site-specific implementations behind `Source`
- `Infrastructure`: Room, DataStore, filesystem, Ktor, and WorkManager

## Architectural invariants

1. Domain code has zero Android, Room, Compose, or Ktor dependencies.
2. Ktor calls stay inside `Source` implementations or repositories.
3. ViewModels never depend on `SourceRegistry` or concrete sources.
4. Novel and manhwa readers stay separate screens.
5. `ChapterContent` stays a sealed interface with exactly `Text(html)` and
   `Pages(imageUrls)`.
6. Downloads stay in app-private storage.
7. No `runBlocking` in production code.

## Screen pattern

- Each screen folder owns `*Screen.kt`, `*Content.kt`, `*ViewModel.kt`, and
  `*UiState.kt`.
- `*Screen` wires `hiltViewModel()`, collects effects, and delegates rendering
  to `*Content`.
- `*Content` is the stateless preview/test target.
- `UiState` is the single source of truth, actions are the only UI entry
  point, and one-shot effects handle navigation or toasts.

## Source and data pattern

- New sites usually extend `HtmlSource`.
- Repositories are the only bridge between presentation, sources, and local
  persistence.
- `Source` IDs come from `computeSourceId(name, lang, type)`.
- Series and chapter identity is `(sourceId, url)` across domain and
  persistence.

## Persistence pattern

- Room schema changes require version bumps plus explicit migrations.
- `fallbackToDestructiveMigration` is forbidden.
- Mapper updates ship with any entity/domain change.

## Directory summary

- Main production code lives under `app/src/main/java/com/opus/readerparser/`.
- `domain/` — contracts and pure models
- `data/repository/` — repository implementations
- `data/source/` — `Source` contract and base classes
- `data/local/database/` — Room DAOs, entities, migrations, and mappers
- `data/local/filesystem/`, `data/local/prefs/` — storage and preferences
- `sources/` — per-site plugins
- `ui/` — screens, components, navigation, and theme
- `workers/` — background jobs
- `app/src/test/`, `app/src/androidTest/` — test source sets
- `journeys/` — emulator journey specs

Deep structure lives in `codemap.md`.
