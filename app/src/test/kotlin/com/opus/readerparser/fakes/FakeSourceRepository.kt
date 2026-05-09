package com.opus.readerparser.fakes

import com.opus.readerparser.domain.SourceRepository
import com.opus.readerparser.domain.model.SourceInfo

class FakeSourceRepository : SourceRepository {
    var sourceList: List<SourceInfo> = emptyList()

    override fun getSources(): List<SourceInfo> = sourceList
}
