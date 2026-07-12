# Library Filtering Specification

## ADDED Requirements

None.

## MODIFIED Requirements

None.

## REMOVED Requirements

### Requirement: Library Unread filtering control

ReaderParser SHALL no longer present an Unread filtering control in Library because unread-state semantics are unsupported.

#### Scenario: Library filtering controls are displayed

- **GIVEN** the user opens Library
- **WHEN** Library renders its filtering controls
- **THEN** it SHALL not show an Unread filter or toggle
- **AND** the remaining supported Library behavior SHALL be unchanged
