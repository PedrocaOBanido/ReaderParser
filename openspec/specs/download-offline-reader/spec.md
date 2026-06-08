## Requirements

### Requirement: Readers prefer local content

Both the novel reader and the manhwa reader SHALL check `DownloadStore` for a
locally cached copy of the chapter before fetching from the network. If a local
copy exists, it SHALL be returned immediately without a network call.

#### Scenario: Novel reader serves cached chapter
- **WHEN** the novel reader loads a chapter that has been previously downloaded
- **THEN** `DownloadStore.read(chapter)` is called
- **AND** if it returns non-null `ChapterContent.Text`, that content is
  displayed without any network request

#### Scenario: Manhwa reader serves cached chapter
- **WHEN** the manhwa reader loads a chapter that has been previously downloaded
- **THEN** `DownloadStore.read(chapter)` is called
- **AND** if it returns non-null `ChapterContent.Pages`, those pages are
  displayed without any network request

#### Scenario: Fallback to network when not downloaded
- **WHEN** a reader loads a chapter and `DownloadStore.read(chapter)` returns
  null
- **THEN** the reader SHALL fall back to fetching content from the source via
  `chapterRepository.getContent(chapter)`

### Requirement: Downloaded indicator on chapters

The chapter list in series detail and reader chapter-list overlays SHALL
indicate which chapters have been downloaded locally.

#### Scenario: Downloaded chapter shown in list
- **WHEN** the chapter list is displayed
- **THEN** chapters where `downloaded = true` SHALL show a visual download
  indicator (e.g., icon or badge)

### Requirement: Delete downloaded chapter

The user SHALL be able to delete a single chapter's downloaded files from the
chapter list or the Downloads screen.

#### Scenario: Delete from Downloads screen
- **WHEN** the user taps "Delete" on a COMPLETED download in the Downloads
  screen
- **THEN** the chapter's files are removed from `DownloadStore`
- **AND** the chapter's `downloaded` flag is set to false
- **AND** the item is removed from the download queue
