# Download Offline Reader Delta

## MODIFIED Requirements

### Requirement: Readers prefer local content

The unified Reader SHALL check `DownloadStore` for a locally cached copy of the chapter before fetching from the network, regardless of whether the route content type is NOVEL or MANHWA. If a local copy exists and matches the route content type, it SHALL be returned immediately without a network call. A non-null downloaded payload with the wrong `ChapterContent` variant SHALL be treated as invalid for that route and SHALL trigger one forced network fetch that bypasses the downloaded payload.

#### Scenario: Reader serves cached novel chapter

- **WHEN** Reader loads a NOVEL chapter that has been previously downloaded
- **THEN** `DownloadStore.read(chapter)` SHALL be called
- **AND** if it returns matching `ChapterContent.Text`, that content SHALL be displayed without a network request

#### Scenario: Reader serves cached manhwa chapter

- **WHEN** Reader loads a MANHWA chapter that has been previously downloaded
- **THEN** `DownloadStore.read(chapter)` SHALL be called
- **AND** if it returns matching `ChapterContent.Pages`, those pages SHALL be displayed without a network request

#### Scenario: Reader falls back to network when not downloaded

- **WHEN** Reader loads a chapter and `DownloadStore.read(chapter)` returns null
- **THEN** Reader SHALL fall back to fetching content through `chapterRepository.getContent(chapter)`

#### Scenario: Downloaded content variant mismatches the route

- **WHEN** `DownloadStore.read(chapter)` returns a non-null content variant that does not match the Reader route content type
- **THEN** Reader SHALL NOT display the downloaded payload
- **AND** it SHALL perform one forced network fetch for that chapter

#### Scenario: Forced response remains mismatched

- **WHEN** the forced network fetch also returns a content variant that does not match the Reader route content type
- **THEN** Reader SHALL expose a retryable unexpected-content error
- **AND** it SHALL NOT loop forced network requests automatically
