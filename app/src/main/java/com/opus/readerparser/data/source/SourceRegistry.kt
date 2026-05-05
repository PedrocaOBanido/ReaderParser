package com.opus.readerparser.data.source

/**
 * Compile-time registry of all [Source] plugins loaded at app start by Hilt.
 *
 * @property sources Map keyed by each source's [Source.id].
 */
class SourceRegistry(private val sources: Map<Long, Source>) {

    /** Returns the [Source] registered under [id], or throws if no such source exists. */
    operator fun get(id: Long): Source =
        sources[id] ?: error("No source registered with id=$id")

    /** Returns all registered sources, sorted by [Source.name]. */
    fun all(): List<Source> = sources.values.sortedBy { it.name }
}
