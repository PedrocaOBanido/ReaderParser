# app/src/main/java/com/opus/readerparser/ui/downloads/

## Responsibility

Manages the download queue screen — displays queued, running, completed, and
failed chapter downloads, and allows the user to cancel running/queued items
or retry failed ones.

## Design

Four files following the standard screen pattern:

| File | Role |
|---|---|
| `DownloadsScreen.kt` | Wires `DownloadsViewModel` via `hiltViewModel()`, collects state/effects, delegates to `DownloadsContent`. Never previewed. |
| `DownloadsContent.kt` | Stateless composable — single `@Composable fun DownloadsContent(state, onAction)`. Has `@Preview`s for loading, error, and populated states. |
| `DownloadsUiState.kt` | `data class DownloadsUiState` (downloads list, isLoading, error) + sealed `DownloadsAction` + sealed `DownloadsEffect`. |
| `DownloadsViewModel.kt` | `@HiltViewModel` — injects `DownloadRepository`, exposes `state: StateFlow<DownloadsUiState>` and `effects: Flow<DownloadsEffect>`. |

**State:** `DownloadsUiState` holds `downloads: List<DownloadItem>`, `isLoading`,
and `error: String?`. The list is the only dynamic data; `isLoading` and `error`
are structural (the screen has no explicit refresh — the repository pushes
updates reactively).

**Action:** `DownloadsAction.Cancel(sourceId, chapterUrl)` and
`DownloadsAction.Retry(sourceId, chapterUrl)`. Each maps to the corresponding
`DownloadRepository` method.

**Effect:** `DownloadsEffect.ShowError(message)` — sent when a cancel or retry
call throws. The screen collects this in a `LaunchedEffect` block (currently a
TODO placeholder for snackbar integration).

**Sub-composables:** `DownloadItemRow` renders a `Card` with series title,
chapter name, a `LinearProgressIndicator` (only when `RUNNING`), a `StateBadge`
chip, and Cancel/Retry buttons depending on state. `StateBadge` is a
read-only `SuggestionChip` labelled per `DownloadState`.

## Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ DownloadsScreen                                                 │
│  collectAsStateWithLifecycle → DownloadsContent(state, onAction) │
│  LaunchedEffect { effects.collect { ShowError → snackbar } }    │
└────────────────┬────────────────────────────────────────────────┘
                 │ onAction(Cancel / Retry)
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│ DownloadsViewModel                                              │
│  init { downloadRepository.observeQueue().collect { → state } } │
│  Cancel  → downloadRepository.cancel()    │ catch → ShowError   │
│  Retry   → downloadRepository.retry()     │ catch → ShowError   │
└────────────────┬────────────────────────────────────────────────┘
                 │ observeQueue()
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│ DownloadRepository (domain interface, impl in data/)            │
│  observeQueue(): Flow<List<DownloadItem>>                       │
│  cancel(sourceId, chapterUrl)                                   │
│  retry(sourceId, chapterUrl)                                    │
└─────────────────────────────────────────────────────────────────┘
```

The ViewModel's `init` block launches a coroutine that permanently collects
the `observeQueue()` flow. Every queue change (new download, progress tick,
completion, failure) updates `_state` reactively — no polling, no explicit
refresh action.

Cancel and Retry actions are fire-and-forget from the user's perspective:
the ViewModel calls the repository, and any mutation to the queue is reflected
automatically via the ongoing `observeQueue()` collection. Errors from the
repository call are caught and emitted as one-shot `ShowError` effects.

## Integration

- **Depends on:** `DownloadRepository` (domain interface), `DownloadItem`,
  `DownloadState` (domain models). No knowledge of Room, DataStore, or Ktor.
- **No dependency on `SourceRegistry` or any concrete source** — all source
  interaction is behind the repository.
- The `downloads_list` test tag on `LazyColumn` and `loading` / `error_message`
  tags enable Compose UI testing of the four screen states (loading, error,
  empty, populated).
- The `DownloadsScreen` effect-collection TODO (`ShowError → snackbar`) is the
  planned integration point for a `SnackbarHost`; currently errors only
  appear in the one-shot effect (unobserved until the snackbar is wired).
