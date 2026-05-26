# app/src/main/java/com/opus/readerparser/core/

## Responsibility

The system‑wide cross‑cutting infrastructure. This package owns the **dependency
injection wiring**, the **pure‑JVM utility functions**, and a **placeholder for
result / error‑mapping types** that all layers may reference. No business logic,
no UI, no data‑access code lives here.

Sub‑packages:

| Package    | Role |
|------------|------|
| `di/`      | All Hilt modules — one file per subsystem |
| `result/`  | Reserved for shared `Result` / error‑mapping types (currently empty) |
| `util/`    | Stateless helper functions with zero Android dependencies |

## Design

- **Hilt‑only DI**: Every binding lives in `di/` as a `@Module` installed in
  `SingletonComponent`. Services are wired with `@Provides` (third‑party or
  Android types) or `@Binds` (interface‑to‑impl for our own types).
- **No Android types in `util/`**: Both `ComputeSourceId.kt` and `Hashing.kt`
  compile against plain JVM, preserving the domain layer's purity constraint.
- **One module per concern**: Each Hilt module (`DatabaseModule`,
  `NetworkModule`, …) is self‑contained; no module pulls dependencies from
  another module's private bindings — they rely only on types already in the
  Dagger graph.
- **Qualifiers for disambiguation**: `FilesystemModule` defines a
  `@DownloadRoot` qualifier so the `File` for the download directory doesn't
  collide with other `File` bindings.

## Flow

```
App startup
  └─ Hilt enters SingletonComponent
       ├─ SourceModule          → SourceRegistry (reads `di/` modules for bindings)
       ├─ NetworkModule         → HttpClient, Json
       ├─ DatabaseModule        → AppDatabase, SeriesDao, ChapterDao, DownloadQueueDao
       ├─ FilesystemModule      → DownloadStore, @DownloadRoot File
       ├─ PrefsModule           → DataStore<Preferences>
       └─ RepositoryModule      → binds 5 repository interfaces → impls

Runtime
  Repositories (data/repository/) inject HttpClient + SourceRegistry + DAOs + DownloadStore + DataStore
  ViewModels inject repository interfaces (domain/)
  Every injection edge passes through a binding declared in core/di/
```

## Integration

- **Consumed by**: All `data/repository/` implementations, which inject the
  services wired here. ViewModels in `ui/*/` consume only domain‑layer
  interfaces and never see the DI graph directly.
- **Dependencies on**: `data/` package (DAO interfaces, source interfaces,
  pref store, filesystem store), `domain/` package (repository interfaces),
  `sources/` package (concrete `Source` implementations wired into
  `SourceModule`), and third‑party libraries (Ktor, Room, DataStore, Coil,
  Jsoup).
- **Does not depend on**: `ui/` (presentation layer is a pure consumer).
