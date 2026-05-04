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
                    R.string.track_quality_label_with_detail_format to "%1\$s %2\$s",
                    R.string.track_quality_detail_sample_rate_bit_depth_format to "%1\$s/%2\$d-%3\$s",
                    R.string.track_quality_detail_bit_depth_format to "%1\$d-%2\$s",
                    R.string.track_quality_detail_khz_format to "%1\$s%2\$s",
                    R.string.track_quality_detail_kbps_format to "%1\$s %2\$s",
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
                    R.string.track_quality_label_with_detail_format to "%1\$s %2\$s",
                    R.string.track_quality_detail_sample_rate_bit_depth_format to "%1\$s/%2\$d-%3\$s",
                    R.string.track_quality_detail_bit_depth_format to "%1\$d-%2\$s",
                    R.string.track_quality_detail_khz_format to "%1\$s%2\$s",
                    R.string.track_quality_detail_kbps_format to "%1\$s %2\$s",
                    R.string.track_quality_unit_khz to "kHzX",
                    R.string.track_quality_unit_kbps to "kbpsX",
                    R.string.track_quality_unit_bit to "bitX"
                )
            )
        )

        assertEquals("320 kbpsX", label)
    }

    @Test
    fun formatTrackQualityLabel_decimalSampleRate_usesResourceTemplate() {
        val label = formatTrackQualityLabel(
            quality = "lossless 44.1 kHz/16-bit",
            resolveLabel = resolver(
                mapOf(
                    R.string.track_quality_lossless to "Sin pérdida",
                    R.string.track_quality_label_with_detail_format to "%1\$s %2\$s",
                    R.string.track_quality_detail_sample_rate_bit_depth_format to "%1\$s/%2\$d-%3\$s",
                    R.string.track_quality_detail_bit_depth_format to "%1\$d-%2\$s",
                    R.string.track_quality_detail_khz_format to "%1\$s%2\$s",
                    R.string.track_quality_detail_kbps_format to "%1\$s %2\$s",
                    R.string.track_quality_decimal_one_place_format to "d%1\$.1f",
                    R.string.track_quality_unit_khz to "kHzX",
                    R.string.track_quality_unit_kbps to "kbpsX",
                    R.string.track_quality_unit_bit to "bitX"
                )
            )
        )

        assertEquals("Sin pérdida d44.1kHzX/16-bitX", label)
    }

    @Test
    fun formatTrackQualityLabel_decimalBitrate_usesResourceTemplate() {
        val label = formatTrackQualityLabel(
            quality = "320.5 kbps",
            resolveLabel = resolver(
                mapOf(
                    R.string.track_quality_label_with_detail_format to "%1\$s %2\$s",
                    R.string.track_quality_detail_sample_rate_bit_depth_format to "%1\$s/%2\$d-%3\$s",
                    R.string.track_quality_detail_bit_depth_format to "%1\$d-%2\$s",
                    R.string.track_quality_detail_khz_format to "%1\$s%2\$s",
                    R.string.track_quality_detail_kbps_format to "%1\$s %2\$s",
                    R.string.track_quality_decimal_one_place_format to "d%1\$.1f",
                    R.string.track_quality_unit_khz to "kHzX",
                    R.string.track_quality_unit_kbps to "kbpsX",
                    R.string.track_quality_unit_bit to "bitX"
                )
            )
        )

        assertEquals("d320.5 kbpsX", label)
    }

    private fun resolver(values: Map<Int, String>): (Int) -> String = { resId ->
        values[resId] ?: error("Missing resolver value for resource id: $resId")
    }
}
