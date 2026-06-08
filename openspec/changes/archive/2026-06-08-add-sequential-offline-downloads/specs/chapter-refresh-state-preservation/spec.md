## ADDED Requirements

### Requirement: refreshChapters preserves chapter state

When `ChapterRepository.refreshChapters(series)` fetches the remote chapter
list and upserts it into the local database, the system SHALL preserve the
existing `read`, `progress`, and `downloaded` values for chapters that already
exist locally.

#### Scenario: Refresh preserves read state
- **WHEN** `refreshChapters` is called for a series
- **AND** a chapter already exists locally with `read = true`
- **THEN** after refresh, the chapter SHALL still have `read = true`

#### Scenario: Refresh preserves progress
- **WHEN** `refreshChapters` is called for a series
- **AND** a chapter already exists locally with `progress = 0.75`
- **THEN** after refresh, the chapter SHALL still have `progress = 0.75`

#### Scenario: Refresh preserves downloaded flag
- **WHEN** `refreshChapters` is called for a series
- **AND** a chapter already exists locally with `downloaded = true`
- **THEN** after refresh, the chapter SHALL still have `downloaded = true`

#### Scenario: New chapters get default state
- **WHEN** `refreshChapters` is called for a series
- **AND** a chapter in the remote list does not exist locally
- **THEN** the new chapter SHALL be inserted with `read = false`,
  `progress = 0f`, `downloaded = false`

#### Scenario: Removed chapters are cleaned up
- **WHEN** `refreshChapters` is called for a series
- **AND** a chapter that existed locally is no longer in the remote list
- **THEN** the stale chapter row SHALL be removed from the local database
