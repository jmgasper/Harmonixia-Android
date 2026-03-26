package com.harmonixia.android.service.playback

import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryBrowserTitleCollationTest {

    @Test
    fun compareTitlesWithCollator_nullCollator_handlesCaseAndAccentEquivalence() {
        val result = compareTitlesWithCollator("Beyoncé", "beyonce", null)

        assertEquals(0, result)
    }

    @Test
    fun compareTitlesWithCollator_nullCollator_maintainsAlphabeticalOrder() {
        val result = compareTitlesWithCollator("Éclair", "Zebra", null)

        assertTrue(result < 0)
    }

    @Test
    fun compareArtistsAlphabetically_whenNamesEquivalent_usesProviderItemTieBreak() {
        val left = artist(itemId = "artist-1", provider = "alpha", name = "Beyoncé")
        val right = artist(itemId = "artist-2", provider = "beta", name = "beyonce")

        val result = compareArtistsAlphabetically(left, right) { first, second ->
            compareTitlesWithCollator(first, second, null)
        }

        assertTrue(result < 0)
    }

    @Test
    fun compareAlbumsAlphabetically_whenTitleAndArtistEquivalent_usesProviderItemTieBreak() {
        val left = album(
            itemId = "album-1",
            provider = "alpha",
            name = "Álbum",
            artistName = "Beyoncé"
        )
        val right = album(
            itemId = "album-2",
            provider = "beta",
            name = "Album",
            artistName = "beyonce"
        )

        val result = compareAlbumsAlphabetically(left, right) { first, second ->
            compareTitlesWithCollator(first, second, null)
        }

        assertTrue(result < 0)
    }

    private fun artist(itemId: String, provider: String, name: String): Artist {
        return Artist(
            itemId = itemId,
            provider = provider,
            uri = "$provider://$itemId",
            name = name
        )
    }

    private fun album(itemId: String, provider: String, name: String, artistName: String): Album {
        return Album(
            itemId = itemId,
            provider = provider,
            uri = "$provider://$itemId",
            name = name,
            artists = listOf(artistName)
        )
    }
}
