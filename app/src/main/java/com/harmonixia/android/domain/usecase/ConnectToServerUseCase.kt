package com.harmonixia.android.domain.usecase

import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class ConnectToServerUseCase @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(
        serverUrl: String,
        authToken: String,
        persistSettings: Boolean = true
    ): Result<Unit> {
        if (serverUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Server URL cannot be empty"))
        }
        val normalizedUrl = normalizeUrl(serverUrl)
        val sanitizedToken = authToken.trim()
        val result = repository.connect(normalizedUrl, sanitizedToken)
        return result.fold(
            onSuccess = {
                if (!persistSettings) {
                    Result.success(Unit)
                } else {
                    runCatching {
                        settingsDataStore.saveServerUrl(normalizedUrl)
                        settingsDataStore.saveAuthToken(sanitizedToken)
                    }
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun normalizeUrl(input: String): String {
        var normalized = input.trim().trimEnd('/')
        normalized = when {
            normalized.startsWith("http://") || normalized.startsWith("https://") -> normalized
            normalized.startsWith("ws://") -> "http://${normalized.removePrefix("ws://")}"
            normalized.startsWith("wss://") -> "https://${normalized.removePrefix("wss://")}"
            else -> "http://$normalized"
        }
        return normalized.trimEnd('/')
    }
}
