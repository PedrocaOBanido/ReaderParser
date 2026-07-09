# source-listing-pagination Specification

## Purpose
TBD - created by archiving change fix-source-listing-pagination. Update Purpose after archive.
## Requirements
### Requirement: Browse results auto-load when scrolled to the end

Browse results MUST automatically request the next page when the user scrolls to the end, while still supporting a manual Load more fallback.

#### Scenario: browse auto-loads next page on end-of-list

- **Given** a browse screen with `hasNextPage=true`
- **When** the list reaches the end and more content is needed
- **Then** the UI triggers the next page load
- **And** the manual Load more fallback remains available when supported

### Requirement: FreeWebNovel chapters are fully aggregated across paginated chapter lists

FreeWebNovel chapter lists MUST collect all chapters even when the source paginates the chapter list internally.

#### Scenario: FreeWebNovel chapter list walks multiple pages

- **Given** a FreeWebNovel series whose chapters span multiple paginated list pages
- **When** the chapter list is requested
- **Then** all chapters across the paginated chapter list are returned
- **And** the Source contract remains unchanged

### Requirement: Supported source listings request and parse the requested page

Source listings that are paged MUST build a page-specific request for each page number and parse the matching page content.

#### Scenario: FreeWebNovel latest uses page-specific requests

- **Given** the FreeWebNovel latest listing is requested for page 1 and page 2
- **When** the source builds and parses both requests
- **Then** page 1 and page 2 use distinct page URLs
- **And** page 2 is parsed from page 2 content, not duplicated page 1 content

#### Scenario: AsuraScans popular and search use page-specific requests

- **Given** the AsuraScans popular or search listing is requested for page 1 and page 2
- **When** the source builds and parses both requests
- **Then** page 2 uses the next page URL for that listing mode
- **And** the parsed results reflect the page 2 fixture/content

#### Scenario: AsuraScans latest and popular remain distinct

- **Given** AsuraScans latest and popular are requested for page 1
- **When** the source builds and parses both requests
- **Then** the requested URLs are distinct
- **And** the parsed results are distinct

### Requirement: Terminal pages disable next-page navigation

When a listing has no further page, the source MUST report that no next page is available.

#### Scenario: End-of-list pages return hasNextPage=false

- **Given** a listing mode and fixture that represent the terminal page
- **When** the source parses that page
- **Then** the result reports `hasNextPage=false`
- **And** the UI has no Load More eligibility for that result

### Requirement: Single-page listings remain single-page

Listings that are documented or observed as single-page MUST not expose a next page.

#### Scenario: FreeWebNovel popular remains single-page

- **Given** FreeWebNovel popular is requested beyond page 1
- **When** the source builds the request and parses the response
- **Then** the listing remains terminal
- **And** no next page is advertised

#### Scenario: FreeWebNovel search remains single-page

- **Given** FreeWebNovel search is exercised with page 1 and page 2
- **When** Slice 1 evidence compares both requests
- **Then** both requests use the same search URL
- **And** both responses parse the same single-page result set
- **And** `hasNextPage=false` for both requests

#### Scenario: FreeWebNovel latest remains terminal at its final page

- **Given** the terminal FreeWebNovel latest page is requested
- **When** the source parses the response
- **Then** the result reports `hasNextPage=false`

#### Scenario: AsuraScans latest remains homepage-only for page > 1

- **Given** AsuraScans latest is requested for page 2
- **When** the source builds the request
- **Then** page > 1 is terminal/non-paged
- **And** no separate page-2 parsing is expected
- **And** `hasNextPage=false` means no Load More eligibility unless product semantics change

