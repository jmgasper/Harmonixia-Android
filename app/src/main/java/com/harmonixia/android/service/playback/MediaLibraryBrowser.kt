package com.harmonixia.android.service.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
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
import com.harmonixia.android.util.buildPlaybackExtras
import com.harmonixia.android.util.playbackDurationMs
import java.io.File
import kotlinx.coroutines.flow.first

@UnstableApi
class MediaLibraryBrowser(
    private val repository: MusicAssistantRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val offlineLibraryRepository: OfflineLibraryRepository,
    private val networkConnectivityManager: NetworkConnectivityManager
) {
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
        return buildRootItem()
    }

    suspend fun getChildren(parentId: String, page: Int, pageSize: Int): List<MediaItem> {
        return when (parentId) {
            MEDIA_ID_ROOT -> buildRootCategories()
            MEDIA_ID_ALBUMS -> buildAlbumsList(page, pageSize)
            MEDIA_ID_ARTISTS -> buildArtistsList(page, pageSize)
            MEDIA_ID_PLAYLISTS -> buildPlaylistsList(page, pageSize)
            MEDIA_ID_LOCAL_MEDIA -> buildLocalContent()
            MEDIA_ID_LOCAL_ALBUMS -> buildLocalAlbumsList(page, pageSize)
            MEDIA_ID_LOCAL_ARTISTS -> buildLocalArtistsList(page, pageSize)
            MEDIA_ID_LOCAL_TRACKS -> buildLocalTracksList(page, pageSize)
            else -> {
                when {
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
            val serverResults = repository.searchLibrary(trimmed, SEARCH_LIMIT)
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

    private fun buildRootCategories(): List<MediaItem> {
        return if (isOfflineMode()) {
            listOf(buildCategoryItem(MEDIA_ID_LOCAL_MEDIA, TITLE_LOCAL_MEDIA))
        } else {
            listOf(
                buildCategoryItem(MEDIA_ID_ALBUMS, TITLE_ALBUMS),
                buildCategoryItem(MEDIA_ID_ARTISTS, TITLE_ARTISTS),
                buildCategoryItem(MEDIA_ID_PLAYLISTS, TITLE_PLAYLISTS),
                buildCategoryItem(MEDIA_ID_LOCAL_MEDIA, TITLE_LOCAL_MEDIA)
            )
        }
    }

    private fun buildCategoryItem(
        mediaId: String,
        title: String,
        subtitle: String? = null,
        artworkUrl: String? = null
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun buildAlbumsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = if (isOfflineMode()) {
            localMediaRepository.getAllAlbums().first()
        } else {
            val (offset, limit) = resolvePaging(page, pageSize)
            val serverAlbums = repository.fetchAlbums(limit, offset).getOrDefault(emptyList())
            val localAlbums = localMediaRepository.getAllAlbums().first()
            serverAlbums.mergeWithLocal(localAlbums)
        }
        val paged = if (isOfflineMode()) applyPaging(albums, page, pageSize) else albums
        return paged.map { it.toBrowsableMediaItem() }
    }

    private suspend fun buildArtistsList(page: Int, pageSize: Int): List<MediaItem> {
        return if (isOfflineMode()) {
            buildOfflineArtistsList(page, pageSize)
        } else {
            val (offset, limit) = resolvePaging(page, pageSize)
            val artists = repository.fetchArtists(limit, offset).getOrDefault(emptyList())
            val localArtists = localMediaRepository.getAllArtists().first()
            val mergedArtists = artists.mergeWithLocal(localArtists)
            mergedArtists.forEach { cacheArtistName(it) }
            mergedArtists.map { it.toBrowsableMediaItem() }
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

    private fun buildLocalContent(): List<MediaItem> {
        return listOf(
            buildCategoryItem(MEDIA_ID_LOCAL_ALBUMS, TITLE_LOCAL_ALBUMS),
            buildCategoryItem(MEDIA_ID_LOCAL_ARTISTS, TITLE_LOCAL_ARTISTS),
            buildCategoryItem(MEDIA_ID_LOCAL_TRACKS, TITLE_LOCAL_TRACKS)
        )
    }

    private suspend fun buildLocalAlbumsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = localMediaRepository.getAllAlbums().first()
        return applyPaging(albums, page, pageSize).map { it.toBrowsableMediaItem() }
    }

    private suspend fun buildLocalArtistsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = localMediaRepository.getAllAlbums().first()
        val tracks = localMediaRepository.getAllTracks().first()
        val artists = buildOfflineArtists(albums, tracks)
        artists.forEach { cacheArtistName(it) }
        return applyPaging(artists, page, pageSize).map { it.toBrowsableMediaItem() }
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
            val allAlbums = fetchAllAlbums()
            val localAlbums = localMediaRepository.getAllAlbums().first()
            val mergedAlbums = allAlbums.mergeWithLocal(localAlbums)
            filterAlbumsForArtist(mergedAlbums, resolvedName)
        }
        return albums.map { it.toBrowsableMediaItem() }
    }

    private fun isOfflineMode(): Boolean = networkConnectivityManager.isOfflineMode()

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

    private fun normalizeName(name: String?): String {
        return name?.trim()?.lowercase().orEmpty()
    }

    private suspend fun buildOfflineArtistsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = localMediaRepository.getAllAlbums().first()
        val tracks = localMediaRepository.getAllTracks().first()
        val artists = buildOfflineArtists(albums, tracks)
        artists.forEach { cacheArtistName(it) }
        return applyPaging(artists, page, pageSize).map { it.toBrowsableMediaItem() }
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
            .sortedBy { it.name.lowercase() }
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
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(imageUrl?.let { Uri.parse(it) })
            .setDurationMs(durationMs)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_TRACK:$itemId:$provider")
            .setUri(localFile?.let { Uri.fromFile(it) } ?: Uri.parse(uri))
            .setMediaMetadata(metadata)
            .build()
            .also { cacheMediaItem(it) }
    }

    private fun Album.toBrowsableMediaItem(): MediaItem {
        val artistsLabel = artists.joinToString(", ")
        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(artistsLabel)
            .setArtworkUri(imageUrl?.let { Uri.parse(it) })
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_ALBUM:$itemId:$provider")
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Artist.toBrowsableMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(name)
            .setArtworkUri(imageUrl?.let { Uri.parse(it) })
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_ARTIST:$itemId:$provider")
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Playlist.toBrowsableMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(owner)
            .setArtworkUri(imageUrl?.let { Uri.parse(it) })
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

    private companion object {
        private const val MEDIA_ID_ROOT = "root"
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

        private const val ROOT_TITLE = "Harmonixia"
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
        private const val MEDIA_ITEM_CACHE_SIZE = 500
        private const val ARTIST_NAME_CACHE_SIZE = 500
        private const val PLAYLIST_URI_CACHE_SIZE = 500
    }
}
