package com.opus.readerparser.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashingTest {

    @Test
    fun `same url produces same hash`() {
        val url = "https://example.com/series/my-series"
        assertEquals(hashUrl(url), hashUrl(url))
    }

    @Test
    fun `different urls produce different hashes`() {
        val a = hashUrl("https://example.com/series/a")
        val b = hashUrl("https://example.com/series/b")
        assertNotEquals(a, b)
    }

    @Test
    fun `output is exactly 16 characters`() {
        val hash = hashUrl("https://example.com/any-url")
        assertEquals(16, hash.length)
    }

    @Test
    fun `output contains only lowercase hex characters`() {
        val hash = hashUrl("https://example.com/any-url")
        assertTrue(
            "Expected only lowercase hex, got: $hash",
            hash.all { it in '0'..'9' || it in 'a'..'f' },
        )
    }

    @Test
    fun `known vector matches precomputed sha1 truncated`() {
        // SHA-1("https://test.invalid/series/test") = dc40130356d9a3ebdc97a81b2994e29032b61fbc
        // Truncated to 16 chars: dc40130356d9a3eb
        val url = "https://test.invalid/series/test"
        assertEquals("dc40130356d9a3eb", hashUrl(url))
    }

    @Test
    fun `empty string produces a consistent 16 char hash`() {
        val hash = hashUrl("")
        assertEquals(16, hash.length)
        assertEquals(hashUrl(""), hash)
    }
}
