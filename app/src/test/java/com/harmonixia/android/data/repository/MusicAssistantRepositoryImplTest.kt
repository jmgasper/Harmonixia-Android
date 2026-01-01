package com.harmonixia.android.data.repository

import com.harmonixia.android.data.remote.ApiCommand
import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.data.remote.MusicAssistantWebSocketClient
import com.harmonixia.android.domain.model.QueueOption
import com.harmonixia.android.util.PerformanceMonitor
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicAssistantRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val okHttpClient = OkHttpClient()
    private val performanceMonitor = PerformanceMonitor()

    @Test
    fun fetchAlbums_returnsAlbums() = runBlocking {
        val resultPayload = buildJsonObject {
            put(
                "items",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("item_id", JsonPrimitive("album-1"))
                            put("provider", JsonPrimitive("test"))
                            put("uri", JsonPrimitive("test://album-1"))
                            put("name", JsonPrimitive("Album One"))
                            put(
                                "artists",
                                buildJsonArray {
                                    add(JsonPrimitive("Artist One"))
                                }
                            )
                        }
                    )
                )
            )
        }
        val client = FakeMusicAssistantWebSocketClient { command, _ ->
            assertEquals(ApiCommand.MUSIC_GET_LIBRARY_ALBUMS, command)
            Result.success(resultPayload)
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.fetchAlbums(200, 0)

        assertTrue(result.isSuccess)
        val albums = result.getOrThrow()
        assertEquals(1, albums.size)
        assertEquals("Album One", albums.first().name)
    }

    @Test
    fun fetchArtists_returnsArtists() = runBlocking {
        val resultPayload = buildJsonObject {
            put(
                "items",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("item_id", JsonPrimitive("artist-1"))
                            put("provider", JsonPrimitive("test"))
                            put("uri", JsonPrimitive("test://artist-1"))
                            put("name", JsonPrimitive("Artist One"))
                        }
                    )
                )
            )
        }
        val client = FakeMusicAssistantWebSocketClient { command, _ ->
            assertEquals(ApiCommand.MUSIC_GET_LIBRARY_ARTISTS, command)
            Result.success(resultPayload)
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.fetchArtists(200, 0)

        assertTrue(result.isSuccess)
        val artists = result.getOrThrow()
        assertEquals(1, artists.size)
        assertEquals("Artist One", artists.first().name)
    }

    @Test
    fun fetchPlaylists_returnsPlaylists() = runBlocking {
        val resultPayload = buildJsonObject {
            put(
                "items",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("item_id", JsonPrimitive("playlist-1"))
                            put("provider", JsonPrimitive("test"))
                            put("uri", JsonPrimitive("test://playlist-1"))
                            put("name", JsonPrimitive("Playlist One"))
                            put("is_editable", JsonPrimitive(true))
                        }
                    )
                )
            )
        }
        val client = FakeMusicAssistantWebSocketClient { command, _ ->
            assertEquals(ApiCommand.MUSIC_GET_LIBRARY_PLAYLISTS, command)
            Result.success(resultPayload)
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.fetchPlaylists(200, 0)

        assertTrue(result.isSuccess)
        val playlists = result.getOrThrow()
        assertEquals(1, playlists.size)
        assertEquals("Playlist One", playlists.first().name)
        assertTrue(playlists.first().isEditable)
    }

    @Test
    fun fetchAlbums_returnsSinglePage() = runBlocking {
        val calls = AtomicInteger(0)
        val client = FakeMusicAssistantWebSocketClient { command, params ->
            assertEquals(ApiCommand.MUSIC_GET_LIBRARY_ALBUMS, command)
            val offset = params["offset"] as Int
            val limit = params["limit"] as Int
            val size = if (offset == 0) limit else 50
            val items = (0 until size).map { index ->
                buildJsonObject {
                    put("item_id", JsonPrimitive("album-${offset + index}"))
                    put("provider", JsonPrimitive("test"))
                    put("uri", JsonPrimitive("test://album-${offset + index}"))
                    put("name", JsonPrimitive("Album ${offset + index}"))
                    put(
                        "artists",
                        buildJsonArray {
                            add(JsonPrimitive("Artist"))
                        }
                    )
                }
            }
            calls.incrementAndGet()
            Result.success(buildJsonObject { put("items", JsonArray(items)) })
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.fetchAlbums(200, 0)

        assertTrue(result.isSuccess)
        assertEquals(200, result.getOrThrow().size)
        assertEquals(1, calls.get())
    }

    @Test
    fun searchLibrary_returnsResults() = runBlocking {
        val resultPayload = buildJsonObject {
            put(
                "albums",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("item_id", JsonPrimitive("album-1"))
                            put("provider", JsonPrimitive("test"))
                            put("uri", JsonPrimitive("test://album-1"))
                            put("name", JsonPrimitive("Album One"))
                        }
                    )
                }
            )
            put(
                "artists",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("item_id", JsonPrimitive("artist-1"))
                            put("provider", JsonPrimitive("test"))
                            put("uri", JsonPrimitive("test://artist-1"))
                            put("name", JsonPrimitive("Artist One"))
                        }
                    )
                }
            )
            put(
                "playlists",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("item_id", JsonPrimitive("playlist-1"))
                            put("provider", JsonPrimitive("test"))
                            put("uri", JsonPrimitive("test://playlist-1"))
                            put("name", JsonPrimitive("Playlist One"))
                        }
                    )
                }
            )
            put(
                "tracks",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("item_id", JsonPrimitive("track-1"))
                            put("provider", JsonPrimitive("test"))
                            put("uri", JsonPrimitive("test://track-1"))
                            put("name", JsonPrimitive("Track One"))
                            put("artist", JsonPrimitive("Artist One"))
                            put("album", JsonPrimitive("Album One"))
                            put("duration", JsonPrimitive(200))
                        }
                    )
                }
            )
        }
        val client = FakeMusicAssistantWebSocketClient { command, params ->
            assertEquals(ApiCommand.MUSIC_SEARCH, command)
            assertEquals("query", params["search_query"])
            assertEquals(25, params["limit"])
            assertEquals(true, params["library_only"])
            Result.success(resultPayload)
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.searchLibrary("query", 25)

        assertTrue(result.isSuccess)
        val searchResults = result.getOrThrow()
        assertEquals(1, searchResults.albums.size)
        assertEquals("Album One", searchResults.albums.first().name)
        assertEquals(1, searchResults.tracks.size)
    }

    @Test
    fun getAlbumTracks_returnsTracks() = runBlocking {
        val resultPayload = buildJsonArray {
            add(
                buildJsonObject {
                    put("item_id", JsonPrimitive("track-1"))
                    put("provider", JsonPrimitive("test"))
                    put("uri", JsonPrimitive("test://track-1"))
                    put("name", JsonPrimitive("Track One"))
                    put("artist", JsonPrimitive("Artist One"))
                    put("album", JsonPrimitive("Album One"))
                    put("duration", JsonPrimitive(180))
                }
            )
        }
        val client = FakeMusicAssistantWebSocketClient { command, params ->
            assertEquals(ApiCommand.MUSIC_GET_ALBUM_TRACKS, command)
            assertEquals("album-1", params["item_id"])
            assertEquals("test", params["provider_instance_id_or_domain"])
            Result.success(resultPayload)
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.getAlbumTracks("album-1", "test")

        assertTrue(result.isSuccess)
        val tracks = result.getOrThrow()
        assertEquals(1, tracks.size)
        assertEquals("Track One", tracks.first().title)
    }

    @Test
    fun fetchPlayers_returnsPlayers() = runBlocking {
        val resultPayload = buildJsonArray {
            add(
                buildJsonObject {
                    put("player_id", JsonPrimitive("player-1"))
                    put("name", JsonPrimitive("Living Room"))
                    put("available", JsonPrimitive(true))
                    put("enabled", JsonPrimitive(true))
                    put("volume_level", JsonPrimitive(25))
                }
            )
        }
        val client = FakeMusicAssistantWebSocketClient { command, _ ->
            assertEquals(ApiCommand.PLAYERS_FETCH_STATE, command)
            Result.success(resultPayload)
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.fetchPlayers()

        assertTrue(result.isSuccess)
        val players = result.getOrThrow()
        assertEquals(1, players.size)
        assertEquals("Living Room", players.first().name)
    }

    @Test
    fun getActiveQueue_returnsQueue() = runBlocking {
        val queuePayload = buildJsonObject {
            put("queue_id", JsonPrimitive("queue-1"))
            put("state", JsonPrimitive("playing"))
            put("current_index", JsonPrimitive(0))
            put("elapsed_time", JsonPrimitive(12))
        }
        val itemsPayload = buildJsonArray {
            add(
                buildJsonObject {
                    put("media_item", buildJsonObject {
                        put("item_id", JsonPrimitive("track-1"))
                        put("provider", JsonPrimitive("test"))
                        put("uri", JsonPrimitive("test://track-1"))
                        put("name", JsonPrimitive("Track One"))
                        put("artist", JsonPrimitive("Artist One"))
                        put("album", JsonPrimitive("Album One"))
                        put("duration", JsonPrimitive(120))
                    })
                }
            )
        }
        val client = FakeMusicAssistantWebSocketClient { command, params ->
            when (command) {
                ApiCommand.PLAYER_QUEUES_FETCH_STATE -> {
                    assertEquals("player-1", params["player_id"])
                    Result.success(queuePayload)
                }
                ApiCommand.PLAYER_QUEUES_ITEMS -> {
                    assertEquals("queue-1", params["queue_id"])
                    Result.success(itemsPayload)
                }
                else -> Result.failure(IllegalStateException("Unexpected command"))
            }
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.getActiveQueue("player-1")

        assertTrue(result.isSuccess)
        val queue = result.getOrThrow()
        assertEquals("queue-1", queue?.queueId)
        assertEquals(1, queue?.items?.size)
    }

    @Test
    fun fetchArtists_propagatesErrors() = runBlocking {
        val client = FakeMusicAssistantWebSocketClient { _, _ ->
            Result.failure(IllegalStateException("Network error"))
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.fetchArtists(200, 0)

        assertTrue(result.isFailure)
    }

    @Test
    fun playMedia_propagatesErrors() = runBlocking {
        val client = FakeMusicAssistantWebSocketClient { command, _ ->
            assertEquals(ApiCommand.PLAYER_QUEUES_PLAY_MEDIA, command)
            Result.failure(IllegalStateException("Server error"))
        }
        val repository = MusicAssistantRepositoryImpl(client, okHttpClient, json, performanceMonitor)

        val result = repository.playMedia("queue-1", listOf("test://track-1"), QueueOption.REPLACE)

        assertTrue(result.isFailure)
    }

    private class FakeMusicAssistantWebSocketClient(
        private val handler: (String, Map<String, Any?>) -> Result<JsonElement>
    ) : MusicAssistantWebSocketClient(OkHttpClient(), Json { ignoreUnknownKeys = true }) {
        private val state = MutableStateFlow<ConnectionState>(ConnectionState.Connected)

        override val connectionState: StateFlow<ConnectionState> = state

        override suspend fun sendRequest(
            command: String,
            params: Map<String, Any?>
        ): Result<JsonElement> = handler(command, params)

        override suspend fun connect(serverUrl: String, authToken: String?): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun disconnect() {
            state.value = ConnectionState.Disconnected
        }
    }
}
