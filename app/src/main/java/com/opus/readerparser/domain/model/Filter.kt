package com.opus.readerparser.domain.model

/** A search or browse filter that sources can optionally support. */
sealed interface Filter {
    data class Text(val key: String, val value: String) : Filter
    data class Select(val key: String, val value: String) : Filter
    data class Toggle(val key: String, val value: Boolean) : Filter
}
