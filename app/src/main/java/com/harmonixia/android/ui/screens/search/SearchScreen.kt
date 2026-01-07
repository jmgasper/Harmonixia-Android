package com.harmonixia.android.ui.screens.search

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.R
import com.harmonixia.android.domain.model.Album
import com.harmonixia.android.domain.model.Artist
import com.harmonixia.android.domain.model.Playlist
import com.harmonixia.android.domain.model.Track
import com.harmonixia.android.ui.components.ErrorCard
import com.harmonixia.android.ui.components.OfflineModeBanner
import com.harmonixia.android.ui.navigation.MainScaffoldActions
import com.harmonixia.android.ui.screens.settings.SettingsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToSettings: (SettingsTab?) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onTrackClick: (Track) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val imageQualityManager = viewModel.imageQualityManager

    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val horizontalPadding by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 16.dp
                WindowWidthSizeClass.Medium -> 24.dp
                WindowWidthSizeClass.Expanded -> 32.dp
                else -> 16.dp
            }
        }
    }
    val listPadding = remember(horizontalPadding) {
        PaddingValues(horizontal = horizontalPadding, vertical = 16.dp)
    }
    val sectionSpacing by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 8.dp
                WindowWidthSizeClass.Medium -> 12.dp
                WindowWidthSizeClass.Expanded -> 16.dp
                else -> 8.dp
            }
        }
    }
    val showMoreMultiplier by remember(windowSizeClass) {
        derivedStateOf {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 2 else 1
        }
    }
    val searchFieldHeight by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 48.dp
                WindowWidthSizeClass.Medium -> 52.dp
                WindowWidthSizeClass.Expanded -> 52.dp
                else -> 48.dp
            }
        }
    }
    val searchFieldMaxWidth by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 560.dp
                WindowWidthSizeClass.Medium -> 520.dp
                WindowWidthSizeClass.Expanded -> 600.dp
                else -> 560.dp
            }
        }
    }
    val searchFieldEndPadding by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 8.dp
                WindowWidthSizeClass.Medium -> 12.dp
                WindowWidthSizeClass.Expanded -> 16.dp
                else -> 8.dp
            }
        }
    }
    val searchFieldHorizontalPadding by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 12.dp
                WindowWidthSizeClass.Medium -> 16.dp
                WindowWidthSizeClass.Expanded -> 20.dp
                else -> 12.dp
            }
        }
    }
    val searchFieldIconSpacing by remember(windowSizeClass) {
        derivedStateOf {
            when (windowSizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 8.dp
                WindowWidthSizeClass.Medium -> 12.dp
                WindowWidthSizeClass.Expanded -> 12.dp
                else -> 8.dp
            }
        }
    }
    val searchFieldTextStyle = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> MaterialTheme.typography.bodyMedium
        else -> MaterialTheme.typography.bodyLarge
    }

    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }
    val successState = uiState as? SearchUiState.Success

    LaunchedEffect(successState?.query) {
        expandedSections.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        SearchInputField(
                            query = searchQuery,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            onClearQuery = { viewModel.onSearchQueryChanged("") },
                            placeholder = stringResource(R.string.search_query_hint),
                            height = searchFieldHeight,
                            horizontalPadding = searchFieldHorizontalPadding,
                            iconSpacing = searchFieldIconSpacing,
                            textStyle = searchFieldTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = searchFieldMaxWidth)
                                .padding(end = searchFieldEndPadding)
                        )
                    }
                },
                actions = {
                    MainScaffoldActions()
                    IconButton(onClick = { onNavigateToSettings(null) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_open_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = listPadding,
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            if (isOfflineMode) {
                item {
                    OfflineModeBanner(
                        text = stringResource(R.string.offline_search_active),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            when (val state = uiState) {
                    SearchUiState.Idle -> {
                        item {
                            SearchCenteredMessage(
                                text = stringResource(R.string.search_empty_query)
                            )
                        }
                    }
                    SearchUiState.Loading -> {
                        item {
                            SearchLoadingState()
                        }
                    }
                    is SearchUiState.Error -> {
                        item {
                            SearchErrorState(
                                message = state.message.ifBlank {
                                    stringResource(R.string.search_error)
                                },
                                onRetry = viewModel::retrySearch
                            )
                        }
                    }
                    is SearchUiState.Success -> {
                        if (state.isEmpty) {
                            item {
                                SearchCenteredMessage(
                                    text = stringResource(R.string.search_no_results, state.query)
                                )
                            }
                        } else {
                            val results = state.results
                            val playlistResults = results.playlists
                                .take(state.playlistLimit * showMoreMultiplier)
                            val albumResults = results.albums
                                .take(state.albumLimit * showMoreMultiplier)
                            val artistResults = results.artists
                                .take(state.artistLimit * showMoreMultiplier)
                            val trackResults = results.tracks
                                .take(state.trackLimit * showMoreMultiplier)
                            val showMorePlaylists = results.playlists.size > playlistResults.size
                            val showMoreAlbums = results.albums.size > albumResults.size
                            val showMoreArtists = results.artists.size > artistResults.size
                            val showMoreTracks = results.tracks.size > trackResults.size

                            item {
                                val isExpanded = expandedSections.getOrPut(SECTION_PLAYLISTS) { true }
                                SearchResultsSection(
                                    title = stringResource(R.string.search_section_playlists),
                                    itemCount = results.playlists.size,
                                    isExpanded = isExpanded,
                                    onToggleExpanded = {
                                        expandedSections[SECTION_PLAYLISTS] = !isExpanded
                                    }
                                ) {
                                    if (playlistResults.isEmpty()) {
                                        SearchSectionEmptyMessage(
                                            text = stringResource(R.string.playlists_empty)
                                        )
                                    } else {
                                        PlaylistsResultsGrid(
                                            playlists = playlistResults,
                                            onPlaylistClick = onPlaylistClick,
                                            windowSizeClass = windowSizeClass,
                                            imageQualityManager = imageQualityManager
                                        )
                                    }
                                    if (showMorePlaylists) {
                                        ShowMoreButton(onClick = viewModel::showMorePlaylists)
                                    }
                                }
                            }
                            item {
                                val isExpanded = expandedSections.getOrPut(SECTION_ALBUMS) { true }
                                SearchResultsSection(
                                    title = stringResource(R.string.search_section_albums),
                                    itemCount = results.albums.size,
                                    isExpanded = isExpanded,
                                    onToggleExpanded = {
                                        expandedSections[SECTION_ALBUMS] = !isExpanded
                                    }
                                ) {
                                    if (albumResults.isEmpty()) {
                                        SearchSectionEmptyMessage(
                                            text = stringResource(R.string.albums_empty)
                                        )
                                    } else {
                                        AlbumsResultsGrid(
                                            albums = albumResults,
                                            onAlbumClick = onAlbumClick,
                                            windowSizeClass = windowSizeClass,
                                            isOfflineMode = isOfflineMode,
                                            imageQualityManager = imageQualityManager
                                        )
                                    }
                                    if (showMoreAlbums) {
                                        ShowMoreButton(onClick = viewModel::showMoreAlbums)
                                    }
                                }
                            }
                            item {
                                val isExpanded = expandedSections.getOrPut(SECTION_ARTISTS) { true }
                                SearchResultsSection(
                                    title = stringResource(R.string.search_section_artists),
                                    itemCount = results.artists.size,
                                    isExpanded = isExpanded,
                                    onToggleExpanded = {
                                        expandedSections[SECTION_ARTISTS] = !isExpanded
                                    }
                                ) {
                                    if (artistResults.isEmpty()) {
                                        SearchSectionEmptyMessage(
                                            text = stringResource(R.string.artists_empty)
                                        )
                                    } else {
                                        ArtistsResultsList(
                                            artists = artistResults,
                                            onArtistClick = onArtistClick,
                                            imageQualityManager = imageQualityManager
                                        )
                                    }
                                    if (showMoreArtists) {
                                        ShowMoreButton(onClick = viewModel::showMoreArtists)
                                    }
                                }
                            }
                            item {
                                val isExpanded = expandedSections.getOrPut(SECTION_TRACKS) { true }
                                SearchResultsSection(
                                    title = stringResource(R.string.search_section_tracks),
                                    itemCount = results.tracks.size,
                                    isExpanded = isExpanded,
                                    onToggleExpanded = {
                                        expandedSections[SECTION_TRACKS] = !isExpanded
                                    }
                                ) {
                                    if (trackResults.isEmpty()) {
                                        SearchSectionEmptyMessage(
                                            text = stringResource(R.string.album_detail_no_tracks)
                                        )
                                    } else {
                                        TracksResultsList(
                                            tracks = trackResults,
                                            onTrackClick = onTrackClick
                                        )
                                    }
                                    if (showMoreTracks) {
                                        ShowMoreButton(onClick = viewModel::showMoreTracks)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun ShowMoreButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.search_show_more))
    }
}

@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    placeholder: String,
    height: Dp,
    horizontalPadding: Dp,
    iconSpacing: Dp,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val iconTint = if (isFocused) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = if (isFocused) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else {
            null
        },
        modifier = modifier.height(height)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(iconSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = iconTint
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isBlank()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotBlank()) {
                IconButton(onClick = onClearQuery) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.content_desc_clear_search),
                        tint = iconTint
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchCenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SearchLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.search_loading),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SearchErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ErrorCard(message = message)
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
private fun SearchSectionEmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private const val SECTION_PLAYLISTS = "playlists"
private const val SECTION_ALBUMS = "albums"
private const val SECTION_ARTISTS = "artists"
private const val SECTION_TRACKS = "tracks"
