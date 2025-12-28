package com.harmonixia.android.service.playback

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.harmonixia.android.domain.model.EqBandConfig
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EqualizerManagerIntegrationTest {

    @Test
    fun applyPresetDoesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = ExoPlayer.Builder(context).build()
        val manager = EqualizerManager()

        manager.attachPlayer(player, player.audioSessionId)
        manager.applyPreset(listOf(EqBandConfig(freq = 1000.0, bandwidth = 200.0, gain = 3.0)))
        manager.setEnabled(true)

        manager.release()
        player.release()
    }
}
