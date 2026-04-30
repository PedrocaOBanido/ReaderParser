# Conventions

Key rules distilled from `AGENTS.md`. See the source for full detail and rationale.

## Non-negotiables (must follow)

1. Domain layer has **zero Android dependencies** — no `androidx.*`, `android.*`, `io.ktor.*`, `androidx.room.*`, `androidx.compose.*`
2. Ktor calls **only** inside `Source` implementations or repositories
3. ViewModels **never** reference `SourceRegistry` or any concrete `Source`
4. Novel reader and manhwa reader are **separate screens** — share only navigation actions and `ChapterContent`
5. `ChapterContent` is a **sealed interface** with exactly two variants: `Text(html)` and `Pages(imageUrls)`
6. Domain models are **immutable `data class`es** — no `var`, no mutable collections
7. **No `runBlocking`** in production code (tests only)
8. Compose previews target **`*Content`** composables, not `*Screen`
9. Series/chapter identity is **`(sourceId, url)`** — no auto-increment IDs
10. Downloads go to **app-private storage** (`context.filesDir`)

## File placement

| Adding... | Goes in |
|---|---|
| New site plugin | `sources/<sitename>/<SiteName>.kt` |
| Domain model | `domain/model/` |
| Use case | `domain/usecase/` |
| Repository interface | `domain/` (interface) + `data/repository/` (impl) |
| Room entity, DAO, migration | `data/local/database/` (dao/, entities/, mappers/, migrations/) |
| File-system helper | `data/local/filesystem/` |
| DataStore prefs | `data/local/prefs/` |
| Ktor / JSON config | `data/network/` |
| `Source` interface, `HtmlSource` | `data/source/` |
| Hilt module | `core/di/` |
| Generic util / extension | `core/util/` |
| New screen | 4 files: `ui/<screen>/<Screen>Screen.kt`, `*Content.kt`, `*ViewModel.kt`, `*UiState.kt` |
| Reusable composable | `ui/components/` |
| Theme tokens | `ui/theme/` |
| Background worker | `workers/` |

## State management pattern (each screen)

```kotlin
val state: StateFlow<XUiState>          // single source of truth
val effects: Flow<XEffect>              // one-shot via Channel(BUFFERED)
fun onAction(action: XAction)           // single entry point for UI events
```

- `UiState` is a single `data class` with everything, including `isLoading` and `error`
- `Action` is a sealed interface — every UI event maps to exactly one action
- `Effect` is a sealed interface via `Channel<Effect>(BUFFERED)` → `receiveAsFlow()`
- Navigation and toasts go in effects, not in `UiState`
- Collect state: `collectAsStateWithLifecycle()`, never `collectAsState`
- Effects collected in `LaunchedEffect(Unit)` inside `*Screen`, never inside `*Content`

## Compose

- Material 3 only. No Material 2 imports.
- Theme tokens via `MaterialTheme.colorScheme` / `.typography`. No hardcoded colors/sizes.
- `*Screen` takes `viewModel = hiltViewModel()` and delegates to `*Content(state, onAction)`
- Lists: `LazyColumn` / `LazyVerticalGrid` with stable `key`
- Images: Coil 3 `AsyncImage` with `placeholder` and `error`
- No business logic in composables

## Coroutines

- ViewModel work: `viewModelScope`
- DB work: Room suspend DAOs (already on `Dispatchers.IO` — don't wrap)
- File I/O: `Dispatchers.IO`
- Network: Ktor OkHttp engine (on `Dispatchers.IO`)
- `stateIn()`: use `SharingStarted.WhileSubscribed(5_000)`, not `Eagerly` or `Lazily`

## Errors

- Repositories let exceptions propagate. No `Result<T>` wrapping unless recovery path exists.
- ViewModels catch at boundary → map to `UiState.error` + emit `ShowError` effect
- Background refresh failures go to `UiState` only
- Sources throw — do not log, do not catch broadly, do not return null sentinels
- Network errors, parse errors, storage errors distinguishable in logs

## Sources

- Extend `HtmlSource`, not `Source` directly (unless JSON-based)
- `id` via `computeSourceId(name, lang, type)`, never hand-picked
- Override `chapterTextParse` for novels **or** `chapterPagesParse` for manhwa, never both
- Register in `core/di/SourceModule.kt`
- Use `selectFirst` + null-safety for optional elements; always `.trim()` text nodes; always `absUrl(...)` for URLs

## Database

- Bump `AppDatabase.version` on any schema change
- Write explicit `Migration` — no `fallbackToDestructiveMigration`
- `exportSchema = true` — schemas in `app/schemas/`, checked in
- Update mapper between entity and domain model

## Style

- `kotlin.code.style=official`; 4-space indent; 120-col soft, 140 hard
- No wildcard imports, no unused imports
- Public APIs: KDoc on type + non-obvious members
- `camelCase` functions; `PascalCase` composables; `UPPER_SNAKE` constants
- No abbreviations except established ones (`url`, `id`, `db`, `vm`)
