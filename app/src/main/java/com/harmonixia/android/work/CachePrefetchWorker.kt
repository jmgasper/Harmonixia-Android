package com.harmonixia.android.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class CachePrefetchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                CachePrefetchEntryPoint::class.java
            )
            val repository = entryPoint.repository()
            val albumIds = inputData.getStringArray(KEY_ALBUM_IDS) ?: emptyArray()
            val providers = inputData.getStringArray(KEY_ALBUM_PROVIDERS) ?: emptyArray()
            val artistNames = inputData.getStringArray(KEY_ARTIST_NAMES) ?: emptyArray()

            val albumCount = minOf(albumIds.size, providers.size, MAX_PREFETCH_ITEMS)
            for (index in 0 until albumCount) {
                repository.getAlbumTracks(albumIds[index], providers[index])
            }
            if (artistNames.isNotEmpty()) {
                repository.fetchArtists(ARTIST_PREFETCH_LIMIT, 0)
                repository.fetchAlbums(ALBUM_PREFETCH_LIMIT, 0)
            }
            Result.success()
        } catch (error: Exception) {
            Logger.w(TAG, "Prefetch worker failed", error)
            Result.retry()
        }
    }

    private companion object {
        private const val TAG = "CachePrefetchWorker"
        private const val MAX_PREFETCH_ITEMS = 50
        private const val ARTIST_PREFETCH_LIMIT = 50
        private const val ALBUM_PREFETCH_LIMIT = 50
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CachePrefetchEntryPoint {
    fun repository(): MusicAssistantRepository
}

const val KEY_ALBUM_IDS = "prefetch_album_ids"
const val KEY_ALBUM_PROVIDERS = "prefetch_album_providers"
const val KEY_ARTIST_NAMES = "prefetch_artist_names"
