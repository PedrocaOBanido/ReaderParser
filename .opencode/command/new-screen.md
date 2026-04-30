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
