package com.opus.readerparser.data.local.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Migration tests for [AppDatabase].
 *
 * Currently the database is at version 1, so there is no `Migration_X_Y` class
 * to exercise. This file's job is to establish the testing infrastructure and
 * verify that the schema export pipeline + [MigrationTestHelper] are wired up
 * correctly. As soon as the schema changes, follow the template at the bottom
 * of this file to add the corresponding migration test alongside the new
 * `Migration_X_Y` class registered in `DatabaseModule`.
 *
 * Per `data/local/database/AGENTS.md`:
 * - Bump `AppDatabase.version` by 1 per change. Never skip versions.
 * - Every version change ships an explicit `Migration` class. No exceptions.
 * - `fallbackToDestructiveMigration` is forbidden in every build configuration.
 * - Migration tests live here in `androidTest/`, not `test/`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    /**
     * Sanity check: the `MigrationTestHelper` can create the database at the
     * current schema version using the exported JSON in `app/schemas/`. If
     * this fails, either the schema export is broken or `room-testing` is not
     * picking up `room.schemaLocation` from the KSP arg in `app/build.gradle.kts`.
     */
    @Test
    @Throws(IOException::class)
    fun createsDatabaseAtCurrentVersion() {
        helper.createDatabase(TEST_DB, CURRENT_VERSION).use { db ->
            assertThat(db.version).isEqualTo(CURRENT_VERSION)
        }
    }

    /**
     * Sanity check: opening the production database with no migrations
     * registered succeeds at the current version. This guards against an
     * accidental version bump landing without the matching schema export
     * (which would make the production database unable to open on a fresh
     * install).
     */
    @Test
    @Throws(IOException::class)
    fun openRealDatabaseAtCurrentVersion() {
        helper.createDatabase(TEST_DB, CURRENT_VERSION).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .build()
        try {
            assertThat(db.openHelper.writableDatabase.version).isEqualTo(CURRENT_VERSION)
        } finally {
            db.close()
            context.deleteDatabase(TEST_DB)
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"

        // Bump in lockstep with `@Database(version = N)` on `AppDatabase`.
        private const val CURRENT_VERSION = 1
    }

    // ---------------------------------------------------------------------
    // Template for future migration tests. Copy, uncomment, fill in.
    //
    // When you add `Migration_1_2`:
    //   1. Bump `AppDatabase.version` to 2 (and the CURRENT_VERSION constant).
    //   2. `./gradlew :app:assembleDebug` to regenerate `app/schemas/.../2.json`.
    //   3. Add the test below alongside the migration class.
    //
    // /**
    //  * Verifies `Migration_1_2` preserves existing rows and applies the new
    //  * schema (e.g., the new `tags` column on `series`).
    //  */
    // @Test
    // @Throws(IOException::class)
    // fun migrate1To2() {
    //     // 1. Open at the OLD version and seed representative rows.
    //     helper.createDatabase(TEST_DB, 1).use { db ->
    //         db.execSQL(
    //             """
    //             INSERT INTO series
    //                 (sourceId, url, title, genresJson, status, type,
    //                  inLibrary, addedAt)
    //             VALUES
    //                 (1, 'https://example.com/series/1', 'Test Series',
    //                  '[]', 'ONGOING', 'NOVEL', 1, 1000)
    //             """.trimIndent(),
    //         )
    //     }
    //
    //     // 2. Run the migration. `validateDroppedTables = true` makes Room
    //     //    diff the resulting schema against `2.json`.
    //     val migrated = helper.runMigrationsAndValidate(
    //         TEST_DB,
    //         2,
    //         /* validateDroppedTables = */ true,
    //         Migration_1_2,
    //     )
    //
    //     // 3. Verify data survived and the new column has its default.
    //     migrated.query("SELECT title, tags FROM series WHERE sourceId = 1").use { c ->
    //         assertThat(c.moveToFirst()).isTrue()
    //         assertThat(c.getString(0)).isEqualTo("Test Series")
    //         assertThat(c.getString(1)).isEqualTo("")
    //     }
    //     migrated.close()
    // }
}
