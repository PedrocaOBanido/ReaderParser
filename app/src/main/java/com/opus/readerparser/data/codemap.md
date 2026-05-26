# app/src/main/java/com/opus/readerparser/data/

## Responsibility

Orchestration layer that **implements** the domain-defined repository interfaces
by bridging source plugins (network + HTML/JSON parsing) with local persistence
(Room, DataStore, filesystem). This is the only layer that can reference both
`Source` types and Android/DB infrastructure.

Sub-packages are sealed by concern:
- `data/source/` — plugin contract (`Source`, `HtmlSource`, `SourceRegistry`)
- `data/repository/` — repository implementations (`SeriesRepositoryImpl`, `ChapterRepositoryImpl`, `DownloadRepositoryImpl`, `SourceRepositoryImpl`)
- `data/local/` — all local persistence (database, filesystem cache, preferences)
- `data/network/` — Ktor `HttpClient` config (currently empty; wiring lives in `core/di/NetworkModule`)

## Design

**Repository-per-aggregate.** Each domain interface (`SeriesRepository`,
`ChapterRepository`, etc.) has exactly one `data/repository/*Impl` class.
Constructors receive both `SourceRegistry` (for network-backed operations) and
the relevant Room DAO or DataStore wrapper. This keeps orchestration logic in
one place per aggregate.

**`SourceRegistry` as a compile-time map.** Plugins are registered in
`core/di/SourceModule.kt`; `SourceRegistry` is a thin `Map<Long, Source>`
wrapper. There is no dynamic loading — every source is statically known at
build time.

**Identity discipline.** All Room entities use composite primary keys
`(sourceId, url)` matching the domain identity rule. No auto-increment IDs
cross layer boundaries.

**Reactive exposure.** Repositories expose `Flow<List<...>>` for observable
state (library, chapters, download queue) by mapping Room DAO flows through
mapper extension functions. One-shot methods (network fetches, writes) are
`suspend`.

## Flow

```
ViewModel → domain/Repository interface  →  data/repository/Impl
                                               ├── SourceRegistry (network)
                                               └── data/local/ (persistence)
```

- **Read path (observable):** ViewModel collects a `Flow` from the domain
  repository → the impl delegates to a Room DAO `observe*()` flow → DAO
  queries Room → entities are mapped to domain models via `mappers/`.
- **Write path (user action):** ViewModel calls `suspend` method on domain
  repository → impl writes to Room DAO or DataStore.
- **Network fetch path:** ViewModel calls `suspend` method → impl calls
  `SourceRegistry[id].get*()` → source plugin fetches/parses → impl persists
  results to Room → UI observes updates reactively through the flow.
- **Downloaded content reads:** `ChapterRepository.getContent()` checks
  `DownloadStore.read()` first (local file cache); if absent, falls through to
  `SourceRegistry[id].getChapterContent()` (network).

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `domain/` (interfaces & models) | Implements | `SeriesRepository`, `ChapterRepository`, `DownloadRepository`, `SourceRepository`, `SettingsRepository` — all defined in `domain/` |
| `core/di/` | Wired by Hilt | `RepositoryModule` binds `*Impl` → domain interfaces; `DatabaseModule` provides DAOs; `SourceModule` provides `SourceRegistry` |
| `sources/` (plugins) | Calls into | `SourceRegistry[id].get*()` from repository impls |
| `domain/model/` | Returns domain types | Entities are mapped via `mappers/` extension functions |
