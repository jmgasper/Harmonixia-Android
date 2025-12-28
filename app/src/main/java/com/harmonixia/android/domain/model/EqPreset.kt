package com.harmonixia.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EqPreset(
    val id: String,
    val name: String,
    val displayName: String,
    val manufacturer: String? = null,
    val model: String? = null,
    val creator: String? = null,
    val description: String? = null,
    val filters: List<EqFilter> = emptyList()
)

@Serializable
data class EqFilter(
    val frequency: Double,
    val gain: Double,
    val q: Double,
    val type: String
)

@Serializable
data class EqBandConfig(
    val freq: Double,
    val bandwidth: Double,
    val gain: Double
)

data class EqPresetDetails(
    val presetId: String,
    val filterCount: Int,
    val supportedBands: Int,
    val droppedFilters: Int
)
