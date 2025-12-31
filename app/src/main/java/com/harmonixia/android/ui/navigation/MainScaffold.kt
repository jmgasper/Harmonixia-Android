package com.harmonixia.android.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.harmonixia.android.R
import com.harmonixia.android.domain.usecase.GetConnectionStateUseCase
import com.harmonixia.android.ui.components.ConnectionStatusProvider
import com.harmonixia.android.ui.components.MiniPlayer
import com.harmonixia.android.ui.components.MiniPlayerDefaults
import com.harmonixia.android.ui.playback.NowPlayingUiState
import com.harmonixia.android.ui.playback.PlaybackViewModel
import com.harmonixia.android.ui.screens.settings.SettingsTab

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    getConnectionStateUseCase: GetConnectionStateUseCase,
    onNavigateToSettings: (SettingsTab?) -> Unit,
    modifier: Modifier = Modifier,
    enableSharedArtworkTransition: Boolean,
    onSharedArtworkTransitionChange: (Boolean) -> Unit,
    content: @Composable SharedTransitionScope.(PlaybackViewModel) -> Unit
) {
    val destinations = mainNavigationDestinations()
    val configuration = LocalConfiguration.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isCompactLayout by remember(windowSizeClass, configuration) {
        derivedStateOf {
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        }
    }
    val isExpandedLayout = !isCompactLayout
    val topLevelRoutes = remember(destinations) { destinations.map { it.screen.route }.toSet() }
    val currentTopLevelRoute = currentDestination
        ?.hierarchy
        ?.firstOrNull { it.route in topLevelRoutes }
        ?.route
    var savedTopLevelRoute by rememberSaveable { mutableStateOf(currentTopLevelRoute) }
    LaunchedEffect(currentTopLevelRoute) {
        if (currentTopLevelRoute != null) {
            savedTopLevelRoute = currentTopLevelRoute
        }
    }
    // Scope playback to the activity to survive size-class configuration changes.
    val activity = LocalContext.current as? ComponentActivity
    val playbackViewModel: PlaybackViewModel = if (activity != null) {
        hiltViewModel(activity)
    } else {
        hiltViewModel()
    }
    val connectionState = getConnectionStateUseCase().collectAsStateWithLifecycle()
    val nowPlayingUiState by playbackViewModel.nowPlayingUiState.collectAsStateWithLifecycle()
    val playbackInfo = when (val state = nowPlayingUiState) {
        is NowPlayingUiState.Loading -> state.info
        is NowPlayingUiState.Playing -> state.info
        NowPlayingUiState.Idle -> null
    }
    val isNowPlayingDestination = currentDestination
        ?.hierarchy
        ?.any { it.route == Screen.NowPlaying.route } == true
    val showMiniPlayer = nowPlayingUiState !is NowPlayingUiState.Idle && !isNowPlayingDestination
    val isLoading = nowPlayingUiState is NowPlayingUiState.Loading
    val miniPlayerBottomPadding = if (isCompactLayout) {
        NavigationBarDefaults.windowInsets
            .asPaddingValues()
            .calculateBottomPadding()
    } else {
        0.dp
    }
    val miniPlayerContentPadding = if (showMiniPlayer) {
        MiniPlayerDefaults.totalHeight(isExpandedLayout) + miniPlayerBottomPadding
    } else {
        0.dp
    }
    ConnectionStatusProvider(
        connectionState = connectionState,
        onNavigateToSettings = onNavigateToSettings
    ) {
        SharedTransitionLayout {
            Row(modifier = modifier) {
                if (isExpandedLayout) {
                    NavigationRail {
                        destinations.forEach { destination ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == destination.screen.route } == true ||
                                savedTopLevelRoute == destination.screen.route
                            val contentDescriptionRes = when (destination.screen) {
                                Screen.Home -> R.string.content_desc_nav_home
                                Screen.Albums -> R.string.content_desc_nav_albums
                                Screen.Artists -> R.string.content_desc_nav_artists
                                Screen.Playlists -> R.string.content_desc_nav_playlists
                                Screen.Search -> R.string.content_desc_nav_search
                                else -> null
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    when (destination.screen) {
                                        Screen.Home -> navController.navigateToHome()
                                        Screen.Albums -> navController.navigateToAlbums()
                                        Screen.Artists -> navController.navigateToArtists()
                                        Screen.Playlists -> navController.navigateToPlaylists()
                                        Screen.Search -> navController.navigateToSearch()
                                        else -> Unit
                                    }
                                },
                                icon = {
                                    val contentDescription =
                                        contentDescriptionRes?.let { stringResource(it) }
                                            ?: destination.label
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = contentDescription
                                    )
                                },
                                label = { Text(text = destination.label) }
                            )
                        }
                    }
                }
                key("main_content") {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = miniPlayerContentPadding)
                            ) {
                                content(playbackViewModel)
                            }
                            if (playbackInfo != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = miniPlayerBottomPadding)
                                ) {
                                    MiniPlayer(
                                        playbackInfo = playbackInfo,
                                        onPlayPauseClick = { playbackViewModel.togglePlayPause() },
                                        onPreviousClick = { playbackViewModel.previous() },
                                        onNextClick = { playbackViewModel.next() },
                                        onExpandClick = {
                                            onSharedArtworkTransitionChange(true)
                                            navController.navigateToNowPlaying()
                                        },
                                        isVisible = showMiniPlayer,
                                        isLoading = isLoading,
                                        isExpandedLayout = isExpandedLayout,
                                        enableSharedArtworkTransition = enableSharedArtworkTransition
                                    )
                                }
                            }
                        }
                        if (isCompactLayout) {
                            NavigationBar {
                                destinations.forEach { destination ->
                                    val selected = currentDestination
                                        ?.hierarchy
                                        ?.any { it.route == destination.screen.route } == true ||
                                        savedTopLevelRoute == destination.screen.route
                                    val contentDescriptionRes = when (destination.screen) {
                                        Screen.Home -> R.string.content_desc_nav_home
                                        Screen.Albums -> R.string.content_desc_nav_albums
                                        Screen.Artists -> R.string.content_desc_nav_artists
                                        Screen.Playlists -> R.string.content_desc_nav_playlists
                                        Screen.Search -> R.string.content_desc_nav_search
                                        else -> null
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            when (destination.screen) {
                                                Screen.Home -> navController.navigateToHome()
                                                Screen.Albums -> navController.navigateToAlbums()
                                                Screen.Artists -> navController.navigateToArtists()
                                                Screen.Playlists -> navController.navigateToPlaylists()
                                                Screen.Search -> navController.navigateToSearch()
                                                else -> Unit
                                            }
                                        },
                                        icon = {
                                            val contentDescription =
                                                contentDescriptionRes?.let { stringResource(it) }
                                                    ?: destination.label
                                            Icon(
                                                imageVector = if (selected) {
                                                    destination.selectedIcon
                                                } else {
                                                    destination.unselectedIcon
                                                },
                                                contentDescription = contentDescription
                                            )
                                        },
                                        label = { Text(text = destination.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal val LocalMainScaffoldActions =
    staticCompositionLocalOf<@Composable RowScope.() -> Unit> { {} }

@Composable
fun RowScope.MainScaffoldActions() {
    LocalMainScaffoldActions.current(this)
}
