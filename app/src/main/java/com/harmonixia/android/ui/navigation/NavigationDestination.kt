package com.harmonixia.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.harmonixia.android.R

data class NavigationDestination(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun mainNavigationDestinations(): List<NavigationDestination> = listOf(
    NavigationDestination(
        screen = Screen.Home,
        label = stringResource(R.string.nav_home),
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    NavigationDestination(
        screen = Screen.Albums,
        label = stringResource(R.string.nav_albums),
        selectedIcon = Icons.Filled.Album,
        unselectedIcon = Icons.Outlined.Album
    ),
    NavigationDestination(
        screen = Screen.Artists,
        label = stringResource(R.string.nav_artists),
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    ),
    NavigationDestination(
        screen = Screen.Playlists,
        label = stringResource(R.string.nav_playlists),
        selectedIcon = Icons.Filled.QueueMusic,
        unselectedIcon = Icons.Outlined.QueueMusic
    ),
    NavigationDestination(
        screen = Screen.Search,
        label = stringResource(R.string.nav_search),
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )
)
