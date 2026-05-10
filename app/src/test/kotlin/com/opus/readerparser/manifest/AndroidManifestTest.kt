package com.opus.readerparser.manifest

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Validates critical AndroidManifest.xml declarations that have caused
 * production crashes and are otherwise invisible until runtime on a device.
 *
 * Both bugs caught here were confirmed crashes:
 *  1. Missing android:name on <application> → Hilt crash at startup
 *     (IllegalStateException before MainActivity even started).
 *  2. Missing INTERNET permission → SecurityException / EPERM on the OkHttp
 *     Dispatcher thread on the first network call (fatal, unkillable crash).
 *
 * These are plain JVM tests — no emulator or device needed.
 */
class AndroidManifestTest {

    private val manifest: Element by lazy {
        val file = File("src/main/AndroidManifest.xml")
        assertTrue(
            "AndroidManifest.xml not found at ${file.absolutePath}",
            file.exists(),
        )
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement
    }

    // -------------------------------------------------------------------------
    // 1. Hilt application class
    // -------------------------------------------------------------------------

    /**
     * Regression: app crashed immediately on every launch with
     *   "Hilt Activity must be attached to an @HiltAndroidApp Application"
     * because <application> had no android:name, so Android instantiated the
     * default Application instead of our @HiltAndroidApp App.
     */
    @Test
    fun `application tag declares android name`() {
        val application = manifest
            .getElementsByTagName("application")
            .item(0) as? Element
        assertNotNull("<application> element not found in manifest", application)

        val name = application!!
            .getAttribute("android:name")
        assertTrue(
            "<application android:name> is missing or empty — " +
                "Hilt cannot initialise and the app will crash at startup.",
            name.isNotBlank(),
        )
    }

    /**
     * The declared class must be the @HiltAndroidApp-annotated App.
     * Accepts both the short form (.App) and the fully-qualified form.
     */
    @Test
    fun `application name points to App class`() {
        val application = manifest
            .getElementsByTagName("application")
            .item(0) as? Element
        assertNotNull(application)

        val name = application!!.getAttribute("android:name")
        assertTrue(
            "android:name=\"$name\" does not refer to the App class. " +
                "Expected \".App\" or \"com.opus.readerparser.App\".",
            name == ".App" || name == "com.opus.readerparser.App",
        )
    }

    // -------------------------------------------------------------------------
    // 2. INTERNET permission
    // -------------------------------------------------------------------------

    /**
     * Regression: app crashed with SecurityException on the OkHttp Dispatcher
     * thread the moment the browse screen made its first network request.
     * The INTERNET permission was absent from the manifest entirely.
     * Because the exception escaped a background thread with no catch-all,
     * Android treated it as a fatal crash and killed the process.
     */
    @Test
    fun `INTERNET permission is declared`() {
        val permissions = manifest.getElementsByTagName("uses-permission")
        val declaredNames = (0 until permissions.length)
            .map { permissions.item(it) as Element }
            .map { it.getAttribute("android:name") }

        assertTrue(
            "android.permission.INTERNET is not declared — " +
                "every network call will throw SecurityException at runtime.",
            "android.permission.INTERNET" in declaredNames,
        )
    }
}
