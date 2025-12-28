package com.harmonixia.android.domain.usecase

import com.harmonixia.android.data.remote.ConnectionState
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class GetConnectionStateUseCase @Inject constructor(
    private val repository: MusicAssistantRepository
) {
    operator fun invoke(): StateFlow<ConnectionState> {
        return repository.getConnectionState()
    }
}
