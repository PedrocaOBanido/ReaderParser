# Adaptive Series Catalog UI Specification

## ADDED Requirements

### Requirement: Library and Browse use a shared cover-first series card

Library and Browse SHALL render their series collections with the same reusable cover-first card presentation. The card SHALL show the series cover and title and preserve each surface's existing series interaction.

#### Scenario: Library renders catalog cards

- **GIVEN** Library has series to display
- **WHEN** the Library catalog is rendered
- **THEN** each displayed series SHALL use the shared cover-first card
- **AND** the existing Library series interaction SHALL remain available

#### Scenario: Browse renders catalog cards

- **GIVEN** Browse has search results to display
- **WHEN** the Browse catalog is rendered
- **THEN** each displayed series SHALL use the shared cover-first card
- **AND** selecting a series SHALL retain navigation to its existing detail destination

### Requirement: Catalog layout adapts to available width

Library and Browse SHALL arrange shared series cards in an adaptive grid that remains usable at compact and expanded widths. The cards and grid SHALL use the active Material theme's color, typography, shape, and spacing tokens.

#### Scenario: Compact width remains usable

- **GIVEN** Library or Browse is rendered at a compact width
- **WHEN** its catalog contains series
- **THEN** the adaptive grid SHALL render usable cover-first cards with readable titles and available interactions

#### Scenario: Expanded width uses available space

- **GIVEN** Library or Browse is rendered at an expanded width
- **WHEN** its catalog contains series
- **THEN** the adaptive grid SHALL increase its practical card layout to use the available width

## MODIFIED Requirements

None.

## REMOVED Requirements

None.
