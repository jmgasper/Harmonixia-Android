package com.harmonixia.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProviderMapping(
    @SerialName("item_id")
    val itemId: String,
    @SerialName("provider_instance")
    val providerInstance: String,
    @SerialName("provider_domain")
    val providerDomain: String,
    val available: Boolean = false
)
