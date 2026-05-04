package com.harmonixia.android.ui.components

import com.harmonixia.android.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackQualityFormatterTest {

    @Test
    fun formatTrackQualityLabel_losslessDetail_usesResolverUnits() {
        val label = formatTrackQualityLabel(
            quality = "lossless 48kHz/24-bit",
            resolveLabel = resolver(
                mapOf(
                    R.string.track_quality_lossless to "Sin pérdida",
                    R.string.track_quality_unit_khz to "kHzX",
                    R.string.track_quality_unit_kbps to "kbpsX",
                    R.string.track_quality_unit_bit to "bitX"
                )
            )
        )

        assertEquals("Sin pérdida 48kHzX/24-bitX", label)
    }

    @Test
    fun formatTrackQualityLabel_bitrateOnly_usesResolverUnit() {
        val label = formatTrackQualityLabel(
            quality = "320 kbps",
            resolveLabel = resolver(
                mapOf(
                    R.string.track_quality_unit_khz to "kHzX",
                    R.string.track_quality_unit_kbps to "kbpsX",
                    R.string.track_quality_unit_bit to "bitX"
                )
            )
        )

        assertEquals("320 kbpsX", label)
    }

    private fun resolver(values: Map<Int, String>): (Int) -> String = { resId ->
        values[resId] ?: error("Missing resolver value for resource id: $resId")
    }
}
