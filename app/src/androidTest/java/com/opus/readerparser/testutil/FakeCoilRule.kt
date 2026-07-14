package com.opus.readerparser.testutil

import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.test.FakeImage
import coil3.test.FakeImageLoaderEngine
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit [TestRule] that builds a deterministic [ImageLoader] backed by a
 * [FakeImageLoaderEngine]. Every request returns a 200×1200 pixel [FakeImage]
 * so items rendered via Coil's `AsyncImage` have a known non-zero size.
 *
 * Use [imageLoader] with `AsyncImage(…, imageLoader = …)` or pass to
 * [ReaderContent].
 *
 * Usage:
 * ```
 * @get:Rule val fakeCoilRule = FakeCoilRule()
 * // then: AsyncImage(…, imageLoader = fakeCoilRule.imageLoader, …)
 * ```
 */
class FakeCoilRule : TestRule {

    lateinit var imageLoader: ImageLoader
        private set

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val engine = FakeImageLoaderEngine.Builder()
                    .default(FakeImage(width = 200, height = 1200))
                    .build()
                imageLoader = ImageLoader.Builder(context)
                    .components { add(engine) }
                    .build()
                try {
                    base.evaluate()
                } finally {
                    imageLoader.shutdown()
                }
            }
        }
    }
}
