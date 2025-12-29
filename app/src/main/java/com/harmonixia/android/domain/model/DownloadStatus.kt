package com.harmonixia.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PAUSED
}
