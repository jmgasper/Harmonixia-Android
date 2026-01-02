package com.harmonixia.android.domain.repository

import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.data.remote.WebSocketMessage
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.model.Queue
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MusicAssistantRepository {
    suspend fun connect(serverUrl: String, authToken: String): Result<Unit>
    suspend fun loginWithCredentials(serverUrl: String, username: String, password: String): Result<String>
    suspend fun disconnect()
    fun getConnectionState(): StateFlow<ConnectionState>
    fun observeEvents(): Flow<WebSocketMessage.EventMessage>
    suspend fun fetchAlbums(limit: Int, offset: Int): Result<List<Album>>
    suspend fun fetchArtists(limit: Int, offset: Int): Result<List<Artist>>
    suspend fun fetchPlaylists(limit: Int, offset: Int): Result<List<Playlist>>
    suspend fun fetchRecentlyPlayed(limit: Int): Result<List<Album>>
    suspend fun fetchRecentlyPlayedPlaylists(limit: Int): Result<List<Playlist>>
    suspend fun fetchRecentlyAdded(limit: Int): Result<List<Album>>
    suspend fun getAlbum(itemId: String, provider: String): Result<Album>
    fun getCachedAlbum(itemId: String, provider: String): Album?
    fun getCachedPlaylist(itemId: String, provider: String): Playlist?
    fun getCachedAlbumTracks(albumId: String, provider: String): List<Track>?
    fun getCachedPlaylistTracks(playlistId: String, provider: String): List<Track>?
    suspend fun searchLibrary(query: String, limit: Int): Result<SearchResults>
    suspend fun getAlbumTracks(albumId: String, provider: String): Result<List<Track>>
    suspend fun getAlbumTracksChunked(
        albumId: String,
        provider: String,
        offset: Int,
        limit: Int
    ): Result<List<Track>>
    suspend fun getPlaylistTracks(playlistId: String, provider: String): Result<List<Track>>
    suspend fun getPlaylistTracksChunked(
        playlistId: String,
        provider: String,
        offset: Int,
        limit: Int
    ): Result<List<Track>>
    suspend fun getPlaylist(playlistId: String, provider: String): Result<Playlist>
    suspend fun fetchPlayers(): Result<List<Player>>
    suspend fun getActiveQueue(playerId: String, includeItems: Boolean = true): Result<Queue?>
    suspend fun playMedia(queueId: String, mediaUris: List<String>, option: QueueOption): Result<Unit>
    suspend fun playMediaItem(
        queueId: String,
        media: String,
        option: QueueOption,
        startItem: String? = null
    ): Result<Unit>
    suspend fun playIndex(queueId: String, index: Int): Result<Unit>
    suspend fun pauseQueue(queueId: String): Result<Unit>
    suspend fun resumeQueue(queueId: String): Result<Unit>
    suspend fun nextTrack(queueId: String): Result<Unit>
    suspend fun previousTrack(queueId: String): Result<Unit>
    suspend fun seekTo(queueId: String, position: Int): Result<Unit>
    suspend fun reportPlaybackProgress(queueId: String, track: Track, positionSeconds: Int): Result<Unit>
    suspend fun reportTrackCompleted(queueId: String, track: Track, durationSeconds: Int): Result<Unit>
    suspend fun setRepeatMode(queueId: String, repeatMode: RepeatMode): Result<Unit>
    suspend fun setShuffleMode(queueId: String, shuffle: Boolean): Result<Unit>
    suspend fun clearQueue(queueId: String): Result<Unit>
    suspend fun createPlaylist(name: String): Result<Playlist>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    suspend fun addTracksToPlaylist(playlistId: String, trackUris: List<String>): Result<Unit>
    suspend fun removeTracksFromPlaylist(playlistId: String, positions: List<Int>): Result<Unit>
    suspend fun addToFavorites(itemId: String, provider: String, mediaType: String): Result<Unit>
    suspend fun removeFromFavorites(itemId: String, provider: String, mediaType: String): Result<Unit>
    suspend fun fetchFavorites(limit: Int, offset: Int): Result<List<Track>>
    suspend fun setPlayerVolume(playerId: String, volume: Int): Result<Unit>
    suspend fun setPlayerMute(playerId: String, muted: Boolean): Result<Unit>
}
