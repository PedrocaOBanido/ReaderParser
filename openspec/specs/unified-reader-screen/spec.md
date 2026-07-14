# unified-reader-screen Specification

## Purpose
TBD - created by archiving change unify-reader-screen. Update Purpose after archive.
## Requirements
### Requirement: Both series types use one Reader destination and screen

NOVEL and MANHWA chapters SHALL open the same Reader navigation destination, screen, ViewModel, and UI state. The destination SHALL carry source ID, series URL, chapter URL, and content type without resolving a concrete source in the ViewModel.

#### Scenario: Novel chapter opens Reader

- **WHEN** the user selects a chapter from a NOVEL series
- **THEN** navigation SHALL open the shared Reader destination with `ContentType.NOVEL`

#### Scenario: Manhwa chapter opens Reader

- **WHEN** the user selects a chapter from a MANHWA series
- **THEN** navigation SHALL open the same Reader destination with `ContentType.MANHWA`

#### Scenario: ViewModel initializes route context

- **WHEN** Reader is created from saved navigation state
- **THEN** it SHALL initialize chapter-list context from source ID, series URL, chapter URL, and content type
- **AND** it SHALL NOT access `SourceRegistry` or a concrete source

### Requirement: Reader renders the existing chapter content shapes

Reader SHALL render `ChapterContent.Text` through the text/HTML renderer and `ChapterContent.Pages` through the vertical image-page renderer. `ChapterContent` SHALL remain sealed with exactly those two variants.

#### Scenario: Text content loads

- **WHEN** `ChapterRepository` returns `ChapterContent.Text`
- **THEN** Reader SHALL display its HTML in the JavaScript-disabled text renderer

#### Scenario: Page content loads

- **WHEN** `ChapterRepository` returns `ChapterContent.Pages`
- **THEN** Reader SHALL display its image URLs in reading order through the page renderer

#### Scenario: Route type and content disagree

- **WHEN** initially loaded content does not match the route content type
- **THEN** Reader SHALL perform one forced network fetch that bypasses downloaded content
- **AND** it SHALL NOT render the mismatched payload

#### Scenario: Forced content still disagrees

- **WHEN** the forced network response still does not match the route content type
- **THEN** Reader SHALL display a retryable unexpected-content error
- **AND** it SHALL NOT render the mismatched payload

### Requirement: Reader provides shared immersive controls

Both content renderers SHALL use the same tap-to-toggle overlay chrome. Shared controls SHALL provide Back, previous chapter, next chapter, chapter list, download, and content-appropriate progress.

#### Scenario: Reader content is tapped

- **WHEN** the user taps the reading surface
- **THEN** the top and bottom controls SHALL toggle visibility

#### Scenario: Text progress is displayed

- **WHEN** text content is visible
- **THEN** shared chrome SHALL display normalized scroll progress as a percentage

#### Scenario: Page progress is displayed

- **WHEN** page content is visible
- **THEN** shared chrome SHALL display the current page and total page count

#### Scenario: Back is selected

- **WHEN** the user selects Back in Reader chrome
- **THEN** Reader SHALL invoke navigation back

### Requirement: Reader preserves content-specific progress and read semantics

Reader SHALL clamp text scroll progress to `[0f, 1f]`, persist each distinct renderer-reported value by `(sourceId, chapterUrl)`, restore the stored value after text layout, and mark text content read immediately after successful display. Page content SHALL start at page zero for each load, keep page position only in current Reader state, and mark the chapter read when the final page becomes current.

#### Scenario: Text scroll position changes

- **WHEN** the text renderer reports normalized progress
- **THEN** Reader SHALL clamp and persist a distinct value by `(sourceId, chapterUrl)`

#### Scenario: Stored text progress is loaded

- **GIVEN** a text chapter has stored progress
- **WHEN** its WebView content finishes layout
- **THEN** Reader SHALL restore the scroll position corresponding to that normalized progress

#### Scenario: Text content loads successfully

- **WHEN** a text chapter is displayed successfully
- **THEN** Reader SHALL mark that chapter read immediately

#### Scenario: Final image page becomes current

- **WHEN** the page renderer reports the final page as current
- **THEN** Reader SHALL mark the chapter read

#### Scenario: Earlier image page becomes current

- **WHEN** the current page is not the final page
- **THEN** Reader SHALL NOT mark the chapter read solely from that page change

#### Scenario: Page chapter loads again

- **WHEN** a page chapter is newly loaded or reloaded
- **THEN** its current page SHALL start at zero
- **AND** this change SHALL NOT restore or persist page position

### Requirement: Reader supports chapter actions and recovery

Reader SHALL preserve previous/next navigation, chapter-sheet selection, and download enqueue behavior for both content types. A load error SHALL expose Retry, and Retry SHALL reload the current chapter without adding another navigation destination.

#### Scenario: Adjacent chapter is selected

- **WHEN** the user selects an available previous or next chapter
- **THEN** Reader SHALL navigate to that chapter using the same Reader destination and content type

#### Scenario: Chapter is selected from the sheet

- **WHEN** the user selects a different chapter from the shared chapter list
- **THEN** Reader SHALL navigate to that chapter
- **AND** preserve the current source ID, series URL, and content type in the destination

#### Scenario: Download is selected

- **WHEN** the user selects Download for the current chapter
- **THEN** Reader SHALL enqueue that chapter using the existing download flow

#### Scenario: Load fails and Retry is selected

- **WHEN** loading the current chapter fails and the user selects Retry
- **THEN** Reader SHALL reload the same chapter
- **AND** it SHALL retain the current Reader destination

