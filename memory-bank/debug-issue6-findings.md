# Debug Findings: Issue #6 — "Unexpected content type" (ongoing)

**Date**: 2026-05-10  
**Status**: Partially fixed. Remaining bugs identified. Root cause of continued
user report unresolved via static analysis alone.

---

## What was fixed (commit 71631a3)

`HtmlSource.getSeriesDetails` was passing the stub series (built with
`type = ContentType.NOVEL` hardcoded in `SeriesViewModel.stubSeries`) directly
into `seriesDetailsParse`. The concrete `AsuraScans.seriesDetailsParse` did
`series.copy(author=..., ...)` without overriding `type`, so the returned
series kept `type=NOVEL`. `_state.series.type == NOVEL` → routed to
`NovelReaderScreen` → expects `ChapterContent.Text`, receives
`ChapterContent.Pages` from the MANHWA source → "Unexpected content type".

**Fix applied**:
```kotlin
// HtmlSource.kt line 161-162
override suspend fun getSeriesDetails(series: Series): Series =
    seriesDetailsParse(fetchDoc(series.url), series.copy(type = type))
//                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^
//                                           type = this.type = MANHWA for AsuraScans
```

**Regression test added** in `HtmlSourceTest.kt`: verifies that a stub with
`type=NOVEL` passed to `getSeriesDetails` of a MANHWA source comes back
with `type=MANHWA`.

---

## Why the user says "bug still happens" — analysis

### Exhaustive static analysis of all code paths

The fix is correctly applied. Tracing the full runtime path after the fix:

1. `SeriesViewModel.init` → `refresh()` → `seriesRepository.refreshDetails(stubSeries)`
   - `stubSeries.type == NOVEL` (still hardcoded in ViewModel, line 44)
2. `SeriesRepositoryImpl.refreshDetails(series)` → `sourceRegistry[series.sourceId].getSeriesDetails(series)`
3. `HtmlSource.getSeriesDetails(series)` → `seriesDetailsParse(fetchDoc(url), series.copy(type = type))`
   - `this.type` for `AsuraScans` = `ContentType.MANHWA`
   - `series.copy(type = MANHWA)` is what `seriesDetailsParse` receives
4. `AsuraScans.seriesDetailsParse(doc, series)` → `series.copy(author=..., ...)` (no `type` override)
   - Returns series with `type=MANHWA` ✓
5. `refreshDetails` returns `updated` with `type=MANHWA`
6. `_state.update { it.copy(series = updated, ...) }` → `_state.series.type == MANHWA` ✓
7. User taps chapter → `onAction(OpenChapter)` → `val type = MANHWA` → `NavigateToReader(chapter, MANHWA)` ✓
8. `SeriesScreen` routes to `onNavigateToMangaReader(...)` ✓
9. `MangaReaderViewModel.getContent(chapter)` → `AsuraScans.getChapterContent(chapter)` branches on
   `this.type = MANHWA` → `chapterPagesParse(doc)` → returns `ChapterContent.Pages` ✓

**Conclusion: no code path produces "Unexpected content type" for AsuraScans after the fix.**

### Possible explanations for continued user report

1. **Stale APK**: User tested against an old build that doesn't include commit 71631a3.
2. **DB state from pre-fix visits**: Old series rows in DB with `type="NOVEL"`. HOWEVER, `refreshDetails`
   doesn't read from DB — it always calls the source. The fix ensures the source returns MANHWA type
   regardless of DB state.
3. **Different bug manifestation**: The user may be experiencing a DIFFERENT bug (see below) and
   misidentifying it as the same issue.
4. **Race condition**: `observe()` and `refresh()` run concurrently. If the user taps a chapter
   BEFORE `_state.series` is set (while refresh is in progress), `_state.series` is null and
   `onAction(OpenChapter)` returns early without navigating. This is a UX bug (silent no-op)
   but NOT "Unexpected content type".

---

