package com.opus.readerparser.data.local.search

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Samsung Search v2 ContentProvider calls for schema registration,
 * bulk document insertion, and index clearing.
 *
 * All methods are safe to call when Samsung Search is not installed — they
 * catch exceptions internally and return `false` on failure.
 */
@Singleton
open class SamsungSearchClient @Inject constructor(
    private val delegate: SearchProviderDelegate,
    private val schema: SamsungSearchSchema,
) {

    /**
     * Returns `true` if the Samsung Search ContentProvider is reachable on
     * this device. Probes via [METHOD_REQUEST_API_VERSION] — `null` bundle,
     * missing key, or version < 1 means unavailable.
     */
    open fun isAvailable(): Boolean = try {
        val result = delegate.call(AUTHORITY_URI, METHOD_REQUEST_API_VERSION, null, null)
        val version = result?.getInt("response_search_api_version", 0) ?: 0
        version >= 1
    } catch (e: Exception) {
        Log.w(TAG, "Samsung Search provider not available", e)
        false
    }

    /**
     * Registers (or re-registers) the search schema. Idempotent — safe to
     * call on every app launch.
     */
    open fun registerSchema(): Boolean = try {
        val schemaBytes = schema.readSchemaBytes()
        val extras = Bundle().apply {
            putString("name", SCHEMA_NAME)
            putByteArray("schema-content", schemaBytes)
        }
        val result = delegate.call(AUTHORITY_URI, METHOD_REGISTER_SCHEMA, SCHEMA_NAME, extras)
        val status = result?.getInt("status", -1) ?: -1
        if (status == 0) {
            Log.i(TAG, "Schema '$SCHEMA_NAME' registered successfully")
            true
        } else {
            Log.w(TAG, "Schema registration returned status $status")
            false
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to register schema", e)
        false
    }

    /**
     * Inserts a list of documents into the search index, splitting into
     * batches of [BATCH_SIZE] to respect ContentProvider limits.
     *
     * @return `true` if all batches were fully inserted, `false` on failure
     *   or when any batch inserts fewer rows than requested (partial insert).
     */
    open fun bulkInsert(documents: List<ContentValues>): Boolean {
        if (documents.isEmpty()) return true
        return try {
            documents.chunked(BATCH_SIZE).forEach { batch ->
                val count = delegate.bulkInsert(SCHEMA_URI, batch.toTypedArray())
                Log.d(TAG, "bulkInsert batch: $count documents inserted")
                if (count != batch.size) {
                    Log.w(TAG, "bulkInsert partial: expected ${batch.size}, got $count")
                    return false
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "bulkInsert failed", e)
            false
        }
    }

    /**
     * Deletes all documents from the search index.
     *
     * @return `true` if the delete succeeded, `false` on failure.
     */
    open fun deleteAll(): Boolean {
        return try {
            delegate.delete(SCHEMA_URI, null, null)
            Log.d(TAG, "deleteAll completed")
            true
        } catch (e: Exception) {
            Log.w(TAG, "deleteAll failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "SamsungSearchClient"
        private const val SCHEMA_NAME = "com.opus.readerparser.series"
        private const val METHOD_REQUEST_API_VERSION = "request_search_api_version"
        private const val METHOD_REGISTER_SCHEMA = "register_schema"
        private const val BATCH_SIZE = 100

        /**
         * URI for the Samsung Search authority. Lazy to avoid `Uri.parse()`
         * during class-loading on JVM unit tests (Android stub → crash).
         */
        @VisibleForTesting
        val AUTHORITY_URI: Uri by lazy {
            Uri.parse("content://com.samsung.android.smartsuggestions.search/v2")
        }

        /**
         * URI for the series schema endpoint. Lazy for the same reason as
         * [AUTHORITY_URI].
         */
        @VisibleForTesting
        val SCHEMA_URI: Uri by lazy {
            Uri.parse("content://com.samsung.android.smartsuggestions.search/v2/com.opus.readerparser.series")
        }
    }
}

/**
 * Abstraction over [ContentResolver] operations used by [SamsungSearchClient].
 * Extracted to allow JVM unit tests to substitute a fake.
 */
interface SearchProviderDelegate {
    fun getType(uri: Uri): String?
    fun call(authority: Uri, method: String, arg: String?, extras: Bundle?): Bundle?
    fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int
    fun delete(uri: Uri, where: String?, selectionArgs: Array<String?>?): Int
}

/**
 * Production implementation that delegates to [ContentResolver].
 */
class ContentResolverDelegate(
    private val resolver: ContentResolver,
) : SearchProviderDelegate {

    override fun getType(uri: Uri): String? = resolver.getType(uri)

    override fun call(authority: Uri, method: String, arg: String?, extras: Bundle?): Bundle? =
        resolver.call(authority, method, arg, extras)

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int =
        resolver.bulkInsert(uri, values)

    override fun delete(uri: Uri, where: String?, selectionArgs: Array<String?>?): Int =
        resolver.delete(uri, where, selectionArgs)
}
