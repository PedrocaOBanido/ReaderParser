# Decision log

Chronological record of non-obvious decisions. Newest first.

---

## 2026-04-29 — Phase 0 test dep additions

**Decision:** Added `ksp { arg("room.schemaLocation", ...) }` instead of `room { schemaDirectory(...) }` in `app/build.gradle.kts`.
**Why:** The `room { }` DSL requires the Room Gradle plugin (`id("androidx.room")`), which isn't in the project. The KSP argument approach works with just the KSP processor already applied. Both export schemas to the same directory — the KSP approach is simpler.

**Decision:** Created new version refs only for `turbine` (`1.2.0`) and `truth` (`1.4.4`). Reused existing version refs for `kotlinx-coroutines-test` (`coroutines`), `room-testing` (`room`), `work-testing` (`work`), `ktor-client-mock` (`ktor`).
**Why:** These libraries track the same version as their production counterparts. Separate entries would risk drift.

**Decision:** Test utility classes go in `testutil/`; fakes go in `fakes/`.
**Why:** Clear distinction between infrastructure helpers (`MainDispatcherRule`, `KtorMockHelpers`) and hand-rolled test doubles (`FakeSource`, `FakeSeriesRepository`). `fakes/` is reserved per AGENTS.md §10.

**Decision:** `TestFixtures.kt` (factory functions for test `Series`, `Chapter`, etc.) deferred to Phase 1.
**Why:** It depends on domain model types that don't exist yet. Same for `FakeSource.kt` etc. — all deferred until their interfaces are written.

**Decision:** Invented HTML fixtures approved for Phase 2 source tests.
**Why:** User confirmed invented fixtures are acceptable ("for now invented fixtures should do the trick"). Real site HTML will replace them when a target site is chosen.

## 2026-04-29 — Bootstrap decisions (prior to this session)

**Decision:** Package is `com.opus.readerparser`, not `com.example.reader` as in architecture examples.
**Why:** Android Studio project template default. Architecture examples use `com.example.reader` for illustration only.

**Decision:** Source files placed under `src/main/java/` not `src/main/kotlin/`.
**Why:** Android Studio default. Kotlin compiles from either directory; `java/` is the template default and works identically.

**Decision:** Hilt chosen over Koin for DI.
**Why:** Hilt is listed first in architecture.md; AGENTS.md §13 says replacing Hilt requires pre-approval. Hilt is already wired in build config.

**Decision:** `opencode.json` instructions path is `app/src/main/kotlin/**/AGENTS.md` but actual AGENTS.md files live under `app/src/main/java/`.
**Why:** Likely a typo in the initial scaffold. The path is dead — nested AGENTS.md files are not loaded by opencode. Fixed in this session.
