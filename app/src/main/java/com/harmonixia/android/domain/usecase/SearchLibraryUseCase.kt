package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class SearchLibraryUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(query: String, limit: Int): Result<SearchResults> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Search query is required"))
        }
        return repository.searchLibrary(trimmed, limit)
    }
}
