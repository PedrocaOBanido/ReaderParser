# app/src/main/java/

## Responsibility

Root of the Kotlin source tree for production code. All application logic — UI, domain models, data layer, source plugins, dependency injection, and background workers — lives in the `com.opus.readerparser` package nested below. This directory itself is purely structural: it contains only subdirectories, no source files.

## Design

- **Single-level convention.** The Java/Kotlin source root directly houses the `com/` namespace. Every source file in the app traces through `com/opus/readerparser/`.
- No source files at this level — it is a tree root that the Kotlin compiler traverses to find `com.opus.readerparser.*` packages.

## Flow

- The Kotlin compiler (`org.jetbrains.kotlin.android` plugin) scans this directory and its subdirectories recursively.
- Package declarations in `.kt` files must match the directory structure, enforced by the compiler.
- No special Gradle configuration needed beyond `namespace = "com.opus.readerparser"` in `build.gradle.kts`.

## Integration

- Connects the Android build system to the Kotlin package hierarchy.
- All imports throughout the app are rooted at `com.opus.readerparser.*`.
- JVM test sources in `app/src/test/java/` mirror the same package tree for test classes.
