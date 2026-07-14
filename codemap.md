# Repository Atlas: ReaderParser

This document is the **descriptive repository atlas**. It owns current
structure, entry points, directory responsibilities, and implementation
locations. Use `architecture.md` for normative layer rules, contracts,
invariants, and architectural decisions.

## Project Responsibility

ReaderParser is a single-module Android application for reading webnovel and manhwa content from
site-specific source plugins behind a common `Source` contract. The repository is organised around a
strict layered architecture: Compose UI and ViewModels consume pure-Kotlin domain contracts, repository
implementations bridge those contracts to Room/DataStore/filesystem persistence, and source plugins use
Ktor + Jsoup to fetch and parse remote content.

## Root Assets

| Path | Role |
|---|---|
| `AGENTS.md` | Repository operating rules and doc-routing policy for coding agents. |
| `architecture.md` | Normative architecture guide for layer rules, contracts, invariants, and decisions. |
| `README.md` | Human-facing project overview and setup notes. |
| `app/` | The only Gradle module; contains all production Android code, tests, resources, and schemas. |
| `build.gradle.kts` | Root plugin declarations and shared build entry point. |
| `settings.gradle.kts` | Build graph and repository resolution (`:app` only). |
| `gradle.properties` | Gradle/JVM configuration flags. |
| `gradle/` | Version catalog, wrapper, and daemon JVM settings. |
| `avd-config.json` | Shared Android emulator configuration used by tooling. |
| `scripts/` | Developer and CI automation entry points. |
| `journeys/` | XML journey specs for emulator-driven interaction testing. |
| `.slim/codemap.json` | Codemap change-tracking state for this atlas. |

## System Entry Points

| Path | Responsibility |
|---|---|
| `app/src/main/java/com/opus/readerparser/App.kt` | `@HiltAndroidApp` application bootstrap and WorkManager factory integration. |
| `app/src/main/java/com/opus/readerparser/MainActivity.kt` | Single Android activity that installs the Compose app shell. |
| `app/src/main/java/com/opus/readerparser/ui/navigation/NavGraph.kt` | Registers every screen route and dispatches reader navigation by content type. |
| `app/src/main/java/com/opus/readerparser/domain/` | Repository contracts and pure domain types consumed by presentation. |
| `app/src/main/java/com/opus/readerparser/data/repository/` | Concrete repository implementations that orchestrate sources and persistence. |
| `app/src/main/java/com/opus/readerparser/data/source/Source.kt` | Stable plugin interface implemented by each site source. |
| `app/src/main/java/com/opus/readerparser/core/di/` | Hilt object-graph wiring for network, database, storage, sources, and repositories. |
| `app/src/main/java/com/opus/readerparser/workers/` | Background download and library-refresh execution via WorkManager. |

## Architecture Snapshot

- **Presentation model:** each screen follows the four-file pattern `Screen` + `Content` + `ViewModel` + `UiState`.
- **State management:** ViewModels expose a single `StateFlow<UiState>`, optional `Flow<Effect>`, and `onAction(...)`.
- **Domain boundary:** `domain/` is Android-free and defines immutable models plus repository contracts only.
- **Data orchestration:** `data/repository/` is the only layer allowed to coordinate `SourceRegistry` and local storage.
- **Plugin model:** site integrations live under `sources/`, usually extending `HtmlSource`.
- **Persistence split:** Room stores structured metadata/progress, DataStore stores app settings, filesystem stores downloaded chapter payloads.
- **Unified reader:** one Reader screen in `ui/reader/` renders both text and image content through dedicated renderers.
- **Identity rule:** series and chapters are identified by `(sourceId, url)` across layers.

## Primary Runtime Flow

