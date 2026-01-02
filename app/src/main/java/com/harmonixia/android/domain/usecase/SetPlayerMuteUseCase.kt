package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class SetPlayerMuteUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(playerId: String, muted: Boolean): Result<Unit> {
        return repository.setPlayerMute(playerId, muted)
    }
}
