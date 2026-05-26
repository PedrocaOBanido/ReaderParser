# app/src/main/

## Responsibility

The production source set for the app module. Contains everything shipped in the APK: the `AndroidManifest.xml`, the Kotlin source tree under `java/`, and all Android resources (`res/`: layouts, drawables, strings, themes, and XML backup/config files).

## Design

- **Standard Android source-set structure.** `java/` mirrors the `com.opus.readerparser` package; `res/` follows the standard resource directory convention.
- The manifest declares `INTERNET` permission, the `@HiltAndroidApp`-annotated `App` class, the `@AndroidEntryPoint`-annotated `MainActivity`, and removes the default `WorkManagerInitializer` to let Hilt manage worker DI.
- Resources include launcher icons, a Material 3 theme definition, string resources, backup rules, and data-extraction rules.
- No flavors or build-type source overlays — all variations are in `build.gradle.kts` `buildTypes` blocks.

## Flow

- At app start, `Application.onCreate()` (from `App`) initializes the Hilt component tree, which wires all Hilt modules (`core/di/`).
- `MainActivity.onCreate()` calls `setContent` → `ReaderParserTheme` → `AppNavGraph`, launching the Compose navigation root.
- Room schemas are emitted at compile time to `$projectDir/schemas`, not inside this source set.

## Integration

- `AndroidManifest.xml` references `android:name=".App"` (resolved to `com.opus.readerparser.App`) and `android:name=".MainActivity"`.
- The `@HiltAndroidApp` annotation in `App.kt` triggers Hilt's APT to generate the DI component at this package level.
- Resources defined in `res/` are referenced from Kotlin code via `R.*` and from the manifest via `@resource/` notation.
