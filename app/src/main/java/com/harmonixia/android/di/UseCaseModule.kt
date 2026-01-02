package com.harmonixia.android.di

import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.domain.repository.EqPresetRepository
import com.harmonixia.android.domain.usecase.ControlPlaybackUseCase
import com.harmonixia.android.domain.usecase.DeletePlaylistUseCase
import com.harmonixia.android.domain.usecase.ApplyEqPresetUseCase
import com.harmonixia.android.domain.usecase.GetEqSettingsUseCase
import com.harmonixia.android.domain.usecase.GetPlayersUseCase
import com.harmonixia.android.domain.usecase.LoadEqPresetsUseCase
import com.harmonixia.android.domain.usecase.PlayAlbumUseCase
import com.harmonixia.android.domain.usecase.PlayLocalTracksUseCase
import com.harmonixia.android.domain.usecase.PlayPlaylistUseCase
import com.harmonixia.android.domain.usecase.RenamePlaylistUseCase
import com.harmonixia.android.domain.usecase.SearchEqPresetsUseCase
import com.harmonixia.android.domain.usecase.SearchLibraryUseCase
import com.harmonixia.android.domain.usecase.SetPlayerVolumeUseCase
import com.harmonixia.android.domain.usecase.SetPlayerMuteUseCase
import com.harmonixia.android.data.local.EqDataStore
import com.harmonixia.android.data.local.EqPresetParser
import com.harmonixia.android.service.playback.EqualizerManager
import com.harmonixia.android.service.playback.PlaybackServiceConnection
import com.harmonixia.android.service.playback.PlaybackStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun providePlayAlbumUseCase(
        repository: MusicAssistantRepository,
        playbackStateManager: PlaybackStateManager
    ): PlayAlbumUseCase = PlayAlbumUseCase(repository, playbackStateManager)

    @Provides
    @Singleton
    fun providePlayPlaylistUseCase(
        repository: MusicAssistantRepository,
        playbackStateManager: PlaybackStateManager
    ): PlayPlaylistUseCase = PlayPlaylistUseCase(repository, playbackStateManager)

    @Provides
    @Singleton
    fun providePlayLocalTracksUseCase(
        playbackServiceConnection: PlaybackServiceConnection
    ): PlayLocalTracksUseCase = PlayLocalTracksUseCase(playbackServiceConnection)

    @Provides
    @Singleton
    fun provideControlPlaybackUseCase(
        repository: MusicAssistantRepository,
        playbackStateManager: PlaybackStateManager
    ): ControlPlaybackUseCase = ControlPlaybackUseCase(repository, playbackStateManager)

    @Provides
    @Singleton
    fun provideDeletePlaylistUseCase(
        repository: MusicAssistantRepository
    ): DeletePlaylistUseCase = DeletePlaylistUseCase(repository)

    @Provides
    @Singleton
    fun provideRenamePlaylistUseCase(
        repository: MusicAssistantRepository
    ): RenamePlaylistUseCase = RenamePlaylistUseCase(repository)

    @Provides
    @Singleton
    fun provideSearchLibraryUseCase(
        repository: MusicAssistantRepository
    ): SearchLibraryUseCase = SearchLibraryUseCase(repository)

    @Provides
    @Singleton
    fun provideGetPlayersUseCase(
        repository: MusicAssistantRepository
    ): GetPlayersUseCase = GetPlayersUseCase(repository)

    @Provides
    @Singleton
    fun provideSetPlayerVolumeUseCase(
        repository: MusicAssistantRepository
    ): SetPlayerVolumeUseCase = SetPlayerVolumeUseCase(repository)

    @Provides
    @Singleton
    fun provideSetPlayerMuteUseCase(
        repository: MusicAssistantRepository
    ): SetPlayerMuteUseCase = SetPlayerMuteUseCase(repository)

    @Provides
    @Singleton
    fun provideLoadEqPresetsUseCase(
        repository: EqPresetRepository
    ): LoadEqPresetsUseCase = LoadEqPresetsUseCase(repository)

    @Provides
    @Singleton
    fun provideSearchEqPresetsUseCase(
        repository: EqPresetRepository
    ): SearchEqPresetsUseCase = SearchEqPresetsUseCase(repository)

    @Provides
    @Singleton
    fun provideApplyEqPresetUseCase(
        repository: EqPresetRepository,
        eqDataStore: EqDataStore,
        eqPresetParser: EqPresetParser,
        equalizerManager: EqualizerManager,
        playbackServiceConnection: PlaybackServiceConnection
    ): ApplyEqPresetUseCase = ApplyEqPresetUseCase(
        repository,
        eqDataStore,
        eqPresetParser,
        equalizerManager,
        playbackServiceConnection
    )

    @Provides
    @Singleton
    fun provideGetEqSettingsUseCase(
        eqDataStore: EqDataStore
    ): GetEqSettingsUseCase = GetEqSettingsUseCase(eqDataStore)
}
