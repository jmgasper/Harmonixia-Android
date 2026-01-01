package com.harmonixia.android.util

import java.net.URI

enum class UrlValidationError {
    EMPTY,
    INVALID_FORMAT
}

data class UrlValidationResult(
    val isValid: Boolean,
    val error: UrlValidationError? = null
)

enum class UsernameValidationError {
    EMPTY,
    INVALID_FORMAT
}

enum class PasswordValidationError {
    EMPTY,
    TOO_SHORT
}

data class UsernameValidationResult(
    val isValid: Boolean,
    val error: UsernameValidationError? = null
)

data class PasswordValidationResult(
    val isValid: Boolean,
    val error: PasswordValidationError? = null
)

object ValidationUtils {
    private val ipRegex = Regex(
        "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"
    )
    private val domainRegex = Regex(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    )
    private val usernameRegex = Regex("^[A-Za-z0-9_.-]+$")
    private const val MIN_PASSWORD_LENGTH = 4

    fun isValidUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return false
        val normalized = normalizeUrl(trimmed)
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host ?: return false
        return host == "localhost" || isValidIpAddress(host) || isValidDomain(host)
    }

    fun isValidIpAddress(input: String): Boolean {
        return ipRegex.matches(input)
    }

    fun isValidDomain(input: String): Boolean {
        return domainRegex.matches(input)
    }

    fun normalizeUrl(input: String): String {
        var normalized = input.trim().trimEnd('/')
        normalized = when {
            normalized.startsWith("http://") || normalized.startsWith("https://") -> normalized
            normalized.startsWith("ws://") -> "http://${normalized.removePrefix("ws://")}"
            normalized.startsWith("wss://") -> "https://${normalized.removePrefix("wss://")}"
            else -> "http://$normalized"
        }
        return normalized.trimEnd('/')
    }

    fun validateServerUrl(input: String): UrlValidationResult {
        if (input.isBlank()) {
            return UrlValidationResult(isValid = false, error = UrlValidationError.EMPTY)
        }
        if (!isValidUrl(input)) {
            return UrlValidationResult(isValid = false, error = UrlValidationError.INVALID_FORMAT)
        }
        return UrlValidationResult(isValid = true)
    }

    fun validateUsername(input: String): UsernameValidationResult {
        if (input.isBlank()) {
            return UsernameValidationResult(isValid = false, error = UsernameValidationError.EMPTY)
        }
        if (!usernameRegex.matches(input)) {
            return UsernameValidationResult(isValid = false, error = UsernameValidationError.INVALID_FORMAT)
        }
        return UsernameValidationResult(isValid = true)
    }

    fun validatePassword(input: String): PasswordValidationResult {
        if (input.isBlank()) {
            return PasswordValidationResult(isValid = false, error = PasswordValidationError.EMPTY)
        }
        if (input.length < MIN_PASSWORD_LENGTH) {
            return PasswordValidationResult(isValid = false, error = PasswordValidationError.TOO_SHORT)
        }
        return PasswordValidationResult(isValid = true)
    }
}
