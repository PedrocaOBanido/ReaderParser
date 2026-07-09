## A. Browse auto-load-on-scroll

- [x] A.1 Add or adjust a Browse UI/content test that proves scrolling to the end triggers LoadMore automatically (`BrowseContentTest.scrollToEnd_dispatchesLoadMoreAutomatically`; compile + BrowseViewModelTest green, adb BrowseContentTest previously green at 6 tests OK).
- [x] A.2 Keep the manual Load more fallback covered for unsupported/terminal cases (`BrowseContentTest.manualLoadMoreButton_remainsClickable`; compile + BrowseViewModelTest green, adb BrowseContentTest previously green at 6 tests OK).

## B. FreeWebNovel paginated chapter list

- [x] B.1 Add a focused chapter-list test with page 1 + page 2 fixtures/stubs that proves all chapters are aggregated.
- [x] B.2 Update FreeWebNovel chapter-list handling to walk paginated chapter pages without changing the Source contract.
- [x] B.3 Preserve existing chapter-list semantics for sources that already expose all chapters on one page.

## C. AsuraScans latest/popular split regression

- [x] C.1 Add regression proof that AsuraScans latest(1) and popular(1) request distinct URLs.
- [x] C.2 Add regression proof that AsuraScans latest(1) and popular(1) parse distinct results.
- [x] C.3 Change production parser code only if the regression test proves URL/parser drift.

## D. Verify the flow end to end

- [x] D.1 Run targeted tests for Browse UI/ViewModel, FreeWebNovelTest, and AsuraScansTest.
- [x] D.2 Run `:app:testDebugUnitTest` after the targeted runs are clean.
- [x] D.3 Run `:app:assembleDebug` only if a device APK is needed.
