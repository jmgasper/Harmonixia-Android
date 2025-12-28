package com.harmonixia.android.service.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.util.Logger
class AudioDeviceManager(
    private val context: Context
) {
    private val audioManager: AudioManager =
        context.getSystemService(AudioManager::class.java)
    private var player: ExoPlayer? = null
    private var receiverRegistered = false
    private var lastUsbDeviceId: Int? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                player?.pause()
            }
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_HEADSET_PLUG != intent?.action) return
            val state = intent.getIntExtra("state", 0)
            if (state == 0) {
                player?.pause()
            }
            refreshDevices()
        }
    }

    fun attachPlayer(player: ExoPlayer) {
        this.player = player
    }

    fun startMonitoring() {
        if (receiverRegistered) return
        context.registerReceiver(
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
        context.registerReceiver(
            headsetReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        )
        receiverRegistered = true
        refreshDevices()
    }

    fun stopMonitoring() {
        if (!receiverRegistered) return
        context.unregisterReceiver(noisyReceiver)
        context.unregisterReceiver(headsetReceiver)
        receiverRegistered = false
    }

    fun refreshDevices() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val usbDac = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        val wasConnected = lastUsbDeviceId != null
        val isConnected = usbDac != null
        if (wasConnected && !isConnected) {
            player?.pause()
        }
        if (usbDac != null && usbDac.id != lastUsbDeviceId) {
            logDeviceInfo(usbDac)
        }
        lastUsbDeviceId = usbDac?.id
        player?.setAudioAttributes(audioAttributes, true)
    }

    private fun logDeviceInfo(device: AudioDeviceInfo) {
        val sampleRates = device.sampleRates.joinToString(prefix = "[", postfix = "]")
        val channelMasks = device.channelMasks.joinToString(prefix = "[", postfix = "]")
        Logger.i(
            TAG,
            "USB audio device detected: id=${device.id}, " +
                "sampleRates=$sampleRates, channelMasks=$channelMasks"
        )
    }

    companion object {
        private const val TAG = "AudioDeviceManager"
    }
}
