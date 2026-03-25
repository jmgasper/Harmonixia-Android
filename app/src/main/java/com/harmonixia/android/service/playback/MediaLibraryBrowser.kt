package com.harmonixia.android.service.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import com.harmonixia.android.data.paging.fetchAllPages
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import com.harmonixia.android.util.mergeWithLocal
import com.harmonixia.android.util.replaceWithLocalMatches
import com.harmonixia.android.util.NetworkConnectivityManager
import com.harmonixia.android.util.Logger
import java.text.Collator
import com.harmonixia.android.util.buildPlaybackExtras
import com.harmonixia.android.util.playbackDurationMs
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@UnstableApi
class MediaLibraryBrowser(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val offlineLibraryRepository: OfflineLibraryRepository,
    private val networkConnectivityManager: NetworkConnectivityManager
) {
    private val prefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefetchLock = Any()
    @Volatile
    private var libraryPrefetchJob: Job? = null
    @Volatile
    private var fullAlbums: List<Album>? = null
    @Volatile
    private var fullArtists: List<Artist>? = null
    private val artistNameCache = object : LinkedHashMap<String, String>(
        ARTIST_NAME_CACHE_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > ARTIST_NAME_CACHE_SIZE
        }
    }
    private val playlistUriCache = object : LinkedHashMap<String, String>(
        PLAYLIST_URI_CACHE_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > PLAYLIST_URI_CACHE_SIZE
        }
    }
    private val mediaItemCache = object : LinkedHashMap<String, MediaItem>(
        MEDIA_ITEM_CACHE_SIZE,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, MediaItem>?
        ): Boolean {
            return size > MEDIA_ITEM_CACHE_SIZE
        }
    }

    suspend fun getLibraryRoot(extras: Bundle?): MediaItem {
        ensureFullLibraryPrefetch()
        return buildRootItem()
    }

    suspend fun getChildren(
        parentId: String,
        page: Int,
        pageSize: Int,
        useAutoBuckets: Boolean
    ): List<MediaItem> {
        return when (parentId) {
            MEDIA_ID_ROOT -> buildRootCategories(useAutoBuckets)
            MEDIA_ID_HOME -> buildHomeCategories()
            MEDIA_ID_HOME_RECENTLY_PLAYED -> buildHomeRecentlyPlayed(page, pageSize)
            MEDIA_ID_HOME_FAVORITES -> buildHomeFavorites(page, pageSize)
            MEDIA_ID_HOME_NEW_ALBUMS -> buildHomeNewAlbums(page, pageSize)
            MEDIA_ID_HOME_PLAYLISTS -> buildPlaylistsList(page, pageSize)
            MEDIA_ID_ALBUMS -> if (useAutoBuckets) {
                buildLetterCategoryItems(
                    MEDIA_ID_PREFIX_ALBUMS_LETTER,
                    childBrowsableContentStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                )
            } else {
                buildAlbumsList(page, pageSize)
            }
            MEDIA_ID_ARTISTS -> if (useAutoBuckets) {
                buildLetterCategoryItems(MEDIA_ID_PREFIX_ARTISTS_LETTER)
            } else {
                buildArtistsList(page, pageSize)
            }
            MEDIA_ID_PLAYLISTS -> buildPlaylistsList(page, pageSize)
            MEDIA_ID_LOCAL_MEDIA -> buildLocalContent(useAutoBuckets)
            MEDIA_ID_LOCAL_ALBUMS -> if (useAutoBuckets) {
                buildLetterCategoryItems(
                    MEDIA_ID_PREFIX_LOCAL_ALBUMS_LETTER,
                    childBrowsableContentStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                )
            } else {
                buildLocalAlbumsList(page, pageSize)
            }
            MEDIA_ID_LOCAL_ARTISTS -> if (useAutoBuckets) {
                buildLetterCategoryItems(MEDIA_ID_PREFIX_LOCAL_ARTISTS_LETTER)
            } else {
                buildLocalArtistsList(page, pageSize)
            }
            MEDIA_ID_LOCAL_TRACKS -> buildLocalTracksList(page, pageSize)
            else -> {
                when {
                    parentId.startsWith("$MEDIA_ID_PREFIX_ARTISTS_LETTER:") -> {
                        val bucket = parseLetterBucket(MEDIA_ID_PREFIX_ARTISTS_LETTER, parentId)
                            ?: return emptyList()
                        buildArtistsByBucket(bucket, useLocal = false)
                    }
                    parentId.startsWith("$MEDIA_ID_PREFIX_ALBUMS_LETTER:") -> {
                        val bucket = parseLetterBucket(MEDIA_ID_PREFIX_ALBUMS_LETTER, parentId)
                            ?: return emptyList()
                        buildAlbumsByBucket(bucket, useLocal = false)
                    }
                    parentId.startsWith("$MEDIA_ID_PREFIX_LOCAL_ARTISTS_LETTER:") -> {
                        val bucket = parseLetterBucket(MEDIA_ID_PREFIX_LOCAL_ARTISTS_LETTER, parentId)
                            ?: return emptyList()
                        buildArtistsByBucket(bucket, useLocal = true)
                    }
                    parentId.startsWith("$MEDIA_ID_PREFIX_LOCAL_ALBUMS_LETTER:") -> {
                        val bucket = parseLetterBucket(MEDIA_ID_PREFIX_LOCAL_ALBUMS_LETTER, parentId)
                            ?: return emptyList()
                        buildAlbumsByBucket(bucket, useLocal = true)
                    }
                    parentId.startsWith("$MEDIA_ID_PREFIX_ALBUM:") -> {
                        val (albumId, provider) =
                            parseQualifiedId(MEDIA_ID_PREFIX_ALBUM, parentId) ?: return emptyList()
                        buildAlbumTracks(albumId, provider, page, pageSize)
                    }
                    parentId.startsWith("$MEDIA_ID_PREFIX_PLAYLIST:") -> {
                        val (playlistId, provider) =
                            parseQualifiedId(MEDIA_ID_PREFIX_PLAYLIST, parentId) ?: return emptyList()
                        buildPlaylistTracks(playlistId, provider, page, pageSize)
                    }
                    parentId.startsWith("$MEDIA_ID_PREFIX_ARTIST:") -> {
                        val (artistId, provider) =
                            parseQualifiedId(MEDIA_ID_PREFIX_ARTIST, parentId) ?: return emptyList()
                        val cachedName = synchronized(artistNameCache) {
                            artistNameCache[artistCacheKey(artistId, provider)].orEmpty()
                        }
                        buildArtistAlbums(artistId, provider, cachedName)
                    }
                    else -> emptyList()
                }
            }
        }
    }

    suspend fun getSearchResults(query: String): SearchResults {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return SearchResults()
        return if (isOfflineMode()) {
            offlineLibraryRepository.searchDownloadedContent(trimmed).first()
        } else {
            val serverResults = repository.searchLibrary(
                trimmed,
                SEARCH_LIMIT,
                libraryOnly = false
            )
                .getOrDefault(SearchResults())
            cachePlaylists(serverResults.playlists)
            val localTracks = localMediaRepository.searchTracks(trimmed).first()
            val localAlbums = localMediaRepository.searchAlbums(trimmed).first()
            val localArtists = localMediaRepository.searchArtists(trimmed).first()
            SearchResults(
                albums = serverResults.albums.mergeWithLocal(localAlbums),
                artists = serverResults.artists.mergeWithLocal(localArtists),
                playlists = serverResults.playlists,
                tracks = serverResults.tracks.mergeWithLocal(localTracks)
            )
        }
    }

    suspend fun search(query: String, page: Int, pageSize: Int): List<MediaItem> {
        val results = getSearchResults(query)
        val items = mutableListOf<MediaItem>()
        results.albums.forEach { items.add(it.toBrowsableMediaItem()) }
        results.artists.forEach {
            cacheArtistName(it)
            items.add(it.toBrowsableMediaItem())
        }
        results.playlists.forEach { items.add(it.toBrowsableMediaItem()) }
        for (track in results.tracks) {
            items.add(track.toPlayableMediaItem())
        }
        return applyPaging(items, page, pageSize)
    }

    fun resolveMediaItem(mediaId: String): MediaItem? {
        if (mediaId.isBlank()) return null
        return synchronized(mediaItemCache) { mediaItemCache[mediaId] }
    }

    fun resolvePlaylistUri(parentMediaId: String): String? {
        val (playlistId, provider) =
            parseQualifiedId(MEDIA_ID_PREFIX_PLAYLIST, parentMediaId) ?: return null
        val key = playlistCacheKey(playlistId, provider)
        val cached = synchronized(playlistUriCache) { playlistUriCache[key] }
        return cached?.takeIf { it.isNotBlank() }
            ?: repository.getCachedPlaylist(playlistId, provider)?.uri?.takeIf { it.isNotBlank() }
    }

    suspend fun getParentTrackItems(parentMediaId: String): List<MediaItem> {
        return when {
            parentMediaId == MEDIA_ID_HOME_FAVORITES -> {
                buildHomeFavoritesTrackItems()
            }
            parentMediaId.startsWith("$MEDIA_ID_PREFIX_ALBUM:") -> {
                val (albumId, provider) =
                    parseQualifiedId(MEDIA_ID_PREFIX_ALBUM, parentMediaId) ?: return emptyList()
                buildAlbumTrackItems(albumId, provider)
            }
            parentMediaId.startsWith("$MEDIA_ID_PREFIX_PLAYLIST:") -> {
                val (playlistId, provider) =
                    parseQualifiedId(MEDIA_ID_PREFIX_PLAYLIST, parentMediaId) ?: return emptyList()
                buildPlaylistTrackItems(playlistId, provider)
            }
            else -> emptyList()
        }
    }

    private fun buildRootItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(ROOT_TITLE)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId(MEDIA_ID_ROOT)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun buildRootCategories(useAutoBuckets: Boolean): List<MediaItem> {
        val letterGridStyle = if (useAutoBuckets) {
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
        } else {
            null
        }
        val albumsStyle = if (useAutoBuckets) {
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
        } else {
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
        }
        return if (isOfflineMode()) {
            listOf(buildCategoryItem(MEDIA_ID_LOCAL_MEDIA, TITLE_LOCAL_MEDIA))
        } else {
            listOf(
                buildCategoryItem(
                    MEDIA_ID_HOME,
                    TITLE_HOME,
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                    folderType = MediaMetadata.FOLDER_TYPE_MIXED
                ),
                buildCategoryItem(
                    MEDIA_ID_ALBUMS,
                    TITLE_ALBUMS,
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                    folderType = MediaMetadata.FOLDER_TYPE_ALBUMS,
                    browsableContentStyle = albumsStyle
                ),
                buildCategoryItem(
                    MEDIA_ID_ARTISTS,
                    TITLE_ARTISTS,
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                    folderType = MediaMetadata.FOLDER_TYPE_ARTISTS,
                    browsableContentStyle = letterGridStyle
                ),
                buildCategoryItem(
                    MEDIA_ID_PLAYLISTS,
                    TITLE_PLAYLISTS,
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                    folderType = MediaMetadata.FOLDER_TYPE_PLAYLISTS,
                    browsableContentStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                ),
                buildCategoryItem(
                    MEDIA_ID_LOCAL_MEDIA,
                    TITLE_LOCAL_MEDIA,
                    mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                    folderType = MediaMetadata.FOLDER_TYPE_MIXED
                )
            )
        }
    }

    private fun buildHomeCategories(): List<MediaItem> {
        return listOf(
            buildCategoryItem(
                MEDIA_ID_HOME_RECENTLY_PLAYED,
                TITLE_HOME_RECENTLY_PLAYED,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                folderType = MediaMetadata.FOLDER_TYPE_ALBUMS,
                browsableContentStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            ),
            buildCategoryItem(
                MEDIA_ID_HOME_FAVORITES,
                TITLE_HOME_FAVORITES,
                mediaType = MediaMetadata.MEDIA_TYPE_MIXED,
                folderType = MediaMetadata.FOLDER_TYPE_TITLES
            ),
            buildCategoryItem(
                MEDIA_ID_HOME_NEW_ALBUMS,
                TITLE_HOME_NEW_ALBUMS,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                folderType = MediaMetadata.FOLDER_TYPE_ALBUMS,
                browsableContentStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            ),
            buildCategoryItem(
                MEDIA_ID_HOME_PLAYLISTS,
                TITLE_PLAYLISTS,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                folderType = MediaMetadata.FOLDER_TYPE_PLAYLISTS,
                browsableContentStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            )
        )
    }

    private fun buildCategoryItem(
        mediaId: String,
        title: String,
        subtitle: String? = null,
        artworkUrl: String? = null,
        mediaType: Int? = null,
        folderType: Int? = null,
        browsableContentStyle: Int? = null,
        playableContentStyle: Int? = null
    ): MediaItem {
        val resolvedArtworkUrl = resolveAutoArtworkUrl(artworkUrl)
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(resolvedArtworkUrl?.let { Uri.parse(it) })
            .setIsBrowsable(true)
            .setIsPlayable(false)
        if (mediaType != null) {
            metadataBuilder.setMediaType(mediaType)
        }
        @Suppress("DEPRECATION")
        if (folderType != null) {
            metadataBuilder.setFolderType(folderType)
        }
        val styleExtras = buildContentStyleExtras(
            browsableContentStyle = browsableContentStyle,
            playableContentStyle = playableContentStyle
        )
        if (styleExtras != null) {
            metadataBuilder.setExtras(styleExtras)
        }
        val metadata = metadataBuilder.build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun buildAlbumsList(page: Int, pageSize: Int): List<MediaItem> {
        val offline = isOfflineMode()
        val albums = if (offline) {
            localMediaRepository.getAllAlbums().first()
        } else {
            val fullList = loadFullAlbums()
            val localAlbums = localMediaRepository.getAllAlbums().first()
            fullList.mergeWithLocal(localAlbums)
        }
        val sortedAlbums = albums.sortedWith(AlbumAlphabeticalComparator)
        logAlphabetDistribution("albums", sortedAlbums.map { it.name })
        val includeArtwork = shouldIncludeArtwork(sortedAlbums.size)
        val compactMetadata = shouldUseCompactMetadata(sortedAlbums.size)
        if (!includeArtwork) {
            Logger.d(TAG, "Auto browse albums omitting artwork count=${sortedAlbums.size}")
        }
        if (compactMetadata) {
            Logger.d(TAG, "Auto browse albums using compact metadata count=${sortedAlbums.size}")
        }
        val paged = if (offline) applyPaging(sortedAlbums, page, pageSize) else sortedAlbums
        return paged.map {
            it.toBrowsableMediaItem(
                includeArtwork = includeArtwork,
                includeSubtitle = !compactMetadata,
                includeDisplayTitle = !compactMetadata
            )
        }
    }

    private suspend fun buildArtistsList(page: Int, pageSize: Int): List<MediaItem> {
        return if (isOfflineMode()) {
            buildOfflineArtistsList(page, pageSize)
        } else {
            val fullList = loadFullArtists()
            val localArtists = localMediaRepository.getAllArtists().first()
            val mergedArtists = fullList.mergeWithLocal(localArtists)
                .sortedWith(ArtistAlphabeticalComparator)
            mergedArtists.forEach { cacheArtistName(it) }
            logAlphabetDistribution("artists", mergedArtists.map { it.name })
            val includeArtwork = shouldIncludeArtwork(mergedArtists.size)
            if (!includeArtwork) {
                Logger.d(TAG, "Auto browse artists omitting artwork count=${mergedArtists.size}")
            }
            val compactMetadata = shouldUseCompactMetadata(mergedArtists.size)
            if (compactMetadata) {
                Logger.d(TAG, "Auto browse artists using compact metadata count=${mergedArtists.size}")
            }
            mergedArtists.map {
                it.toBrowsableMediaItem(
                    includeArtwork = includeArtwork,
                    includeSubtitle = !compactMetadata,
                    includeDisplayTitle = !compactMetadata
                )
            }
        }
    }

    private suspend fun buildPlaylistsList(page: Int, pageSize: Int): List<MediaItem> {
        val playlists = if (isOfflineMode()) {
            emptyList()
        } else {
            val (offset, limit) = resolvePaging(page, pageSize)
            repository.fetchPlaylists(limit, offset).getOrDefault(emptyList())
        }
        cachePlaylists(playlists)
        val paged = if (isOfflineMode()) applyPaging(playlists, page, pageSize) else playlists
        return paged.map { it.toBrowsableMediaItem() }
    }

    private fun buildLocalContent(useAutoBuckets: Boolean): List<MediaItem> {
        val letterGridStyle = if (useAutoBuckets) {
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
        } else {
            null
        }
        val albumsStyle = if (useAutoBuckets) {
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
        } else {
            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
        }
        return listOf(
            buildCategoryItem(
                MEDIA_ID_LOCAL_ALBUMS,
                TITLE_LOCAL_ALBUMS,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                folderType = MediaMetadata.FOLDER_TYPE_ALBUMS,
                browsableContentStyle = albumsStyle
            ),
            buildCategoryItem(
                MEDIA_ID_LOCAL_ARTISTS,
                TITLE_LOCAL_ARTISTS,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                folderType = MediaMetadata.FOLDER_TYPE_ARTISTS,
                browsableContentStyle = letterGridStyle
            ),
            buildCategoryItem(
                MEDIA_ID_LOCAL_TRACKS,
                TITLE_LOCAL_TRACKS,
                mediaType = MediaMetadata.MEDIA_TYPE_MIXED,
                folderType = MediaMetadata.FOLDER_TYPE_TITLES
            )
        )
    }

    private fun buildLetterCategoryItems(
        prefix: String,
        childBrowsableContentStyle: Int? = null
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val letters = listOf("123") + ('A'..'Z').map { it.toString() } + "#"
        for (letter in letters) {
            items.add(
                buildCategoryItem(
                    mediaId = "$prefix:$letter",
                    title = letter,
                    browsableContentStyle = childBrowsableContentStyle
                )
            )
        }
        return items
    }

    private fun parseLetterBucket(prefix: String, mediaId: String): String? {
        val token = mediaId.substringAfter("$prefix:", "")
        if (token.isBlank()) return null
        return token.uppercase()
    }

    private fun bucketForName(name: String): String {
        val trimmed = name.trim()
        val first = trimmed.firstOrNull() ?: return "#"
        return when {
            first.isDigit() -> "123"
            first.isLetter() -> first.uppercaseChar().toString()
            else -> "#"
        }
    }

    private suspend fun buildHomeRecentlyPlayed(page: Int, pageSize: Int): List<MediaItem> {
        if (isOfflineMode()) return emptyList()
        val limit = resolveHomeLimit(page, pageSize)
        val albums = repository.fetchRecentlyPlayed(limit).getOrDefault(emptyList())
        val paged = applyPaging(albums, page, pageSize)
        return paged.map { it.toBrowsableMediaItem() }
    }

    private suspend fun buildHomeFavorites(page: Int, pageSize: Int): List<MediaItem> {
        if (isOfflineMode()) return emptyList()
        val (offset, limit) = resolvePaging(page, pageSize)
        val favorites = repository.fetchFavorites(limit, offset).getOrDefault(emptyList())
        val items = mutableListOf<MediaItem>()
        for (track in favorites) {
            items.add(track.toPlayableMediaItem(MEDIA_ID_HOME_FAVORITES))
        }
        return items
    }

    private suspend fun buildHomeFavoritesTrackItems(): List<MediaItem> {
        if (isOfflineMode()) return emptyList()
        val favorites = repository.fetchFavorites(FAVORITES_QUEUE_LIMIT, 0)
            .getOrDefault(emptyList())
        val items = mutableListOf<MediaItem>()
        for (track in favorites) {
            items.add(track.toPlayableMediaItem(MEDIA_ID_HOME_FAVORITES))
        }
        return items
    }

    private suspend fun buildHomeNewAlbums(page: Int, pageSize: Int): List<MediaItem> {
        if (isOfflineMode()) return emptyList()
        val limit = resolveHomeLimit(page, pageSize)
        val albums = repository.fetchRecentlyAdded(limit).getOrDefault(emptyList())
        val paged = applyPaging(albums, page, pageSize)
        return paged.map { it.toBrowsableMediaItem() }
    }

    private suspend fun buildLocalAlbumsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = localMediaRepository.getAllAlbums().first()
            .sortedWith(AlbumAlphabeticalComparator)
        val includeArtwork = shouldIncludeArtwork(albums.size)
        if (!includeArtwork) {
            Logger.d(TAG, "Auto browse local albums omitting artwork count=${albums.size}")
        }
        val compactMetadata = shouldUseCompactMetadata(albums.size)
        if (compactMetadata) {
            Logger.d(TAG, "Auto browse local albums using compact metadata count=${albums.size}")
        }
        return applyPaging(albums, page, pageSize).map {
            it.toBrowsableMediaItem(
                includeArtwork = includeArtwork,
                includeSubtitle = !compactMetadata,
                includeDisplayTitle = !compactMetadata
            )
        }
    }

    private suspend fun buildLocalArtistsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = localMediaRepository.getAllAlbums().first()
        val tracks = localMediaRepository.getAllTracks().first()
        val artists = buildOfflineArtists(albums, tracks)
            .sortedWith(ArtistAlphabeticalComparator)
        artists.forEach { cacheArtistName(it) }
        val includeArtwork = shouldIncludeArtwork(artists.size)
        if (!includeArtwork) {
            Logger.d(TAG, "Auto browse local artists omitting artwork count=${artists.size}")
        }
        val compactMetadata = shouldUseCompactMetadata(artists.size)
        if (compactMetadata) {
            Logger.d(TAG, "Auto browse local artists using compact metadata count=${artists.size}")
        }
        return applyPaging(artists, page, pageSize).map {
            it.toBrowsableMediaItem(
                includeArtwork = includeArtwork,
                includeSubtitle = !compactMetadata,
                includeDisplayTitle = !compactMetadata
            )
        }
    }

    private suspend fun buildLocalTracksList(page: Int, pageSize: Int): List<MediaItem> {
        val tracks = localMediaRepository.getAllTracks().first()
        val paged = applyPaging(tracks, page, pageSize)
        val items = mutableListOf<MediaItem>()
        for (track in paged) {
            items.add(track.toPlayableMediaItem())
        }
        return items
    }

    private suspend fun buildArtistsByBucket(bucket: String, useLocal: Boolean): List<MediaItem> {
        val resolvedBucket = bucket.uppercase()
        val artists = if (useLocal || isOfflineMode()) {
            val albums = localMediaRepository.getAllAlbums().first()
            val tracks = localMediaRepository.getAllTracks().first()
            buildOfflineArtists(albums, tracks).sortedWith(ArtistAlphabeticalComparator)
        } else {
            val fullList = loadFullArtists()
            val localArtists = localMediaRepository.getAllArtists().first()
            fullList.mergeWithLocal(localArtists).sortedWith(ArtistAlphabeticalComparator)
        }
        val filtered = artists.filter { bucketForName(it.name) == resolvedBucket }
        val includeArtwork = shouldIncludeArtwork(filtered.size)
        val compactMetadata = shouldUseCompactMetadata(filtered.size)
        return filtered.map {
            it.toBrowsableMediaItem(
                includeArtwork = includeArtwork,
                includeSubtitle = !compactMetadata,
                includeDisplayTitle = !compactMetadata
            )
        }
    }

    private suspend fun buildAlbumsByBucket(bucket: String, useLocal: Boolean): List<MediaItem> {
        val resolvedBucket = bucket.uppercase()
        val albums = if (useLocal || isOfflineMode()) {
            localMediaRepository.getAllAlbums().first()
                .sortedWith(AlbumAlphabeticalComparator)
        } else {
            val fullList = loadFullAlbums()
            val localAlbums = localMediaRepository.getAllAlbums().first()
            fullList.mergeWithLocal(localAlbums).sortedWith(AlbumAlphabeticalComparator)
        }
        val filtered = albums.filter { bucketForName(it.name) == resolvedBucket }
        val includeArtwork = shouldIncludeArtwork(filtered.size)
        val compactMetadata = shouldUseCompactMetadata(filtered.size)
        return filtered.map {
            it.toBrowsableMediaItem(
                includeArtwork = includeArtwork,
                includeSubtitle = !compactMetadata,
                includeDisplayTitle = !compactMetadata
            )
        }
    }

    private suspend fun buildAlbumTracks(
        albumId: String,
        provider: String,
        page: Int,
        pageSize: Int
    ): List<MediaItem> {
        val parentMediaId = albumMediaId(albumId, provider)
        return if (isOfflineMode()) {
            val (albumName, albumArtist) = decodeOfflineAlbumId(albumId)
            val tracks = localMediaRepository.getTracksByAlbum(albumName, albumArtist).first()
            val paged = applyPaging(tracks, page, pageSize)
            paged.map { it.toPlayableMediaItem(parentMediaId) }
        } else {
            val tracks = repository.getAlbumTracks(albumId, provider).getOrDefault(emptyList())
            val localTracks = loadLocalTracksForAlbum(albumId, provider, tracks)
            val mergedTracks = tracks.mergeWithLocal(localTracks)
            val paged = applyPaging(mergedTracks, page, pageSize)
            val items = mutableListOf<MediaItem>()
            for (track in paged) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
            items
        }
    }

    private suspend fun buildPlaylistTracks(
        playlistId: String,
        provider: String,
        page: Int,
        pageSize: Int
    ): List<MediaItem> {
        val parentMediaId = playlistMediaId(playlistId, provider)
        return if (isOfflineMode()) {
            emptyList()
        } else {
            val tracks = repository.getPlaylistTracks(playlistId, provider).getOrDefault(emptyList())
            val localTracks = localMediaRepository.getAllTracks().first()
            val mergedTracks = tracks.replaceWithLocalMatches(localTracks)
            val paged = applyPaging(mergedTracks, page, pageSize)
            val items = mutableListOf<MediaItem>()
            for (track in paged) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
            items
        }
    }

    private suspend fun buildArtistAlbums(
        artistId: String,
        provider: String,
        artistName: String
    ): List<MediaItem> {
        val resolvedName = resolveArtistName(artistId, provider, artistName)
        if (resolvedName.isBlank()) return emptyList()
        val albums = if (isOfflineMode()) {
            localMediaRepository.getAlbumsByArtist(resolvedName).first()
        } else {
            val allAlbums = loadFullAlbums()
            val localAlbums = localMediaRepository.getAllAlbums().first()
            val mergedAlbums = allAlbums.mergeWithLocal(localAlbums)
            filterAlbumsForArtist(mergedAlbums, resolvedName)
        }
        return albums.map { it.toBrowsableMediaItem() }
    }

    private fun isOfflineMode(): Boolean = networkConnectivityManager.isOfflineMode()

    private fun ensureFullLibraryPrefetch() {
        if (isOfflineMode()) return
        val albumsLoaded = fullAlbums != null
        val artistsLoaded = fullArtists != null
        if (albumsLoaded && artistsLoaded) return
        synchronized(prefetchLock) {
            if (libraryPrefetchJob?.isActive == true) return
            val shouldLoadAlbums = fullAlbums == null
            val shouldLoadArtists = fullArtists == null
            if (!shouldLoadAlbums && !shouldLoadArtists) return
            libraryPrefetchJob = prefetchScope.launch {
                supervisorScope {
                    val albumsDeferred = if (shouldLoadAlbums) {
                        async {
                            fetchAllPages(
                                pageSize = ALBUM_PAGE_LIMIT,
                                fetchPage = { offset, limit -> repository.fetchAlbums(limit, offset) }
                            )
                        }
                    } else {
                        null
                    }
                    val artistsDeferred = if (shouldLoadArtists) {
                        async {
                            fetchAllPages(
                                pageSize = ARTIST_PAGE_LIMIT,
                                fetchPage = { offset, limit -> repository.fetchArtists(limit, offset) }
                            )
                        }
                    } else {
                        null
                    }

                    val albumsResult = albumsDeferred?.let { runCatching { it.await() } }
                    val artistsResult = artistsDeferred?.let { runCatching { it.await() } }

                    albumsResult?.onSuccess { fullAlbums = it }
                    artistsResult?.onSuccess {
                        fullArtists = it
                        it.forEach { artist -> cacheArtistName(artist) }
                    }
                }
            }
        }
    }

    private suspend fun awaitFullLibraryPrefetch() {
        if (isOfflineMode()) return
        ensureFullLibraryPrefetch()
        libraryPrefetchJob?.join()
    }

    private suspend fun loadFullAlbums(): List<Album> {
        if (isOfflineMode()) return emptyList()
        awaitFullLibraryPrefetch()
        val cached = fullAlbums
        if (cached != null) return cached
        val fetched = fetchAllAlbums()
        fullAlbums = fetched
        return fetched
    }

    private suspend fun loadFullArtists(): List<Artist> {
        if (isOfflineMode()) return emptyList()
        awaitFullLibraryPrefetch()
        val cached = fullArtists
        if (cached != null) return cached
        val fetched = fetchAllArtists()
        fullArtists = fetched
        fetched.forEach { cacheArtistName(it) }
        return fetched
    }

    private fun cacheArtistName(artist: Artist) {
        val key = artistCacheKey(artist.itemId, artist.provider)
        synchronized(artistNameCache) {
            artistNameCache[key] = artist.name
        }
    }

    private fun cachePlaylists(playlists: List<Playlist>) {
        playlists.forEach { playlist ->
            val uri = playlist.uri.trim()
            if (uri.isNotBlank()) {
                val key = playlistCacheKey(playlist.itemId, playlist.provider)
                synchronized(playlistUriCache) {
                    playlistUriCache[key] = uri
                }
            }
        }
    }

    private fun artistCacheKey(artistId: String, provider: String): String {
        return "$provider:$artistId"
    }

    private fun playlistCacheKey(playlistId: String, provider: String): String {
        return "$provider:$playlistId"
    }

    private suspend fun resolveArtistName(
        artistId: String,
        provider: String,
        cachedName: String
    ): String {
        if (cachedName.isNotBlank()) return cachedName
        val key = artistCacheKey(artistId, provider)
        val cached = synchronized(artistNameCache) { artistNameCache[key] }
        if (!cached.isNullOrBlank()) return cached
        val prefetchedArtists = fullArtists
        if (prefetchedArtists != null) {
            val match = prefetchedArtists.firstOrNull {
                it.itemId == artistId && it.provider == provider
            }
            if (match != null) {
                cacheArtistName(match)
                return match.name
            }
        }
        if (provider == OFFLINE_PROVIDER) {
            return Uri.decode(artistId)
        }
        var offset = 0
        while (true) {
            val page = repository.fetchArtists(ARTIST_PAGE_LIMIT, offset).getOrDefault(emptyList())
            if (page.isEmpty()) break
            val match = page.firstOrNull { it.itemId == artistId && it.provider == provider }
            if (match != null) {
                cacheArtistName(match)
                return match.name
            }
            if (page.size < ARTIST_PAGE_LIMIT) break
            offset += ARTIST_PAGE_LIMIT
        }
        return ""
    }

    private fun parseQualifiedId(prefix: String, mediaId: String): Pair<String, String>? {
        val parts = mediaId.split(":", limit = 3)
        if (parts.size < 3 || parts[0] != prefix) return null
        val id = parts[1]
        val provider = parts[2]
        if (id.isBlank() || provider.isBlank()) return null
        return id to provider
    }

    private suspend fun fetchAllAlbums(): List<Album> {
        val albums = mutableListOf<Album>()
        var offset = 0
        while (true) {
            val page = repository.fetchAlbums(ALBUM_PAGE_LIMIT, offset).getOrDefault(emptyList())
            if (page.isEmpty()) break
            albums.addAll(page)
            if (page.size < ALBUM_PAGE_LIMIT) break
            offset += ALBUM_PAGE_LIMIT
        }
        return albums
    }

    private suspend fun fetchAllArtists(): List<Artist> {
        val artists = mutableListOf<Artist>()
        var offset = 0
        while (true) {
            val page = repository.fetchArtists(ARTIST_PAGE_LIMIT, offset).getOrDefault(emptyList())
            if (page.isEmpty()) break
            artists.addAll(page)
            if (page.size < ARTIST_PAGE_LIMIT) break
            offset += ARTIST_PAGE_LIMIT
        }
        return artists
    }

    private fun filterAlbumsForArtist(albums: List<Album>, artistName: String): List<Album> {
        val normalized = normalizeName(artistName)
        if (normalized.isBlank()) return emptyList()
        return albums.filter { album ->
            album.artists.any { name -> normalizeName(name) == normalized }
        }
    }

    private fun decodeOfflineAlbumId(albumId: String): Pair<String, String> {
        val decoded = Uri.decode(albumId)
        val parts = decoded.split(":", limit = 2)
        return if (parts.size == 2) {
            parts[1] to parts[0]
        } else {
            decoded to ""
        }
    }

    private fun shouldIncludeArtwork(count: Int): Boolean {
        return count <= AUTO_ARTWORK_LIMIT
    }

    private fun shouldUseCompactMetadata(count: Int): Boolean {
        return count > AUTO_COMPACT_METADATA_LIMIT
    }

    private fun normalizeName(name: String?): String {
        return name?.trim()?.lowercase().orEmpty()
    }

    private fun resolveAutoArtworkUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return trimmed
        val encodedPath = uri.encodedPath ?: return trimmed
        if (!encodedPath.endsWith("/imageproxy")) return trimmed
        val pathParam = uri.getQueryParameter("path")?.trim().orEmpty()
        if (pathParam.isBlank()) return trimmed
        if (pathParam.startsWith("http://") || pathParam.startsWith("https://")) {
            return pathParam
        }
        val provider = uri.getQueryParameter("provider").orEmpty()
        if (provider == "builtin") {
            val base = trimmed.substringBefore("/imageproxy")
            val normalizedPath = pathParam.trimStart('/')
            if (normalizedPath.isNotBlank()) {
                return "$base/$normalizedPath"
            }
        }
        return trimmed
    }

    private fun buildContentStyleExtras(
        browsableContentStyle: Int?,
        playableContentStyle: Int?
    ): Bundle? {
        if (browsableContentStyle == null && playableContentStyle == null) return null
        return Bundle().apply {
            browsableContentStyle?.let {
                putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, it)
            }
            playableContentStyle?.let {
                putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, it)
            }
        }
    }

    private fun logAlphabetDistribution(label: String, names: List<String>) {
        if (names.isEmpty()) return
        val counts = IntArray(26)
        val firstIndexes = IntArray(26) { -1 }
        var other = 0
        for ((index, name) in names.withIndex()) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) continue
            val first = trimmed.first().uppercaseChar()
            if (first in 'A'..'Z') {
                val bucket = first - 'A'
                counts[bucket] += 1
                if (firstIndexes[bucket] == -1) {
                    firstIndexes[bucket] = index
                }
            } else {
                other += 1
            }
        }
        val missing = buildString {
            for (index in counts.indices) {
                if (counts[index] == 0) append(('A'.code + index).toChar())
            }
        }
        val firstIndexSummary = buildString {
            for (index in firstIndexes.indices) {
                if (index > 0) append(",")
                val char = ('A'.code + index).toChar()
                append(char)
                append("=")
                append(firstIndexes[index])
            }
        }
        Logger.d(
            TAG,
            "Auto alpha $label total=${names.size} other=$other missing=$missing firstIndex=$firstIndexSummary"
        )
    }

    private fun artistSortKey(artist: Artist): String {
        return artist.name.trim()
    }

    private fun albumSortKey(album: Album): String {
        return album.name.trim()
    }

    private fun compareTitles(first: String, second: String): Int {
        val collator = titleCollator.get()
        return collator.compare(first, second)
    }

    private suspend fun buildOfflineArtistsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = localMediaRepository.getAllAlbums().first()
        val tracks = localMediaRepository.getAllTracks().first()
        val artists = buildOfflineArtists(albums, tracks)
            .sortedWith(ArtistAlphabeticalComparator)
        artists.forEach { cacheArtistName(it) }
        val includeArtwork = shouldIncludeArtwork(artists.size)
        if (!includeArtwork) {
            Logger.d(TAG, "Auto browse offline artists omitting artwork count=${artists.size}")
        }
        val compactMetadata = shouldUseCompactMetadata(artists.size)
        if (compactMetadata) {
            Logger.d(TAG, "Auto browse offline artists using compact metadata count=${artists.size}")
        }
        return applyPaging(artists, page, pageSize).map {
            it.toBrowsableMediaItem(
                includeArtwork = includeArtwork,
                includeSubtitle = !compactMetadata,
                includeDisplayTitle = !compactMetadata
            )
        }
    }

    private fun buildOfflineArtists(albums: List<Album>, tracks: List<Track>): List<Artist> {
        val artistsByName = linkedMapOf<String, Pair<String, String?>>()
        for (album in albums) {
            for (artist in album.artists) {
                val normalized = normalizeName(artist)
                if (normalized.isBlank()) continue
                if (!artistsByName.containsKey(normalized)) {
                    artistsByName[normalized] = artist to album.imageUrl
                }
            }
        }
        for (track in tracks) {
            val normalized = normalizeName(track.artist)
            if (normalized.isBlank()) continue
            if (!artistsByName.containsKey(normalized)) {
                artistsByName[normalized] = track.artist to track.imageUrl
            }
        }
        return artistsByName.values
            .map { (name, imageUrl) -> createOfflineArtist(name, imageUrl) }
    }

    private fun createOfflineArtist(name: String, imageUrl: String?): Artist {
        val trimmed = name.trim()
        val encodedId = Uri.encode(trimmed)
        return Artist(
            itemId = encodedId,
            provider = OFFLINE_PROVIDER,
            uri = "offline:artist:$encodedId",
            name = trimmed,
            sortName = trimmed.lowercase(),
            imageUrl = imageUrl
        )
    }

    private suspend fun Track.toPlayableMediaItem(parentMediaId: String? = null): MediaItem {
        val localFile = if (provider == OFFLINE_PROVIDER) {
            val file = File(uri)
            file.takeIf { it.exists() && it.length() > 0L }
        } else {
            null
        }
        val isLocalFile = localFile != null
        val durationMs = playbackDurationMs()
        val extras = buildPlaybackExtras(isLocalFile = isLocalFile, parentMediaId = parentMediaId)
        val resolvedArtworkUrl = resolveAutoArtworkUrl(imageUrl)
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(resolvedArtworkUrl?.let { Uri.parse(it) })
            .setDurationMs(durationMs)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_TRACK:$itemId:$provider")
            .setUri(localFile?.let { Uri.fromFile(it) } ?: Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
            .also { cacheMediaItem(it) }
    }

    private fun Album.toBrowsableMediaItem(
        includeArtwork: Boolean = true,
        includeSubtitle: Boolean = true,
        includeDisplayTitle: Boolean = true
    ): MediaItem {
        val artistsLabel = artists.joinToString(", ")
        val resolvedArtworkUrl = if (includeArtwork) resolveAutoArtworkUrl(imageUrl) else null
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(name)
        if (includeDisplayTitle) {
            metadataBuilder.setDisplayTitle(name)
        }
        if (includeSubtitle) {
            metadataBuilder.setArtist(artistsLabel)
        }
        val metadata = metadataBuilder
            .setArtworkUri(resolvedArtworkUrl?.let { Uri.parse(it) })
            .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_ALBUM:$itemId:$provider")
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Artist.toBrowsableMediaItem(
        includeArtwork: Boolean = true,
        includeSubtitle: Boolean = true,
        includeDisplayTitle: Boolean = true
    ): MediaItem {
        val resolvedArtworkUrl = if (includeArtwork) resolveAutoArtworkUrl(imageUrl) else null
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(name)
        if (includeDisplayTitle) {
            metadataBuilder.setDisplayTitle(name)
        }
        if (includeSubtitle) {
            metadataBuilder.setArtist(name)
        }
        val metadata = metadataBuilder
            .setArtworkUri(resolvedArtworkUrl?.let { Uri.parse(it) })
            .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_ARTIST:$itemId:$provider")
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Playlist.toBrowsableMediaItem(): MediaItem {
        val resolvedArtworkUrl = resolveAutoArtworkUrl(imageUrl)
        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setDisplayTitle(name)
            .setArtist(owner)
            .setArtworkUri(resolvedArtworkUrl?.let { Uri.parse(it) })
            .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_PLAYLIST:$itemId:$provider")
            .setMediaMetadata(metadata)
            .build()
    }


    private suspend fun buildAlbumTrackItems(albumId: String, provider: String): List<MediaItem> {
        val parentMediaId = albumMediaId(albumId, provider)
        val items = mutableListOf<MediaItem>()
        if (isOfflineMode()) {
            val (albumName, albumArtist) = decodeOfflineAlbumId(albumId)
            val tracks = localMediaRepository.getTracksByAlbum(albumName, albumArtist).first()
            for (track in tracks) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
        } else {
            val tracks = repository.getAlbumTracks(albumId, provider).getOrDefault(emptyList())
            val localTracks = loadLocalTracksForAlbum(albumId, provider, tracks)
            val mergedTracks = tracks.mergeWithLocal(localTracks)
            for (track in mergedTracks) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
        }
        return items
    }

    private suspend fun buildPlaylistTrackItems(
        playlistId: String,
        provider: String
    ): List<MediaItem> {
        val parentMediaId = playlistMediaId(playlistId, provider)
        val items = mutableListOf<MediaItem>()
        if (isOfflineMode()) {
            return emptyList()
        } else {
            val tracks = repository.getPlaylistTracks(playlistId, provider).getOrDefault(emptyList())
            val localTracks = localMediaRepository.getAllTracks().first()
            val mergedTracks = tracks.replaceWithLocalMatches(localTracks)
            for (track in mergedTracks) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
        }
        return items
    }

    private suspend fun loadLocalTracksForAlbum(
        albumId: String,
        provider: String,
        fallbackTracks: List<Track>
    ): List<Track> {
        val album = repository.getAlbum(albumId, provider).getOrNull()
        val albumName = album?.name?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTracks.firstOrNull()?.album?.trim().orEmpty()
        val artistNames = album?.artists
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: fallbackTracks.map { it.artist.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        if (albumName.isBlank() || artistNames.isEmpty()) return emptyList()
        val localTracks = mutableListOf<Track>()
        for (artistName in artistNames) {
            localTracks.addAll(
                localMediaRepository.getTracksByAlbum(albumName, artistName).first()
            )
        }
        return localTracks.distinctBy { "${it.provider}:${it.itemId}" }
    }

    private fun <T> applyPaging(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (items.isEmpty()) return emptyList()
        val safePage = if (page == C.INDEX_UNSET || page < 0) 0 else page
        val safeSize = if (pageSize == C.INDEX_UNSET || pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
        val fromIndex = (safePage.toLong() * safeSize.toLong()).coerceAtMost(items.size.toLong()).toInt()
        val toIndex = (fromIndex + safeSize).coerceAtMost(items.size)
        return items.subList(fromIndex, toIndex)
    }

    private fun resolvePaging(page: Int, pageSize: Int): Pair<Int, Int> {
        val safePage = if (page == C.INDEX_UNSET || page < 0) 0 else page
        val safeSize = if (pageSize == C.INDEX_UNSET || pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
        val offset = (safePage.toLong() * safeSize.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return offset to safeSize
    }

    private fun resolveHomeLimit(page: Int, pageSize: Int): Int {
        val safePage = if (page == C.INDEX_UNSET || page < 0) 0 else page
        val safeSize = if (pageSize == C.INDEX_UNSET || pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
        val limit = (safePage.toLong() + 1L) * safeSize.toLong()
        return limit.coerceAtMost(HOME_SECTION_LIMIT.toLong()).toInt()
    }

    private fun cacheMediaItem(mediaItem: MediaItem) {
        val mediaId = mediaItem.mediaId
        if (mediaId.isBlank()) return
        synchronized(mediaItemCache) {
            mediaItemCache[mediaId] = mediaItem
        }
    }

    private fun albumMediaId(albumId: String, provider: String): String {
        return "$MEDIA_ID_PREFIX_ALBUM:$albumId:$provider"
    }

    private fun playlistMediaId(playlistId: String, provider: String): String {
        return "$MEDIA_ID_PREFIX_PLAYLIST:$playlistId:$provider"
    }

    private val titleCollator = ThreadLocal.withInitial {
        Collator.getInstance().apply {
            strength = Collator.PRIMARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }
    }

    private val ArtistAlphabeticalComparator = Comparator<Artist> { left, right ->
        val primary = compareTitles(artistSortKey(left), artistSortKey(right))
        if (primary != 0) {
            primary
        } else {
            "${left.provider}:${left.itemId}".compareTo("${right.provider}:${right.itemId}")
        }
    }

    private val AlbumAlphabeticalComparator = Comparator<Album> { left, right ->
        val primary = compareTitles(albumSortKey(left), albumSortKey(right))
        if (primary != 0) {
            primary
        } else {
            val artistCompare = compareTitles(
                left.artists.firstOrNull().orEmpty().trim(),
                right.artists.firstOrNull().orEmpty().trim()
            )
            if (artistCompare != 0) {
                artistCompare
            } else {
                "${left.provider}:${left.itemId}".compareTo("${right.provider}:${right.itemId}")
            }
        }
    }

    private companion object {
        private const val TAG = "MediaLibraryBrowser"
        private const val MEDIA_ID_ROOT = "root"
        private const val MEDIA_ID_HOME = "home"
        private const val MEDIA_ID_HOME_RECENTLY_PLAYED = "home_recently_played"
        private const val MEDIA_ID_HOME_FAVORITES = "home_favorites"
        private const val MEDIA_ID_HOME_NEW_ALBUMS = "home_new_albums"
        private const val MEDIA_ID_HOME_PLAYLISTS = "home_playlists"
        private const val MEDIA_ID_ALBUMS = "albums"
        private const val MEDIA_ID_ARTISTS = "artists"
        private const val MEDIA_ID_PLAYLISTS = "playlists"
        private const val MEDIA_ID_LOCAL_MEDIA = "local_media"
        private const val MEDIA_ID_LOCAL_ALBUMS = "local_albums"
        private const val MEDIA_ID_LOCAL_ARTISTS = "local_artists"
        private const val MEDIA_ID_LOCAL_TRACKS = "local_tracks"

        private const val MEDIA_ID_PREFIX_ALBUM = "album"
        private const val MEDIA_ID_PREFIX_ARTIST = "artist"
        private const val MEDIA_ID_PREFIX_PLAYLIST = "playlist"
        private const val MEDIA_ID_PREFIX_TRACK = "track"
        private const val MEDIA_ID_PREFIX_ARTISTS_LETTER = "artists_letter"
        private const val MEDIA_ID_PREFIX_ALBUMS_LETTER = "albums_letter"
        private const val MEDIA_ID_PREFIX_LOCAL_ARTISTS_LETTER = "local_artists_letter"
        private const val MEDIA_ID_PREFIX_LOCAL_ALBUMS_LETTER = "local_albums_letter"

        private const val ROOT_TITLE = "Harmonixia"
        private const val TITLE_HOME = "Home"
        private const val TITLE_HOME_RECENTLY_PLAYED = "Recently Played"
        private const val TITLE_HOME_FAVORITES = "Favourites"
        private const val TITLE_HOME_NEW_ALBUMS = "New Albums"
        private const val TITLE_ALBUMS = "Albums"
        private const val TITLE_ARTISTS = "Artists"
        private const val TITLE_PLAYLISTS = "Playlists"
        private const val TITLE_LOCAL_MEDIA = "Local Media"
        private const val TITLE_LOCAL_ALBUMS = "Local Albums"
        private const val TITLE_LOCAL_ARTISTS = "Local Artists"
        private const val TITLE_LOCAL_TRACKS = "Local Tracks"

        private const val DEFAULT_PAGE_SIZE = 50
        private const val SEARCH_LIMIT = 200
        private const val ALBUM_PAGE_LIMIT = 200
        private const val ARTIST_PAGE_LIMIT = 200
        private const val HOME_SECTION_LIMIT = 200
        private const val FAVORITES_QUEUE_LIMIT = 1000
        private const val AUTO_ARTWORK_LIMIT = 1000
        private const val AUTO_COMPACT_METADATA_LIMIT = 1000
        private const val MEDIA_ITEM_CACHE_SIZE = 500
        private const val ARTIST_NAME_CACHE_SIZE = 500
        private const val PLAYLIST_URI_CACHE_SIZE = 500
    }
}
