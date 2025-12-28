package com.harmonixia.android.di

import com.harmonixia.android.data.repository.EqPresetRepositoryImpl
import com.harmonixia.android.domain.repository.EqPresetRepository
import com.harmonixia.android.service.playback.EqualizerManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EqModule {

    @Binds
    @Singleton
    abstract fun bindEqPresetRepository(
        impl: EqPresetRepositoryImpl
    ): EqPresetRepository

    companion object {
        @Provides
        @Singleton
        fun provideEqualizerManager(): EqualizerManager = EqualizerManager()
    }
}
