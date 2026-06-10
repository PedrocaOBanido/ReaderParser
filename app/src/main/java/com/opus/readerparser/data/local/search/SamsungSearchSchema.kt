package com.opus.readerparser.data.local.search

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the Samsung Search schema XML asset and provides it as a [ByteArray]
 * for the `register_schema` ContentProvider call.
 */
@Singleton
open class SamsungSearchSchema private constructor(
    private val reader: () -> ByteArray,
) {
    @Inject constructor(
        @ApplicationContext context: Context,
    ) : this(reader = { context.assets.open(SCHEMA_ASSET_PATH).use { it.readBytes() } })

    /**
     * Returns the raw bytes of the bundled search schema XML asset.
     */
    open fun readSchemaBytes(): ByteArray = reader()

    companion object {
        private const val SCHEMA_ASSET_PATH = "search/samsung-search-indexable-series.xml"

        /**
         * Creates a [SamsungSearchSchema] that returns the given bytes,
         * useful for unit tests that don't have an Android asset system.
         */
        @VisibleForTesting
        fun fake(bytes: ByteArray): SamsungSearchSchema =
            SamsungSearchSchema(reader = { bytes })
    }
}
