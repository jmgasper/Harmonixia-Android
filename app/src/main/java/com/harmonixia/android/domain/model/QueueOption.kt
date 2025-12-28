package com.harmonixia.android.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class QueueOption {
    @SerialName("replace")
    REPLACE,
    @SerialName("add")
    ADD,
    @SerialName("next")
    NEXT,
    @SerialName("replace_next")
    REPLACE_NEXT
}
