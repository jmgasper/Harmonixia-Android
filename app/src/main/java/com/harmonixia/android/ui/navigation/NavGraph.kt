package com.harmonixia.android.ui.navigation

import android.app.Activity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.NavType
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.ui.screens.albums.AlbumDetailScreen
import com.harmonixia.android.ui.screens.albums.AlbumsScreen
import com.harmonixia.android.ui.screens.artists.ArtistDetailScreen
import com.harmonixia.android.ui.screens.artists.ArtistsScreen
import com.harmonixia.android.ui.screens.home.HomeScreen
import com.harmonixia.android.ui.screens.nowplaying.NowPlayingScreen
import com.harmonixia.android.ui.screens.onboarding.OnboardingScreen
import com.harmonixia.android.ui.screens.playlists.PlaylistDetailScreen
import com.harmonixia.android.ui.screens.playlists.PlaylistsScreen
import com.harmonixia.android.ui.screens.search.SearchScreen
import com.harmonixia.android.ui.screens.settings.PerformanceSettingsScreen
import com.harmonixia.android.ui.screens.settings.SettingsScreen
import com.harmonixia.android.ui.screens.settings.eq.EqSettingsScreen
import com.harmonixia.android.util.Logger
import kotlinx.coroutines.android.awaitFrame

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraph(
    navController: NavHostController,
    settingsDataStore: SettingsDataStore
) {
    val serverUrl by settingsDataStore.getServerUrl().collectAsStateWithLifecycle(initialValue = "")
    val startDestination = if (serverUrl.isBlank()) {
        Screen.Onboarding.route
    } else {
        Screen.Home.route
    }
    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val mainNavController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            NavigationPerformanceLogger(screenName = "Onboarding")
            OnboardingScreen(
                onNavigateToHome = { navController.navigateToHome() }
            )
        }
        composable(
            route = Screen.Settings.route,
            deepLinks = listOf(navDeepLink { uriPattern = "harmonixia://settings" })
        ) {
            NavigationPerformanceLogger(screenName = "Settings")
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEqSettings = { navController.navigate(Screen.SettingsEqualizer.route) },
                onNavigateToPerformanceSettings = {
                    navController.navigate(Screen.PerformanceSettings.route)
                }
            )
        }
        composable(Screen.SettingsEqualizer.route) {
            NavigationPerformanceLogger(screenName = "SettingsEqualizer")
            EqSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PerformanceSettings.route) {
            NavigationPerformanceLogger(screenName = "PerformanceSettings")
            PerformanceSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            NavigationPerformanceLogger(screenName = "Main")
            MainScaffold(
                navController = mainNavController,
                windowSizeClass = windowSizeClass,
                modifier = Modifier.fillMaxSize()
            ) { playbackViewModel ->
                NavHost(
                    navController = mainNavController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        NavigationPerformanceLogger(screenName = "Home")
                        HomeScreen(
                            onNavigateToSettings = { navController.navigateToSettings() },
                            onAlbumClick = { album ->
                                mainNavController.navigateToAlbumDetail(album.itemId, album.provider)
                            }
                        )
                    }
                    composable(Screen.Albums.route) {
                        NavigationPerformanceLogger(screenName = "Albums")
                        AlbumsScreen(
                            onNavigateToSettings = { navController.navigateToSettings() },
                            onAlbumClick = { album ->
                                mainNavController.navigateToAlbumDetail(album.itemId, album.provider)
                            }
                        )
                    }
                    composable(Screen.Artists.route) {
                        NavigationPerformanceLogger(screenName = "Artists")
                        ArtistsScreen(
                            onNavigateToSettings = { navController.navigateToSettings() },
                            onArtistClick = { artist ->
                                mainNavController.navigateToArtistDetail(artist.itemId, artist.provider)
                            }
                        )
                    }
                    composable(Screen.Playlists.route) {
                        NavigationPerformanceLogger(screenName = "Playlists")
                        PlaylistsScreen(
                            onNavigateToSettings = { navController.navigateToSettings() },
                            onPlaylistClick = { playlist ->
                                mainNavController.navigate(
                                    Screen.PlaylistDetail.createRoute(
                                        playlist.itemId,
                                        playlist.provider
                                    )
                                )
                            }
                        )
                    }
                    composable(Screen.Search.route) {
                        NavigationPerformanceLogger(screenName = "Search")
                        SearchScreen(
                            onNavigateToSettings = { navController.navigateToSettings() },
                            onAlbumClick = { album ->
                                mainNavController.navigateToAlbumDetail(album.itemId, album.provider)
                            },
                            onArtistClick = { artist ->
                                mainNavController.navigateToArtistDetail(artist.itemId, artist.provider)
                            },
                            onPlaylistClick = { playlist ->
                                mainNavController.navigate(
                                    Screen.PlaylistDetail.createRoute(
                                        playlist.itemId,
                                        playlist.provider
                                    )
                                )
                            },
                            onTrackClick = { }
                        )
                    }
                    composable(Screen.NowPlaying.route) {
                        NavigationPerformanceLogger(screenName = "NowPlaying")
                        NowPlayingScreen(
                            onNavigateBack = { mainNavController.popBackStack() },
                            viewModel = playbackViewModel
                        )
                    }
                    composable(
                        route = Screen.AlbumDetail.route,
                        arguments = listOf(
                            navArgument(Screen.AlbumDetail.ARG_ALBUM_ID) { type = NavType.StringType },
                            navArgument(Screen.AlbumDetail.ARG_PROVIDER) { type = NavType.StringType }
                        )
                    ) {
                        NavigationPerformanceLogger(screenName = "AlbumDetail")
                        AlbumDetailScreen(
                            onNavigateBack = { mainNavController.popBackStack() },
                            onNavigateToSettings = { navController.navigateToSettings() }
                        )
                    }
                    composable(
                        route = Screen.ArtistDetail.route,
                        arguments = listOf(
                            navArgument(Screen.ArtistDetail.ARG_ARTIST_ID) { type = NavType.StringType },
                            navArgument(Screen.ArtistDetail.ARG_PROVIDER) { type = NavType.StringType }
                        )
                    ) {
                        NavigationPerformanceLogger(screenName = "ArtistDetail")
                        ArtistDetailScreen(
                            onNavigateBack = { mainNavController.popBackStack() },
                            onNavigateToSettings = { navController.navigateToSettings() },
                            onAlbumClick = { album ->
                                mainNavController.navigateToAlbumDetail(album.itemId, album.provider)
                            }
                        )
                    }
                    composable(
                        route = Screen.PlaylistDetail.route,
                        arguments = listOf(
                            navArgument(Screen.PlaylistDetail.ARG_PLAYLIST_ID) { type = NavType.StringType },
                            navArgument(Screen.PlaylistDetail.ARG_PROVIDER) { type = NavType.StringType }
                        )
                    ) {
                        NavigationPerformanceLogger(screenName = "PlaylistDetail")
                        PlaylistDetailScreen(
                            onNavigateBack = { mainNavController.popBackStack() },
                            onNavigateToSettings = { navController.navigateToSettings() },
                            onNavigateToPlaylist = { playlist ->
                                mainNavController.popBackStack()
                                mainNavController.navigate(
                                    Screen.PlaylistDetail.createRoute(
                                        playlist.itemId,
                                        playlist.provider
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationPerformanceLogger(screenName: String) {
    val startTime = remember(screenName) { System.currentTimeMillis() }
    DisposableEffect(screenName) {
        Logger.d(TAG, "Screen $screenName start=$startTime")
        onDispose {
            val endTime = System.currentTimeMillis()
            Logger.d(TAG, "Screen $screenName stop=$endTime tti=${endTime - startTime}ms")
        }
    }
    LaunchedEffect(screenName) {
        awaitFrame()
        val tti = System.currentTimeMillis() - startTime
        Logger.d(TAG, "Screen $screenName tti=${tti}ms")
    }
}

private const val TAG = "Navigation"
