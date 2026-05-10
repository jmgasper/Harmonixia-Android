package com.harmonixia.android.domain.usecase

import android.content.Context
import com.harmonixia.android.R
import com.harmonixia.android.data.local.SettingsDataStore
import com.harmonixia.android.domain.model.AuthMethod
import com.harmonixia.android.domain.repository.MusicAssistantRepository
import com.harmonixia.android.util.ValidationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ConnectToServerUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
            return Result.failure(
                IllegalArgumentException(context.getString(R.string.error_server_url_required))
            )
        }
        val normalizedUrl = ValidationUtils.normalizeUrl(serverUrl)
        if (!ValidationUtils.isValidUrl(normalizedUrl)) {
            return Result.failure(
                IllegalArgumentException(context.getString(R.string.error_invalid_url))
            )
        }
        if (authMethod == AuthMethod.USERNAME_PASSWORD) {
            if (username.isBlank()) {
                return Result.failure(
                    IllegalArgumentException(context.getString(R.string.error_username_required))
                )
            }
            if (password.isBlank()) {
                return Result.failure(
                    IllegalArgumentException(context.getString(R.string.error_password_required))
                )
            }
        }
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
}
