# app/

## Responsibility

The single Android application Gradle module. Owns the build configuration (SDK versions, dependencies, signing), the Android manifest, resource assets, Room schema exports, and all production / test source code. After building, it produces the `com.opus.readerparser` APK.

## Design

- **Single-module layout.** All source lives under `app/` — domain, data, UI, sources, workers — within one Gradle module. No multi-module split yet (tracked for when build times grow).
- **Hilt-wired from the top.** `@HiltAndroidApp` on `App`, `@AndroidEntryPoint` on `MainActivity`. Hilt modules live in `core/di/`.
- **WorkManager configured via App** (`Configuration.Provider` + `HiltWorkerFactory`) to enable dependency injection in workers. Default `WorkManagerInitializer` is removed from the manifest via `tools:node="remove"`.
- **Room schema export** enabled via KSP arg `room.schemaLocation=$projectDir/schemas`. Schema JSON files checked into version control.
- **Compiler parameters:** Java 17 source/target, Kotlin Compose plugin via `kotlin.compose`.

## Flow

1. Build entry point: `./gradlew :app:assembleDebug` or `:app:assembleRelease`.
2. `AndroidManifest.xml` declares `INTERNET` permission, the `App` Application subclass, and `MainActivity` as the launcher activity.
3. Room schemas are generated at compile time into `app/schemas/`. Migration tests read them from `androidTest/assets/`.
4. Signing for CI releases reads `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` from environment variables.

## Integration

- Depends on the version catalog defined in `gradle/libs.versions.toml` at the project root.
- Shares Gradle wrapper, settings, and convention plugins with the root project.
- Produces the APK that is installed onto an Android device (minSdk=26, targetSdk=36).
- Artifacts: `app/build/outputs/apk/` (debug/release), `app/schemas/` (Room exports).
