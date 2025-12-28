package com.harmonixia.android.service.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.Player
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VolumeHandler(
    private val context: Context,
    private val repository: MusicAssistantRepository,
    private val audioDeviceManager: AudioDeviceManager
) : Player.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val audioManager: AudioManager =
        context.getSystemService(AudioManager::class.java)

    private var playerId: String? = null
    private var receiverRegistered = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            audioDeviceManager.refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            audioDeviceManager.refreshDevices()
        }
    }

    fun attachPlayer(player: Player) {
        player.addListener(this)
    }

    fun setPlayerId(playerId: String?) {
        this.playerId = playerId
    }

    fun startMonitoring() {
        if (receiverRegistered) return
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        receiverRegistered = true
    }

    fun stopMonitoring() {
        if (!receiverRegistered) return
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        receiverRegistered = false
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        val playerId = playerId ?: return
        val streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        scope.launch {
            repository.setPlayerVolume(playerId, streamVolume)
                .onFailure { Logger.w(TAG, "Failed to update player volume", it) }
        }
    }

    companion object {
        private const val TAG = "VolumeHandler"
    }
}
