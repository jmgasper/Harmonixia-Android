package com.harmonixia.android.service.playback

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.harmonixia.android.domain.model.RepeatMode
import com.harmonixia.android.domain.model.Track
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
class PlaybackService : MediaLibraryService() {
    @Inject lateinit var repository: MusicAssistantRepository
    @Inject lateinit var playbackStateManager: PlaybackStateManager
    @Inject lateinit var queueManager: QueueManager
    @Inject lateinit var audioDeviceManager: AudioDeviceManager
    @Inject lateinit var playbackNotificationManager: PlaybackNotificationManager
    @Inject lateinit var volumeHandler: VolumeHandler
    @Inject lateinit var equalizerManager: EqualizerManager
    @Inject lateinit var sendspinPlaybackManager: SendspinPlaybackManager
    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var mediaLibraryBrowser: MediaLibraryBrowser

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaLibrarySession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastPlaybackState: Int = Player.STATE_IDLE
    private var lastMediaItemId: String? = null
    private var lastMediaItemDurationSeconds: Int = 0
    private var lastReportedStartKey: String? = null

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
                    val timestamp = System.currentTimeMillis()
                    Logger.d(
                        TAG,
                        "Playback isPlaying=$isPlaying mediaId=${player.currentMediaItem?.mediaId.orEmpty()} at $timestamp"
                    )
                    if (isPlaying) {
                        val trackId = player.currentMediaItem?.mediaId.orEmpty()
                        performanceMonitor.markPlaybackStarted(trackId)
                        reportTrackStarted(player.currentMediaItem, timestamp)
                        acquireWakeLock()
                    } else {
                        releaseWakeLock()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val timestamp = System.currentTimeMillis()
                    val label = when (playbackState) {
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        Player.STATE_IDLE -> "IDLE"
                        else -> "UNKNOWN"
                    }
                    Logger.d(TAG, "Playback state: $label at $timestamp")
                    if (playbackState == Player.STATE_READY) {
                        refreshLastMediaItemDuration()
                    }
                    if (playbackState == Player.STATE_ENDED && lastPlaybackState != Player.STATE_ENDED) {
                        reportCurrentTrackCompleted(timestamp)
                    }
                    lastPlaybackState = playbackState
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val timestamp = System.currentTimeMillis()
                    val reasonLabel = when (reason) {
                        Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "AUTO"
                        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "PLAYLIST_CHANGED"
                        Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "REPEAT"
                        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "SEEK"
                        else -> "OTHER"
                    }
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                        reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                    ) {
                        reportPreviousTrackCompleted(timestamp)
                    }
                    cacheCurrentMediaItem(mediaItem)
                    if (player.isPlaying) {
                        reportTrackStarted(mediaItem, timestamp)
                    }
                    Logger.d(
                        TAG,
                        "Media item transition: reason=$reasonLabel mediaId=${mediaItem?.mediaId.orEmpty()} at $timestamp"
                    )
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    Logger.d(TAG, "Buffering: $isLoading")
                }

                override fun onVolumeChanged(volume: Float) {
                    sendspinPlaybackManager.onPlayerVolumeChanged(volume)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    updateMediaButtonPreferences(player.repeatMode, shuffleModeEnabled)
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    updateMediaButtonPreferences(repeatMode, player.shuffleModeEnabled)
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
            mediaLibraryBrowser = mediaLibraryBrowser,
            performanceMonitor = performanceMonitor,
            scope = serviceScope
        )

        mediaSession = MediaLibrarySession.Builder(this, player, sessionCallback)
            .build()
        updateMediaButtonPreferences(player.repeatMode, player.shuffleModeEnabled)

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

        serviceScope.launch {
            playbackStateManager.shuffle.collect { shuffle ->
                queueManager.updateShuffleState(shuffle)
                if (player.shuffleModeEnabled != shuffle) {
                    player.shuffleModeEnabled = shuffle
                }
            }
        }

        serviceScope.launch {
            playbackStateManager.repeatMode.collect { repeatMode ->
                val playerRepeatMode = repeatMode.toPlayerRepeatMode()
                if (player.repeatMode != playerRepeatMode) {
                    player.repeatMode = playerRepeatMode
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
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

    private fun reportCurrentTrackCompleted(timestamp: Long) {
        val mediaId = player.currentMediaItem?.mediaId
        val track = playbackStateManager.resolveTrack(mediaId) ?: return
        val durationSeconds = resolveDurationSeconds(track)
        reportTrackCompleted(track, durationSeconds, timestamp)
    }

    private fun reportPreviousTrackCompleted(timestamp: Long) {
        val previousMediaId = lastMediaItemId ?: return
        val track = playbackStateManager.resolveTrack(previousMediaId) ?: return
        val durationSeconds = lastMediaItemDurationSeconds.takeIf { it > 0 } ?: track.lengthSeconds
        reportTrackCompleted(track, durationSeconds, timestamp)
    }

    private fun reportTrackCompleted(track: Track, durationSeconds: Int, timestamp: Long) {
        val queueId = playbackStateManager.currentQueueId ?: return
        Logger.d(
            TAG,
            "Reporting track completion queueId=$queueId mediaId=${track.itemId} duration=$durationSeconds at $timestamp"
        )
        serviceScope.launch(Dispatchers.IO) {
            repository.reportTrackCompleted(queueId, track, durationSeconds)
                .onFailure { Logger.w(TAG, "Track completion report failed", it) }
        }
        lastReportedStartKey = null
    }

    private fun reportTrackStarted(mediaItem: MediaItem?, timestamp: Long) {
        val mediaId = mediaItem?.mediaId ?: return
        val queueId = playbackStateManager.currentQueueId ?: return
        val key = "$queueId:$mediaId"
        if (key == lastReportedStartKey) return
        val track = playbackStateManager.resolveTrack(mediaId) ?: return
        val positionSeconds = (player.currentPosition / 1000L).toInt()
        Logger.d(
            TAG,
            "Reporting track start queueId=$queueId mediaId=${track.itemId} position=$positionSeconds at $timestamp"
        )
        serviceScope.launch(Dispatchers.IO) {
            repository.reportPlaybackProgress(queueId, track, positionSeconds)
                .onFailure { Logger.w(TAG, "Track start report failed", it) }
        }
        lastReportedStartKey = key
    }

    private fun cacheCurrentMediaItem(mediaItem: MediaItem?) {
        lastMediaItemId = mediaItem?.mediaId
        lastMediaItemDurationSeconds = resolveDurationSecondsForMediaItem(mediaItem)
    }

    private fun refreshLastMediaItemDuration() {
        val mediaItem = player.currentMediaItem ?: return
        if (lastMediaItemId != mediaItem.mediaId) {
            cacheCurrentMediaItem(mediaItem)
            return
        }
        lastMediaItemDurationSeconds = resolveDurationSecondsForMediaItem(mediaItem)
    }

    private fun resolveDurationSecondsForMediaItem(mediaItem: MediaItem?): Int {
        val track = playbackStateManager.resolveTrack(mediaItem?.mediaId)
        return if (track != null) resolveDurationSeconds(track) else resolvePlayerDurationSeconds()
    }

    private fun resolveDurationSeconds(track: Track): Int {
        val playerDuration = player.duration
        val playerSeconds = if (playerDuration > 0) (playerDuration / 1000L).toInt() else 0
        return if (playerSeconds > 0) playerSeconds else track.lengthSeconds
    }

    private fun resolvePlayerDurationSeconds(): Int {
        val playerDuration = player.duration
        return if (playerDuration > 0) (playerDuration / 1000L).toInt() else 0
    }

    private fun updateMediaButtonPreferences(
        @Player.RepeatMode repeatMode: Int,
        shuffleModeEnabled: Boolean
    ) {
        val session = mediaSession ?: return
        val shuffleIcon = if (shuffleModeEnabled) {
            CommandButton.ICON_SHUFFLE_ON
        } else {
            CommandButton.ICON_SHUFFLE_OFF
        }
        val shuffleButton = CommandButton.Builder(shuffleIcon)
            .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, !shuffleModeEnabled)
            .setDisplayName("Shuffle")
            .build()

        val (repeatIcon, nextRepeatMode) = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE to Player.REPEAT_MODE_OFF
            Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL to Player.REPEAT_MODE_ONE
            else -> CommandButton.ICON_REPEAT_OFF to Player.REPEAT_MODE_ALL
        }
        val repeatButton = CommandButton.Builder(repeatIcon)
            .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, nextRepeatMode)
            .setDisplayName("Repeat")
            .build()

        session.setMediaButtonPreferences(listOf(shuffleButton, repeatButton))
    }

    private fun RepeatMode.toPlayerRepeatMode(): Int {
        return when (this) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    companion object {
        private const val TAG = "PlaybackService"
        private const val WAKE_LOCK_TAG = "Harmonixia:PlaybackWakeLock"
    }
}
