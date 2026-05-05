# Source contract

- The `Source` interface is stable. Changing it requires explicit human approval.
- `HtmlSource` is the base class for HTML sites. Don't add site-specific logic to it.
- `SourceRegistry` is a `Map<Long, Source>` populated by Hilt. No dynamic loading.
- Source IDs come from `computeSourceId(name, lang, type)`. Never hand-pick.
- Sources throw on error. They do not log, do not catch, do not return null sentinels.
