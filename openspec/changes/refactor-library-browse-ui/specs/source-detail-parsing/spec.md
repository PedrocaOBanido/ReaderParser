# Source Detail Parsing Specification

## ADDED Requirements

None.

## MODIFIED Requirements

### Requirement: Source detail parsing produces a usable title

Each source detail parser SHALL use the title extracted from its detail page when that title is nonblank. When extraction is blank or unavailable, it SHALL use the incoming series title only when that incoming title is nonblank.

#### Scenario: Extracted title is preferred

- **GIVEN** a source detail page yields a nonblank title and the incoming series has a title
- **WHEN** the source parses the detail page
- **THEN** the parsed series title SHALL be the extracted detail-page title

#### Scenario: Nonblank incoming title is a fallback

- **GIVEN** a source detail page yields no title or a blank title and the incoming series title is nonblank
- **WHEN** the source parses the detail page
- **THEN** the parsed series title SHALL be the incoming title

#### Scenario: Blank incoming title does not become a usable fallback

- **GIVEN** a source detail page yields no title or a blank title and the incoming series title is blank
- **WHEN** the source parses the detail page
- **THEN** the parser SHALL not treat the incoming title as a usable extracted title

## REMOVED Requirements

None.
