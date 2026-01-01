package com.harmonixia.android.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.work.CachePrefetchWorker
import com.harmonixia.android.work.KEY_ALBUM_IDS
import com.harmonixia.android.work.KEY_ALBUM_PROVIDERS
import com.harmonixia.android.work.KEY_ARTIST_NAMES
import com.harmonixia.android.work.KEY_PLAYLIST_IDS
import com.harmonixia.android.work.KEY_PLAYLIST_PROVIDERS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefetchScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleAlbumTrackPrefetch(albums: List<Album>) {
        val limited = albums.take(MAX_PREFETCH_ITEMS)
        if (limited.isEmpty()) return
        val ids = limited.map { it.itemId }.toTypedArray()
        val providers = limited.map { it.provider }.toTypedArray()
        enqueueUniqueWork(
            uniqueName = WORK_NAME_ALBUM_TRACKS,
            data = workDataOf(
                KEY_ALBUM_IDS to ids,
                KEY_ALBUM_PROVIDERS to providers
            )
        )
    }

    fun schedulePlaylistTrackPrefetch(playlists: List<Playlist>) {
        val limited = playlists.take(MAX_PREFETCH_ITEMS)
        if (limited.isEmpty()) return
        val ids = limited.map { it.itemId }.toTypedArray()
        val providers = limited.map { it.provider }.toTypedArray()
        enqueueUniqueWork(
            uniqueName = WORK_NAME_PLAYLIST_TRACKS,
            data = workDataOf(
                KEY_PLAYLIST_IDS to ids,
                KEY_PLAYLIST_PROVIDERS to providers
            )
        )
    }

    fun scheduleDetailPrefetch(albums: List<Album>, playlists: List<Playlist>) {
        val limitedAlbums = albums.take(MAX_PREFETCH_ITEMS)
        val limitedPlaylists = playlists.take(MAX_PREFETCH_ITEMS)
        if (limitedAlbums.isEmpty() && limitedPlaylists.isEmpty()) return
        val albumIds = limitedAlbums.map { it.itemId }.toTypedArray()
        val albumProviders = limitedAlbums.map { it.provider }.toTypedArray()
        val playlistIds = limitedPlaylists.map { it.itemId }.toTypedArray()
        val playlistProviders = limitedPlaylists.map { it.provider }.toTypedArray()
        enqueueUniqueWork(
            uniqueName = WORK_NAME_DETAIL_PREFETCH,
            data = workDataOf(
                KEY_ALBUM_IDS to albumIds,
                KEY_ALBUM_PROVIDERS to albumProviders,
                KEY_PLAYLIST_IDS to playlistIds,
                KEY_PLAYLIST_PROVIDERS to playlistProviders
            )
        )
    }

    fun scheduleArtistPrefetch(artistNames: List<String>) {
        val limited = artistNames.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_PREFETCH_ITEMS)
        if (limited.isEmpty()) return
        enqueueUniqueWork(
            uniqueName = WORK_NAME_ARTISTS,
            data = workDataOf(KEY_ARTIST_NAMES to limited.toTypedArray())
        )
    }

    private fun enqueueUniqueWork(uniqueName: String, data: Data) {
        val request = OneTimeWorkRequestBuilder<CachePrefetchWorker>()
            .setConstraints(buildConstraints())
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun buildConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private companion object {
        private const val MAX_PREFETCH_ITEMS = 50
        private const val WORK_NAME_ALBUM_TRACKS = "prefetch_album_tracks"
        private const val WORK_NAME_PLAYLIST_TRACKS = "prefetch_playlist_tracks"
        private const val WORK_NAME_DETAIL_PREFETCH = "prefetch_detail_cache"
        private const val WORK_NAME_ARTISTS = "prefetch_artist_cache"
    }
}
