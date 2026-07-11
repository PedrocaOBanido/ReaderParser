# app/src/main/java/com/opus/readerparser/domain/

## Responsibility

This package defines the **contract layer** — the interfaces that decouple
presentation from data infrastructure, and the pure-Kotlin model types they
operate on.

- **Repository interfaces** (`*.kt` at this level): the contract between
  ViewModels (upper layer) and data-layer implementations (lower layer).
- **`model/` sub-package**: the shape of every data object that crosses a
  contract boundary (see `model/codemap.md`).

Everything here compiles against plain JVM. No Android, Ktor, Room, Compose,
or Jsoup types appear.

The package contains six files (interface contracts) and a sub-package
`model/` with ~14 files (data types). No use cases live here — the app is
small enough that ViewModels call repositories directly.

## Design

### 1. Repository interfaces — ownership map

| Interface | Key methods | Domain models used |
|---|---|---|
| `SeriesRepository` | `observeLibrary()`, `fetchPopular/Latest/search()`, `refreshDetails()`, `addToLibrary()/removeFromLibrary()`, `isInLibrary()` | `Series`, `SeriesPage`, `FilterList` |
| `ChapterRepository` | `observeChapters()`, `refreshChapters()`, `getContent()`, `markRead()`, `setProgress()`, `findByUrl()` | `Chapter`, `ChapterContent`, `ChapterWithState`, `Series` |
| `SourceRepository` | `getSources()` | `SourceInfo` |
| `DownloadRepository` | `observeQueue()`, `cancel()`, `retry()`, `updateQueueState()` | `DownloadItem`, `DownloadState` |
| `SettingsRepository` | `observeSettings()`, `setTheme()`, `setNovelFontSize/Family()`, `setManhwaLayout/Zoom()` | `AppSettings`, `AppTheme`, `ManhwaLayout`, `ManhwaZoom` |

### 2. Reactive vs. suspend boundary

- **`observe*()` methods return `Flow<T>`** — these power real-time UI updates
  from Room (via `Flow` DAOs) or DataStore. ViewModels collect them with
  `.stateIn(viewModelScope, WhileSubscribed(5000), initialValue)`.
- **`fetch*()`, `search()`, `refresh*()` methods are `suspend`** — they
  perform network I/O and return a single result. ViewModels call them inside
  `viewModelScope.launch { }`.
- **`addToLibrary()`, `removeFromLibrary()`, `markRead()`, `setProgress()` are
  `suspend`** — fire-and-forget side-effect writes. The UI reacts via the
  corresponding `observe*()` flow.

### 3. Identity binding

Every repository interface uses the `(sourceId, url)` pair to identify series
and chapters. There is no auto-increment ID anywhere in the domain layer.

- `SeriesRepository.fetchPopular(sourceId, page)` — source-scoped listing.
- `ChapterRepository.observeChapters(series: Series)` — derives `series.url`
  and `series.sourceId` from the model, but the interface takes a `Series`
  object to avoid leaking identity internals to callers.
- `ChapterRepository.findByUrl(sourceId, url)` — explicit pair lookup for
  navigation deep links.

### 4. Contracts do not throw — implementations may

Repository interfaces declare no `@Throws` annotations. Callers should assume
that implementations may throw on network errors, parse errors, or storage
failures. ViewMLodels catch at the boundary and map to `UiState.error`.

The exception is `getContent(chapter)` on `ChapterRepository` — it may
delegate to a remote source which can throw. The ViewModel catches this.

### 5. No aggregation / composite interfaces

Each interface owns one concern. A ViewModel that needs both series metadata
and chapters injects two repositories:
```kotlin
class SeriesViewModel @Inject constructor(
    private val seriesRepo: SeriesRepository,
    private val chapterRepo: ChapterRepository,
)
```

### 6. `SourceRepository` is intentionally thin

`SourceRepository.getSources(): List<SourceInfo>` is a non-suspend, cold call
that returns a static list. This is by design — the list of installed sources
is determined at compile time (`SourceModule` in `core/di/`). The data
implementation wraps `SourceRegistry.all()` mapped to `SourceInfo`.

## Flow

```
  UI / Compose
      │  collectAsStateWithLifecycle()
      ▼
  ViewModel (ui/*/)
      │  calls suspend methods / collects Flow
      ▼
  ┌─────────────────────────────┐
  │   domain/ (this package)    │
  │   Repository interfaces     │
  │   (pure JVM contracts)      │
  └──────┬──────────────────────┘
         │  injected by Hilt
         ▼
  data/repository/ (implementations)
      │  orchestrates SourceRegistry + DAO + DownloadStore
      ▼
  ┌─────────────────────────────┐
  │ Source plugins / Room / Ktor│
  │ (infrastructure layer)      │
  └─────────────────────────────┘
```

1. ViewModel calls `seriesRepo.fetchPopular(sourceId, page)`.
2. The implementation (`SeriesRepositoryImpl`) looks up the `Source` from
   `SourceRegistry`, calls `source.getPopular(page)`, maps the result to
   `SeriesPage`, optionally persists to Room.
3. Returns `SeriesPage` to the ViewModel.
4. ViewModel maps to `BrowseUiState`.

### Typical ViewModel-init flow

```
ViewModel.init {
    viewModelScope.launch {
        seriesRepo.observeLibrary()
            .stateIn(...)
            .collect { uiState = it.mapToUiState() }
    }
}

fun onAction(action: Refresh) {
    viewModelScope.launch {
        seriesRepo.refreshDetails(series)
    }
}
```

## Integration

### Upper-layer consumers (presentation)

ViewModels in `ui/*/` depend on these interfaces via constructor injection:

```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val seriesRepo: SeriesRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel()
```

They import:
- Repository interfaces from `com.opus.readerparser.domain`
- Domain models from `com.opus.readerparser.domain.model`

ViewModels **never** import `SourceRegistry`, `Source`, DAO classes, Ktor
types, or any data-layer implementation.

### Lower-layer implementations (data)

Concrete repository implementations live in `data/repository/` and implement
these interfaces:

| Interface | Implementation | Depends on |
|---|---|---|
| `SeriesRepository` | `SeriesRepositoryImpl` | `SourceRegistry`, `SeriesDao`, `ChapterDao` |
| `ChapterRepository` | `ChapterRepositoryImpl` | `SourceRegistry`, `ChapterDao`, `DownloadStore` |
| `SourceRepository` | `SourceRepositoryImpl` | `SourceRegistry` |
| `DownloadRepository` | `DownloadRepositoryImpl` | `DownloadQueueDao` |
| `SettingsRepository` | `SettingsRepositoryImpl` | `DataStore<Preferences>` |

All binding is done in `core/di/RepositoryModule`:

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindSeries(impl: SeriesRepositoryImpl): SeriesRepository
    // ... one @Binds per interface
}
```

### Hard boundary

Files in this package (`domain/`) must not import from:

- `android.*`, `androidx.*` (no Android framework)
- `io.ktor.*` (no networking)
- `org.jsoup.*` (no HTML parsing)
- `androidx.room.*` (no persistence)
- `kotlinx.serialization.*` (no JSON serialization)

Allowed imports: `kotlin.*`, `kotlinx.coroutines.flow.Flow`.

Repository interfaces that need `Flow` return types pull from
`kotlinx.coroutines.flow.Flow`, which is a pure-Kotlin coroutines type
(no Android dependency).
