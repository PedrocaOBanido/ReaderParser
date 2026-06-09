## Context

The download system currently uses `ChapterDownloadWorker` to download chapters to app-private storage. The worker sets progress to 0f when starting and 1f when completing, with no intermediate updates. The UI already renders a `LinearProgressIndicator` when state is RUNNING, but progress never updates meaningfully.

For manhwa downloads, pages are downloaded sequentially in `DownloadStore.writeManhwa()`, but the worker doesn't report per-page progress. For novels, content is fetched in a single step.

The domain model `DownloadItem` already has `progress: Float` and `state`, and the Room entity `DownloadQueueEntity` also has `progress`. The infrastructure for progress updates exists but isn't being used during download execution.

## Goals / Non-Goals

**Goals:**
- Show meaningful progress updates during chapter downloads
- For manhwa: display per-page progress as percentage (e.g., "45%")
- For novels: display fetch progress (binary: fetching → complete)
- Update UI status text to show percentage
- Maintain existing architecture: domain stays Android-free, repositories own orchestration

**Non-Goals:**
- Redesigning the downloads screen layout
- Adding download speed or ETA estimates
- Changing the download queue or batch download logic
- Modifying the domain model structure (progress field already exists)

## Decisions

### Decision 1: Progress update mechanism

**Choice:** Worker calls `downloadRepository.updateQueueState()` with intermediate progress values.

**Rationale:** 
- Reuses existing infrastructure (`updateQueueState` method already exists)
- Progress flows reactively through DAO → Repository → ViewModel → UI
- No new dependencies or architectural changes needed
- Worker already has access to `downloadRepository`

**Alternatives considered:**
- WorkManager progress data: Would require UI to poll work state instead of using existing reactive flow. More complex and doesn't fit current architecture.
- SharedFlow/StateFlow in worker: Overcomplicates simple progress reporting.

### Decision 2: Progress granularity

**Choice:** 
- Manhwa: Progress = pages downloaded / total pages (e.g., 0.083 for 1/12)
- Novel: Progress = 0.5 after content fetch, 1.0 after write complete

**Rationale:**
- Manhwa downloads are sequential and measurable (page count known upfront)
- Novel downloads are essentially binary (fetch + write)
- Maintains 0.0–1.0 range for consistency

**Alternatives considered:**
- Byte-based progress: Requires tracking total bytes, which varies by source and isn't meaningful for users.
- Time-based estimates: Adds complexity without clear user value.

### Decision 3: UI status text enhancement

**Choice:** Enhance `StateBadge` to show percentage when progress > 0 and < 1.

**Rationale:**
- Minimal UI change (existing `StateBadge` component)
- Shows meaningful status without layout changes
- Consistent with Material 3 design patterns

**Alternatives considered:**
- Separate progress text element: Adds visual clutter without clear benefit.
- Tooltip on hover: Not applicable for touch interfaces.

### Decision 4: Percentage display vs page-count display

**Choice:** Show percentage (e.g., "45%") rather than page-count (e.g., "3/12 pages").

**Rationale:**
- Percentage uses the existing `progress: Float` field with no domain/model/schema changes.
- Page-count display would require carrying `totalPages` and `currentPage` through `DownloadItem` → Room entity → DAO join → ViewModel, adding schema migration cost.
- The per-page `onPageDownloaded` callback still provides exact granularity at the worker level; the UI just renders it as a percentage.
- Consistent across both manhwa and novel content types.

## Risks / Trade-offs

**Risk:** Progress updates may cause excessive database writes if updated too frequently.
→ **Mitigation:** Limit updates to meaningful milestones (per page for manhwa, binary for novel).

**Risk:** Progress values may not be perfectly accurate if download fails mid-progress.
→ **Mitigation:** Existing error handling sets progress to 0f on failure, which is acceptable.

**Risk:** UI may flicker if progress updates are too rapid.
→ **Mitigation:** Updates are per-page (manhwa) or binary (novel), not continuous.

## Migration Plan

No migration needed. The existing infrastructure supports intermediate progress updates. The change is purely additive: worker updates progress more frequently, UI displays it.

## Open Questions

None. The implementation path is clear:
1. Update `ChapterDownloadWorker` to call `updateQueueState` with intermediate progress
2. Enhance `StateBadge` to show percentage when appropriate
3. Verify progress flows correctly through the reactive chain
