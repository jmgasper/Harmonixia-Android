package com.harmonixia.android.domain.usecase

import android.content.Context
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SearchLibraryUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(
        query: String,
        limit: Int,
        libraryOnly: Boolean = true
    ): Result<SearchResults> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return Result.failure(
                IllegalArgumentException(context.getString(R.string.search_validation_query_required))
            )
        }
        return repository.searchLibrary(trimmed, limit, libraryOnly)
    }
}
