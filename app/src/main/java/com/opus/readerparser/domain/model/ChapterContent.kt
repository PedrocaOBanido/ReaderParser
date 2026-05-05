package com.opus.readerparser.domain.model

/**
 * The two shapes a chapter can take. The reader screen branches on this once;
 * everything else in the stack stays content-type-agnostic.
 */
sealed interface ChapterContent {
    /** A novel chapter. HTML is sanitized but otherwise untouched. */
    data class Text(val html: String) : ChapterContent

    /** A manhwa chapter. Pages are HTTP URLs in reading order. */
    data class Pages(val imageUrls: List<String>) : ChapterContent
}