## Remaining real bugs (found during investigation)

### Bug A — `refreshDetails` CASCADE-deletes chapters (HIGH severity)

**Same root cause as issue #5, but in a different method.**

`SeriesRepositoryImpl.refreshDetails` calls:
```kotlin
seriesDao.upsert(updated.toEntity())
```

`seriesDao.upsert` uses `OnConflictStrategy.REPLACE`, which in SQLite performs
`DELETE + INSERT` when the primary key already exists. The FK constraint on
`ChapterEntity`:
```kotlin
onDelete = ForeignKey.CASCADE
```
…silently deletes ALL chapters for the series whenever `refreshDetails` is called,
i.e., every time the user opens a series screen. `refreshChapters` then
re-fetches from network. If the network call fails, chapters remain deleted.

This was already fixed for `addToLibrary` in commit 72aed3c (switched to an
UPDATE-only DAO query). The same fix is needed in `refreshDetails`.

**Fix needed in `SeriesRepositoryImpl.refreshDetails`**:
```kotlin
// CURRENT (broken):
override suspend fun refreshDetails(series: Series): Series {
    val updated = sourceRegistry[series.sourceId].getSeriesDetails(series)
    seriesDao.upsert(updated.toEntity())   // ← DELETE+INSERT, wipes chapters
    return updated
}

// FIX OPTION: add a targeted upsert that preserves chapters
// Need a new DAO method, e.g.:
@Query("""UPDATE series SET title=:title, author=:author, artist=:artist,
          description=:description, coverUrl=:coverUrl, genresJson=:genresJson,
          status=:status, type=:type
          WHERE sourceId=:sourceId AND url=:url""")
suspend fun updateDetails(
    sourceId: Long, url: String, title: String, author: String?, artist: String?,
    description: String?, coverUrl: String?, genresJson: String, status: String, type: String
)

// If the row doesn't exist yet, fall back to INSERT:
// Use @Upsert (Room 2.5+) or a pair of INSERT IGNORE + UPDATE.
```

### Bug B — `Series.toEntity()` hardcodes `inLibrary = false` (HIGH severity)

`SeriesMappers.kt`:
```kotlin
fun Series.toEntity(): SeriesEntity = SeriesEntity(
    ...
    inLibrary = false,   // ← always resets library membership
    addedAt = null,
)
```

This means:
- Every `seriesDao.upsert(series.toEntity())` resets `inLibrary=false` in the DB.
- The series is silently removed from the library each time the user visits the series screen.

**Fix**: `toEntity()` should NOT set `inLibrary`/`addedAt`. Those fields are managed by
dedicated DAO queries (`addToLibrary`, `removeFromLibrary`). The mapper should either:
- Accept an optional `inLibrary` param (not clean), OR
- Use a separate DAO method that only updates the content fields (preferred — see Bug A fix).

### Bug C — `SeriesViewModel.refresh()` hardcodes `inLibrary = false` (MEDIUM severity)

```kotlin
// SeriesViewModel.kt line 89
_state.update { it.copy(series = updated, inLibrary = false, isLoading = false) }
//                                        ^^^^^^^^^^^^^^^^^^ always false
```

The UI always shows "Add to Library" button even when the series IS in the library.
The correct value should come from the `updated` series object or from a fresh DB query.

**Fix**: after `refreshDetails`, query the DB for the `inLibrary` flag and use it:
```kotlin
val inLibrary = seriesRepository.isInLibrary(series.sourceId, series.url)
_state.update { it.copy(series = updated, inLibrary = inLibrary, isLoading = false) }
```
Or add `inLibrary: Boolean` to the `Series` domain model (architectural decision needed).

### Bug D — Test coverage gap: no MANHWA routing test (LOW severity)

`SeriesViewModelTest` only tests NOVEL type routing:
```kotlin
private val series = TestFixtures.testSeries() // type = ContentType.NOVEL
seriesRepo.refreshDetailsResult = { series }   // fake always returns NOVEL-typed series
// The OpenChapter test asserts effect.type == series.type == NOVEL
```

