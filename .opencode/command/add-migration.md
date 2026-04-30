---
description: Add a Room migration for a schema change.
agent: room-migration
---

Schema change description: $ARGUMENTS

Required steps:

1. Read the current `AppDatabase.version` and the latest schema JSON in
   `app/schemas/`.
2. Bump `AppDatabase.version` by 1.
3. Create `Migration_${old}_${new}.kt` under
   `app/src/main/kotlin/com/example/reader/data/local/database/migrations/`
   with explicit SQL for the change. Do not use any auto-migration helpers.
4. Register the migration in `DatabaseModule`.
5. Add a migration test in
   `app/src/androidTest/kotlin/com/example/reader/data/local/database/MigrationTest.kt`
   using `MigrationTestHelper`.
6. Update affected entities, DAOs, and mappers.
7. Run `./gradlew :app:assembleDebug` to regenerate the schema JSON; verify
   the new file appears in `app/schemas/`.

Never use `fallbackToDestructiveMigration` to skip writing the migration.
