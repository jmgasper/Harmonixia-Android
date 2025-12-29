package com.harmonixia.android.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DownloadProgress(
    val trackId: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedBps: Long = 0L,
    val progress: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis()
)
