package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class SetPlayerVolumeUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(playerId: String, volume: Int): Result<Unit> {
        return repository.setPlayerVolume(playerId, volume.coerceIn(0, 100))
    }
}
