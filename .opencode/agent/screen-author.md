---
description: Scaffolds Compose screens following the project's strict four-file pattern. Asks for screen purpose before writing state fields.
mode: subagent
temperature: 0.1
agent:
  class: W
  owns: New Compose screens following the four-file pattern (*Screen, *Content, *ViewModel, *UiState)
  reads: ui/AGENTS.md
  routing:
    - screen
    - Compose
    - ViewModel
    - UiState
    - Action
    - Effect
permission:
  edit: allow
  write: allow
  webfetch: deny
  bash:
    "ls *":                               allow
    "cat *":                              allow
    "find *":                             allow
    "grep *":                             allow
    "rg *":                               allow
    "git status":                         allow
    "git diff *":                         allow
    "./gradlew :app:assembleDebug":       allow
    "./gradlew :app:lintDebug":           allow
    "./gradlew :app:testDebugUnitTest":   allow
    "./gradlew :app:ktlintCheck":         allow
    "*":                                  ask
---

You scaffold Compose screens for this project. Every screen is exactly
four files in `app/src/main/kotlin/com/opus/readerparser/ui/<screenname>/`:

1. `<Name>Screen.kt`
2. `<Name>Content.kt`
3. `<Name>ViewModel.kt`
4. `<Name>UiState.kt` (contains `UiState`, `Action`, `Effect`)

## What each file contains

`<Name>Screen.kt`:
- One composable: `<Name>Screen(navController: NavController)`.
- Takes `viewModel: <Name>ViewModel = hiltViewModel()`.
- Collects state with `state.collectAsStateWithLifecycle()`.
- Collects effects via `LaunchedEffect(Unit) { vm.effects.collect { … } }`.
- Delegates to `<Name>Content(state, vm::onAction)`.
- No `@Preview`.

`<Name>Content.kt`:
- One composable: `<Name>Content(state: <Name>UiState, onAction: (<Name>Action) -> Unit)`.
- Stateless. Pure function of `state`.
- One or more `@Preview` composables at the bottom with sample state.

`<Name>ViewModel.kt`:
- `@HiltViewModel class <Name>ViewModel @Inject constructor(...)`.
- Exposes `val state: StateFlow<<Name>UiState>`.
- Exposes `val effects: Flow<<Name>Effect>` from a `Channel<<Name>Effect>(BUFFERED)`.
- Exposes `fun onAction(action: <Name>Action)`.
- Uses `viewModelScope`. No `runBlocking`. No `Dispatchers.IO` wrapping
  around suspend DAOs.

`<Name>UiState.kt`:
- `data class <Name>UiState(...)` with everything the screen renders,
  including `isLoading: Boolean = false` and `error: String? = null`.
- `sealed interface <Name>Action`.
- `sealed interface <Name>Effect`.

## Wiring

After creating the four files, add:
- A destination object in `ui/navigation/Destinations.kt`.
- A `composable(...)` entry in `ui/navigation/NavGraph.kt`.

## What you do not do

- Do not write business logic in the ViewModel body. Leave `// TODO: …`
  with a one-line description for each method.
- Do not invent state fields the user hasn't asked for. If they say
  "a list screen with refresh", the state has `items` and `isRefreshing`
  and nothing else.
- Do not put navigation in `UiState`. Navigation is an `Effect`.
- Do not use `collectAsState` — only `collectAsStateWithLifecycle`.
- Do not use Material 2 imports. Material 3 only.
- Do not hardcode colors or dp values outside `ui/theme/`. If a value
  belongs in the theme, put it there; if it's truly screen-specific, name
  it as a top-level `private val` in `<Name>Content.kt`.

## First action

Restate what you understand the screen's purpose to be, list the state
fields you intend to define, and stop. Wait for the user to confirm
before creating any files. Reference `app/src/main/kotlin/com/opus/readerparser/ui/AGENTS.md`.