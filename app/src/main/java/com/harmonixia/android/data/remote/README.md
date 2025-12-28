# Music Assistant WebSocket Notes

This module implements the WebSocket protocol used by the Music Assistant server.

## Message Format

Request:
```json
{
  "message_id": "1",
  "command": "music/albums/library_items",
  "args": {
    "limit": 200,
    "offset": 0
  }
}
```

Response:
```json
{
  "message_id": "1",
  "result": [
    {
      "item_id": "album-id",
      "provider": "spotify",
      "uri": "spotify://album/123",
      "name": "Example Album"
    }
  ]
}
```

Error response:
```json
{
  "message_id": "1",
  "error_code": 6,
  "details": "Authentication required. Please send auth command first."
}
```

Auth request:
```json
{
  "message_id": "2",
  "command": "auth",
  "args": {
    "token": "<long-lived-token>"
  }
}
```

Event:
```json
{
  "event": "player/queue_updated",
  "data": {
    "queue_id": "player-queue"
  }
}
```

## Commands

- players/all
- player_queues/get_active_queue
- player_queues/items
- music/albums/library_items
- music/artists/library_items
- music/playlists/library_items
- music/albums/album_tracks
- music/playlists/playlist_tracks
- music/search
- music/playlists/create_playlist
- music/playlists/add_playlist_tracks
- music/playlists/remove_playlist_tracks
- music/playlists/remove
- player_queues/play_media
- player_queues/pause
- player_queues/resume
- player_queues/next
- player_queues/previous
- player_queues/seek
- player_queues/play_index
- player_queues/clear

## Response Structures

- Library endpoints return either a top-level array or an object with an `items` array.
- Track lists return arrays of track objects under `items` or directly as a list.
- Queue fetch responses can be a single queue object or an object containing `items`.

## Authentication

Send an `auth` command after connecting when you have a token. The server will return
`error_code=6` until authentication succeeds.

## Image URLs

Images are returned as `metadata.images` entries. Use the image proxy endpoint:
```
<serverUrl>/imageproxy?path=<encoded_path>&provider=<provider>
```
