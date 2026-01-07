package com.harmonixia.android.data.repository

import com.harmonixia.android.data.remote.ApiCommand
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.data.remote.MusicAssistantWebSocketClient
import com.harmonixia.android.data.remote.WebSocketMessage
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.AlbumType
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.PlaybackState
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.model.ProviderBadge
import com.harmonixia.android.domain.model.ProviderMapping
import com.harmonixia.android.domain.model.Queue
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.NetworkError
import com.harmonixia.android.util.PerformanceMonitor
import com.harmonixia.android.util.toNetworkError
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class MusicAssistantRepositoryImpl @Inject constructor(
    private val webSocketClient: MusicAssistantWebSocketClient,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val performanceMonitor: PerformanceMonitor
) : MusicAssistantRepository {
    private var cachedServerUrl: String? = null
    private var cachedAuthToken: String? = null
    private val cacheLock = Any()
    private val albumCache = object : LinkedHashMap<String, Album>(ALBUM_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Album>): Boolean {
            return size > ALBUM_CACHE_SIZE
        }
    }
    private val playlistCache =
        object : LinkedHashMap<String, Playlist>(PLAYLIST_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, Playlist>
            ): Boolean {
                return size > PLAYLIST_CACHE_SIZE
            }
        }
    private val albumTracksCache =
        object : LinkedHashMap<String, List<Track>>(TRACKS_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, List<Track>>
            ): Boolean {
                return size > TRACKS_CACHE_SIZE
            }
        }
    private val playlistTracksCache =
        object : LinkedHashMap<String, List<Track>>(TRACKS_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, List<Track>>
            ): Boolean {
                return size > TRACKS_CACHE_SIZE
            }
        }
    private val providerCacheLock = Any()
    private var providerManifestsCache: Map<String, ProviderManifest>? = null
    private var providerInstancesCache: Map<String, ProviderInstance>? = null
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        cacheScope.launch {
            webSocketClient.events.collect { event ->
                invalidateCachesForEvent(event)
            }
        }
    }

    override suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        cachedServerUrl = serverUrl
        cachedAuthToken = authToken
        synchronized(providerCacheLock) {
            providerManifestsCache = null
            providerInstancesCache = null
        }
        return webSocketClient.connect(serverUrl, authToken)
    }

    override suspend fun loginWithCredentials(
        serverUrl: String,
        username: String,
        password: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                Logger.d(TAG, "Attempting login for user: $username")
                val loginUrl = "${serverUrl.trimEnd('/')}/${ApiCommand.AUTH_LOGIN}"
                val payload = buildJsonObject {
                    put(
                        "credentials",
                        buildJsonObject {
                            put("username", username)
                            put("password", password)
                        }
                    )
                }
                val mediaType = "application/json".toMediaType()
                val requestBody = payload.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(loginUrl)
                    .post(requestBody)
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val errorMessage = runCatching {
                            val element = json.parseToJsonElement(responseBody)
                            val payloadObject = element as? JsonObject
                            payloadObject?.stringOrNull("error", "message", "detail")
                        }.getOrNull()
                        when (response.code) {
                            401, 403 ->
                                throw SecurityException(errorMessage ?: "Invalid username or password")
                            408, 504 -> throw IOException("Connection timeout. Please check your network.")
                            500, 502, 503 -> throw IOException("Server error. Please try again later.")
                            else -> throw IOException(errorMessage ?: "Login failed: ${response.code}")
                        }
                    }
                    if (responseBody.isBlank()) {
                        throw IOException("Invalid server response")
                    }
                    val element = runCatching { json.parseToJsonElement(responseBody) }
                        .getOrElse { throw IOException("Invalid server response", it) }
                    val payloadObject = element as? JsonObject
                        ?: throw IOException("Invalid server response")
                    val success = payloadObject["success"]?.jsonPrimitive?.booleanOrNull
                    if (success == false) {
                        val message = payloadObject.stringOrNull("error", "message", "detail")
                            ?: "Invalid username or password"
                        throw SecurityException(message)
                    }
                    val token = payloadObject.stringOrNull("token", "access_token", "auth_token")
                        ?: throw IOException("Invalid server response")
                    Logger.i(TAG, "Login successful, token obtained")
                    token
                }
            }.recoverCatching { error ->
                when (error) {
                    is SecurityException -> throw error
                    is SocketTimeoutException ->
                        throw IOException("Connection timeout. Please check your network.", error)
                    is UnknownHostException,
                    is ConnectException,
                    is IllegalArgumentException ->
                        throw IOException("Cannot connect to server. Please check the URL.", error)
                    is IOException -> throw error
                    else -> throw IOException(error.message ?: "Login failed", error)
                }
            }.onFailure { error ->
                Logger.e(TAG, "Login failed: ${error.message}", error)
            }
        }
    }

    override suspend fun disconnect() {
        webSocketClient.disconnect()
    }

    override fun getConnectionState(): StateFlow<ConnectionState> {
        return webSocketClient.connectionState
    }

    override fun observeEvents(): Flow<WebSocketMessage.EventMessage> {
        return webSocketClient.events
    }

    override suspend fun fetchAlbums(limit: Int, offset: Int): Result<List<Album>> {
        return fetchPaged(
            command = ApiCommand.MUSIC_GET_LIBRARY_ALBUMS,
            limit = limit,
            offset = offset
        ) { parseAlbum(it) }
    }

    override suspend fun fetchArtists(limit: Int, offset: Int): Result<List<Artist>> {
        return fetchPaged(
            command = ApiCommand.MUSIC_GET_LIBRARY_ARTISTS,
            limit = limit,
            offset = offset
        ) { parseArtist(it) }
    }

    override suspend fun fetchPlaylists(limit: Int, offset: Int): Result<List<Playlist>> {
        return fetchPaged(
            command = ApiCommand.MUSIC_GET_LIBRARY_PLAYLISTS,
            limit = limit,
            offset = offset
        ) { parsePlaylist(it) }
    }

    override suspend fun fetchRecentlyPlayed(limit: Int): Result<List<Album>> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_RECENTLY_PLAYED,
                mapOf(
                    "limit" to limit,
                    "media_types" to listOf("album")
                )
            ).getOrThrow()
            val albums = mutableListOf<Album>()
            var attempted = 0
            var lastFailure: Throwable? = null
            extractItems(result).forEach { item ->
                val itemId = item.stringOrEmpty("item_id")
                val provider = item.stringOrEmpty("provider")
                if (itemId.isBlank() || provider.isBlank()) return@forEach
                attempted += 1
                getAlbum(itemId, provider)
                    .onSuccess { albums.add(it) }
                    .onFailure { lastFailure = it }
            }
            val trimmed = if (limit > 0) albums.take(limit) else albums
            if (attempted > 0 && trimmed.isEmpty()) {
                throw lastFailure ?: IllegalStateException("Failed to load recently played albums.")
            }
            trimmed
        }
    }

    override suspend fun fetchRecentlyPlayedPlaylists(limit: Int): Result<List<Playlist>> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_RECENTLY_PLAYED,
                mapOf(
                    "limit" to limit,
                    "media_types" to listOf("playlist")
                )
            ).getOrThrow()
            val playlists = mutableListOf<Playlist>()
            var attempted = 0
            var lastFailure: Throwable? = null
            extractItems(result).forEach { item ->
                val itemId = item.stringOrEmpty("item_id")
                val provider = item.stringOrEmpty("provider")
                if (itemId.isBlank() || provider.isBlank()) return@forEach
                attempted += 1
                getPlaylist(itemId, provider)
                    .onSuccess { playlists.add(it) }
                    .onFailure { lastFailure = it }
            }
            val trimmed = if (limit > 0) playlists.take(limit) else playlists
            if (attempted > 0 && trimmed.isEmpty()) {
                throw lastFailure ?: IllegalStateException("Failed to load recently played playlists.")
            }
            trimmed
        }
    }

    override suspend fun fetchRecentlyAdded(limit: Int): Result<List<Album>> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_GET_LIBRARY_ALBUMS,
                mapOf(
                    "limit" to limit,
                    "offset" to 0,
                    "order_by" to "timestamp_added_desc"
                )
            ).getOrThrow()
            parseAlbumItems(result)
        }
    }

    override suspend fun getAlbum(itemId: String, provider: String): Result<Album> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_GET_ALBUM,
                mapOf("item_id" to itemId, "provider_instance_id_or_domain" to provider)
            ).getOrThrow()
            val payload = result as? JsonObject ?: run {
                Logger.w(TAG, "Unexpected album response: $result")
                throw IllegalStateException("Unexpected album response")
            }
            val album = parseAlbum(payload)
            val key = cacheKey(itemId, provider)
            synchronized(cacheLock) {
                albumCache[key] = album
            }
            album
        }
    }

    override suspend fun getArtist(itemId: String, provider: String): Result<Artist> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_GET_ARTIST,
                mapOf("item_id" to itemId, "provider_instance_id_or_domain" to provider)
            ).getOrThrow()
            val payload = result as? JsonObject ?: run {
                Logger.w(TAG, "Unexpected artist response: $result")
                throw IllegalStateException("Unexpected artist response")
            }
            parseArtist(payload)
        }
    }

    override suspend fun getArtistAlbums(
        itemId: String,
        provider: String,
        inLibraryOnly: Boolean
    ): Result<List<Album>> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_GET_ARTIST_ALBUMS,
                mapOf(
                    "item_id" to itemId,
                    "provider_instance_id_or_domain" to provider,
                    "in_library_only" to inLibraryOnly
                )
            ).getOrThrow()
            parseAlbumItems(result)
        }
    }

    override fun getCachedAlbum(itemId: String, provider: String): Album? {
        val key = cacheKey(itemId, provider)
        val cached = synchronized(cacheLock) { albumCache[key] }
        performanceMonitor.recordCacheLookup(PerformanceMonitor.CacheType.ALBUM, cached != null)
        return cached
    }

    override fun getCachedPlaylist(itemId: String, provider: String): Playlist? {
        val key = cacheKey(itemId, provider)
        val cached = synchronized(cacheLock) { playlistCache[key] }
        performanceMonitor.recordCacheLookup(PerformanceMonitor.CacheType.PLAYLIST, cached != null)
        return cached
    }

    override fun getCachedAlbumTracks(albumId: String, provider: String): List<Track>? {
        val key = cacheKey(albumId, provider)
        val cached = synchronized(cacheLock) { albumTracksCache[key] }
        performanceMonitor.recordCacheLookup(PerformanceMonitor.CacheType.ALBUM, cached != null)
        return cached
    }

    override fun getCachedPlaylistTracks(playlistId: String, provider: String): List<Track>? {
        val key = cacheKey(playlistId, provider)
        val cached = synchronized(cacheLock) { playlistTracksCache[key] }
        performanceMonitor.recordCacheLookup(PerformanceMonitor.CacheType.PLAYLIST, cached != null)
        return cached
    }

    override suspend fun searchLibrary(query: String, limit: Int): Result<SearchResults> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_SEARCH,
                mapOf(
                    "search_query" to query,
                    "limit" to limit,
                    "library_only" to true
                )
            ).getOrThrow()
            parseSearchResults(result)
        }
    }

    override suspend fun getAlbumTracks(albumId: String, provider: String): Result<List<Track>> {
        return getAlbumTracksChunked(albumId, provider, 0, Int.MAX_VALUE)
    }

    override suspend fun getAlbumTracksChunked(
        albumId: String,
        provider: String,
        offset: Int,
        limit: Int
    ): Result<List<Track>> {
        return runCatching {
            if (limit <= 0) return@runCatching emptyList()
            val normalizedOffset = offset.coerceAtLeast(0)
            val key = cacheKey(albumId, provider)
            val fullList = if (normalizedOffset == 0) {
                val fetched = fetchAlbumTracks(albumId, provider)
                synchronized(cacheLock) {
                    albumTracksCache[key] = fetched
                }
                fetched
            } else {
                synchronized(cacheLock) { albumTracksCache[key] } ?: run {
                    val fetched = fetchAlbumTracks(albumId, provider)
                    synchronized(cacheLock) {
                        albumTracksCache[key] = fetched
                    }
                    fetched
                }
            }
            fullList.drop(normalizedOffset).take(limit)
        }
    }

    override suspend fun getPlaylistTracks(playlistId: String, provider: String): Result<List<Track>> {
        val key = cacheKey(playlistId, provider)
        val cached = synchronized(cacheLock) { playlistTracksCache[key] }
        if (cached != null) {
            return Result.success(cached)
        }
        return runCatching {
            val fetched = fetchPlaylistTracks(playlistId, provider)
            synchronized(cacheLock) {
                playlistTracksCache[key] = fetched
            }
            fetched
        }
    }

    override suspend fun getPlaylistTracksChunked(
        playlistId: String,
        provider: String,
        offset: Int,
        limit: Int
    ): Result<List<Track>> {
        return runCatching {
            if (limit <= 0) return@runCatching emptyList()
            val normalizedOffset = offset.coerceAtLeast(0)
            val key = cacheKey(playlistId, provider)
            val fullList = if (normalizedOffset == 0) {
                val fetched = fetchPlaylistTracks(playlistId, provider)
                synchronized(cacheLock) {
                    playlistTracksCache[key] = fetched
                }
                fetched
            } else {
                synchronized(cacheLock) { playlistTracksCache[key] } ?: run {
                    val fetched = fetchPlaylistTracks(playlistId, provider)
                    synchronized(cacheLock) {
                        playlistTracksCache[key] = fetched
                    }
                    fetched
                }
            }
            fullList.drop(normalizedOffset).take(limit)
        }
    }

    override suspend fun getPlaylist(playlistId: String, provider: String): Result<Playlist> {
        return runCatching {
            if (playlistId.isBlank() || provider.isBlank()) {
                throw IllegalArgumentException("Playlist details are required")
            }
            var offset = 0
            val pageSize = DEFAULT_PAGE_SIZE
            while (true) {
                val result = webSocketClient.sendRequest(
                    ApiCommand.MUSIC_GET_LIBRARY_PLAYLISTS,
                    mapOf("limit" to pageSize, "offset" to offset)
                ).getOrThrow()
                val page = parsePlaylistItems(result)
                val match = page.firstOrNull { playlist ->
                    playlist.itemId == playlistId && playlist.provider == provider
                }
                if (match != null) {
                    val key = cacheKey(playlistId, provider)
                    synchronized(cacheLock) {
                        playlistCache[key] = match
                    }
                    return@runCatching match
                }
                if (page.size < pageSize) break
                offset += pageSize
            }
            throw IllegalStateException("Playlist not found")
        }
    }

    override suspend fun fetchPlayers(): Result<List<Player>> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.PLAYERS_FETCH_STATE,
                mapOf(
                    "return_unavailable" to true,
                    "return_disabled" to true
                )
            ).getOrThrow()
            parsePlayerItems(result)
        }
    }

    override suspend fun getActiveQueue(playerId: String, includeItems: Boolean): Result<Queue?> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.PLAYER_QUEUES_FETCH_STATE,
                mapOf("player_id" to playerId)
            ).getOrThrow()
            val queue = (result as? JsonObject)?.let { parseQueue(it) }
            queue ?: return@runCatching null
            if (!includeItems || queue.queueId.isBlank()) return@runCatching queue
            val itemsResult = webSocketClient.sendRequest(
                ApiCommand.PLAYER_QUEUES_ITEMS,
                mapOf("queue_id" to queue.queueId, "limit" to QUEUE_ITEM_LIMIT, "offset" to 0)
            ).getOrThrow()
            val queueItems = parseQueueItems(itemsResult)
            queue.copy(items = queueItems)
        }
    }

    override suspend fun playMedia(
        queueId: String,
        mediaUris: List<String>,
        option: QueueOption
    ): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_PLAY_MEDIA,
            mapOf(
                "queue_id" to queueId,
                "media" to mediaUris,
                "option" to option.toApiValue()
            )
        )
    }

    override suspend fun playMediaItem(
        queueId: String,
        media: String,
        option: QueueOption,
        startItem: String?
    ): Result<Unit> {
        val trimmedMedia = media.trim()
        if (trimmedMedia.isBlank()) {
            return Result.failure(IllegalArgumentException("Media URI is required"))
        }
        val params = buildMap {
            put("queue_id", queueId)
            put("media", trimmedMedia)
            put("option", option.toApiValue())
            if (!startItem.isNullOrBlank()) {
                put("start_item", startItem)
            }
        }
        return sendCommand(ApiCommand.PLAYER_QUEUES_PLAY_MEDIA, params)
    }

    override suspend fun playIndex(queueId: String, index: Int): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_PLAY_INDEX,
            mapOf("queue_id" to queueId, "index" to index)
        )
    }

    override suspend fun pauseQueue(queueId: String): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_PAUSE,
            mapOf("queue_id" to queueId)
        )
    }

    override suspend fun resumeQueue(queueId: String): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_RESUME,
            mapOf("queue_id" to queueId)
        )
    }

    override suspend fun nextTrack(queueId: String): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_NEXT,
            mapOf("queue_id" to queueId)
        )
    }

    override suspend fun previousTrack(queueId: String): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_PREVIOUS,
            mapOf("queue_id" to queueId)
        )
    }

    override suspend fun seekTo(queueId: String, position: Int): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_SEEK,
            mapOf("queue_id" to queueId, "position" to position)
        )
    }

    override suspend fun reportPlaybackProgress(
        queueId: String,
        track: Track,
        positionSeconds: Int
    ): Result<Unit> {
        val mediaItem = buildTrackPayload(track)
            ?: return Result.failure(IllegalArgumentException("Missing track metadata for playback reporting"))
        val result = sendMarkPlayed(
            mediaItem = mediaItem,
            queueId = queueId,
            isPlaying = true,
            secondsPlayed = positionSeconds
        )
        val albumItem = buildAlbumPayload(track)
        if (albumItem != null) {
            sendMarkPlayed(
                mediaItem = albumItem,
                queueId = queueId,
                isPlaying = true,
                secondsPlayed = positionSeconds
            ).onFailure { Logger.w(TAG, "Failed to report album playback progress", it) }
        }
        return result
    }

    override suspend fun reportTrackCompleted(
        queueId: String,
        track: Track,
        durationSeconds: Int
    ): Result<Unit> {
        val mediaItem = buildTrackPayload(track)
            ?: return Result.failure(IllegalArgumentException("Missing track metadata for playback reporting"))
        val result = sendMarkPlayed(
            mediaItem = mediaItem,
            queueId = queueId,
            isPlaying = false,
            secondsPlayed = durationSeconds,
            fullyPlayed = true
        )
        val albumItem = buildAlbumPayload(track)
        if (albumItem != null) {
            sendMarkPlayed(
                mediaItem = albumItem,
                queueId = queueId,
                isPlaying = false,
                secondsPlayed = durationSeconds
            ).onFailure { Logger.w(TAG, "Failed to report album completion", it) }
        }
        return result
    }

    override suspend fun setRepeatMode(queueId: String, repeatMode: RepeatMode): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_REPEAT,
            mapOf("queue_id" to queueId, "repeat_mode" to repeatMode.toApiValue())
        )
    }

    override suspend fun setShuffleMode(queueId: String, shuffle: Boolean): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_SHUFFLE,
            mapOf("queue_id" to queueId, "shuffle_enabled" to shuffle)
        )
    }

    override suspend fun clearQueue(queueId: String): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_CLEAR,
            mapOf("queue_id" to queueId)
        )
    }

    override suspend fun setPlayerVolume(playerId: String, volume: Int): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYERS_VOLUME_SET,
            mapOf("player_id" to playerId, "volume_level" to volume.coerceIn(0, 100))
        )
    }

    override suspend fun setPlayerMute(playerId: String, muted: Boolean): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYERS_VOLUME_MUTE,
            mapOf("player_id" to playerId, "muted" to muted)
        )
    }

    override suspend fun createPlaylist(name: String): Result<Playlist> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_CREATE_PLAYLIST,
                mapOf("name" to name)
            ).getOrThrow()
            parsePlaylistObject(result)
        }
    }

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return runCatching {
            if (playlistId.isBlank()) {
                throw IllegalArgumentException("Playlist id is required")
            }
            webSocketClient.sendRequest(
                ApiCommand.MUSIC_DELETE_PLAYLIST,
                mapOf("item_id" to playlistId)
            ).getOrThrow()
            Unit
        }.recoverCatching { error ->
            when (error.toNetworkError()) {
                is NetworkError.AuthenticationError ->
                    throw IllegalStateException("Authentication failed. Please update your token.")
                is NetworkError.ConnectionError,
                is NetworkError.TimeoutError ->
                    throw IllegalStateException("Unable to connect to server.")
                else -> throw error
            }
        }
    }

    override suspend fun addTracksToPlaylist(
        playlistId: String,
        trackUris: List<String>
    ): Result<Unit> {
        return sendCommand(
            ApiCommand.MUSIC_ADD_PLAYLIST_TRACKS,
            mapOf("db_playlist_id" to playlistId, "uris" to trackUris)
        )
    }

    override suspend fun removeTracksFromPlaylist(
        playlistId: String,
        positions: List<Int>
    ): Result<Unit> {
        return sendCommand(
            ApiCommand.MUSIC_REMOVE_PLAYLIST_TRACKS,
            mapOf("db_playlist_id" to playlistId, "positions_to_remove" to positions)
        )
    }

    override suspend fun addToFavorites(track: Track): Result<Unit> {
        val item = buildTrackPayload(track)
            ?: return Result.failure(
                IllegalArgumentException("Missing track metadata for favorites")
            )
        return sendCommand(
            ApiCommand.MUSIC_FAVORITES_ADD_ITEM,
            mapOf("item" to item)
        )
    }

    override suspend fun removeFromFavorites(track: Track): Result<Unit> {
        val itemId = track.itemId
        if (itemId.isBlank()) {
            return Result.failure(IllegalArgumentException("Track item id is required"))
        }
        return sendCommand(
            ApiCommand.MUSIC_FAVORITES_REMOVE_ITEM,
            mapOf(
                "media_type" to "track",
                "library_item_id" to itemId
            )
        )
    }

    override suspend fun fetchFavorites(limit: Int, offset: Int): Result<List<Track>> {
        return fetchPaged(
            command = ApiCommand.MUSIC_FAVORITES_LIBRARY_ITEMS,
            limit = limit,
            offset = offset,
            extraParams = mapOf("media_type" to "track")
        ) { parseTrack(it) }
    }

    override suspend fun resolveProviderBadge(
        providerKey: String?,
        providerDomains: List<String>
    ): Result<ProviderBadge?> {
        return runCatching {
            val trimmedKey = providerKey?.trim().orEmpty()
            val domains = providerDomains.map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (trimmedKey.isBlank() && domains.isEmpty()) return@runCatching null
            val catalog = ensureProviderCatalog()
            val manifest = resolveProviderManifest(trimmedKey, domains, catalog)
            val badgeName = manifest?.name?.trim().orEmpty()
            val iconSvg = manifest?.preferredIconSvg()
            val iconUrl = manifest?.icon?.takeIf {
                it.startsWith("http://") || it.startsWith("https://") || it.startsWith("data:")
            }
            if (badgeName.isBlank() && iconSvg.isNullOrBlank() && iconUrl.isNullOrBlank()) {
                null
            } else {
                ProviderBadge(
                    name = badgeName,
                    iconSvg = iconSvg,
                    iconUrl = iconUrl
                )
            }
        }
    }

    private suspend fun fetchAlbumTracks(albumId: String, provider: String): List<Track> {
        val result = webSocketClient.sendRequest(
            ApiCommand.MUSIC_GET_ALBUM_TRACKS,
            mapOf("item_id" to albumId, "provider_instance_id_or_domain" to provider)
        ).getOrThrow()
        return parseTrackItems(result)
    }

    private suspend fun fetchPlaylistTracks(playlistId: String, provider: String): List<Track> {
        val result = webSocketClient.sendRequest(
            ApiCommand.MUSIC_GET_PLAYLIST_TRACKS,
            mapOf(
                "item_id" to playlistId,
                "provider_instance_id_or_domain" to provider,
                "force_refresh" to false
            )
        ).getOrThrow()
        return parseTrackItems(result)
    }

    private suspend fun ensureProviderCatalog(): ProviderCatalog {
        val cached = synchronized(providerCacheLock) {
            val manifests = providerManifestsCache
            val instances = providerInstancesCache
            if (manifests != null && instances != null) {
                ProviderCatalog(manifests, instances)
            } else {
                null
            }
        }
        if (cached != null) return cached
        val providersResult = webSocketClient.sendRequest(ApiCommand.PROVIDERS).getOrThrow()
        val manifestsResult = webSocketClient.sendRequest(ApiCommand.PROVIDERS_MANIFESTS).getOrThrow()
        val instances = parseProviderInstances(providersResult)
        val manifests = parseProviderManifests(manifestsResult)
        synchronized(providerCacheLock) {
            providerInstancesCache = instances
            providerManifestsCache = manifests
        }
        return ProviderCatalog(manifests, instances)
    }

    private fun invalidateCachesForEvent(event: WebSocketMessage.EventMessage) {
        val payload = event.data as? JsonObject
        val eventName = event.event.lowercase()
        val mediaType = payload?.stringOrNull("media_type", "mediaType")?.lowercase().orEmpty()
        val uri = payload?.stringOrNull("uri")?.lowercase().orEmpty()
        val itemId = payload?.stringOrNull("item_id", "db_playlist_id", "id")
        val provider = payload?.stringOrNull(
            "provider",
            "provider_instance_id_or_domain",
            "provider_domain",
            "provider_instance"
        )
        if (eventName.contains("playlist") || mediaType.contains("playlist") || uri.contains("playlist")) {
            invalidatePlaylistCaches(itemId, provider)
        }
        if (eventName.contains("album") || mediaType.contains("album") || uri.contains("album")) {
            invalidateAlbumCaches(itemId, provider)
        }
    }

    private fun invalidatePlaylistCaches(itemId: String?, provider: String?) {
        synchronized(cacheLock) {
            if (!itemId.isNullOrBlank() && !provider.isNullOrBlank()) {
                val key = cacheKey(itemId, provider)
                playlistCache.remove(key)
                playlistTracksCache.remove(key)
            } else {
                playlistCache.clear()
                playlistTracksCache.clear()
            }
        }
    }

    private fun invalidateAlbumCaches(itemId: String?, provider: String?) {
        synchronized(cacheLock) {
            if (!itemId.isNullOrBlank() && !provider.isNullOrBlank()) {
                val key = cacheKey(itemId, provider)
                albumCache.remove(key)
                albumTracksCache.remove(key)
            } else {
                albumCache.clear()
                albumTracksCache.clear()
            }
        }
    }

    private suspend fun sendCommand(
        command: String,
        params: Map<String, Any?>
    ): Result<Unit> {
        return runCatching {
            webSocketClient.sendRequest(command, params).getOrThrow()
            Unit
        }
    }

    private suspend fun sendMarkPlayed(
        mediaItem: Map<String, Any?>,
        queueId: String,
        isPlaying: Boolean? = null,
        secondsPlayed: Int? = null,
        fullyPlayed: Boolean? = null
    ): Result<Unit> {
        val params = buildMap {
            put("media_item", mediaItem)
            if (secondsPlayed != null) {
                put("seconds_played", secondsPlayed.coerceAtLeast(0))
            }
            if (fullyPlayed != null) {
                put("fully_played", fullyPlayed)
            }
            if (isPlaying != null) {
                put("is_playing", isPlaying)
            }
            if (queueId.isNotBlank()) {
                put("queue_id", queueId)
            }
        }
        return sendCommand(ApiCommand.MUSIC_MARK_PLAYED, params)
    }

    private fun buildTrackPayload(track: Track): Map<String, Any?>? {
        val itemId = track.itemId
        if (itemId.isBlank()) return null
        val provider = track.provider.ifBlank {
            track.providerMappings.firstOrNull()?.providerDomain
                ?: track.providerMappings.firstOrNull()?.providerInstance
                ?: ""
        }
        if (provider.isBlank()) return null
        val providerMappings = if (track.providerMappings.isNotEmpty()) {
            track.providerMappings.map { mapping ->
                mapOf(
                    "item_id" to mapping.itemId,
                    "provider_domain" to mapping.providerDomain,
                    "provider_instance" to mapping.providerInstance,
                    "available" to mapping.available
                )
            }
        } else {
            listOf(
                mapOf(
                    "item_id" to itemId,
                    "provider_domain" to provider,
                    "provider_instance" to provider,
                    "available" to true
                )
            )
        }
        val name = track.title.ifBlank { itemId }
        return buildMap {
            put("item_id", itemId)
            put("provider", provider)
            put("name", name)
            put("provider_mappings", providerMappings)
            put("media_type", "track")
            if (track.uri.isNotBlank()) {
                put("uri", track.uri)
            }
            if (track.lengthSeconds > 0) {
                put("duration", track.lengthSeconds)
            }
        }
    }

    private fun buildAlbumPayload(track: Track): Map<String, Any?>? {
        val itemId = track.albumItemId
        if (itemId.isBlank()) return null
        val provider = track.albumProvider.ifBlank {
            track.albumProviderMappings.firstOrNull()?.providerDomain
                ?: track.albumProviderMappings.firstOrNull()?.providerInstance
                ?: track.provider
        }
        if (provider.isBlank()) return null
        val providerMappings = if (track.albumProviderMappings.isNotEmpty()) {
            track.albumProviderMappings.map { mapping ->
                mapOf(
                    "item_id" to mapping.itemId,
                    "provider_domain" to mapping.providerDomain,
                    "provider_instance" to mapping.providerInstance,
                    "available" to mapping.available
                )
            }
        } else {
            listOf(
                mapOf(
                    "item_id" to itemId,
                    "provider_domain" to provider,
                    "provider_instance" to provider,
                    "available" to true
                )
            )
        }
        val name = track.album.ifBlank { itemId }
        return buildMap {
            put("item_id", itemId)
            put("provider", provider)
            put("name", name)
            put("provider_mappings", providerMappings)
            put("media_type", "album")
            if (track.albumUri.isNotBlank()) {
                put("uri", track.albumUri)
            }
        }
    }

    private suspend fun <T> fetchPaged(
        command: String,
        limit: Int,
        offset: Int,
        extraParams: Map<String, Any?> = emptyMap(),
        parser: (JsonObject) -> T
    ): Result<List<T>> {
        return runCatching {
            val params = buildMap {
                putAll(extraParams)
                if (limit > 0) {
                    put("limit", limit)
                }
                put("offset", offset.coerceAtLeast(0))
            }
            val result = webSocketClient.sendRequest(command, params).getOrThrow()
            logLibraryTotalsIfPresent(command, result, offset, limit)
            extractItems(result).map(parser)
        }
    }

    private fun logLibraryTotalsIfPresent(
        command: String,
        result: JsonElement?,
        offset: Int,
        limit: Int
    ) {
        if (!command.endsWith("/library_items")) return
        val payload = result as? JsonObject ?: return
        val totals = listOf("total", "total_items", "total_count", "count")
            .mapNotNull { key ->
                payload[key]?.jsonPrimitive?.intOrNull?.let { key to it }
            }
        if (totals.isEmpty()) return
        val itemsCount = (payload["items"] as? JsonArray)?.size ?: extractItems(payload).size
        val totalsLabel = totals.joinToString { (key, value) -> "$key=$value" }
        Logger.i(
            TAG,
            "Library totals for $command (offset=$offset, limit=$limit): $totalsLabel, items=$itemsCount"
        )
    }

    private fun parseSearchResults(result: JsonElement): SearchResults {
        val payload = result as? JsonObject ?: return SearchResults()
        val albums = parseAlbumItems(payload["albums"])
        val artists = parseArtistItems(payload["artists"])
        val playlists = parsePlaylistItems(payload["playlists"])
        val tracks = parseTrackItems(payload["tracks"])
        return SearchResults(
            albums = albums,
            artists = artists,
            playlists = playlists,
            tracks = tracks
        )
    }

    private fun parseAlbumItems(result: JsonElement?): List<Album> {
        return extractItems(result).map { parseAlbum(it) }
    }

    private fun parseArtistItems(result: JsonElement?): List<Artist> {
        return extractItems(result).map { parseArtist(it) }
    }

    private fun parsePlaylistItems(result: JsonElement?): List<Playlist> {
        return extractItems(result).map { parsePlaylist(it) }
    }

    private fun parseTrackItems(result: JsonElement?): List<Track> {
        return extractItems(result).map { parseTrack(it) }
    }

    private fun parsePlayerItems(result: JsonElement?): List<Player> {
        return extractItems(result).map { parsePlayer(it) }
    }

    private fun parseQueueItems(result: JsonElement?): List<Track> {
        return extractItems(result).mapNotNull { parseQueueItemTrack(it) }
    }

    private fun parseQueueItemTrack(jsonObject: JsonObject): Track? {
        val available = jsonObject["available"]?.jsonPrimitive?.booleanOrNull
        val mediaItem = jsonObject["media_item"] as? JsonObject
        if (mediaItem != null) {
            val parsed = parseTrack(mediaItem)
            return if (available == null) parsed else parsed.copy(isAvailable = available)
        }
        val name = jsonObject.stringOrEmpty("name")
        val uri = jsonObject.stringOrEmpty("uri")
        if (name.isBlank() && uri.isBlank()) return null
        val providerMappings = parseProviderMappings(jsonObject["provider_mappings"])
        val mappedAvailability = if (providerMappings.isNotEmpty()) {
            providerMappings.any { it.available }
        } else {
            null
        }
        val isAvailable = available ?: mappedAvailability ?: true
        return Track(
            itemId = jsonObject.stringOrEmpty("queue_item_id", "item_id"),
            provider = jsonObject.stringOrEmpty("provider"),
            providerMappings = providerMappings,
            uri = uri,
            title = name,
            artist = "",
            album = "",
            lengthSeconds = jsonObject.intOrZero("duration"),
            imageUrl = extractImageUrl(jsonObject),
            quality = jsonObject.stringOrNull("quality")
                ?: describeTrackQuality(jsonObject["provider_mappings"]),
            isAvailable = isAvailable
        )
    }

    private fun parsePlaylistObject(result: JsonElement): Playlist {
        return when (result) {
            is JsonObject -> {
                val decoded = runCatching { json.decodeFromJsonElement<Playlist>(result) }
                    .getOrElse { parsePlaylist(result) }
                val imageUrl = extractImageUrl(result) ?: decoded.imageUrl
                val trackCount = result.intOrZero("track_count", "track_total", "total_tracks")
                decoded.copy(imageUrl = imageUrl, trackCount = trackCount)
            }
            else -> {
                Logger.w(TAG, "Unexpected playlist response: $result")
                throw IllegalStateException("Unexpected playlist response")
            }
        }
    }

    private fun extractItems(result: JsonElement?): List<JsonObject> {
        if (result == null || result is JsonNull) return emptyList()
        return when (result) {
            is JsonArray -> result.mapNotNull { it as? JsonObject }
            is JsonObject -> {
                val items = result["items"]
                when (items) {
                    is JsonArray -> items.mapNotNull { it as? JsonObject }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun parseAlbum(jsonObject: JsonObject): Album {
        val artists = parseArtistNames(jsonObject["artists"])
        return Album(
            itemId = jsonObject.stringOrEmpty("item_id"),
            provider = jsonObject.stringOrEmpty("provider"),
            uri = jsonObject.stringOrEmpty("uri"),
            name = jsonObject.stringOrEmpty("name"),
            artists = artists,
            imageUrl = extractImageUrl(jsonObject),
            albumType = parseAlbumType(jsonObject.stringOrNull("album_type")),
            providerMappings = parseProviderMappings(jsonObject["provider_mappings"]),
            addedAt = jsonObject.stringOrNull("timestamp_added", "added_at"),
            lastPlayed = jsonObject.stringOrNull("last_played"),
            trackCount = jsonObject.intOrZero("track_count", "track_total", "total_tracks")
        )
    }

    private fun parseArtist(jsonObject: JsonObject): Artist {
        return Artist(
            itemId = jsonObject.stringOrEmpty("item_id"),
            provider = jsonObject.stringOrEmpty("provider"),
            uri = jsonObject.stringOrEmpty("uri"),
            name = jsonObject.stringOrEmpty("name"),
            sortName = jsonObject.stringOrNull("sort_name"),
            imageUrl = extractImageUrl(jsonObject)
        )
    }

    private fun parsePlaylist(jsonObject: JsonObject): Playlist {
        return Playlist(
            itemId = jsonObject.stringOrEmpty("item_id"),
            provider = jsonObject.stringOrEmpty("provider"),
            uri = jsonObject.stringOrEmpty("uri"),
            name = jsonObject.stringOrEmpty("name"),
            owner = jsonObject.stringOrNull("owner"),
            isEditable = jsonObject.booleanOrFalse("is_editable"),
            imageUrl = extractImageUrl(jsonObject),
            trackCount = jsonObject.intOrZero("track_count", "track_total", "total_tracks")
        )
    }

    private fun parseTrack(jsonObject: JsonObject): Track {
        val artists = parseArtistNames(jsonObject["artists"])
        val artistName = when {
            artists.isNotEmpty() -> artists.joinToString(", ")
            else -> jsonObject.stringOrEmpty("artist_str", "artist")
        }
        val providerMappings = parseProviderMappings(jsonObject["provider_mappings"])
        val available = jsonObject["available"]?.jsonPrimitive?.booleanOrNull
        val mappedAvailability = if (providerMappings.isNotEmpty()) {
            providerMappings.any { it.available }
        } else {
            null
        }
        val isAvailable = available ?: mappedAvailability ?: true
        val albumObject = jsonObject["album"] as? JsonObject
        val albumName = albumObject
            ?.stringOrEmpty("name")
            ?.takeIf { it.isNotBlank() }
            ?: jsonObject.stringOrEmpty("album", "album_name")
        val albumItemId = albumObject?.stringOrEmpty("item_id").orEmpty()
        val albumProvider = albumObject?.stringOrEmpty("provider").orEmpty()
        val albumUri = albumObject?.stringOrEmpty("uri").orEmpty()
        val albumProviderMappings = parseProviderMappings(albumObject?.get("provider_mappings"))
        val isFavorite = jsonObject["is_favorite"]?.jsonPrimitive?.booleanOrNull
            ?: jsonObject["favorite"]?.jsonPrimitive?.booleanOrNull
            ?: false
        return Track(
            itemId = jsonObject.stringOrEmpty("item_id"),
            provider = jsonObject.stringOrEmpty("provider"),
            providerMappings = providerMappings,
            uri = jsonObject.stringOrEmpty("uri"),
            trackNumber = jsonObject.intOrZero("track_number"),
            title = jsonObject.stringOrEmpty("name", "title"),
            artist = artistName,
            album = albumName,
            lengthSeconds = jsonObject.intOrZero("length_seconds", "duration"),
            imageUrl = extractImageUrl(jsonObject)
                ?: albumObject?.let { extractImageUrl(it) },
            quality = jsonObject.stringOrNull("quality")
                ?: describeTrackQuality(jsonObject["provider_mappings"]),
            isAvailable = isAvailable,
            isFavorite = isFavorite,
            albumItemId = albumItemId,
            albumProvider = albumProvider,
            albumProviderMappings = albumProviderMappings,
            albumUri = albumUri
        )
    }

    private fun parsePlayer(jsonObject: JsonObject): Player {
        val deviceInfo = jsonObject["device_info"] as? JsonObject
        val deviceManufacturer = deviceInfo?.stringOrNull("manufacturer")
        val deviceModel = deviceInfo?.stringOrNull("model")
        return Player(
            playerId = jsonObject.stringOrEmpty("player_id"),
            name = jsonObject.stringOrEmpty("name"),
            available = jsonObject.booleanOrFalse("available"),
            enabled = jsonObject.booleanOrFalse("enabled"),
            playbackState = parsePlaybackState(jsonObject.stringOrNull("playback_state", "state")),
            volume = jsonObject.intOrZero("volume_level"),
            volumeMuted = jsonObject["volume_muted"]?.jsonPrimitive?.booleanOrNull,
            deviceManufacturer = deviceManufacturer,
            deviceModel = deviceModel
        )
    }

    private fun parseQueue(jsonObject: JsonObject): Queue {
        val currentItem = (jsonObject["current_item"] as? JsonObject)?.let { parseQueueItemTrack(it) }
        val items = parseQueueItems(jsonObject["items"])
        val elapsedTimeLastUpdated = jsonObject["elapsed_time_last_updated"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.toDoubleOrNull()
        val repeatMode = parseRepeatMode(jsonObject.stringOrNull("repeat_mode"))
        val shuffle = jsonObject.booleanOrFalse("shuffle", "shuffle_enabled")
        return Queue(
            queueId = jsonObject.stringOrEmpty("queue_id"),
            state = parsePlaybackState(jsonObject.stringOrNull("state")),
            repeatMode = repeatMode,
            shuffle = shuffle,
            currentItem = currentItem,
            currentIndex = jsonObject.intOrZero("current_index"),
            elapsedTime = jsonObject.intOrZero("elapsed_time"),
            elapsedTimeLastUpdated = elapsedTimeLastUpdated,
            items = items
        )
    }

    private fun parseProviderMappings(element: JsonElement?): List<ProviderMapping> {
        val list = element as? JsonArray ?: return emptyList()
        return list.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            ProviderMapping(
                itemId = obj.stringOrEmpty("item_id"),
                providerInstance = obj.stringOrEmpty("provider_instance"),
                providerDomain = obj.stringOrEmpty("provider_domain"),
                available = obj.booleanOrFalse("available")
            )
        }
    }

    private fun parseProviderInstances(result: JsonElement?): Map<String, ProviderInstance> {
        return extractItems(result)
            .mapNotNull { parseProviderInstance(it) }
            .associateBy { it.instanceId }
    }

    private fun parseProviderManifests(result: JsonElement?): Map<String, ProviderManifest> {
        return extractItems(result)
            .mapNotNull { parseProviderManifest(it) }
            .associateBy { it.domain }
    }

    private fun parseProviderInstance(jsonObject: JsonObject): ProviderInstance? {
        val instanceId = jsonObject.stringOrNull("instance_id", "provider_instance", "id")
            ?.trim()
            .orEmpty()
        if (instanceId.isBlank()) return null
        val domain = jsonObject.stringOrNull("domain", "provider_domain")?.trim().orEmpty()
        val name = jsonObject.stringOrNull("name")?.trim()
        return ProviderInstance(
            instanceId = instanceId,
            domain = domain,
            name = name
        )
    }

    private fun parseProviderManifest(jsonObject: JsonObject): ProviderManifest? {
        val domain = jsonObject.stringOrNull("domain")?.trim().orEmpty()
        if (domain.isBlank()) return null
        val name = jsonObject.stringOrNull("name")?.trim().orEmpty()
        val icon = jsonObject.stringOrNull("icon")?.trim()
        val iconSvg = jsonObject.stringOrNull("icon_svg")?.trim()
        val iconSvgMonochrome = jsonObject.stringOrNull("icon_svg_monochrome")?.trim()
        val iconSvgDark = jsonObject.stringOrNull("icon_svg_dark")?.trim()
        return ProviderManifest(
            domain = domain,
            name = name,
            icon = icon,
            iconSvg = iconSvg,
            iconSvgMonochrome = iconSvgMonochrome,
            iconSvgDark = iconSvgDark
        )
    }

    private fun resolveProviderManifest(
        providerKey: String,
        providerDomains: List<String>,
        catalog: ProviderCatalog
    ): ProviderManifest? {
        if (providerKey.isNotBlank()) {
            catalog.manifests[providerKey]?.let { return it }
            val instance = catalog.instances[providerKey]
            val mappedDomain = instance?.domain?.trim().orEmpty()
            if (mappedDomain.isNotBlank()) {
                catalog.manifests[mappedDomain]?.let { return it }
            }
        }
        for (domain in providerDomains) {
            val trimmed = domain.trim()
            if (trimmed.isBlank()) continue
            catalog.manifests[trimmed]?.let { return it }
        }
        return null
    }

    private fun describeTrackQuality(providerMappings: JsonElement?): String? {
        val mappings = providerMappings as? JsonArray ?: return null
        val bestMapping = mappings
            .mapNotNull { it as? JsonObject }
            .maxByOrNull { it.doubleOrZero("quality") }
            ?: return null
        val audioFormat = bestMapping["audio_format"] as? JsonObject ?: return null
        val contentType = audioFormat["content_type"]
        val isLossless = isLosslessContentType(contentType)
        if (isLossless) {
            val sampleRate = audioFormat.intOrZero("sample_rate")
            val bitDepth = audioFormat.intOrZero("bit_depth")
            if (sampleRate > 0 && bitDepth > 0) {
                val rateText = formatSampleRateKhz(sampleRate)
                return "Lossless ${rateText}kHz/${bitDepth}-bit"
            }
            return "Lossless"
        }
        val bitRate = audioFormat.intOrZero("bit_rate", "bitrate")
        if (bitRate > 0) {
            return "${bitRate} kbps"
        }
        val outputFormat = audioFormat.stringOrNull("output_format_str")
        if (!outputFormat.isNullOrBlank()) {
            return outputFormat
        }
        val contentTypeLabel = contentTypeString(contentType)
        return contentTypeLabel?.takeIf { it.isNotBlank() }
    }

    private fun isLosslessContentType(contentType: JsonElement?): Boolean {
        when (contentType) {
            is JsonObject -> {
                val explicit = contentType["is_lossless"]?.jsonPrimitive?.booleanOrNull
                if (explicit != null) return explicit
                val value = contentType.stringOrNull("value", "name", "content_type", "type")
                return isLosslessContentTypeString(value)
            }
            is JsonPrimitive -> return isLosslessContentTypeString(contentType.contentOrNull)
            else -> return false
        }
    }

    private fun isLosslessContentTypeString(value: String?): Boolean {
        val normalized = value?.lowercase()?.trim().orEmpty()
        if (normalized.isBlank()) return false
        return normalized.contains("lossless") ||
            normalized.contains("hi_res") ||
            normalized.contains("hi-res") ||
            normalized.contains("hires") ||
            normalized.contains("hi res") ||
            normalized.contains("flac") ||
            normalized.contains("alac") ||
            normalized.contains("wav") ||
            normalized.contains("aiff") ||
            normalized.contains("pcm") ||
            normalized.contains("dsd")
    }

    private fun contentTypeString(contentType: JsonElement?): String? {
        return when (contentType) {
            is JsonObject -> contentType.stringOrNull("value", "name", "content_type", "type")
            is JsonPrimitive -> contentType.contentOrNull
            else -> null
        }
    }

    private fun formatSampleRateKhz(sampleRate: Int): String {
        val rateKhz = if (sampleRate >= 1000) sampleRate / 1000.0 else sampleRate.toDouble()
        val rounded = kotlin.math.round(rateKhz)
        return if (kotlin.math.abs(rateKhz - rounded) < 0.01) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rateKhz)
        }
    }

    private fun parseArtistNames(element: JsonElement?): List<String> {
        val list = element as? JsonArray ?: return emptyList()
        return list.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> item.content
                is JsonObject -> item["name"]?.jsonPrimitive?.contentOrNull
                else -> null
            }
        }
    }

    private fun parseAlbumType(value: String?): AlbumType {
        return when (value?.lowercase()) {
            "album" -> AlbumType.ALBUM
            "single" -> AlbumType.SINGLE
            "compilation" -> AlbumType.COMPILATION
            "ep" -> AlbumType.EP
            else -> AlbumType.UNKNOWN
        }
    }

    private fun parsePlaybackState(value: String?): PlaybackState {
        return when (value?.lowercase()) {
            "playing" -> PlaybackState.PLAYING
            "paused" -> PlaybackState.PAUSED
            "idle" -> PlaybackState.IDLE
            else -> PlaybackState.IDLE
        }
    }

    private fun parseRepeatMode(value: String?): RepeatMode {
        return when (value?.lowercase()) {
            "one" -> RepeatMode.ONE
            "all" -> RepeatMode.ALL
            "off" -> RepeatMode.OFF
            else -> RepeatMode.OFF
        }
    }

    private fun resolveImageUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val base = cachedServerUrl?.trimEnd('/') ?: return raw
        val path = raw.trimStart('/')
        return "$base/$path"
    }

    private fun extractImageUrl(jsonObject: JsonObject): String? {
        val legacy = jsonObject.stringOrNull("image_url")
        if (!legacy.isNullOrBlank()) return resolveImageUrl(legacy)
        val imageElement = jsonObject["image"]
        imageElement?.let { element ->
            val fromImage = imageElementToUrl(element)
            if (!fromImage.isNullOrBlank()) return fromImage
        }
        val metadata = jsonObject["metadata"] as? JsonObject
        val images = metadata?.get("images") as? JsonArray
        return images?.let { imageArrayToUrl(it) }
    }

    private fun imageElementToUrl(element: JsonElement): String? {
        val imageObject = element as? JsonObject ?: return null
        return imageObjectToUrl(imageObject)
    }

    private fun imageArrayToUrl(images: JsonArray): String? {
        val objects = images.mapNotNull { it as? JsonObject }
        val preferred = objects.firstOrNull { it.stringOrNull("type") == "thumb" }
            ?: objects.firstOrNull()
        return preferred?.let { imageObjectToUrl(it) }
    }

    private fun imageObjectToUrl(image: JsonObject): String? {
        val path = image.stringOrNull("path")?.trim().orEmpty()
        if (path.isBlank()) return null
        val provider = image.stringOrNull("provider") ?: "builtin"
        return buildImageProxyUrl(path, provider)
    }

    private fun buildImageProxyUrl(path: String, provider: String?): String? {
        val base = cachedServerUrl?.trimEnd('/') ?: return path
        val encodedPath = runCatching { URLEncoder.encode(path, "UTF-8") }.getOrElse { path }
        val providerParam = provider?.ifBlank { "builtin" } ?: "builtin"
        return "$base/imageproxy?path=$encodedPath&provider=$providerParam"
    }

    private fun RepeatMode.toApiValue(): String {
        return when (this) {
            RepeatMode.OFF -> "off"
            RepeatMode.ONE -> "one"
            RepeatMode.ALL -> "all"
        }
    }

    private fun QueueOption.toApiValue(): String {
        return when (this) {
            QueueOption.REPLACE -> "replace"
            QueueOption.ADD -> "add"
            QueueOption.NEXT -> "next"
            QueueOption.REPLACE_NEXT -> "replace_next"
        }
    }

    private fun cacheKey(itemId: String, provider: String): String {
        return "$itemId:$provider"
    }

    private fun JsonObject.stringOrNull(vararg keys: String): String? {
        for (key in keys) {
            val value = this[key]?.jsonPrimitive?.contentOrNull
            if (!value.isNullOrBlank()) return value
        }
        return null
    }

    private fun JsonObject.stringOrEmpty(vararg keys: String): String {
        return stringOrNull(*keys) ?: ""
    }

    private fun JsonObject.intOrZero(vararg keys: String): Int {
        for (key in keys) {
            val primitive = this[key]?.jsonPrimitive ?: continue
            val intValue = primitive.intOrNull
            if (intValue != null) return intValue
            val doubleValue = primitive.contentOrNull?.toDoubleOrNull()
            if (doubleValue != null) return doubleValue.toInt()
        }
        return 0
    }

    private fun JsonObject.doubleOrZero(vararg keys: String): Double {
        for (key in keys) {
            val value = this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            if (value != null) return value
        }
        return 0.0
    }

    private fun JsonObject.booleanOrFalse(vararg keys: String): Boolean {
        for (key in keys) {
            val value = this[key]?.jsonPrimitive?.booleanOrNull
            if (value != null) return value
        }
        return false
    }

    private data class ProviderCatalog(
        val manifests: Map<String, ProviderManifest>,
        val instances: Map<String, ProviderInstance>
    )

    private data class ProviderManifest(
        val domain: String,
        val name: String,
        val icon: String?,
        val iconSvg: String?,
        val iconSvgMonochrome: String?,
        val iconSvgDark: String?
    )

    private data class ProviderInstance(
        val instanceId: String,
        val domain: String,
        val name: String?
    )

    private fun ProviderManifest.preferredIconSvg(): String? {
        return listOf(iconSvg, iconSvgMonochrome, iconSvgDark)
            .firstOrNull { !it.isNullOrBlank() }
    }

    companion object {
        private const val TAG = "MusicAssistantRepo"
        private const val DEFAULT_PAGE_SIZE = 200
        private const val QUEUE_ITEM_LIMIT = 500
        private const val ALBUM_CACHE_SIZE = 100
        private const val PLAYLIST_CACHE_SIZE = 50
        private const val TRACKS_CACHE_SIZE = 50
    }
}
