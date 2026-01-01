package com.harmonixia.android.service.playback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.harmonixia.android.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

data class SendspinPcmFormat(
    val sampleRate: Int,
    val channels: Int,
    val bitDepth: Int
) {
    val frameSize: Int get() = channels * (bitDepth / 8)
}

class SendspinAudioOutput(
    private val equalizerManager: EqualizerManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val lock = Any()
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var currentFormat: SendspinPcmFormat? = null
    private var audioChannel: Channel<AudioChunk>? = null
    private var audioJob: Job? = null
    private var volume: Float = 1f
    private var muted: Boolean = false
    private val playbackGeneration = AtomicInteger(0)
    private val eqProcessor = PcmEqProcessor()

    fun start(format: SendspinPcmFormat) {
        synchronized(lock) {
            if (currentFormat != format) {
                rebuildTrackLocked(format)
            } else {
                invalidateBuffersLocked()
            }
        }
        ensureWriter()
    }

    fun stop() {
        synchronized(lock) {
            invalidateBuffersLocked()
            closeChannelLocked()
            releaseTrackLocked()
        }
    }

    fun flush() {
        synchronized(lock) {
            invalidateBuffersLocked()
            val track = audioTrack ?: return
            try {
                track.pause()
                track.flush()
                track.play()
            } catch (error: Exception) {
                Logger.w(TAG, "Failed to flush AudioTrack", error)
            }
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    fun enqueueAudio(timestampUs: Long, data: ByteArray) {
        val channel = audioChannel ?: return
        val chunk = AudioChunk(playbackGeneration.get(), timestampUs, data)
        if (channel.trySend(chunk).isSuccess) return
        channel.tryReceive()
        if (!channel.trySend(chunk).isSuccess) {
            Logger.w(TAG, "Audio buffer full; dropping chunk")
        } else {
            Logger.w(TAG, "Audio buffer full; dropping oldest chunk")
        }
    }

    fun setVolume(volume: Float, muted: Boolean) {
        synchronized(lock) {
            this.volume = volume
            this.muted = muted
            applyVolumeLocked()
        }
    }

    private fun ensureWriter() {
        if (audioJob?.isActive == true) return
        val channel = Channel<AudioChunk>(AUDIO_QUEUE_CAPACITY)
        audioChannel = channel
        audioJob = scope.launch {
            for (chunk in channel) {
                writeChunk(chunk)
            }
        }
    }

    private fun writeChunk(chunk: AudioChunk) {
        val track = audioTrack ?: return
        if (chunk.generation != playbackGeneration.get()) return
        val format = currentFormat ?: return
        eqProcessor.process(
            chunk.data,
            format,
            equalizerManager.getSoftwareEqConfig(),
            equalizerManager.getSoftwareEqVersion()
        )
        var offset = 0
        val data = chunk.data
        while (offset < data.size) {
            if (chunk.generation != playbackGeneration.get() || audioTrack !== track) return
            val written = try {
                track.write(data, offset, data.size - offset, AudioTrack.WRITE_BLOCKING)
            } catch (error: Exception) {
                Logger.w(TAG, "AudioTrack write failed", error)
                return
            }
            if (written <= 0) {
                Logger.w(TAG, "AudioTrack write returned $written")
                return
            }
            offset += written
        }
    }

    private fun rebuildTrackLocked(format: SendspinPcmFormat) {
        invalidateBuffersLocked()
        releaseTrackLocked()
        val track = buildAudioTrack(format)
        if (track == null) {
            Logger.w(TAG, "Unsupported PCM format: $format")
            return
        }
        currentFormat = format
        audioTrack = track
        applyVolumeLocked()
        equalizerManager.setSoftwareEqActive(true)
        equalizerManager.attachAudioSession(track.audioSessionId)
        try {
            track.play()
        } catch (error: Exception) {
            Logger.w(TAG, "Failed to start AudioTrack", error)
        }
    }

    private fun buildAudioTrack(format: SendspinPcmFormat): AudioTrack? {
        val encoding = when (format.bitDepth) {
            16 -> AudioFormat.ENCODING_PCM_16BIT
            24 -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            32 -> AudioFormat.ENCODING_PCM_32BIT
            else -> return null
        }
        val channelMask = when (format.channels) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> return null
        }
        val minBuffer = AudioTrack.getMinBufferSize(format.sampleRate, channelMask, encoding)
        val bufferSize = max(minBuffer, format.frameSize * BUFFER_FRAME_COUNT) * BUFFER_MULTIPLIER
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(format.sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(encoding)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .build()
        }.getOrElse { error ->
            Logger.w(TAG, "Failed to create AudioTrack", error)
            return null
        }
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            Logger.w(TAG, "AudioTrack failed to initialize")
            runCatching { track.release() }
            return null
        }
        return track
    }

    private fun applyVolumeLocked() {
        val track = audioTrack ?: return
        val effective = if (muted) 0f else volume.coerceIn(0f, 1f)
        try {
            track.setVolume(effective)
        } catch (error: Exception) {
            Logger.w(TAG, "Failed to set AudioTrack volume", error)
        }
    }

    private fun closeChannelLocked() {
        audioChannel?.close()
        audioChannel = null
        audioJob?.cancel()
        audioJob = null
    }

    private fun invalidateBuffersLocked() {
        playbackGeneration.incrementAndGet()
        drainChannelLocked()
    }

    private fun drainChannelLocked() {
        val channel = audioChannel ?: return
        while (channel.tryReceive().isSuccess) {
            // Drain queued audio before flushing.
        }
    }

    private fun releaseTrackLocked() {
        val track = audioTrack ?: return
        audioTrack = null
        currentFormat = null
        equalizerManager.setSoftwareEqActive(false)
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
        } catch (error: Exception) {
            Logger.w(TAG, "Failed to stop AudioTrack", error)
        } finally {
            runCatching { track.release() }
        }
    }

    private data class AudioChunk(
        val generation: Int,
        val timestampUs: Long,
        val data: ByteArray
    )

    companion object {
        private const val TAG = "SendspinAudioOutput"
        private const val AUDIO_QUEUE_CAPACITY = 256
        private const val BUFFER_FRAME_COUNT = 1024
        private const val BUFFER_MULTIPLIER = 2
    }
}
