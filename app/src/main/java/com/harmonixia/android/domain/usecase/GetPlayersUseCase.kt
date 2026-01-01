package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.model.Player
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class GetPlayersUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke(): Result<List<Player>> {
        return runCatching { repository.fetchPlayers().getOrThrow() }
    }
}
