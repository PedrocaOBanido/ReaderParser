# Project brief

## Purpose

ReaderParser is a personal Android app that reads webnovel and manhwa sources
through site-specific plugins behind a common contract.

## Primary goals

- Keep the app source-agnostic above the `Source` and repository layers.
- Support browsing, library management, series detail, chapter reading,
  downloads, and settings.
- Persist data locally with app-private storage and offline-friendly download
  flows.
- Make new sites additive work via plugins rather than app-wide rewrites.

## Boundaries

- Personal-use only.
- No sync, accounts, or cloud features.
- No content hosting or redistribution.
- No multi-user behavior.
- No browser-shell scope expansion unless explicitly requested.

## Durable constraints

- Novel and manhwa readers remain separate screens.
- Series and chapter identity is `(sourceId, url)`.
- Domain models stay pure Kotlin and immutable.
