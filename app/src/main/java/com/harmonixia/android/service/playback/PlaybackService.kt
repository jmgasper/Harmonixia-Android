package com.harmonixia.android.service.playback

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.Logger
import com.harmonixia.android.util.PerformanceMonitor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var repository: MusicAssistantRepository
    @Inject lateinit var playbackStateManager: PlaybackStateManager
    @Inject lateinit var queueManager: QueueManager
    @Inject lateinit var audioDeviceManager: AudioDeviceManager
    @Inject lateinit var playbackNotificationManager: PlaybackNotificationManager
    @Inject lateinit var volumeHandler: VolumeHandler
    @Inject lateinit var equalizerManager: EqualizerManager
    @Inject lateinit var sendspinPlaybackManager: SendspinPlaybackManager
    @Inject lateinit var performanceMonitor: PerformanceMonitor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(SilenceMediaSourceFactory())
            .build()

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { setReferenceCounted(false) }

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        val trackId = player.currentMediaItem?.mediaId.orEmpty()
                        performanceMonitor.markPlaybackStarted(trackId)
                        acquireWakeLock()
                    } else {
                        releaseWakeLock()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val label = when (playbackState) {
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        Player.STATE_IDLE -> "IDLE"
                        else -> "UNKNOWN"
                    }
                    Logger.d(TAG, "Playback state: $label")
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    Logger.d(TAG, "Buffering: $isLoading")
                }

                override fun onVolumeChanged(volume: Float) {
                    sendspinPlaybackManager.onPlayerVolumeChanged(volume)
                }
            }
        )

        queueManager.attachPlayer(player)
        playbackStateManager.attachPlayer(player)
        audioDeviceManager.attachPlayer(player)
        volumeHandler.attachPlayer(player)
        sendspinPlaybackManager.attachPlayer(player)

        val sessionCallback = PlaybackSessionCallback(
            player = player,
            repository = repository,
            playbackStateManager = playbackStateManager,
            queueManager = queueManager,
            performanceMonitor = performanceMonitor,
            scope = serviceScope
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()

        playbackNotificationManager.ensureNotificationChannel()
        setMediaNotificationProvider(playbackNotificationManager.notificationProvider)

        playbackStateManager.start()
        audioDeviceManager.startMonitoring()
        volumeHandler.startMonitoring()
        sendspinPlaybackManager.start()

        serviceScope.launch {
            playbackStateManager.queueIdFlow.collect { queueId ->
                queueManager.updateQueueId(queueId)
            }
        }

        serviceScope.launch {
            playbackStateManager.playerIdFlow.collect { playerId ->
                volumeHandler.setPlayerId(playerId)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!player.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeHandler.stopMonitoring()
        audioDeviceManager.stopMonitoring()
        playbackStateManager.stop()
        sendspinPlaybackManager.stop()
        mediaSession?.release()
        mediaSession = null
        equalizerManager.release()
        player.release()
        releaseWakeLock()
        serviceScope.cancel()
        Logger.d(TAG, "Playback service destroyed")
    }

    private fun acquireWakeLock() {
        val lock = wakeLock ?: return
        if (!lock.isHeld) {
            lock.acquire()
        }
    }

    private fun releaseWakeLock() {
        val lock = wakeLock ?: return
        if (lock.isHeld) {
            lock.release()
        }
    }

    companion object {
        private const val TAG = "PlaybackService"
        private const val WAKE_LOCK_TAG = "Harmonixia:PlaybackWakeLock"
    }
}
