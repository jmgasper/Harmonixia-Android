package com.harmonixia.android.data.repository

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

@Singleton
class OfflineLibraryRepositoryImpl @Inject constructor(
    private val localMediaRepository: LocalMediaRepository
) : OfflineLibraryRepository {
    override fun getDownloadedAlbumsByArtist(artistName: String): Flow<List<Album>> {
        return localMediaRepository.getAlbumsByArtist(artistName)
    }

    override fun getDownloadedTracksByArtist(artistName: String): Flow<List<Track>> {
        return localMediaRepository.getTracksByArtist(artistName)
    }

    override fun searchDownloadedContent(query: String): Flow<SearchResults> {
        if (query.isBlank()) return flowOf(SearchResults())
        return combine(
            localMediaRepository.searchAlbums(query),
            localMediaRepository.searchArtists(query),
            localMediaRepository.searchTracks(query)
        ) { albums, artists, tracks ->
            SearchResults(
                albums = albums,
                artists = artists,
                playlists = emptyList(),
                tracks = tracks
            )
        }
    }
}
