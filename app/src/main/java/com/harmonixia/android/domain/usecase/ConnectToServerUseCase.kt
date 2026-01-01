package com.harmonixia.android.domain.usecase

import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import javax.inject.Inject

class ConnectToServerUseCase @Inject constructor(
    private val repository: MusicAssistantRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(
        serverUrl: String,
        authToken: String = "",
        authMethod: AuthMethod = AuthMethod.TOKEN,
        username: String = "",
        password: String = "",
        persistSettings: Boolean = true
    ): Result<Unit> {
        if (serverUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Server URL cannot be empty"))
        }
        if (authMethod == AuthMethod.USERNAME_PASSWORD) {
            if (username.isBlank()) {
                return Result.failure(IllegalArgumentException("Username cannot be empty"))
            }
            if (password.isBlank()) {
                return Result.failure(IllegalArgumentException("Password cannot be empty"))
            }
        }
        val normalizedUrl = normalizeUrl(serverUrl)
        val tokenResult = when (authMethod) {
            AuthMethod.USERNAME_PASSWORD -> repository.loginWithCredentials(
                normalizedUrl,
                username,
                password
            )
            AuthMethod.TOKEN -> Result.success(authToken.trim())
        }
        return tokenResult.fold(
            onSuccess = { token ->
                val sanitizedToken = token.trim()
                val result = repository.connect(normalizedUrl, sanitizedToken)
                result.fold(
                    onSuccess = {
                        if (!persistSettings) {
                            Result.success(Unit)
                        } else {
                            runCatching {
                                settingsDataStore.saveServerUrl(normalizedUrl)
                                settingsDataStore.saveAuthToken(sanitizedToken)
                                settingsDataStore.saveAuthMethod(authMethod)
                                if (authMethod == AuthMethod.USERNAME_PASSWORD) {
                                    settingsDataStore.saveUsername(username)
                                    settingsDataStore.savePassword(password)
                                } else {
                                    settingsDataStore.saveUsername("")
                                    settingsDataStore.savePassword("")
                                }
                            }
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
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
