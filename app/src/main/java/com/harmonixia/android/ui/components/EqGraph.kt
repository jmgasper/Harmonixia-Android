package com.harmonixia.android.ui.components

import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harmonixia.android.domain.model.EqFilter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun EqGraphCanvas(
    filters: List<EqFilter>,
    width: Dp,
    height: Dp,
    placeholder: String?,
    modifier: Modifier = Modifier
) {
    val curvePoints = remember(filters) { calculateCurvePoints(filters) }
    val density = LocalDensity.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val curveColor = MaterialTheme.colorScheme.primary
    val textPaint = remember {
        TextPaint().apply {
            isAntiAlias = true
        }
    }
    val labelSizePx = with(density) { 10.sp.toPx() }

    Box(
        modifier = modifier.size(width, height),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width, height)) {
            textPaint.color = labelColor.toArgb()
            textPaint.textSize = labelSizePx

            val minFreq = MIN_FREQUENCY
            val maxFreq = MAX_FREQUENCY
            val minGain = MIN_GAIN_DB
            val maxGain = MAX_GAIN_DB
            val logMin = ln(minFreq)
            val logMax = ln(maxFreq)
            val gainRange = maxGain - minGain
            val freqTicks = listOf(20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000)
            val gainTicks = listOf(-24, -18, -12, -6, 0, 6, 12)

            fun freqToX(freq: Double): Float {
                val logFreq = ln(freq)
                val ratio = (logFreq - logMin) / (logMax - logMin)
                return (ratio * size.width).toFloat()
            }

            fun gainToY(gain: Double): Float {
                val ratio = (gain - minGain) / gainRange
                return (size.height - ratio * size.height).toFloat()
            }

            freqTicks.forEach { freq ->
                val x = freqToX(freq.toDouble())
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText(
                    formatFrequencyLabel(freq),
                    x + 4.dp.toPx(),
                    size.height - 4.dp.toPx(),
                    textPaint
                )
            }

            gainTicks.forEach { gain ->
                val y = gainToY(gain.toDouble())
                val stroke = if (gain == 0) 2.dp.toPx() else 1.dp.toPx()
                drawLine(
                    color = if (gain == 0) labelColor else gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke
                )
                drawContext.canvas.nativeCanvas.drawText(
                    formatGainLabel(gain),
                    4.dp.toPx(),
                    y - 4.dp.toPx(),
                    textPaint
                )
            }

            if (curvePoints.isNotEmpty()) {
                val path = Path()
                curvePoints.forEachIndexed { index, point ->
                    val x = freqToX(point.frequency)
                    val y = gainToY(point.gainDb.coerceIn(minGain, maxGain))
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = curveColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        if (placeholder != null && filters.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor
            )
        }
    }
}

private data class EqCurvePoint(
    val frequency: Double,
    val gainDb: Double
)

private fun calculateCurvePoints(filters: List<EqFilter>): List<EqCurvePoint> {
    if (filters.isEmpty()) return emptyList()
    val points = mutableListOf<EqCurvePoint>()
    val minFreq = MIN_FREQUENCY
    val maxFreq = MAX_FREQUENCY
    val samples = SAMPLE_POINTS
    for (index in 0 until samples) {
        val t = index.toDouble() / (samples - 1).toDouble()
        val freq = minFreq * (maxFreq / minFreq).pow(t)
        var magnitude = 1.0
        filters.forEach { filter ->
            magnitude *= peakingResponse(filter, freq)
        }
        val gainDb = if (magnitude > 0) 20.0 * log10(magnitude) else MIN_GAIN_DB
        points.add(EqCurvePoint(freq, gainDb))
    }
    return points
}

private fun peakingResponse(filter: EqFilter, freq: Double): Double {
    val gainDb = filter.gain
    val q = max(filter.q, 0.1)
    val w0 = 2.0 * Math.PI * filter.frequency / SAMPLE_RATE
    val w = 2.0 * Math.PI * freq / SAMPLE_RATE
    val alpha = sin(w0) / (2.0 * q)
    val a = 10.0.pow(gainDb / 40.0)

    val b0 = 1 + alpha * a
    val b1 = -2 * cos(w0)
    val b2 = 1 - alpha * a
    val a0 = 1 + alpha / a
    val a1 = -2 * cos(w0)
    val a2 = 1 - alpha / a

    val cosW = cos(w)
    val cos2W = cos(2 * w)
    val sinW = sin(w)
    val sin2W = sin(2 * w)

    val num = (b0 + b1 * cosW + b2 * cos2W).pow(2) + (b1 * sinW + b2 * sin2W).pow(2)
    val den = (a0 + a1 * cosW + a2 * cos2W).pow(2) + (a1 * sinW + a2 * sin2W).pow(2)

    return sqrt(num / den)
}

private fun formatFrequencyLabel(freq: Int): String {
    return if (freq >= 1000) {
        val value = freq / 1000.0
        if (value % 1.0 == 0.0) {
            "${value.toInt()}k"
        } else {
            String.format(Locale.US, "%.1fk", value)
        }
    } else {
        freq.toString()
    }
}

private fun formatGainLabel(gain: Int): String {
    return if (gain > 0) {
        "+$gain"
    } else {
        gain.toString()
    }
}

private const val MIN_FREQUENCY = 20.0
private const val MAX_FREQUENCY = 20000.0
private const val MIN_GAIN_DB = -24.0
private const val MAX_GAIN_DB = 12.0
private const val SAMPLE_RATE = 48000.0
private const val SAMPLE_POINTS = 240
