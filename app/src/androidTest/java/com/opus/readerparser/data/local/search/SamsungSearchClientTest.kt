package com.opus.readerparser.data.local.search

import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SamsungSearchClient] and [SamsungSearchSchema].
 *
 * SamsungSearchClient is tested with a [FakeSearchProviderDelegate] that
 * records calls without requiring Android framework classes. Batching
 * logic, register success/failure, and deleteAll are all covered.
 *
 * These tests require the Android framework (Bundle, Uri, ContentValues)
 * and therefore live in androidTest rather than local JVM tests.
 */
@RunWith(AndroidJUnit4::class)
class SamsungSearchClientSchemaTest {

    @Test
    fun schema_asset_file_exists_and_is_non_empty() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bytes = context.assets.open("search/samsung-search-indexable-series.xml").use { it.readBytes() }
        assertTrue("Schema asset must be non-empty", bytes.isNotEmpty())
        val xml = bytes.toString(Charsets.UTF_8)
        assertTrue("Schema must declare schema name", xml.contains("com.opus.readerparser.series"))
        assertTrue("Schema root must be <schema>", xml.contains("<schema"))
        assertTrue("Schema must declare keyFieldName", xml.contains("keyFieldName"))
    }
}

@RunWith(AndroidJUnit4::class)
class SamsungSearchClientRegisterTest {

    private val fakeSchema = SamsungSearchSchema.fake("test-schema".toByteArray())

    @Test
    fun registerSchema_returns_true_when_provider_reports_status_0() {
        val resultBundle = Bundle().apply { putInt("status", 0) }
        val fake = FakeSearchProviderDelegate(callResult = resultBundle)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertTrue(client.registerSchema())

        assertEquals(1, fake.callCount)
        assertEquals(SamsungSearchClient.AUTHORITY_URI, fake.lastCallAuthority)
        assertEquals("register_schema", fake.lastCallMethod)
        assertEquals(null, fake.lastCallArg)
    }

    @Test
    fun registerSchema_returns_false_when_provider_reports_non_zero_status() {
        val resultBundle = Bundle().apply { putInt("status", 1) }
        val fake = FakeSearchProviderDelegate(callResult = resultBundle)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertFalse(client.registerSchema())
    }

    @Test
    fun registerSchema_returns_false_when_result_bundle_is_null() {
        val fake = FakeSearchProviderDelegate(callResult = null)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertFalse(client.registerSchema())
    }

    @Test
    fun registerSchema_returns_false_when_call_throws() {
        val fake = FakeSearchProviderDelegate(callException = RuntimeException("boom"))
        val client = SamsungSearchClient(fake, fakeSchema)

        assertFalse(client.registerSchema())
    }

    @Test
    fun registerSchema_includes_name_and_schema_content_in_extras() {
        val resultBundle = Bundle().apply { putInt("status", 0) }
        val fake = FakeSearchProviderDelegate(callResult = resultBundle)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertTrue(client.registerSchema())

        val extras = fake.lastCallExtras
        assertEquals("com.opus.readerparser.series", extras?.getString("name"))
        val schemaContent = extras?.getByteArray("schema-content")
        assertNotNull("extras must contain non-null schema-content byte[]", schemaContent)
        assertTrue("schema-content must be non-empty", schemaContent!!.isNotEmpty())
    }
}

@RunWith(AndroidJUnit4::class)
class SamsungSearchClientAvailabilityTest {

    private val fakeSchema = SamsungSearchSchema.fake(ByteArray(0))

    @Test
    fun isAvailable_returns_true_when_bundle_has_version_1() {
        val bundle = Bundle().apply { putInt("response_search_api_version", 1) }
        val fake = FakeSearchProviderDelegate(callResult = bundle)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertTrue(client.isAvailable())

        assertEquals(1, fake.callCount)
        assertEquals(SamsungSearchClient.AUTHORITY_URI, fake.lastCallAuthority)
        assertEquals("request_search_api_version", fake.lastCallMethod)
        assertEquals(null, fake.lastCallArg)
        assertEquals(null, fake.lastCallExtras)
    }

    @Test
    fun isAvailable_returns_false_when_bundle_is_null() {
        val fake = FakeSearchProviderDelegate(callResult = null)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertFalse(client.isAvailable())
    }

    @Test
    fun isAvailable_returns_false_when_bundle_missing_version_key() {
        val bundle = Bundle().apply { putString("other_key", "value") }
        val fake = FakeSearchProviderDelegate(callResult = bundle)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertFalse(client.isAvailable())
    }

    @Test
    fun isAvailable_returns_false_when_version_is_zero() {
        val bundle = Bundle().apply { putInt("response_search_api_version", 0) }
        val fake = FakeSearchProviderDelegate(callResult = bundle)
        val client = SamsungSearchClient(fake, fakeSchema)

        assertFalse(client.isAvailable())
    }

