# Library Blank Title Repair Specification

## ADDED Requirements

### Requirement: Library repairs blank bookmark titles through refresh

During a Library screen lifecycle, ReaderParser SHALL attempt to refresh a bookmarked series whose stored title is blank through the existing source-detail refresh and persistence flow. A successful refresh with a repaired title SHALL persist that title while retaining the bookmark's existing data and identity.

#### Scenario: Successful refresh repairs a blank title

- **GIVEN** a bookmarked series has a blank stored title
- **AND** its source detail refresh returns a series with a nonblank title
- **WHEN** Library performs its repair attempt
- **THEN** the refreshed title SHALL be persisted for that bookmark
- **AND** the bookmark identity SHALL remain `(sourceId, url)`

#### Scenario: Failed repair preserves the bookmark

- **GIVEN** a bookmarked series has a blank stored title
- **WHEN** source retrieval, response handling, or detail parsing fails during its repair attempt
- **THEN** ReaderParser SHALL leave the existing bookmark data unchanged

### Requirement: Library limits blank-title repair attempts per lifecycle

Library SHALL make no more than one repair attempt for the same blank bookmark during one Library screen lifecycle.

#### Scenario: Failed repair is not retried in the same lifecycle

- **GIVEN** Library has already attempted to repair a blank bookmark during its current screen lifecycle
- **AND** the bookmark still has a blank title
- **WHEN** Library refreshes or recomposes again in that lifecycle
- **THEN** it SHALL not make another repair request for that bookmark

#### Scenario: A later lifecycle may retry

- **GIVEN** a blank bookmark's repair attempt failed in a previous Library screen lifecycle
- **WHEN** a new Library screen lifecycle begins
- **THEN** Library MAY make one new repair attempt for that bookmark

## MODIFIED Requirements

None.

## REMOVED Requirements

None.
