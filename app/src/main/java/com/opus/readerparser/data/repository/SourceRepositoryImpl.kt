package com.opus.readerparser.data.repository

import com.opus.readerparser.data.source.SourceRegistry
import com.opus.readerparser.domain.SourceRepository
import com.opus.readerparser.domain.model.SourceInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRepositoryImpl @Inject constructor(
    private val registry: SourceRegistry,
) : SourceRepository {

    override fun getSources(): List<SourceInfo> =
        registry.all().map { SourceInfo(it.id, it.name, it.lang, it.type) }
}