    @Test
    fun isAvailable_returns_false_when_call_throws() {
        val fake = FakeSearchProviderDelegate(callException = RuntimeException("not found"))
        val client = SamsungSearchClient(fake, fakeSchema)

        assertFalse(client.isAvailable())
    }
}

@RunWith(AndroidJUnit4::class)
class SamsungSearchClientBulkInsertTest {

    private val fakeSchema = SamsungSearchSchema.fake(ByteArray(0))

    @Test
    fun bulkInsert_sends_all_documents_in_batches_of_100() {
        val fake = FakeSearchProviderDelegate()
        val client = SamsungSearchClient(fake, fakeSchema)
        val documents = (1..250).map { ContentValues() }

        val result = client.bulkInsert(documents)

        assertTrue(result)
        assertEquals(3, fake.bulkInsertCalls.size)
        assertEquals(SamsungSearchClient.SCHEMA_URI, fake.bulkInsertCalls[0].first)
        assertEquals(100, fake.bulkInsertCalls[0].second.size)
        assertEquals(100, fake.bulkInsertCalls[1].second.size)
        assertEquals(50, fake.bulkInsertCalls[2].second.size)
    }

    @Test
    fun bulkInsert_returns_true_for_empty_list() {
        val fake = FakeSearchProviderDelegate()
        val client = SamsungSearchClient(fake, fakeSchema)

        val result = client.bulkInsert(emptyList())

        assertTrue(result)
        assertTrue(fake.bulkInsertCalls.isEmpty())
    }

    @Test
    fun bulkInsert_sends_single_batch_for_15_documents() {
        val fake = FakeSearchProviderDelegate()
        val client = SamsungSearchClient(fake, fakeSchema)
        val documents = (1..15).map { ContentValues() }

        val result = client.bulkInsert(documents)

        assertTrue(result)
        assertEquals(1, fake.bulkInsertCalls.size)
        assertEquals(15, fake.bulkInsertCalls[0].second.size)
    }

    @Test
    fun bulkInsert_returns_false_when_provider_throws() {
        val fake = FakeSearchProviderDelegate(bulkInsertException = RuntimeException("io error"))
        val client = SamsungSearchClient(fake, fakeSchema)

        val result = client.bulkInsert(listOf(ContentValues()))

        assertFalse(result)
    }

    @Test
    fun bulkInsert_returns_false_on_partial_insert() {
        val fake = FakeSearchProviderDelegate().apply { bulkInsertCountOverride = 10 }
        val client = SamsungSearchClient(fake, fakeSchema)
        val documents = (1..15).map { ContentValues() }

        val result = client.bulkInsert(documents)

        assertFalse(result)
        assertEquals(1, fake.bulkInsertCalls.size)
    }
}

@RunWith(AndroidJUnit4::class)
class SamsungSearchClientDeleteAllTest {

    private val fakeSchema = SamsungSearchSchema.fake(ByteArray(0))

    @Test
    fun deleteAll_calls_provider_delete_with_correct_URI() {
        val fake = FakeSearchProviderDelegate()
        val client = SamsungSearchClient(fake, fakeSchema)

        val result = client.deleteAll()

        assertTrue(result)
        assertEquals(1, fake.deleteCalls.size)
        assertEquals(SamsungSearchClient.SCHEMA_URI, fake.deleteCalls[0].first)
    }

    @Test
    fun deleteAll_returns_false_when_provider_throws() {
        val fake = FakeSearchProviderDelegate(deleteException = RuntimeException("io error"))
        val client = SamsungSearchClient(fake, fakeSchema)

        val result = client.deleteAll()

        assertFalse(result)
    }
}

@RunWith(AndroidJUnit4::class)
class SamsungSearchClientQueryTest {

    private val fakeSchema = SamsungSearchSchema.fake(ByteArray(0))

    @Test
    fun query_returns_hits_from_cursor() {
        val cursor = MatrixCursor(arrayOf("_id", "title", "source_url")).apply {
            addRow(arrayOf("1:https://a", "Alpha", "readerparser://series/1/https%3A%2F%2Fa"))
            addRow(arrayOf("2:https://b", "Beta", "readerparser://series/2/https%3A%2F%2Fb"))
        }
        val fake = FakeSearchProviderDelegate(queryResult = cursor)
        val client = SamsungSearchClient(fake, fakeSchema)

        runTest {
            when (val result = client.query("alpha")) {
                is SamsungSearchQueryResult.Success -> {
                    assertEquals(2, result.hits.size)
                    assertEquals("1:https://a", result.hits[0].id)
                    assertEquals("Alpha", result.hits[0].title)
                    assertEquals("readerparser://series/1/https%3A%2F%2Fa", result.hits[0].sourceUrl)
                    assertEquals(SamsungSearchClient.SCHEMA_URI, fake.lastQueryUri)
                    assertEquals("title LIKE ? OR author LIKE ? OR genres LIKE ?", fake.lastQuerySelection)
                    assertEquals("%alpha%", fake.lastQuerySelectionArgs?.get(0))
                }
                is SamsungSearchQueryResult.Failure -> throw AssertionError("Expected success")
            }
        }
    }

