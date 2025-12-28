package com.harmonixia.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EqSettings(
    val enabled: Boolean = false,
    val selectedPresetId: String? = null,
    val customBands: List<EqBandConfig>? = null
)
