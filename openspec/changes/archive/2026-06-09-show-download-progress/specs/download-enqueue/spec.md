## MODIFIED Requirements

### Requirement: Download progress visibility

The Downloads screen SHALL display the current state (QUEUED, RUNNING,
COMPLETED, FAILED) and progress (0.0–1.0) for each enqueued chapter, along
with the series title and chapter name. When state is RUNNING and progress
is greater than 0, the status badge SHALL show the progress percentage.

#### Scenario: Observe queue updates
- **WHEN** the download queue changes (item added, state updated, item
  removed)
- **THEN** the Downloads screen SHALL reflect the updated queue in real time

#### Scenario: Progress percentage display
- **WHEN** a download item has state RUNNING and progress > 0
- **THEN** the status badge SHALL show the progress percentage (e.g., "45%")