If `refreshDetails` somehow returned NOVEL type, this test wouldn't catch it.

**Fix needed**: add a MANHWA routing test:
```kotlin
@Test
fun `OpenChapter routes to MANHWA reader when series type is MANHWA`() = runTest {
    val manhwaSeries = TestFixtures.testSeries(type = ContentType.MANHWA)
    seriesRepo.refreshDetailsResult = { manhwaSeries }
    val vmManhwa = SeriesViewModel(savedState, seriesRepo, chapterRepo)
    vmManhwa.effects.test {
        vmManhwa.onAction(SeriesAction.OpenChapter(chapter))
        val effect = awaitItem() as SeriesEffect.NavigateToReader
        assertThat(effect.type).isEqualTo(ContentType.MANHWA)
    }
}
```

---

## Files to change for remaining bugs

| File | Change |
|------|--------|
| `data/local/database/dao/SeriesDao.kt` | Add `updateDetails(...)` query that does NOT touch `inLibrary`/`addedAt`/chapters |
| `data/local/database/mappers/SeriesMappers.kt` | Remove `inLibrary = false, addedAt = null` from `toEntity()` or deprecate that path |
| `data/repository/SeriesRepositoryImpl.kt` | Replace `seriesDao.upsert(updated.toEntity())` in `refreshDetails` with non-destructive update |
| `ui/series/SeriesViewModel.kt` | Fix hardcoded `inLibrary = false` in `refresh()` |
| `ui/series/SeriesViewModelTest.kt` | Add MANHWA routing test |
| `data/repository/SeriesRepositoryImplTest.kt` | Add test proving `refreshDetails` does NOT delete chapters |

---

## Architecture notes (for reference)

- **`SeriesViewModel.stubSeries`** always has `type=ContentType.NOVEL` (line 44). This is intentional
  (the ViewModel doesn't know the type until `refreshDetails` returns). The HtmlSource fix ensures
  the type is normalized by the source before propagating.
- **`getChapterContent`** branches on `this.type` (the SOURCE's type), not the chapter's or series'
  type. For AsuraScans (MANHWA), this always calls `chapterPagesParse` → always returns `Pages`.
  Cannot produce "Unexpected content type" in MangaReaderScreen for AsuraScans.
- **`computeSourceId`** includes the ContentType in its hash: `"$name/$lang/${type.name}"`. Changing
  the type would change the source ID and break FK references. Do NOT change AsuraScans' type.
- Only one source is registered: `AsuraScans(client)` in `SourceModule.kt`.

---

## Next steps for the next agent

1. **Verify the fix**: Build a debug APK from commit 71631a3, install fresh on device/emulator,
   browse to an AsuraScans manhwa series, tap a chapter. It should open in MangaReaderScreen
   without "Unexpected content type". If it still fails, attach a logcat to identify WHICH
   ViewModel and WHICH line produces the error.

2. **Fix Bug A + B together**: Implement a non-destructive `updateDetails` DAO method that patches
   only content columns (title, author, artist, description, coverUrl, genresJson, status, type)
   without touching `inLibrary`, `addedAt`, or child rows. Update `refreshDetails` to use it.
   Handle the "row doesn't exist" case (first visit: INSERT; subsequent visits: UPDATE only).
   Write an instrumented test proving chapters survive a `refreshDetails` call.

3. **Fix Bug C**: Fix the hardcoded `inLibrary = false` in `SeriesViewModel.refresh()`.
   May require adding `isInLibrary(sourceId, url): Boolean` to `SeriesRepository` and
   `SeriesDao`, or adding `inLibrary` to the `Series` domain model.

4. **Fix Bug D**: Add MANHWA type routing test to `SeriesViewModelTest`.

5. Run full CI check: `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`.
