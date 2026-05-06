package com.opus.readerparser.sources.asurascans

import com.opus.readerparser.domain.model.FilterList
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Live smoke tests that hit the real [asurascans.com](https://asurascans.com/).
 *
 * These use the Ktor CIO engine (pure Kotlin, no Android dependencies) so they
 * can run on JVM without an Android emulator — suitable for WSL, CI, or local
 * dev machines.
 *
 * All tests are `@Ignore` by default to avoid slowing regular test runs.
 * Run manually with:
 * ```
 * ./gradlew :app:testDebugUnitTest --tests "*LiveTest"
 * ```
 */
class AsuraScansLiveTest {

    @Test
    @Ignore("Live network test — run manually against asurascans.com")
    fun `getPopular returns results from live site`() = runTest(timeout = 60.seconds) {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
            }
        }

        val source = AsuraScans(client)
        val result = source.getPopular(1)

        assertTrue(
            "Expected at least one series from live site — Cloudflare may be blocking",
            result.series.isNotEmpty()
        )

        // Verify the first series has basic fields filled
        val first = result.series[0]
        assertTrue("Title must not be blank", first.title.isNotBlank())
        assertTrue("URL must not be blank", first.url.isNotBlank())
    }

    @Test
    @Ignore("Live network test — run manually against asurascans.com")
    fun `search returns results from live site`() = runTest(timeout = 60.seconds) {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
            }
        }

        val source = AsuraScans(client)
        val result = source.search("solo", 1, FilterList())

        assertTrue(
            "Expected search results from live site — Cloudflare may be blocking",
            result.series.isNotEmpty()
        )
    }
}
