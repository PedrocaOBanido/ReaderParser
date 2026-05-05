package com.opus.readerparser.core.util

import com.opus.readerparser.domain.model.ContentType

/**
 * Generates a stable [Long] identifier for a source from its name, language, and content type.
 *
 * The formula is:
 * ```
 * "$name/$lang/${type.name}".hashCode().toLong() and 0xFFFFFFFFL
 * ```
 *
 * This ID is deterministic across reinstalls and is used as a foreign key in the database.
 */
fun computeSourceId(name: String, lang: String, type: ContentType): Long =
    "$name/$lang/${type.name}".hashCode().toLong() and 0xFFFFFFFFL
