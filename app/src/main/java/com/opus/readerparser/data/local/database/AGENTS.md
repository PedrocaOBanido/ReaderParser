# Database rules

- Bump `AppDatabase.version` by 1 per schema change. Never skip versions.
- Every version change ships with an explicit `Migration` class. No exceptions.
- `fallbackToDestructiveMigration` is forbidden in every build configuration.
- `app/schemas/` is auto-generated. Don't hand-edit. Commit changes to it.
- Identity is `(sourceId, url)`. Never use auto-incrementing IDs as foreign keys.
- Migration tests are required. They live in `androidTest/`, not `test/`.
