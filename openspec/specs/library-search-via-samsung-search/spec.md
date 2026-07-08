# library-search-via-samsung-search Specification

## Purpose
TBD - created by archiving change use-samsung-library-search. Update Purpose after archive.
## Requirements
### Requirement: Active Library search uses Samsung Search query
When the user enters a non-blank Library search query, ReaderParser SHALL query Samsung Search's public ContentProvider at `content://com.samsung.android.smartsuggestions.search/v2/com.opus.readerparser.series` using `ContentResolver.query()` with a projection that includes at least `_id`, `title`, and `source_url`.

#### Scenario: Non-blank query uses provider results
- **WHEN** the Library screen receives a non-blank search query
- **THEN** ReaderParser SHALL query Samsung Search instead of filtering titles in memory

#### Scenario: Blank query keeps local library view
- **WHEN** the Library search query is blank
- **THEN** ReaderParser SHALL continue showing the normal observed library list

### Requirement: Library search resolves local rows and preserves provider ordering
ReaderParser SHALL resolve Samsung Search hits back to local Room series rows before displaying them. The UI SHALL show the local row's canonical title and cover, and the displayed results SHALL keep Samsung Search's ordering.

#### Scenario: Local display data is used
- **WHEN** a Samsung Search hit maps to a local series row with a different stored display title than the provider row
- **THEN** ReaderParser SHALL display the local row's title and cover

#### Scenario: Provider order is preserved
- **WHEN** Samsung Search returns hits in relevance order
- **THEN** ReaderParser SHALL display the results in that same order

### Requirement: Search results are limited to downloadable library rows
ReaderParser SHALL only display search hits that resolve to series rows that are both in the user's library and still indexable by downloaded chapters.

#### Scenario: Non-library hit is hidden
- **WHEN** Samsung Search returns a hit for a series that is not marked in-library
- **THEN** ReaderParser SHALL not display that hit in Library search results

#### Scenario: Non-indexable hit is hidden
- **WHEN** Samsung Search returns a hit for a series with no downloaded chapters
- **THEN** ReaderParser SHALL not display that hit in Library search results

### Requirement: Active search revalidates on library and indexable changes
When the underlying library membership or downloaded-chapter indexability changes, ReaderParser SHALL rerun the active non-blank Samsung search so stale hits disappear from the Library screen.

#### Scenario: Search hit disappears after indexability changes
- **WHEN** a series loses its last downloaded chapter while a non-blank Library search is active
- **THEN** ReaderParser SHALL rerun the Samsung Search query and remove the now-non-indexable hit from the results

### Requirement: Provider failure is distinct from empty results
If Samsung Search query execution fails, ReaderParser SHALL expose that as a search error state rather than an empty-result state.

#### Scenario: Provider failure shows an error
- **WHEN** the Samsung Search query throws or otherwise fails
- **THEN** ReaderParser SHALL surface a search error instead of a blank empty-results message

#### Scenario: No matches shows empty results
- **WHEN** Samsung Search returns no hits
- **THEN** ReaderParser SHALL show the normal no-matches empty state without an error

