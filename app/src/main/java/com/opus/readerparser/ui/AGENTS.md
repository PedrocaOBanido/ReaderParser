# UI rules

- Material 3 only. No Material 2 imports.
- Every screen = four files: `*Screen.kt`, `*Content.kt`, `*ViewModel.kt`, `*UiState.kt`.
- `*Screen` wires the ViewModel and collects effects. Never previewed.
- `*Content` is stateless. Always has a `@Preview`.
- Collect state with `collectAsStateWithLifecycle()`. Never `collectAsState`.
- Navigation goes through `Effect`, never `UiState`.
- Hardcoded colors and dp values belong in `ui/theme/`, nowhere else.
- Hoist anything used in 2+ screens into `ui/components/`.
- To debug Compose UI output on a running emulator, load
  `skill("android-cli")` and use `android layout` to inspect the UI tree as
  JSON, or `android screen capture --annotate` for a labeled screenshot.
