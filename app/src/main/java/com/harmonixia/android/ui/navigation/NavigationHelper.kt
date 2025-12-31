package com.harmonixia.android.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.navOptions
import com.harmonixia.android.ui.screens.settings.SettingsTab
import com.harmonixia.android.util.Logger

fun NavController.navigateToSettings(tab: SettingsTab? = null) {
    val suffix = tab?.let { " tab=$it" }.orEmpty()
    Logger.i(TAG, "Navigate to Settings$suffix")
    navigate(Screen.Settings.createRoute(tab))
}

fun NavController.navigateToHome() {
    Logger.i(TAG, "Navigate to Home")
    val startDestination = graph.findStartDestination()
    if (startDestination.route == Screen.Onboarding.route) {
        navigate(
            Screen.Home.route,
            navOptions {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
                launchSingleTop = true
            }
        )
        return
    }
    navigate(
        Screen.Home.route,
        navOptions {
            popUpTo(startDestination.id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    )
}

fun NavController.navigateToAlbums() {
    Logger.i(TAG, "Navigate to Albums")
    navigate(
        Screen.Albums.route,
        navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    )
}

fun NavController.navigateToArtists() {
    Logger.i(TAG, "Navigate to Artists")
    navigate(
        Screen.Artists.route,
        navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    )
}

fun NavController.navigateToPlaylists() {
    Logger.i(TAG, "Navigate to Playlists")
    navigate(
        Screen.Playlists.route,
        navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    )
}

fun NavController.navigateToSearch() {
    Logger.i(TAG, "Navigate to Search")
    navigate(
        Screen.Search.route,
        navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    )
}

fun NavController.navigateToNowPlaying() {
    Logger.i(TAG, "Navigate to Now Playing")
    navigate(
        Screen.NowPlaying.route,
        navOptions {
            launchSingleTop = true
        }
    )
}

fun NavHostController.navigateToAlbumDetail(
    albumId: String,
    provider: String
) {
    Logger.i(TAG, "Navigate to AlbumDetail")
    navigate(
        Screen.AlbumDetail.createRoute(albumId, provider),
        navOptions {
            launchSingleTop = true
            restoreState = true
        }
    )
}

fun NavHostController.navigateToArtistDetail(
    artistId: String,
    provider: String
) {
    Logger.i(TAG, "Navigate to ArtistDetail")
    navigate(
        Screen.ArtistDetail.createRoute(artistId, provider),
        navOptions {
            launchSingleTop = true
            restoreState = true
        }
    )
}

fun NavController.navigateToOnboarding() {
    Logger.i(TAG, "Navigate to Onboarding")
    navigate(
        Screen.Onboarding.route,
        navOptions {
            popUpTo(graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    )
}

private const val TAG = "Navigation"
