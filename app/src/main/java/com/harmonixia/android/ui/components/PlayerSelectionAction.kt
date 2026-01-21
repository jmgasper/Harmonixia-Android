package com.harmonixia.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harmonixia.android.ui.playback.PlaybackViewModel

@Composable
fun PlayerSelectionAction(
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val availablePlayers by playbackViewModel.availablePlayers.collectAsStateWithLifecycle()
    val selectedPlayer by playbackViewModel.selectedPlayer.collectAsStateWithLifecycle()
    val localPlayerId by playbackViewModel.localPlayerId.collectAsStateWithLifecycle()
    var showPlayerDialog by remember { mutableStateOf(false) }

    PlayerIndicatorPill(
        selectedPlayer = selectedPlayer,
        localPlayerId = localPlayerId,
        onClick = {
            playbackViewModel.refreshPlayers()
            showPlayerDialog = true
        },
        modifier = modifier
    )

    if (showPlayerDialog) {
        PlayerSelectionDialog(
            players = availablePlayers,
            selectedPlayer = selectedPlayer,
            localPlayerId = localPlayerId,
            onPlayerSelected = { player ->
                playbackViewModel.selectPlayer(player)
            },
            onPlayerVolumeChange = { player, volume ->
                playbackViewModel.setPlayerVolume(player, volume)
            },
            onPlayerMuteChange = { player, muted ->
                playbackViewModel.setPlayerMute(player, muted)
            },
            onReconnect = {
                playbackViewModel.requestLocalPlayerReconnect()
            },
            onDismiss = { showPlayerDialog = false }
        )
    }
}
