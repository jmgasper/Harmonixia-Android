package com.harmonixia.android.ui.util

import coil3.ImageLoader
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaylistCoverEntryPoint {
    fun repository(): MusicAssistantRepository
    fun imageLoader(): ImageLoader
}
