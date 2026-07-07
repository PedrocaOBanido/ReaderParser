package com.opus.readerparser.domain.model

sealed interface LibrarySearchResult {
    data class Success(val series: List<Series>) : LibrarySearchResult
    data class Failure(val message: String) : LibrarySearchResult
}
