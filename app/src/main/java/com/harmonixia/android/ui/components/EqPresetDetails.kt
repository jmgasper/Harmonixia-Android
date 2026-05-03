package com.harmonixia.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.harmonixia.android.domain.model.EqPreset
import com.harmonixia.android.domain.model.EqPresetDetails
import com.harmonixia.android.R
import java.util.Locale

@Composable
fun PresetDetailsCard(
    preset: EqPreset?,
    details: EqPresetDetails?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (preset == null) {
                Text(
                    text = stringResource(R.string.eq_no_preset_selected),
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            preset.manufacturer?.takeIf { it.isNotBlank() }?.let {
                DetailLine(label = stringResource(R.string.eq_detail_manufacturer), value = it)
            }
            preset.model?.takeIf { it.isNotBlank() }?.let {
                DetailLine(label = stringResource(R.string.eq_detail_model), value = it)
            }
            preset.creator?.takeIf { it.isNotBlank() }?.let {
                DetailLine(label = stringResource(R.string.eq_detail_creator), value = it)
            }
            preset.description?.takeIf { it.isNotBlank() }?.let {
                DetailLine(label = stringResource(R.string.eq_detail_description), value = it)
            }

            details?.let {
                Spacer(modifier = Modifier.height(12.dp))
                DetailLine(
                    label = stringResource(R.string.eq_detail_band_count),
                    value = it.filterCount.toString()
                )
                DetailLine(
                    label = stringResource(R.string.eq_detail_supported_bands),
                    value = it.supportedBands.toString()
                )
                DetailLine(
                    label = stringResource(R.string.eq_detail_dropped_filters),
                    value = it.droppedFilters.toString()
                )
                if (it.droppedFilters > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.eq_detail_dropped_filters_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (preset.filters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.eq_detail_filters_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                preset.filters.forEach { filter ->
                    Text(
                        text = stringResource(
                            R.string.eq_detail_filter_line,
                            formatFrequency(filter.frequency),
                            formatGain(filter.gain),
                            formatQ(filter.q)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text(
        text = stringResource(R.string.eq_detail_line_format, label, value),
        style = MaterialTheme.typography.bodyMedium
    )
}

private fun formatFrequency(frequency: Double): String {
    return if (frequency >= 1000) {
        val value = frequency / 1000.0
        String.format(Locale.US, "%.1f kHz", value)
    } else {
        String.format(Locale.US, "%.0f Hz", frequency)
    }
}

private fun formatGain(gain: Double): String {
    val sign = if (gain > 0) "+" else ""
    return String.format(Locale.US, "%s%.1f dB", sign, gain)
}

private fun formatQ(q: Double): String {
    return String.format(Locale.US, "%.2f", q)
}
