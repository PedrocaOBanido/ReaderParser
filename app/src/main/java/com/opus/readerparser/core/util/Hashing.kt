package com.opus.readerparser.core.util

import java.security.MessageDigest

/**
 * Computes a stable, filesystem-safe hash of [url].
 *
 * Algorithm: SHA-1 of the UTF-8 bytes, lower-cased hex, truncated to 16 characters.
 * The truncation is intentional — 16 hex chars (64 bits) is sufficient to avoid
 * accidental collisions within a single series/chapter namespace while keeping
 * directory names short.
 *
 * This is the canonical hashing function used to derive path components under
 * `filesDir/downloads/`. Do not swap the algorithm without a migration.
 *
 * Pure JVM — no Android dependencies.
 */
fun hashUrl(url: String): String {
    val digest = MessageDigest.getInstance("SHA-1")
    val bytes = digest.digest(url.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }.take(16)
}
