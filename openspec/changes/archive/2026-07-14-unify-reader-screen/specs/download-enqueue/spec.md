# Download Enqueue Delta

## MODIFIED Requirements

### Requirement: Enqueue single chapter download

The system SHALL allow the user to enqueue a single chapter for download from the unified Reader screen or the series detail screen. The chapter SHALL be inserted into the download queue with state QUEUED and SHALL be processed by `ChapterDownloadWorker`, independent of whether its content type is NOVEL or MANHWA.

#### Scenario: Enqueue novel from unified Reader

- **WHEN** the user taps Download for a NOVEL chapter in the unified Reader
- **THEN** the chapter SHALL be added to the download queue with state QUEUED
- **AND** a `ChapterDownloadWorker` work request SHALL be enqueued for that chapter

#### Scenario: Enqueue manhwa from unified Reader

- **WHEN** the user taps Download for a MANHWA chapter in the unified Reader
- **THEN** the chapter SHALL be added to the download queue with state QUEUED
- **AND** a `ChapterDownloadWorker` work request SHALL be enqueued for that chapter

#### Scenario: Enqueue from series detail

- **WHEN** the user enqueues one chapter from the series detail screen
- **THEN** the chapter SHALL be added to the download queue with state QUEUED
- **AND** a `ChapterDownloadWorker` work request SHALL be enqueued for that chapter

#### Scenario: Duplicate enqueue is ignored

- **WHEN** the user enqueues a chapter that is already QUEUED or RUNNING
- **THEN** the existing queue entry SHALL be preserved unchanged
- **AND** no duplicate work request SHALL be created
