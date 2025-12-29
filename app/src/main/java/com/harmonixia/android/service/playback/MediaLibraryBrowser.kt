package com.harmonixia.android.service.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId
import com.harmonixia.android.domain.repository.DownloadRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import com.harmonixia.android.util.EXTRA_IS_LOCAL_FILE
import com.harmonixia.android.util.EXTRA_PARENT_MEDIA_ID
import com.harmonixia.android.util.EXTRA_STREAM_URI
import com.harmonixia.android.util.EXTRA_TRACK_QUALITY
import com.harmonixia.android.util.NetworkConnectivityManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@UnstableApi
class MediaLibraryBrowser(
    private val repository: MusicAssistantRepository,
    private val downloadRepository: DownloadRepository,
    private val offlineLibraryRepository: OfflineLibraryRepository,
    private val networkConnectivityManager: NetworkConnectivityManager
) {
    private val artistNameCache = ConcurrentHashMap<String, String>()
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
            MEDIA_ID_DOWNLOADS -> buildDownloadedContent()
            MEDIA_ID_DOWNLOADED_ALBUMS -> buildDownloadedAlbumsList(page, pageSize)
            MEDIA_ID_DOWNLOADED_PLAYLISTS -> buildDownloadedPlaylistsList(page, pageSize)
            MEDIA_ID_DOWNLOADED_TRACKS -> buildDownloadedTracksList(page, pageSize)
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
                        val cachedName = artistNameCache[artistCacheKey(artistId, provider)].orEmpty()
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
            repository.searchLibrary(trimmed, SEARCH_LIMIT).getOrDefault(SearchResults())
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
            listOf(buildCategoryItem(MEDIA_ID_DOWNLOADS, TITLE_DOWNLOADS))
        } else {
            listOf(
                buildCategoryItem(MEDIA_ID_ALBUMS, TITLE_ALBUMS),
                buildCategoryItem(MEDIA_ID_ARTISTS, TITLE_ARTISTS),
                buildCategoryItem(MEDIA_ID_PLAYLISTS, TITLE_PLAYLISTS),
                buildCategoryItem(MEDIA_ID_DOWNLOADS, TITLE_DOWNLOADS)
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
            downloadRepository.getDownloadedAlbums().first()
        } else {
            val (offset, limit) = resolvePaging(page, pageSize)
            repository.fetchAlbums(limit, offset).getOrDefault(emptyList())
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
            artists.forEach { cacheArtistName(it) }
            artists.map { it.toBrowsableMediaItem() }
        }
    }

    private suspend fun buildPlaylistsList(page: Int, pageSize: Int): List<MediaItem> {
        val playlists = if (isOfflineMode()) {
            downloadRepository.getDownloadedPlaylists().first()
        } else {
            val (offset, limit) = resolvePaging(page, pageSize)
            repository.fetchPlaylists(limit, offset).getOrDefault(emptyList())
        }
        val paged = if (isOfflineMode()) applyPaging(playlists, page, pageSize) else playlists
        return paged.map { it.toBrowsableMediaItem() }
    }

    private fun buildDownloadedContent(): List<MediaItem> {
        return listOf(
            buildCategoryItem(MEDIA_ID_DOWNLOADED_ALBUMS, TITLE_DOWNLOADED_ALBUMS),
            buildCategoryItem(MEDIA_ID_DOWNLOADED_PLAYLISTS, TITLE_DOWNLOADED_PLAYLISTS),
            buildCategoryItem(MEDIA_ID_DOWNLOADED_TRACKS, TITLE_DOWNLOADED_TRACKS)
        )
    }

    private suspend fun buildDownloadedAlbumsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = downloadRepository.getDownloadedAlbums().first()
        return applyPaging(albums, page, pageSize).map { it.toBrowsableMediaItem() }
    }

    private suspend fun buildDownloadedPlaylistsList(page: Int, pageSize: Int): List<MediaItem> {
        val playlists = downloadRepository.getDownloadedPlaylists().first()
        return applyPaging(playlists, page, pageSize).map { it.toBrowsableMediaItem() }
    }

    private suspend fun buildDownloadedTracksList(page: Int, pageSize: Int): List<MediaItem> {
        val tracks = downloadRepository.getDownloadedTracks().first()
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
            val downloads = filterDownloadedTracksByAlbum(albumId, provider)
            val paged = applyPaging(downloads, page, pageSize)
            paged.map { it.toPlayableMediaItem(parentMediaId) }
        } else {
            val tracks = repository.getAlbumTracks(albumId, provider).getOrDefault(emptyList())
            val paged = applyPaging(tracks, page, pageSize)
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
            val downloads = filterDownloadedTracksByPlaylist(playlistId, provider)
            val paged = applyPaging(downloads, page, pageSize)
            paged.map { it.toPlayableMediaItem(parentMediaId) }
        } else {
            val tracks = repository.getPlaylistTracks(playlistId, provider).getOrDefault(emptyList())
            val paged = applyPaging(tracks, page, pageSize)
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
            offlineLibraryRepository.getDownloadedAlbumsByArtist(resolvedName).first()
        } else {
            val allAlbums = fetchAllAlbums()
            filterAlbumsForArtist(allAlbums, resolvedName)
        }
        return albums.map { it.toBrowsableMediaItem() }
    }

    private fun isOfflineMode(): Boolean = networkConnectivityManager.isOfflineMode()

    private fun cacheArtistName(artist: Artist) {
        val key = artistCacheKey(artist.itemId, artist.provider)
        artistNameCache[key] = artist.name
    }

    private fun artistCacheKey(artistId: String, provider: String): String {
        return "$provider:$artistId"
    }

    private suspend fun resolveArtistName(
        artistId: String,
        provider: String,
        cachedName: String
    ): String {
        if (cachedName.isNotBlank()) return cachedName
        val key = artistCacheKey(artistId, provider)
        val cached = artistNameCache[key]
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

    private suspend fun filterDownloadedTracksByAlbum(
        albumId: String,
        provider: String
    ): List<DownloadedTrack> {
        val albumDownloadId = "$albumId-$provider"
        val tracks = downloadRepository.getDownloadedTracks().first()
        val results = mutableListOf<DownloadedTrack>()
        for (track in tracks) {
            val downloaded = downloadRepository.getDownloadedTrack(track.downloadId) ?: continue
            if (downloaded.albumId == albumDownloadId) {
                results.add(downloaded)
            }
        }
        return results
    }

    private suspend fun filterDownloadedTracksByPlaylist(
        playlistId: String,
        provider: String
    ): List<DownloadedTrack> {
        val playlistDownloadId = "$playlistId-$provider"
        val tracks = downloadRepository.getDownloadedTracks().first()
        val results = mutableListOf<DownloadedTrack>()
        for (track in tracks) {
            val downloaded = downloadRepository.getDownloadedTrack(track.downloadId) ?: continue
            if (downloaded.playlistIds.contains(playlistDownloadId)) {
                results.add(downloaded)
            }
        }
        return results
    }

    private fun normalizeName(name: String?): String {
        return name?.trim()?.lowercase().orEmpty()
    }

    private suspend fun buildOfflineArtistsList(page: Int, pageSize: Int): List<MediaItem> {
        val albums = downloadRepository.getDownloadedAlbums().first()
        val tracks = downloadRepository.getDownloadedTracks().first()
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
        val downloaded = downloadRepository.getDownloadedTrack(downloadId)
        val localFile = resolveLocalFile(downloaded)
        val isLocalFile = localFile != null
        val durationMs = lengthSeconds
            .takeIf { it > 0 }
            ?.toLong()
            ?.times(1000L)
        val artworkUri = resolveArtworkUri(downloaded, imageUrl)
        val extras = Bundle().apply {
            if (!quality.isNullOrBlank()) {
                putString(EXTRA_TRACK_QUALITY, quality)
            }
            putBoolean(EXTRA_IS_LOCAL_FILE, isLocalFile)
            putString(EXTRA_STREAM_URI, uri)
            if (!parentMediaId.isNullOrBlank()) {
                putString(EXTRA_PARENT_MEDIA_ID, parentMediaId)
            }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
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

    private fun DownloadedTrack.toPlayableMediaItem(parentMediaId: String? = null): MediaItem {
        val localFile = File(localFilePath).takeIf { it.exists() && it.length() > 0L }
        val durationMs = track.lengthSeconds
            .takeIf { it > 0 }
            ?.toLong()
            ?.times(1000L)
        val artworkUri = resolveArtworkUri(this, track.imageUrl)
        val extras = Bundle().apply {
            if (!track.quality.isNullOrBlank()) {
                putString(EXTRA_TRACK_QUALITY, track.quality)
            }
            putBoolean(EXTRA_IS_LOCAL_FILE, localFile != null)
            putString(EXTRA_STREAM_URI, track.uri)
            if (!parentMediaId.isNullOrBlank()) {
                putString(EXTRA_PARENT_MEDIA_ID, parentMediaId)
            }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(artworkUri)
            .setDurationMs(durationMs)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_PREFIX_TRACK:${track.itemId}:${track.provider}")
            .setUri(localFile?.let { Uri.fromFile(it) } ?: Uri.parse(track.uri))
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

    private suspend fun resolveLocalFile(downloaded: DownloadedTrack?): File? {
        if (downloaded == null) return null
        return withContext(Dispatchers.IO) {
            val file = File(downloaded.localFilePath)
            if (file.exists() && file.length() > 0L) file else null
        }
    }

    private fun resolveArtworkUri(
        downloaded: DownloadedTrack?,
        fallbackImageUrl: String?
    ): Uri? {
        val coverPath = downloaded?.coverArtPath
        if (!coverPath.isNullOrBlank()) {
            val coverFile = File(coverPath)
            if (coverFile.exists() && coverFile.length() > 0L) {
                return Uri.fromFile(coverFile)
            }
        }
        val imageUrl = downloaded?.track?.imageUrl ?: fallbackImageUrl
        return imageUrl?.let { Uri.parse(it) }
    }

    private suspend fun buildAlbumTrackItems(albumId: String, provider: String): List<MediaItem> {
        val parentMediaId = albumMediaId(albumId, provider)
        val items = mutableListOf<MediaItem>()
        if (isOfflineMode()) {
            val downloads = filterDownloadedTracksByAlbum(albumId, provider)
            for (track in downloads) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
        } else {
            val tracks = repository.getAlbumTracks(albumId, provider).getOrDefault(emptyList())
            for (track in tracks) {
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
            val downloads = filterDownloadedTracksByPlaylist(playlistId, provider)
            for (track in downloads) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
        } else {
            val tracks = repository.getPlaylistTracks(playlistId, provider).getOrDefault(emptyList())
            for (track in tracks) {
                items.add(track.toPlayableMediaItem(parentMediaId))
            }
        }
        return items
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
        private const val MEDIA_ID_DOWNLOADS = "downloads"
        private const val MEDIA_ID_DOWNLOADED_ALBUMS = "downloaded_albums"
        private const val MEDIA_ID_DOWNLOADED_PLAYLISTS = "downloaded_playlists"
        private const val MEDIA_ID_DOWNLOADED_TRACKS = "downloaded_tracks"

        private const val MEDIA_ID_PREFIX_ALBUM = "album"
        private const val MEDIA_ID_PREFIX_ARTIST = "artist"
        private const val MEDIA_ID_PREFIX_PLAYLIST = "playlist"
        private const val MEDIA_ID_PREFIX_TRACK = "track"

        private const val ROOT_TITLE = "Harmonixia"
        private const val TITLE_ALBUMS = "Albums"
        private const val TITLE_ARTISTS = "Artists"
        private const val TITLE_PLAYLISTS = "Playlists"
        private const val TITLE_DOWNLOADS = "Downloads"
        private const val TITLE_DOWNLOADED_ALBUMS = "Downloaded Albums"
        private const val TITLE_DOWNLOADED_PLAYLISTS = "Downloaded Playlists"
        private const val TITLE_DOWNLOADED_TRACKS = "Downloaded Tracks"

        private const val DEFAULT_PAGE_SIZE = 50
        private const val SEARCH_LIMIT = 200
        private const val ALBUM_PAGE_LIMIT = 200
        private const val ARTIST_PAGE_LIMIT = 200
        private const val MEDIA_ITEM_CACHE_SIZE = 500
    }
}