1. Android launches `App` and Hilt builds the dependency graph.
2. `MainActivity` composes `ReaderParserTheme` and `AppNavGraph`.
3. Screen composables delegate actions to ViewModels.
4. ViewModels call domain repository interfaces.
5. Repository implementations read/write Room/DataStore/filesystem and call `SourceRegistry[id]` when remote data is needed.
6. Source plugins fetch and parse site content and return domain models.
7. State flows back to Compose via `StateFlow`; one-shot navigation and snackbar events flow via `Effect` channels.
8. Background work (`ChapterDownloadWorker`, `LibraryUpdateWorker`) reuses the same repository/source/storage graph outside the UI lifecycle.

## Repository Directory Map

| Directory | Responsibility Summary | Detailed Map |
|---|---|---|
| `app/` | The single Android application Gradle module. | [View Map](app/codemap.md) |
| `app/src/` | Standard Android source set root. | [View Map](app/src/codemap.md) |
| `app/src/main/` | The production source set for the app module. | [View Map](app/src/main/codemap.md) |
| `app/src/main/java/` | Root of the Kotlin source tree for production code. | [View Map](app/src/main/java/codemap.md) |
| `app/src/main/java/com/` | Convention-level namespace root for Java/Kotlin packages. | [View Map](app/src/main/java/com/codemap.md) |
| `app/src/main/java/com/opus/` | Intermediate package node between the `com` namespace root and the `readerparser` application package. | [View Map](app/src/main/java/com/opus/codemap.md) |
| `app/src/main/java/com/opus/readerparser/` | Root package of the ReaderParser Android application. | [View Map](app/src/main/java/com/opus/readerparser/codemap.md) |
| `app/src/main/java/com/opus/readerparser/core/` | The system‑wide cross‑cutting infrastructure. | [View Map](app/src/main/java/com/opus/readerparser/core/codemap.md) |
| `app/src/main/java/com/opus/readerparser/core/di/` | Hilt object-graph wiring for network, database, storage, sources, repositories, and Samsung Search. | [View Map](app/src/main/java/com/opus/readerparser/core/di/codemap.md) |
| `app/src/main/java/com/opus/readerparser/core/util/` | **Pure‑JVM utility functions** that are referenced across multiple layers of the app. | [View Map](app/src/main/java/com/opus/readerparser/core/util/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/` | Orchestration layer that **implements** the domain-defined repository interfaces by bridging source plugins (network + HTML/JSON parsing) with local persistence (Room, DataStore, filesystem). | [View Map](app/src/main/java/com/opus/readerparser/data/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/` | All local persistence on the Android device. | [View Map](app/src/main/java/com/opus/readerparser/data/local/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/search/` | Samsung Search v2 integration: `SearchIndexSyncer` observes indexable series and keeps the external search index in sync; `SamsungSearchClient` wraps ContentProvider calls; `SamsungSearchSchema` reads the schema XML asset. | [View Map](app/src/main/java/com/opus/readerparser/data/local/search/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/database/` | Room database layer — the single source of truth for structured, persisted app state. | [View Map](app/src/main/java/com/opus/readerparser/data/local/database/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/database/dao/` | Room DAO interfaces — the query surface for every database table. | [View Map](app/src/main/java/com/opus/readerparser/data/local/database/dao/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/database/entities/` | Room `@Entity` data classes — the table definitions for the app's persistent storage. | [View Map](app/src/main/java/com/opus/readerparser/data/local/database/entities/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/database/mappers/` | Pure-Kotlin extension functions that convert between Room entities and domain models. | [View Map](app/src/main/java/com/opus/readerparser/data/local/database/mappers/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/filesystem/` | File-system cache for downloaded chapter content (novel HTML and manhwa page images). | [View Map](app/src/main/java/com/opus/readerparser/data/local/filesystem/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/local/prefs/` | App-wide user preferences backed by Jetpack DataStore (Preferences). | [View Map](app/src/main/java/com/opus/readerparser/data/local/prefs/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/repository/` | Concrete implementations of the domain-defined repository interfaces. | [View Map](app/src/main/java/com/opus/readerparser/data/repository/codemap.md) |
| `app/src/main/java/com/opus/readerparser/data/source/` | The **plugin system** for content sources. | [View Map](app/src/main/java/com/opus/readerparser/data/source/codemap.md) |
| `app/src/main/java/com/opus/readerparser/domain/` | This package defines the **contract layer** — the interfaces that decouple presentation from data infrastructure, and the pure-Kotlin model types they operate on. | [View Map](app/src/main/java/com/opus/readerparser/domain/codemap.md) |
| `app/src/main/java/com/opus/readerparser/domain/model/` | This is the **pure-Kotlin domain model** layer. | [View Map](app/src/main/java/com/opus/readerparser/domain/model/codemap.md) |
| `app/src/main/java/com/opus/readerparser/sources/` | This directory holds all **source plugin implementations** — one subdirectory per supported site (e.g., `asurascans/`). | [View Map](app/src/main/java/com/opus/readerparser/sources/codemap.md) |
| `app/src/main/java/com/opus/readerparser/sources/asurascans/` | Provides the source plugin for **Asura Scans** — a manhwa scanlation aggregation site (`https://asurascans.com/`). | [View Map](app/src/main/java/com/opus/readerparser/sources/asurascans/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/` | Houses all Jetpack Compose UI code: six screens, shared components, navigation graph, and Material 3 theme. | [View Map](app/src/main/java/com/opus/readerparser/ui/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/browse/` | Provides a source-picker and browse/search interface for discovering series across all registered sources. | [View Map](app/src/main/java/com/opus/readerparser/ui/browse/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/downloads/` | Manages the download queue screen — displays queued, running, completed, and failed chapter downloads, and allows the user to cancel running/queued items or retry failed ones. | [View Map](app/src/main/java/com/opus/readerparser/ui/downloads/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/library/` | Displays the user's saved series library and provides local sort/filter controls. | [View Map](app/src/main/java/com/opus/readerparser/ui/library/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/navigation/` | Defines all app routes and the single compose-navigation graph. | [View Map](app/src/main/java/com/opus/readerparser/ui/navigation/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/reader/` | Unified Reader screen — renders both text and image content through dedicated renderers with shared immersive controls. | [View Map](app/src/main/java/com/opus/readerparser/ui/reader/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/series/` | Displays full series details (cover, title, author, status, description, genre) and its chapter list with read/downloaded state. | [View Map](app/src/main/java/com/opus/readerparser/ui/series/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/settings/` | Manages the app-wide settings screen — lets the user configure theme (system / light / dark), novel reader font size and font family, and manhwa reader layout (paged LTR / paged RTL / webtoon) and zoom (fit width / fit height / original). | [View Map](app/src/main/java/com/opus/readerparser/ui/settings/codemap.md) |
| `app/src/main/java/com/opus/readerparser/ui/theme/` | Defines the app's visual identity: Material 3 colour palette, typography, and the top-level composable that wires them into `MaterialTheme`. | [View Map](app/src/main/java/com/opus/readerparser/ui/theme/codemap.md) |
| `app/src/main/java/com/opus/readerparser/workers/` | Background task orchestration via WorkManager: chapter downloads, library refresh, Samsung Search index rebuild. Also houses `SamsungSearchUpdateReceiver` for Samsung Search broadcasts. | [View Map](app/src/main/java/com/opus/readerparser/workers/codemap.md) |
| `gradle/` | Centralised build configuration for the single-module Android project. | [View Map](gradle/codemap.md) |
| `scripts/` | Developer tooling and CI automation surface for the ReaderParser project. | [View Map](scripts/codemap.md) |

## How To Use This Atlas

- Start here for codebase navigation, current structure, entry points, and the directory index.
- Read `architecture.md` for layer rules, contracts, invariants, and architectural decisions.
- Before editing a feature area, open that folder's `codemap.md` for local implementation details.
- For app-wide work, the most important sub-maps are `app/src/main/java/com/opus/readerparser/codemap.md`,
  `app/src/main/java/com/opus/readerparser/ui/codemap.md`,
  `app/src/main/java/com/opus/readerparser/data/codemap.md`, and
  `app/src/main/java/com/opus/readerparser/domain/codemap.md`.
