package com.harmonixia.android.service.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.harmonixia.android.domain.repository.LocalMediaRepository
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.OfflineLibraryRepository
import com.harmonixia.android.util.NetworkConnectivityManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MediaLibraryBrowserCategoryMetadataTest {

    private val repository = mockk<MusicAssistantRepository>(relaxed = true)
    private val localMediaRepository = mockk<LocalMediaRepository>(relaxed = true)
    private val offlineLibraryRepository = mockk<OfflineLibraryRepository>(relaxed = true)
    private val networkConnectivityManager = mockk<NetworkConnectivityManager>()
    private val context = mockk<Context>(relaxed = true)

    private lateinit var browser: MediaLibraryBrowser

    @Before
    fun setUp() {
        every { networkConnectivityManager.isOfflineMode() } returns false
        browser = MediaLibraryBrowser(
            context = context,
            repository = repository,
            localMediaRepository = localMediaRepository,
            offlineLibraryRepository = offlineLibraryRepository,
            networkConnectivityManager = networkConnectivityManager
        )
    }

    @Test
    fun buildCategoryItem_folderMediaType_marksItemBrowsableAndNotPlayable() {
        val metadata = buildCategoryItem(
            mediaId = "albums",
            title = "Albums",
            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
        ).mediaMetadata

        assertEquals(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS, metadata.mediaType)
        assertEquals(true, metadata.isBrowsable)
        assertEquals(false, metadata.isPlayable)
    }

    @Test
    fun buildCategoryItem_mixedMediaType_preservesBrowsableTrackCollectionSemantics() {
        val metadata = buildCategoryItem(
            mediaId = "home_favorites",
            title = "Favourites",
            mediaType = MediaMetadata.MEDIA_TYPE_MIXED
        ).mediaMetadata

        assertEquals(MediaMetadata.MEDIA_TYPE_MIXED, metadata.mediaType)
        assertEquals(true, metadata.isBrowsable)
        assertEquals(false, metadata.isPlayable)
    }

    @Test
    fun buildCategoryItem_withoutMediaType_stillBuildsBrowsableCategoryItem() {
        val metadata = buildCategoryItem(
            mediaId = "letter_A",
            title = "A",
            mediaType = null
        ).mediaMetadata

        assertEquals(null, metadata.mediaType)
        assertEquals(true, metadata.isBrowsable)
        assertEquals(false, metadata.isPlayable)
    }

    private fun buildCategoryItem(
        mediaId: String,
        title: String,
        mediaType: Int?
    ): MediaItem {
        val method = MediaLibraryBrowser::class.java.getDeclaredMethod(
            "buildCategoryItem",
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            Int::class.javaObjectType,
            Int::class.javaObjectType,
            Int::class.javaObjectType
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(
            browser,
            mediaId,
            title,
            null,
            null,
            mediaType,
            null,
            null
        ) as MediaItem
    }
}
