package com.harmonixia.android.data.local.mapper

import com.harmonixia.android.data.local.entity.DownloadedAlbumEntity
import com.harmonixia.android.data.local.entity.DownloadedPlaylistEntity
import com.harmonixia.android.data.local.entity.DownloadedTrackEntity
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.AlbumType
import com.harmonixia.android.domain.model.DownloadStatus
import com.harmonixia.android.domain.model.DownloadedTrack
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.model.downloadId

private const val LIST_DELIMITER = '|'

private fun String?.toStringList(): List<String> {
    if (this.isNullOrBlank()) return emptyList()
    return split(LIST_DELIMITER).filter { it.isNotBlank() }
}

private fun List<String>.toStorageString(): String? {
    if (isEmpty()) return null
    return joinToString(LIST_DELIMITER.toString())
}

private fun String?.toDownloadStatusOrDefault(): DownloadStatus {
    if (this.isNullOrBlank()) return DownloadStatus.PENDING
    return runCatching { DownloadStatus.valueOf(this) }.getOrElse { DownloadStatus.PENDING }
}

fun DownloadedTrackEntity.toDomain(): DownloadedTrack {
    val track = Track(
        itemId = itemId,
        provider = provider,
        uri = uri,
        trackNumber = trackNumber,
        title = title,
        artist = artist,
        album = album,
        lengthSeconds = lengthSeconds,
        imageUrl = imageUrl,
        quality = quality
    )
    return DownloadedTrack(
        track = track,
        localFilePath = localFilePath,
        downloadStatus = downloadStatus.toDownloadStatusOrDefault(),
        downloadedAt = downloadedAt,
        fileSize = fileSize,
        coverArtPath = coverArtPath,
        albumId = albumId,
        playlistIds = playlistIds.toStringList()
    )
}

fun DownloadedTrack.toEntity(): DownloadedTrackEntity {
    return DownloadedTrackEntity(
        downloadId = track.downloadId,
        itemId = track.itemId,
        provider = track.provider,
        uri = track.uri,
        trackNumber = track.trackNumber,
        title = track.title,
        artist = track.artist,
        album = track.album,
        lengthSeconds = track.lengthSeconds,
        imageUrl = track.imageUrl,
        quality = track.quality,
        localFilePath = localFilePath,
        downloadStatus = downloadStatus.name,
        downloadedAt = downloadedAt,
        fileSize = fileSize,
        coverArtPath = coverArtPath,
        albumId = albumId,
        playlistIds = playlistIds.toStorageString()
    )
}

fun DownloadedAlbumEntity.toDomain(): Album {
    return Album(
        itemId = albumId,
        provider = provider,
        uri = "",
        name = name,
        artists = artists.toStringList(),
        imageUrl = localCoverArtPath ?: imageUrl,
        albumType = AlbumType.UNKNOWN,
        providerMappings = emptyList(),
        addedAt = null,
        lastPlayed = null
    )
}

fun Album.toDownloadedEntity(
    trackCount: Int = 0,
    localCoverArtPath: String? = null,
    downloadedAt: Long? = null
): DownloadedAlbumEntity {
    return DownloadedAlbumEntity(
        downloadId = downloadId,
        albumId = itemId,
        provider = provider,
        name = name,
        artists = artists.toStorageString() ?: "",
        imageUrl = imageUrl,
        localCoverArtPath = localCoverArtPath,
        downloadedAt = downloadedAt,
        trackCount = trackCount
    )
}

fun DownloadedPlaylistEntity.toDomain(): Playlist {
    return Playlist(
        itemId = playlistId,
        provider = provider,
        uri = "",
        name = name,
        owner = owner,
        isEditable = false,
        imageUrl = localCoverArtPath ?: imageUrl
    )
}

fun Playlist.toDownloadedEntity(
    trackCount: Int = 0,
    localCoverArtPath: String? = null,
    downloadedAt: Long? = null
): DownloadedPlaylistEntity {
    return DownloadedPlaylistEntity(
        downloadId = downloadId,
        playlistId = itemId,
        provider = provider,
        name = name,
        owner = owner,
        imageUrl = imageUrl,
        localCoverArtPath = localCoverArtPath,
        downloadedAt = downloadedAt,
        trackCount = trackCount
    )
}
