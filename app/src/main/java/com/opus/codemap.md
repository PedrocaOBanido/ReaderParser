# app/src/main/java/com/opus/

## Responsibility

Intermediate package node between the `com` namespace root and the `readerparser` application package. Holds a single subdirectory — `readerparser/` — which contains all of the app's production Kotlin sources. No files at this level.

## Design

- **Single-project namespace.** `com.opus` has only one application (`readerparser`) and no shared libraries.
- This directory exists purely to satisfy the reverse-domain package convention. It has no logical grouping role beyond nesting.

## Flow

- The Kotlin compiler traverses `com/opus/readerparser/` when compiling any `import com.opus.readerparser.*` statement.
- Hilt component generation uses the `com.opus.readerparser` namespace for its generated classes (e.g., `DaggerReaderParser_HiltComponents_SingletonC`).

## Integration

- Links the `com.opus` org namespace to the concrete app package.
- Future `com.opus.*` packages (if any) would be siblings of `readerparser/`.
