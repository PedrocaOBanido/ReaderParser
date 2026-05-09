package com.opus.readerparser.domain

import com.opus.readerparser.domain.model.SourceInfo

interface SourceRepository {
    fun getSources(): List<SourceInfo>
}
