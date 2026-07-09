## Context

The app already has the core paging plumbing in BrowseViewModel: it increments pages for load-more and guards on `isLoading` / `hasNextPage`. The missing piece is browse auto-load-on-end behavior in the UI and a few source-side pagination edges.

The change must stay within existing contracts. Domain, Source, repository, and ViewModel interfaces should not change.

## Goals / Non-Goals

**Goals:**

- Add auto-load-on-scroll for browse results while keeping the manual Load more footer as fallback.
- Aggregate all FreeWebNovel chapters across paginated chapter-list pages.
- Prove AsuraScans latest and popular remain distinct at URL and parser level.
- Preserve intentionally single-page modes.
- Add tests that isolate each slice before production changes.

**Non-Goals:**

- Changing Source/repository/ViewModel contracts.
- Adding generic pagination behavior to HtmlSource beyond the existing selector-based model.
- Room/schema, permissions, or dependency changes.

## Decisions

### Decision: keep the scope split into ordered slices

Slice A covers browse auto-load behavior, Slice B covers FreeWebNovel chapter pagination, and Slice C covers the AsuraScans latest/popular regression proof.

### Decision: preserve the prior useful source findings

FreeWebNovel popular/search remain single-page. FreeWebNovel latest is page-aware. AsuraScans popular/search are page-aware. AsuraScans latest remains homepage-only for page > 1.

### Decision: use test fixtures that distinguish requested URLs and page content

Existing tests may return the same HTML for any URL, which can mask pagination bugs. The tests need page-specific fixture data and explicit URL/path assertions so page 1, page 2, and terminal states are all observable.

### Decision: make page-level assertions explicit

| Source / mode | Page 2 expectation |
| --- | --- |
| FreeWebNovel popular | single-page / terminal |
| FreeWebNovel latest | paged, distinct page 2 |
| FreeWebNovel search | single-page / terminal |
| FreeWebNovel chapters | paged until all chapters are collected |
| AsuraScans popular | paged, distinct page 2 |
| AsuraScans search | paged, distinct page 2 |
| AsuraScans latest | single-page / terminal |

## Risks / Trade-offs

- Some modes may turn out to be intentionally single-page, so the change needs evidence before expanding pagination expectations.
- Infinite-scroll UI changes can over-trigger if the viewport sentinel is too eager.
- Chapter pagination needs a bounded fetch loop to avoid duplicates or infinite requests.
- Over-generalizing pagination logic would risk altering intentionally special-case listing behavior.
