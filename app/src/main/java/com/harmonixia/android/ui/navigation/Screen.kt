package com.harmonixia.android.ui.navigation

import com.harmonixia.android.ui.screens.settings.SettingsTab

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Settings : Screen("settings?tab={tab}") {
        const val ARG_TAB = "tab"
        private const val BASE_ROUTE = "settings"

        fun createRoute(tab: SettingsTab? = null): String {
            return if (tab == null) {
                BASE_ROUTE
            } else {
                "$BASE_ROUTE?$ARG_TAB=${tab.name}"
            }
        }
    }
    @Deprecated(
        "EQ settings are now inline in the main Settings screen. " +
            "Use Screen.Settings.createRoute(SettingsTab.EQUALIZER) instead"
    )
    data object SettingsEqualizer : Screen("settings/equalizer")
    data object PerformanceSettings : Screen("settings/performance")
    data object Home : Screen("home")
    data object NowPlaying : Screen("now_playing")
    data object Albums : Screen("albums")
    data object AlbumDetail : Screen("album_detail/{albumId}/{provider}") {
        const val ARG_ALBUM_ID = "albumId"
        const val ARG_PROVIDER = "provider"

        fun createRoute(albumId: String, provider: String): String {
            return "album_detail/$albumId/$provider"
        }
    }
    data object Artists : Screen("artists")
    data object ArtistDetail : Screen("artist_detail/{artistId}/{provider}") {
        const val ARG_ARTIST_ID = "artistId"
        const val ARG_PROVIDER = "provider"

        fun createRoute(artistId: String, provider: String): String {
            return "artist_detail/$artistId/$provider"
        }
    }
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}/{provider}") {
        const val ARG_PLAYLIST_ID = "playlistId"
        const val ARG_PROVIDER = "provider"

        fun createRoute(playlistId: String, provider: String): String {
            return "playlist_detail/$playlistId/$provider"
        }
    }
    data object Playlists : Screen("playlists")
    data object Search : Screen("search")
}
