package com.harmonixia.android.domain.repository

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface OfflineLibraryRepository {
    fun getDownloadedAlbumsByArtist(artistName: String): Flow<List<Album>>

    fun getDownloadedTracksByArtist(artistName: String): Flow<List<Track>>

    fun searchDownloadedContent(query: String): Flow<SearchResults>
}

const val OFFLINE_PROVIDER = "offline"
