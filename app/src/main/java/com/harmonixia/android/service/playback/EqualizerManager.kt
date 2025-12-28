package com.harmonixia.android.service.playback

import android.media.audiofx.Equalizer
import androidx.media3.exoplayer.ExoPlayer
import com.harmonixia.android.domain.model.EqBandConfig
import com.harmonixia.android.util.Logger
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

class EqualizerManager {
    private val lock = Any()
    private var equalizer: Equalizer? = null
    private var audioSessionId: Int? = null
    private var isEnabled = false
    private var bandLevels: ShortArray? = null
    private var currentBands: List<EqBandConfig> = emptyList()

    fun attachPlayer(player: ExoPlayer, audioSessionId: Int) {
        attachAudioSession(audioSessionId)
    }

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        synchronized(lock) {
            if (this.audioSessionId == audioSessionId && equalizer != null) return
            releaseInternal()
            try {
                val newEqualizer = Equalizer(0, audioSessionId)
                newEqualizer.enabled = isEnabled
                equalizer = newEqualizer
                this.audioSessionId = audioSessionId
                if (currentBands.isNotEmpty()) {
                    applyPresetInternal(currentBands)
                }
                Logger.i(TAG, "Equalizer attached to session $audioSessionId")
            } catch (error: Exception) {
                Logger.w(TAG, "Failed to attach equalizer", error)
            }
        }
    }

    fun applyPreset(bands: List<EqBandConfig>) {
        synchronized(lock) {
            currentBands = bands
            applyPresetInternal(bands)
        }
    }

    fun setEnabled(enabled: Boolean) {
        synchronized(lock) {
            isEnabled = enabled
            equalizer?.enabled = enabled
        }
    }

    fun release() {
        synchronized(lock) {
            releaseInternal()
        }
    }

    private fun applyPresetInternal(bands: List<EqBandConfig>) {
        val eq = equalizer ?: return
        if (bands.isEmpty()) return
        val bandCount = eq.numberOfBands.toInt()
        val levelRange = eq.bandLevelRange
        val minLevel = levelRange[0].toInt()
        val maxLevel = levelRange[1].toInt()
        val levels = ShortArray(bandCount)

        for (index in 0 until bandCount) {
            val centerHz = eq.getCenterFreq(index.toShort()) / 1000.0
            val gainDb = calculateGainAtFrequency(centerHz, bands)
            val levelMb = (gainDb * 100.0).roundToInt().coerceIn(minLevel, maxLevel)
            eq.setBandLevel(index.toShort(), levelMb.toShort())
            levels[index] = levelMb.toShort()
        }
        bandLevels = levels
    }

    private fun calculateGainAtFrequency(freq: Double, bands: List<EqBandConfig>): Double {
        var gain = 0.0
        bands.forEach { band ->
            val bandwidth = if (band.bandwidth > 0) band.bandwidth else band.freq * 0.5
            val relativeWidth = max(MIN_RELATIVE_WIDTH, bandwidth / band.freq)
            val distance = abs(ln(freq / band.freq))
            val weight = exp(-0.5 * (distance / relativeWidth) * (distance / relativeWidth))
            gain += band.gain * weight
        }
        return gain
    }

    private fun releaseInternal() {
        try {
            equalizer?.release()
        } catch (error: Exception) {
            Logger.w(TAG, "Failed to release equalizer", error)
        } finally {
            equalizer = null
            audioSessionId = null
            bandLevels = null
        }
    }

    companion object {
        private const val TAG = "EqualizerManager"
        private const val MIN_RELATIVE_WIDTH = 0.05
    }
}
