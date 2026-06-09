## ADDED Requirements

### Requirement: Enqueue single chapter download

The system SHALL allow the user to enqueue a single chapter for download from
any reader screen or the series detail screen. The chapter SHALL be inserted
into the download queue with state QUEUED and SHALL be processed by
`ChapterDownloadWorker`.

#### Scenario: Enqueue from novel reader
- **WHEN** the user taps "Download" on a chapter in the novel reader
- **THEN** the chapter is added to the download queue with state QUEUED
- **AND** a `ChapterDownloadWorker` work request is enqueued for that chapter

#### Scenario: Enqueue from manhwa reader
- **WHEN** the user taps "Download" on a chapter in the manhwa reader
- **THEN** the chapter is added to the download queue with state QUEUED
- **AND** a `ChapterDownloadWorker` work request is enqueued for that chapter

#### Scenario: Duplicate enqueue is ignored
- **WHEN** the user enqueues a chapter that is already QUEUED or RUNNING
- **THEN** the existing queue entry is preserved unchanged
- **AND** no duplicate work request is created

### Requirement: Enqueue batch download for a series

The system SHALL allow the user to enqueue a batch download for multiple
chapters of a series. The batch SHALL support two modes: unread-only (all
chapters not yet marked read) and user-specified range (start and end chapter
inclusive).

#### Scenario: Enqueue unread-only batch
- **WHEN** the user selects "Download unread" on the series detail screen
- **THEN** all chapters in the series where `read = false` SHALL be added to
  the download queue in chapter-number order
- **AND** each chapter SHALL have a `ChapterDownloadWorker` work request
  enqueued

#### Scenario: Enqueue range batch
- **WHEN** the user selects a start chapter and end chapter on the series
  detail screen and confirms download
- **THEN** all chapters from start through end (inclusive, ordered by chapter
  number) SHALL be added to the download queue
- **AND** each chapter SHALL have a `ChapterDownloadWorker` work request
  enqueued

#### Scenario: Empty batch
- **WHEN** the user selects "Download unread" and all chapters are already
  read
- **THEN** no items are added to the queue
- **AND** the user sees a brief message indicating nothing to download

### Requirement: Sequential execution

Downloads SHALL execute one chapter at a time. WorkManager SHALL process
`ChapterDownloadWorker` requests sequentially; the next chapter download SHALL
not start until the previous one completes, fails permanently, or is cancelled.

#### Scenario: Sequential processing
- **WHEN** multiple chapters are enqueued as a batch
- **THEN** only one `ChapterDownloadWorker` SHALL execute at a time
- **AND** the next chapter's worker SHALL start after the current one finishes

### Requirement: Cancel batch download

The system SHALL allow the user to cancel an in-progress batch download.
Canceling SHALL remove all QUEUED items for that batch from the download queue
and SHALL cancel the active worker if it belongs to that batch.

#### Scenario: Cancel active batch
- **WHEN** the user cancels a batch while a chapter from that batch is RUNNING
- **THEN** the RUNNING chapter's worker SHALL be cancelled
- **AND** all QUEUED chapters belonging to the same batch SHALL be removed
  from the download queue

#### Scenario: Cancel batch with only queued items
- **WHEN** the user cancels a batch and no chapter from that batch is
  currently RUNNING
- **THEN** all QUEUED chapters belonging to that batch SHALL be removed from
  the download queue

### Requirement: Download progress visibility

The Downloads screen SHALL display the current state (QUEUED, RUNNING,
COMPLETED, FAILED) and progress (0.0–1.0) for each enqueued chapter, along
with the series title and chapter name.

#### Scenario: Observe queue updates
- **WHEN** the download queue changes (item added, state updated, item
  removed)
- **THEN** the Downloads screen SHALL reflect the updated queue in real time