    @Test
    fun query_returns_failure_when_cursor_is_null() {
        val fake = FakeSearchProviderDelegate(queryResult = null)
        val client = SamsungSearchClient(fake, fakeSchema)

        runTest {
            when (val result = client.query("alpha")) {
                is SamsungSearchQueryResult.Success -> throw AssertionError("Expected failure")
                is SamsungSearchQueryResult.Failure -> assertEquals("Samsung Search query returned null cursor", result.message)
            }
        }
    }

    @Test
    fun query_returns_empty_for_blank_query_without_calling_provider() {
        val fake = FakeSearchProviderDelegate(queryResult = MatrixCursor(arrayOf("_id", "title", "source_url")))
        val client = SamsungSearchClient(fake, fakeSchema)

        runTest {
            when (val result = client.query("   ")) {
                is SamsungSearchQueryResult.Success -> assertTrue(result.hits.isEmpty())
                is SamsungSearchQueryResult.Failure -> throw AssertionError("Expected success")
            }
        }
        assertEquals(0, fake.queryCount)
    }

    @Test
    fun query_returns_failure_when_provider_throws() {
        val fake = FakeSearchProviderDelegate(queryException = RuntimeException("boom"))
        val client = SamsungSearchClient(fake, fakeSchema)

        runTest {
            when (val result = client.query("alpha")) {
                is SamsungSearchQueryResult.Success -> throw AssertionError("Expected failure")
                is SamsungSearchQueryResult.Failure -> assertEquals("boom", result.message)
            }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class SamsungSearchClientBatchingTest {

    @Test
    fun documents_250_split_into_3_batches() {
        val documents = (1..250).map { ContentValues() }
        val batches = documents.chunked(100)

        assertEquals(3, batches.size)
        assertEquals(100, batches[0].size)
        assertEquals(100, batches[1].size)
        assertEquals(50, batches[2].size)
    }

    @Test
    fun documents_15_produce_a_single_batch() {
        val documents = (1..15).map { ContentValues() }
        val batches = documents.chunked(100)

        assertEquals(1, batches.size)
        assertEquals(15, batches[0].size)
    }

    @Test
    fun empty_documents_produce_no_batches() {
        val documents = emptyList<ContentValues>()
        val batches = documents.chunked(100)

        assertTrue(batches.isEmpty())
    }

    @Test
    fun exactly_100_documents_produce_one_batch() {
        val documents = (1..100).map { ContentValues() }
        val batches = documents.chunked(100)

        assertEquals(1, batches.size)
        assertEquals(100, batches[0].size)
    }
}

/**
 * Fake [SearchProviderDelegate] for instrumented tests.
 * Records calls and allows injecting results/exceptions.
 */
internal class FakeSearchProviderDelegate(
    private val getTypeResult: String? = null,
    private val getTypeException: Exception? = null,
    private val callResult: Bundle? = null,
    private val callException: Exception? = null,
    private val queryResult: Cursor? = null,
    private val queryException: Exception? = null,
    private val bulkInsertException: Exception? = null,
    private val deleteException: Exception? = null,
) : SearchProviderDelegate {

    /** When non-null, [bulkInsert] returns this value instead of `values.size`. */
    var bulkInsertCountOverride: Int? = null

    var callCount = 0
        private set
    var lastCallAuthority: Uri? = null
        private set
    var lastCallMethod: String? = null
        private set
    var lastCallArg: String? = null
        private set
    var lastCallExtras: Bundle? = null
        private set
    var lastQueryUri: Uri? = null
        private set
    var lastQuerySelection: String? = null
        private set
    var lastQuerySelectionArgs: Array<String>? = null
        private set
    var queryCount = 0
        private set

    val bulkInsertCalls = mutableListOf<Pair<Uri, Array<ContentValues>>>()
    val deleteCalls = mutableListOf<Triple<Uri, String?, Array<String?>?>>()

    override fun getType(uri: Uri): String? {
        getTypeException?.let { throw it }
        return getTypeResult
    }

    override fun call(authority: Uri, method: String, arg: String?, extras: Bundle?): Bundle? {
        callException?.let { throw it }
        callCount++
        lastCallAuthority = authority
        lastCallMethod = method
        lastCallArg = arg
        lastCallExtras = extras
        return callResult
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        queryException?.let { throw it }
        queryCount++
        lastQueryUri = uri
        lastQuerySelection = selection
        lastQuerySelectionArgs = selectionArgs
        return queryResult
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        bulkInsertException?.let { throw it }
        bulkInsertCalls.add(uri to values)
        return bulkInsertCountOverride ?: values.size
    }

    override fun delete(uri: Uri, where: String?, selectionArgs: Array<String?>?): Int {
        deleteException?.let { throw it }
        deleteCalls.add(Triple(uri, where, selectionArgs))
        return 0
    }
}
