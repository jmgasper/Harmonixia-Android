package com.harmonixia.android.ui.screens.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.ui.components.AlbumCard
import com.harmonixia.android.ui.components.ArtistListItem
import com.harmonixia.android.ui.components.PlaylistCard
import com.harmonixia.android.ui.components.TrackList

@Composable
fun PlaylistsResultsGrid(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = gridColumns(windowSizeClass, isLandscape)
    val spacing = gridSpacing(windowSizeClass, isLandscape)
    val artworkSize = gridArtworkSize(windowSizeClass)
    val cardHeight = artworkSize + 96.dp
    val gridHeight = gridHeight(playlists.size, columns, cardHeight, spacing)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)
    ) {
        items(
            items = playlists,
            key = { playlist -> "${playlist.provider}:${playlist.itemId}" }
        ) { playlist ->
            PlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                isGrid = true,
                gridArtworkSize = artworkSize,
                modifier = Modifier.height(cardHeight)
            )
        }
    }
}

@Composable
fun AlbumsResultsGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = gridColumns(windowSizeClass, isLandscape)
    val spacing = gridSpacing(windowSizeClass, isLandscape)
    val artworkSize = gridArtworkSize(windowSizeClass)
    val cardHeight = artworkSize + 70.dp
    val gridHeight = gridHeight(albums.size, columns, cardHeight, spacing)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)
    ) {
        items(
            items = albums,
            key = { album -> "${album.provider}:${album.itemId}" }
        ) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) },
                artworkSize = artworkSize,
                modifier = Modifier.height(cardHeight)
            )
        }
    }
}

@Composable
fun ArtistsResultsList(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    val rowHeight = 76.dp
    val listHeight = rowHeight * artists.size

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(listHeight),
        contentPadding = PaddingValues(0.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(
            items = artists,
            key = { _, artist -> "${artist.provider}:${artist.itemId}" }
        ) { index, artist ->
            ArtistListItem(
                artist = artist,
                onClick = { onArtistClick(artist) },
                showDivider = index < artists.lastIndex
            )
        }
    }
}

@Composable
fun TracksResultsList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    val rowHeight = 76.dp
    val listHeight = rowHeight * tracks.size

    TrackList(
        tracks = tracks,
        onTrackClick = onTrackClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(listHeight),
        contentPadding = PaddingValues(0.dp),
        showContextMenu = false
    )
}

private fun gridColumns(windowSizeClass: WindowSizeClass, isLandscape: Boolean): Int {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> if (isLandscape) 3 else 2
        WindowWidthSizeClass.Medium -> if (isLandscape) 5 else 4
        WindowWidthSizeClass.Expanded -> if (isLandscape) 10 else 8
        else -> 2
    }
}

private fun gridSpacing(windowSizeClass: WindowSizeClass, isLandscape: Boolean): Dp {
    val baseSpacing = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 8.dp
        WindowWidthSizeClass.Medium -> 12.dp
        WindowWidthSizeClass.Expanded -> 20.dp
        else -> 8.dp
    }
    return if (isLandscape) baseSpacing * 0.75f else baseSpacing
}

private fun gridArtworkSize(windowSizeClass: WindowSizeClass): Dp {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 120.dp
        WindowWidthSizeClass.Medium -> 150.dp
        WindowWidthSizeClass.Expanded -> 180.dp
        else -> 120.dp
    }
}

private fun gridHeight(
    itemCount: Int,
    columns: Int,
    itemHeight: Dp,
    spacing: Dp
): Dp {
    val safeColumns = columns.coerceAtLeast(1)
    val rows = (itemCount + safeColumns - 1) / safeColumns
    return if (rows == 0) {
        0.dp
    } else {
        (itemHeight * rows) + (spacing * (rows - 1))
    }
}
