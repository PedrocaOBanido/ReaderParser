package com.opus.readerparser.workers

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for [SamsungSearchUpdateReceiver] action constant and
 * action-gating logic.
 *
 * Full WorkManager scheduling tests are in the instrumented test suite
 * ([SamsungSearchUpdateReceiverInstrumentedTest]).
 */
class SamsungSearchUpdateReceiverTest {

    @Test
    fun `ACTION_UPDATE_INDEX matches Samsung Search contract`() {
        assertEquals(
            "com.samsung.android.smartsuggestions.search.ACTION_UPDATE_INDEX",
            SamsungSearchUpdateReceiver.ACTION_UPDATE_INDEX,
        )
    }

    @Test
    fun `receiver action string is not empty`() {
        assertTrue(SamsungSearchUpdateReceiver.ACTION_UPDATE_INDEX.isNotEmpty())
    }

    @Test
    fun `wrong action string does not match`() {
        val wrongAction = "com.samsung.android.smartsuggestions.search.SOMETHING_ELSE"
        assertTrue(wrongAction != SamsungSearchUpdateReceiver.ACTION_UPDATE_INDEX)
    }
}
