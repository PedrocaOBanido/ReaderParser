# ReaderParser architecture vs codemap documentation split plan

Saved for implementation in the next session.

## Objective

Keep `architecture.md` as durable architecture guidance while shifting live
repository structure and implementation mapping responsibility to the codemap
workflow.

## Current problems

- `architecture.md` mixes stable architectural rules with high-churn
  implementation detail.
- Root `codemap.md` already works as a repository atlas, but ownership between
  the two docs is not explicit enough.
- `AGENTS.md` references both documents, but the routing can be sharper so
  future edits do not recreate overlap.
- Long code examples in `architecture.md` will drift faster than codemap output.

## Target model

- `architecture.md` = **normative** document.
  - layering rules
  - contracts
  - invariants
  - architectural decisions
  - high-level runtime/data flow
- `codemap.md` = **descriptive** repository atlas.
  - current entry points
  - directory responsibilities
  - concrete implementation locations
  - links to folder-level maps
- folder `codemap.md` files = **local implementation maps**.
- `AGENTS.md` = **routing policy** that tells humans and agents which document
  to read for which purpose.

## Files in scope

- `AGENTS.md`
- `architecture.md`
- `codemap.md`
- optional folder `codemap.md` files only if cross-links or wording need cleanup

## Out of scope

- changing app architecture
- changing source contracts or runtime behavior
- regenerating all codemaps unless the rewrite exposes stale atlas content

## Rewrite strategy

### Phase 1 — redefine document ownership

Update the top-level documentation contract before touching content.

1. In `AGENTS.md`, keep the existing split but make it explicit:
   - read `architecture.md` for layer rules, contracts, and architectural
     decisions
   - read `codemap.md` for current structure, entry points, and directory maps
   - read a folder's `codemap.md` for local feature/module details
2. In `codemap.md`, keep the root-atlas role and point back to
   `architecture.md` for normative rules.
3. In `architecture.md`, add a short statement near the top that this file does
   not own file-by-file repository mapping.

### Phase 2 — shrink `architecture.md` to durable content

Keep only content that should stay true even if files move or concrete classes
change.

#### Keep

- project purpose and architectural style
- concise tech-stack summary
- layer boundaries and dependency direction
- non-negotiable invariants already aligned with `AGENTS.md`
- contract-level descriptions of the source plugin model
- identity rules such as `(sourceId, url)`
- high-level runtime flow and data flow
- short architectural rationale where it prevents future misuse

#### Remove or compress

- long Kotlin code listings
- worked examples of concrete sources
- concrete DI registration examples that belong to code or codemap
- directory inventories already covered by `codemap.md`
- detailed implementation walkthroughs tied to current class names

#### Replace with

- short prose descriptions
- very small signatures or pseudocode only when needed to define a contract
- links/references to `codemap.md` and folder codemaps for concrete locations

### Phase 3 — strengthen the repository atlas

Review `codemap.md` after the architecture rewrite and ensure it clearly owns
the descriptive surface.

1. Keep the existing sections for root assets, entry points, architecture
   snapshot, runtime flow, and repository directory map.
2. Ensure the “How To Use This Atlas” section explicitly says:
   - start here for codebase navigation
   - use `architecture.md` for rules and decisions
   - use folder codemaps for local implementation details
3. Only regenerate or edit codemaps if the wording is inconsistent with the new
   split.

### Phase 4 — optional decision extraction

If `architecture.md` is still too long after trimming, move historical or
decision-heavy content into a lightweight ADR area such as `docs/adr/` and keep
`architecture.md` as the current authoritative summary.

Do this only if the rewrite still feels crowded.

## Proposed end-state outline

### `architecture.md`

1. Purpose and scope
2. Architectural principles
3. Layer model and dependency rules
4. Core contracts and invariants
5. Source plugin model
6. Persistence and ownership boundaries
7. High-level runtime/data flow
8. Key architectural decisions and trade-offs
9. How this doc relates to `codemap.md`

### `codemap.md`

Keep the current atlas shape:

1. Project responsibility
2. Root assets
3. System entry points
4. Architecture snapshot
5. Primary runtime flow
6. Repository directory map
7. How to use this atlas

## Editing order

1. Update `AGENTS.md` wording first.
2. Rewrite `architecture.md` second.
3. Review `codemap.md` for wording and cross-links third.
4. Touch folder codemaps only if cross-references need cleanup.

## Decision rule for future maintenance

- **Structure or file ownership changed** → update/regenerate codemap.
- **Layer rule, contract, or architectural decision changed** → update
  `architecture.md`.
- **Both changed** → update both, but do not duplicate details.

## Acceptance criteria

- `architecture.md` is shorter, more stable, and clearly normative.
- `codemap.md` remains the main repository navigation document.
- `AGENTS.md` gives unambiguous routing for when to read each doc.
- No major file/directory inventory remains in `architecture.md`.
- No long concrete implementation examples remain in `architecture.md` unless
  they define a contract that prose cannot express cleanly.
- The three documents cross-reference each other without circular ambiguity.

## Verification

- inspect `AGENTS.md`
- inspect `architecture.md`
- inspect `codemap.md`
- confirm each document has a distinct responsibility and no obvious overlap
