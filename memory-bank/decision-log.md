# Decision log

Only durable decisions that still affect current work belong here.

## Active decisions

- Package root is `com.opus.readerparser`.
- Production code lives under `app/src/main/java/com/opus/readerparser/`.
- JVM tests live under `app/src/test/kotlin/`; instrumented tests live under
  `app/src/androidTest/java/`.
- Hilt is the active DI framework.
- Core task-start memory is `memory-bank/activeContext.md` plus
  `memory-bank/progress.md`.
- Additional memory files are lazy-loaded by relevance; historical
  investigations and archives stay outside `memory-bank/`.
