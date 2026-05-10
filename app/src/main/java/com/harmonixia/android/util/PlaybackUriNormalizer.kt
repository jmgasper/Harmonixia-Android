package com.harmonixia.android.util

private val URI_SCHEME_PREFIX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private val WINDOWS_DRIVE_PATH_PREFIX = Regex("^[a-zA-Z]:[\\\\/].+")

fun normalizePlaybackUri(value: String?): String? {
    val normalized = value?.trim().orEmpty()
    return normalized.takeIf { it.isNotBlank() }
}

fun normalizePlaybackUriOrOriginal(value: String): String {
    return normalizePlaybackUri(value) ?: value
}

fun resolvePlaybackStreamUri(
    extrasStreamUri: String?,
    localConfigurationUri: String?
): String? {
    return normalizePlaybackUri(extrasStreamUri) ?: normalizePlaybackUri(localConfigurationUri)
}

fun isSchemeLessPlaybackUri(value: String?): Boolean {
    val normalized = normalizePlaybackUri(value) ?: return false
    if (WINDOWS_DRIVE_PATH_PREFIX.containsMatchIn(normalized)) {
        return true
    }
    return !URI_SCHEME_PREFIX.containsMatchIn(normalized)
}
