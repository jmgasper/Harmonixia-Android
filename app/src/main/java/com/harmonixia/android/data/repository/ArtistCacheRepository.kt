package com.harmonixia.android.data.repository

import com.harmonixia.android.data.local.dao.CachedArtistDao
import com.harmonixia.android.data.local.entity.CachedArtistEntity
import com.harmonixia.android.data.paging.DEFAULT_FULL_FETCH_PAGE_SIZE
import com.harmonixia.android.domain.model.Artist
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

@Singleton
class ArtistCacheRepository @Inject constructor(
    private val cachedArtistDao: CachedArtistDao,
    private val repository: MusicAssistantRepository,
    ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val currentSyncId = AtomicLong(0L)
    private var prefetchJob: Job? = null
    private val _isCacheComplete = MutableStateFlow(false)
    val isCacheComplete: StateFlow<Boolean> = _isCacheComplete.asStateFlow()

    init {
        scope.launch {
            val count = cachedArtistDao.getCount()
            val syncCount = cachedArtistDao.getDistinctSyncIdCount()
            _isCacheComplete.value = count > 0 && syncCount <= 1
        }
    }

    fun observeCachedArtists(): Flow<List<Artist>> {
        return cachedArtistDao.observeCachedArtists()
            .map { entities -> entities.map { it.toArtist() } }
    }

    suspend fun getCachedPage(offset: Int, limit: Int): CachedPage? {
        if (limit <= 0) return null
        val totalCount = cachedArtistDao.getCount()
        if (totalCount == 0 || offset >= totalCount) return null
        if (!_isCacheComplete.value && offset + limit > totalCount) return null
        val entities = cachedArtistDao.getPage(limit, offset)
        if (entities.isEmpty()) return null
        return CachedPage(entities.map { it.toArtist() }, totalCount)
    }

    fun prefetchFromInitialPage(
        initialArtists: List<Artist>,
        startOffset: Int,
        force: Boolean = true
    ) {
        launchPrefetch(initialArtists, startOffset, force)
    }

    fun prefetchIfIdle() {
        launchPrefetch(emptyList(), 0, force = false)
    }

    fun refresh() {
        launchPrefetch(emptyList(), 0, force = true)
    }

    @Synchronized
    private fun launchPrefetch(
        initialArtists: List<Artist>,
        startOffset: Int,
        force: Boolean
    ) {
        if (!force && prefetchJob?.isActive == true) return
        prefetchJob?.cancel()
        val syncId = System.currentTimeMillis()
        currentSyncId.set(syncId)
        prefetchJob = scope.launch {
            if (initialArtists.isNotEmpty()) {
                upsertPage(initialArtists, 0, syncId)
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
                cachedArtistDao.deleteStale(syncId)
                _isCacheComplete.value = cachedArtistDao.getCount() > 0
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
            val result = repository.fetchArtists(limit, offset)
            val page = result.getOrElse { return false }
            if (page.isEmpty()) return true
            upsertPage(page, offset, syncId)
            offset += page.size
            if (page.size < limit) return true
        }
        return false
    }

    private suspend fun upsertPage(artists: List<Artist>, offset: Int, syncId: Long) {
        val entities = artists.mapIndexed { index, artist ->
            artist.toCachedEntity(offset + index, syncId)
        }
        cachedArtistDao.upsertAll(entities)
    }

    private fun CachedArtistEntity.toArtist(): Artist {
        return Artist(
            itemId = itemId,
            provider = provider,
            uri = uri,
            name = name,
            sortName = sortName,
            imageUrl = imageUrl
        )
    }

    private fun Artist.toCachedEntity(sortIndex: Int, syncId: Long): CachedArtistEntity {
        val cacheKey = "${provider.trim()}:${itemId.trim()}"
        return CachedArtistEntity(
            cacheKey = cacheKey,
            itemId = itemId,
            provider = provider,
            uri = uri,
            name = name,
            sortName = sortName,
            imageUrl = imageUrl,
            sortIndex = sortIndex,
            syncId = syncId
        )
    }

    data class CachedPage(
        val artists: List<Artist>,
        val totalCount: Int
    )
}
