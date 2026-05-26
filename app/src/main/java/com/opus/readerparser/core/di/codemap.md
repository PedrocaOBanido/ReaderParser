# app/src/main/java/com/opus/readerparser/core/di/

## Responsibility

Contains **all Hilt modules** that wire the app's object graph. Each module is
responsible for providing a single subsystem's bindings. Together they form the
complete dependency graph that every ViewModel, repository, and source plugin
consumes.

No business logic exists here — only wiring.

## Design

Six `@Module` files, all installed in `SingletonComponent`:

| File | Provides | Pattern |
|------|----------|---------|
| `DatabaseModule.kt` | `AppDatabase`, `SeriesDao`, `ChapterDao`, `DownloadQueueDao` | `@Provides` |
| `FilesystemModule.kt` | `DownloadStore`, `@DownloadRoot File` | `@Binds` (interface) + `@Provides` (Android `File`) |
| `NetworkModule.kt` | `HttpClient` (OkHttp engine), `Json` | `@Provides` |
| `PrefsModule.kt` | `DataStore<Preferences>` | `@Provides` via extension `settingsDataStore` |
| `RepositoryModule.kt` | `SeriesRepository`, `ChapterRepository`, `SourceRepository`, `DownloadRepository`, `SettingsRepository` | `@Binds` (all interface‑to‑impl) |
| `SourceModule.kt` | `SourceRegistry` (compile‑time map of all source plugins) | `@Provides` |

Key decisions:

- **`@Binds` for first‑party interfaces, `@Provides` for third‑party & Android types.**
  This keeps the module code minimal and makes it impossible to accidentally
  provide two instances of the same interface.
- **`@DownloadRoot` qualifier** (`FilesystemModule`) prevents `File` ambiguity
  in the Dagger graph.
- **`RepositoryModule` is a single abstract class** with five `@Binds` methods
  rather than five separate modules. All repository bindings co‑locate for
  discoverability.
- **`SourceModule` is the registration point** for new site plugins. Adding a
  source means adding one line in `listOf(...)`.

## Flow

```
SingletonComponent (app start)
  │
  ├─ SourceModule.registry(client)
  │   └─ instantiates every Source plugin → associateBy { it.id } → SourceRegistry
  │
  ├─ NetworkModule provideHttpClient(ctx, json)
  │   └─ HttpClient(OkHttp) with cookies, retry, timeout, content-negotiation, cache
  │
  ├─ DatabaseModule provideDatabase(ctx)
  │   └─ Room.databaseBuilder → AppDatabase
  │       ├─ provideSeriesDao(db)
  │       ├─ provideChapterDao(db)
  │       └─ provideDownloadQueueDao(db)
  │
  ├─ FilesystemModule
  │   ├─ provideDownloadRoot(ctx) → @DownloadRoot File
  │   └─ bindDownloadStore(impl) → DownloadStore
  │
  ├─ PrefsModule providePreferencesDataStore(ctx)
  │   └─ DataStore<Preferences>
  │
  └─ RepositoryModule
      ├─ bindSeriesRepository(impl)     → SeriesRepository
      ├─ bindChapterRepository(impl)    → ChapterRepository
      ├─ bindSourceRepository(impl)     → SourceRepository
      ├─ bindDownloadRepository(impl)   → DownloadRepository
      └─ bindSettingsRepository(impl)   → SettingsRepository
```

## Integration

- **Producers**: Each module is the sole provider of its subsystem's root types.
  For example, `HttpClient` is only provided by `NetworkModule`; no other module
  creates a second client.
- **Consumers**: Every `*RepositoryImpl` in `data/repository/` injects the
  services wired here. ViewModels inject domain‑level interfaces that are bound
  in `RepositoryModule`. Source plugins (`sources/*`) receive `HttpClient` from
  the graph and are assembled inside `SourceModule`.
- **Testability**: The `@Binds` pattern makes it trivial to replace any
  repository with a `Fake*` in a test component. The `@DownloadRoot` qualifier
  allows JVM tests to inject a `TemporaryFolder` root without Android context.
- **Does not depend on**: `ui/` in any form.
