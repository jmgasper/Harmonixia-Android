package com.harmonixia.android.ui.screens.settings.localmedia

import com.harmonixia.android.ui.screens.settings.LocalMediaScanState

data class LocalMediaSettingsUiState(
    val folderUri: String = "",
    val trackCount: Int = 0,
    val scanState: LocalMediaScanState = LocalMediaScanState()
)
