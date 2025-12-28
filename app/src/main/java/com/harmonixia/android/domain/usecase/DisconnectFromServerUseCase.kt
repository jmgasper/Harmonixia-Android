package com.harmonixia.android.domain.usecase

import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class DisconnectFromServerUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    suspend operator fun invoke() {
        repository.disconnect()
    }
}
