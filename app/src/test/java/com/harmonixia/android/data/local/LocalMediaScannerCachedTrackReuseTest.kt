package com.harmonixia.android.data.local

import com.harmonixia.android.data.local.entity.LocalTrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalMediaScannerCachedTrackReuseTest {

    @Test
    fun `returns null when cached track is missing`() {
        val reused = reuseCachedTrackEntityIfUnchanged(
            cachedTrack = null,
            lastModified = 100L,
            fileSize = 42L,
            mimeType = "audio/flac"
        )

        assertNull(reused)
    }

    @Test
    fun `returns null when file metadata changed`() {
        val cached = localTrack(
            id = 9L,
            filePath = "content://library/song.flac",
            title = "Song",
            artist = "Artist",
            album = "Album",
            albumArtist = "Artist",
            trackNumber = 2,
            durationMs = 120000L,
            mimeType = "audio/flac",
            fileSize = 1_024L,
            lastModified = 5_000L,
            dateAdded = 4_000L
        )

        val reused = reuseCachedTrackEntityIfUnchanged(
            cachedTrack = cached,
            lastModified = 5_001L,
            fileSize = 1_024L,
            mimeType = "audio/flac"
        )

        assertNull(reused)
    }

    @Test
    fun `reuses cached metadata and resets id when file metadata is unchanged`() {
        val cached = localTrack(
            id = 99L,
            filePath = "content://library/song.flac",
            title = "Song",
            artist = "Artist",
            album = "Album",
            albumArtist = "Artist",
            trackNumber = 3,
            durationMs = 123456L,
            mimeType = "audio/old",
            fileSize = 9_999L,
            lastModified = 8_888L,
            dateAdded = 7_777L
        )

        val reused = reuseCachedTrackEntityIfUnchanged(
            cachedTrack = cached,
            lastModified = 8_888L,
            fileSize = 9_999L,
            mimeType = "audio/flac"
        )

        requireNotNull(reused)
        assertEquals(0L, reused.id)
        assertEquals(cached.filePath, reused.filePath)
        assertEquals(cached.title, reused.title)
        assertEquals(cached.artist, reused.artist)
        assertEquals(cached.album, reused.album)
        assertEquals(cached.albumArtist, reused.albumArtist)
        assertEquals(cached.trackNumber, reused.trackNumber)
        assertEquals(cached.durationMs, reused.durationMs)
        assertEquals("audio/flac", reused.mimeType)
        assertEquals(9_999L, reused.fileSize)
        assertEquals(8_888L, reused.lastModified)
        assertEquals(cached.dateAdded, reused.dateAdded)
    }

    private fun localTrack(
        id: Long,
        filePath: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String?,
        trackNumber: Int,
        durationMs: Long,
        mimeType: String,
        fileSize: Long,
        lastModified: Long,
        dateAdded: Long
    ): LocalTrackEntity {
        return LocalTrackEntity(
            id = id,
            filePath = filePath,
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            trackNumber = trackNumber,
            durationMs = durationMs,
            mimeType = mimeType,
            fileSize = fileSize,
            lastModified = lastModified,
            dateAdded = dateAdded
        )
    }
}
