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
import com.harmonixia.android.domain.model.ProviderMapping
import com.harmonixia.android.domain.model.Queue
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.domain.model.SearchResults
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.NetworkError
import com.harmonixia.android.util.toNetworkError
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

@Singleton
class MusicAssistantRepositoryImpl @Inject constructor(
    private val webSocketClient: MusicAssistantWebSocketClient,
    private val json: Json
) : MusicAssistantRepository {
    private var cachedServerUrl: String? = null
    private var cachedAuthToken: String? = null

    override suspend fun connect(serverUrl: String, authToken: String): Result<Unit> {
        cachedServerUrl = serverUrl
        cachedAuthToken = authToken
        return webSocketClient.connect(serverUrl, authToken)
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
            parseAlbum(payload)
        }
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
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_GET_ALBUM_TRACKS,
                mapOf("item_id" to albumId, "provider_instance_id_or_domain" to provider)
            ).getOrThrow()
            parseTrackItems(result)
        }
    }

    override suspend fun getPlaylistTracks(playlistId: String, provider: String): Result<List<Track>> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.MUSIC_GET_PLAYLIST_TRACKS,
                mapOf(
                    "item_id" to playlistId,
                    "provider_instance_id_or_domain" to provider,
                    "force_refresh" to false
                )
            ).getOrThrow()
            parseTrackItems(result)
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
                if (match != null) return@runCatching match
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

    override suspend fun getActiveQueue(playerId: String): Result<Queue?> {
        return runCatching {
            val result = webSocketClient.sendRequest(
                ApiCommand.PLAYER_QUEUES_FETCH_STATE,
                mapOf("player_id" to playerId)
            ).getOrThrow()
            val queue = (result as? JsonObject)?.let { parseQueue(it) }
            queue ?: return@runCatching null
            if (queue.queueId.isBlank()) return@runCatching queue
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

    override suspend fun clearQueue(queueId: String): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYER_QUEUES_CLEAR,
            mapOf("queue_id" to queueId)
        )
    }

    override suspend fun setPlayerVolume(playerId: String, volume: Int): Result<Unit> {
        return sendCommand(
            ApiCommand.PLAYERS_VOLUME_SET,
            mapOf("player_id" to playerId, "volume" to volume)
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
            mapOf("db_playlist_id" to playlistId, "positions" to positions)
        )
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

    private suspend fun <T> fetchPaged(
        command: String,
        limit: Int,
        offset: Int,
        extraParams: Map<String, Any?> = emptyMap(),
        parser: (JsonObject) -> T
    ): Result<List<T>> {
        return runCatching {
            val pageSize = if (limit > 0) limit else DEFAULT_PAGE_SIZE
            val params = buildMap {
                putAll(extraParams)
                put("limit", pageSize)
                put("offset", offset.coerceAtLeast(0))
            }
            val result = webSocketClient.sendRequest(command, params).getOrThrow()
            extractItems(result).map(parser)
        }
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
        val mediaItem = jsonObject["media_item"] as? JsonObject
        if (mediaItem != null) {
            return parseTrack(mediaItem)
        }
        val name = jsonObject.stringOrEmpty("name")
        val uri = jsonObject.stringOrEmpty("uri")
        if (name.isBlank() && uri.isBlank()) return null
        return Track(
            itemId = jsonObject.stringOrEmpty("queue_item_id", "item_id"),
            provider = jsonObject.stringOrEmpty("provider"),
            uri = uri,
            title = name,
            artist = "",
            album = "",
            lengthSeconds = jsonObject.intOrZero("duration"),
            imageUrl = extractImageUrl(jsonObject),
            quality = null
        )
    }

    private fun parsePlaylistObject(result: JsonElement): Playlist {
        return when (result) {
            is JsonObject -> {
                val decoded = runCatching { json.decodeFromJsonElement<Playlist>(result) }
                    .getOrElse { parsePlaylist(result) }
                val imageUrl = extractImageUrl(result) ?: decoded.imageUrl
                decoded.copy(imageUrl = imageUrl)
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
            lastPlayed = jsonObject.stringOrNull("last_played")
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
            imageUrl = extractImageUrl(jsonObject)
        )
    }

    private fun parseTrack(jsonObject: JsonObject): Track {
        val artists = parseArtistNames(jsonObject["artists"])
        val artistName = when {
            artists.isNotEmpty() -> artists.joinToString(", ")
            else -> jsonObject.stringOrEmpty("artist_str", "artist")
        }
        val albumName = (jsonObject["album"] as? JsonObject)
            ?.stringOrEmpty("name")
            ?.takeIf { it.isNotBlank() }
            ?: jsonObject.stringOrEmpty("album", "album_name")
        return Track(
            itemId = jsonObject.stringOrEmpty("item_id"),
            provider = jsonObject.stringOrEmpty("provider"),
            uri = jsonObject.stringOrEmpty("uri"),
            trackNumber = jsonObject.intOrZero("track_number"),
            title = jsonObject.stringOrEmpty("name", "title"),
            artist = artistName,
            album = albumName,
            lengthSeconds = jsonObject.intOrZero("length_seconds", "duration"),
            imageUrl = extractImageUrl(jsonObject)
                ?: (jsonObject["album"] as? JsonObject)?.let { extractImageUrl(it) },
            quality = jsonObject.stringOrNull("quality")
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
            volume = jsonObject.intOrZero("volume_level"),
            deviceManufacturer = deviceManufacturer,
            deviceModel = deviceModel
        )
    }

    private fun parseQueue(jsonObject: JsonObject): Queue {
        val currentItem = (jsonObject["current_item"] as? JsonObject)?.let { parseQueueItemTrack(it) }
        val items = parseQueueItems(jsonObject["items"])
        return Queue(
            queueId = jsonObject.stringOrEmpty("queue_id"),
            state = parsePlaybackState(jsonObject.stringOrNull("state")),
            currentItem = currentItem,
            currentIndex = jsonObject.intOrZero("current_index"),
            elapsedTime = jsonObject.intOrZero("elapsed_time"),
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

    private fun QueueOption.toApiValue(): String {
        return when (this) {
            QueueOption.REPLACE -> "replace"
            QueueOption.ADD -> "add"
            QueueOption.NEXT -> "next"
            QueueOption.REPLACE_NEXT -> "replace_next"
        }
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

    private fun JsonObject.booleanOrFalse(key: String): Boolean {
        return this[key]?.jsonPrimitive?.booleanOrNull ?: false
    }

    companion object {
        private const val TAG = "MusicAssistantRepo"
        private const val DEFAULT_PAGE_SIZE = 200
        private const val QUEUE_ITEM_LIMIT = 500
    }
}
