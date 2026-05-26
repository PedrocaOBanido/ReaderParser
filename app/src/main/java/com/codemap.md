# app/src/main/java/com/

## Responsibility

Convention-level namespace root for Java/Kotlin packages. The sole subdirectory is `opus/`, which corresponds to the `com.opus` organizational namespace. No source files live here; it exists to satisfy the package-to-directory mapping convention.

## Design

- **Standard Java reverse-domain nesting.** The path `com/opus/` maps to the package `com.opus.*`.
- Only one child: `opus/`. No other top-level packages under `com/`.
- No source files — purely structural.

## Flow

- The Kotlin compiler resolves `import com.opus.readerparser.*` by traversing `com/opus/readerparser/` from this root.
- No logic or configuration at this level.

## Integration

- Completes the path from the source root (`java/`) to the app's package root (`com.opus.readerparser/`).
- All Android resources and the manifest reference the full package `com.opus.readerparser` via the `namespace` in `build.gradle.kts`.
