package com.harmonixia.android.util

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.domain.repository.OFFLINE_PROVIDER
import kotlin.jvm.JvmName

fun Track.matchesLocal(other: Track): Boolean {
    return normalizeMatchKey(title) == normalizeMatchKey(other.title) &&
        normalizeMatchKey(artist) == normalizeMatchKey(other.artist) &&
        normalizeMatchKey(album) == normalizeMatchKey(other.album)
}

@JvmName("mergeTracksWithLocal")
fun List<Track>.mergeWithLocal(localTracks: List<Track>): List<Track> {
    if (localTracks.isEmpty()) return this
    val usedLocal = BooleanArray(localTracks.size)
    val merged = ArrayList<Track>(size + localTracks.size)
    for (track in this) {
        var matchedIndex = -1
        for (index in localTracks.indices) {
            if (!usedLocal[index] && track.matchesLocal(localTracks[index])) {
                matchedIndex = index
                break
            }
        }
        if (matchedIndex >= 0) {
            usedLocal[matchedIndex] = true
            merged.add(localTracks[matchedIndex])
        } else {
            merged.add(track)
        }
    }
    for (index in localTracks.indices) {
        if (!usedLocal[index]) {
            merged.add(localTracks[index])
        }
    }
    return merged
}

@JvmName("mergeAlbumsWithLocal")
fun List<Album>.mergeWithLocal(localAlbums: List<Album>): List<Album> {
    if (localAlbums.isEmpty()) return this
    val localByKey = linkedMapOf<String, Album>()
    for (album in localAlbums) {
        val key = albumMatchKey(album)
        if (!localByKey.containsKey(key)) {
            localByKey[key] = album
        }
    }
    val merged = ArrayList<Album>(size + localByKey.size)
    val usedKeys = mutableSetOf<String>()
    for (album in this) {
        val key = albumMatchKey(album)
        val local = localByKey[key]
        if (local != null) {
            merged.add(local)
            usedKeys.add(key)
        } else {
            merged.add(album)
        }
    }
    for ((key, album) in localByKey) {
        if (!usedKeys.contains(key)) {
            merged.add(album)
        }
    }
    return merged
}

@JvmName("mergeArtistsWithLocal")
fun List<Artist>.mergeWithLocal(localArtists: List<Artist>): List<Artist> {
    if (localArtists.isEmpty()) return this
    val localByKey = linkedMapOf<String, Artist>()
    for (artist in localArtists) {
        val key = artistMatchKey(artist)
        if (!localByKey.containsKey(key)) {
            localByKey[key] = artist
        }
    }
    val merged = ArrayList<Artist>(size + localByKey.size)
    val usedKeys = mutableSetOf<String>()
    for (artist in this) {
        val key = artistMatchKey(artist)
        val local = localByKey[key]
        if (local != null) {
            merged.add(local)
            usedKeys.add(key)
        } else {
            merged.add(artist)
        }
    }
    for ((key, artist) in localByKey) {
        if (!usedKeys.contains(key)) {
            merged.add(artist)
        }
    }
    return merged
}

val Track.isLocal: Boolean
    get() = provider == OFFLINE_PROVIDER

private fun albumMatchKey(album: Album): String {
    val artist = album.artists.firstOrNull().orEmpty()
    return "${normalizeMatchKey(album.name)}::${normalizeMatchKey(artist)}"
}

private fun artistMatchKey(artist: Artist): String {
    return normalizeMatchKey(artist.name)
}

private fun normalizeMatchKey(value: String): String {
    return value.trim().lowercase()
}
