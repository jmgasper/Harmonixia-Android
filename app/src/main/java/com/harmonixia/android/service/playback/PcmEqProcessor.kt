package com.harmonixia.android.service.playback

import com.harmonixia.android.domain.model.EqFilter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

class PcmEqProcessor {
    private var sampleRate = 0
    private var channels = 0
    private var bitDepth = 0
    private var enabled = false
    private var configVersion = -1
    private var filters: List<BiquadFilter> = emptyList()

    fun process(
        buffer: ByteArray,
        format: SendspinPcmFormat,
        config: SoftwareEqConfig,
        version: Int
    ) {
        if (buffer.isEmpty()) return
        updateConfig(format, config, version)
        if (!enabled || filters.isEmpty()) return

        val bytesPerSample = bitDepth / 8
        val frameSize = bytesPerSample * channels
        if (bytesPerSample <= 0 || frameSize <= 0) return
        val frameCount = buffer.size / frameSize
        var frameOffset = 0
        repeat(frameCount) {
            for (channel in 0 until channels) {
                val sampleOffset = frameOffset + channel * bytesPerSample
                val sample = readSample(buffer, sampleOffset, bytesPerSample)
                var value = sampleToFloat(sample, bitDepth)
                for (filter in filters) {
                    value = filter.process(value, channel)
                }
                val output = floatToSample(value, bitDepth)
                writeSample(buffer, sampleOffset, bytesPerSample, output)
            }
            frameOffset += frameSize
        }
    }

    private fun updateConfig(
        format: SendspinPcmFormat,
        config: SoftwareEqConfig,
        version: Int
    ) {
        val formatChanged = format.sampleRate != sampleRate ||
            format.channels != channels ||
            format.bitDepth != bitDepth
        if (!formatChanged && version == configVersion) return

        sampleRate = format.sampleRate
        channels = format.channels
        bitDepth = format.bitDepth
        enabled = config.enabled
        configVersion = version

        filters = if (enabled) {
            buildFilters(config.filters, sampleRate, channels)
        } else {
            emptyList()
        }
    }

    private fun buildFilters(
        filters: List<EqFilter>,
        sampleRate: Int,
        channels: Int
    ): List<BiquadFilter> {
        if (sampleRate <= 0 || channels <= 0) return emptyList()
        val nyquist = sampleRate / 2.0
        if (nyquist <= MIN_FREQUENCY) return emptyList()
        return filters.mapNotNull { filter ->
            if (abs(filter.gain) < MIN_GAIN_DB) return@mapNotNull null
            val q = filter.q.takeIf { it > MIN_Q } ?: MIN_Q
            val frequency = filter.frequency
                .coerceIn(MIN_FREQUENCY, nyquist - 1.0)
            if (frequency <= 0.0 || frequency >= nyquist) return@mapNotNull null

            val w0 = 2.0 * PI * frequency / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)
            val a = 10.0.pow(filter.gain / 40.0)

            // Audio EQ Cookbook biquad for peaking EQ.
            val b0 = 1.0 + alpha * a
            val b1 = -2.0 * cosW0
            val b2 = 1.0 - alpha * a
            val a0 = 1.0 + alpha / a
            val a1 = -2.0 * cosW0
            val a2 = 1.0 - alpha / a
            val norm = 1.0 / a0

            BiquadFilter(
                (b0 * norm).toFloat(),
                (b1 * norm).toFloat(),
                (b2 * norm).toFloat(),
                (a1 * norm).toFloat(),
                (a2 * norm).toFloat(),
                channels
            )
        }
    }

    private fun readSample(buffer: ByteArray, offset: Int, bytesPerSample: Int): Int {
        return when (bytesPerSample) {
            2 -> {
                (buffer[offset].toInt() and 0xFF) or (buffer[offset + 1].toInt() shl 8)
            }
            3 -> {
                val b0 = buffer[offset].toInt() and 0xFF
                val b1 = buffer[offset + 1].toInt() and 0xFF
                val b2 = buffer[offset + 2].toInt()
                b0 or (b1 shl 8) or (b2 shl 16)
            }
            4 -> {
                val b0 = buffer[offset].toInt() and 0xFF
                val b1 = buffer[offset + 1].toInt() and 0xFF
                val b2 = buffer[offset + 2].toInt() and 0xFF
                val b3 = buffer[offset + 3].toInt()
                b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            }
            else -> 0
        }
    }

    private fun writeSample(
        buffer: ByteArray,
        offset: Int,
        bytesPerSample: Int,
        sample: Int
    ) {
        when (bytesPerSample) {
            2 -> {
                buffer[offset] = sample.toByte()
                buffer[offset + 1] = (sample shr 8).toByte()
            }
            3 -> {
                buffer[offset] = sample.toByte()
                buffer[offset + 1] = (sample shr 8).toByte()
                buffer[offset + 2] = (sample shr 16).toByte()
            }
            4 -> {
                buffer[offset] = sample.toByte()
                buffer[offset + 1] = (sample shr 8).toByte()
                buffer[offset + 2] = (sample shr 16).toByte()
                buffer[offset + 3] = (sample shr 24).toByte()
            }
        }
    }

    private fun sampleToFloat(sample: Int, bitDepth: Int): Float {
        return when (bitDepth) {
            16 -> sample / 32768f
            24 -> sample / 8388608f
            32 -> sample / 2147483648f
            else -> 0f
        }
    }

    private fun floatToSample(value: Float, bitDepth: Int): Int {
        val clipped = value.coerceIn(-1f, 1f)
        return when (bitDepth) {
            16 -> (clipped * 32768f).roundToInt().coerceIn(-32768, 32767)
            24 -> (clipped * 8388608f).roundToInt().coerceIn(-8388608, 8388607)
            32 -> {
                val scaled = clipped * 2147483648f
                when {
                    scaled >= Int.MAX_VALUE.toFloat() -> Int.MAX_VALUE
                    scaled <= Int.MIN_VALUE.toFloat() -> Int.MIN_VALUE
                    else -> scaled.roundToInt()
                }
            }
            else -> 0
        }
    }

    private class BiquadFilter(
        private val b0: Float,
        private val b1: Float,
        private val b2: Float,
        private val a1: Float,
        private val a2: Float,
        channels: Int
    ) {
        private val x1 = FloatArray(channels)
        private val x2 = FloatArray(channels)
        private val y1 = FloatArray(channels)
        private val y2 = FloatArray(channels)

        fun process(sample: Float, channel: Int): Float {
            val output = b0 * sample +
                b1 * x1[channel] +
                b2 * x2[channel] -
                a1 * y1[channel] -
                a2 * y2[channel]
            x2[channel] = x1[channel]
            x1[channel] = sample
            y2[channel] = y1[channel]
            y1[channel] = output
            return output
        }
    }

    companion object {
        private const val MIN_Q = 0.1
        private const val MIN_GAIN_DB = 0.01
        private const val MIN_FREQUENCY = 20.0
    }
}
