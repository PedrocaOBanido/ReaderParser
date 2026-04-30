package com.opus.novelparser.testutil

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

fun mockHttpClient(
    block: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): HttpClient = HttpClient(MockEngine) {
    engine {
        addHandler(block)
    }
}

fun readFixture(path: String): String {
    val classLoader = Thread.currentThread().contextClassLoader
    return requireNotNull(
        classLoader.getResource(path),
    ) { "Fixture not found: $path" }.readText()
}

suspend fun MockRequestHandleScope.respondHtml(
    html: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(
    content = html,
    status = status,
    headers = headersOf("Content-Type", "text/html; charset=UTF-8"),
)
