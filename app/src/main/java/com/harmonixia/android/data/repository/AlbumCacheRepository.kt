package com.harmonixia.android.data.repository

import com.harmonixia.android.data.local.dao.CachedAlbumDao
import com.harmonixia.android.data.local.entity.CachedAlbumEntity
import com.harmonixia.android.data.paging.DEFAULT_FULL_FETCH_PAGE_SIZE
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.AlbumType
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class AlbumCacheRepository @Inject constructor(
    private val cachedAlbumDao: CachedAlbumDao,
    private val repository: MusicAssistantRepository,
    private val json: Json,
    ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val currentSyncId = AtomicLong(0L)
    private var prefetchJob: Job? = null
    private val _isCacheComplete = MutableStateFlow(false)
    val isCacheComplete: StateFlow<Boolean> = _isCacheComplete.asStateFlow()

    init {
        scope.launch {
            val count = cachedAlbumDao.getCount()
            val syncCount = cachedAlbumDao.getDistinctSyncIdCount()
            _isCacheComplete.value = count > 0 && syncCount <= 1
        }
    }

    fun observeCachedAlbums(): Flow<List<Album>> {
        return cachedAlbumDao.observeCachedAlbums()
            .map { entities -> entities.map { it.toAlbum() } }
    }

    suspend fun getCachedPage(offset: Int, limit: Int): CachedPage? {
        if (limit <= 0) return null
        val totalCount = cachedAlbumDao.getCount()
        if (totalCount == 0 || offset >= totalCount) return null
        if (!_isCacheComplete.value && offset + limit > totalCount) return null
        val entities = cachedAlbumDao.getPage(limit, offset)
        if (entities.isEmpty()) return null
        return CachedPage(entities.map { it.toAlbum() }, totalCount)
    }

    fun prefetchFromInitialPage(
        initialAlbums: List<Album>,
        startOffset: Int,
        force: Boolean = true
    ) {
        launchPrefetch(initialAlbums, startOffset, force)
    }

    fun prefetchIfIdle() {
        launchPrefetch(emptyList(), 0, force = false)
    }

    fun refresh() {
        launchPrefetch(emptyList(), 0, force = true)
    }

    @Synchronized
    private fun launchPrefetch(
        initialAlbums: List<Album>,
        startOffset: Int,
        force: Boolean
    ) {
        if (!force && prefetchJob?.isActive == true) return
        prefetchJob?.cancel()
        val syncId = System.currentTimeMillis()
        currentSyncId.set(syncId)
        prefetchJob = scope.launch {
            if (initialAlbums.isNotEmpty()) {
                upsertPage(initialAlbums, 0, syncId)
            }
            val completed = prefetchSequential(
                startOffset = startOffset.coerceAtLeast(0),
                pageSize = DEFAULT_FULL_FETCH_PAGE_SIZE,
                syncId = syncId
            )
            if (completed &&
                currentCoroutineContext().isActive &&
                currentSyncId.get() == syncId
            ) {
                cachedAlbumDao.deleteStale(syncId)
                _isCacheComplete.value = cachedAlbumDao.getCount() > 0
            }
        }
    }

    private suspend fun prefetchSequential(
        startOffset: Int,
        pageSize: Int,
        syncId: Long
    ): Boolean {
        var offset = startOffset
        val limit = pageSize.coerceAtLeast(1)
        val context = currentCoroutineContext()
        while (context.isActive) {
            val result = repository.fetchAlbums(limit, offset)
            val page = result.getOrElse { return false }
            if (page.isEmpty()) return true
            upsertPage(page, offset, syncId)
            offset += page.size
            if (page.size < limit) return true
        }
        return false
    }

    private suspend fun upsertPage(albums: List<Album>, offset: Int, syncId: Long) {
        val entities = albums.mapIndexed { index, album ->
            album.toCachedEntity(offset + index, syncId)
        }
        cachedAlbumDao.upsertAll(entities)
    }

    private fun CachedAlbumEntity.toAlbum(): Album {
        val artists = runCatching { json.decodeFromString<List<String>>(artistsJson) }
            .getOrDefault(emptyList())
        val parsedType = runCatching { AlbumType.valueOf(albumType) }
            .getOrDefault(AlbumType.UNKNOWN)
        return Album(
            itemId = itemId,
            provider = provider,
            uri = uri,
            name = name,
            artists = artists,
            imageUrl = imageUrl,
            albumType = parsedType,
            addedAt = addedAt,
            lastPlayed = lastPlayed,
            trackCount = trackCount
        )
    }

    private fun Album.toCachedEntity(sortIndex: Int, syncId: Long): CachedAlbumEntity {
        val cacheKey = "${provider.trim()}:${itemId.trim()}"
        val artistsPayload = runCatching { json.encodeToString(artists) }
            .getOrDefault("[]")
        return CachedAlbumEntity(
            cacheKey = cacheKey,
            itemId = itemId,
            provider = provider,
            uri = uri,
            name = name,
            artistsJson = artistsPayload,
            imageUrl = imageUrl,
            albumType = albumType.name,
            trackCount = trackCount,
            addedAt = addedAt,
            lastPlayed = lastPlayed,
            sortIndex = sortIndex,
            syncId = syncId
        )
    }

    data class CachedPage(
        val albums: List<Album>,
        val totalCount: Int
    )
}
