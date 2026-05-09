---
description: Scaffold a new Source plugin from a template.
argument-hint: <SiteName> <baseUrl> <NOVEL|MANHWA> [lang]
---

Dispatch the `source-author` agent via the `Agent` tool with
`subagent_type: "source-author"` and the following brief:

> Create a new source plugin for the site named `$1` (PascalCase) at base
> URL `$2`, content type `$3` (NOVEL or MANHWA), language `$4` (ISO 639-1
> code, default "en" if not given).
>
> Required steps:
>
> 1. Create directory `app/src/main/kotlin/com/opus/readerparser/sources/${1,,}/`
>    (lowercase the package).
> 2. Create `$1.kt` extending `HtmlSource`. Override only the methods
>    listed in `app/src/main/kotlin/com/opus/readerparser/sources/AGENTS.md`.
>    Leave the non-applicable content method as `error("...")`.
> 3. Compute `id` via `computeSourceId("$1", "$4", ContentType.$3)`.
> 4. Add the source to the `SourceModule` provider in
>    `app/src/main/kotlin/com/opus/readerparser/core/di/SourceModule.kt`.
> 5. Create test scaffolding under
>    `app/src/test/kotlin/.../sources/${1,,}/` with placeholder fixtures
>    in `app/src/test/resources/fixtures/${1,,}/`.
>
> Stop and ask the user for the CSS selectors before filling in parser
> logic. Do not invent selectors.
